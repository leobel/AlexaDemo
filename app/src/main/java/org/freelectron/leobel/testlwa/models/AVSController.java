/** 
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *   http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package org.freelectron.leobel.testlwa.models;


import android.util.Log;

import org.freelectron.leobel.testlwa.models.exception.DirectiveHandlingException;
import org.freelectron.leobel.testlwa.models.message.request.AVSRequest;
import org.freelectron.leobel.testlwa.models.message.request.RequestFactory;
import org.freelectron.leobel.testlwa.models.message.request.context.Alert;
import org.freelectron.leobel.testlwa.models.message.request.settings.LocaleSetting;
import org.freelectron.leobel.testlwa.models.message.request.settings.Setting;
import org.freelectron.leobel.testlwa.models.message.request.speechrecognizer.SpeechProfile;
import org.freelectron.leobel.testlwa.models.message.response.Directive;
import org.freelectron.leobel.testlwa.models.AlertManager.ResultListener;
import org.freelectron.leobel.testlwa.models.message.response.alerts.DeleteAlert;
import org.freelectron.leobel.testlwa.models.message.response.alerts.SetAlert;
import org.freelectron.leobel.testlwa.models.message.response.audioplayer.ClearQueue;
import org.freelectron.leobel.testlwa.models.message.response.audioplayer.Play;
import org.freelectron.leobel.testlwa.models.message.response.speaker.SetMute;
import org.freelectron.leobel.testlwa.models.message.response.speaker.VolumePayload;
import org.freelectron.leobel.testlwa.models.message.response.speechsynthesizer.Speak;
import org.freelectron.leobel.testlwa.models.message.response.system.SetEndpoint;
import org.freelectron.leobel.testlwa.services.PreferenceService;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Subscriber;

public class AVSController implements RecordingStateListener, AlertHandler, AlertEventListener,
        DirectiveDispatcher, AVSAudioPlayer.AlexaSpeechListener, UserActivityListener, AVSClient.ConnectionListener{

    private final PreferenceService preferenceService;
    private final CloseConnectionListener connectionListener;
    private AudioCapture microphone;
    private AVSClient avsClient;

    private final DialogRequestIdAuthority dialogRequestIdAuthority;
    private AlertManager alertManager;
    private boolean eventRunning = false; // is an event currently being sent

    private static final AudioInputFormat AUDIO_TYPE = AudioInputFormat.LPCM;
    private static final String START_SOUND = "/sdcard/start.mp3";
    private static final String END_SOUND = "/sdcard/stop.mp3";
    private static final String ERROR_SOUND = "/sdcard/error.mp3";
    private static final SpeechProfile PROFILE = SpeechProfile.NEAR_FIELD;
    private static final String FORMAT = "AUDIO_L16_RATE_16000_CHANNELS_1";

    private static final long MILLISECONDS_PER_SECOND = 1000;
    private static final long USER_INACTIVITY_REPORT_PERIOD_HOURS = 1;

    private final AVSAudioPlayer player;
    private BlockableDirectiveThread dependentDirectiveThread;
    private BlockableDirectiveThread independentDirectiveThread;
    private BlockingQueue<Directive> dependentQueue;
    private BlockingQueue<Directive> independentQueue;
    public SpeechRequestAudioPlayerPauseController speechRequestAudioPlayerPauseController;
    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private AtomicLong lastUserInteractionTimestampSeconds;

    private final Set<ExpectSpeechListener> expectSpeechListeners;
    private ExpectStopCaptureListener stopCaptureHandler;

    private boolean wakeWordAgentEnabled = false;

//    private WakeWordIPC wakeWordIPC = null;
    private boolean acceptWakeWordEvents = true; // to ensure we only process one event at a time
//    private WakeWordDetectedHandler wakeWordDetectedHandler;
    private final int WAKE_WORD_AGENT_PORT_NUMBER = 5123;
    private final int WAKE_WORD_RELEASE_TRIES = 5;
    private final int WAKE_WORD_RELEASE_RETRY_DELAY_MS = 1000;
    private final AVSClientFactory avsClientFactory;
    private final DirectiveEnqueuer directiveEnqueuer;
//    private DeviceConfig config;

    public AVSController(CloseConnectionListener connectionListener, ExpectSpeechListener listenHandler, AVSAudioPlayerFactory audioFactory,
                         AlertManagerFactory alarmFactory, AVSClientFactory avsClientFactory,
                         DialogRequestIdAuthority dialogRequestIdAuthority, PreferenceService preferenceService
            /*WakeWordIPCFactory wakewordIPCFactory, DeviceConfig config, WakeWordDetectedHandler wakeWakeDetectedHandler*/)
                    throws Exception {
        this.connectionListener = connectionListener;
        this.preferenceService = preferenceService;
        this.avsClientFactory = avsClientFactory;
//        this.wakeWordAgentEnabled = config.getWakeWordAgentEnabled();
//        this.wakeWordDetectedHandler = wakeWakeDetectedHandler;
//        this.config = config;

//        if (this.wakeWordAgentEnabled) {
//            try {
//                log.info("Creating Wake Word IPC | port number: " + WAKE_WORD_AGENT_PORT_NUMBER);
//                this.wakeWordIPC =
//                        wakewordIPCFactory.createWakeWordIPC(this, WAKE_WORD_AGENT_PORT_NUMBER);
//                this.wakeWordIPC.init();
//                Thread.sleep(1000);
//                log.info("Created Wake Word IPC ok.");
//            } catch (IOException e) {
//                log.error("Error creating Wake Word IPC ok.", e);
//            }
//        }

        initializeMicrophone();

        this.player = audioFactory.getAudioPlayer(this);
        this.player.registerAlexaSpeechListener(this);
        this.dialogRequestIdAuthority = dialogRequestIdAuthority;
        speechRequestAudioPlayerPauseController = new SpeechRequestAudioPlayerPauseController(player);

        expectSpeechListeners = new HashSet<ExpectSpeechListener>(Arrays.asList(listenHandler, speechRequestAudioPlayerPauseController));
        dependentQueue = new LinkedBlockingDeque<>();

        independentQueue = new LinkedBlockingDeque<>();

        directiveEnqueuer =
                new DirectiveEnqueuer(dialogRequestIdAuthority, dependentQueue, independentQueue);

        avsClient = avsClientFactory.getAVSClient(this, directiveEnqueuer, preferenceService);

        alertManager = alarmFactory.getAlertManager(this, this, AlertsFileDataStore.getInstance());

        Log.d("Connection", "Success");
        // Ensure that we have attempted to finish loading all alarms from file before sending
        // synchronize state
        alertManager.loadFromDisk(new ResultListener() {
            @Override
            public void onSuccess() {
                sendSynchronizeStateEvent();
            }

            @Override
            public void onFailure() {
                sendSynchronizeStateEvent();
            }
        });

        // ensure we notify AVS of playbackStopped on app exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                player.stop();
                avsClient.shutdown();
            }
        });

        dependentDirectiveThread =
                new BlockableDirectiveThread(dependentQueue, this, "DependentDirectiveThread");
        independentDirectiveThread =
                new BlockableDirectiveThread(independentQueue, this, "IndependentDirectiveThread");

        lastUserInteractionTimestampSeconds =
                new AtomicLong(System.currentTimeMillis() / MILLISECONDS_PER_SECOND);
        scheduledExecutor.scheduleAtFixedRate(new UserInactivityReport(),
                USER_INACTIVITY_REPORT_PERIOD_HOURS, USER_INACTIVITY_REPORT_PERIOD_HOURS,
                TimeUnit.HOURS);

