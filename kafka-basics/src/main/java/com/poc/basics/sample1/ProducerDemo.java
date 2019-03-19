package com.poc.basics.sample1;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Hello world!
 */
public class ProducerDemo 
{
    public static void main( String[] args )
    {
//        System.out.println( "Hello World!" );
        String bootstrapServer = "127.0.0.1:9092";
        //create producer properties
        Properties prop = new Properties();
        prop.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        prop.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        prop.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // create the producer
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(prop);
        //create producer record
        ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>("first-topic", "hello world");
        // send data -asynchronous
        kafkaProducer.send(producerRecord);
        // flush and close
        kafkaProducer.flush();
        kafkaProducer.close();
    }
}
