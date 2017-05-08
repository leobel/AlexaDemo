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
import org.freelectron.leobel.testlwa.models.message.Header;
import org.freelectron.leobel.testlwa.models.message.request.context.AlertsStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;
import org.freelectron.leobel.testlwa.models.message.request.context.PlaybackStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.SpeechStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.VolumeStatePayload;

public class ComponentStateFactory {

    public static ComponentState createPlaybackState(PlaybackStatePayload playerState) {
        return new ComponentState(new Header(AVSAPIConstants.AudioPlayer.NAMESPACE,
                AVSAPIConstants.AudioPlayer.Events.PlaybackState.NAME), playerState);
    }

    public static ComponentState createSpeechState(SpeechStatePayload speechState) {
        return new ComponentState(new Header(AVSAPIConstants.SpeechSynthesizer.NAMESPACE,
                AVSAPIConstants.SpeechSynthesizer.Events.SpeechState.NAME), speechState);
    }

    public static ComponentState createAlertState(AlertsStatePayload alertState) {
        return new ComponentState(new Header(AVSAPIConstants.Alerts.NAMESPACE,
                AVSAPIConstants.Alerts.Events.AlertsState.NAME), alertState);
    }

    public static ComponentState createVolumeState(VolumeStatePayload volumeState) {
        return new ComponentState(new Header(AVSAPIConstants.Speaker.NAMESPACE,
                AVSAPIConstants.Speaker.Events.VolumeState.NAME), volumeState);
    }
}
