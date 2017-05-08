package org.freelectron.leobel.testlwa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by leobel on 2/20/17.
 */

public class ActiveAlert implements Serializable {

    @JsonProperty("token")
    private String token;
    @JsonProperty("type")
    private String type;
    @JsonProperty("scheduledTime")
    private String scheduledTime;

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("scheduledTime")
    public String getScheduledTime() {
        return scheduledTime;
    }
}
