package com.saurabh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saurabh.userDB.service.ProducerForCategoryDataKTable;
import com.saurabh.userDB.service.ProducerForProductDataKTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class DBMain {

    @Autowired
    private ProducerForCategoryDataKTable producerForCategoryDataKTable;

    @Autowired
    private ProducerForProductDataKTable producerForProductDataKTable;

    public DBMain(ProducerForCategoryDataKTable producerForCategoryDataKTable, ProducerForProductDataKTable producerForProductDataKTable) {
        this.producerForCategoryDataKTable = producerForCategoryDataKTable;
        this.producerForProductDataKTable = producerForProductDataKTable;
    }

    public static void main(String[] args) {
        SpringApplication.run(DBMain.class, args);
    }

    @PostConstruct
    public void initialize() throws JsonProcessingException {
        System.out.println("About to run application DBMain");
        producerForCategoryDataKTable.sendAllCategoriesToKafka();
        producerForProductDataKTable.produceCategoryAndCount();
        producerForProductDataKTable.produceForProductData();
        System.out.println("Hello DBService!");
    }
}
