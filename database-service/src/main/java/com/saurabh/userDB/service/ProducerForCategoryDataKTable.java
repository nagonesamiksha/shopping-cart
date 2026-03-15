package com.saurabh.userDB.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.userDB.entity.Categories;
import com.saurabh.userDB.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class ProducerForCategoryDataKTable {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ProducerForCategoryDataKTable(KafkaTemplate<String, String> kafkaTemplate, CategoryRepository categoryRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.categoryRepository = categoryRepository;
    }
    @Autowired
    CategoryRepository catRepo;

    public void sendAllCategoriesToKafka() throws JsonProcessingException {
        List<Categories> categoriesList = catRepo.findAll();
        for(Categories category: categoriesList){
            System.out.println("****************************************************************** "+category);
        }
        System.out.println("****************************************About to send category info to the topic*******************************************************");
        for (Categories category : categoriesList) {
            // Send each category to the Kafka topic with the category ID as the key
            log.info("Sending Category to the topic "+category);
            String jsonCategory = new ObjectMapper().writeValueAsString(category);
            log.info("The category which I am about to send is "+jsonCategory);

            kafkaTemplate.send("CategoryData", category.getCategoryId(), jsonCategory);
        }
    }
}
