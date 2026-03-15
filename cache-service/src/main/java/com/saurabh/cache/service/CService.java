package com.saurabh.cache.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.saurabh.CacheEntity.User;
import com.saurabh.CacheEntity.UserOtpDetails;

import com.saurabh.grpc.cservice.*;
import com.saurabh.grpc.cservice.CheckOTPForForgotPasswordRequest;
import com.saurabh.grpc.cservice.CheckOTPForForgotPasswordResponse;
import com.saurabh.grpc.cservice.EraseUserForForgotPasswordRequest;
import com.saurabh.grpc.cservice.EraseUserForForgotPasswordResponse;
import com.saurabh.grpc.cservice.EraseUserRequest;
import com.saurabh.grpc.cservice.EraseUserResponse;
import com.saurabh.grpc.cservice.GetUserDetails;
import com.saurabh.grpc.cservice.GetUserDetailsRequest;
import com.saurabh.grpc.cservice.IsOtpCorrect;
import com.saurabh.grpc.cservice.ResponseOfOtp;
import com.saurabh.grpc.cservice.StoreOtpForResetPasswordRequest;
import com.saurabh.grpc.cservice.StoreOtpForResetPasswordResponse;
import com.saurabh.grpc.cservice.UserDetailResponse;
import com.saurabh.grpc.cservice.UserDetails;
import com.saurabh.grpc.cservice.UserOtp;
import io.grpc.stub.StreamObserver;

import net.devh.boot.grpc.server.service.GrpcService;


@GrpcService
public class CService extends com.saurabh.grpc.cservice.CacheServiceGrpc.CacheServiceImplBase {
    @Override
    public void getUserDetailsFromCache(GetUserDetailsRequest request, StreamObserver<GetUserDetails> responseObserver) {
        //System.out.println("Hello Config");

        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();
        IMap<String, User> map = hzClusterInstance1.getMap("userMap");
        User user = map.get(request.getUserid());
        System.out.println("I am in cache service" + user);
        GetUserDetails getUserDetails;
        if (user == null) {
            getUserDetails = GetUserDetails.newBuilder().setStatus(false).build();//

        } else {
            getUserDetails = GetUserDetails.newBuilder().setName(user.getName()).setEmail(user.getEmail()).setContact(user.getContact()).setDob(user.getDob()).setStatus(true).build();
        }
        responseObserver.onNext(getUserDetails);
        responseObserver.onCompleted();
    }

