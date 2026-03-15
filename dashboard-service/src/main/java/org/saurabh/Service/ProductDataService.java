package org.saurabh.Service;

import com.saurabh.userDB.entity.ProductDB;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
public class ProductDataService {
    public KTable<String,ProductDB> productTable;

    public static void formKTableFromProductData(){
        Properties props = new Properties();
        StreamsBuilder builderForProductData= new StreamsBuilder();
        log.info("Streams for ProductData has been formed");
        KTable<String, ProductDB> productTable = builderForProductData.table("ProductData", Consumed.with(Serdes.String(),Serdes.serdeFrom(ProductDB.class)));
        KafkaStreams streamsForProductData = new KafkaStreams(builderForProductData.build(),props);
        streamsForProductData.start();

    }
//
//    public void KTableForCategoryCount() {
//        Properties props = new Properties();
//        StreamsBuilder builderForCategoryCount= new StreamsBuilder();
//        log.info("Streams for CategoryCountTable has been created");
//        KTable<String,Integer> categoryCountTable = builderForCategoryCount.table("ProductData",);
//
//    }

    public void KTableForProductCount(){
        //it will be updated as per the user click


    }
}
