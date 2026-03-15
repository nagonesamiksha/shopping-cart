package org.saurabh.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saurabh.grpc.dashboardserviceforwebsocket.DashBoardServiceForWebsocket1Grpc;
import com.saurabh.grpc.dashboardserviceforwebsocket.ProductRequest;
import com.saurabh.grpc.dashboardserviceforwebsocket.ProductResponse;
import com.saurabh.userDB.entity.ProductDB;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class DashBoardGrpcService extends DashBoardServiceForWebsocket1Grpc.DashBoardServiceForWebsocket1ImplBase{
    @Autowired
    StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @Override
    public void getProductWithSimilarProducts(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {

    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode getProductWithSimilarProducts(String category, String productId, String userId) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        List<JsonNode> productData = new ArrayList<>();

        KafkaStreams streams =streamsBuilderFactoryBean.getKafkaStreams();

        // Define the store names and types
        StoreQueryParameters<ReadOnlyKeyValueStore<String, ProductDB>> productStoreParams =
                StoreQueryParameters.fromNameAndType("ProductDataStore", QueryableStoreTypes.keyValueStore());

        StoreQueryParameters<ReadOnlyKeyValueStore<String, Long>> countStoreParams =
                StoreQueryParameters.fromNameAndType("ProductCountStore", QueryableStoreTypes.keyValueStore());

        // Get the read-only key-value stores
        ReadOnlyKeyValueStore<String, ProductDB> productDataStore = streams.store(productStoreParams);
        ReadOnlyKeyValueStore<String, Long> productCountStore = streams.store(countStoreParams);

        // Query the store for the product data based on the productId
        ProductDB product = productDataStore.get(productId);

        if (product != null) {
            // Convert the ProductDB object to a JSON representation
            ObjectNode productNode = objectMapper.convertValue(product, ObjectNode.class);
            productData.add(productNode);

            // Find and include similar products with the highest combined count (viewCount + count from ProductCountStore)
            List<JsonNode> similarProducts = new ArrayList<>();
            KeyValueIterator<String, ProductDB> allProductsIterator = productDataStore.all();

            // Iterate through all products in the store
            while (allProductsIterator.hasNext()) {
                KeyValue<String, ProductDB> entry = allProductsIterator.next();
                ProductDB p = entry.value;

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

}
