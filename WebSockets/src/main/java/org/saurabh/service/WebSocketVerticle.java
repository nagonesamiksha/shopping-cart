package org.saurabh.service;

import akka.actor.Actor;
import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Handler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Service
public class WebSocketVerticle implements Handler<ServerWebSocket> {


    @Autowired
    MapServiceForTimeKeeping mapService;

    @Autowired
    SessionForWebSocketService sessionForWebSocketService;


    private static final ObjectMapper objectMapper = new ObjectMapper();

//    private Map<String, Long> lastActivityTimes = new ConcurrentHashMap<>();
    //private static Map<String, ServerWebSocket> activeConnections = new ConcurrentHashMap<>();

    private final Vertx vertx;
    public WebSocketVerticle(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(ServerWebSocket webSocket) {
        try {
            MultiMap map = webSocket.headers();
            String headerString = map.get("Sec-WebSocket-Protocol");
            String[] data = headerString.split(",");
            String uniqueKey = data[1].trim() + "-" + data[2].trim() + "-" + data[3].trim();
            String userId = data[1].trim();
            String sessionId = data[2].trim();
            String device = data[3].trim();
            log.info("Device is " + device);
            log.info("SessionId is " + sessionId);
            log.info("userId is " + userId);

//        //boolean res = webSocketService.isUserActive(userId, device);
//        boolean res = true;
//        if (!res) {
//            log.info("User is not logged in the system");
//            webSocket.close((short) 400, "Invalid connection request: User not logged in.");
//            return;
//        }
//        else {
//
//            createActor(webSocket, uniqueKey);
//            handleTheIncomingMessages(webSocket,uniqueKey,userId);
//            activeConnections.put(uniqueKey, webSocket);
//            resetOrStartTimer(uniqueKey);
//        }
            vertx.executeBlocking(task -> {
                if (mapService.isUserThere(uniqueKey)) {
                    if (mapService.getStatus(uniqueKey).equals("Close")) {
                        log.info("Connection reestablished for "+uniqueKey);
                        task.complete();
                    } else {
                        SingletonActorMapService.getInstance().removerActorRef(uniqueKey);
                        task.complete();
                    }
                } else if (sessionForWebSocketService.checkIfUserIsActive(userId,device)) {
                    log.info("New Websocket connection for the respectie user");
                    task.complete("New websocket");
                } else {
                    webSocket.close();
                    task.fail("Error occured");
                }
            }, true, asyncResult -> {
                if (asyncResult.succeeded()) {
                    System.out.println("Connection Established " + webSocket.textHandlerID());

                    createActor(webSocket, uniqueKey);
                    handleTheIncomingMessages(webSocket, uniqueKey, userId);
                    mapService.updateHeartbeat(uniqueKey);

                    webSocket.closeHandler(task -> {
                        log.info("Close Handler called");
                        closeWebsocket(uniqueKey);
                        log.info("Websocket Connection is closed");
                    });
                }
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }
    }

    public void handleTheIncomingMessages(ServerWebSocket webSocket,String uniqueKey,String userId){
        webSocket.handler(message -> {
            log.info("Now we are restarting the timer");
            //resetOrStartTimer(userId);
            vertx.executeBlocking(task -> {
                if (mapService.isUserThere(uniqueKey)) {
                    JsonNode msg;
                    try {
                        msg = objectMapper.readTree(message.toString());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("Message Received from the user is " + msg);

                    String S = msg.get("MT").asText();
                    ObjectNode response = objectMapper.createObjectNode();

                    switch (S) {
                        case "ping":

                            response.put("OSC", "100");
                            webSocket.writeTextMessage(response.toString());
                            task.complete();
                            break;
                        case "2":
                            log.info("Inside case 2");

                            response.put("OSC", "Get_Products")
                                    .put("category", msg.get("catId").asText())
                                    .put("productId", msg.get("prodId").asText())
                                    .put("userId", userId);
                            log.info("About to tell Actor to do the needfull about case 2");
                            SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(response, Actor.noSender());

                            break;
                    }
                }
            });
        });



    }

    public void closeWebsocket(String uniqueKey){
        SingletonActorMapService.getInstance().removerActorRef(uniqueKey);
    }

    public void createActor(ServerWebSocket webSocket, String uniqueKey) {
        ActorRef actorRef = SingletonActorService.getInstance().getActorSystem().actorOf(ActorForUser.getProps(webSocket));
        SingletonActorMapService.getInstance().saveActorRef(uniqueKey, actorRef);
    }



}



/*package org.saurabh.service;
import akka.actor.Actor;
import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Slf4j
@Service
public class WebSocketVerticle extends AbstractVerticle {

    @Autowired
    WebSocketService webSocketService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Long> lastActivityTimes = new ConcurrentHashMap<>();
    private Map<String, ServerWebSocket> activeConnections = new ConcurrentHashMap<>();
    private Map<String, ScheduledFuture<?>> timerMap = new ConcurrentHashMap<>();
    private Vertx vertx;
    private ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(10);

    @Override
    public void start() {
        vertx = Vertx.vertx();

        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol("OSC-WebSocket-Protocol");

        HttpServer server = vertx.createHttpServer(options);

        server.webSocketHandler(this::handleWebSocketConnection);

        server.listen(8888);
    }

    private void handleWebSocketConnection(ServerWebSocket webSocket) {
        MultiMap map = webSocket.headers();
        String headerString = map.get("Sec-WebSocket-Protocol");
        String[] data = headerString.split(",");
        String uniqueKey = data[1].trim() + "-" + data[2].trim() + "-" + data[3].trim();
        String userId = data[1].trim();
        String sessionId = data[2].trim();
        String device = data[3].trim();
        log.info("Device is" + device);
        log.info("SessionId is " + sessionId);
        log.info("userId is " + userId);


        boolean res= webSocketService.isUserActive(userId,device);
        //boolean res = true;
        if(!res){

            log.info("user is not logged in the system");

            webSocket.close((short) 400, "Invalid connection request: User not logged in.");

            return;
        }

        activeConnections.put(userId, webSocket);
        createActor(webSocket,uniqueKey);



        // Handle received messages
        webSocket.textMessageHandler(message -> {
            webSocket.writeTextMessage(message);
            handleReceivedPing(userId);
        });



        //message handler
        webSocket.handler(message->{
            vertx.executeBlocking(task->{
                if(activeConnections.get(userId)!=null){
                    JsonNode msg;

                    try {
                        msg = objectMapper.readTree(message.toString());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("Message Received from the user is "+msg);



                    String S = msg.get("MT").asText();
                    ObjectNode response = objectMapper.createObjectNode();

                    switch (S){
                        case "ping":
                            resetOrStartTimer(userId);
                            response.put("Ping Recieved","Yes");
                            webSocket.writeTextMessage(response.toString());
                            task.complete();
                            break;
                        case"2":
                            response.put("User wants to see products","Get_Products").put("category",msg.get("catId").asText()).put("productId",msg.get("prodId").asText()).put("userId",userId);
                            SingletonActorMapService.getInstance().getActorRef(uniqueKey).tell(response, Actor.noSender());

                    }
                }
            });
        });

        // Handle WebSocket close event
        webSocket.closeHandler(close -> {
            handleWebSocketClose(userId);
        });

        // Start or reset the timer for this WebSocket connection
        resetOrStartTimer(userId);
    }

    public void createActor(ServerWebSocket webSocket,String uniqueKey){
        ActorRef actorRef = SingletonActorService.getInstance().getActorSystem().actorOf(org.saurabh.service.Actor.getProps(webSocket));
        SingletonActorMapService.getInstance().saveActorRef(uniqueKey,actorRef);

    }

    private void resetOrStartTimer(String userId) {
        ScheduledFuture<?> existingTimer = timerMap.get(userId);
        if (existingTimer != null) {
            existingTimer.cancel(true);
        }

        ScheduledFuture<?> newTimer = timerExecutor.schedule(() -> {
            System.out.println("******************************30 seconds*************************" + userId);
            handleWebSocketClose(userId);

        }, 30, TimeUnit.SECONDS);

        timerMap.put(userId, newTimer);
    }



    private void handleWebSocketClose(String userId) {
        ServerWebSocket webSocket = activeConnections.remove(userId);
        if (webSocket != null) {
            try {
                webSocket.close();
            } catch (Exception e) {
                log.warn("Error while closing WebSocket for UserId=" + userId + ": " + e.getMessage());
            }
        }
    }

    private void handleUserLogout(String userId, String device) {
        webSocketService.logUserOffForcefully(userId,device);
    }
}

/*@Autowired
private KafkaMessageProducerService kafkaProducerService;

// ...

public void messageHandler(ServerWebSocket webSocket, String uniqueKey, String userId) {
    webSocket.handler(message -> {
        vertxServer.executeBlocking(task -> {
            if (hazelcastService.isUserAvailable(uniqueKey)) {
                JsonNode msg;
                try {
                    msg = objectMapper.readTree(message.toString());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                String type = msg.get("MT").asText();
                ObjectNode responseMsg = objectMapper.createObjectNode();
                switch (type) {
                    case "ping":
                        hazelcastService.updateHeartbeat(uniqueKey);
                        responseMsg.put("BCM", 100);
                        webSocket.writeTextMessage(responseMsg.toString());
                        task.complete();
                        break;
                    case "2":
                        responseMsg.put("BCM", "GET_PRODUCT").put("category", msg.get("catId").asText()).put("productId", msg.get("prodId").asText()).put("userId", userId);
                        kafkaProducerService.publishMessage("your-kafka-topic", responseMsg.toString()); // Publish to Kafka
                        break;
                    case "3":
                        String filter = msg.get("filter").asText();
                        String kafkaTopic;

                        if (filter.equalsIgnoreCase("LH")) {
                            kafkaTopic = "kafka-topic-LH";
                        } else if (filter.equalsIgnoreCase("HL")) {
                            kafkaTopic = "kafka-topic-HL";
                        } else {
                            kafkaTopic = "kafka-topic-P";
                        }

                        responseMsg.put("BCM", "GET_PRODUCTS_BY_CATEGORY").put("category", msg.get("catId").asText()).put("filter", filter);
                        kafkaProducerService.publishMessage(kafkaTopic, responseMsg.toString()); // Publish to Kafka
                        break;
                    case "6":
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            System.out.println("**********************************Thread error MT 6****************");
                        }
                        responseMsg.put("BCM", "GET_PRODUCT_CART").put("userId", msg.get("userId").asText());
                        kafkaProducerService.publishMessage("kafka-topic-6", responseMsg.toString()); // Publish to Kafka
                        break;
                    case "8":
                        responseMsg.put("BCM", "UPDATE_PRODUCT_IN_CART").put("request", "update").put("userId", msg.get("userId").asText()).put("productId", msg.get("prodId").asText()).put("updateValue", -1);
                        kafkaProducerService.publishMessage("kafka-topic-8", responseMsg.toString()); // Publish to Kafka
                        break;
                    case "9":
                        responseMsg.put("BCM", "UPDATE_PRODUCT_IN_CART").put("request", "update").put("userId", msg.get("userId").asText()).put("productId", msg.get("prodId").asText()).put("updateValue", 1);
                        kafkaProducerService.publishMessage("kafka-topic-9", responseMsg.toString()); // Publish to Kafka
                        break;
                    case "10":
                        responseMsg.put("BCM", "REMOVE_PRODUCT_IN_CART").put("request", "remove").put("userId", msg.get("userId").asText()).put("productId", msg.get("prodId").asText());
                        kafkaProducerService.publishMessage("kafka-topic-10", responseMsg.toString()); // Publish to Kafka
                        break;
                    default:
                        responseMsg.put("BCM", "REFRESH").put("userId", userId);
                        kafkaProducerService.publishMessage("kafka-topic-DEFAULT", responseMsg.toString()); // Publish to Kafka
                }
            } else {
                webSocket.close();
                task.complete();
            }
        }, false, asyncResult -> {
            // Handle completion or errors here...
        });
    });
}
*/


/*import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WebSocketIdleTimeout {

    private final ScheduledExecutorService timerExecutor;
    private final Map<String, ScheduledFuture<?>> timerMap;

    public WebSocketIdleTimeout(ScheduledExecutorService timerExecutor) {
        this.timerExecutor = timerExecutor;
        this.timerMap = new HashMap<>();
    }

    public void resetOrStartTimer(String userId) {
        ScheduledFuture<?> existingTimer = timerMap.get(userId);
        if (existingTimer != null) {
            existingTimer.cancel(true);
        }

        ScheduledFuture<?> newTimer = timerExecutor.schedule(() -> {
            if (System.currentTimeMillis() - lastActivityTime < 30000) {
                // User is active, reset the timer
                resetOrStartTimer(userId);
            } else {
                // User is idle, log them out
                System.out.println("******************************120 seconds*************************" + userId);
                handleWebSocketClose(userId);
            }
        }, 30, TimeUnit.SECONDS);

        timerMap.put(userId, newTimer);
    }

    private void handleWebSocketClose(String userId) {

    }
}
*/
/*import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class WebSocketVerticle extends AbstractVerticle {

    @Autowired
    WebSocketService webSocketService;


    private Map<String, Long> lastActivityTimes = new ConcurrentHashMap<>();
    private Map<String, ServerWebSocket> activeConnections = new ConcurrentHashMap<>();
    private Vertx vertx;

    @Override
    public void start() {
        vertx = Vertx.vertx();

        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol("OSC-WebSocket-Protocol");

        HttpServer server = vertx.createHttpServer(options);

        server.webSocketHandler(this::handleWebSocketConnection);

        server.listen(8888);
    }

    private void handleWebSocketConnection(ServerWebSocket webSocket) {
        MultiMap map = webSocket.headers();
        String headerString = map.get("Sec-WebSocket-Protocol");
        System.out.println(headerString);
        String[] data = headerString.split(",");
        String uniqueKey = data[1].trim() + "-" + data[2].trim() + "-" + data[3].trim();
        String userId = data[1].trim();
        String sessionId = data[2].trim();
        String device = data[3].trim();
        log.info("Device is"+device);
        log.info("SessionId is "+sessionId);
        log.info("userId is"+userId);

//        boolean res = webSocketService.isUserActive(userId,device);
//        System.out.println("*****************************************************************************************"+res);
//        if(!res){
//            webSocket.close((short) 500, " User isn't logged in");
//            return;
//        }
        activeConnections.put(userId, webSocket);//storing the connection in map
        // Handle received messages
        webSocket.textMessageHandler(message -> {
            webSocket.writeTextMessage(message);
            handleReceivedPing(userId);//this is to handle any messages
            resetTimer(userId);//this to reset the timer
        });

        // Handle WebSocket close event
        webSocket.closeHandler(close -> {
            handleWebSocketClose(userId);
        });

        // Start the timer for this WebSocket connection
        startTimer(userId,device);
    }

    private void startTimer(String userId,String device) {
        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(30000);
                System.out.println("******************************30 seconds*************************"+userId);
                handleWebSocketClose(userId);
                Thread.sleep(120000);
                System.out.println("******************************120 seconds*************************"+userId);
                handleUserLogout(userId,device);

            } catch (InterruptedException e) {
                System.out.println(e.getMessage());

            }
        });

        timerThread.start();
    }

    private void resetTimer(String userId) {
        // Restart the timer by interrupting the existing thread
        Long lastActivityTime = lastActivityTimes.get(userId);
        if (lastActivityTime != null) {
            lastActivityTimes.put(userId, System.currentTimeMillis());
        }
    }

    private void handleReceivedPing(String userId) {
        // Reset the timer on receiving a ping message

        resetTimer(userId);
    }

    private void handleWebSocketClose(String userId) {
        ServerWebSocket webSocket = activeConnections.remove(userId);
        if (webSocket != null) {
            try {
                webSocket.close();
            } catch (Exception e) {
                log.warn("Error while closing WebSocket for UserId=" + userId + ": " + e.getMessage());
            }
        }
    }

    private void handleUserLogout(String userId,String device) {
        // Perform user logout logic here
        webSocketService.logUserOffForcefully(userId,device);
    }
}

*/
/*  private void resetOrStartTimer(String uniqueKey) {
        ScheduledFuture<?> existingTimer = timerMap.get(uniqueKey);
        if (existingTimer != null) {
            existingTimer.cancel(true);
        }

        ScheduledFuture<?> newTimer = timerExecutor.schedule(() -> {
            System.out.println("******************************30 seconds*************************" + uniqueKey);
            handleWebSocketClose(uniqueKey);

        }, 30, TimeUnit.SECONDS);

        timerMap.put(uniqueKey, newTimer);
    }
*/

