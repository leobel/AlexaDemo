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
package org.freelectron.leobel.testlwa.models.message.request;


import org.freelectron.leobel.testlwa.models.AVSAPIConstants;
import org.freelectron.leobel.testlwa.models.exception.DirectiveHandlingException;
import org.freelectron.leobel.testlwa.models.message.DialogRequestIdHeader;
import org.freelectron.leobel.testlwa.models.message.Header;
import org.freelectron.leobel.testlwa.models.message.MessageIdHeader;
import org.freelectron.leobel.testlwa.models.message.Payload;
import org.freelectron.leobel.testlwa.models.message.request.alerts.AlertPayload;
import org.freelectron.leobel.testlwa.models.message.request.audioplayer.AudioPlayerPayload;
import org.freelectron.leobel.testlwa.models.message.request.audioplayer.PlaybackFailedPayload;
import org.freelectron.leobel.testlwa.models.message.request.audioplayer.PlaybackStutterFinishedPayload;
import org.freelectron.leobel.testlwa.models.message.request.context.AlertsStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;
import org.freelectron.leobel.testlwa.models.message.request.context.PlaybackStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.SpeechStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.VolumeStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.settings.Setting;
import org.freelectron.leobel.testlwa.models.message.request.settings.SettingsUpdatedPayload;
import org.freelectron.leobel.testlwa.models.message.request.speechrecognizer.SpeechProfile;
import org.freelectron.leobel.testlwa.models.message.request.speechrecognizer.SpeechRecognizerPayload;
import org.freelectron.leobel.testlwa.models.message.request.speechsynthesizer.SpeechLifecyclePayload;
import org.freelectron.leobel.testlwa.models.message.request.system.ExceptionEncounteredPayload;
import org.freelectron.leobel.testlwa.models.message.request.system.UserInactivityReportPayload;

import java.util.Arrays;
import java.util.List;

public class RequestFactory {

    public interface Request {
        AVSRequest withPlaybackStatePayload(PlaybackStatePayload state);
    }

    public static AVSRequest createSpeechRecognizerRecognizeRequest(String dialogRequestId,
                                                                     SpeechProfile profile, String format, PlaybackStatePayload playerState,
                                                                     SpeechStatePayload speechState, AlertsStatePayload alertState,
                                                                     VolumeStatePayload volumeState) {
        SpeechRecognizerPayload payload = new SpeechRecognizerPayload(profile, format);
        Header header = new DialogRequestIdHeader(AVSAPIConstants.SpeechRecognizer.NAMESPACE,
                AVSAPIConstants.SpeechRecognizer.Events.Recognize.NAME, dialogRequestId);
        Event event = new Event(header, payload);
        return createRequestWithAllState(event, playerState, speechState, alertState, volumeState);
    }

