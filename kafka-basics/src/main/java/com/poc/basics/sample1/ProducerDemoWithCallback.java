package com.poc.basics.sample1;

import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerDemoWithCallback {
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class.getName());

		// System.out.println( "Hello World!" );
		String bootstrapServer = "127.0.0.1:9092";
		// create producer properties
		Properties prop = new Properties();
		prop.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
		prop.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		prop.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		// create the producer
		KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(prop);
		for (int i = 0; i < 10; i++) {

			// create producer record
			ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>("first-topic",
					"hello world");
			// send data -asynchronous
			kafkaProducer.send(producerRecord, new Callback() {

				public void onCompletion(RecordMetadata metadata, Exception e) {
					// execute every time a record is successfully sent or an exception is thrown
					if (e == null) {
						logger.info("Received new metadata. \n" + "Topic: " + metadata.topic() + "\n"
								+ "Partition: " + metadata.partition() + "\n" + "Offset: " + metadata.offset() + "\n"
								+ "TimeStamp: " + metadata.timestamp());
					} else {
						e.printStackTrace();
					}
				}
			});
		}
		// flush and close
		kafkaProducer.flush();
		kafkaProducer.close();
	}
}
