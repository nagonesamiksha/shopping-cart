package com.saurabh.service;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;


public class UserProductMapSingleton {
    private  Map<String, Queue<String>> userProductMap;
    private static UserProductMapSingleton userProductMapSingleton;

    private UserProductMapSingleton() {
        // Private constructor to prevent instantiation
        this.userProductMap = new HashMap<>();
    }

    public Map<String, Queue<String>> getUserProductMap() {

        return userProductMap;
}
    public static UserProductMapSingleton getInstance(){
        if(userProductMapSingleton==null){
            userProductMapSingleton= new UserProductMapSingleton();
        }
        return userProductMapSingleton;
    }
}
