package org.freelectron.leobel.testlwa.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.freelectron.leobel.testlwa.models.message.Payload;

import java.io.Serializable;

/**
 * Created by leobel on 2/21/17.
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SpeechRecognizer extends Payload {

    public static final String CLOSE_TALK_PROFILE = "CLOSE_TALK";
    public static final String NEAR_FIELD_PROFILE =  "NEAR_FIELD";
    public static final String FAR_FIELD_PROFILE = "FAR_FIELD";

    public static final String AUDIO_L16_FORMAT = "AUDIO_L16_RATE_16000_CHANNELS_1";

    @JsonProperty("profile")
    private String profile;

    @JsonProperty("format")
    private String format;

    @JsonProperty("timeoutInMilliseconds")
    private Long timeoutInMilliseconds;

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getTimeoutInMilliseconds() {
        return timeoutInMilliseconds;
    }

    public void setTimeoutInMilliseconds(Long timeoutInMilliseconds) {
        this.timeoutInMilliseconds = timeoutInMilliseconds;
    }
}
