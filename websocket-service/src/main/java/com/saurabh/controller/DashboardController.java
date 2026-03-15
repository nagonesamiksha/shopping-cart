package com.saurabh.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saurabh.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController

public class DashboardController {
    @Autowired
    private ProductService productService;

    @GetMapping("/user/dashboard")
    public ResponseEntity<JsonNode> getProducts(@RequestBody String userInfo) {
        try {
            log.info("Received user is " + userInfo);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(userInfo);
            String userId = jsonNode.get("userId").asText();
            String sessionId = jsonNode.get("sessionId").asText();
            String[] sessionIdParts = sessionId.split("-");
            String device = null;
            if (sessionIdParts.length == 2) {
                device = sessionIdParts[1];
            }
            log.info("Received userId: " + userId);
            log.info("Received sessionId: " + sessionId);
            log.info("Received device: " + device);
            ObjectNode jsonResponse = null;
            //boolean isUserHasHistory = false;
            boolean isUserHasHistory1 = productService.checkIfUserHasAnyHistory(userId);
            log.info("This user "+userId+" Has viewing history "+isUserHasHistory1);
            if (isUserHasHistory1){
                log.info("The User has certain History");
                JsonNode responseLastViewed = productService.getLastViewedProducts(userId);
                log.info("Recently viewed products by the user "+responseLastViewed);
                JsonNode responseSimilarProducts = productService.getSimilarProductsForUser(userId);
                log.info("Similar products for this user "+responseSimilarProducts);
                jsonResponse = objectMapper.createObjectNode();
                jsonResponse.put("message", responseLastViewed );
                jsonResponse.put("similarProducts",responseSimilarProducts);

            }

            if (!isUserHasHistory1) {
                String response = "Products fetched successfully for userId: " + userId;
                JsonNode response1 = productService.getCategories();
                JsonNode featureProducts = productService.getFeaturedProducts(12);


                jsonResponse = objectMapper.createObjectNode();
                jsonResponse.put("message", response1);
                jsonResponse.put("Featured Products",featureProducts);
            }
            return ResponseEntity.ok(jsonResponse);
        } catch (JsonMappingException e) {
            log.info("JsonMapping exception occurred");
        } catch (JsonProcessingException e) {
            log.info("JsonProcessing exception occurred");
        } catch (InterruptedException e) {
            log.info("Interruption exception by streams");
        }
        return null;
    }
}


