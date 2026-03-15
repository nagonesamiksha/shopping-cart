package com.saurabh.service;

import com.saurabh.grpc.sessionService.*;
import com.saurabh.grpc.sessionService.LogUserOffWebSocketRequest;
import com.saurabh.grpc.sessionService.LogUserOffWebSocketResponse;
import com.saurabh.grpc.sessionService.LogUserOutRequest;
import com.saurabh.grpc.sessionService.LogUserOutResponse;
import com.saurabh.grpc.sessionService.LoginInfoRequest;
import com.saurabh.grpc.sessionService.LoginInfoResponse;
import com.saurabh.grpc.sessionService.SessionExistsUpdateRequest;
import com.saurabh.grpc.sessionService.SessionExistsUpdateResponse;
import com.saurabh.grpc.sessionService.SessionServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@GrpcService
public class SessionUpdateService extends SessionServiceGrpc.SessionServiceImplBase {
    @Autowired
    UserSessionStoreService userSessionStoreService;

    @Override
    public void logTheUserOut(LogUserOutRequest request, StreamObserver<LogUserOutResponse> responseObserver) {
        String userId = request.getUserId();
        String device = request.getDevice();
        String value = "Session" + "-" + device;
        String key = userId + "-" + device;

       boolean res= userSessionStoreService.updateStoreForLogout(key,value);
       LogUserOutResponse logUserOutResponse= LogUserOutResponse.newBuilder().setIsUserLoggedOff(res).build();
       responseObserver.onNext(logUserOutResponse);
       responseObserver.onCompleted();

    }

    @Override
    public void checkSession(SessionExistsUpdateRequest request, StreamObserver<SessionExistsUpdateResponse> responseObserver) {
        String userId = request.getUserId();
        String device = request.getDevice();
        boolean isUserAlreadyLoggedWithSameDevice = userSessionStoreService.getSessionStatus(userId,device);
        if(isUserAlreadyLoggedWithSameDevice)
        {
            SessionExistsUpdateResponse sessionExistsUpdateResponse = SessionExistsUpdateResponse.newBuilder().setIsSessionActive(true).build();
            responseObserver.onNext(sessionExistsUpdateResponse);
            responseObserver.onCompleted();
        }
        else {
            SessionExistsUpdateResponse sessionExistsUpdateResponse = SessionExistsUpdateResponse.newBuilder().setIsSessionActive(false).build();
            responseObserver.onNext(sessionExistsUpdateResponse);
            responseObserver.onCompleted();
        }

    }

    @Override
    public void logUserOutForcefully(LogUserOffWebSocketRequest request, StreamObserver<LogUserOffWebSocketResponse> responseObserver) {
        String userId = request.getUserId();
        String device = request.getDevice();
        String key = userId +"-"+device;
        boolean res = userSessionStoreService.logUserOffWebsocket(key);
        LogUserOffWebSocketResponse logUserOffWebSocketResponse = LogUserOffWebSocketResponse.newBuilder().setIsUserLoggedOffFromWebSocket(res).build();
        responseObserver.onNext(logUserOffWebSocketResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void checkLoginStatusForWebsocket(LoginInfoRequest request, StreamObserver<LoginInfoResponse> responseObserver) {
        String userId1 = request.getUserId();
        String device1 = request.getDevice();
        System.out.println("********************************UserId is******************************************"+userId1);
        System.out.println("********************************Device is******************************************"+device1);

//        String sessionId = "Session" + "-" + device;
//        String key = userId + "-" + device;

        boolean isUserLoggedIn = userSessionStoreService.checkStatusForWebsocket(userId1,device1);
        LoginInfoResponse loginInfoResponse = LoginInfoResponse.newBuilder().setIsUserSessionActive(isUserLoggedIn).build();
        System.out.println("********************************isUserLoggedIn******************************************"+isUserLoggedIn);

        responseObserver.onNext(loginInfoResponse);
        responseObserver.onCompleted();

    }
}
