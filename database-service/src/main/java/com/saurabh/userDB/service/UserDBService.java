package com.saurabh.userDB.service;
//import com.saurabh.grpc.userdbservice;
import com.saurabh.grpc.userdbservice.*;
//import com.saurabh.service.LogService;
import com.saurabh.grpc.userdbservice.CheckEmailRequest;
import com.saurabh.grpc.userdbservice.CheckEmailResponse;
import com.saurabh.grpc.userdbservice.EmailRequest;
import com.saurabh.grpc.userdbservice.EmailResponse;
import com.saurabh.grpc.userdbservice.GetNameRequest;
import com.saurabh.grpc.userdbservice.GetNameResponse;
import com.saurabh.grpc.userdbservice.GetUserRequestFromUserService;
import com.saurabh.grpc.userdbservice.GetUserResponseFromUserService;
import com.saurabh.grpc.userdbservice.PasswordFromDBRequest;
import com.saurabh.grpc.userdbservice.PasswordFromDBResponse;
import com.saurabh.grpc.userdbservice.UpdatePasswordRequest;
import com.saurabh.grpc.userdbservice.UpdatePasswordResponse;
import com.saurabh.grpc.userdbservice.UserCheckRequest;
import com.saurabh.grpc.userdbservice.UserCheckResponse;
import com.saurabh.grpc.userdbservice.UserIdValidationRequest;
import com.saurabh.grpc.userdbservice.UserIdValidationResponse;
import com.saurabh.userDB.entity.UserDB;
import com.saurabh.userDB.repository.UserDBRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.Optional;
@Slf4j
@GrpcService
public class UserDBService extends com.saurabh.grpc.userdbservice.DBServiceGrpc.DBServiceImplBase {
    @Autowired
    private UserDBRepository userDBRepository;

    @Override
    public void checkIfEmailExists(CheckEmailRequest request, StreamObserver<CheckEmailResponse> responseObserver) {
        String email= request.getEmailId();
        System.out.println(email);
        Optional<UserDB> userDB
                =userDBRepository.existsByEmail(email);
        System.out.println(userDB);
        if(userDB.isEmpty())
        {
            CheckEmailResponse checkEmailResponse = CheckEmailResponse.newBuilder().setIsEmailThere(false).build();
            responseObserver.onNext(checkEmailResponse);
        }
        else {
            CheckEmailResponse checkEmailResponse = CheckEmailResponse.newBuilder().setIsEmailThere(true).build();
            responseObserver.onNext(checkEmailResponse);
        }

        responseObserver.onCompleted();
    }


//    @Autowired
//    LogService logService;

    @Override
    public void getName(GetNameRequest request, StreamObserver<GetNameResponse> responseObserver) {
        String userid = request.getUserid();
        Optional<UserDB> userDB =userDBRepository.findById(userid);
        String name = userDB.get().getName();

        GetNameResponse getNameResponse = GetNameResponse.newBuilder().setUserName(name).build();
        responseObserver.onNext(getNameResponse);
        responseObserver.onCompleted();

    }

    @Override
    public void sendDataToDB(GetUserRequestFromUserService request, StreamObserver<GetUserResponseFromUserService> responseObserver) {

        String userId= request.getUserid();
        String name = request.getName();
        String email = request.getEmail();
        String contact = request.getContact();
        String dob = request.getDob();
        String password = request.getPassword();

        GetUserResponseFromUserService getUserResponseFromUserService;

        try{
            userDBRepository.save(new UserDB(userId,name,email,contact,dob,password,0));
            getUserResponseFromUserService= GetUserResponseFromUserService.newBuilder().setIsReceived(true).build();
            //from here give the email notification via kafka
        }
        catch(Exception ex)
        {
            getUserResponseFromUserService= GetUserResponseFromUserService.newBuilder().setIsReceived(false).build();
        }
        responseObserver.onNext(getUserResponseFromUserService);
        responseObserver.onCompleted();

    }