    public static AVSRequest createAudioPlayerPlaybackStartedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackStarted.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerPlaybackNearlyFinishedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.PlaybackNearlyFinished.NAME, streamToken,
                offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerPlaybackStutterStartedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.PlaybackStutterStarted.NAME, streamToken,
                offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerPlaybackStutterFinishedEvent(String streamToken,
            long offsetInMilliseconds, long stutterDurationInMilliseconds) {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackStutterFinished.NAME);
        Event event = new Event(header, new PlaybackStutterFinishedPayload(streamToken,
                offsetInMilliseconds, stutterDurationInMilliseconds));
        return new AVSRequest(event);
    }

    public static AVSRequest createAudioPlayerPlaybackFinishedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackFinished.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerPlaybackStoppedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackStopped.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerPlaybackPausedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackPaused.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerPlaybackResumedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(AVSAPIConstants.AudioPlayer.Events.PlaybackResumed.NAME,
                streamToken, offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerPlaybackQueueClearedEvent() {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackQueueCleared.NAME);
        Event event = new Event(header, new Payload());
        return new AVSRequest(event);
    }

    public static AVSRequest createAudioPlayerPlaybackFailedEvent(String streamToken,
            PlaybackStatePayload playbackStatePayload, PlaybackFailedPayload.ErrorType errorType) {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackFailed.NAME);
        Event event = new Event(header,
                new PlaybackFailedPayload(streamToken, playbackStatePayload, errorType));
        return new AVSRequest(event);
    }

    public static AVSRequest createAudioPlayerProgressReportDelayElapsedEvent(String streamToken,
            long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.ProgressReportDelayElapsed.NAME, streamToken,
                offsetInMilliseconds);
    }

    public static AVSRequest createAudioPlayerProgressReportIntervalElapsedEvent(
            String streamToken, long offsetInMilliseconds) {
        return createAudioPlayerEvent(
                AVSAPIConstants.AudioPlayer.Events.ProgressReportIntervalElapsed.NAME, streamToken,
                offsetInMilliseconds);
    }

    private static AVSRequest createAudioPlayerEvent(String name, String streamToken,
            long offsetInMilliseconds) {
        Header header = new MessageIdHeader(AVSAPIConstants.AudioPlayer.NAMESPACE, name);
        Payload payload = new AudioPlayerPayload(streamToken, offsetInMilliseconds);
        Event event = new Event(header, payload);
        return new AVSRequest(event);
    }

    public static AVSRequest createPlaybackControllerNextEvent(PlaybackStatePayload playbackState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.NextCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    public static AVSRequest createPlaybackControllerPreviousEvent(
            PlaybackStatePayload playbackState, SpeechStatePayload speechState,
            AlertsStatePayload alertState, VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.PreviousCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    public static AVSRequest createPlaybackControllerPlayEvent(PlaybackStatePayload playbackState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.PlayCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    public static AVSRequest createPlaybackControllerPauseEvent(PlaybackStatePayload playbackState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        return createPlaybackControllerEvent(
                AVSAPIConstants.PlaybackController.Events.PauseCommandIssued.NAME, playbackState,
                speechState, alertState, volumeState);
    }

    private static AVSRequest createPlaybackControllerEvent(String name,
            PlaybackStatePayload playbackState, SpeechStatePayload speechState,
            AlertsStatePayload alertState, VolumeStatePayload volumeState) {
        Header header = new MessageIdHeader(AVSAPIConstants.PlaybackController.NAMESPACE, name);
        Event event = new Event(header, new Payload());
        return createRequestWithAllState(event, playbackState, speechState, alertState,
                volumeState);
    }

    public static AVSRequest createSpeechSynthesizerSpeechStartedEvent(String speakToken) {
        return createSpeechSynthesizerEvent(
                AVSAPIConstants.SpeechSynthesizer.Events.SpeechStarted.NAME, speakToken);
    }

    public static AVSRequest createSpeechSynthesizerSpeechFinishedEvent(String speakToken) {
        return createSpeechSynthesizerEvent(
                AVSAPIConstants.SpeechSynthesizer.Events.SpeechFinished.NAME, speakToken);
    }

    private static AVSRequest createSpeechSynthesizerEvent(String name, String speakToken) {
        Header header = new MessageIdHeader(AVSAPIConstants.SpeechSynthesizer.NAMESPACE, name);
        Event event = new Event(header, new SpeechLifecyclePayload(speakToken));
        return new AVSRequest(event);
    }

    public static AVSRequest createAlertsSetAlertEvent(String alertToken, boolean success) {
        if (success) {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.SetAlertSucceeded.NAME,
                    alertToken);
        } else {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.SetAlertFailed.NAME, alertToken);
        }
    }

    public static AVSRequest createAlertsDeleteAlertEvent(String alertToken, boolean success) {
        if (success) {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.DeleteAlertSucceeded.NAME,
                    alertToken);
        } else {
            return createAlertsEvent(AVSAPIConstants.Alerts.Events.DeleteAlertFailed.NAME,
                    alertToken);
        }
    }

    public static AVSRequest createAlertsAlertStartedEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertStarted.NAME, alertToken);
    }

    public static AVSRequest createAlertsAlertStoppedEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertStopped.NAME, alertToken);
    }

    public static AVSRequest createAlertsAlertEnteredForegroundEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertEnteredForeground.NAME,
                alertToken);
    }

    public static AVSRequest createAlertsAlertEnteredBackgroundEvent(String alertToken) {
        return createAlertsEvent(AVSAPIConstants.Alerts.Events.AlertEnteredBackground.NAME,
                alertToken);
    }

    private static AVSRequest createAlertsEvent(String name, String alertToken) {
        Header header = new MessageIdHeader(AVSAPIConstants.Alerts.NAMESPACE, name);
        Payload payload = new AlertPayload(alertToken);
        Event event = new Event(header, payload);
        return new AVSRequest(event);
    }

    public static AVSRequest createSpeakerVolumeChangedEvent(long volume, boolean muted) {
        return createSpeakerEvent(AVSAPIConstants.Speaker.Events.VolumeChanged.NAME, volume, muted);
    }

    public static AVSRequest createSpeakerMuteChangedEvent(long volume, boolean muted) {
        return createSpeakerEvent(AVSAPIConstants.Speaker.Events.MuteChanged.NAME, volume, muted);
    }

    public static AVSRequest createSpeakerEvent(String name, long volume, boolean muted) {
        Header header = new MessageIdHeader(AVSAPIConstants.Speaker.NAMESPACE, name);

        Event event = new Event(header, new VolumeStatePayload(volume, muted));
        return new AVSRequest(event);
    }

    public static AVSRequest createSystemSynchronizeStateEvent(PlaybackStatePayload playerState,
            SpeechStatePayload speechState, AlertsStatePayload alertState,
            VolumeStatePayload volumeState) {
        Header header = new MessageIdHeader(AVSAPIConstants.System.NAMESPACE,
                AVSAPIConstants.System.Events.SynchronizeState.NAME);
        Event event = new Event(header, new Payload());
        return createRequestWithAllState(event, playerState, speechState, alertState, volumeState);
    }

    public static AVSRequest createSystemExceptionEncounteredEvent(String directiveJson,
                                                                    DirectiveHandlingException.ExceptionType type, String message, PlaybackStatePayload playbackState,
                                                                    SpeechStatePayload speechState, AlertsStatePayload alertState,
                                                                    VolumeStatePayload volumeState) {
        Header header = new MessageIdHeader(AVSAPIConstants.System.NAMESPACE,
                AVSAPIConstants.System.Events.ExceptionEncountered.NAME);

        Event event =
                new Event(header, new ExceptionEncounteredPayload(directiveJson, type, message));

        return createRequestWithAllState(event, playbackState, speechState, alertState,
                volumeState);
    }

    public static AVSRequest createSystemUserInactivityReportEvent(long inactiveTimeInSeconds) {
        Header header = new MessageIdHeader(AVSAPIConstants.System.NAMESPACE,
                AVSAPIConstants.System.Events.UserInactivityReport.NAME);
        Event event = new Event(header, new UserInactivityReportPayload(inactiveTimeInSeconds));
        return new AVSRequest(event);
    }

    public static AVSRequest createSettingsUpdatedEvent(List<Setting> settings) {
        Header header = new MessageIdHeader(AVSAPIConstants.Settings.NAMESPACE,
                AVSAPIConstants.Settings.Events.SettingsUpdated.NAME);
        Event event = new Event(header, new SettingsUpdatedPayload(settings));
        return new AVSRequest(event);
    }

    private static AVSRequest createRequestWithAllState(Event event,
            PlaybackStatePayload playbackState, SpeechStatePayload speechState,
            AlertsStatePayload alertState, VolumeStatePayload volumeState) {
        List<ComponentState> context =
                Arrays.asList(ComponentStateFactory.createPlaybackState(playbackState),
                        ComponentStateFactory.createSpeechState(speechState),
                        ComponentStateFactory.createAlertState(alertState),
                        ComponentStateFactory.createVolumeState(volumeState));
        return new AVSRequest(context, event);
    }
}
