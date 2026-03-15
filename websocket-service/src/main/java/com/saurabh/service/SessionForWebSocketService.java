//package com.saurabh.service;
//
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.streams.KafkaStreams;
//import org.apache.kafka.streams.StoreQueryParameters;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.kstream.Consumed;
//import org.apache.kafka.streams.kstream.KStream;
//import org.apache.kafka.streams.kstream.KTable;
//import org.apache.kafka.streams.kstream.Materialized;
//import org.apache.kafka.streams.state.QueryableStoreTypes;
//import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.kafka.config.StreamsBuilderFactoryBean;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//@Slf4j
//@Service
//public class SessionForWebSocketService {
//    @Autowired
//    private StreamsBuilderFactoryBean streamsBuilderFactory;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    public SessionForWebSocketService(KafkaTemplate<String, String> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    @Bean
//    public KTable<String, String> UserSessionKTable(StreamsBuilder streamsBuilder) {
//        KStream<String, String> userSessionStreamForWebsocket = streamsBuilder.stream("user-session-topic-for-websockets",
//                Consumed.with(Serdes.String(), Serdes.String()));
//        return userSessionStreamForWebsocket.toTable(Materialized.as("UserSessionStoreForWebSocket"));
//    }
//
//    public boolean checkIfUserIsActive(String userId, String device) {
//        waitForStreamsRunning();
//        KafkaStreams streams = streamsBuilderFactory.getKafkaStreams();
//
//
//
//        ReadOnlyKeyValueStore<String, String> sessionDataStore = streams.store(StoreQueryParameters.fromNameAndType("UserSessionStoreForWebSocket", QueryableStoreTypes.keyValueStore()));
//        String sessionId = "Session" + "-" + device;
//        String key = userId + "-" + device;
//
//        String isSessionThere = sessionDataStore.get(key);
//        if (isSessionThere == null || isSessionThere.equals("0")) {
//            return false;
//        }
//        return true;
//    }
//
//
//// ...
//
//    private boolean waitForStreamsRunning() {
//        KafkaStreams streams = streamsBuilderFactory.getKafkaStreams();
//        if (streams == null) {
//            return false;
//        }
//
//        // Create a ScheduledExecutorService with a single thread
//        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//
//        // Create a Runnable task to periodically check the state
//        Runnable checkStateTask = () -> {
//            if (streams.state() == KafkaStreams.State.RUNNING) {
//                // Kafka Streams is in RUNNING state, shutdown the executor
//                executor.shutdown();
//            }
//        };
//
//        // Schedule the task to run with a fixed delay
//        long initialDelay = 0; // Start immediately
//        long pollingInterval = 5000;
//        executor.scheduleWithFixedDelay(checkStateTask, initialDelay, pollingInterval, TimeUnit.MILLISECONDS);
//
//        // Wait for Kafka Streams to be running or until the executor is shut down
//        try {
//            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        } catch (InterruptedException e) {
//            log.error("Interrupted while waiting for Kafka Streams to be running.", e);
//            Thread.currentThread().interrupt();
//        }
//
//        return streams.state() == KafkaStreams.State.RUNNING;
//    }
//
//
//    public void logUserOut(String userId, String device) {
//        String sessionId = "Session" + "-" + device;
//        String key = userId + "-" + device;
//        kafkaTemplate.send("user-session-topic-for-websockets", key, "0");
//        kafkaTemplate.send("user-session-topic", key, "0");
//    }
//}
