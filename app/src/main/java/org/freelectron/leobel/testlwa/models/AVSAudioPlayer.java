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


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.freelectron.leobel.testlwa.CustomMediaPlayer;
import org.freelectron.leobel.testlwa.models.exception.DirectiveHandlingException;
import org.freelectron.leobel.testlwa.models.message.request.RequestFactory;
import org.freelectron.leobel.testlwa.models.message.request.context.PlaybackStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.SpeechStatePayload;
import org.freelectron.leobel.testlwa.models.message.request.context.VolumeStatePayload;
import org.freelectron.leobel.testlwa.models.message.response.audioplayer.AudioItem;
import org.freelectron.leobel.testlwa.models.message.response.audioplayer.ClearQueue;
import org.freelectron.leobel.testlwa.models.message.response.audioplayer.Play;
import org.freelectron.leobel.testlwa.models.message.response.audioplayer.Stream;
import org.freelectron.leobel.testlwa.models.message.response.speaker.SetMute;
import org.freelectron.leobel.testlwa.models.message.response.speaker.VolumePayload;
import org.freelectron.leobel.testlwa.models.message.response.speechsynthesizer.Speak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class AVSAudioPlayer {

    // callback to send audio events
    private final AVSController controller;
    private final AudioManager audioManager;
    // vlc instance to play media
    private CustomMediaPlayer audioPlayer;
    // queue of listen directive media
    private final Queue<Stream> playQueue;
    // queue of speak directive media
    private final Queue<SpeakItem> speakQueue;
    // Cache of URLs associated with the current AVSPlayItem/stream
    private Set<String> streamUrls;
    // Urls associated with the current stream that we've already tried to play
    private Set<String> attemptedUrls;
    // Map of VLC-centric urls to file urls - we use this map to delete local files after playing
    private Map<String, String> cachedAudioFiles;
    // Alarm thread
    private Thread alarmThread;
    // Speaker thread
    private Thread playThread;
    // Object on which to lock
    private Object playLock = new Object();
    // How long the thread should block on waiting for audio to finish playing
    private static final int TIMEOUT_IN_MS = 3000;

    // VLCJ volumes are between 0-200. Alexa volumes are from 0-100. These constants are used to
    // convert and limit volume values.
    private static float VOLUME_SCALAR = 100;
    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 1;

    // VLC's elapsed time doesn't work correctly. So we're using System.nanoTime() to get accurate
    // timestamps
    private AudioPlayerTimer timer;
    // track the last progressReport sent time
    private boolean waitForPlaybackFinished;
    // used for speak directives and earcons
    private CustomMediaPlayer speaker = null;
    private final ClassLoader resLoader; // used to load resource files

    private String latestStreamToken = "";

    private String latestToken = "";

    /*
     * The AudioPlayerStateMachine is used to keep track of local audio playback state changes,
     * ensuring the PlaybackEvents are sent at the right time, in the correct order, and only once.
     */
    private final AudioPlayerStateMachine audioPlayerStateMachine;

    private int currentVolume;

    private long playbackStutterStartedOffsetInMilliseconds;

    private final Set<AlexaSpeechListener> listeners;

    private final AudioPlayerProgressReporter progressReporter;

    private enum SpeechState {
        PLAYING,
        FINISHED;
    }

    private enum AlertState {
        PLAYING,
        INTERRUPTED,
        FINISHED;
    }

    private volatile AlertState alertState = AlertState.FINISHED;

    private volatile SpeechState speechState = SpeechState.FINISHED;

    private boolean currentlyMuted;

    private final int maxVolume;

    private boolean playbackStartedSuccessfully;

    private boolean bufferUnderrunInProgress;

    private boolean isPaused;

    public AVSAudioPlayer(AudioManager audioManager, AVSController controller) {
        this.audioManager = audioManager;
        this.controller = controller;
        resLoader = Thread.currentThread().getContextClassLoader();
        timer = new AudioPlayerTimer();
        waitForPlaybackFinished = false;
        playQueue = new LinkedList<Stream>();
        speakQueue = new LinkedList<SpeakItem>();
        streamUrls = new HashSet<String>();
        attemptedUrls = new HashSet<String>();
        cachedAudioFiles = new HashMap<String, String>();
        setupAudioPlayer();

        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        VOLUME_SCALAR = maxVolume/VOLUME_SCALAR;
        currentlyMuted = currentVolume == 0;

        audioPlayerStateMachine = new AudioPlayerStateMachine(this, controller);

        progressReporter = new AudioPlayerProgressReporter(
                new ProgressReportDelayEventRunnable(audioPlayerStateMachine),
                new ProgressReportIntervalEventRunnable(audioPlayerStateMachine), timer);

        listeners = new HashSet<>();
    }

    public void registerAlexaSpeechListener(AlexaSpeechListener listener) {
        listeners.add(listener);
    }

    public void handleSpeak(Speak speak) {
        SpeakItem speakItem = new SpeakItem(speak.getToken(), speak.getAttachedContent());

        speakQueue.add(speakItem);
        // if not already speaking, start speech
        if (speakQueue.size() == 1) {
            startSpeech();
        }
    }

    public void handlePlay(Play play) throws DirectiveHandlingException {
        AudioItem item = play.getAudioItem();
        if (play.getPlayBehavior() == Play.PlayBehavior.REPLACE_ALL) {
            clearAll();
        } else if (play.getPlayBehavior() == Play.PlayBehavior.REPLACE_ENQUEUED) {
            clearEnqueued();
        }

        Stream stream = item.getStream();
        String streamUrl = stream.getUrl();
        String streamId = stream.getToken();
        long offset = stream.getOffsetInMilliseconds();
        Log.i("URL: {}", streamUrl);
        Log.i("StreamId: {}", streamId);
        Log.i("Offset: {}", String.valueOf(offset));

        if (stream.hasAttachedContent()) {
            try {
                File tmp = File.createTempFile(UUID.randomUUID().toString(), ".mp3");
                tmp.deleteOnExit();
                copyInputStreamToFile(stream.getAttachedContent(), tmp);
                stream.setUrl(tmp.getAbsolutePath());
                add(stream);
            } catch (IOException e) {
                Log.d("Error", "Error while saving audio to a file", e);
                throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.INTERNAL_ERROR,
                        "Error saving attached content to disk, unable to handle Play directive.");
            }
        } else {
            add(stream);
        }
    }

    private void copyInputStreamToFile(InputStream inputStream, File file){
        try {
            OutputStream output = new FileOutputStream(file);
            try {
                try {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace(); // handle exception, define IOException and others
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void handleStop() {
        synchronized (audioPlayer) {
            stop();
            audioPlayerStateMachine.playbackStopped();
        }
    }

    public void handleClearQueue(ClearQueue clearQueue) {
        if (clearQueue.getClearBehavior() == ClearQueue.ClearBehavior.CLEAR_ALL) {
            audioPlayerStateMachine.clearQueueAll();
            clearAll();
        } else {
            audioPlayerStateMachine.clearQueueEnqueued();
            clearEnqueued();
        }
    }

    public void handleSetVolume(VolumePayload volumePayload) {
        currentVolume = (int) (volumePayload.getVolume() * VOLUME_SCALAR);
        audioPlayer.setVolume(currentVolume, currentVolume);
        controller.sendRequest(
                RequestFactory.createSpeakerVolumeChangedEvent(getVolume(), isMuted()));
    }

    public void handleAdjustVolume(VolumePayload volumePayload) {
        int adjustVolumeBy = (int) (volumePayload.getVolume() * VOLUME_SCALAR);
        currentVolume = Math.min(MAX_VOLUME,
                Math.max(MIN_VOLUME, currentVolume + adjustVolumeBy));
        audioPlayer.setVolume(currentVolume, currentVolume);
        controller.sendRequest(
                RequestFactory.createSpeakerVolumeChangedEvent(getVolume(), isMuted()));
    }

    public void handleSetMute(SetMute setMutePayload) {
        currentlyMuted = setMutePayload.getMute();
        if(currentlyMuted){
            audioPlayer.setVolume(0, 0);
        }
        controller.sendRequest(RequestFactory.createSpeakerMuteChangedEvent(getVolume(), isMuted()));
    }

    private void setupAudioPlayer() {
        audioPlayer = new CustomMediaPlayer();
        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        audioPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer player, int newCache) {
                Stream stream = playQueue.peek();
                if (stream == null) {
                    return;
                }
                if (playbackStartedSuccessfully && !bufferUnderrunInProgress) {
                    // We started buffering mid playback
                    bufferUnderrunInProgress = true;
                    long startOffset = 0;
                    startOffset = stream.getOffsetInMilliseconds();
                    playbackStutterStartedOffsetInMilliseconds =
                            Math.max(startOffset, getCurrentOffsetInMilliseconds());
                    stopTimerAndProgressReporter();
                    audioPlayerStateMachine.playbackStutterStarted();
                }

                if (bufferUnderrunInProgress && newCache >= 100.0f) {
                    // We are fully buffered after a buffer underrun event
                    bufferUnderrunInProgress = false;
                    audioPlayerStateMachine.playbackStutterFinished();
                    startTimerAndProgressReporter();
                }

                if (!playbackStartedSuccessfully && newCache >= 100.0f) {
                    // We have successfully buffered the first time and started playback
                    playbackStartedSuccessfully = true;

                    long offset = stream.getOffsetInMilliseconds();

                    timer.reset(offset, audioPlayer.getDuration());
                    progressReporter.disable();
                    if (stream.getProgressReportRequired()) {
                        progressReporter.setup(stream.getProgressReport());
                    }

                    audioPlayerStateMachine.playbackStarted();
                    startTimerAndProgressReporter();

                    if (isPaused) {
                        audioPlayerStateMachine.playbackPaused();
                    }
                }
            }
        });

        audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                CustomMediaPlayer mediaPlayer = (CustomMediaPlayer) player;
                Log.d("Audio finished", "Finished playing " + mediaPlayer.mrl());
                List<String> items = new ArrayList<String>();/*mediaPlayer.subItems();*/
                // Remember the url we just tried
                attemptedUrls.add(mediaPlayer.mrl());

                if (cachedAudioFiles.containsKey(mediaPlayer.mrl())) {
                    String key = mediaPlayer.mrl();
                    String cachedUrl = cachedAudioFiles.get(key);
                    deleteCachedFile(cachedUrl);
                    cachedAudioFiles.remove(key);
                }

                if ((items.size() > 0) || (streamUrls.size() > 0)) {
                    // Add to the set of URLs to attempt playback
                    streamUrls.addAll(items);

                    // Play any url associated with this play item that
                    // we haven't already tried
                    for (String mrl : streamUrls) {
                        if (!attemptedUrls.contains(mrl)) {
                            Log.d("Playing {}", mrl);
                            try {
                                playAudio(mrl);
                                return;
                            } catch (IOException e) {
                                Log.d("Playing Error", e.getMessage(), e);
                            }
                        }
                    }
                }

                // wait for any pending events to finish(playbackStarted/progressReport)
                while (controller.eventRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }

                // remove the item from the queue since it has finished playing
                playQueue.poll();

                stopTimerAndProgressReporter();
                audioPlayerStateMachine.playbackNearlyFinished();
                audioPlayerStateMachine.playbackFinished();

                // unblock playback now that playbackFinished has been sent
                waitForPlaybackFinished = false;
                if (!playQueue.isEmpty()) {
                    // start playback if it wasn't the last item
                    startPlayback();
                }
            }
        });

        audioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer player, int i, int i1) {
                CustomMediaPlayer mediaPlayer = (CustomMediaPlayer)player;
                Log.d("Error playing:", mediaPlayer.mrl());

                attemptedUrls.add(mediaPlayer.mrl());
                // If there are any urls left to try, don't throw an error
                for (String mrl : streamUrls) {
                    if (!attemptedUrls.contains(mrl)) {
                        try {
                            playAudio(mrl);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                }

                // wait for any pending events to finish(playbackStarted/progressReport)
                while (controller.eventRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }
                playQueue.clear();
                stopTimerAndProgressReporter();
                audioPlayerStateMachine.playbackFailed();
                return false;
            }
        });
    }

    /**
     * Returns true if Alexa is currently speaking
     */
    public boolean isSpeaking() {
        return speechState == SpeechState.PLAYING;
    }

    /**
     * Returns true if Alexa is currently playing media
     */
    public boolean isPlayingOrPaused() {
        return isPlaying() || audioPlayerStateMachine.getState() == AudioPlayerStateMachine.AudioPlayerState.PAUSED;
    }

    private boolean isPlaying() {
        return (audioPlayerStateMachine.getState() == AudioPlayerStateMachine.AudioPlayerState.PLAYING
                || audioPlayerStateMachine.getState() == AudioPlayerStateMachine.AudioPlayerState.PAUSED
                || audioPlayerStateMachine.getState() == AudioPlayerStateMachine.AudioPlayerState.BUFFER_UNDERRUN);
    }

    /**
     * Returns true if Alexa is currently playing an alarm sound
     */
    public boolean isAlarming() {
        return alertState == AlertState.PLAYING;
    }

    /**
     * Interrupt all audio - Alarms, speech, and media
     */
    public void interruptAllAlexaOutput() {
        Log.d("Interrupt Alexa", "Interrupting all Alexa output");
        if (isSpeaking()) {
            // Then we are interrupting some speech
            interruptCurrentlyPlaying();
        }
        speakQueue.clear();

        interruptAlertsAndContent();
    }

    /**
     * Interrupt only alerts and content
     */
    private void interruptAlertsAndContent() {
        if (isAlarming()) {
            alertState = AlertState.INTERRUPTED;
        }

        interruptContent();
    }

    /**
     * Interrupt only content
     */
    private void interruptContent() {

        synchronized (audioPlayer) {
            if (!playQueue.isEmpty() && isPlaying() && audioPlayer.isPlaying()) {
                Log.d("AudioPlayer", "AudioPlayer content interrupted");
                audioPlayer.pause();
            }
        }
    }

    /**
     * Resume all audio from interrupted state. Since the speech queue is cleared when interrupted,
     * resuming speech is not necessary
     */
    public void resumeAllAlexaOutput() {
        Log.d("Resuming Alexa", "Resuming all Alexa output");
        if (speakQueue.isEmpty() && !resumeAlerts()) {
            resumeContent();
        }
    }

    /**
     * Resume alert audio
     */
    private boolean resumeAlerts() {
        if (alertState == AlertState.INTERRUPTED) {
            startAlert();
            return true;
        }
        return false;
    }

    /**
     * Resume any content
     */
    private void resumeContent() {
        synchronized (audioPlayer) {
            if (!playQueue.isEmpty() && isPlayingOrPaused()
                    && !audioPlayer.isPlaying()) {
                // Pause toggles the pause state of the media player, if it was previously paused it
                // will be resumed.

                //Starts or resumes playback. If playback had previously been paused, playback will
                // continue from where it was paused. If playback had been stopped, or never started before, playback will start at the beginning.
                Log.d("AudioPlayer", "AudioPlayer content resumed");
                audioPlayer.start();
            }
        }
    }

    /**
     * Add audio to be played by the media player. This is triggered by the play directive
     *
     * @param stream
     *            Stream to add to the play queue
     */
    private void add(Stream stream) {
        String expectedPreviousToken = stream.getExpectedPreviousToken();

        boolean startPlaying = playQueue.isEmpty();

        if (expectedPreviousToken == null || latestStreamToken.isEmpty()
                || latestStreamToken.equals(expectedPreviousToken)) {
            playQueue.add(stream);
        }

        if (startPlaying) {
            startPlayback();
        }
    }

    /**
     * Play media in the play queue
     */
    private void startPlayback() {
        if (playQueue.isEmpty()) {
            return;
        }

        Thread thread = new Thread() {

            @Override
            public void run() {
                // wait for any speech to complete before starting playback
                // also wait for playbackFinished to be called after getNextItem
                while (!speakQueue.isEmpty() || waitForPlaybackFinished) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.d("Error", "Interupted while waiting to start playback", e);
                    }
                }

                Stream stream = playQueue.peek();

                if (stream == null) {
                    // if a stop/clearQueue came down before we started
                    return;
                }

                latestStreamToken = stream.getToken();

                if (!playItem(stream)) {
                    // an error will be reported from the vlcj listener
                    return;
                }

                if (isSpeaking() || isAlarming()) {
                    // pause if Alexa is speaking or there is an active alert.
                    interruptContent();
                }
            }
        };
        thread.start();
    }

    /**
     * Play the media from the given url, at the given offset
     *
     * @param stream
     *            The Stream object we will be playing
     * @return true if played successfully, false otherwise
     */
    private boolean playItem(Stream stream) {
        synchronized (audioPlayer) {

            // Reset url caches and state information
            streamUrls = new HashSet<String>();
            attemptedUrls = new HashSet<String>();

            // Resetting the audio player is necessary to prevent hanging behavior
            // when listening to some long-running music tracks
//            setupAudioPlayer();

            String url = stream.getUrl();
            long offset = stream.getOffsetInMilliseconds();

            Log.d("Play", "playing " + url);

            timer.reset(); // Clear the old values
            try{
                playAudio(url, offset);

                if (stream.hasAttachedContent()) {
                    cachedAudioFiles.put(audioPlayer.mrl(), url);
                }

                return true;
            } catch (Exception e){
                Log.d("Error", e.getMessage(), e);
                return false;
            }
        }
    }

    private void playAudio(String url)throws IOException {
        playAudio(url, 0);
    }

    private void playAudio(String url, long offset) throws IOException {
        audioPlayer.reset();
        audioPlayer.setDataSource(url);
        audioPlayer.prepare();
        audioPlayer.start();
        if (offset > 0) {
            audioPlayer.seekTo((int) offset);
        }
    }

    /**
     * Stop all media playback
     */
    public void stop() {
        synchronized (audioPlayer) {
            if (!playQueue.isEmpty() && isPlayingOrPaused()) {

                // Stop keeping track of the offset and sending reporting events
                stopTimerAndProgressReporter();

                audioPlayer.stop();
            }
        }
    }

    /**
     * Play items from the speech play queue
     */
    private void startSpeech() {

        final SpeakItem speak = speakQueue.peek();

        // This addresses the possibility of the queue being cleared
        // between the time of this function call and this line of code.
        if (speak == null) {
            return;
        }

        notifyAlexaSpeechStarted();

        speechState = SpeechState.PLAYING;
        latestToken = speak.getToken();

        controller
                .sendRequest(RequestFactory.createSpeechSynthesizerSpeechStartedEvent(latestToken));

        Thread thread = new Thread() {
            @Override
            public void run() {
                synchronized (playLock) {
                    try {
                        InputStream inpStream = speak.getAudio();
                        interruptAlertsAndContent();
                        play(inpStream, true);
                        while (inpStream.available() > 0) {
                            playLock.wait(TIMEOUT_IN_MS);
                        }
                    } catch (InterruptedException | IOException e) {
                    }

//                    finishedSpeechItem();
                }
            }
        };
        thread.start();
    }

    /**
     * When a speech item is finished, perform the necessary actions
     */
    private void finishedSpeechItem() {
        // remove the finished item
        speakQueue.poll();

        if (speakQueue.isEmpty()) {
            speechState = SpeechState.FINISHED;
            controller.sendRequest(
                    RequestFactory.createSpeechSynthesizerSpeechFinishedEvent(latestToken));

            notifyAlexaSpeechFinished();
        } else {
            // if not done start the next speech
            startSpeech();
        }
    }

    /**
     * Clear the queue of items to play, but keep the most recent item.
     */
    public void clearEnqueued() {
        // save the top item
        Stream top = playQueue.poll();
        // clear the queue and re-add the top item
        playQueue.clear();
        if (top != null) {
            playQueue.add(top);
        }
    }

    /**
     * Clear all media scheduled to play, including items currently playing
     */
    public void clearAll() {
        // stop playback and clear all
        stop();
        playQueue.clear();
    }

    /**
     * Get the position of the currently playing media item
     *
     * @return The position in milliseconds of the stream
     */
    private long getProgress() {
        synchronized (audioPlayer) {
            return timer.getOffsetInMilliseconds();
        }
    }

    /**
     * Start/resume the AudioPlayer Timer and ProgressReporter
     */
    private void startTimerAndProgressReporter() {
        timer.start();
        if (progressReporter.isSetup()) {
            progressReporter.start();
        }
    }

    /**
     * Stop/pause the AudioPlayer Timer and ProgressReporter
     */
    private void stopTimerAndProgressReporter() {
        timer.stop();
        progressReporter.stop();
    }

    /**
     * Get the playback state of the media player
     */
    public PlaybackStatePayload getPlaybackState() {
        AudioPlayerStateMachine.AudioPlayerState playerState = audioPlayerStateMachine.getState();

        long offset = getCurrentOffsetInMilliseconds();

        return new PlaybackStatePayload(latestStreamToken, offset, playerState.toString());
    }

    public String getCurrentStreamToken() {
        return latestStreamToken;
    }

    public long getPlaybackStutterStartedOffsetInMilliseconds() {
        return playbackStutterStartedOffsetInMilliseconds;
    }

    public long getCurrentOffsetInMilliseconds() {
        AudioPlayerStateMachine.AudioPlayerState playerActivity = audioPlayerStateMachine.getState();

        long offset;
        switch (playerActivity) {
            case PLAYING:
            case PAUSED:
            case BUFFER_UNDERRUN:
            case STOPPED:
            case FINISHED:
                offset = getProgress();
                break;
            case IDLE:
            default:
                offset = 0;
        }

        return offset;
    }

    /**
     * Get the speech state
     */
    public SpeechStatePayload getSpeechState() {
        String contentId = latestToken;
        return new SpeechStatePayload(contentId, getPlayerPosition(), speechState.name());
    }

    public VolumeStatePayload getVolumeState() {
        return new VolumeStatePayload(getVolume(), isMuted());
    }

    public long getVolume() {
        return (long) (currentVolume / VOLUME_SCALAR);
    }

    public boolean isMuted() {
        return currentlyMuted;
    }

    /**
     * Returns the offset in milliseconds of the default audio player. If there is no player
     * position, this function defaults to 0
     *
     * @return Player offset in milliseconds
     */
    private synchronized long getPlayerPosition() {
        long offsetInMilliseconds = 0;
        if (speaker != null) {
            offsetInMilliseconds = speaker.getCurrentPosition();
        }
        return offsetInMilliseconds;
    }

    /**
     * plays MP3 data from a resource asynchronously. will stop any previous playback and start the
     * new audio
     */
    public synchronized void playMp3FromResource(String resource) {
        try {
            FileInputStream fileInputStream = new FileInputStream(resource);
            play(fileInputStream, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Play the alarm sound
     */
    public void startAlert() {
        if (!isAlarming()) {
            interruptContent();
            if (isSpeaking()) {
                // alerts are in the background when Alexa is speaking
                alertState = AlertState.INTERRUPTED;
            } else {
                alertState = AlertState.PLAYING;

                alarmThread = new Thread() {
                    @Override
                    public void run() {
                        while (isAlarming() && !isSpeaking()) {
                            if (Thread.interrupted()) {
                                break;
                            }
                            InputStream inpStream = resLoader.getResourceAsStream("/sdcard/alarm.mp3");
                            synchronized (playLock) {
                                try {
                                    play(inpStream, false);
                                    while (inpStream.available() > 0) {
                                        playLock.wait(TIMEOUT_IN_MS);
                                    }
                                } catch (InterruptedException | IOException e) {
                                }
                            }
                        }
                    }
                };
                alarmThread.start();
            }
        }
    }

    /**
     * Stop the alarm
     */
    public void stopAlert() {
        interruptCurrentlyPlaying();
        alertState = AlertState.FINISHED;
    }

    /**
     * Interrupt whatever audio is currently playing through the default audio player
     */
    private synchronized void interruptCurrentlyPlaying() {
        if (playThread != null) {
            playThread.interrupt();
        }
        stopPlayer();
    }

    /**
     * Ends playback of the default audio player
     */
    private synchronized void stopPlayer() {
        if (speaker != null) {
            speaker.stop();
            speaker.release();
            speaker = null;
            if (isSpeaking()) {
                speechState = SpeechState.FINISHED;
                notifyAlexaSpeechFinished();
            }
        }
    }


    /**
     * Play a generic input stream through the default audio player
     */
    private synchronized void play(final InputStream inpStream, final boolean speech) {
        playThread = new Thread() {
            @Override
            public void run() {
                synchronized (playLock) {
                    try {
                        speak(inpStream, speech);
                    } catch (Exception e) {
                        Log.d("Error", "An error occurred while trying to play audio", e);
                    } finally {
                        IOUtils.closeQuietly(inpStream);
                    }
                    playLock.notifyAll();
                }
            }
        };
        playThread.start();
    }

    private void speak(InputStream inputStream, boolean speech) {
        try {
            File tmp = File.createTempFile(UUID.randomUUID().toString(), ".mp3");
            tmp.deleteOnExit();
            copyInputStreamToFile(inputStream, tmp);
            setupSpeaker(speech);
            speaker.setDataSource(tmp.getAbsolutePath());
            speaker.prepare();
            speaker.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setupSpeaker(boolean speech) {
        speaker = new CustomMediaPlayer();
        speaker.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if(speech){
            speaker.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    finishedSpeechItem();
                }
            });
        }

    }

    private void notifyAlexaSpeechStarted() {
        for (AlexaSpeechListener listener : listeners) {
            listener.onAlexaSpeechStarted();
        }
    }

    private void notifyAlexaSpeechFinished() {
        for (AlexaSpeechListener listener : listeners) {
            listener.onAlexaSpeechFinished();
        }
    }

    private static class ProgressReportDelayEventRunnable implements Runnable {

        private final AudioPlayerStateMachine playbackStateMachine;

        public ProgressReportDelayEventRunnable(AudioPlayerStateMachine playbackStateMachine) {
            this.playbackStateMachine = playbackStateMachine;
        }

        @Override
        public void run() {
            playbackStateMachine.reportProgressDelay();
        }
    };

    private static class ProgressReportIntervalEventRunnable implements Runnable {

        private final AudioPlayerStateMachine playbackStateMachine;

        public ProgressReportIntervalEventRunnable(AudioPlayerStateMachine playbackStateMachine) {
            this.playbackStateMachine = playbackStateMachine;
        }

        @Override
        public void run() {
            playbackStateMachine.reportProgressInterval();
        }
    }

    public interface AlexaSpeechListener {
        void onAlexaSpeechStarted();

        void onAlexaSpeechFinished();
    }

    private void deleteCachedFile(final String uri) {
        File file = new File(uri);
        file.delete();
    }
}
