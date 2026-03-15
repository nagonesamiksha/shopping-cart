package com.saurabh.notification.service;

import com.saurabh.grpc.usernotificationservice.*;
import com.saurabh.grpc.usernotificationservice.EmailRequestNS;
import com.saurabh.grpc.usernotificationservice.EmailResponseNS;
import com.saurabh.grpc.usernotificationservice.NotificationServiceGrpc;
import com.saurabh.grpc.usernotificationservice.SendOtpResetPasswordRequest;
import com.saurabh.grpc.usernotificationservice.SendOtpResetPasswordResponse;
import com.saurabh.notification.entity.ResponseDto;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;


@GrpcService
public class  NService extends NotificationServiceGrpc.NotificationServiceImplBase

{
    @Override
    public void sendEmailForPasswordReset(SendOtpResetPasswordRequest request, StreamObserver<SendOtpResetPasswordResponse> responseObserver) {
        String email= request.getEmailid();
        ResponseDto isEmailSent = sendEmailToUserForForgetPassword(email);
        SendOtpResetPasswordResponse sendOtpResetPasswordResponse = SendOtpResetPasswordResponse.newBuilder().setOtp(isEmailSent.getOtp()).setIsOtpSent(isEmailSent.isOtpSent()).build();
        responseObserver.onNext(sendOtpResetPasswordResponse);
        responseObserver.onCompleted();

    }


    @Override
    public void sendEmail(EmailRequestNS request, StreamObserver<EmailResponseNS> responseObserver) {

        String otp = request.getOtp();
        String email = request.getEmail();

        boolean emailSent = sendEmailToUser(email,otp);
        EmailResponseNS responseNS =  EmailResponseNS.newBuilder().setSuccess(emailSent).build();

       responseObserver.onNext(responseNS);
       responseObserver.onCompleted();

    }


    private boolean sendEmailToUser(String recipientEmail, String otp) {
        // Email configuration properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        // SMTP account credentials
        String senderEmail = "manesaurabh264@gmail.com";
        String senderPassword = "jmvtgivwjwhimybz";

        // Create a session with SMTP server
        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("manesaurabh264@gmail.com","jmvtgivwjwhimybz" );
            }
        });

        try {

            MimeMessage message = new MimeMessage(session);


            message.setFrom(new InternetAddress(senderEmail));
            message.addRecipient( Message.RecipientType.TO, new InternetAddress(recipientEmail));


            message.setSubject("OTP Verification");


            String emailContent = "Your OTP is: " + otp+ "And your UserId is user"+otp;
            message.setText(emailContent);


            Transport.send(message);

            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ResponseDto sendEmailToUserForForgetPassword(String recipientEmail) {

        String digits ="123456";
        Random random = new Random();
        StringBuilder otpBuilder = new StringBuilder();

        for(int i=0;i<digits.length();i++)
        {
            int index = random.nextInt(digits.length());
            otpBuilder.append(digits.charAt(index));
        }


        String otp =otpBuilder.toString();
        // Email configuration properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        // SMTP account credentials
        String senderEmail = "manesaurabh264@gmail.com";
        String senderPassword = "jmvtgivwjwhimybz";

        // Create a session with SMTP server
        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("manesaurabh264@gmail.com","jmvtgivwjwhimybz" );
            }
        });

        try {

            MimeMessage message = new MimeMessage(session);


            message.setFrom(new InternetAddress(senderEmail));
            message.addRecipient( Message.RecipientType.TO, new InternetAddress(recipientEmail));


            message.setSubject("OTP Verification");


            String emailContent = "Your OTP is: " + otp;
            message.setText(emailContent);


            Transport.send(message);

            ResponseDto responseDto = new ResponseDto();
            responseDto.setOtp(otp);
            responseDto.setOtpSent(true);
            return responseDto;

        } catch (MessagingException e) {
            e.printStackTrace();
            ResponseDto responseDto = new ResponseDto();
            responseDto.setOtp(null);
            responseDto.setOtpSent(false);
            return responseDto;
        }
    }

}





