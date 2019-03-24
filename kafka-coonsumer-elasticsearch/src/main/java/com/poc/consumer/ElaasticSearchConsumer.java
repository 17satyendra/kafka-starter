package com.poc.consumer;

import java.io.IOException;
import java.net.Authenticator.RequestorType;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

public class ElaasticSearchConsumer {
	private static Logger logger = LoggerFactory.getLogger(ElaasticSearchConsumer.class.getName());

	public static void main(String[] args) throws IOException {
		KafkaConsumer<String, String> consumer = createConsumer("twitter_tweets");
		RestHighLevelClient client = getESClient();
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
			Integer recordCount = records.count();
			logger.info("Recived "+ recordCount+ " records" );
			BulkRequest bulkRequest = new BulkRequest();
			for(ConsumerRecord<String, String> record : records) {
				try {
					String twitterJson = record.value();
					String id_str = extractIdFromTwitter(twitterJson);
					IndexRequest indexRequest = new IndexRequest("twitter", "tweets", id_str)
							.source(twitterJson, XContentType.JSON);
					bulkRequest.add(indexRequest);//We add to bulk request (takes no time) 
				} catch (NullPointerException e) {
					logger.error("Skiping bad data : "+ record.value());
				}
				//String id = client.index(indexRequest).getId();
				//logger.info(id);
				/*
				 * try { Thread.sleep(10); } catch (InterruptedException e) {
				 * e.printStackTrace(); }
				 */	
			}
			if(recordCount>0) {
				BulkResponse bulkResponse = client.bulk(bulkRequest);
				
				logger.info("committing offset...");
				consumer.commitSync();
				logger.info("offsets has been committed");
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private static JsonParser jsonParser = new JsonParser(); 
	private static String extractIdFromTwitter(String twitterJson) {
		return jsonParser.parse(twitterJson).getAsJsonObject().get("id_str").getAsString();
	}

	public static KafkaConsumer<String, String> createConsumer(String topic) {
		String bootstrapServers = "127.0.0.1:9092";
		String groupId = "kafka-demo-elasticsearch";

		// Create consumer config
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");// earliest/latest/none
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");//disable auto commit of offset
		properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

		// create consumer
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
		consumer.subscribe(Arrays.asList(topic));
		return consumer;
	}

	public static JestClient getJestClient() {
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig.Builder("http://localhost:9200").multiThreaded(true).build());
//		JestClientFactory factory = new JestClientFactory();
//		factory.setHttpClientConfig(new HttpClientConfig(builder));
		/*
		 * JestClientFactory factory = new JestClientFactory(); HttpClientConfig.Builder
		 * builder = new HttpClientConfig.Builder("http://localhost:9200");
		 * factory.setHttpClientConfig(builder.build());
		 */
		return factory.getObject();

	}

	public static RestHighLevelClient getESClient() {
		return new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
	}
}
