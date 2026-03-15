package com.saurabh.user.service;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import java.util.Properties;
@Service
public class KafkaProducerForWelcomeEmail {
    public static void sendWelcomeEmail(String email) {
        Properties props = new Properties();
        props.put("bootstrap.servers","localhost:9092");
        props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer<String,String> producer = new KafkaProducer<>(props);
        try
        {
            String topic = "EmailTopic";
            ProducerRecord<String,String> record = new ProducerRecord<>(topic,email);
            producer.send(record);
            System.out.println("Email Sent to the kafka topic"+ topic);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            producer.close();
        }
    }
}
