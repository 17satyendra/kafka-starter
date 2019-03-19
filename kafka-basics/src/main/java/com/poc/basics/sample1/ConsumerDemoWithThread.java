package com.poc.basics.sample1;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemoWithThread {

	private ConsumerDemoWithThread() {
	}
	public static void main(String[] args) {
		new ConsumerDemoWithThread().run();
	}
	private void run() {
		Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThread.class.getName());
		String topic ="first-topic";
		String bootstrapServers = "127.0.0.1:9092";
		String groupId = "my-sixth-application";
		
		//latch for dealing multiple thread
		CountDownLatch latch = new CountDownLatch(1);
		
		//Creating consumer runnable 
		logger.info("Creating consumer thread");
		Runnable myConsumerRunnable = new ConsumerRunnable(bootstrapServers, groupId, topic, latch);
		
		//Start the thread
		Thread thread = new Thread(myConsumerRunnable);
		thread.start();
		
		//Adding shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			System.out.println("caught shutdown hook");
			logger.info("caught shutdown hook");
			((ConsumerRunnable) myConsumerRunnable).shutDown();
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logger.info("Applicaton has exited");
		}));
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			logger.error("Application got intrupted", e);
		}finally {
			logger.info("Application is closing");
		}
	}
	
	public class ConsumerRunnable implements Runnable{
		private CountDownLatch latch;
		private KafkaConsumer<String, String> consumer;
		Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

		public ConsumerRunnable( String bootstrapServers, 
				String groupId, String topic,
				CountDownLatch latch) {
			// Create consumer config
			Properties properties = new Properties();
			properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
			properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
			properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
			properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");//earliest/latest/none
			properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
			this.latch=latch;
			
			//create consumer
			this.consumer = new KafkaConsumer<>(properties);
			
			//subscribe consumer to our topic(s)
			this.consumer.subscribe(Arrays.asList(topic));
		}
		@Override
		public void run() {
			//poll for new data
			//for demo purpose using true
			
			try {
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
					records.forEach(record -> {
						logger.info("key: " + record.key() + ", value: " + record.value());
						logger.info("partition: " + record.partition() + ", offset: " + record.offset());
					});
				} 
			} catch (WakeupException e) {
				logger.error("Received shutdown signal!!!");
			}finally {
				consumer.close();
				latch.countDown();
			}
		}
		public void shutDown() {
			// the wakeup() method is special method to interrupt consumer.poll();
			// It will throw the WakeUpException			
			consumer.wakeup();
		}
	}

}
