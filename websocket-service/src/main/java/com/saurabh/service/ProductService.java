package com.saurabh.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saurabh.entity.CartData;
import com.saurabh.entity.ProductDB1;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;




@Slf4j
@Service
@Configuration




public class ProductService {
    @Autowired
    StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    private final ObjectMapper objectMapper = new ObjectMapper();




    @Bean
    public KTable<String, String> ProductKTable(StreamsBuilder streamsBuilder) {
        KStream<String, String> productDataStream = streamsBuilder
                .stream("ProductData", Consumed.with(Serdes.String(), Serdes.String()));
        return productDataStream.toTable(Materialized.as("ProductDataStore"));
    }

    @Bean
    public KTable<String, String> CategoryKTable(StreamsBuilder streamsBuilder) {
        KStream<String, String> categoryStreams = streamsBuilder.stream("CategoryData", Consumed.with(Serdes.String(), new JsonSerde<>()));
        return categoryStreams.toTable(Materialized.as("CategoryDataStore"));

    }

    @Bean
    public KTable<String, String> CategoryIntialCountFromDB(StreamsBuilder streamsBuilder) {
        KStream<String, String> categoryCountFromDBStream = streamsBuilder.stream("CategoryIdCount", Consumed.with(Serdes.String(), Serdes.String()));

        return categoryCountFromDBStream.toTable(Materialized.as("CategoryCountFromDB"));
    }


    @Bean
    public KTable<String, String> ProductCountKTable(StreamsBuilder streamsBuilder) {

        KStream<String, String> productCountStream = streamsBuilder
                .stream("ProductCountTopic", Consumed.with(Serdes.String(), Serdes.String()));
        return productCountStream.groupByKey().reduce((oldValue, newValue) -> String.valueOf(Integer.parseInt(oldValue) + Integer.parseInt(newValue)),
                Materialized.as("ProductCountStore"));
    }

    @Bean
    public KTable<String, String> CategoryCountKTable(StreamsBuilder streamsBuilder) {
        KStream<String, String> categoryCountStream = streamsBuilder.stream(
                "CategoryCountTopic", Consumed.with(Serdes.String(), Serdes.String())
        );
        return categoryCountStream.groupByKey().reduce((oldValue, newValue) -> String.valueOf(Integer.parseInt(oldValue) + Integer.parseInt(newValue)),
                Materialized.as("CategoryCountStore")
        );
    }



