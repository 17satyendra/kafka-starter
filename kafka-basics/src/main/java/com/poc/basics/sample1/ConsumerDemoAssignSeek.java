package com.poc.basics.sample1;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemoAssignSeek {
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(ConsumerDemoAssignSeek.class.getName());
		
		String topic ="first-topic";
		String bootstrapServers = "127.0.0.1:9092";
		
		// Create consumer config
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");//earliest/latest/none
		
		//create consumer
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
		
		//assign and seek are mostly user for replay data or fetch a specific message
		
		//assign 
		TopicPartition partitionFromRead = new TopicPartition(topic, 0);
		consumer.assign(Arrays.asList(partitionFromRead));
		
		//seek 
		long offsetToReadFrom = 15L;//start read from this offset
		consumer.seek(partitionFromRead, offsetToReadFrom);
		
		//poll for new data
		//for demo purpose using true
		
		int numberOfmessageToRead = 5;
		boolean keepOnReading = true;
		int numberOfMessageReadSoFar = 0;
		while(keepOnReading) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
			for(ConsumerRecord<String, String> record : records){
				numberOfMessageReadSoFar+=1;
				logger.info("key: "+ record.key()+", value: "+ record.value());
				logger.info("partition: "+record.partition()+", offset: "+record.offset());
				if(numberOfMessageReadSoFar>=numberOfmessageToRead) {
					keepOnReading = false;
					break;
				}
			}
		}
		logger.info("Exiting the application");
	}

}
