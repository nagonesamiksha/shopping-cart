package org.saurabh.service;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ActorForUser extends AbstractActor {

   @Autowired
   ProductService productService;

    private final ServerWebSocket websocket;
    public ActorForUser(ServerWebSocket webSocket){
        this.websocket = webSocket;
    }
    public static Props getProps(ServerWebSocket webSocket){
        return Props.create(ActorForUser.class,()->new ActorForUser(webSocket));
    }



    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ObjectNode.class, message -> {
            String request = message.get("OSC").asText();
            switch (request) {
                case "Get_Products":
                    log.info("About to fetch the products from the Ktable");
                    JsonNode product = productService.getProductWithSimilarProducts(message.get("category").asText(),message.get("productId").asText(),message.get("userId").asText());
                    if (product != null) {
                        log.info("The products fetched are for the case MT2 "+product.toString());
                        websocket.writeTextMessage(product.toString());

                    } else {
                       log.info("Product fetching failed");
                    }
                    break;
                default:

                    break;
            }
        }).build();
    }






    }


