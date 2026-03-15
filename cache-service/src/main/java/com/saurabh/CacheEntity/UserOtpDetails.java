package com.saurabh.CacheEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOtpDetails implements Serializable {

    private String otp;
    private int errorCount;
}
