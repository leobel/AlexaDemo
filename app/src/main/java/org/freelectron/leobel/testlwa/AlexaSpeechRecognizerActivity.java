package org.freelectron.leobel.testlwa;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.freelectron.leobel.testlwa.models.AVSAPIConstants;
import org.freelectron.leobel.testlwa.models.AVSAudioPlayerFactory;
import org.freelectron.leobel.testlwa.models.AVSClient;
import org.freelectron.leobel.testlwa.models.AVSClientFactory;
import org.freelectron.leobel.testlwa.models.AVSController;
import org.freelectron.leobel.testlwa.models.AlertManagerFactory;
import org.freelectron.leobel.testlwa.models.DialogRequestIdAuthority;
import org.freelectron.leobel.testlwa.models.ExpectSpeechListener;
import org.freelectron.leobel.testlwa.models.ExpectStopCaptureListener;
import org.freelectron.leobel.testlwa.models.RecordingRMSListener;
import org.freelectron.leobel.testlwa.models.message.DialogRequestIdHeader;
import org.freelectron.leobel.testlwa.models.message.Directive;
import org.freelectron.leobel.testlwa.models.message.MessageIdHeader;
import org.freelectron.leobel.testlwa.models.message.Payload;
import org.freelectron.leobel.testlwa.models.message.request.AVSRequest;
import org.freelectron.leobel.testlwa.models.AudioInputFormat;
import org.freelectron.leobel.testlwa.models.HttpHeaders;
import org.freelectron.leobel.testlwa.models.MultipartParser;
import org.freelectron.leobel.testlwa.models.message.request.ComponentStateFactory;
import org.freelectron.leobel.testlwa.models.message.request.RequestListener;
import org.freelectron.leobel.testlwa.models.message.request.context.PlaybackStatePayload;
import org.freelectron.leobel.testlwa.models.message.response.AlexaExceptionResponse;
import org.freelectron.leobel.testlwa.models.exception.AlexaSystemException;
import org.freelectron.leobel.testlwa.models.message.request.Event;
import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;
import org.freelectron.leobel.testlwa.models.message.request.speechrecognizer.SpeechProfile;
import org.freelectron.leobel.testlwa.models.message.request.speechrecognizer.SpeechRecognizerPayload;
import org.freelectron.leobel.testlwa.models.message.response.speechrecognizer.StopCapture;
import org.freelectron.leobel.testlwa.services.AlexaService;
import org.freelectron.leobel.testlwa.services.PreferenceService;
import org.freelectron.leobel.testlwa.services.SecurityInterceptor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.internal.http2.StreamResetException;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class AlexaSpeechRecognizerActivity extends BaseActivity implements ExpectSpeechListener, ExpectStopCaptureListener,  AVSController.CloseConnectionListener, RecordingRMSListener {

    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 12345;
    private static final String TAG = "DownChannel";

    @Inject
    public AlexaService alexaService;

    @Inject
    public PreferenceService preferenceService;

    @Inject
    public ObjectMapper objectMapper;

//    @Inject
//    public OkHttpClient httpClient;

    private ProgressBar progressBar;


    AlertDialog dialog;
    private AudioRecord mAudioRecord;
    private AudioInputFormat audioInputFormat;
    private Button stopButton;

    public OkHttpClient httpClient;


    String directives = BuildConfig.ALEXA_BASE_API_URL + BuildConfig.ALEXA_API_VERSION + "/directives";
    String events = BuildConfig.ALEXA_BASE_API_URL + BuildConfig.ALEXA_API_VERSION + "/events";

    private boolean restartConnection;
    private MediaPlayer mp;
    private Observable<Directive> allMessage;
    private ParcelFileDescriptor.AutoCloseOutputStream audioStreaming;
    private AVSController controller;
    private boolean recording;
    private RecordingThread mRecordingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alexa_speech_recognizer);

        TestLWAApp.getInstance().getComponent().inject(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);

        audioInputFormat = AudioInputFormat.LPCM;

        stopButton = (Button) findViewById(R.id.stop_record);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission(MY_PERMISSIONS_REQUEST_RECORD_AUDIO, R.string.require_permission, Manifest.permission.RECORD_AUDIO)){
                    startRecording();
                }
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        authorizeListener.checkAccessToken();
    }

    @Override
    public void onLoginSuccess(String accessToken) {
        super.onLoginSuccess(accessToken);

        try {
            stopButton.setEnabled(true);
            controller = new AVSController(this, this, new AVSAudioPlayerFactory((AudioManager) getSystemService(Context.AUDIO_SERVICE)), new AlertManagerFactory(), new AVSClientFactory(), DialogRequestIdAuthority.getInstance(), preferenceService);
            controller.initializeStopCaptureHandler(this);
            controller.startHandlingDirectives();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        controller.startRecording(this);
    }

    public boolean checkPermission(final int requestCode, int permissionExplanation, final String... permissions) {
        boolean permissionGranted = true;
        for (String permission: permissions) {
            if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                permissionGranted = false;
                break;
            }
        }
        if (!permissionGranted) {
            // Should we show an explanation?
            boolean shouldShowRequestPermissionRationale = true;
            for (String permission: permissions) {
                if(!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
                    shouldShowRequestPermissionRationale = false;
                    break;
                }
            }
            if (shouldShowRequestPermissionRationale) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                dialog = new AlertDialog.Builder(this)
                        .setMessage(permissionExplanation)
                        .setNegativeButton(R.string.premission_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.premission_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(dialog.getOwnerActivity(), permissions, requestCode);
                            }
                        })
                        .create();

                dialog.show();

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, permissions, requestCode);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // storage-related task you need to do.
                    startRecording();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    @Override
    public void onExpectSpeechDirective() {
        controller.startRecording(this);
    }

    @Override
    public void onStopCaptureDirective() {
        controller.stopRecording();
    }



    @Override
    public void rmsChanged(int rms) {

    }

    @Override
    public void onCloseConnection() {
        preferenceService.setAccessToken("");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    //    @Override
//    public void onDirective(Directive directive) {
//        Log.d("RECEIVING DIRECTIVE", "Directive: " + directive);
//        if(directive.getPayload() instanceof StopCapture){
//            multipartParser.shutdownMultiParserGracefully();
//        }
//    }
//
//    @Override
//    public void onDirectiveAttachment(String contentId, InputStream attachmentContent) {
//        Log.d("RECEIVING AUDIO", "AUDIO: " + contentId);
////        FileInputStream fileInputStream = new FileInputStream(attachmentContent);
//        playAudio(attachmentContent);
//    }

}
