package org.saurabh.Entity;


import java.io.Serializable;

public class HeartbeatDetails implements Serializable {

    private Long heartbeat;
    private String status;

    public HeartbeatDetails(Long heartbeat, String status) {
        this.heartbeat = heartbeat;
        this.status = status;
    }

    public Long getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Long heartbeat) {
        this.heartbeat = heartbeat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
