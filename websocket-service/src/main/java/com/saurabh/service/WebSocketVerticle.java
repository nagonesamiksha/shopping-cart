package com.saurabh.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebSocketVerticle extends AbstractVerticle {



    // Map to store active WebSocket connections with their user IDs
    private Map<String, ServerWebSocket> activeClientConnections = new HashMap<>();

    // Map to store last heartbeat times for each user
    private Map<String, Long> heartBeatLocalStore = new HashMap<>();

    // Maximum time allowed for a connection to remain idle (in milliseconds)
    private static final long MAX_IDLE_TIME_MS = 15_000; // 120 seconds

    // Maximum time allowed for a ping response to be received (in milliseconds)
    private static final long MAX_PING_RESPONSE_TIME_MS = 10_000; // 30 seconds

    @Override
    public void start() {
        HttpServerOptions options = new HttpServerOptions()
                .addWebSocketSubProtocol("OSC-WebSocket-Protocol"); // Add the subprotocol here

        HttpServer server = vertx.createHttpServer(options);

        server.webSocketHandler(this::handleWebSocketConnection);

        server.listen(8888);

        // Start the periodic check to see if clients have sent a message
        vertx.setPeriodic(10_000, this::checkPingInterval);
    }


    private void handleWebSocketConnection(ServerWebSocket webSocket) {
        // Validate WebSocket connection parameters (Login device, UserId, SessionId)

        MultiMap map = webSocket.headers();
        String headerString = map.get("Sec-WebSocket-Protocol");
        System.out.println(headerString);
        String[] data  = headerString.split(",");
        String uniqueKey = data[1].trim()+"-"+data[2].trim()+"-"+data[3].trim();
        String userId = data[1].trim();
        String sessionId = data[2].trim();
        String deviceType = data[3].trim();
        if (userId == null || sessionId == null || deviceType == null) {
            // Reject the WebSocket connection if any of the parameters is missing
            webSocket.close((short) 400, "Invalid connection request: Missing parameters.");
            return;
        }

        boolean userLoggedIn = true;

        if (!userLoggedIn) {
            // Close the WebSocket connection if the user is not logged in
            log.info("user is not logged in the system");
            webSocket.close((short) 400, "Invalid connection request: User not logged in.");
            return;
        }


        // WebSocket connection parameters are valid, proceed with connection
        log.info("WebSocket connection established: UserId=" + userId + ", SessionId=" + sessionId
                + ", DeviceType=" + deviceType);

        // Store the WebSocket connection in the activeClientConnections map
        activeClientConnections.put(userId, webSocket);

        // Echo back any received message
        webSocket.textMessageHandler(message -> {
            log.info("WebSocket message: UserId=" + message);
            webSocket.writeTextMessage(message);
            // Update the last heartbeat time when a message is received
            heartBeatLocalStore.put(userId, System.currentTimeMillis());

        });

        // Handle WebSocket close event
        webSocket.closeHandler(close -> {
            log.info("WebSocket closed: UserId=" + userId + ", SessionId=" + sessionId + ", DeviceType=" + deviceType);
            handleWebSocketClose(userId);
        });
    }
    private void handleWebSocketClose(String userId) {
        // Remove the WebSocket connection from activeClientConnections map
        ServerWebSocket webSocket = activeClientConnections.remove(userId);
        if (webSocket != null) {
            try {
                webSocket.close();
            } catch (Exception e) {
                log.warn("Error while closing WebSocket for UserId=" + userId + ": " + e.getMessage());
            }
        }
    }
    private void checkPingInterval(Long timerId) {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<String, Long>> iterator = heartBeatLocalStore.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String userId = entry.getKey();
            Long lastHeartbeatTime = entry.getValue();
            long idleTime = currentTime - lastHeartbeatTime;
            log.info("Time passed: idleTime=" + idleTime);
            if (idleTime > MAX_PING_RESPONSE_TIME_MS) {
                // Perform actions if the last ping response time exceeds MAX_PING_RESPONSE_TIME_MS (120 seconds)
                log.info("Connection closed due to inactivity: UserId=" + userId);
                handleWebSocketClose(userId);
                log.info("No ping response received: UserId=" + userId);

            }
            if (idleTime > MAX_IDLE_TIME_MS) {
                // Perform logout and cleanup actions for the user if idle time exceeds MAX_IDLE_TIME_MS (30 seconds)
                log.info("User logged out due to inactivity: UserId=" + userId);
                iterator.remove(); // Remove the user details from the heartbeat store
            }
        }
    }
}
