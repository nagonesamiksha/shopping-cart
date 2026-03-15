package com.saurabh.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
@Slf4j
@Service
public class UserSessionStoreService {
    private final KafkaStreams streams;
    private final KafkaProducer<String, String> producer;
    public ReadOnlyKeyValueStore<String, String> userSessionStore;

    public UserSessionStoreService() {

        Properties producerProps = new Properties();
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(producerProps);


        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> userSessionsStream = builder.stream("user-session-topic", Consumed.with(Serdes.String(), Serdes.String()));
        userSessionsStream
                .groupByKey()   // DSL (Domain Specific Lang)
                .reduce((oldValue, newValue) -> newValue, Materialized.as("user-session-store"));

        Properties props = new Properties();
        // Configure Kafka Streams properties
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ktable-example");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

        streams = new KafkaStreams(builder.build(), props);
        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING && oldState == KafkaStreams.State.REBALANCING) {
                // Access your state store here, it's now in the RUNNING state, we should only acceess it while it is running state
                userSessionStore = streams.store(StoreQueryParameters.fromNameAndType("user-session-store", QueryableStoreTypes.keyValueStore()));
            }
        });


        streams.start();
    }

    public void close() {

        if (streams != null) {
            streams.close();
        }
        if (producer != null) {
            producer.close();
        }
    }

    public boolean  getSessionStatus(String userId, String device) {
        String sessionId = "Session" + "-" + device;
        String key = userId + "-" + device;
        while (userSessionStore == null) {
            try {
                Thread.sleep(100); // Sleep for a short time before checking again
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        String userStatus = userSessionStore.get(key);
        if(userStatus!= null && !userStatus.equals("0")) {
            System.out.println("****************************************User Already logged in wth same device****************************************************");
            return true;
        }
        else {
            System.out.println("*****************************************Unique User******************************************************");
            try {
                System.out.println("************************************Inside try catch**************************************");
                String value = sessionId;
                log.info("Received Session Id "+value);
                producer.send(new ProducerRecord<>("user-session-topic", key, value)).get();
                producer.send(new ProducerRecord<>("user-session-topic-for-websockets", key, value)).get();

                log.info(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }


    }

    public boolean updateStoreForLogout(String key, String value) {
        while (userSessionStore == null) {
            try {
                Thread.sleep(100); // Sleep for a short time before checking again
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        String userStatus = userSessionStore.get(key);
        if(userStatus== null || userStatus.equals("0"))
        {
            return false;
        }
        else {
            try {
                producer.send(new ProducerRecord<>("user-session-topic", key, "0")).get();
                producer.send(new ProducerRecord<>("user-session-topic-for-websockets", key, "0")).get();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public boolean checkStatusForWebsocket(String userId, String device) {
        // String sessionId = "Session" + "-" + device;
        log.info("Inside checkStatusForWebsocket for userId "+userId);
        String key = userId + "-" + device;
        log.info("Key formed for "+userId+" is "+key);
//        while (userSessionStore == null) {
//            try {
//                Thread.sleep(100); // Sleep for a short time before checking again
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
        //producer.send(new ProducerRecord<>("user-session-topic","dummyuser", "dummyuser-laptop")).get();
        String userStatus = userSessionStore.get(key);

        if(userStatus== null)
        {
            log.info("Inside condition of userStatus==null");
            return false;
        } else if (userStatus.equals("0")) {
            log.info("Inside condition of userStatus.equals(0)");
            return false;
        }
        log.info("surpassed both the false condition");
        return true;
    }

    public boolean logUserOffWebsocket(String key) {
        //This is dummy method
        return false;
    }
}
