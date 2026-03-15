package com.saurabh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class HazelcastApplication {



    public static void main(String[] args) {

        SpringApplication.run(HazelcastApplication.class,args);
        //logger.info("==========================HC==Application==Started==========================");
        System.out.println("hello hazelcast");
    }
}



