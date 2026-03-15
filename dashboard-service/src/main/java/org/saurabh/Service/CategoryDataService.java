package org.saurabh.Service;

import com.saurabh.userDB.entity.Categories;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.saurabh.Entity.CatCount;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service

public class CategoryDataService {
    public static void kTableFromCategoryDataWithoutCount(){
        log.info("Defining Props");
        Properties props = new Properties();
//        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.JsonSerializer");

        log.info("Inside Category without count consumer and about to form KTable");
        StreamsBuilder builder = new StreamsBuilder();
        log.warn("I have added a dependency of database-service here.If any error occurs,check Database service");
        //KTable Name = CategoryTable
        KTable<String, Categories> CategoryTable = builder.table("CategoryDataWithoutCount", Consumed.with(Serdes.String(),Serdes.serdeFrom(Categories.class)));
        log.info("KTable Formed for Cat_Data without count");
        KafkaStreams streams = new KafkaStreams(builder.build(),props);
        //starting the stream
        streams.start();


    }




}
/*    public static void consumeEmailFromTopic()
    {

        consumer.subscribe(Collections.singleton(topic));
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    String email = record.value();
                    System.out.println("Sending welcome mail to "+email);
                    sendEmail(email);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the consumer when done
            consumer.close();
        }

    }*/