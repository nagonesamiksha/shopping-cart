package org.saurabh.service;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public class WebSocketStartService {
    public void start(){

        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        HttpServerOptions httpOSC = new HttpServerOptions();
        httpOSC.addWebSocketSubProtocol("OSC-WebSocket-Protocol");

        HttpServer server = vertx.createHttpServer(httpOSC);
        server.webSocketHandler(new WebSocketVerticle(vertx));


        server.listen(8888,result->{
            if (result.succeeded()){
                System.out.println("******************************Websocket successs*************************************");
            }
            else{
                System.out.println("*******************************Websocket failed*********************************************");
            }
        });
    }
}
