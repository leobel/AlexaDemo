package org.freelectron.leobel.testlwa.services;

import org.freelectron.leobel.testlwa.models.AlertsState;
import org.freelectron.leobel.testlwa.models.PlaybackState;
import org.freelectron.leobel.testlwa.models.SpeechState;
import org.freelectron.leobel.testlwa.models.VolumeState;
import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;

/**
 * Created by leobel on 2/17/17.
 */
public interface PreferenceService {

    void setAccessToken(String token);
    String getAccessToken();

    void setDowChannel(boolean state);
    Boolean getDownChannel();

    void setAlertsState(AlertsState alertsState);
    ComponentState getAlertsState();

    void setPlaybackState(PlaybackState playbackState);
    ComponentState getPlaybackState();

    void setVolumeState(VolumeState volumeState);
    ComponentState getVolumeState();

    void setSpeechState(SpeechState speechState);
    ComponentState getSpeechState();
}
