package com.saurabh.service;

import com.saurabh.entity.CartData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CartDataMapSingleton {
    private Map<String, ArrayList<CartData>> cartDataMap;
    private static CartDataMapSingleton cartDataMapSingleton;

    private CartDataMapSingleton() {
        // Private constructor to prevent instantiation
        this.cartDataMap = new ConcurrentHashMap<>();
    }

    public Map<String, ArrayList<CartData>> getCartDataMap() {

        return cartDataMap;
    }
    public static CartDataMapSingleton getInstance(){
        if(cartDataMapSingleton==null){
            cartDataMapSingleton= new CartDataMapSingleton();
        }
        return cartDataMapSingleton;
    }
}

