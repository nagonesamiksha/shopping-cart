package com.saurabh.service;

import akka.actor.ActorRef;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SingletonActorMapService {

    private static SingletonActorMapService singletonActorMapService = null;
    private ConcurrentMap<String, ActorRef> actorMap;

    public SingletonActorMapService(ConcurrentMap<String, ActorRef> actorMap) {
        this.actorMap = actorMap;
    }

    public static SingletonActorMapService getInstance(){
        if(singletonActorMapService==null){
            singletonActorMapService = new SingletonActorMapService(new ConcurrentHashMap<>());
        }
        return singletonActorMapService;
    }

    public synchronized void saveActorRef(String key,ActorRef actorRef){
        actorMap.put(key,actorRef);
    }

    public synchronized ActorRef getActorRef(String key){
        return this.actorMap.get(key);
    }

    public synchronized void removerActorRef(String key){
        this.actorMap.remove(key);
    }
}
