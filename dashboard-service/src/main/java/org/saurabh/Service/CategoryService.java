package org.saurabh.Service;

import com.saurabh.userDB.entity.Categories;
import com.saurabh.userDB.entity.ProductDB;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@KafkaListener()
public class CategoryService {
//
//    @Bean
//    public KTable<String, Categories> CategoryKTable (StreamsBuilder streamsBuilder){
//        KStream<String,Categories> categoryStreams= streamsBuilder.stream("CategoryData",Consumed.with(Serdes.String(),Serdes.serdeFrom(Categories.class)));
//        return categoryStreams.toTable();
//
//
//
//
//    }



}

