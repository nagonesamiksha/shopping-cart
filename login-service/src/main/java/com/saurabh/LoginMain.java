package com.saurabh;

//import com.saurabh.service.KProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoginMain {
    //@Autowired
    //KProducer kProducer;
    public static void main(String[] args) {

            SpringApplication.run(LoginMain.class,args);
            System.out.println("Login Service Started");

    }
}