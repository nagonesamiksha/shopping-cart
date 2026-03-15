//package com.saurabh.service;
//
//import com.saurabh.grpc.loginService.LoginServiceGrpc;
//import com.saurabh.grpc.loginService.SessionRequest;
//import com.saurabh.grpc.loginService.SessionResponse;
//import io.grpc.stub.StreamObserver;
//import net.devh.boot.grpc.server.service.GrpcService;
//import org.springframework.beans.factory.annotation.Autowired;
//
//@GrpcService
//public class LoginGrpcService extends LoginServiceGrpc.LoginServiceImplBase{
//    @Autowired
//    KProducer kProducer;
//
//    @Override
//    public void isSessionExists(SessionRequest request, StreamObserver<SessionResponse> responseObserver) {
//        String key= request.getKey();
//        boolean res = kProducer.getStatusForWebSocket(key);
//        SessionResponse sessionResponse =SessionResponse.newBuilder().setResult(res).build();
//    }
//}