    @Override
    public void userCheck(UserCheckRequest request, StreamObserver<UserCheckResponse> responseObserver) {
        String userid = request.getUserid();
//        System.out.println(userid);
//        Optional<UserDB> userExists = userDBRepository.existsByUsername(username);
       //Optional<UserDB> userDB= userDBRepository.findById(request.getUserid());
        Optional<UserDB> userDB = userDBRepository.findById(userid);
        System.out.println(userDB);
        UserCheckResponse userCheckResponse;
        int checkAttempts = 0;

        try {
            if(userDB.isEmpty())
            {
                userCheckResponse =UserCheckResponse.newBuilder().setIsUserThere(false).setCode(201).build();
            }
            else{
                if (!(userDB.get().getPassword()).equals(request.getPassword())) {
                    checkAttempts = userDB.get().getPwdRetry();
                    checkAttempts++;
                    if (checkAttempts >= 3)//need to confirm this logic
                    {
                        userCheckResponse = UserCheckResponse.newBuilder().setIsUserThere(true).setCode(205).build();
                        userDB.get().setPwdRetry(checkAttempts);
                        userDBRepository.save(userDB.get());
                    }
                    else
                    {
                        userCheckResponse = UserCheckResponse.newBuilder().setIsUserThere(false).setCode(202).build();
                        userDB.get().setPwdRetry(checkAttempts);
                        userDBRepository.save(userDB.get());
                    }
                }else {
                    checkAttempts = 0;
                    userDB.get().setPwdRetry(checkAttempts);
                    userDBRepository.save(userDB.get());
                    userCheckResponse = UserCheckResponse.newBuilder().setIsUserThere(true).setCode(200).build();
                }
            }
        } catch (Exception e) {
            userCheckResponse =UserCheckResponse.newBuilder().setIsUserThere(false).setCode(0).build();
            System.out.println(e);
        }
        responseObserver.onNext(userCheckResponse);
        responseObserver.onCompleted();

    }

    @Override
    public void checkEmail(EmailRequest request, StreamObserver<EmailResponse> responseObserver) {
        //super.checkEmail(request, responseObserver);

        System.out.println("********************************Success***********************");
        String email = request.getEmail();

        Optional<UserDB> emailExists = userDBRepository.existsByEmail(email);
        System.out.println("existsByEmail method hit");

        EmailResponse response;

        if(emailExists.isEmpty()){
            response = EmailResponse.newBuilder().setIsExists(false).build();
        }
        else{
            response = EmailResponse.newBuilder().setIsExists(true).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updatePassword(UpdatePasswordRequest request, StreamObserver<UpdatePasswordResponse> responseObserver) {
        String email = request.getEmail();
        String password =request.getPassword();
        Optional<UserDB> userDB = userDBRepository.existsByEmail(email);
        if (userDB.isPresent()) {
            String userId = userDB.get().getUserId();
            userDB.get().setPassword(password);
            userDBRepository.save(userDB.get());
            UpdatePasswordResponse updatePasswordResponse= UpdatePasswordResponse.newBuilder().setCode(200).build();
            responseObserver.onNext(updatePasswordResponse);
            responseObserver.onCompleted();
        } else {
            UpdatePasswordResponse updatePasswordResponse= UpdatePasswordResponse.newBuilder().setCode(199).build();
            responseObserver.onNext(updatePasswordResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void checkUserId(UserIdValidationRequest request, StreamObserver<UserIdValidationResponse> responseObserver) throws IllegalStateException{
       try{
           String userId = request.getUserId();
           log.info("UserId received for the validation is "+userId);
        Optional<UserDB> userDB = userDBRepository.findById(userId);
        log.info("Checking whether the userId exists or not "+userDB.isEmpty());
        if(userDB.isEmpty())
        {
            log.info("userId is ivalid and there is no user with userid "+userId);
            UserIdValidationResponse userIdValidationResponse= UserIdValidationResponse.newBuilder().setIsUserIdThere(false).build();
            responseObserver.onNext(userIdValidationResponse);
            responseObserver.onCompleted();
        }
        else{
        log.info("userId is valid and there is user with userid "+userId);
        UserIdValidationResponse userIdValidationResponse = UserIdValidationResponse.newBuilder().setIsUserIdThere(true).build();
        responseObserver.onNext(userIdValidationResponse);
        responseObserver.onCompleted();}}
        catch(IllegalStateException ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void getPasswordFromDB(PasswordFromDBRequest request, StreamObserver<PasswordFromDBResponse> responseObserver) {
        String userId = request.getUserId();
        Optional<UserDB> userDB = userDBRepository.findById(userId);
        String password = userDB.get().getPassword();

        PasswordFromDBResponse passwordFromDBResponse = PasswordFromDBResponse.newBuilder().setPassword(password).build();
        responseObserver.onNext(passwordFromDBResponse);
        responseObserver.onCompleted();



    }
}
