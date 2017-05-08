package org.freelectron.leobel.testlwa.services;

import android.content.SharedPreferences;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.freelectron.leobel.testlwa.models.AlertsState;
import org.freelectron.leobel.testlwa.models.PlaybackState;
import org.freelectron.leobel.testlwa.models.SpeechState;
import org.freelectron.leobel.testlwa.models.VolumeState;
import org.freelectron.leobel.testlwa.models.message.request.ComponentStateFactory;
import org.freelectron.leobel.testlwa.models.message.request.context.Alert;
import org.freelectron.leobel.testlwa.models.message.request.context.AlertsStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;
import org.freelectron.leobel.testlwa.models.message.request.context.PlaybackStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.SpeechStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.VolumeStatePayload;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by leobel on 2/17/17.
 */

public class PreferenceServiceImpl implements PreferenceService {

    private static final String PREFERENCE_TOKEN = "PREFERENCE_TOKEN";
    private static final String PREFERENCE_DOWN_CHANNEL = "PREFERENCE_DOWN_CHANNEL";
    private static final String PREFERENCE_ALERT_STATE = "PREFERENCE_ALERT_STATE";
    private static final String PREFERENCE_PLAYBACK_STATE = "PREFERENCE_PLAYBACK_STATE";
    private static final String PREFERENCE_VOLUME_STATE = "PREFERENCE_VOLUME_STATE";
    private static final String PREFERENCE_SPEECH_STATE = "PREFERENCE_SPEECH_STATE";


    private final SharedPreferences sharedPreferences;
    private final ObjectMapper objectMapper;

    public PreferenceServiceImpl(SharedPreferences sharedPreferences, ObjectMapper objectMapper){
        this.sharedPreferences = sharedPreferences;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setAccessToken(String token) {
        SharedPreferences.Editor editor =  sharedPreferences.edit();
        editor.putString(PREFERENCE_TOKEN, token);
        editor.commit();
    }

    @Override
    public String getAccessToken() {
        return sharedPreferences.getString(PREFERENCE_TOKEN, "");
    }

    @Override
    public void setDowChannel(boolean state) {
        SharedPreferences.Editor editor =  sharedPreferences.edit();
        editor.putBoolean(PREFERENCE_DOWN_CHANNEL, state);
        editor.commit();
    }

    @Override
    public Boolean getDownChannel() {
        return sharedPreferences.getBoolean(PREFERENCE_DOWN_CHANNEL, false);
    }

    @Override
    public void setAlertsState(AlertsState alertsState) {
        try {
            SharedPreferences.Editor editor =  sharedPreferences.edit();
            String credentialsJson = objectMapper.writeValueAsString(alertsState);
            editor.putString(PREFERENCE_ALERT_STATE, credentialsJson);
            editor.commit();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ComponentState getAlertsState() {
        String alertStateJson = sharedPreferences.getString(PREFERENCE_ALERT_STATE, "");
        if(alertStateJson.isEmpty()){
           return ComponentStateFactory.createAlertState(new AlertsStatePayload(new ArrayList<Alert>(), new ArrayList<Alert>()));
        }
        try {
            return objectMapper.readValue(alertStateJson, ComponentState.class);
        } catch (IOException e) {
            return ComponentStateFactory.createAlertState(new AlertsStatePayload(new ArrayList<Alert>(), new ArrayList<Alert>()));
        }
    }

    @Override
    public void setPlaybackState(PlaybackState playbackState) {
        try {
            SharedPreferences.Editor editor =  sharedPreferences.edit();
            String credentialsJson = objectMapper.writeValueAsString(playbackState);
            editor.putString(PREFERENCE_PLAYBACK_STATE, credentialsJson);
            editor.commit();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ComponentState getPlaybackState() {
        String playbackStateJson = sharedPreferences.getString(PREFERENCE_PLAYBACK_STATE, "");
        if(playbackStateJson.isEmpty()){
            return ComponentStateFactory.createPlaybackState(new PlaybackStatePayload("", 0, "IDLE"));
        }
        try {
            return objectMapper.readValue(playbackStateJson, ComponentState.class);
        } catch (IOException e) {
            return ComponentStateFactory.createPlaybackState(new PlaybackStatePayload("", 0, "IDLE"));
        }
    }

    @Override
    public void setVolumeState(VolumeState volumeState) {
        try {
            SharedPreferences.Editor editor =  sharedPreferences.edit();
            String credentialsJson = objectMapper.writeValueAsString(volumeState);
            editor.putString(PREFERENCE_VOLUME_STATE, credentialsJson);
            editor.commit();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ComponentState getVolumeState() {
        String volumeStateJson = sharedPreferences.getString(PREFERENCE_VOLUME_STATE, "");
        if(volumeStateJson.isEmpty()){
            return ComponentStateFactory.createVolumeState(new VolumeStatePayload(60, false));
        }
        try {
            return objectMapper.readValue(volumeStateJson, ComponentState.class);
        } catch (IOException e) {
            return ComponentStateFactory.createVolumeState(new VolumeStatePayload(60, false));
        }
    }

    @Override
    public void setSpeechState(SpeechState speechState) {
        try {
            SharedPreferences.Editor editor =  sharedPreferences.edit();
            String credentialsJson = objectMapper.writeValueAsString(speechState);
            editor.putString(PREFERENCE_SPEECH_STATE, credentialsJson);
            editor.commit();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ComponentState getSpeechState() {
        String speechStateJson = sharedPreferences.getString(PREFERENCE_SPEECH_STATE, "");
        if(speechStateJson.isEmpty()){
            return ComponentStateFactory.createSpeechState(new SpeechStatePayload("", 0, "FINISHED"));
        }
        try {
            return objectMapper.readValue(speechStateJson, ComponentState.class);
        } catch (IOException e) {
            return ComponentStateFactory.createSpeechState(new SpeechStatePayload("", 0, "FINISHED"));
        }
    }
}
