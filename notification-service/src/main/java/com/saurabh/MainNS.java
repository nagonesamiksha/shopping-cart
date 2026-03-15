package com.saurabh;


import com.saurabh.notification.service.KafkaServiceForWelcomeEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MainNS  {
    @Autowired
   KafkaServiceForWelcomeEmail kafkaServiceForWelcomeEmail;
    public static void main(String[] args)
    {
        SpringApplication.run(MainNS.class,args);
        KafkaServiceForWelcomeEmail.consumeEmailFromTopic();
        System.out.println("Hello Notification Service!");
    }
}