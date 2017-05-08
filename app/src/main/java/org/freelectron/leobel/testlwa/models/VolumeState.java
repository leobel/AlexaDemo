package org.freelectron.leobel.testlwa.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.freelectron.leobel.testlwa.models.message.Payload;

/**
 * Created by leobel on 2/20/17.
 */

public class VolumeState extends Payload {

    @JsonProperty("volume")
    private Long volume;
    @JsonProperty("muted")
    private Boolean muted;

    @JsonProperty("volume")
    public Long getVolume() {
        return volume;
    }

    @JsonProperty("volume")
    public void setVolume(Long volume) {
        this.volume = volume;
    }

    @JsonProperty("muted")
    public Boolean getMuted() {
        return muted;
    }

    @JsonProperty("muted")
    public void setMuted(Boolean muted) {
        this.muted = muted;
    }

}