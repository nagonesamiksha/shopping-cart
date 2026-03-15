package com.saurabh.service;

//import com.saurabh.grpc.loginservice.UserCheckRequest;
//import com.saurabh.grpc.loginservice.UserCheckResponse;
import com.saurabh.entity.DataObject;
import com.saurabh.entity.ResData1;
import com.saurabh.entity.UserRes;
import com.saurabh.entity.UserRes1;
import com.saurabh.grpc.cservice.*;
import com.saurabh.grpc.cservice.CacheServiceGrpc;
import com.saurabh.grpc.cservice.CheckOTPForForgotPasswordRequest;
import com.saurabh.grpc.cservice.CheckOTPForForgotPasswordResponse;
import com.saurabh.grpc.cservice.EraseUserForForgotPasswordRequest;
import com.saurabh.grpc.cservice.EraseUserForForgotPasswordResponse;
import com.saurabh.grpc.cservice.StoreOtpForResetPasswordRequest;
import com.saurabh.grpc.cservice.StoreOtpForResetPasswordResponse;
import com.saurabh.grpc.sessionService.SessionServiceGrpc;
import com.saurabh.grpc.userdbservice.*;
import com.saurabh.grpc.userdbservice.DBServiceGrpc;
import com.saurabh.grpc.userdbservice.EmailRequest;
import com.saurabh.grpc.userdbservice.EmailResponse;
import com.saurabh.grpc.userdbservice.GetNameRequest;
import com.saurabh.grpc.userdbservice.GetNameResponse;
import com.saurabh.grpc.userdbservice.UpdatePasswordRequest;
import com.saurabh.grpc.userdbservice.UpdatePasswordResponse;
import com.saurabh.grpc.userdbservice.UserCheckRequest;
import com.saurabh.grpc.userdbservice.UserCheckResponse;
import com.saurabh.grpc.usernotificationservice.NotificationServiceGrpc;
import com.saurabh.grpc.usernotificationservice.SendOtpResetPasswordRequest;
import com.saurabh.grpc.usernotificationservice.SendOtpResetPasswordResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public class LogService {
//
//    @Autowired
//    KProducer kProducer;
    @GrpcClient("userdbservice")
    private DBServiceGrpc.DBServiceBlockingStub stubDB1;
    @GrpcClient("NService")//TO send otp to the given email in case user wants to reset the password
    private NotificationServiceGrpc.NotificationServiceBlockingStub stubN1;
    @GrpcClient("CService")
    private CacheServiceGrpc.CacheServiceBlockingStub stubC1;
    @GrpcClient("sessionService")
    private SessionServiceGrpc.SessionServiceBlockingStub stubSS;


    public ResData1 checkUser(String userid, String password)
    {
        //to make call to the database service whether user exists or not
        System.out.println("checking...."+password+"userid"+userid);
        UserCheckRequest userCheckRequest = UserCheckRequest.newBuilder().setUserid(userid).setPassword(password).build();
        System.out.println("inside logservice");
        UserCheckResponse userCheckResponse = stubDB1.userCheck(userCheckRequest);

        ResData1 resData1= new ResData1();
        resData1.setUsernameValid(userCheckResponse.getIsUserThere());
        resData1.setCode(userCheckResponse.getCode());
        return resData1;

    }


  /*  public UserRes1 checkSession(String userid, String device) {
        UserRes getUser = kProducer.getSessionStatus(userid,device);
        if (getUser.getCode()==204)
        {
            UserRes1 userRes1 = UserRes1.builder().code(getUser.getCode())
                    .dataObject(null).build();
            return userRes1;
        }
        UserRes1 userRes1 = UserRes1.builder().code(getUser.getCode())
                        .dataObject(new DataObject(getUser.getSessionId(),getNameFromDB(getUser.getUserId()))).build();

        return userRes1;
    }
    */
    public String getNameFromDB(String userid)
    {
        System.out.println(userid);
        GetNameRequest gateNameRequest = GetNameRequest.newBuilder().setUserid(userid).build();
        GetNameResponse getNameResponse = stubDB1.getName(gateNameRequest);
        return getNameResponse.getUserName();

    }
/*
    public int logUserOutSession(String userid, String sessionid) {
        int logThemOut = kProducer.logUserOut(userid,sessionid);
        return logThemOut;
    }
*/
    public int checkEmail(String emailid) {
//        CheckEmailRequest checkEmailRequest = CheckEmailRequest.newBuilder().setEmailId(emailid).build();
//        CheckEmailResponse checkEmailResponse = stubDB1.checkIfEmailExists(checkEmailRequest);

        EmailRequest emailRequest = EmailRequest.newBuilder().setEmail(emailid).build();
        EmailResponse emailResponse = stubDB1.checkEmail(emailRequest);
        if(emailResponse.getIsExists()==true)
        {
            return 200;
        }
        return 199;
    }

    public void sendOTPToUser(String emailid) {
        SendOtpResetPasswordRequest sendOtpResetPasswordRequest = SendOtpResetPasswordRequest.newBuilder().setEmailid(emailid).build();
        SendOtpResetPasswordResponse sendOtpResetPasswordResponse = stubN1.sendEmailForPasswordReset(sendOtpResetPasswordRequest);

        if(sendOtpResetPasswordResponse.getIsOtpSent()==true)
        {
            StoreOtpForResetPasswordRequest storeOtpForResetPasswordRequest = StoreOtpForResetPasswordRequest.newBuilder().setEmail(emailid)
                    .setOtp(sendOtpResetPasswordResponse.getOtp()).build();
            StoreOtpForResetPasswordResponse storeOtpForResetPasswordResponse = stubC1.storeUserOtpForResetPassword(storeOtpForResetPasswordRequest);
        }


    }

    public int checkOTP(String email, String otp) {
        CheckOTPForForgotPasswordRequest checkOTPForForgotPasswordRequest = CheckOTPForForgotPasswordRequest.newBuilder().setEmail(email).setOtp(otp).build();
        CheckOTPForForgotPasswordResponse checkOTPForForgotPasswordResponse = stubC1.checkOTPForResetPassword(checkOTPForForgotPasswordRequest);
        return checkOTPForForgotPasswordResponse.getCode();
    }

    public int updatePasswordInDB(String email, String password) {
        UpdatePasswordRequest updatePasswordRequest = UpdatePasswordRequest.newBuilder().setEmail(email).setPassword(password).build();
        UpdatePasswordResponse updatePasswordResponse = stubDB1.updatePassword(updatePasswordRequest);
        return updatePasswordResponse.getCode();
    }

    public boolean eraseUser(String email) {
        EraseUserForForgotPasswordRequest eraseUserForForgotPasswordRequest = EraseUserForForgotPasswordRequest.newBuilder().setEmail(email).build();
        EraseUserForForgotPasswordResponse eraseUserForForgotPasswordResponse = stubC1.cleanData(eraseUserForForgotPasswordRequest);
        return eraseUserForForgotPasswordResponse.getIsDataCleaned();

    }


    public String extractDeviceFromSessionId(String sessionId) {
        //int index = sessionId.indexOf("-");
        int index = sessionId.indexOf("-");
        if (index != -1) {
            return sessionId.substring(index + 1);
        } else {
            return null;
        }
    }
}
