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

import java.util.concurrent.CountDownLatch;

/**
 * This class keeps track of running speech requests and whether the device is listening/speaking to
 * appropriately manage the pause state of the player.
 */
public class SpeechRequestAudioPlayerPauseController implements AVSAudioPlayer.AlexaSpeechListener, ExpectSpeechListener {

    private final AVSAudioPlayer audioPlayer;
    private CountDownLatch outstandingDirectiveCount;
    private Thread resumeAudioThread;
    private CountDownLatch alexaSpeaking;
    private CountDownLatch alexaListening;
    private volatile boolean speechRequestRunning = false;

    public SpeechRequestAudioPlayerPauseController(AVSAudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        audioPlayer.registerAlexaSpeechListener(this);
    }

    /**
     * Called when the starting a speech request to alexa voice service
     */
    public void startSpeechRequest() {
        Log.d("Speech", "Speech request started");
        alexaListening = new CountDownLatch(1);
        audioPlayer.interruptAllAlexaOutput();
        if(resumeAudioThread != null){
            resumeAudioThread.interrupt();
        }
        speechRequestRunning = true;
    }

    /**
     * Called when finished Listening
     */
    public void finishedListening() {
        Log.d("Speech", "Finished listening to user speech");
        if(alexaListening != null){
            alexaListening.countDown();
        }
        if (!speechRequestRunning) {
            audioPlayer.resumeAllAlexaOutput();
        }
    }

    /**
     * Called each time a directive is dispatched
     */
    public void dispatchDirective() {
        Log.d("Speech", "Dispatching directive");
        if(outstandingDirectiveCount != null){
            outstandingDirectiveCount.countDown();
        }
    }

    @Override
    public void onAlexaSpeechStarted() {
        Log.d("Speech", "Alexa speech started");
        alexaSpeaking = new CountDownLatch(1);
    }

    @Override
    public void onAlexaSpeechFinished() {
        Log.d("Speech", "Alexa speech finished");
        if(alexaSpeaking != null){
            alexaSpeaking.countDown();
        }
        if (!speechRequestRunning) {
            audioPlayer.resumeAllAlexaOutput();
        }
    }

    @Override
    public void onExpectSpeechDirective() {
        alexaListening = new CountDownLatch(1);
    }

    /**
     * A speech request has been finished processing
     *
     * @param directiveCount
     *            the number of outstanding directives that correspond to the speech request that
     *            just finished
     */
    public void speechRequestProcessingFinished(int directiveCount) {
        Log.d("Speech", "Finished processing speech request");
        if(resumeAudioThread != null){
            resumeAudioThread.interrupt();
        }
        outstandingDirectiveCount = new CountDownLatch(directiveCount);
        resumeAudioThread = new Thread() {

            boolean isInterrupted = false;

            @Override
            public void run() {
                Log.d("Speech", "Started resume audio thread");
                if(outstandingDirectiveCount != null){
                    awaitOnLatch(outstandingDirectiveCount);
                }
                if (alexaListening != null || alexaSpeaking != null) {
                    if(alexaListening != null){
                        awaitOnLatch(alexaListening);
                    }
                    if(alexaSpeaking != null){
                        awaitOnLatch(alexaSpeaking);
                    }
                }
                if (!isInterrupted) {
                    speechRequestRunning = false;
                    Log.d("Speech", "Resuming all Alexa output");
                    audioPlayer.resumeAllAlexaOutput();
                }

            }

            private void awaitOnLatch(CountDownLatch latch) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    // If another speech request is kicked off while we're processing the
                    // current request we expect this thread to be interrupted
                    isInterrupted = true;
                }
            }

        };
        if(resumeAudioThread != null){
            resumeAudioThread.start();
        }
    }

}
