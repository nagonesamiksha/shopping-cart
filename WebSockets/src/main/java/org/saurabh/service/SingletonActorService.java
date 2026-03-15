package org.saurabh.service;



import akka.actor.ActorSystem;

public class SingletonActorService {

    private static SingletonActorService singletonActorService = null;

    private ActorSystem actorSystem;

    private SingletonActorService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    public static SingletonActorService getInstance(){
        if(singletonActorService==null){
            singletonActorService = new SingletonActorService(ActorSystem.create());
        }
        return singletonActorService;
    }

    public ActorSystem getActorSystem(){
        return this.actorSystem;
    }

}


/*

*/