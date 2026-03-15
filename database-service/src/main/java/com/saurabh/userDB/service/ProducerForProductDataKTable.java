package com.saurabh.userDB.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.StringValue;
import com.saurabh.userDB.entity.Products;
import com.saurabh.userDB.repository.CategoryRepository;
import com.saurabh.userDB.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProducerForProductDataKTable {
    private static final Logger log = LoggerFactory.getLogger(ProducerForProductDataKTable.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTemplate<String, Long> kafkaTemplate1;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoriesRepository;

    @Autowired
    ProductRepository proRepo;

    public ProducerForProductDataKTable(KafkaTemplate<String, String> kafkaTemplate, KafkaTemplate<String, Long> kafkaTemplate1, ProductRepository productRepository, CategoryRepository categoriesRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplate1 = kafkaTemplate1;

    }

    public void produceForProductData() throws JsonProcessingException {
        log.info("About to produce data into ProductData Kafka topic");
        List<Products> productsReceivedFromDB = proRepo.findAll();

        for (Products product : productsReceivedFromDB) {
            String productId = product.getProductId();
            String jsonProduct = new ObjectMapper().writeValueAsString(product);
            try {
                // Send the record
                kafkaTemplate.send("ProductData", productId, jsonProduct);
                log.info("Record sent to Kafka topic ProductData for productId: " + productId);
            } catch (Exception e) {
                log.error("Error sending record: {}", e.getMessage());
            }
        }
    }

    public void produceCategoryAndCount() {
        log.info("About to produce category id and count into CategoryIdCount Kafka topic");
        List<Products> productsReceivedFromDB = proRepo.findAll();
        Map<String, Long> categoryCountMap = new HashMap<>();

        // Calculate the category count based on product counts
        for (Products product : productsReceivedFromDB) {
            String categoryId = product.getCategoryId();
            Long productCount = product.getViewCount();
            log.info("Product category is "+categoryId+" and product count is "+productCount);
            // Update the category count in the map
            categoryCountMap.put(categoryId, categoryCountMap.getOrDefault(categoryId, 0L) + productCount);
        }

        // Produce category id and count to the Kafka topic
        for (Map.Entry<String, Long> entry : categoryCountMap.entrySet()) {
            String categoryId = entry.getKey();
            Long categoryCount = entry.getValue();

            try {
                // Check if categoryCount is a valid number before converting to string
                if (categoryCount != null) {
                    String S = categoryCount.toString();
                    log.info("The count in String is "+S);
                    kafkaTemplate.send("CategoryIdCount", categoryId, S);
                    log.info("For the Category " + categoryId + " The count is " + S);
                    log.info("******************************************************************************************");
                    log.info("Record sent to Kafka topic CategoryIdCount for categoryId: " + categoryId);
                } else {
                    log.warn("Skipping invalid categoryCount for categoryId: " + categoryId);
                }
            } catch (Exception e) {
                log.error("Error sending record: {}", e.getMessage());
            }
        }
    }
}
