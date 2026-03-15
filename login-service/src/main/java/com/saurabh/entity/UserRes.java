package com.saurabh.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRes {
    private String sessionId;
    private String userId;
    private int code;
}
