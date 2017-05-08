package org.freelectron.leobel.testlwa.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.freelectron.leobel.testlwa.models.message.Payload;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by leobel on 2/20/17.
 */

public class AlertsState extends Payload {

    @JsonProperty("allAlerts")
    private List<AllAlert> allAlerts = new ArrayList<>();
    @JsonProperty("activeAlerts")
    private List<ActiveAlert> activeAlerts = new ArrayList<>();

    @JsonProperty("allAlerts")
    public List<AllAlert> getAllAlerts() {
        return allAlerts;
    }

    @JsonProperty("allAlerts")
    public void setAllAlerts(List<AllAlert> allAlerts) {
        this.allAlerts = allAlerts;
    }

    @JsonProperty("activeAlerts")
    public List<ActiveAlert> getActiveAlerts() {
        return activeAlerts;
    }

    @JsonProperty("activeAlerts")
    public void setActiveAlerts(List<ActiveAlert> activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

}