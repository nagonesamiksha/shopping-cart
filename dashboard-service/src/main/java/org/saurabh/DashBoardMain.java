package org.saurabh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DashBoardMain {
    public static void main(String[] args) {
        SpringApplication.run(DashBoardMain.class,args);
        System.out.println("Hello Dashboard!");
    }
}