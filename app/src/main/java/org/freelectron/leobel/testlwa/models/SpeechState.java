package org.freelectron.leobel.testlwa.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.freelectron.leobel.testlwa.models.message.Payload;

/**
 * Created by leobel on 2/20/17.
 */

public class SpeechState extends Payload {

    @JsonProperty("token")
    private String token;
    @JsonProperty("offsetInMilliseconds")
    private Long offsetInMilliseconds;
    @JsonProperty("playerActivity")
    private String playerActivity;

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("offsetInMilliseconds")
    public Long getOffsetInMilliseconds() {
        return offsetInMilliseconds;
    }

    @JsonProperty("offsetInMilliseconds")
    public void setOffsetInMilliseconds(Long offsetInMilliseconds) {
        this.offsetInMilliseconds = offsetInMilliseconds;
    }

    @JsonProperty("playerActivity")
    public String getPlayerActivity() {
        return playerActivity;
    }

    @JsonProperty("playerActivity")
    public void setPlayerActivity(String playerActivity) {
        this.playerActivity = playerActivity;
    }

}