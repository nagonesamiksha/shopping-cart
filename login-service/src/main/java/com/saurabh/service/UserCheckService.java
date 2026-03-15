package com.saurabh.service;

import com.saurabh.grpc.sessionService.*;
import com.saurabh.grpc.sessionService.LogUserOutRequest;
import com.saurabh.grpc.sessionService.LogUserOutResponse;
import com.saurabh.grpc.sessionService.SessionExistsUpdateRequest;
import com.saurabh.grpc.sessionService.SessionExistsUpdateResponse;
import com.saurabh.grpc.sessionService.SessionServiceGrpc;
import com.saurabh.grpc.userdbservice.*;
import com.saurabh.grpc.userdbservice.DBServiceGrpc;
import com.saurabh.grpc.userdbservice.PasswordFromDBRequest;
import com.saurabh.grpc.userdbservice.PasswordFromDBResponse;
import com.saurabh.grpc.userdbservice.UserIdValidationRequest;
import com.saurabh.grpc.userdbservice.UserIdValidationResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service

public class UserCheckService {
    @GrpcClient("userdbservice")
    private DBServiceGrpc.DBServiceBlockingStub stubDB2;

    @GrpcClient("sessionService")
    private SessionServiceGrpc.SessionServiceBlockingStub stubSS;

    public  String getPasswordFromDB(String userId) {
        PasswordFromDBRequest passwordFromDBRequest =PasswordFromDBRequest.newBuilder().setUserId(userId).build();
        PasswordFromDBResponse passwordFromDBResponse = stubDB2.getPasswordFromDB(passwordFromDBRequest);
        return passwordFromDBResponse.getPassword();

    }

    public boolean checkIfUserIdIsValid(String userId) {
        log.info("UserId Recieved is"+userId);
        UserIdValidationRequest userIdValidationRequest = UserIdValidationRequest.newBuilder().setUserId(userId).build();
        log.info("After building UserIdValidationRequest");
        UserIdValidationResponse userIdValidationResponse = stubDB2.checkUserId(userIdValidationRequest);
        log.info("UserId is correct result "+userIdValidationResponse.getIsUserIdThere());
        return userIdValidationResponse.getIsUserIdThere();

    }
    public Map<String,Integer> UserPasswordAttemptCount = new HashMap<>();

    public boolean isSessonAlreadyExists(String userId, String loginDevice) {

        //String sessionId = "Session"+"-"+loginDevice;
        SessionExistsUpdateRequest sessionExistsUpdateRequest = SessionExistsUpdateRequest.newBuilder().setUserId(userId).setDevice(loginDevice).build();
        SessionExistsUpdateResponse sessionExistsUpdateResponse = stubSS.checkSession(sessionExistsUpdateRequest);


        return sessionExistsUpdateResponse.getIsSessionActive();

    }

    public boolean logUserOff(String userId, String device) {
        LogUserOutRequest logUserOutRequest = LogUserOutRequest.newBuilder().setUserId(userId).setDevice(device).build();
        LogUserOutResponse logUserOutResponse= stubSS.logTheUserOut(logUserOutRequest);
        return logUserOutResponse.getIsUserLoggedOff();

    }
}
