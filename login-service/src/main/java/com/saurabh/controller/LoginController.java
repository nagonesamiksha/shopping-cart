package com.saurabh.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.entity.*;
//import com.saurabh.service.KProducer;
import com.saurabh.service.LogService;
import com.saurabh.service.UserCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/user")
public class LoginController {
    @Autowired
    LogService logService;
    //    @Autowired
//    KProducer kProducer;
    @Autowired
    UserCheckService userCheckService;
    int count =0;


    @PostMapping("/login")
    public ResponseEntity<LoginDataDto> loginUser(@RequestBody LoginDataFromUser loginDataFromUser) {
       userCheckService.UserPasswordAttemptCount.put(loginDataFromUser.getUserId(),count);

        System.out.println("************************************Login Data******************************************");
        //userCheckService.UserPasswordAttemptCount.put(loginDataFromUser.getUserId(), count);
        System.out.println("************************************After Login Data******************************************");

        boolean isUserValid = userCheckService.checkIfUserIdIsValid(loginDataFromUser.getUserId());
        if (!isUserValid) {
            System.out.println(isUserValid);

            return new ResponseEntity<>(new LoginDataDto(201, null), HttpStatus.OK);
        } else {
            String passwordFromDB = userCheckService.getPasswordFromDB(loginDataFromUser.getUserId());
            System.out.println(passwordFromDB);
            //if sessionStatus is true then user is already logged in!
            System.out.println("********************SessionStatus is*********************");


            if (loginDataFromUser.getPassword().equals(passwordFromDB)) {
                //userCheckService.UserPasswordAttemptCount.put(loginDataFromUser.getUserId(), 0);
                boolean sessionStatus = userCheckService.isSessonAlreadyExists(loginDataFromUser.getUserId(), loginDataFromUser.getLoginDevice());
                System.out.println("********************After hitting userCheckService*********************"+sessionStatus);
                if(!sessionStatus) {
                    String userName = logService.getNameFromDB(loginDataFromUser.getUserId());
                    String sessonId = "Session" + "-" + loginDataFromUser.getLoginDevice();
                    return new ResponseEntity<>(new LoginDataDto(200, new DataObject(sessonId, userName)), HttpStatus.OK);
                }
                return new ResponseEntity<>(new LoginDataDto(204, new DataObject()), HttpStatus.OK);
            }

            if (!loginDataFromUser.getPassword().equals(passwordFromDB)) {

                count = userCheckService.UserPasswordAttemptCount.get(loginDataFromUser.getUserId());
                count++;
                userCheckService.UserPasswordAttemptCount.put(loginDataFromUser.getUserId(), count);

                if (count >= 3) {
                    return new ResponseEntity<>(new LoginDataDto(205, new DataObject()), HttpStatus.OK);

                } else {
                    return new ResponseEntity<>(new LoginDataDto(202, new DataObject()), HttpStatus.OK);
                }

            }

        }
        return new ResponseEntity<>(new LoginDataDto(0, new DataObject()), HttpStatus.OK);
    }
    /*Response to UI if userID is invalid.
{"code": 201," dataObject ": null}

Response to UI if password is not valid.
{"code": 202," dataObject ": null}

Response to UI if active session exists on this device.
{"code": 204," dataObject ": null}

Response to UI if Invalid credentials 3 consecutive times.
{"code": 205," dataObject ": null}

Response to UI if successful login
{"code": 200," dataObject ": {"sessionId":"3",”name”: “John Doe”}}

Response to UI default response if any unexpected error happens
{"code": 0," dataObject ": null}*/

/*

    @PostMapping("/login")
    public ResponseEntity<UserRes1> loginUser(@RequestBody LoginData loginData) {
        System.out.println("Password:"+loginData.getPassword() +"userid:"+loginData.getUserId() +"device:"+loginData.getLoginDevice());
        ResData1 resData1 = logService.checkUser(loginData.getUserId(), loginData.getPassword());
        UserRes1 userRes1 = new UserRes1();
//
//        if (resData1.getCode() == 201)
//        {
//            return new ResponseEntity<>(new UserRes1(201,null),HttpStatus.OK);
//
//        }
//        if (resData1.getCode() == 202)
//        {
//            return new ResponseEntity<>(new UserRes1(202,null),HttpStatus.OK);
//
//        }
//        if (resData1.getCode() == 205)
//        {
//            return new ResponseEntity<>(new UserRes1(205,null),HttpStatus.OK);
//
//        }

        if (resData1.getCode() == 200) {
            System.out.println("userid:"+loginData.getUserId() +"device:"+loginData.getLoginDevice());

            userRes1 = logService.checkSession(loginData.getUserId(), loginData.getLoginDevice());
           return new ResponseEntity<>(userRes1,HttpStatus.OK);
        }
        userRes1.setCode(resData1.getCode());
        return new ResponseEntity<>(userRes1, HttpStatus.OK);
    }


    @PostMapping("/logout")
    public ResponseEntity<ResData> logoutUser(@RequestBody LogoutData logoutData)
    {

        int isUserLoggedOut =  logService.logUserOutSession(logoutData.getUserId(),logoutData.getSessionId());
        if(isUserLoggedOut==200)
        {
            return new ResponseEntity<>(new ResData(200,null),HttpStatus.OK);
        }
         return new ResponseEntity<>(new ResData(0,null),HttpStatus.OK);
    }*/


    @PostMapping("/logout")
    public ResponseEntity<ResData> logUserOut (@RequestBody LogoutData logoutData)
    {
        String device = logService.extractDeviceFromSessionId(logoutData.getSessionId());
        boolean logoutUser= userCheckService.logUserOff(logoutData.getUserId(),device);
        if(logoutUser)
        {
            return new ResponseEntity<>(new ResData(200,null),HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResData(199,null),HttpStatus.OK);

    }


    @PostMapping("/forgotPassword")
    public ResponseEntity<ResData> resetPassword(@RequestBody EmailData emailData)
    {
//        System.out.println("email data"+emailData.getEmail());
        int isEmailIdValid = logService.checkEmail(emailData.getEmail());
        if (isEmailIdValid==200)
        {
            logService.sendOTPToUser(emailData.getEmail());
            return new ResponseEntity<>(new ResData(200,null),HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResData(0,null),HttpStatus.OK);
    }

    @PostMapping("/validateOTPForForgotPassword")
    //public ResponseEntity<ResData> validateOTP(@RequestBody ValidateOtpDto validateOtpDto)
    public ResponseEntity<ResData> validateOTP(@RequestBody String userOtpData )
    {
        ValidateOtpDto validateOtpDto = new ValidateOtpDto();
        try {
            JsonNode json = new ObjectMapper().readTree(String.valueOf(userOtpData));

            validateOtpDto.setEmail(json.get("email").asText());
            validateOtpDto.setOTP(json.get("OTP").asText());


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        int code =logService.checkOTP(validateOtpDto.getEmail(),validateOtpDto.getOTP());
        return new ResponseEntity<>(new ResData(code,null),HttpStatus.OK);
    }


    @PostMapping("/changePassword")
    public ResponseEntity<ResData> updatePassword(@RequestBody UpdatePasswordDto updatePasswordDto)
    {
        boolean isUserErasedFromCache = logService.eraseUser(updatePasswordDto.getEmail());

       int code= logService.updatePasswordInDB(updatePasswordDto.getEmail(), updatePasswordDto.getPassword());
        return new ResponseEntity<>(new ResData(code,null),HttpStatus.OK);
    }
}