//        setLocale(config.getLocale());
        setLocale(Locale.US);
    }

    private void initializeMicrophone() {
        getMicrophone(this);
    }

    private void getMicrophone(AVSController controller) {
        controller.microphone = AudioCapture.getAudioHardware(AUDIO_TYPE);
    }

    public void startHandlingDirectives() {
        dependentDirectiveThread.start();
        independentDirectiveThread.start();
    }

    public void initializeStopCaptureHandler(ExpectStopCaptureListener stopHandler) {
        stopCaptureHandler = stopHandler;
    }

    public void sendSynchronizeStateEvent() {
        sendRequest(RequestFactory.createSystemSynchronizeStateEvent(player.getPlaybackState(),
                player.getSpeechState(), alertManager.getState(), player.getVolumeState()));
    }

//    @Override
//    public void onAccessTokenReceived(String accessToken) {
//        avsClient.setAccessToken(accessToken);
//    }

    // start the recording process and send to server
    // takes an optional RMS callback and an optional request callback
    public void startRecording(RecordingRMSListener rmsListener) {

//        if (this.wakeWordAgentEnabled) {
//
//            acceptWakeWordEvents = false;
//
//            try {
//                wakeWordIPC.sendCommand(IPCCommand.IPC_PAUSE_WAKE_WORD_ENGINE);
//            } catch (IOException e) {
//                log.warn("Could not send the IPC_PAUSE_WAKE_WORD_ENGINE command");
//            }
//        }

        try {
            String dialogRequestId = dialogRequestIdAuthority.createNewDialogRequestId();

            AVSRequest request = RequestFactory.createSpeechRecognizerRecognizeRequest(
                    dialogRequestId, PROFILE, FORMAT, player.getPlaybackState(),
                    player.getSpeechState(), alertManager.getState(), player.getVolumeState());

            dependentQueue.clear();

            StreamContentProvider streamContentProvider = microphone.getAudioContentProvider(this, rmsListener);

            request.setStreamProvider(streamContentProvider);

            avsClient.sendEvent(request);

            speechRequestAudioPlayerPauseController.startSpeechRequest();
        } catch (Exception e) {
            Log.d("Recording Error", e.getMessage(), e);
            player.playMp3FromResource(ERROR_SOUND);
            connectionListener.onCloseConnection();
        }
    }

    private Observable<byte[]> createAudioInputStreamProvider(final InputStream inputStream) {
        Observable<byte[]> streamProvider = Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {
                int bufferSize = microphone.getAudioBufferSizeInBytes();
//                File fileIn = new File("/sdcard/alexa_news.wav");
                byte[] buffer = new byte[bufferSize];
                try {
//                    InputStream inputStream = new BufferedInputStream(new FileInputStream(fileIn));

                    while (inputStream.read(buffer, 0, bufferSize) != -1){
                        subscriber.onNext(buffer);
                    }
                    subscriber.onCompleted();
//                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        return streamProvider;
    }

//    private void getMicrophoneInputStream(AVSController controller, RecordingRMSListener rmsListener) throws IOException {
//
//        int numberRetries = 1;
//
////        if (this.wakeWordAgentEnabled) {
////            numberRetries = WAKE_WORD_RELEASE_TRIES;
////        }
//
//        for (; numberRetries > 0; numberRetries--) {
//            try {
//                microphone.getAudioContentProvider(controller, rmsListener);
//            } catch (IOException e) {
//                if (numberRetries == 1) {
//                    throw e;
//                }
//                Log.d("Error", "Could not open the microphone line.");
//                try {
//                    Thread.sleep(WAKE_WORD_RELEASE_RETRY_DELAY_MS);
//                } catch (InterruptedException e1) {
//                    Log.d("Error", "exception:", e1);
//                }
//            }
//        }
//
//        throw new IOException();
//    }

    public void handlePlaybackAction(PlaybackAction action) {
        switch (action) {
            case PLAY:
                if (alertManager.hasActiveAlerts()) {
                    alertManager.stopActiveAlert();
                } else {
                    sendRequest(RequestFactory.createPlaybackControllerPlayEvent(
                            player.getPlaybackState(), player.getSpeechState(),
                            alertManager.getState(), player.getVolumeState()));
                }
                break;
            case PAUSE:
                if (alertManager.hasActiveAlerts()) {
                    alertManager.stopActiveAlert();
                } else {
                    sendRequest(RequestFactory.createPlaybackControllerPauseEvent(
                            player.getPlaybackState(), player.getSpeechState(),
                            alertManager.getState(), player.getVolumeState()));
                }
                break;
            case PREVIOUS:
                sendRequest(RequestFactory.createPlaybackControllerPreviousEvent(
                        player.getPlaybackState(), player.getSpeechState(), alertManager.getState(),
                        player.getVolumeState()));
                break;
            case NEXT:
                sendRequest(RequestFactory.createPlaybackControllerNextEvent(
                        player.getPlaybackState(), player.getSpeechState(), alertManager.getState(),
                        player.getVolumeState()));
                break;
            default:
                Log.d("Error", "Failed to handle playback action");
        }
    }

    public void sendRequest(AVSRequest request) {
        eventRunning = true;
        try {
            avsClient.sendEvent(request);
        } catch (Exception e) {
            Log.d("Failed to send request", e.getMessage(), e);
        }
        eventRunning = false;
    }

    /**
     * Set this device account's locale to the given locale by
     * sending a SettingsUpdated event to AlexaService.
     * @param locale
     */
    public void setLocale(Locale locale) {
        List<Setting> settings = new ArrayList<>();
        settings.add(new LocaleSetting(locale.toLanguageTag()));
        sendRequest(RequestFactory.createSettingsUpdatedEvent(settings));
    }

    public boolean eventRunning() {
        return eventRunning;
    }

    @Override
    public synchronized void dispatch(Directive directive) {
        String directiveNamespace = directive.getNamespace();

        String directiveName = directive.getName();
        Log.d("Directive", "Handling directive: " + directiveNamespace + "." + directiveName);
        if (dialogRequestIdAuthority.isCurrentDialogRequestId(directive.getDialogRequestId())) {
            speechRequestAudioPlayerPauseController.dispatchDirective();
        }
        try {
            if (AVSAPIConstants.SpeechRecognizer.NAMESPACE.equals(directiveNamespace)) {
                handleSpeechRecognizerDirective(directive);
            } else if (AVSAPIConstants.SpeechSynthesizer.NAMESPACE.equals(directiveNamespace)) {
                handleSpeechSynthesizerDirective(directive);
            } else if (AVSAPIConstants.AudioPlayer.NAMESPACE.equals(directiveNamespace)) {
                handleAudioPlayerDirective(directive);
            } else if (AVSAPIConstants.Alerts.NAMESPACE.equals(directiveNamespace)) {
                handleAlertsDirective(directive);
            } else if (AVSAPIConstants.Speaker.NAMESPACE.equals(directiveNamespace)) {
                handleSpeakerDirective(directive);
            } else if (AVSAPIConstants.System.NAMESPACE.equals(directiveNamespace)) {
                handleSystemDirective(directive);
            } else {
                throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                        "No device side component to handle the directive.");
            }
        } catch (DirectiveHandlingException e) {
            sendExceptionEncounteredEvent(directive.getRawMessage(), e.getType(), e);
        } catch (Exception e) {
            sendExceptionEncounteredEvent(directive.getRawMessage(), DirectiveHandlingException.ExceptionType.INTERNAL_ERROR,
                    e);
            throw e;
        }
    }

    private void sendExceptionEncounteredEvent(String directiveJson, DirectiveHandlingException.ExceptionType type,
            Exception e) {
        sendRequest(RequestFactory.createSystemExceptionEncounteredEvent(directiveJson, type,
                e.getMessage(), player.getPlaybackState(), player.getSpeechState(),
                alertManager.getState(), player.getVolumeState()));
        Log.d("Error", "error handling directive: " + directiveJson, e);
    }

    private void handleAudioPlayerDirective(Directive directive) throws DirectiveHandlingException {
        String directiveName = directive.getName();
        if (AVSAPIConstants.AudioPlayer.Directives.Play.NAME.equals(directiveName)) {
            player.handlePlay((Play) directive.getPayload());
        } else if (AVSAPIConstants.AudioPlayer.Directives.Stop.NAME.equals(directiveName)) {
            player.handleStop();
        } else if (AVSAPIConstants.AudioPlayer.Directives.ClearQueue.NAME.equals(directiveName)) {
            player.handleClearQueue((ClearQueue) directive.getPayload());
        } else {
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                    "The device's audio player component cannot handle this directive.");
        }
    }

    private void handleSystemDirective(Directive directive) throws DirectiveHandlingException {
        String directiveName = directive.getName();
        if (AVSAPIConstants.System.Directives.ResetUserInactivity.NAME.equals(directiveName)) {
            onUserActivity();
        } else if (AVSAPIConstants.System.Directives.SetEndpoint.NAME.equals(directiveName)) {
            handleSetEndpoint((SetEndpoint) directive.getPayload());
        } else {
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                    "The device's system component cannot handle this directive.");
        }
    }

    private void handleSpeechSynthesizerDirective(Directive directive)
            throws DirectiveHandlingException {
        if (AVSAPIConstants.SpeechSynthesizer.Directives.Speak.NAME.equals(directive.getName())) {
            player.handleSpeak((Speak) directive.getPayload());
        } else {
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                    "The device's speech synthesizer component cannot handle this directive.");
        }
    }


    private void handleSpeechRecognizerDirective(Directive directive) throws DirectiveHandlingException {
        String directiveName = directive.getName();
        if (AVSAPIConstants.SpeechRecognizer.Directives.ExpectSpeech.NAME.equals(directiveName)) {

            // If your device cannot handle automatically starting to listen, you must
            // implement a listen timeout event, as described here:
            // https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/rest/speechrecognizer-listentimeout-request
            notifyExpectSpeechDirective();
        } else if (directiveName
                .equals(AVSAPIConstants.SpeechRecognizer.Directives.StopCapture.NAME)) {
            stopCaptureHandler.onStopCaptureDirective();
        } else {
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                    "The device's speech recognizer component cannot handle this directive.");
        }
    }

    private void handleAlertsDirective(Directive directive) throws DirectiveHandlingException {
        String directiveName = directive.getName();
        if (AVSAPIConstants.Alerts.Directives.SetAlert.NAME.equals(directiveName)) {
            SetAlert payload = (SetAlert) directive.getPayload();
            String alertToken = payload.getToken();
            DateTime scheduledTime = payload.getScheduledTime();
            SetAlert.AlertType type = payload.getType();

            if (alertManager.hasAlert(alertToken)) {
                AlertScheduler scheduler = alertManager.getScheduler(alertToken);
                if (scheduler.getAlert().getScheduledTime().equals(scheduledTime)) {
                    return;
                } else {
                    scheduler.cancel();
                }
            }

            Alert alert = new Alert(alertToken, type, scheduledTime);
            alertManager.add(alert);
        } else if (AVSAPIConstants.Alerts.Directives.DeleteAlert.NAME.equals(directiveName)) {
            DeleteAlert payload = (DeleteAlert) directive.getPayload();
            alertManager.delete(payload.getToken());
        } else {
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                    "The device's alert component cannot handle this directive.");
        }
    }

    private void handleSpeakerDirective(Directive directive) throws DirectiveHandlingException {
        String directiveName = directive.getName();
        if (AVSAPIConstants.Speaker.Directives.SetVolume.NAME.equals(directiveName)) {
            player.handleSetVolume((VolumePayload) directive.getPayload());
        } else if (AVSAPIConstants.Speaker.Directives.AdjustVolume.NAME.equals(directiveName)) {
            player.handleAdjustVolume((VolumePayload) directive.getPayload());
        } else if (AVSAPIConstants.Speaker.Directives.SetMute.NAME.equals(directiveName)) {
            player.handleSetMute((SetMute) directive.getPayload());
        } else {
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                    "The device's speaker component cannot handle this directive.");
        }
    }

    private void handleSetEndpoint(SetEndpoint setEndpoint) throws DirectiveHandlingException {
        try {
            URL endpoint = new URL(setEndpoint.getEndpoint());
            if (endpoint.equals(avsClient.getHost())) {
                return;
            }
//            config.setAvsHost(endpoint);
//            config.saveConfig();

            avsClient.shutdown();
            avsClient = avsClientFactory.getAVSClient(this, directiveEnqueuer, preferenceService);
        } catch (MalformedURLException e) {
            Log.d("Error", "The SetEndpoint payload had a malformed URL", e);
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNEXPECTED_INFORMATION_RECEIVED,
                    "Received SetEndpoint directive with malformed url");
        } catch (Exception e) {
            Log.d("Error", "Failed to set a new avs client.", e);
            throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.INTERNAL_ERROR,
                    "Error while creating avsClient");
        }
    }

    private void notifyExpectSpeechDirective() {
        for (ExpectSpeechListener listener : expectSpeechListeners) {
            listener.onExpectSpeechDirective();
        }
    }

    public void stopRecording() {
        speechRequestAudioPlayerPauseController.finishedListening();
        microphone.stopCapture();

//        if (this.wakeWordAgentEnabled) {
//            try {
//                wakeWordIPC.sendCommand(IPCCommand.IPC_RESUME_WAKE_WORD_ENGINE);
//            } catch (IOException e) {
//                log.warn("could not send resume wake word engine command", e);
//            }
//            acceptWakeWordEvents = true;
//        }
    }

    // audio state callback for when recording has started
    @Override
    public void recordingStarted() {
        player.playMp3FromResource(START_SOUND);
    }

    // audio state callback for when recording has completed
    @Override
    public void recordingCompleted() {
        player.playMp3FromResource(END_SOUND);
    }

    public boolean isSpeaking() {
        return player.isSpeaking();
    }

    public boolean isPlaying() {
        return player.isPlayingOrPaused();
    }

    @Override
    public void onAlertStarted(String alertToken) {
        sendRequest(RequestFactory.createAlertsAlertStartedEvent(alertToken));

        if (player.isSpeaking()) {
            sendRequest(RequestFactory.createAlertsAlertEnteredBackgroundEvent(alertToken));
        } else {
            sendRequest(RequestFactory.createAlertsAlertEnteredForegroundEvent(alertToken));
        }
    }

    @Override
    public void onAlertStopped(String alertToken) {
        sendRequest(RequestFactory.createAlertsAlertStoppedEvent(alertToken));
    }

    @Override
    public void onAlertSet(String alertToken, boolean success) {
        sendRequest(RequestFactory.createAlertsSetAlertEvent(alertToken, success));
    }

    @Override
    public void onAlertDelete(String alertToken, boolean success) {
        sendRequest(RequestFactory.createAlertsDeleteAlertEvent(alertToken, success));
    }

    @Override
    public void startAlert(String alertToken) {
        player.startAlert();
    }

    @Override
    public void stopAlert(String alertToken) {
        if (!alertManager.hasActiveAlerts()) {
            player.stopAlert();
        }
    }

    public void processingFinished() {
        Log.d("Process finishing", "Speech processing finished. Dependent queue size: " + dependentQueue.size());
        speechRequestAudioPlayerPauseController
                .speechRequestProcessingFinished(dependentQueue.size());
    }

    @Override
    public void onAlexaSpeechStarted() {
        dependentDirectiveThread.block();

        if (alertManager.hasActiveAlerts()) {
            for (String alertToken : alertManager.getActiveAlerts()) {
                sendRequest(RequestFactory.createAlertsAlertEnteredBackgroundEvent(alertToken));
            }
        }
    }

    @Override
    public void onAlexaSpeechFinished() {
        dependentDirectiveThread.unblock();

        if (alertManager.hasActiveAlerts()) {
            for (String alertToken : alertManager.getActiveAlerts()) {
                sendRequest(RequestFactory.createAlertsAlertEnteredForegroundEvent(alertToken));
            }
        }
    }

