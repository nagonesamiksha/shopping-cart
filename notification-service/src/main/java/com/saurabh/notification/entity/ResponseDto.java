package com.saurabh.notification.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor

public class ResponseDto{
    private boolean isOtpSent;
    private String otp;
}

