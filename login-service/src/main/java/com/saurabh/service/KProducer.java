//package com.saurabh.service;
//
//import com.saurabh.entity.UserRes;
//import org.apache.kafka.clients.producer.*;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.streams.*;
//import org.apache.kafka.streams.kstream.Consumed;
//import org.apache.kafka.streams.kstream.KStream;
//import org.apache.kafka.streams.kstream.Materialized;
//import org.apache.kafka.streams.state.QueryableStoreTypes;
//import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
//import org.springframework.stereotype.Service;
//
//import java.util.Properties;
//import java.util.concurrent.ExecutionException;
//
//@Service
//public class KProducer {
//
//    private final KafkaStreams streams;
//    private final KafkaProducer<String, String> producer;
//    public ReadOnlyKeyValueStore<String, String> userSessionStore;
//
//    public KProducer() {
//
//        Properties producerProps = new Properties();
//        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        producer = new KafkaProducer<>(producerProps);
//
//
//        StreamsBuilder builder = new StreamsBuilder();
//        KStream<String, String> userSessionsStream = builder.stream("user-session-topic", Consumed.with(Serdes.String(), Serdes.String()));
//        userSessionsStream
//                .groupByKey()   // DSL (Domain Specific Lang)
//                .reduce((oldValue, newValue) -> newValue, Materialized.as("user-session-store"));
//
//        Properties props = new Properties();
//        // Configure Kafka Streams properties
//        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ktable-example");
//        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
//
//        streams = new KafkaStreams(builder.build(), props);
//        streams.setStateListener((newState, oldState) -> {
//            if (newState == KafkaStreams.State.RUNNING && oldState == KafkaStreams.State.REBALANCING) {
//                // Access your state store here, it's now in the RUNNING state, we should only acceess it while it is running state
//                userSessionStore = streams.store(StoreQueryParameters.fromNameAndType("user-session-store", QueryableStoreTypes.keyValueStore()));
//            }
//        });
//
//
//        streams.start();
//    }
//
//    public void close() {
//
//        if (streams != null) {
//            streams.close();
//        }
//        if (producer != null) {
//            producer.close();
//        }
//    }
//
//    public UserRes getSessionStatus(String userid, String device) {
//
//        String sessionId = "Session" + "-" + device;
//        String key = userid + "-" + device;
//        while (userSessionStore == null) {
//            try {
//                Thread.sleep(100); // Sleep for a short time before checking again
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        //producer.send(new ProducerRecord<>("user-session-topic","dummyuser", "dummyuser-laptop")).get();
//        String sessionStatus = userSessionStore.get(key);
//        System.out.println("sessionStatus: " + sessionStatus);
//
//        if (sessionStatus != null && !sessionStatus.equals("0")) {
//            UserRes userRes = new UserRes();
//            userRes.setCode(204);
//            userRes.setSessionId(null);
//            userRes.setUserId(null);
//            return userRes;
//        } else {
//            try {
//                String value = sessionId;
//                producer.send(new ProducerRecord<>("user-session-topic", key, value)).get();
//                UserRes userRes = new UserRes();
//                userRes.setCode(200);
//                userRes.setUserId(userid);
//                userRes.setSessionId(sessionId);
//                return userRes;
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                UserRes userRes = new UserRes();
//                userRes.setCode(0);
//                return userRes;
//            }
//        }
//    }
//
//    public int logUserOut(String userid, String sessionid) {
//        String device = extractDeviceFromSessionId(sessionid);
//        if (device == null) {
//            return 199;
//        }
//        String key = userid + "-" + device;
//        try {
//            producer.send(new ProducerRecord<>("user-session-topic", key, "0")).get();
//            return 200;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 199;
//        }
//    }
//
//    public static String extractDeviceFromSessionId(String sessionId) {
//        int index = sessionId.indexOf("-");
//        if (index != -1) {
//            return sessionId.substring(index + 1);
//        } else {
//            return null;
//        }
//
//    }
//
//    public boolean getStatusForWebSocket(String key) {
//        while (userSessionStore == null) {
//            try {
//                Thread.sleep(100); // Sleep for a short time before checking again
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        //producer.send(new ProducerRecord<>("user-session-topic","dummyuser", "dummyuser-laptop")).get();
//        String sessionStatus = userSessionStore.get(key);
//        System.out.println("sessionStatus: " + sessionStatus);
//
//        if (sessionStatus != null && !sessionStatus.equals("0")) {
//            return true;
//        }
//        return false;
//
//    }
//}
//
//
//
//
//
//
///*package com.saurabh.service;
//
//import com.saurabh.entity.UserRes;
//import org.apache.kafka.clients.producer.KafkaProducer;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.common.serialization.Serdes;
//import org.apache.kafka.streams.KafkaStreams;
//import org.apache.kafka.streams.StoreQueryParameters;
//import org.apache.kafka.streams.StreamsBuilder;
//import org.apache.kafka.streams.StreamsConfig;
//import org.apache.kafka.streams.kstream.Consumed;
//import org.apache.kafka.streams.kstream.KStream;
//import org.apache.kafka.streams.kstream.Materialized;
//import org.apache.kafka.streams.state.QueryableStoreTypes;
//import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
//import org.springframework.stereotype.Service;
//
//import java.util.Properties;
//
//@Service
//public class KProducer {
//    private final KafkaStreams streams;
//    private final KafkaProducer<String, String> producer;
//    private ReadOnlyKeyValueStore<String, String> userSessionStore;
//
//    public KProducer() {
//
//        Properties producerProps = new Properties();
//        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        producer = new KafkaProducer<>(producerProps);
//
//
//
//        StreamsBuilder builder = new StreamsBuilder();
//        KStream<String, String> userSessionsStream = builder.stream("user-session-topic", Consumed.with(Serdes.String(), Serdes.String()));
//        userSessionsStream
//                .groupByKey()
//                .reduce((oldValue, newValue) -> newValue, Materialized.as("user-session-store"));
//
//        Properties props = new Properties();
//        // Configure Kafka Streams properties
//        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ktable-example");
//        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
//
//        streams = new KafkaStreams(builder.build(), props);
//        streams.setStateListener((newState, oldState) -> {
//            if (newState == KafkaStreams.State.RUNNING && oldState == KafkaStreams.State.REBALANCING) {
//                // Access your state store here, it's now in the RUNNING state
//                userSessionStore = streams.store(StoreQueryParameters.fromNameAndType("user-session-store", QueryableStoreTypes.keyValueStore()));
//            }
//        });
//        streams.start();
//        while (streams.state()!=KafkaStreams.State.RUNNING)
//            try {
//                Thread.sleep(1000); // Sleep for a short time before checking again
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        // Start the KafkaStreams application asynchronously
//
//
//
//
////        while (streams.state()!=KafkaStreams.State.RUNNING)
////        try {
////            Thread.sleep(10000); // Sleep for a short time before checking again
////        } catch (InterruptedException e) {
////            throw new RuntimeException(e);
////        }
//
//    }
//
//
//    public void close() {
//        // Close the Kafka Streams application and the Kafka Producer
//        if (streams != null) {
//            streams.close();
//        }
//        if (producer != null) {
//            producer.close();
//        }
//    }
//
//    public UserRes getSessionStatus(String userid, String device) {
//        String sessionId = "Session"+ "-" +device;
//        String key = userid+"-"+device;
//        String sessionStatus = userSessionStore.get(key);
//        System.out.println("sessionStatus"+sessionStatus);
//
//        if (sessionStatus != null && !sessionStatus.equals("0")) {
//            UserRes userRes = new UserRes();
////            System.out.println(userRes.getSessionId());
//
//            userRes.setCode(204);
//            userRes.setSessionId(sessionStatus);
//            userRes.setUserId(userid);
//            return userRes;
//        } else {
//            try {
//                //String key = userid+ device;
//                String value = sessionId;
//                System.out.println("Inside the try block");
//                producer.send(new ProducerRecord<>("user-session-topic", key, value));
//                producer.flush();
//                System.out.println("After flush");
//                UserRes userRes = new UserRes();
//                userRes.setCode(200);
//                userRes.setUserId(userid);
//                userRes.setSessionId(sessionId);
//                return userRes;
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                UserRes userRes = new UserRes();
//                userRes.setCode(0);
//                return userRes;
//            }
//        }
//    }
//    public int logUserOut(String userid, String sessionid) {
//        String device = extractDeviceFromSessionId(sessionid);
//        if (device == null) {
//            return 199;
//        }
//        String key = userid + "-" + device;
//        try {
//            producer.send(new ProducerRecord<>("user-session-topic", key, "0"));
//            return 200;
//        } catch (Exception e) {
//            return 199;
//        }
//    }
//    public static String extractDeviceFromSessionId(String sessionId) {
//        int index = sessionId.indexOf("-");
//        if (index != -1) {
//            return sessionId.substring(index + 1);
//        } else {
//            return null;
//        }
//    }
//}
//
//*/
//
//
//
//
//
//
