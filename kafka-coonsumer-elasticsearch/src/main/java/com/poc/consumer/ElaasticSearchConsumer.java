package com.poc.consumer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElaasticSearchConsumer 
{
	private static Logger logger = LoggerFactory.getLogger(ElaasticSearchConsumer.class.getName());
    public static void main( String[] args )
    {
    	KafkaConsumer<String, String> consumer = createConsumer("twitter_tweets");
    	
    	while(true) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
			records.forEach(record->{
				logger.info("key: "+ record.key()+", value: "+ record.value());
				logger.info("partition: "+record.partition()+", offset: "+record.offset());
			});
		}
    	
    }
    public static KafkaConsumer<String, String> createConsumer(String topic){
		String bootstrapServers = "127.0.0.1:9092";
		String groupId = "kafka-demo-elasticsearch";
		
		// Create consumer config
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");//earliest/latest/none
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		
		//create consumer
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
		consumer.subscribe(Arrays.asList(topic));
		return consumer;
		
    }
}
