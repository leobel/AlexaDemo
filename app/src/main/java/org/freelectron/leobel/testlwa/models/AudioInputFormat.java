package org.freelectron.leobel.testlwa.models;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by leobel on 2/21/17.
 */

public enum AudioInputFormat {
    LPCM(Constants.LPCM_CHUNK_SIZE_BYTES, Constants.LPCM_CHUNK_SIZE_MS, Constants.LPCM_SAMPLE_FREQUENCY_RATE, Constants.LPCM_SAMPLE_SIZE_BITS, Constants.LPCM_CHANNELS,  Constants.LPCM_AUDIO_FORMAT, Constants.LPCM_CONTENT_TYPE);

    private final int chunkSizeBytes;
    private final int chunkSizeMs;
    private final AudioFormat audioFormat;
    private final int sampleFrequencyRate;
    private final int sampleSizeBits;
    private final int channels;
    private final String contentType;


    private AudioInputFormat(final int chunkSizeBytes, final int chunkSizeMs, int sampleFrequencyRate, int sampleSizeBits, int channels, int audioFormat, String contentType) {
        this.chunkSizeBytes = chunkSizeBytes;
        this.chunkSizeMs = chunkSizeMs;
        this.sampleFrequencyRate = sampleFrequencyRate;
        this.sampleSizeBits = sampleSizeBits;
        this.channels = channels;

        this.audioFormat = new AudioFormat.Builder()
                .setSampleRate(sampleFrequencyRate)
                .setEncoding(audioFormat)
                .setChannelMask(channels)
                .build();

        this.contentType = contentType;
    }

    public int getChunkSizeBytes() {
        return chunkSizeBytes;
    }

    public int getChunkSizeMs() {
        return chunkSizeMs;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSampleRate() {
        return sampleFrequencyRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeBits;
    }

    public int getChannels() {
        return channels;
    }

    public int getMinBufferSise(){
        return AudioRecord.getMinBufferSize(audioFormat.getSampleRate(), audioFormat.getChannelMask(), audioFormat.getEncoding());
    }

    private static final class Constants {
        private static final int LPCM_CHUNK_SIZE_BYTES = 320;
        private static final int LPCM_CHUNK_SIZE_MS = 10;
        private static final int LPCM_SAMPLE_FREQUENCY_RATE = 16000;
        private static final int LPCM_SAMPLE_SIZE_BITS = 16;
        private static final int LPCM_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        private static final boolean LPCM_SIGNED = true;
        private static final boolean LPCM_BIG_ENDIAN_BYTE_ORDER = false;

//        private static final AudioFormat LPCM_AUDIO_FORMAT1 = new AudioFormat(16000f, 16, 1, true, false);
        private static final int LPCM_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
        private static final String LPCM_CONTENT_TYPE = "audio/L16; rate=16000; channels=1";
    }
}