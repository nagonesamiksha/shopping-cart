package com.saurabh.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import lombok.Data;
@Data
@NoArgsConstructor
@AllArgsConstructor


    public class LoginData {
      private   String userId; // change to userId
      private   String password;
      private   String loginDevice; // change to loginDevice
    }
//{ "userId": "user016", "password": "Psh@190000", "loginDevice": "Web"}
