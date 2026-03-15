package org.saurabh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.saurabh.Entity.Categories1;

import org.saurabh.Entity.ClickTracker;
import org.saurabh.Entity.ProductDB1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.json.*;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@Configuration

@Component


public class ProductService {
    @Autowired
    StreamsBuilderFactoryBean streamsBuilderFactoryBean;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public KTable<String, String> ProductKTable(StreamsBuilder streamsBuilder) {
        KStream<String,String> productDataStream = streamsBuilder
                .stream("ProductData", Consumed.with(Serdes.String(), new JsonSerde<>()));
        return productDataStream.toTable(Materialized.as("ProductDataStore"));
    }

    @Bean
    public KTable<String,String> CategoryKTable(StreamsBuilder streamsBuilder) {
        KStream<String,String> categoryStreams = streamsBuilder.stream("CategoryData", Consumed.with(Serdes.String(), new JsonSerde<>()));
        return categoryStreams.toTable(Materialized.as("CategoryDataStore"));

    }

    @Bean
    public KTable<String, String> CategoryCountFromDB(StreamsBuilder streamsBuilder) {
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



//    @Bean
//    public KTable<String, String> userRecentlyViewed(StreamsBuilder streamsBuilder) {
//
//        KGroupedStream<String, String> groupedStream = streamsBuilder
//                .stream("RecentlyViewedProducts",
//                        Consumed.with(Serdes.String(), new JsonSerde<>(String.class)))
//                .groupBy((key, value) -> key, Grouped.with(Serdes.String(), new JsonSerde<>(String.class)));
//
//        return groupedStream.aggregate(
//                // Initializer to create an empty JSON array
//                () -> "[]",
//                // Aggregator to merge newly viewed products with existing ones
//                (key, newValue, aggregate) -> {
//                    // Merge the JSON arrays
//                    // Assuming newValue and aggregate are JSON arrays as strings
//                    JSONArray newValueArray = new JSONArray(newValue);
//                    JSONArray aggregateArray = new JSONArray(aggregate);
//
//                    for (int i = newValueArray.length() - 1; i >= 0; i--) {
//                        aggregateArray.put(0, newValueArray.getJSONObject(i)); // Add at the beginning
//                    }
//                    return aggregateArray.toString();
//                },
//                // Materialized configuration to specify the state store name
//                Materialized.as("RecentlyViewedProductStore"));
//    }
//
//    @Bean
//    public KTable<String, String> cartProducts(StreamsBuilder streamsBuilder) {
//        KGroupedStream<String, String> groupedStream = streamsBuilder
//                .stream("AddedToCartProducts",
//                        Consumed.with(Serdes.String(), new JsonSerde<>(String.class)))
//                .groupBy((key, value) -> key, Grouped.with(Serdes.String(), new JsonSerde<>(String.class)));
//        return groupedStream.aggregate(
//                // Initializer to create an empty JSON array
//                () -> "[]",
//                // Aggregator to merge newly viewed products with existing ones
//                (key, newValue, aggregate) -> {
//                    // Merge the JSON arrays
//                    // Assuming newValue and aggregate are JSON arrays as strings
//                    JSONArray newValueArray = new JSONArray(newValue);
//                    JSONArray aggregateArray = new JSONArray(aggregate);
//                    for (int i = 0; i < newValueArray.length(); i++) {
//                        aggregateArray.put(newValueArray.getJSONObject(i));
//                    }
//                    return aggregateArray.toString();
//                },
//                // Materialized configuration to specify the state store name
//                Materialized.as("AddedToCartProductsStore"));
//
//    }
//
//    @Bean
//    KStream<String, ClickTracker> countProcessor(StreamsBuilder streamsBuilder) {
//        JsonSerde<ClickTracker> clickSerde = new JsonSerde<>(ClickTracker.class);
//        KStream<String, ClickTracker> clickEventsTracker = streamsBuilder.stream("ClickEventTopic", Consumed.with(Serdes.String(), clickSerde));
//        clickEventsTracker
//                .filter((key, clickEvent) -> clickEvent.getProductId() != null)
//                .mapValues(clickEvent -> "1")
//                .to("ProductViewCount", Produced.with(Serdes.String(), Serdes.String()));
//
//
//        clickEventsTracker
//                .filter((key, clickEvent) -> clickEvent.getCategoryId() != null)
//                .map((key, clickEvent) -> new KeyValue<>(clickEvent.getCategoryId(), "1"))
//                .to("CategoryViewCount", Produced.with(Serdes.String(), Serdes.String()));
//
//        return clickEventsTracker;
//
//    }
    //when app started populate all 4KTables, cat count pan update kara. jpa madhu data ghya ani 4 hi table update karaaaa.

    public List<CategoryDataWithCount> getCategories() {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, Long> categoryCountStore = streams.store(StoreQueryParameters.fromNameAndType("CategoryCountStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, Categories1> categoryDataStore = streams.store(StoreQueryParameters.fromNameAndType("CategoryDataStore", QueryableStoreTypes.keyValueStore()));

        List<CategoryDataWithCount> categoryData = new ArrayList<>();
        categoryDataStore.all().forEachRemaining(entry -> {
            String categoryId = entry.key;
            Categories1 category = entry.value;

            try {
                Long count = Optional.ofNullable(categoryCountStore.get(categoryId)).orElse(0L);
                categoryData.add(new CategoryDataWithCount(category, count));
            } catch (NumberFormatException e) {
                e.printStackTrace();

            }
        });

        // Sort the categoryData list by view count in descending order
        categoryData.sort((s1, s2) -> Long.compare(s2.getViewCount(), s1.getViewCount()));

        return categoryData;
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

    public List<ProductDB1> getFeaturedProducts(int n) {
        List<ProductDB1> productList = new ArrayList<>();
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, ProductDB1> productDataStore = streams.store(StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore()));
        ReadOnlyKeyValueStore<String, String> latestProductViewCountStore = streams.store(StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore()));

        productDataStore.all().forEachRemaining(entry -> {
            ProductDB1 product = entry.value;
            String latestCount = latestProductViewCountStore.get(product.getProductId());
            if (latestCount != null) {
                long latestCount1 = Long.parseLong(latestCount);
                product.setViewCount(product.getViewCount() + latestCount1);
            }
            productList.add(product);
        });
        //sorting
        productList.sort((p1, p2) -> Long.compare(p2.getViewCount(), p1.getViewCount()));

        return productList.subList(0, Math.min(productList.size(), n));
    }

    public List<ProductDB1> getLastViewedProducts(String userId) {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, ProductDB1> recentlyViewedProductStore = streams.store(StoreQueryParameters.fromNameAndType("RecentlyViewedProductStore", QueryableStoreTypes.keyValueStore()));
        List<ProductDB1> list = new ArrayList<>();

        KeyValueIterator<String, ProductDB1> allProducts = recentlyViewedProductStore.all();
        while (allProducts.hasNext()) {
            KeyValue<String, ProductDB1> keyValue = allProducts.next();
            list.add(keyValue.value);
        }
        allProducts.close();

        // Check if there are 6 or fewer products
        if (list.size() <= 6) {
            return list;  // Return all products
        } else {
            return list.subList(0, 6);  // Return the first 6 products
        }
    }

    public List<ProductDB1> getSimilarProducts(List<ProductDB1> recentlyViewed, List<ProductDB1> productDataWithCount) {
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

    public boolean checkIfUserHasAnyHistory(String userId) {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, ProductDB1> userViewedProducts = streams.store(StoreQueryParameters.fromNameAndType("RecentlyViewedProductStore", QueryableStoreTypes.keyValueStore()));

        // Query the store for the specified userId
        ProductDB1 productDB = userViewedProducts.get(userId);

        // Check if a record exists for the userId
        if (productDB != null) {
            // A record exists for the userId
            return true;
        }
        return false;
    }

    public JsonNode getProductWithSimilarProducts(String category, String productId, String userId) throws InterruptedException {
        waitForStreams();
        ObjectNode objectNode = objectMapper.createObjectNode();
        List<JsonNode> productData = new ArrayList<>();

        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        // Define the store names and types
        StoreQueryParameters<ReadOnlyKeyValueStore<String, ProductDB1>> productStoreParams =
                StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore());

        StoreQueryParameters<ReadOnlyKeyValueStore<String, Long>> countStoreParams =
                StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore());

        // Get the read-only key-value stores
        ReadOnlyKeyValueStore<String, ProductDB1> productDataStore = streams.store(productStoreParams);
        ReadOnlyKeyValueStore<String, Long> productCountStore = streams.store(countStoreParams);

        // Query the store for the product data based on the productId
        ProductDB1  product = productDataStore.get(productId);

        if (product != null) {
            // Convert the ProductDB object to a JSON representation
            ObjectNode productNode = objectMapper.convertValue(product, ObjectNode.class);
            productData.add(productNode);

            // Find and include similar products with the highest combined count (viewCount + count from ProductCountStore)
            List<JsonNode> similarProducts = new ArrayList<>();
            KeyValueIterator<String, ProductDB1> allProductsIterator = productDataStore.all();

            // Iterate through all products in the store
            while (allProductsIterator.hasNext()) {
                KeyValue<String, ProductDB1> entry = allProductsIterator.next();
                ProductDB1 p = entry.value;

                if (category.equals(p.getCategoryId()) && !productId.equals(p.getProductId())) {
                    ObjectNode similarProductNode = objectMapper.convertValue(p, ObjectNode.class);

                    // Calculate the combined count (viewCount + count from ProductCountStore)
                    long viewCount = similarProductNode.get("viewCount").asLong();
                    long count = productCountStore.get(p.getProductId());

                    // Add the combined count as a property to the similar product node
                    similarProductNode.put("combinedCount", viewCount + count);

                    similarProducts.add(similarProductNode);
                }
            }

            allProductsIterator.close();

            // Sort by combined count in descending order
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

}




















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