    //when app started populate all 4KTables, cat count pan update kara. jpa madhu data ghya ani 4 hi table update karaaaa.
    public JsonNode getCartProducts(String userId) {
        try {
            waitForStreams();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        log.info("Inside method getCartProducts");
        Map<String, ArrayList<CartData>> cartDataMap = CartDataMapSingleton.getInstance().getCartDataMap();
        log.info("Got the singleton instance for cartDataMap");
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode resultJson = objectMapper.createObjectNode();

        // Create an array to store cart products
        ArrayNode cartProductsArray = objectMapper.createArrayNode();

        // Calculate the total price and count of products in the cart
        int totalPrice = 0;
        int productsCartCount = 0;
        ArrayList<CartData> userCart = cartDataMap.get(userId);
        log.info("CartData for "+userId+" is "+userCart);
        for (CartData cartData : userCart) {
            String productId = cartData.getProductId();
            String count = cartData.getCartQty();
            log.info("For the productId "+productId+" we are Qering the productDataStore");
            String productInfo = productDataStore.get(productId);
            log.info("for productId "+productId+" we got the produtInfo "+productInfo);
            String name =null;
            long price = 0L;
            try {
                JsonNode product = objectMapper.readTree(productInfo);
                name = product.get("productName").asText();
                price = product.get("productPrice").asLong();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            if (productInfo != null) {
                ObjectNode cartProduct = objectMapper.createObjectNode();
                cartProduct.put("prodId", productId);
                cartProduct.put("prodName", name);
                cartProduct.put("price", price);
                cartProduct.put("cartQty", cartData.getCartQty());

                cartProductsArray.add(cartProduct);
                totalPrice += price * Long.valueOf(cartData.getCartQty()) ;
                productsCartCount++;
            }
        }
        // Add the cartProducts array to the result JSON
        resultJson.set("cartProducts", cartProductsArray);

        // Add total values to the result JSON
        resultJson.put("ProductsCartCount", productsCartCount);
        resultJson.put("totalPrice", totalPrice);



        return resultJson;
    }


    public void addProductToCart(String userId, String prodId) {
        // Get the user's cart for the given userId


        Map<String, ArrayList<CartData>> cartDataMap = CartDataMapSingleton.getInstance().getCartDataMap();
        log.info("We got the singleton instance for cartDataMap");
        ArrayList<CartData> userCart = cartDataMap.get(userId);
        log.info("We got the userCart for the user "+userId+" which has "+userCart);
        // Check if the user's cart exists and if it contains the prodId
        if (userCart != null) {
            log.info("Usercart is not null");
            boolean prodIdAlreadyExists = false;

            for (CartData cartData : userCart) {
                log.info("Inside the iteration of CartData");
                String id = cartData.getProductId();
                log.info("ProductId is "+id);

                if (id.equals(prodId)){
                    log.info("Product is already there in the cart,we need to update the count");
                    // The prodId already exists in the cart, you can perform any additional logic here
                    prodIdAlreadyExists = true;
                    updateProductInCart(userId,id,"1");
                    break;
                }
            }

            if (!prodIdAlreadyExists) {
                log.info("The product is new");
                // If the prodId doesn't exist in the cart, add it
                CartData cartData = new CartData(prodId,"1");
                log.info("Added cartData is "+cartData);
                userCart.add(cartData);
            }
        } else {
            log.info("If the user's cart doesn't exist, create a new cart and add the prodId");
            ArrayList<CartData> newCart = new ArrayList<>();
            log.info("we created a new cart");
            CartData cartData = new CartData(prodId,"1");
            log.info("Added data is "+cartData);
            newCart.add(cartData);
            cartDataMap.put(userId, newCart);
        }
    }


    public void updateProductInCart(String userId, String productId, String updateValue) {
        // Get the user's cart for the given userId
        Map<String, ArrayList<CartData>> cartDataMap = CartDataMapSingleton.getInstance().getCartDataMap();
        log.info("We got the singleton instance for cartDataMap");
        log.info("UserId is "+userId);
        ArrayList<CartData> userCart = cartDataMap.get(userId);
        log.info("We got the userCart "+userCart);
        if (userCart == null) {
            // If the user's cart doesn't exist, create a new cart
            userCart = new ArrayList<>();
            userCart.add(new CartData(productId,"1"));
            log.info("Product added");
            cartDataMap.put(userId, userCart);
            log.info("Printing cartDataMap after adding a new cart:");
            for (Map.Entry<String, ArrayList<CartData>> entry : cartDataMap.entrySet()) {
                String mapUserId = entry.getKey();
                ArrayList<CartData> cartDataList = entry.getValue();

                log.info("UserId: " + mapUserId);
                for (CartData cartData : cartDataList) {
                    log.info("ProductId: " + cartData.getProductId() + ", CartQty: " + cartData.getCartQty());
                }
            }
        }
        if (userCart != null) {
            // Find the CartData object with the matching productId
            for (CartData cartData : userCart) {
                String id = cartData.getProductId();

                if (id.equals(productId)) {
                    // Update the count property based on the updateValue
                    int count = Integer.parseInt(cartData.getCartQty()); // Get the current count
                    int update = Integer.parseInt(updateValue);
                    log.info("Intial count is "+count);
                    log.info("Updated count is "+update);
                    // Ensure that count remains non-negative
                    if (count + update >= 0) {
                        cartData.setCartQty(String.valueOf(count + update));

                    }
                    log.info("Printing cartDataMap after adding a new cart:");
                    for (Map.Entry<String, ArrayList<CartData>> entry : cartDataMap.entrySet()) {
                        String mapUserId = entry.getKey();
                        ArrayList<CartData> cartDataList = entry.getValue();

                        log.info("UserId: " + mapUserId);
                        for (CartData cartData1 : cartDataList) {
                            log.info("ProductId: " + cartData1.getProductId() + ", CartQty: " + cartData1.getCartQty());
                        }
                    }

                    // Exit the loop after updating the matching CartData
                    break;
                }
            }
        }
    }


    public void removeProductFromCart(String userId, String productId) {
        // Get the user's cart for the given userId
        log.info("We are inside RemoveProductFrom Cart method");
        Map<String, ArrayList<CartData>> cartDataMap = CartDataMapSingleton.getInstance().getCartDataMap();
        log.info("We got the singleton cartDataMap");
        ArrayList<CartData> userCart = cartDataMap.get(userId);
        log.info("for giver userId "+userId+" usercart we got is "+userCart);
        if (userCart != null) {
            log.info("userCart is not null");
            CartData cartDataToRemove = null;
            log.info("Iterating through usercart");
            for (CartData cartData : userCart) {
                String id = cartData.getProductId();
                log.info("Product id is "+id);

                if (id.equals(productId)) {
                    log.info("We got the desired productId");
                    // Store the CartData object to be removed
                    cartDataToRemove = cartData;
                    log.info("CartData to remove is "+cartDataToRemove);
                    break; // Exit the loop after finding the matching CartData
                }
            }

            if (cartDataToRemove != null) {
                log.info("Remove the CartData object from the user's cart");
                userCart.remove(cartDataToRemove);
            }
        }
    }

    public JsonNode getCategories() throws InterruptedException {
        waitForStreams();
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, String> categoryCountStore = streams.store(StoreQueryParameters.fromNameAndType("CategoryCountStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> categoryDataStore = streams.store(StoreQueryParameters.fromNameAndType("CategoryDataStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> categoryCountStoreFromDB = streams.store(StoreQueryParameters.fromNameAndType("CategoryCountFromDB", QueryableStoreTypes.keyValueStore()));

        ObjectMapper objectMapper = new ObjectMapper();

        // Create a JSON array to store category data
        ArrayNode categoryDataArray = objectMapper.createArrayNode();

        // Create a list to store category data
        List<JsonNode> categoryDataList = new ArrayList<>();

        // Iterate through the keys of categoryDataStore
        KeyValueIterator<String, String> categoryDataIterator = categoryDataStore.all();
        while (categoryDataIterator.hasNext()) {
            KeyValue<String, String> entry = categoryDataIterator.next();
            String category = entry.key;
            log.info("The key I am iterating is " + category);
            String categoryInfo = entry.value;
            log.info("The category info of the same is " + categoryInfo);

            // Retrieve counts from categoryCountStore and categoryCountStoreFromDB
            String countFromStore = categoryCountStore.get(category);
            log.info("For CategoryId= " + category + " count from store is " + countFromStore);
            String countFromStoreFromDB = categoryCountStoreFromDB.get(category);
            log.info("For CategoryId= " + category + " count from DB without replacing colun is " + countFromStoreFromDB);
            String countFromDB = countFromStoreFromDB.replace("\"", "");
            log.info("For CategoryId= " + category + " count from DB after replacing colun is " + countFromDB);

            // Parse counts as integers
            int count1 = 0;
            int count2 = 0;
            if (countFromStore != null) {
                count1 = Integer.parseInt(countFromStore);

            }
            if (countFromDB != null) {
                count2 = Integer.parseInt(countFromDB);
            }
            // Calculate the combined count
            int combinedCount = count1 + count2;
            log.info("Combined count of these " + combinedCount + " For the categoryId " + category);

            // Create a JSON object for the category
            ObjectNode categoryJson = objectMapper.createObjectNode();
            categoryJson.put("id", category);
            categoryJson.put("description", categoryInfo);
            categoryJson.put("combinedCount", combinedCount);

            // Add the JSON object to the list
            categoryDataList.add(categoryJson);
        }

        // Close the iterator
        categoryDataIterator.close();

        // Sort the list based on combined count in descending order
        categoryDataList.sort((a, b) -> b.get("combinedCount").asInt() - a.get("combinedCount").asInt());

        // Add the sorted JSON objects to the JSON array
        categoryDataList.forEach(categoryDataArray::add);

        // Return the JSON array as a JsonNode
        return categoryDataArray;
    }


    public List<ProductDB1> getAllProductsWithCounts() {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, ProductDB1> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> productViewCountStore = streams.store(StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore()));

        List<ProductDB1> productDataWithCount = new ArrayList<>();

        productDataStore.all().forEachRemaining(entry -> {
            ProductDB1 product = entry.value;
            String latestCount = productViewCountStore.get(product.getProductId());
            if (latestCount != null) {
                try {
                    Long latestCount1 = Long.parseLong(latestCount);
                    product.setViewCount(product.getViewCount() + latestCount1);
                    productDataWithCount.add(product);
                } catch (NumberFormatException e) {

                    log.error("Error parsing latestCount for product " + product.getProductId(), e);
                }
            }
        });
        productDataWithCount.sort(Comparator.comparing(ProductDB1::getViewCount).reversed());


        return productDataWithCount;
    }

    public JsonNode getFeaturedProducts(int n) {
        List<String> productList = new ArrayList<>();
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> latestProductViewCountStore = streams.store(StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore()));

        ObjectMapper objectMapper = new ObjectMapper();

        productDataStore.all().forEachRemaining(entry -> {
            String productJson = entry.value;

            try {
                // Parse the JSON string into a JsonNode
                JsonNode jsonNode = objectMapper.readTree(productJson);

                // Retrieve the latest count
                String productId = jsonNode.get("productId").asText();
                String latestCount = latestProductViewCountStore.get(productId);
                if (latestCount != null) {
                    long latestCount1 = Long.parseLong(latestCount);

                    // Update the viewCount field in the JSON
                    ((ObjectNode) jsonNode).put("viewCount", jsonNode.get("viewCount").asLong() + latestCount1);
                }

                // Convert the updated JsonNode back to a JSON string
                String updatedProductJson = objectMapper.writeValueAsString(jsonNode);

                productList.add(updatedProductJson);
            } catch (IOException e) {
                throw new RuntimeException("Error processing product: " + productJson, e);
            }
        });

        // Sort the productList based on combined counts in descending order
        productList.sort(Comparator.comparingLong(productJson -> {
            try {
                JsonNode jsonNode = objectMapper.readTree((String) productJson);
                return jsonNode.get("viewCount").asLong();
            } catch (IOException e) {
                throw new RuntimeException("Error parsing JSON: " + productJson, e);
            }
        }).reversed());

        // Get the first n products
        List<String> featuredProducts = productList.subList(0, Math.min(productList.size(), n));

        // Convert the List of JSON strings to a JSON array
        String jsonArray = "[" + String.join(",", featuredProducts) + "]";

        try {
            // Parse the JSON array into a JsonNode
            return objectMapper.readTree(jsonArray);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing JSON array: " + jsonArray, e);
        }

    }


    public boolean checkIfUserHasAnyHistory(String userId) {
        log.info("Inside checkIfUserHasAnyHistory method, userId received is " + userId);

        // Access the userProductMap from the singleton
        Map<String, Queue<String>> userProducts = UserProductMapSingleton.getInstance().getUserProductMap();
        userProducts.forEach((key,value)->{
            System.out.println("UserId : "+key);
            System.out.println("RecentlyViewedProducts : "+value);
        });
        //Queue<String> userProducts = userProductMapSingleton.getUserProductMap().get(userId);
        log.info("UserProducts are "+userProducts);
        if (userProducts != null && !userProducts.isEmpty()) {
            log.info("User has viewing history");
            return true;
        } else {
            log.info("User does not have viewing history");
            return false;
        }
    }


        @KafkaListener(topics = "RecentlyViewedProducts", groupId = "RecentlyViewedProductsConsumers")
        public void addInProductViewMap(String userId, String jsonValue) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(jsonValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            String productId = jsonNode.get("productId").asText();
            String userId1 = jsonNode.get("userId").asText();
            log.info("UserId received "+userId);
            // Get or create the user's queue of recently viewed products
            log.info("Product viewed by the user is: " + productId);

            Map<String, Queue<String>> userProductMap = UserProductMapSingleton.getInstance().getUserProductMap();

            Queue<String> recentlyViewedProducts = userProductMap.computeIfAbsent(userId1, k -> new LinkedList<>());

            // Ensure the queue size does not exceed 6
            while (recentlyViewedProducts.size() >= 6) {
                recentlyViewedProducts.poll(); // Remove the oldest product
            }

            // Add the newest product at the start of the queue
            recentlyViewedProducts.offer(productId);

            // updated queue
            log.info("Updated recently viewed products for user " + userId1 + ": " + recentlyViewedProducts);

            // size of the userProductMap
            log.info("The Size of the userProductMap is " + userProductMap.size());
        }





    public JsonNode getLastViewedProducts(String userId) {
        try {
            waitForStreams();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Wait for the streams is over");
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        log.info("**************We got the ProductData Store for last viewed operation*******************");
        ObjectMapper  objectMapper = new ObjectMapper();

        // JSON object to store the results
        ObjectNode resultJson = objectMapper.createObjectNode();

        Map<String, Queue<String>> userProductMap = UserProductMapSingleton.getInstance().getUserProductMap();

        // Retrieve the user's recently viewed products from the userProductMap (Queue)
        Queue<String> recentlyViewedProducts = userProductMap.get(userId);
        System.out.println("Recently viewed products by the user are "+recentlyViewedProducts);

        if (recentlyViewedProducts != null) {
            // Create a JSON array to store product details for this user
            ArrayNode productArray = objectMapper.createArrayNode();

             //Iterate through recently viewed products for this user
            /*for (String productId : recentlyViewedProducts) {
                // Retrieve product details from the productDataStore (KTable)
                String productDetails = productDataStore.get(productId);

                // Create a JSON object for this product
                ObjectNode productObject = objectMapper.createObjectNode();
                productObject.put("productDetails", productDetails);

                // Add the product object to the JSON array
                productArray.add(productObject);
            }*/
             //Convert the queue to a list
            List<String> recentlyViewedProductsList = new ArrayList<>(recentlyViewedProducts);

            // Iterate through the list in reverse order
            for (int i = recentlyViewedProductsList.size() - 1; i >= 0; i--) {
                String productId = recentlyViewedProductsList.get(i);

                // Retrieve product details from the productDataStore (KTable)
                String productDetails = productDataStore.get(productId);

                // Create a JSON object for this product
                ObjectNode productObject = objectMapper.createObjectNode();
                productObject.put("productDetails", productDetails);

                // Add the product object to the JSON array
                productArray.add(productObject);
            }


            // Add the user's recently viewed products with details to the result JSON object
            resultJson.set(userId, productArray);
        }

        // Return the result as a JSON node
        return resultJson;
    }

    public JsonNode getSimilarProductsForUser(String userId) {
        try {
            waitForStreams();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Wait for the streams is over");
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ObjectMapper objectMapper = new ObjectMapper();

        // JSON object to store the results
        ObjectNode resultJson = objectMapper.createObjectNode();
        Map<String, Queue<String>> userProductMap = UserProductMapSingleton.getInstance().getUserProductMap();
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));

        Queue<String> reversedProducts  = userProductMap.get(userId);

        List<String> productList = new ArrayList<>(reversedProducts);

        Collections.reverse(productList);


        Queue<String> recentlyViewedProducts = new LinkedList<>(productList);
        System.out.println("Recently viewed products by the user are " + recentlyViewedProducts);

        // Check if recentlyViewedProducts size is equal to 6 before proceeding
        if (recentlyViewedProducts.size() == 6) {
            log.info("The user has viewed 6 products");
            List<String> similarProducts = new ArrayList<>();

            ArrayNode dissimilarProductsArray = objectMapper.createArrayNode();

            for (String productId : recentlyViewedProducts) {
                // Extract the categoryId (first letter) from the productId
                String categoryId = productId.substring(0, 1);

                if (categoryId != null) {
                    // Call the method to get products by category popularity
                    JsonNode categoryProducts = null;
                    try {
                        categoryProducts = getProductsByCategoryPopularity(categoryId);
                        log.info("I am printing categoryProducts now");
                        log.info(String.valueOf(categoryProducts));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    String json = categoryProducts.toString();
                    JsonNode rootNode = null;
                    try {
                        rootNode = objectMapper.readTree(json);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    // Get the "products" array from the JSON
                    JsonNode productsArray = rootNode.get("products");
                    if (productsArray.isArray()) {
                        for (JsonNode product : productsArray) {
                            // Extract the "productId" field from the product JSON
                            String productId1 = product.get("productId").asText();

                            // Check if the productProductId is not in recentlyViewedProducts
                            if (!recentlyViewedProducts.contains(productId1)) {
                                // Add the dissimilar product to the array
                                dissimilarProductsArray.add(product);
                            }

                            // Check if we have found 6 dissimilar products
                            if (dissimilarProductsArray.size() == 6) {
                                break;
                            }
                        }
                    }
                }
            }

// Return the array of dissimilar products
            return dissimilarProductsArray;

        } else {
            //int n = recentlyViewedProducts.size();
            int m = recentlyViewedProducts.size();
            List<String> similarProducts = new ArrayList<>();

            // Iterate through the recently viewed products
            for (String productId : recentlyViewedProducts) {
                // Extract the categoryId (first letter) from the productId
                String categoryId = productId.substring(0, 1);

                if (categoryId != null) {
                    // Call the method to get products by category popularity
                    JsonNode categoryProducts = null;
                    try {
                        categoryProducts = getProductsByCategoryPopularity(categoryId);
                        log.info("I am printing categoryProducts now");
                        log.info(String.valueOf(categoryProducts));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    String json = categoryProducts.toString();
                    JsonNode rootNode = null;
                    try {
                        rootNode = objectMapper.readTree(json);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    // Get the "products" array from the JSON
                    JsonNode productsArray = rootNode.get("products");
                    if (productsArray.isArray()) {
                        for (JsonNode product : productsArray) {
                            // Extract the "productId" field from the product JSON
                            String productProductId = product.get("productId").asText();

                            // Check if the productProductId is not in recentlyViewedProducts
                            if (!recentlyViewedProducts.contains(productProductId)) {
                                // Return the first dissimilar product
                                String dissimilarProductJson = product.toString();
                                similarProducts.add(dissimilarProductJson);
                                break; // Stop searching for dissimilar products in this category
                            }
                        }
                    }
                }
            }

            // Fill remaining products with products from the same category as the first product
            if (similarProducts.size() < m) {
                // Get the categoryId of the first product in recentlyViewedProducts
                String firstProductId = recentlyViewedProducts.peek(); // Peek to get the first element without removing it
                String firstProductCategoryId = firstProductId.substring(0, 1);

                // Call the method to get products from the same category
                JsonNode sameCategoryProducts = null;
                try {
                    sameCategoryProducts = getProductsByCategoryPopularity(firstProductCategoryId);
                    log.info("I am printing sameCategoryProducts now");
                    log.info(String.valueOf(sameCategoryProducts));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                String json = sameCategoryProducts.toString();
                JsonNode rootNode = null;
                try {
                    rootNode = objectMapper.readTree(json);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                // Get the "products" array from the JSON
                JsonNode productsArray = rootNode.get("products");
                if (productsArray.isArray()) {
                    for (JsonNode product : productsArray) {
                        // Extract the "productId" field from the product JSON
                        String productProductId = product.get("productId").asText();

                        // Check if the productProductId is not in similarProducts
                        if (!similarProducts.contains(productProductId)) {
                            // Add the product to similarProducts
                            String productJson = product.toString();
                            similarProducts.add(productJson);

                            // If we have filled m products, break out of the loop
                            if (similarProducts.size() >= m) {
                                break;
                            }
                        }
                    }
                }
            }

            // Convert the list of similar products to a JSON array and add it to the result JSON
            ArrayNode similarProductsArray = objectMapper.createArrayNode();
            for (String product : similarProducts) {
                similarProductsArray.add(product);
            }
            resultJson.set("similarProducts", similarProductsArray);
        }





        return resultJson;
    }




    public JsonNode getProductsByCategoryLowToHigh(String category) throws InterruptedException {
        waitForStreams();
        log.info("Wait for the streams is over");
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        // Define the store names and types
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));

        // Create a list to store products of the specified category
        List<JsonNode> productsInCategory = new ArrayList<>();

        // Iterate through all products in the store
        productDataStore.all().forEachRemaining(entry -> {
            String productJson = entry.value;

            try {
                // Parse the JSON data for the product
                JsonNode productNode = objectMapper.readTree(productJson);

                // Check if the product belongs to the specified category
                if (category.equals(productNode.get("categoryId").asText())) {
                    productsInCategory.add(productNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Sort the products by price in ascending order
        productsInCategory.sort(Comparator.comparing(product -> product.get("productPrice").asInt()));

        // Create a JSON response
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode sortedProductsArray = objectMapper.createArrayNode();

        for (JsonNode product : productsInCategory) {
            sortedProductsArray.add(product);
        }

        response.put("products", sortedProductsArray);

        return response;
    }




    public JsonNode getProductWithSimilarProducts(String category, String productId, String userId) throws InterruptedException {
        log.info("Inside getProductWithSimilarProducts method");
        waitForStreams();
        log.info("Wait for the streams is over");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        List<JsonNode> productData = new ArrayList<>();
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        // Define the store names and types
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> productCountStore = streams.store(StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore()));
        log.info("We got the stores");

        // Query the store for the product data based on the productId
        String product = productDataStore.get(productId);
        log.info("The product asked by the user is " + product);

        if (product != null) {
            // Convert the ProductDB object to a JSON representation
            ObjectNode productNode = null;
            try {
                productNode = objectMapper.readValue(product, ObjectNode.class);
            } catch (JsonProcessingException e) {
                log.info("Json Processing exception");
            }
            productData.add(productNode);

            List<JsonNode> similarProducts = new ArrayList<>();
            KeyValueIterator<String, String> allProductsIterator = productDataStore.all();

            while (allProductsIterator.hasNext()) {
                KeyValue<String, String> entry = allProductsIterator.next();
                String p1 = entry.value;

                String categoryId1 = null;
                String productId1 = null;
                JsonNode similarProductNode = null;
                try {
                    // Parse the JSON string into a JsonNode
                    similarProductNode = objectMapper.readTree(p1);

                    // Extract productId and categoryId
                    productId1 = similarProductNode.get("productId").asText();
                    categoryId1 = similarProductNode.get("categoryId").asText();

                    // Now you have the productId and categoryId values
                    log.info("Product ID: " + productId1);
                    log.info("Category ID: " + categoryId1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                log.info("Category of the requested product is " + category);
                if (category.equals(categoryId1) && !productId.equals(productId1)) {
                    // Calculate the combined count (viewCount + count from ProductCountStore)
                    long viewCount = similarProductNode.get("viewCount").asLong();
                    log.info("Count from the productDataStore is " + viewCount);

                    // Get the count from ProductCountStore as a string
                    String countStr = productCountStore.get(productId1);
                    log.info("Count from productCountStore as string is " + countStr);

                    // Initialize count as 0 in case parsing fails
                    long count = 0L;

                    try {
                        // Attempt to parse the countStr as an Integer
                        int countInt = Integer.parseInt(countStr);
                        count = (long) countInt; // Convert to long
                    } catch (NumberFormatException e) {

                        log.error("Failed to parse count from productCountStore: " + countStr);
                    }

                    // Add the combined count as a property to the similar product node
                    ((ObjectNode) similarProductNode).put("combinedCount", viewCount + count);
                    similarProducts.add(similarProductNode);
                }
            }

            // Sort similar products by combined count in descending order
            similarProducts.sort(Comparator.comparing(p -> -p.get("combinedCount").asLong()));

            // Limit to the top 5 similar products
            similarProducts = similarProducts.stream().limit(5).collect(Collectors.toList());

            // Add similar products to the response
            objectNode.put("products", objectMapper.valueToTree(productData));
            objectNode.put("similarProducts", objectMapper.valueToTree(similarProducts));
        }

        return objectNode;
    }

    public boolean waitForStreams() throws InterruptedException {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        if (streams == null) {
            return false;
        }
        while (streams.state() != KafkaStreams.State.RUNNING) {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                log.info("We have an exception while handling streams");
                Thread.currentThread().interrupt();
            }
        }
        return streams.state() == KafkaStreams.State.RUNNING;
    }


    public JsonNode getProductsByCategoryHighToLow(String category) throws InterruptedException {
        waitForStreams();
        log.info("Wait for the streams is over");
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        // Define the store names and types
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));

        // Create a list to store products of the specified category
        List<JsonNode> productsInCategory = new ArrayList<>();

        // Iterate through all products in the store
        productDataStore.all().forEachRemaining(entry -> {
            String productJson = entry.value;

            try {
                // Parse the JSON data for the product
                JsonNode productNode = objectMapper.readTree(productJson);

                // Check if the product belongs to the specified category
                if (category.equals(productNode.get("categoryId").asText())) {
                    productsInCategory.add(productNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Sort the products by price in descending order (high to low)
        productsInCategory.sort((product1, product2) -> {
            int price1 = product1.get("productPrice").asInt();
            int price2 = product2.get("productPrice").asInt();
            return Integer.compare(price2, price1);
        });

        // Create a JSON response
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode sortedProductsArray = objectMapper.createArrayNode();

        for (JsonNode product : productsInCategory) {
            sortedProductsArray.add(product);
        }

        response.put("products", sortedProductsArray);

        return response;
    }

    public JsonNode getProductsByCategoryPopularity(String category) throws InterruptedException {
        waitForStreams();
        log.info("Wait for the streams is over");
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        // Define the store names and types
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> productCountStore = streams.store(StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore()));

        // Create a list to store products of the specified category
        List<JsonNode> productsInCategory = new ArrayList<>();

        // Iterate through all products in the store
        productDataStore.all().forEachRemaining(entry -> {
            String productJson = entry.value;

            try {
                // Parse the JSON data for the product
                JsonNode productNode = objectMapper.readTree(productJson);

                // Check if the product belongs to the specified category
                if (category.equals(productNode.get("categoryId").asText())) {
                    productsInCategory.add(productNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Calculate the combined count and sort the products by combinedCount in descending order (high to low)
        productsInCategory.sort((product1, product2) -> {
            int viewCount1 = product1.get("viewCount").asInt();
            int viewCount2 = product2.get("viewCount").asInt();

            // Get the count from ProductCountStore
            String productId1 = product1.get("productId").asText();
            String productId2 = product2.get("productId").asText();
            String countStr1 = productCountStore.get(productId1);
            String countStr2 = productCountStore.get(productId2);

            int count1 = 0;
            int count2 = 0;

            // Convert countStr to int if it exists
            if (countStr1 != null) {
                count1 = Integer.parseInt(countStr1);
            }

            if (countStr2 != null) {
                count2 = Integer.parseInt(countStr2);
            }

            // Calculate combinedCount for each product
            int combinedCount1 = viewCount1 + count1;
            int combinedCount2 = viewCount2 + count2;

            // Sort by combinedCount in descending order
            return Integer.compare(combinedCount2, combinedCount1);
        });

        // Create a JSON response
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode sortedProductsArray = objectMapper.createArrayNode();

        for (JsonNode product : productsInCategory) {
            sortedProductsArray.add(product);
        }

        response.put("products", sortedProductsArray);

        return response;
    }


}
/*import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.kafka.core.StreamsBuilderFactoryBean;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

// ...

public JsonNode getSimilarProductsForUser() {
    try {
        waitForStreams();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
    log.info("Wait for the streams is over");
    KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

    ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));

    ObjectMapper objectMapper = new ObjectMapper();

    // JSON object to store the results
    ObjectNode resultJson = objectMapper.createObjectNode();

    // Iterate through userProductMap to retrieve recently viewed products
    for (Map.Entry<String, LinkedHashMap<String, String>> entry : userProductMap.entrySet()) {
        String userId = entry.getKey();
        LinkedHashMap<String, String> recentlyViewedProducts = entry.getValue();

        // Create a JSON array to store similar products
        ArrayNode similarProductsArray = objectMapper.createArrayNode();

        // Check if the user has viewed at least 6 products
        if (recentlyViewedProducts.size() >= 6) {
            // User has viewed 6 or more products
            // Iterate through the categories in the recently viewed products
            Set<String> categories = new HashSet<>();
            for (String productId : recentlyViewedProducts.values()) {
                String productDetails = productDataStore.get(productId);
                // Extract and store the category of each product
                String category = getCategoryFromProductDetails(productDetails);
                categories.add(category);
            }

            // Iterate through the unique categories and find the top-ranked product for each
            for (String category : categories) {
                String topRankedProduct = null;
                int highestViewCount = -1;
                for (Map.Entry<String, String> productEntry : recentlyViewedProducts.entrySet()) {
                    String productId = productEntry.getKey();
                    String productDetails = productDataStore.get(productId);
                    String productCategory = getCategoryFromProductDetails(productDetails);

                    if (category.equals(productCategory) && !recentlyViewedProducts.containsValue(productId)) {
                        // Check view count for this product in the same category
                        int viewCount = 0;
                        for (String viewedProductId : recentlyViewedProducts.values()) {
                            if (productId.equals(viewedProductId)) {
                                viewCount++;
                            }
                        }
                        if (viewCount > highestViewCount) {
                            highestViewCount = viewCount;
                            topRankedProduct = productId;
                        }
                    }
                }

                // Add the top-ranked product to the similar products array
                if (topRankedProduct != null) {
                    similarProductsArray.add(topRankedProduct);
                }
            }
        } else {
            // User has viewed fewer than 6 products
            // Find the most recently viewed category
            String mostRecentlyViewedCategory = null;
            String mostRecentProductId = null;
            for (Map.Entry<String, String> productEntry : recentlyViewedProducts.entrySet()) {
                String productId = productEntry.getKey();
                String productDetails = productDataStore.get(productId);
                String productCategory = getCategoryFromProductDetails(productDetails);

                if (mostRecentlyViewedCategory == null || mostRecentlyViewedCategory.equals(productCategory)) {
                    mostRecentlyViewedCategory = productCategory;
                    mostRecentProductId = productId;
                }
            }

            // Add the top-ranked product of that category to the similar products array
            if (mostRecentProductId != null) {
                similarProductsArray.add(mostRecentProductId);
            }

            // Fill the remaining slots with products from the user's most recently viewed category
            for (int i = 0; i < 6 - similarProductsArray.size(); i++) {
                for (Map.Entry<String, String> productEntry : recentlyViewedProducts.entrySet()) {
                    String productId = productEntry.getKey();
                    String productDetails = productDataStore.get(productId);
                    String productCategory = getCategoryFromProductDetails(productDetails);

                    if (mostRecentlyViewedCategory.equals(productCategory) && !recentlyViewedProducts.containsValue(productId) && !similarProductsArray.contains(productId)) {
                        similarProductsArray.add(productId);
                        break;
                    }
                }
            }
        }

        // Add the similar products array to the result JSON object
        resultJson.set(userId, similarProductsArray);
    }

    // Return the result as a JSON node
    return resultJson;
}
*/


/*public JsonNode getProductsByCategoryPopularity(String category) throws InterruptedException {
        waitForStreams();
        log.info("Wait for the streams is over");
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        // Define the store names and types
        ReadOnlyKeyValueStore<String, String> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> productCountStore = streams.store(StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore()));

        // Create a list to store products of the specified category
        List<JsonNode> productsInCategory = new ArrayList<>();

        // Iterate through all products in the store
        productDataStore.all().forEachRemaining(entry -> {
            String productJson = entry.value;

            try {
                // Parse the JSON data for the product
                JsonNode productNode = objectMapper.readTree(productJson);

                // Check if the product belongs to the specified category
                if (category.equals(productNode.get("categoryId").asText())) {
                    productsInCategory.add(productNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Sort the products by viewCount in descending order (high to low)
        productsInCategory.sort((product1, product2) -> {
            int viewCount1 = product1.get("viewCount").asInt();
            int viewCount2 = product2.get("viewCount").asInt();
            return Integer.compare(viewCount2, viewCount1);
        });

        // Create a JSON response
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode sortedProductsArray = objectMapper.createArrayNode();

        for (JsonNode product : productsInCategory) {
            sortedProductsArray.add(product);
        }

        response.put("products", sortedProductsArray);

        return response;
    }*/


/* public List<CategoryData> getCategoryData() {
        waitForStreamsRunning();

        KafkaStreams kafkaStreams = streamsBuilderFactory.getKafkaStreams();

        ReadOnlyKeyValueStore<String, Category> categoryDataStore = kafkaStreams.store(
            StoreQueryParameters.fromNameAndType("category-data-store", QueryableStoreTypes.keyValueStore()));

        ReadOnlyKeyValueStore<String, String> viewCountStore = kafkaStreams.store(
            StoreQueryParameters.fromNameAndType("category-view-count-store", QueryableStoreTypes.keyValueStore()));

        List<CategoryData> categoryDataList = new ArrayList<>();

        categoryDataStore.all().forEachRemaining(entry -> {
            String categoryId = entry.key;
            Category category = entry.value;

            // Retrieve the view count from the view count store
            Long viewCount = Optional.ofNullable(viewCountStore.get(categoryId)).map(Long::parseLong).orElse(0L);

            categoryDataList.add(new CategoryData(category, viewCount));
        });

        categoryDataList.sort((data1, data2) -> Long.compare(data2.getViewCount(), data1.getViewCount()));

        // Return all categories
        return categoryDataList;
    }*/

/*    public List<ProductDB1> getSimilarProducts(List<ProductDB1> recentlyViewed, List<ProductDB1> productDataWithCount) {
        List<ProductDB1> similarProducts = new ArrayList<>();

        // Step 1: If there are exactly 6 recently viewed products, find similar products by category
        if (recentlyViewed.size() == 6) {
            for (ProductDB1 viewedProduct : recentlyViewed) {
                String categoryId = viewedProduct.getCategoryId();

                // Find products in the same category
                List<ProductDB1> categoryProducts = productDataWithCount.stream()
                        .filter(product -> categoryId.equals(product.getCategoryId()))
                        .collect(Collectors.toList());

                // Sort category products by some criteria (e.g., productCount) in descending order
                categoryProducts.sort(Comparator.comparingLong(ProductDB1::getViewCount).reversed());

                // Determine if the top product is in recently viewed list
                if (!categoryProducts.isEmpty()) {
                    ProductDB1 topProduct = categoryProducts.get(0);
                    if (recentlyViewed.contains(topProduct)) {
                        // If the top product is in recently viewed, find a different similar product
                        for (ProductDB1 product : categoryProducts) {
                            if (!recentlyViewed.contains(product)) {
                                similarProducts.add(product);
                                break;
                            }
                        }
                    } else {
                        // Otherwise, add the top product as the similar product
                        similarProducts.add(topProduct);
                    }
                }
            }
        }
        if (recentlyViewed.size() < 6) {
            int neededSimilarProducts = 6 - recentlyViewed.size();

            // Step 1: Find similar products for the available recently viewed products
            for (ProductDB1 viewedProduct : recentlyViewed) {
                String categoryId = viewedProduct.getCategoryId();

                // Find products in the same category
                List<ProductDB1> categoryProducts = productDataWithCount.stream()
                        .filter(product -> categoryId.equals(product.getCategoryId()))
                        .collect(Collectors.toList());

                // Sort category products by some criteria (e.g., productCount) in descending order
                categoryProducts.sort(Comparator.comparingLong(ProductDB1::getViewCount).reversed());

                // Determine if the top product is in recently viewed list
                if (!categoryProducts.isEmpty()) {
                    ProductDB1 topProduct = categoryProducts.get(0);
                    if (recentlyViewed.contains(topProduct)) {
                        // If the top product is in recently viewed, find a different similar product
                        for (ProductDB1 product : categoryProducts) {
                            if (!recentlyViewed.contains(product)) {
                                similarProducts.add(product);
                                break;
                            }
                        }
                    } else {
                        // Otherwise, add the top product as the similar product
                        similarProducts.add(topProduct);
                    }
                }
            }

            // Step 2: If more similar products are needed, add them based on the first product
            while (similarProducts.size() < 6 && !productDataWithCount.isEmpty()) {
                ProductDB1 firstProduct = productDataWithCount.get(0);
                productDataWithCount.remove(0);
                similarProducts.add(firstProduct);
            }

        }

        return similarProducts;
    }
*/


















/*
* import java.util.*;
import java.util.stream.Collectors;

public List<ProductDB> getSimilarProducts(List<ProductDB> recentlyViewed, List<ProductDB> productDataWithCount) {
    List<ProductDB> similarProducts = new ArrayList<>();

    // Step 1: If there are exactly 6 recently viewed products, find similar products by category
    if (recentlyViewed.size() == 6) {
        for (ProductDB viewedProduct : recentlyViewed) {
            String categoryId = viewedProduct.getCategoryId();

            // Find products in the same category
            List<ProductDB> categoryProducts = productDataWithCount.stream()
                    .filter(product -> categoryId.equals(product.getCategoryId()))
                    .collect(Collectors.toList());

            // Sort category products by some criteria (e.g., productCount) in descending order
            categoryProducts.sort(Comparator.comparingInt(ProductDB::getProductCount).reversed());

            // Determine if the top product is in recently viewed list
            if (!categoryProducts.isEmpty()) {
                ProductDB topProduct = categoryProducts.get(0);
                if (recentlyViewed.contains(topProduct)) {
                    // If the top product is in recently viewed, find a different similar product
                    for (ProductDB product : categoryProducts) {
                        if (!recentlyViewed.contains(product)) {
                            similarProducts.add(product);
                            break;
                        }
                    }
                } else {
                    // Otherwise, add the top product as the similar product
                    similarProducts.add(topProduct);
                }
            }
        }
    }

    // Step 2: If there are fewer than 6 recently viewed products, you can add more logic here
    if (recentlyViewed.size() < 6) {
        // Implement additional logic based on your requirements to find similar products.
        // You can consider other criteria such as product attributes, user preferences, etc.
    }

    return similarProducts;
}
*/

/*import java.util.*;
import java.util.stream.Collectors;

public List<ProductDB> getSimilarProducts(List<ProductDB> recentlyViewed, List<ProductDB> productDataWithCount) {
    List<ProductDB> similarProducts = new ArrayList<>();

    // Calculate how many similar products are needed based on the number of recently viewed products
    int neededSimilarProducts = 6 - recentlyViewed.size();

    // Step 1: Find similar products for the available recently viewed products
    for (ProductDB viewedProduct : recentlyViewed) {
        String categoryId = viewedProduct.getCategoryId();

        // Find products in the same category
        List<ProductDB> categoryProducts = productDataWithCount.stream()
                .filter(product -> categoryId.equals(product.getCategoryId()))
                .collect(Collectors.toList());

        // Sort category products by some criteria (e.g., productCount) in descending order
        categoryProducts.sort(Comparator.comparingInt(ProductDB::getProductCount).reversed());

        // Determine if the top product is in recently viewed list
        if (!categoryProducts.isEmpty()) {
            ProductDB topProduct = categoryProducts.get(0);
            if (recentlyViewed.contains(topProduct)) {
                // If the top product is in recently viewed, find a different similar product
                for (ProductDB product : categoryProducts) {
                    if (!recentlyViewed.contains(product)) {
                        similarProducts.add(product);
                        break;
                    }
                }
            } else {
                // Otherwise, add the top product as the similar product
                similarProducts.add(topProduct);
            }
        }
    }

    // Step 2: If more similar products are needed, add them based on the first product
    while (similarProducts.size() < 6 && !productDataWithCount.isEmpty()) {
        ProductDB firstProduct = productDataWithCount.get(0);
        productDataWithCount.remove(0);
        similarProducts.add(firstProduct);
    }

    return similarProducts;
}
*/



/*
* import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public List<ProductDB> getSimilarProducts(List<ProductDB> recentlyViewed, List<ProductDB> productDataWithCount) {
    List<ProductDB> similarProducts = new ArrayList<>();
    Set<String> viewedCategories = new HashSet<>();

    // Step 1: Add the top-ranked product of each category from the user's last viewed products
    for (ProductDB product : recentlyViewed) {
        if (similarProducts.size() >= 6) {
            break; // Exit loop if we have 6 similar products
        }
        String category = product.getCategoryId();
        if (!viewedCategories.contains(category)) {
            similarProducts.add(getTopRankedProduct(productDataWithCount, category));
            viewedCategories.add(category);
        }
    }

    // Step 2: If there are fewer than 6 similar products, fill the rest with products from the user's most recently viewed category
    if (similarProducts.size() < 6) {
        String lastViewedCategory = recentlyViewed.get(0).getCategoryId(); // Assuming the first item is the most recent
        for (ProductDB product : productDataWithCount) {
            if (similarProducts.size() >= 6) {
                break; // Exit loop if we have 6 similar products
            }
            String category = product.getCategoryId();
            if (category.equals(lastViewedCategory) && !recentlyViewed.contains(product)) {
                similarProducts.add(product);
            }
        }
    }

    return similarProducts;
}

private ProductDB getTopRankedProduct(List<ProductDB> productDataWithCount, String category) {
    // Assuming productDataWithCount is sorted by rank
    for (ProductDB product : productDataWithCount) {
        if (product.getCategoryId().equals(category)) {
            return product;
        }
    }
    return null; // Handle the case where no product of that category is found
}
*/

/*public List<ProductDB> getSimilarProducts(List<ProductDB> recentlyViewed, List<ProductDB> productDataWithCount) {
    Map<String, ProductDB> lastViewedCategories = new HashMap<>();
    List<ProductDB> similarProducts = new ArrayList<>();

    // Step 1: Create a map of last viewed products by category
    for (ProductDB product : recentlyViewed) {
        String category = extractCategory(product); // Implement a function to extract the category
        lastViewedCategories.put(category, product);
    }

    // Step 2: Find the top-ranked product in each category from productDataWithCount
    for (ProductDB product : productDataWithCount) {
        String category = extractCategory(product);
        if (!lastViewedCategories.containsKey(category)) {
            // Add the top-ranked product of the category to similarProducts
            similarProducts.add(product);
            lastViewedCategories.put(category, product);
        }
    }

    // Step 3: Fill the remaining slots with products from the last viewed category
    int remainingSlots = 6 - similarProducts.size();
    if (remainingSlots > 0) {
        ProductDB lastViewedProduct = lastViewedCategories.getOrDefault(
            extractCategory(recentlyViewed.get(0)), null
        );

        if (lastViewedProduct != null) {
            int lastViewedRank = Integer.parseInt(extractRank(lastViewedProduct));
            String lastViewedCategory = extractCategory(lastViewedProduct);

            for (int i = 1; i <= remainingSlots; i++) {
                int newRank = lastViewedRank + i;
                String newProductId = lastViewedCategory + String.format("%02d", newRank);

                for (ProductDB product : productDataWithCount) {
                    if (product.getProductId().equals(newProductId)) {
                        similarProducts.add(product);
                        break;
                    }
                }
            }
        }
    }

    return similarProducts;
}

// Implement these helper functions based on your data structure
private String extractCategory(ProductDB product) {
    // Extract the category from the product name or ID
    // Implement based on your data structure
}

private String extractRank(ProductDB product) {
    // Extract the rank from the product name or ID
    // Implement based on your data structure
}
*/

 /*for (int i = 0; i < newValueArray.length(); i++) {
                        aggregateArray.put(newValueArray.getJSONObject(i));
                    }
                    return aggregateArray.toString();*/
