package com.poc.producer.sample2;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TwitterProducer {
	
	private Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());
	private String consumerKey = "kCmRrAHyaiBSsXCQn081ScrK7";
	private String consumerSecret = "ney9c6LYLk3tFijh0uMyEnlWiBeP2VeclLHV9BhDlvkJdgLSWF";
	private String token = "816084111987195906-ZU7VcDSVMieV52X9ZYL9a2Mh5JLGQPQ";
	private String secret = "TIFZXnO8DlOaf8SGPeNJV9UIfu0tumjzoWuAvdfhe9UHY";
	List<String> terms = Lists.newArrayList("kafka", "java", "ipl");
	public TwitterProducer() {}
	
	public static void main(String[] args) {
		new TwitterProducer().run();
	}

	private void run() {
		logger.info("Setup");
		/** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
		//Create a twitter client
		Client client = createTwitterClient(msgQueue);
		// Attempts to establish a connection.
		client.connect();
		
		//Create a Kafka producer 
		KafkaProducer<String, String> producer = CreateKafkaProducer();
		
		//add a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(()-> {
			logger.info("Stoping Application");
			logger.info("Shutting down client from twitter");
			client.stop();
			logger.info("closing kafka producer");
			producer.close();
			logger.info("Done!!!!");
		}));
		// on a different thread, or multiple different threads....
		while (!client.isDone()) {
			String msg = null;
			try {
				msg = msgQueue.poll(5, TimeUnit.SECONDS);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (msg != null) {
				logger.info(msg);
				producer.send(new ProducerRecord<String, String>("twitter_tweets", null, msg), 
						new Callback() {
					@Override
					public void onCompletion(RecordMetadata metadata, Exception e) {
						if(e!=null) {
							logger.error("Something thing wrong!!!", e);
						}
					}
				});
			}
		}
		
		//loop to send tweets to kafka
		logger.info("End of Application");
	}

	private KafkaProducer<String, String> CreateKafkaProducer() {
		String bootstrapServer = "127.0.0.1:9092";
        //create producer properties
        Properties prop = new Properties();
        prop.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        prop.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        prop.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
       
        //create safe producer properties
        prop.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        prop.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        prop.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        prop.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5"); // kafka>=2.0 so we
        //can keep this as 5, use 1 otherwise 
       
        //high throughput producer (at the expanse of a bit of latency and CPU usage)
        prop.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        prop.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        prop.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));// 32 KB batch size 
        
        // create the producer
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(prop);
		return kafkaProducer;
	}

	private Client createTwitterClient(BlockingQueue<String> msgQueue) {
		
		/** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms
		
		hosebirdEndpoint.trackTerms(terms);

		// These secrets should be read from a config file
		Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);
		
		ClientBuilder builder = new ClientBuilder()
				  .name("Hosebird-Client-01")// optional: mainly for the logs
				  .hosts(hosebirdHosts)
				  .authentication(hosebirdAuth)
				  .endpoint(hosebirdEndpoint)
				  .processor(new StringDelimitedProcessor(msgQueue));

		return builder.build();
	}

}