    @Override
    public void sendUserDetails(UserDetails request, StreamObserver<UserDetailResponse> responseObserver) {

        String userId = request.getUserid();
        String name = request.getName();
        String email = request.getEmail();
        String contact = request.getContact();
        String dob = request.getDob();

        User user = new User(name, email, contact, dob);
        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();

        IMap<String, User> map = hzClusterInstance1.getMap("userMap");
        map.put(userId, user);
        System.out.println(map);
        System.out.println("Stored userId : "+userId+" "+user);
        responseObserver.onNext(UserDetailResponse.newBuilder().setResponse(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void eraseUserDetailsFromCache(EraseUserRequest request, StreamObserver<EraseUserResponse> responseObserver) {

        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();

        IMap<String, User> map = hzClusterInstance1.getMap("userMap");
        map.remove(request.getUserId());
        EraseUserResponse removedUser = EraseUserResponse.newBuilder().setIsUserErased(true).build();
        responseObserver.onNext(removedUser);
        responseObserver.onCompleted();

    }

    @Override
    public void storeUserOtpForResetPassword(StoreOtpForResetPasswordRequest request, StreamObserver<StoreOtpForResetPasswordResponse> responseObserver) {
        String email = request.getEmail();
        String otp = request.getOtp();
        System.out.println("email is"+email+ "otp is"+otp);
        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();

        IMap<String, String> map2 = hzClusterInstance1.getMap("userOtpMapForForgotPassword");
        map2.put(email, otp);
        responseObserver.onNext(StoreOtpForResetPasswordResponse.newBuilder().setIsOtpForResetPasswordStored(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendUserIdAndOtp(UserOtp request, StreamObserver<ResponseOfOtp> responseObserver) {
        String userId = request.getUserid();
        String otp = request.getOtp();

        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();

        IMap<String, UserOtpDetails> map1 = hzClusterInstance1.getMap("userOtpMap");
        map1.put(userId, new UserOtpDetails(otp, 0));
        responseObserver.onNext(ResponseOfOtp.newBuilder().setIsStored(true).build());
        responseObserver.onCompleted();

    }

    @Override
    public void checkOtp(UserOtp request, StreamObserver<IsOtpCorrect> responseObserver) {
        String userId = request.getUserid();
        String otp = request.getOtp();
        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();

        IMap<String, UserOtpDetails> map1 = hzClusterInstance1.getMap("userOtpMap");
        UserOtpDetails storedOtp = map1.get(userId);
        IsOtpCorrect response;

        if (storedOtp == null) {
            response = IsOtpCorrect.newBuilder().setCode("1999").build();
        } else {
            if (storedOtp.getOtp().equals(otp)) {
                response = IsOtpCorrect.newBuilder().setCode("500").build();
            } else {
                if (storedOtp.getErrorCount() < 2) {
                    response = IsOtpCorrect.newBuilder().setCode("502").build();
                    storedOtp.setErrorCount(storedOtp.getErrorCount() + 1);
                    map1.put(userId, storedOtp);
                } else {
                    response = IsOtpCorrect.newBuilder().setCode("301").build();

                }
            }
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void checkOTPForResetPassword(CheckOTPForForgotPasswordRequest request, StreamObserver<CheckOTPForForgotPasswordResponse> responseObserver) {
        String email = request.getEmail();
        String otp = request.getOtp();
        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();

        IMap<String, String> map1 = hzClusterInstance1.getMap("userOtpMapForForgotPassword");
        String otp1= map1.get(email);
        CheckOTPForForgotPasswordResponse response = CheckOTPForForgotPasswordResponse.newBuilder().build();
        System.out.println(otp1);

        if(otp.equals(otp1))
        {
            response= CheckOTPForForgotPasswordResponse.newBuilder().setCode(200).build();

        }
        else
        {
            response=CheckOTPForForgotPasswordResponse.newBuilder().setCode(199).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void cleanData(EraseUserForForgotPasswordRequest request, StreamObserver<EraseUserForForgotPasswordResponse> responseObserver) {
        HazelcastInstance hzClusterInstance1 = HazelcastSingleton.getHazelcastInstance();

        IMap<String, User> map = hzClusterInstance1.getMap("userOtpMapForForgotPassword");
        map.remove(request.getEmail());

        EraseUserForForgotPasswordResponse eraseUserForForgotPasswordResponse = EraseUserForForgotPasswordResponse.newBuilder().setIsDataCleaned(true).build();
        responseObserver.onNext(eraseUserForForgotPasswordResponse);
        responseObserver.onCompleted();

    }
}

//package com.saurabh.cache.service;
//
//import com.hazelcast.config.Config;
//import com.hazelcast.core.Hazelcast;
//import com.hazelcast.core.HazelcastInstance;
//import com.hazelcast.map.IMap;
//import com.saurabh.CacheEntity.User;
//import com.saurabh.CacheEntity.UserOtpDetails;
//import com.saurabh.grpc.cservice.*;
//import io.grpc.stub.StreamObserver;
//import net.devh.boot.grpc.server.service.GrpcService;
//
//
//@GrpcService
//public class CService extends CacheServiceGrpc.CacheServiceImplBase {
//    @Override
//    public void getUserDetailsFromCache(GetUserDetailsRequest request, StreamObserver<GetUserDetails> responseObserver) {
//        System.out.println("Hello Config");
////        System.out.println("Hello Config");
////        System.out.println("Hello Config");
//        Config config1 = new Config();
//        HazelcastInstance hzClusterInstance1 = Hazelcast.newHazelcastInstance(config1);
//        IMap<String, User> map = hzClusterInstance1.getMap("userMap");
//        User user = map.get(request.getUserid());
//        System.out.println("I am in cache service" + user);
//        GetUserDetails getUserDetails;
//        if (user == null) {
//            getUserDetails = GetUserDetails.newBuilder().setStatus(false).build();
//
//        } else {
//            getUserDetails = GetUserDetails.newBuilder().setName(user.getName()).setEmail(user.getEmail()).setContact(user.getContact()).setDob(user.getDob()).setStatus(true).build();
//        }
//        responseObserver.onNext(getUserDetails);
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void sendUserDetails(UserDetails request, StreamObserver<UserDetailResponse> responseObserver) {
//
//        String userId = request.getUserid();
//        String name = request.getName();
//        String email = request.getEmail();
//        String contact = request.getContact();
//        String dob = request.getDob();
//
//        User user = new User(name, email, contact, dob);
//        Config config = new Config();
//
//        HazelcastInstance hzClusterInstance = Hazelcast.newHazelcastInstance(config);
//        IMap<String, User> map = hzClusterInstance.getMap("userMap");
//        map.put(userId, user);
//        System.out.println(map);
//        System.out.println("Stored userId : "+userId+" "+user);
//        responseObserver.onNext(UserDetailResponse.newBuilder().setResponse(true).build());
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void eraseUserDetailsFromCache(EraseUserRequest request, StreamObserver<EraseUserResponse> responseObserver) {
//
//        Config config = new Config();
//
//        HazelcastInstance hzClusterInstance = Hazelcast.newHazelcastInstance(config);
//        IMap<String, User> map = hzClusterInstance.getMap("userMap");
//        map.remove(request.getUserId());
//        EraseUserResponse removedUser = EraseUserResponse.newBuilder().setIsUserErased(true).build();
//        responseObserver.onNext(removedUser);
//        responseObserver.onCompleted();
//
//    }
//
//    @Override
//    public void storeUserOtpForResetPassword(StoreOtpForResetPasswordRequest request, StreamObserver<StoreOtpForResetPasswordResponse> responseObserver) {
//        String email = request.getEmail();
//        String otp = request.getOtp();
//        System.out.println("email is"+email+ "otp is"+otp);
//        Config config2 = new Config();
//        HazelcastInstance hzClusterInstance2 = Hazelcast.newHazelcastInstance(config2);
//        IMap<String, String> map2 = hzClusterInstance2.getMap("userOtpMapForForgotPassword");
//        map2.put(email, otp);
//        responseObserver.onNext(StoreOtpForResetPasswordResponse.newBuilder().setIsOtpForResetPasswordStored(true).build());
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void sendUserIdAndOtp(UserOtp request, StreamObserver<ResponseOfOtp> responseObserver) {
//        String userId = request.getUserid();
//        String otp = request.getOtp();
//
//        Config config1 = new Config();
//        HazelcastInstance hzClusterInstance1 = Hazelcast.newHazelcastInstance(config1);
//        IMap<String, UserOtpDetails> map1 = hzClusterInstance1.getMap("userOtpMap");
//        map1.put(userId, new UserOtpDetails(otp, 0));
//        responseObserver.onNext(ResponseOfOtp.newBuilder().setIsStored(true).build());
//        responseObserver.onCompleted();
//
//    }
//
//    @Override
//    public void checkOtp(UserOtp request, StreamObserver<IsOtpCorrect> responseObserver) {
//        String userId = request.getUserid();
//        String otp = request.getOtp();
//        Config config1 = new Config();
//        HazelcastInstance hzClusterInstance1 = Hazelcast.newHazelcastInstance(config1);
//        IMap<String, UserOtpDetails> map1 = hzClusterInstance1.getMap("userOtpMap");
//        UserOtpDetails storedOtp = map1.get(userId);
//        IsOtpCorrect response;
//
//        if (storedOtp == null) {
//            response = IsOtpCorrect.newBuilder().setCode("1999").build();
//        } else {
//            if (storedOtp.getOtp().equals(otp)) {
//                response = IsOtpCorrect.newBuilder().setCode("500").build();
//            } else {
//                if (storedOtp.getErrorCount() < 2) {
//                    response = IsOtpCorrect.newBuilder().setCode("502").build();
//                    storedOtp.setErrorCount(storedOtp.getErrorCount() + 1);
//                    map1.put(userId, storedOtp);
//                } else {
//                    response = IsOtpCorrect.newBuilder().setCode("301").build();
//
//                }
//            }
//        }
//        responseObserver.onNext(response);
//        responseObserver.onCompleted();
//
//    }
//
//    @Override
//    public void checkOTPForResetPassword(CheckOTPForForgotPasswordRequest request, StreamObserver<CheckOTPForForgotPasswordResponse> responseObserver) {
//        String email = request.getEmail();
//        String otp = request.getOtp();
//        Config config1 = new Config();
//        HazelcastInstance hzClusterInstance1 = Hazelcast.newHazelcastInstance(config1);
//        IMap<String, String> map1 = hzClusterInstance1.getMap("userOtpMapForForgotPassword");
//        String otp1= map1.get(email);
//        CheckOTPForForgotPasswordResponse response = CheckOTPForForgotPasswordResponse.newBuilder().build();
//        System.out.println(otp1);
//
//        if(otp.equals(otp1))
//        {
//           response= CheckOTPForForgotPasswordResponse.newBuilder().setCode(200).build();
//
//        }
//        else
//        {
//            response=CheckOTPForForgotPasswordResponse.newBuilder().setCode(199).build();
//        }
//        responseObserver.onNext(response);
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void cleanData(EraseUserForForgotPasswordRequest request, StreamObserver<EraseUserForForgotPasswordResponse> responseObserver) {
//        Config config = new Config();
//
//        HazelcastInstance hzClusterInstance = Hazelcast.newHazelcastInstance(config);
//        IMap<String, User> map = hzClusterInstance.getMap("userOtpMapForForgotPassword");
//        map.remove(request.getEmail());
//
//        EraseUserForForgotPasswordResponse eraseUserForForgotPasswordResponse = EraseUserForForgotPasswordResponse.newBuilder().setIsDataCleaned(true).build();
//        responseObserver.onNext(eraseUserForForgotPasswordResponse);
//        responseObserver.onCompleted();
//
//    }
//}