package org.saurabh.service;

import akka.actor.Actor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.saurabh.Entity.HeartbeatDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.saurabh.service.SingletonActorMapService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class MapServiceForTimeKeeping {
    @Autowired
    SessionForWebSocketService sessionForWebSocketService;

    private HashMap<String, HeartbeatDetails> userInfoMap;

    public MapServiceForTimeKeeping(HashMap<String, HeartbeatDetails> userInfoMap) {
        this.userInfoMap = userInfoMap;
    }


@Scheduled(fixedRate = 10000) // method to run every 10 seconds
public void checkStatusPeriodically() {
    long currentTime = System.currentTimeMillis();

    // Iterate through userInfoMap
    for (Map.Entry<String, HeartbeatDetails> entry : userInfoMap.entrySet()) {
        String key = entry.getKey();
        HeartbeatDetails details = entry.getValue();
        String[] S= key.split("-");

        long lastHeartbeatTime = details.getHeartbeat();
        String status = details.getStatus();

        if (status.equalsIgnoreCase("CONNECTED") && (currentTime - lastHeartbeatTime > 30000)) {

            updateStatus(key,"CLOSE",lastHeartbeatTime);
            ObjectNode obj = new ObjectMapper().createObjectNode();
            //obj.put("BCM","CLOSE-CONNECTION").put("userId",data[0]);
            //SingleActorMaptonService.getInstance().getActorRef(key).tell(obj, Actor.noSender());
            SingletonActorMapService.getInstance().getActorRef(key).tell(obj,Actor.noSender());
        }
        if(status.equalsIgnoreCase("CLOSE")&&currentTime-lastHeartbeatTime>120000){
            //log user out

           sessionForWebSocketService.logUserOut(S[0],S[2]);
            removeUserHeartbeat(key);
            System.out.println("User Removed!");
        }
    }
}

    public synchronized Long getHeartbeat(String key){
        return userInfoMap.get(key).getHeartbeat();
    }

    public synchronized String getStatus(String key){
        if(userInfoMap.get(key)!=null){
            userInfoMap.get(key).getStatus();
        }
        return null;
    }
//    public Set<String> getKeySet(){
//        return userInfoMap.keySet();
//    }

    public synchronized void updateStatus(String key,String status,Long time){
        HeartbeatDetails heartbeatDetails = userInfoMap.get(key);
        heartbeatDetails.setStatus(status);
        heartbeatDetails.setHeartbeat(time);
        userInfoMap.put(key,heartbeatDetails);
    }

    public synchronized void updateHeartbeat(String uniqueKey ){
        HeartbeatDetails heartbeatDetails = userInfoMap.get(uniqueKey);
        if (heartbeatDetails==null){
            heartbeatDetails = new HeartbeatDetails(System.currentTimeMillis(),"CONNECTED");
        }
        else{
            heartbeatDetails.setHeartbeat(System.currentTimeMillis());
        }
        userInfoMap.put(uniqueKey,heartbeatDetails);
    }

    public synchronized void removeUserHeartbeat(String key){
        userInfoMap.remove(key);
    }

    public synchronized boolean isUserThere(String key){
        if(userInfoMap.containsKey(key))
            return true;
        return false;
    }



}






