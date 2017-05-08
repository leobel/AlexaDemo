package org.freelectron.leobel.testlwa;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by leobel on 3/1/17.
 */

public class CustomMediaPlayer extends MediaPlayer {

    private String url;

    @Override
    public void setDataSource(String url) throws IOException, IllegalArgumentException, IllegalStateException {
        super.setDataSource(url);

        this.url = url;
    }

    public String mrl(){
        return url;
    }
}
