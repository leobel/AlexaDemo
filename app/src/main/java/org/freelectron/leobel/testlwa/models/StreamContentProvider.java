package org.freelectron.leobel.testlwa.models;

import org.apache.commons.io.IOUtils;
import org.freelectron.leobel.testlwa.PlaybackThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by leobel on 3/3/17.
 */

public class StreamContentProvider implements AudioCapture.ContentProvider {

    private boolean streaming;
    private PipedInputStream pipedInputStream;
    private PipedOutputStream pipedOutputStream;
    private Queue<byte[]> streamData;

    public StreamContentProvider(int bufferSize) throws IOException{
        pipedInputStream = new PipedInputStream(bufferSize);
        pipedOutputStream = new PipedOutputStream(pipedInputStream);
        streaming = true;
        streamData = new ArrayDeque<>();
    }

    @Override
    public void read(short[] data, int byteCount){
        byte[] buffer = short2byte(data, byteCount);
        try {
            pipedOutputStream.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getContent() {
        return pipedInputStream;
    }

    @Override
    public void onClose() {
        streaming = false;
        try {
            pipedOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] short2byte(short[] sData, int length) {
//        byte[] bytes = new byte[length * 2];
//        for (int i = 0; i < length; i++) {
//            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
//            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
//        }
//        return bytes;
        ByteBuffer bb = ByteBuffer.allocate(2 * length).order(ByteOrder.LITTLE_ENDIAN);
        bb.asShortBuffer().put(sData, 0, length);
        return bb.array();
    }

    public boolean hasData(){
        return streaming;
    }


    public byte[] getData(){
        return streamData.poll();
    }

}
