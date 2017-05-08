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


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import rx.Observable;
import rx.Subscriber;

public class AudioCapture {
    private static AudioCapture sAudioCapture;
    private final short[] buffer;
    private AudioRecord audioRecord;
    private boolean recording;
    private AudioInputFormat audioFormat;
    private AudioBufferThread thread;

    private static final int BUFFER_SIZE_IN_SECONDS = 6;

    private final int BUFFER_SIZE_IN_BYTES;

    public static AudioCapture getAudioHardware(final AudioInputFormat audioFormat){
        if (sAudioCapture == null) {
            sAudioCapture = new AudioCapture(audioFormat);
        }
        return sAudioCapture;
    }

    private AudioCapture(final AudioInputFormat audioFormat) {
        super();
        this.audioFormat = audioFormat;

//        BUFFER_SIZE_IN_BYTES = ((audioFormat.getSampleSizeInBits() * audioFormat.getSampleRate()) / 8 * BUFFER_SIZE_IN_SECONDS);
        BUFFER_SIZE_IN_BYTES = AudioRecord.getMinBufferSize( 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT );
//        audioRecord = new AudioRecord.Builder()
//                .setAudioFormat(audioFormat.getAudioFormat())
//                .setAudioSource(MediaRecorder.AudioSource.MIC)
//                .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
//                .build();
        buffer = new short[BUFFER_SIZE_IN_BYTES];

    }

    public StreamContentProvider getAudioContentProvider(final RecordingStateListener stateListener, final RecordingRMSListener rmsListener) throws IOException {
        try {
            startCapture();
//            PipedInputStream inputStream = new PipedInputStream(BUFFER_SIZE_IN_BYTES);
            StreamContentProvider provider = new StreamContentProvider(BUFFER_SIZE_IN_BYTES);
            thread = new AudioBufferThread(provider, stateListener, rmsListener);
            thread.start();
            return provider;
        } catch (IOException e) {
            stopCapture();
            throw e;
        }
    }

    public void stopCapture() {
//        microphoneLine.stop();
//        microphoneLine.close();
        recording = false;
        audioRecord.stop();
        audioRecord.release();
    }

    private void startCapture(){
//        microphoneLine.open(audioFormat);
//        microphoneLine.start();
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_IN_BYTES);
        recording = true;
        audioRecord.startRecording();
    }

    public int getAudioBufferSizeInBytes() {
        return audioFormat.getChunkSizeBytes();
    }

    private class AudioBufferThread extends Thread{

//        private final AudioStateOutputStream audioStateOutputStream;
        private final RecordingStateListener stateListener;
        private final RecordingRMSListener rmsListener;
        private final ContentProvider provider;

        public AudioBufferThread(ContentProvider provider, RecordingStateListener recordingStateListener, RecordingRMSListener rmsListener) throws IOException {
//            audioStateOutputStream = new AudioStateOutputStream(inputStream, recordingStateListener, rmsListener);
            this.provider = provider;
            this.stateListener = recordingStateListener;
            this.rmsListener = rmsListener;
            notifyRecordingStarted();
        }

        @Override
        public void run() {
            while (recording) {
                copyAudioBytesFromInputToOutput();
            }
            closePipedOutputStream();
        }

        private void copyAudioBytesFromInputToOutput() {
            int numBytesRead = audioRecord.read(buffer, 0, buffer.length);
            provider.read(buffer, numBytesRead);
//            try {
//                byte data[] = short2byte(buffer);
//                audioStateOutputStream.write(data, 0, numBytesRead);
//            } catch (IOException e) {
//                stopCapture();
//            }
        }

        private void closePipedOutputStream() {
            provider.onClose();
            notifyRecordingCompleted();
            clearRMS();
//            try {
//                audioStateOutputStream.close();
//            } catch (IOException e) {
//                Log.d("Failing close stream ", e.getMessage(), e);
//            }
        }



        private void notifyRecordingStarted() {
            if (stateListener != null) {
                stateListener.recordingStarted();
            }
        }

        private void notifyRecordingCompleted() {
            if (stateListener != null) {
                stateListener.recordingCompleted();
            }
        }

        private void clearRMS() {
            if (rmsListener != null) {
                rmsListener.rmsChanged(0);
            }
        }

        // rmsListener is the AudioRMSListener callback for audio visualizer(optional - can be null)
        // assuming 16bit samples, 1 channel, little endian
        private void calculateDB(byte[] data, int cnt) {
            if ((rmsListener == null) || (cnt < 2)) {
                return;
            }

            final int bytesPerSample = 2;
            int len = cnt / bytesPerSample;
            double avg = 0;

            for (int i = 0; i < cnt; i += bytesPerSample) {
                ByteBuffer bb = ByteBuffer.allocate(bytesPerSample);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(data[i]);
                bb.put(data[i + 1]);
                // generate the signed 16 bit number from the 2 bytes
                double dVal = Math.abs(bb.getShort(0));
                // scale it from 1 to 100. Use max/2 as values tend to be low
                dVal = ((100 * dVal) / (Short.MAX_VALUE / 2.0)) + 1;
                avg += dVal * dVal; // add the square to the running average
            }
            avg /= len;
            avg = Math.sqrt(avg);
            // update the AudioRMSListener callback with the scaled root-mean-squared power value
            rmsListener.rmsChanged((int) avg);
        }
    }

    public interface ContentProvider{
        void read(short[] data, int byteCount);
        InputStream getContent();
        void onClose();
    }

}