//    @Override
//    public void onParsingFailed(String unparseable) {
//        String message = "Failed to parse message from AVS";
//        sendRequest(RequestFactory.createSystemExceptionEncounteredEvent(unparseable,
//                DirectiveHandlingException.ExceptionType.UNEXPECTED_INFORMATION_RECEIVED, message, player.getPlaybackState(),
//                player.getSpeechState(), alertManager.getState(), player.getVolumeState()));
//    }



    @Override
    public void onUserActivity() {
        lastUserInteractionTimestampSeconds.set(System.currentTimeMillis() / MILLISECONDS_PER_SECOND);
    }

    @Override
    public void onConnectionSuccess() {

    }

    @Override
    public void onSynchronizeSuccess() {

    }

    @Override
    public void onCloseConnection(Throwable e) {
        connectionListener.onCloseConnection();
    }

    private class UserInactivityReport implements Runnable {

        @Override
        public void run() {
            sendRequest(RequestFactory.createSystemUserInactivityReportEvent(
                    (System.currentTimeMillis() / MILLISECONDS_PER_SECOND)
                            - lastUserInteractionTimestampSeconds.get()));
        }
    }

    public interface CloseConnectionListener {
        void onCloseConnection();
    }

//    @Override
//    public synchronized void onWakeWordDetected() {
//        if (acceptWakeWordEvents) {
//            wakeWordDetectedHandler.onWakeWordDetected();
//        }
//    }
}
