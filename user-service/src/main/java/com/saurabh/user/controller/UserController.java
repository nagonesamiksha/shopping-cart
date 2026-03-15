package com.saurabh.user.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.user.entity.*;
import com.saurabh.user.exception.EmailNotSentException;
import com.saurabh.user.exception.UserAlreadyThereException;

import com.saurabh.user.service.KafkaProducerForWelcomeEmail;
import com.saurabh.user.service.UserFormation;
//import com.saurabh.user.service.VerifyOtp;
//import com.saurabh.user.service.UserServiceForLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserFormation userService;

    @PostMapping("/signup")
    public ResponseEntity<ResponseData> makeUser(@RequestBody String user) {
        User1 user1 = new User1();
        //user1.setName();
        try {
            JsonNode json = new ObjectMapper().readTree(user);

            user1.setName(json.get("name").asText());
            user1.setEmail(json.get("email").asText());
            user1.setDOB(json.get("DOB").asText());
            user1.setContact(json.get("contact").asText());

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        try {

            String savedUser1 = userService.createUser(user1);
            ResponseData responseData =new ResponseData(200, new DataObject(savedUser1));
            return ResponseEntity.ok(responseData);
        } catch (UserAlreadyThereException e) {
            ResponseData responseData = new ResponseData(30,new DataObject(null));

            return ResponseEntity.ok(responseData);

        } catch (EmailNotSentException e) {
            ResponseData responseData = new ResponseData(220,new DataObject(null));
            //ResponseData responseData = ResponseData.builder().code(220).dataObject(null).build();
            return ResponseEntity.ok(responseData);
        }
        catch (Exception e)
        {
            ResponseData responseData = new ResponseData(0,new DataObject(null));
            //ResponseData responseData = ResponseData.builder().code(0).dataObject(null).build();
            return ResponseEntity.ok(responseData);
        }

    }

    @PostMapping("/validateotp")
    public ResponseEntity<ResponseCode> checkOtp(@RequestBody String otpDetails) {

        IdAndOtp idAndOtp1 = new IdAndOtp();
        try {
            JsonNode json = new ObjectMapper().readTree(otpDetails);

            idAndOtp1.setUserId(json.get("userId").asText());
            idAndOtp1.setOTP(json.get("OTP").asText());

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String id1 = idAndOtp1.getUserId();
        String otp1 = idAndOtp1.getOTP();

        try {
            String code = userService.validateOtp(id1, otp1);
            return new ResponseEntity<ResponseCode>(new ResponseCode(Integer.parseInt(code)), HttpStatus.OK);
        } catch (Exception exe) {
            return new ResponseEntity<ResponseCode>(new ResponseCode(0), HttpStatus.OK);
        }
    }






    @PostMapping("/addUserDetails")
    public ResponseEntity<ResponseCode> sendData(@RequestBody IdAndPassword idAndPassword) {
        String id = idAndPassword.getUserId();
        String password = idAndPassword.getPassword();

        try {
            User1 user1 = userService.getUserInfo(id);

            if (user1 == null) {
                System.out.println(user1);
                System.out.println("hey i am in the null block");
                return new ResponseEntity<ResponseCode>(new ResponseCode(0), HttpStatus.OK);
            }
            boolean isUserStored = userService.sendUserDataToDB(new UserInformation(id, user1.getName(), user1.getEmail(), user1.getContact(), user1.getDOB(), password));
            if (!isUserStored) {
                System.out.println("Checking whether user is stored or not" + isUserStored);

                return new ResponseEntity<ResponseCode>(new ResponseCode(0), HttpStatus.OK);
            }
            boolean isCacheErased = userService.eraseUser(id);
            if (!isCacheErased) {
                System.out.println("Checking is cache erased" + isCacheErased);
                return new ResponseEntity<ResponseCode>(new ResponseCode(0), HttpStatus.OK); //do check this use of this code
            }
            //from friday!
            KafkaProducerForWelcomeEmail.sendWelcomeEmail(user1.getEmail());
            return new ResponseEntity<ResponseCode>(new ResponseCode(200), HttpStatus.OK);
        } catch (Exception ex) {
            System.out.println("Help,we have a problem");
            return new ResponseEntity<ResponseCode>(new ResponseCode(0), HttpStatus.OK);
        }
    }
}



