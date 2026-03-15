package com.saurabh.service;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j

public class ActorForUser extends AbstractActor {

    private final ServerWebSocket websocket;
    private final ProductService productService;

    @Autowired
    public ActorForUser(ServerWebSocket webSocket, ProductService productService) {
        this.websocket = webSocket;
        this.productService = productService;
    }




    public static Props getProps(ServerWebSocket webSocket, ProductService productService) {
        return Props.create(ActorForUser.class, () -> new ActorForUser(webSocket, productService));
    }




    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ObjectNode.class, message -> {
            System.out.println("********************************msg is**************************************** "+message);
            String request = message.get("BCM").asText();
            System.out.println("*****************************request is*************************************** "+request);
            switch (request) {
                case "GET_PRODUCT":
                    log.info("About to fetch the products from the Ktable");

                    JsonNode product = productService.getProductWithSimilarProducts(message.get("category").asText(),message.get("productId").asText(),message.get("userId").asText());
                    if (product != null) {
                        log.info("The products fetched are for the case MT2 "+product.toString());
                        websocket.writeTextMessage(product.toString());

                    } else {
                       log.info("Product fetching failed");
                    }
                    break;
                case "GET_PRODUCTS_BY_CATEGORY_LH" :
                    log.info("Getting Products By Low to high");
                    JsonNode productsListLH =productService.getProductsByCategoryLowToHigh(message.get("category").asText());
                    websocket.writeTextMessage(productsListLH.toString());
                    break;
                case "GET_PRODUCTS_BY_CATEGORY_HL" :
                    System.out.println("Getting Products By high to low");
                    JsonNode productsListHL =productService.getProductsByCategoryHighToLow(message.get("category").asText());
                    websocket.writeTextMessage(productsListHL.toString());
                    break;
                case "GET_PRODUCTS_BY_CATEGORY_P" :
                    System.out.println("Getting Products By high to low");
                    JsonNode productsListP = productService.getProductsByCategoryPopularity(message.get("category").asText());
                    websocket.writeTextMessage(productsListP.toString());
                    break;
                case "GET_PRODUCT_CART" :
                    System.out.println("Getting Products by user cart");
                    JsonNode cartProducts = productService.getCartProducts(message.get("userId").asText());
                    websocket.writeTextMessage(cartProducts.toString());
                    break;
                case "ADD_PRODUCT_TO_CART" :
                    System.out.println("Adding Product To Cart");
                    productService.addProductToCart(message.get("userId").asText(),message.get("productId").asText());
                    break;
                case "UPDATE_PRODUCT_IN_CART" :
                    System.out.println("Updating Product in Cart : "+message);
                    productService.updateProductInCart(message.get("userId").asText(),message.get("productId").asText(),message.get("updateValue").asText());
                    break;
                case "REMOVE_PRODUCT_IN_CART" :
                    System.out.println("Removing Product from Cart");
                    productService.removeProductFromCart(message.get("userId").asText(),message.get("productId").asText());

                    break;


                default:
                    System.out.println("Refresh Method Started");
                    boolean isUserHasHistory = productService.checkIfUserHasAnyHistory(message.get("userId").asText());
                    if (!isUserHasHistory) {
                        JsonNode response1 = productService.getCategories();
                        JsonNode featureProducts = productService.getFeaturedProducts(12);
                        ObjectMapper objectMapper = new ObjectMapper();

                        ObjectNode responseObject = objectMapper.createObjectNode();
                        responseObject.put("response1", response1.toString());
                        responseObject.put("featuredProducts", featureProducts.toString());

                        String response = responseObject.toString();

                        websocket.writeTextMessage(response);
                    }
                    else{
                        log.info("The User has certain History");
                        JsonNode responseLastViewed = productService.getLastViewedProducts(message.get("userId").asText());
                        log.info("Recently viewed products by the user " + responseLastViewed);
                        JsonNode responseSimilarProducts = productService.getSimilarProductsForUser(message.get("userId").asText());
                        log.info("Similar products for this user " + responseSimilarProducts);

                        ObjectMapper objectMapper = new ObjectMapper();
                        ObjectNode jsonResponse = objectMapper.createObjectNode();
                        jsonResponse.put("message", responseLastViewed.toString());
                        jsonResponse.put("similarProducts", responseSimilarProducts.toString());

                        String response = jsonResponse.toString();

                        websocket.writeTextMessage(response);


                    }
                    break;

            }
        }).build();
    }






    }


