package com.saurabh.service;


import akka.actor.Actor;
import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saurabh.entity.ProductDB1;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WebSocketVerticles2 extends AbstractVerticle {
//    @Autowired
//    SessionForWebSocketService sessionForWebSocketService;
    @Autowired
    ProductService productService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTemplate<String, ProductDB1> kafkaTemplate1;
    private static final int INACTIVITY_THRESHOLD_1 = 30000; // 30 seconds
    private static final int INACTIVITY_THRESHOLD_2 = 120000; // 120 seconds

    private Map<String, Long> lastActivityTimes = new ConcurrentHashMap<>();
    private Map<String, ServerWebSocket> activeConnections = new ConcurrentHashMap<>();
    private Vertx vertx;

    public WebSocketVerticles2(KafkaTemplate<String, String> kafkaTemplate, KafkaTemplate<String, ProductDB1> kafkaTemplate1) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplate1 = kafkaTemplate1;
    }

    @Override
    public void start() {
        vertx = Vertx.vertx();

        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol("OSC-WebSocket-Protocol");

        HttpServer server = vertx.createHttpServer(options);

        server.webSocketHandler(this::handleWebSocketConnection);

        // Schedule the task to run every 5 seconds
        executor.scheduleAtFixedRate(this::processLastActivityTimes, 0, 5, TimeUnit.SECONDS);

        server.listen(8888);
    }

    private void processLastActivityTimes() {

        log.info("processLastActivityTimes Started : "+new Date().getTime());
        long currentTime = System.currentTimeMillis();
        log.info("Size of map : "+lastActivityTimes.entrySet().size());

        // Iterate over the map entries and check the last activity time
        for (Map.Entry<String, Long> entry : lastActivityTimes.entrySet()) {
            log.info("User : "+entry.getKey());
            String key1 = entry.getKey();
            String[] data = key1.split("-");
            String userId = data[0].trim();
            String deviceType = data[1].trim();

            long lastActivityTime = entry.getValue();

            // Calculate the elapsed time since the last activity
            long elapsedTime = currentTime - lastActivityTime;

            // Apply the 30-second and 120-second logic
            if (elapsedTime >= INACTIVITY_THRESHOLD_1) {
                // Handle inactivity of 30 seconds
                log.info("Connection closed due to inactivity (30 seconds): UserId=" + userId);
                handleWebSocketClose(key1);

                if (elapsedTime >= INACTIVITY_THRESHOLD_2) {
                    // Handle inactivity of 120 seconds
                    log.info("User logged out due to inactivity (120 seconds): UserId=" + userId);
                    handleUserLogout(userId, deviceType);
                }
            }
        }
    }

    private void handleWebSocketConnection(ServerWebSocket webSocket) {
        MultiMap map = webSocket.headers();
        String headerString = map.get("Sec-WebSocket-Protocol");
        String[] data = headerString.split(",");
        String userId = data[1].trim();
        String sessionId = data[2].trim();
        String deviceType = data[3].trim();
        String uniqueKey = data[1].trim()+"-"+data[2].trim()+"-"+data[3].trim().toLowerCase();
        if (userId == null || sessionId == null || deviceType == null) {
            // Reject the WebSocket connection if any of the parameters is missing
            webSocket.close((short) 400, "Invalid connection request: Missing parameters.");
            return;
        }

        boolean userLoggedIn = true;
        //boolean userLoggedIn1= sessionForWebSocketService.checkIfUserIsActive(userId,deviceType);

        if (!userLoggedIn) {
            // Close the WebSocket connection if the user is not logged in
            log.info("user is not logged in the system");
            webSocket.close((short) 400, "Invalid connection request: User not logged in.");
            return;
        }


        // WebSocket connection parameters are valid, proceed with connection
        log.info("WebSocket connection established: UserId=" + userId + ", SessionId=" + sessionId + ", DeviceType=" + deviceType);
        // Store the WebSocket connection
        String key =userId+"-"+deviceType;
        createActor(webSocket, uniqueKey);
        activeConnections.put(key, webSocket);
        lastActivityTimes.put(key,System.currentTimeMillis());

        //create an actor

        // Handle received messages
        webSocket.textMessageHandler(message -> {

            handleReceivedPing(userId,deviceType);

            vertx.executeBlocking(task->{
                log.info("We will check is user there in our ActiveConnections Map");
                log.info("Is user there "+activeConnections.get(key));

            if(activeConnections.get(key)!=null){
                JsonNode msg;
                //System.out.println("*******************************************msg is in json format************ "+msg);
                try {
                    System.out.println("***********************Inside try block for msg objectmapper**************************");
                    msg = objectMapper.readTree(message.toString());
                    System.out.println("**********************msg is********************************* "+msg);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                String type = msg.get("MT").asText();
                log.info("The type of the message is "+type);
                ObjectNode responseMsg = objectMapper.createObjectNode();
                switch (type){
                    case "ping":
                        //handleReceivedPing(userId,deviceType);
                        responseMsg.put("BCM",100);
                        webSocket.writeTextMessage(responseMsg.toString());
                        task.complete();
                        break;
                    case "2" :
                        System.out.println("********************************Inside Case 2***********************************************************");
                        String categoryId = msg.get("catId").asText();
                        String productId = msg.get("prodId").asText();
                        System.out.println("*********************CategoryId********************** "+categoryId);
                        System.out.println("*********************ProductId************************ "+productId);

                        kafkaTemplate.send("ProductCountTopic",productId,"1");
                        log.info("count send into ProductCountTopic");
                        kafkaTemplate.send("CategoryCountTopic",categoryId,"1");
                        log.info("count send into CategoryCountTopic");
                        String jsonValue = "{\"userId\":\"" + userId + "\",\"productId\":\"" + productId + "\"}";
                        kafkaTemplate.send("RecentlyViewedProducts",userId,jsonValue);
                        log.info("***************************productId send into RecentlyViewedProducts***************************************");
                        responseMsg.put("BCM","GET_PRODUCT").put("category",msg.get("catId").asText()).put("productId",msg.get("prodId").asText()).put("userId",userId);
                        SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        break;
                    case "3" :
                        String filter = msg.get("filter").asText();

                        if(filter.equalsIgnoreCase("LH")) {
                            responseMsg.put("BCM", "GET_PRODUCTS_BY_CATEGORY_LH").put("category", msg.get("catId").asText()).put("filter", filter);
                            SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        } else if (filter.equalsIgnoreCase("HL")) {
                            responseMsg.put("BCM", "GET_PRODUCTS_BY_CATEGORY_HL").put("category", msg.get("catId").asText()).put("filter", filter);
                            SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        }
                        else{
                            responseMsg.put("BCM", "GET_PRODUCTS_BY_CATEGORY_P").put("category", msg.get("catId").asText()).put("filter", filter);
                            SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        }
                        break;
                    case "6" :

                        responseMsg.put("BCM","GET_PRODUCT_CART").put("userId",msg.get("userId").asText());
                       SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        break;
                    case "8" :
                        //String categoryId1 = msg.get("catId").asText();
                        System.out.println("*****************************************INSIDE CASE 8*****************************************");
                        String productId1 = msg.get("prodId").asText();
                        String jsonValueForCart = "{\"userId\":\"" + userId + "\",\"productId\":\"" + productId1+ "\"}";

                        responseMsg.put("BCM","UPDATE_PRODUCT_IN_CART").put("request","update").put("userId",msg.get("userId").asText()).put("productId",msg.get("prodId").asText()).put("updateValue",-1);
                        //kafkaTemplate.send("CartProduct",jsonValueForCart,"-1");
                        SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        break;
                    case "9" :
                        System.out.println("*****************************************INSIDE CASE 9*****************************************");

                         String productId2 = msg.get("prodId").asText();
                        log.info("ProductId to add is "+productId2);
                        String jsonValueForCart1 = "{\"userId\":\"" + userId + "\",\"productId\":\"" + productId2+ "\"}";
                        responseMsg.put("BCM","UPDATE_PRODUCT_IN_CART").put("request","update").put("userId",msg.get("userId").asText()).put("productId",msg.get("prodId").asText()).put("updateValue",1);
                        //kafkaTemplate.send("CartProduct",jsonValueForCart1,"1");

                        SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        break;
                    case "10":
                        String categoryId3 = msg.get("catId").asText();
                        String productId3 = msg.get("prodId").asText();
                        String jsonValueForCart2 = "{\"userId\":\"" + userId + "\",\"productId\":\"" + productId3+ "\"}";
                        responseMsg.put("BCM","REMOVE_PRODUCT_IN_CART").put("request","remove").put("userId",msg.get("userId").asText()).put("productId",msg.get("prodId").asText());
                        //kafkaTemplate.send("CartProduct",jsonValueForCart2,"0");
                        SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                        break;
                    default:
                        responseMsg.put("BCM","REFRESH").put("userId",userId);
                        SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(responseMsg, Actor.noSender());
                }
            }

            });



        });

        // Handle WebSocket close event
        webSocket.closeHandler(close -> {
            handleWebSocketClose(key);
        });

        // Start the timer for this WebSocket connection
        //startTimer(userId,deviceType);
    }



    public void createActor(ServerWebSocket webSocket, String uniqueKey) {
        ActorRef actorRef = SingletonActorService.getInstance().getActorSystem().actorOf(ActorForUser.getProps(webSocket, productService));
        SingletonActorMapService.getInstance().saveActorRef(uniqueKey, actorRef);
    }



    /*private void startTimer(String userId,String deviceType) {
        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(INACTIVITY_THRESHOLD_1);
                log.info("Connection closed due to inactivity (30 seconds): UserId=" + userId);
                handleWebSocketClose(userId);

                Thread.sleep(INACTIVITY_THRESHOLD_2 - INACTIVITY_THRESHOLD_1);
                log.info("User logged out due to inactivity (120 seconds): UserId=" + userId);
                handleUserLogout(userId,deviceType);
            } catch (InterruptedException e) {
                log.warn("Timer thread interrupted: " + e.getMessage());
            }
        });

        timerThread.start();
    }*/

    private void resetTimer(String userId,String deviceType) {
        // Restart the timer by interrupting the existing thread
        Long lastActivityTime = lastActivityTimes.get(userId+"-"+deviceType);
        if (lastActivityTime != null) {
            lastActivityTimes.put(userId+"-"+deviceType, System.currentTimeMillis());
        }
    }

    private void handleReceivedPing(String userId,String deviceType) {
        // Reset the timer on receiving a ping message
        log.info("Received ping from user: " + userId);
        resetTimer(userId,deviceType);
    }

    private void handleWebSocketClose(String key) {
        ServerWebSocket webSocket = activeConnections.remove(key);
        if (webSocket != null) {
            try {
                webSocket.close();
            } catch (Exception e) {
                log.warn("Error while closing WebSocket for UserId=" + key + ": " + e.getMessage());
            }
        }
    }

    private void handleUserLogout(String userId,String deviceType) {

        log.info("So we are shutting down the {}",userId);
        activeConnections.remove(userId+"-"+deviceType);
        lastActivityTimes.remove(userId+"-"+deviceType);
        //sessionForWebSocketService.logUserOut(userId,deviceType);

    }


}
/*import java.util.concurrent.*;

// ...

// Define a ScheduledExecutorService to schedule the task
private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

@Override
public void start() {
    // ...

    // Schedule the task to run every 10 seconds
    executor.scheduleAtFixedRate(this::processLastActivityTimes, 0, 10, TimeUnit.SECONDS);
}

// Define a method to process the lastActivityTimes map
private void processLastActivityTimes() {
    long currentTime = System.currentTimeMillis();

    // Iterate over the map entries and check the last activity time
    for (Map.Entry<String, Long> entry : lastActivityTimes.entrySet()) {
        String userId = entry.getKey();
        long lastActivityTime = entry.getValue();

        // Calculate the elapsed time since the last activity
        long elapsedTime = currentTime - lastActivityTime;

        // Apply the 30-second and 120-second logic
        if (elapsedTime >= INACTIVITY_THRESHOLD_1) {
            // Handle inactivity of 30 seconds
            log.info("Connection closed due to inactivity (30 seconds): UserId=" + userId);
            handleWebSocketClose(userId);

            if (elapsedTime >= INACTIVITY_THRESHOLD_2) {
                // Handle inactivity of 120 seconds
                log.info("User logged out due to inactivity (120 seconds): UserId=" + userId);
                handleUserLogout(userId, deviceType);
            }
        }
    }
}
*/
