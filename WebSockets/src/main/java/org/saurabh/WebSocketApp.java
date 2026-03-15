package org.saurabh;

import javax.annotation.PostConstruct;

import org.saurabh.Entity.HeartbeatDetails;
import org.saurabh.service.MapServiceForTimeKeeping;
import org.saurabh.service.WebSocketStartService;
import org.saurabh.service.WebSocketVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vertx.core.Vertx;
import org.springframework.kafka.annotation.EnableKafkaStreams;

import java.util.HashMap;

@EnableKafkaStreams
@SpringBootApplication
public class WebSocketApp {
    //private final WebSocketVerticle webSocketVerticle;

//    @Autowired
//    static
//    MapServiceForTimeKeeping mapServiceForTimeKeeping;

    public static void main(String[] args) {


        SpringApplication.run(WebSocketApp.class, args);
        new WebSocketStartService().start();
        new MapServiceForTimeKeeping(new HashMap<>()).checkStatusPeriodically();
    }



}