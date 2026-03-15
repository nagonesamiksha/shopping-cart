package com.saurabh.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogoutData {
    private String userId; //userId
    private String sessionId; //sessionId
}
