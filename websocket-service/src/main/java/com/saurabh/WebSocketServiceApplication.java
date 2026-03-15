package com.saurabh;

import javax.annotation.PostConstruct;

import com.saurabh.service.WebSocketVerticle;
import com.saurabh.service.WebSocketVerticles2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


import io.vertx.core.Vertx;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@EnableKafkaStreams
@SpringBootApplication
public class WebSocketServiceApplication {
    private final WebSocketVerticle webSocketVerticle;

    @Autowired
    WebSocketVerticles2 webSocketVerticle2;


    public WebSocketServiceApplication(WebSocketVerticle webSocketVerticle) {
        this.webSocketVerticle = webSocketVerticle;
    }

    public static void main(String[] args) {
        SpringApplication.run(WebSocketServiceApplication.class, args);
    }

    @Bean
    Vertx vertx() {
        return Vertx.vertx();
    }

    @PostConstruct
    public void deployVerticles() {
        // Deploy the WebSocketVerticle
        Vertx.vertx().deployVerticle(webSocketVerticle2);
    }
}