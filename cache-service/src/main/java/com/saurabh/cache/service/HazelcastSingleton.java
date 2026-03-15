package com.saurabh.cache.service;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastSingleton {
    private static HazelcastInstance hazelcastInstance;

    private HazelcastSingleton(){
        //this constructor is there for preventing external instantiation
    }
    public static synchronized HazelcastInstance getHazelcastInstance(){
        if(hazelcastInstance==null)
        {
            Config config = new Config();
            hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        }
        return hazelcastInstance;
    }
}
