package org.saurabh.service;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionForWebSocketService {
    @Autowired
    StreamsBuilderFactoryBean streamsBuilderFactoryBean1;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SessionForWebSocketService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    public KTable<String,String> UserSessionKTable(StreamsBuilder streamsBuilder){
        KStream<String,String> userSessionStreamForWebsocket = streamsBuilder.stream("user-session-topic-for-websockets"
                , Consumed.with(Serdes.String(),Serdes.String()));
        return userSessionStreamForWebsocket.toTable(Materialized.as("UserSessionStoreForWebSocket"));
    }


    public boolean checkIfUserIsActive(String userId, String device) {
        KafkaStreams streams = streamsBuilderFactoryBean1.getKafkaStreams();
        ReadOnlyKeyValueStore<String, String> sessionDataStore = streams.store(StoreQueryParameters.fromNameAndType("UserSessionStoreForWebSocket", QueryableStoreTypes.keyValueStore()));
        String sessionId = "Session" + "-" + device;
        String key = userId + "-" + device;

        String isSessionThere = sessionDataStore.get(key);
        if(isSessionThere==null || isSessionThere=="0")
            return false;
        return true;
    }

    public void logUserOut(String userId, String device) {

        String sessionId = "Session" + "-" + device;
        String key = userId + "-" + device;
        kafkaTemplate.send("user-session-topic-for-websockets",key,"0");
        kafkaTemplate.send("user-session-topic",key,"0");




    }
}
