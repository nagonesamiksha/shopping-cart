package com.saurabh.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInformation {
    private String userId;
    private String name;
    private String email;
    private String contact;
    private String dob;
    private String password;

}
