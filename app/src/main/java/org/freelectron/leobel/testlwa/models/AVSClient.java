package org.freelectron.leobel.testlwa.models;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.freelectron.leobel.testlwa.BuildConfig;
import org.freelectron.leobel.testlwa.PlaybackThread;
import org.freelectron.leobel.testlwa.config.ObjectMapperFactory;
import org.freelectron.leobel.testlwa.models.exception.AlexaSystemException;
import org.freelectron.leobel.testlwa.models.exception.AlexaSystemExceptionCode;
import org.freelectron.leobel.testlwa.models.message.DialogRequestIdHeader;
import org.freelectron.leobel.testlwa.models.message.Directive;
import org.freelectron.leobel.testlwa.models.message.MessageIdHeader;
import org.freelectron.leobel.testlwa.models.message.Payload;
import org.freelectron.leobel.testlwa.models.message.request.AVSRequest;
import org.freelectron.leobel.testlwa.models.message.request.Event;
import org.freelectron.leobel.testlwa.models.message.request.RequestListener;
import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;
import org.freelectron.leobel.testlwa.models.message.request.speechrecognizer.SpeechProfile;
import org.freelectron.leobel.testlwa.models.message.request.speechrecognizer.SpeechRecognizerPayload;
import org.freelectron.leobel.testlwa.models.message.response.AlexaExceptionResponse;
import org.freelectron.leobel.testlwa.services.PreferenceService;
import org.freelectron.leobel.testlwa.services.SecurityInterceptor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by leobel on 2/28/17.
 */

public class AVSClient implements Observable.OnSubscribe<Directive> {

    private static final String TAG = "Request";
    private MultipartParser multipartParser;
    private ConnectionListener connectionListener;
    private Observable<Directive> directiveObservable;
    private LinkedBlockingDeque<AVSRequest> requestQueue;
    private RequestThread requestThread;
    private DownchannelRequestThread downChannelThread;


    String directives = BuildConfig.ALEXA_BASE_API_URL + BuildConfig.ALEXA_API_VERSION + "/directives";
    String events = BuildConfig.ALEXA_BASE_API_URL + BuildConfig.ALEXA_API_VERSION + "/events";

    private OkHttpClient httpClient;
    private Response downChannel;
    private Subscriber<? super Directive> subscriber;
    private URL host;

    public AVSClient(ConnectionListener connectionListener, MultipartParser.MultipartParserConsumer multipartParserConsumer, PreferenceService preferenceService) {
        this.connectionListener = connectionListener;
        httpClient = initializeHttpClient(preferenceService);
        multipartParser = new MultipartParser(multipartParserConsumer);

        directiveObservable = Observable.create(this);
        requestQueue = new LinkedBlockingDeque<>();
        requestThread = new RequestThread(requestQueue);
        startRequestThread();
        startDownchannelThread();
    }

    @Override
    public void call(Subscriber<? super Directive> subscriber) {
        this.subscriber = subscriber;
    }

    public void shutdownMultiParserGracefully() {
        multipartParser.shutdownGracefully();
    }

    public URL getHost() {
        return host;
    }


    public interface ConnectionListener{
        void onConnectionSuccess();
        void onSynchronizeSuccess();
        void onCloseConnection(Throwable  e);
    }

    interface FilterDirective{
        boolean filter(Directive message);
    }


    /**
     * When the application shuts down make sure to clean up the HTTPClient
     */
    public void shutdown() {
        try {
            shutdownMultiParserGracefully();
            requestThread.shutdown();
            closeConnection();
        } catch (Exception e) {
        }
    }


    public void synchronizeComponents() {
        // Synchronizing your product’s component states with AVS (AudioPlayer, Alerts, Speaker, SpeechSynthesizer)
        AVSRequest avsRequest = new AVSRequest();

        List<ComponentState> context = new ArrayList<>();
//        context.add(alertState);
//        context.add(playbackState);
//        context.add(volumeState);
//        context.add(speechState);

        MessageIdHeader header = new MessageIdHeader();
        header.setNamespace(AVSAPIConstants.System.NAMESPACE);
        header.setName(AVSAPIConstants.System.Events.SynchronizeState.NAME);
        final String id = String.valueOf(System.currentTimeMillis());
        header.setMessageId("messageRequestId-" + id);

        Event event = new Event(header, new Payload());

        avsRequest.setContext(context);
        avsRequest.setEvent(event);
        sendEvent(avsRequest);
    }

    public void SpeechRecognizerEvent(Observable<byte[]> streamProvider){
        AVSRequest avsRequest = new AVSRequest();

        List<ComponentState> context = new ArrayList<>();
//        context.add(alertState);
//        context.add(playbackState);
//        context.add(volumeState);
//        context.add(speechState);

        DialogRequestIdHeader header = new DialogRequestIdHeader();
        header.setNamespace(AVSAPIConstants.SpeechRecognizer.NAMESPACE);
        header.setName(AVSAPIConstants.SpeechRecognizer.Events.Recognize.NAME);
        final String id = String.valueOf(System.currentTimeMillis());
        header.setMessageId("messageRequestId-" + id);
        header.setDialogRequestId("dialogRequestId-" + id);

        SpeechRecognizerPayload payload = new SpeechRecognizerPayload(SpeechProfile.NEAR_FIELD);
        Event event = new Event(header, payload);

        avsRequest.setContext(context);
        avsRequest.setEvent(event);
        avsRequest.setStreamProvider(streamProvider);
        sendEvent(avsRequest);
    }

    public Observable<Directive> addObservable(final FilterDirective filter){
        return directiveObservable.filter(new Func1<Directive, Boolean>() {
            @Override
            public Boolean call(Directive message) {
                return filter.filter(message);
            }
        });
    }


//    @Override
//    public void onDirective(Directive directive) {
//        subscriber.onNext(directive);
//    }
//
//    @Override
//    public void onDirectiveAttachment(String contentId, InputStream attachmentContent) {
//        attachListener.onAttach(contentId, attachmentContent);
//    }

    private void enqueueRequest(AVSRequest request) {
        if (!requestQueue.offer(request)) {
            Log.d("Failed", "Failed to enqueue request");
        }
    }

    private void startRequestThread() {
        if (!requestThread.isAlive()) {
            requestThread.start();
        }
    }

    private void startDownchannelThread() {
        downChannelThread = new DownchannelRequestThread();
        downChannelThread.start();
    }

    private class DownchannelRequestThread extends Thread{

        public DownchannelRequestThread() {
            setName(this.getClass().getSimpleName());
        }

        @Override
        public void run() {
            createConnection();
        }
    }

    private class RequestThread extends Thread{
        private BlockingQueue<AVSRequest> queue;
        private boolean running;

        public RequestThread(BlockingQueue<AVSRequest> queue) {
            this.queue = queue;
            running = true;
            setName(this.getClass().getSimpleName());
        }

        public void shutdown(){
            running = false;
        }

        @Override
        public void run() {
            while (running){
                try {
                    AVSRequest request = queue.take();
                    sendRequest(request);
                } catch (InterruptedException e) {
                    Log.d("Request thread error", e.getMessage(), e);
                }
            }
        }
    }

    private void createConnection(){
        Request request = new Request.Builder()
                .url(directives)
                .get()
                .build();

        AVSRequest avsRequest = new AVSRequest();
        avsRequest.setRequest(request);

        avsRequest.setRequestListener(new RequestListener() {
            @Override
            public void onRequestSuccess(Response response) {
                try{
                    downChannel = response;
                    if(response.isSuccessful()){
                        connectionListener.onConnectionSuccess();

                        Buffer buffer = new Buffer();
                        BufferedSource bufferedSource = response.body().source();
                        while (!bufferedSource.exhausted())
                        {
                            Log.d(TAG, "downchannel received data!!!:");
                            InputStream stream = bufferedSource.inputStream();
                            while (stream.available() > 0) {
                                long size = bufferedSource.read(buffer, 8192);
                                Log.d(TAG, "READING downchannel data size: " + size);
                            }
                            String contentType = response.header(HttpHeaders.CONTENT_TYPE);
                            InputStream inputStream = buffer.inputStream();
                            String boundary = getHeaderParameter(contentType, HttpHeaders.Parameters.BOUNDARY);
                            if(boundary != null){
                                multipartParser.parseStream(inputStream, boundary);
                            }
                            else{
                                handleException(inputStream);
                            }
                        }
                    }
                    else{
                        handleException(response.body().byteStream());
                    }
                }
                catch (Exception e){
                    handleRequestException(e);
                }

            }

            @Override
            public void onRequestError(Throwable e) {
                handleRequestException(e);
            }
        });

        sendRequest(avsRequest);
    }


    public void sendEvent(AVSRequest avsRequest) {
        try {
            RequestBody requestBody = buildMultipartBody(avsRequest, avsRequest.getStreamContentProvider());
            Request request = new Request.Builder()
                    .url(events)
                    .post(requestBody)
                    .build();
            avsRequest.setRequest(request);

            Log.d("Request data", avsRequest.getEvent().getHeader().getName() + ":\n" + ObjectMapperFactory.getObjectWriter()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(avsRequest));

            avsRequest.setRequestListener(new RequestListener() {
                @Override
                public void onRequestSuccess(Response response) {
                    try {
                        if(response.code() != 204){
                            Buffer buffer = new Buffer();
                            BufferedSource bufferedSource = response.body().source();

                            while (!bufferedSource.exhausted())
                            {
                                response.body().source().read(buffer, 8192);
                                Log.d("READING", "stream data");
                            }
                            InputStream inputStream = buffer.inputStream();
                            String contentType = response.header(HttpHeaders.CONTENT_TYPE);
                            String boundary = getHeaderParameter(contentType, HttpHeaders.Parameters.BOUNDARY);
                            if(boundary != null){
                                multipartParser.parseStream(inputStream, boundary);
                            }
                            else{
                                handleException(inputStream);
                            }
                        }
                        else{
                            connectionListener.onSynchronizeSuccess();
                        }
                    }
                    catch (Exception e){
                        if(shouldExceptionCauseShutdown(e)){
                            closeConnection();
                            connectionListener.onCloseConnection(e);
                        }
                    }
                    finally {
                        response.close();
                    }

                }

                @Override
                public void onRequestError(Throwable e) {
                    Log.d(TAG, "Response with Error", e);
                    if(shouldExceptionCauseShutdown(e)){
                        closeConnection();
                        connectionListener.onCloseConnection(e);
                    }
                }
            });

            enqueueRequest(avsRequest);
        } catch (JsonProcessingException e) {
            Log.d(TAG, "Response with Error", e);
        }
    }

    private RequestBody buildMultipartBody(final AVSRequest request, final StreamContentProvider streamContentProvider) throws JsonProcessingException {
        String sRequest = ObjectMapperFactory.getObjectMapper().writeValueAsString(request);
        RequestBody requestJson = RequestBody.create(MediaType.parse("application/json"), sRequest);
        MultipartBody.Builder builder =  new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, requestJson);
        if(streamContentProvider != null){
            RequestBody requestStream = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse("application/octet-stream");
                }

                @Override
                public void writeTo(final BufferedSink sink) throws IOException {
//                    OutputStream output = new FileOutputStream("/sdcard/recording.pcm");
//                    observable.subscribe(new Action1<byte[]>() {
//                                @Override
//                                public void call(byte[] bytes) {
//                                    try {
//                                        Log.d("Copy recording audio", String.valueOf(bytes.length));
//                                        sink.write(bytes);
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                    });

                    Log.d("Polling", "Audio Streaming ????????????");

                    InputStream inputStream = streamContentProvider.getContent();

                    FileOutputStream output = new FileOutputStream("/sdcard/recording.pcm");
                    byte[] buffer = new byte[8194];
                    while(inputStream.read(buffer, 0, buffer.length) != -1){
                        output.write(buffer, 0, buffer.length);
                    }
                    output.close();
                    PlaybackThread playbackThread = new PlaybackThread(getAudioSample(), new PlaybackThread.PlaybackListener() {
                        @Override
                        public void onProgress(int progress) {

                        }

                        @Override
                        public void onCompletion() {

                        }
                    });
                    playbackThread.startPlayback();

                    Source source = Okio.source(inputStream);
                    sink.writeAll(source);
//                    while(streamContentProvider.hasData()){
//                        byte[] buffer = streamContentProvider.getData();
//                        Log.d("Polling", "Audio Streaming");
//                        if(buffer != null){
//                            int count = buffer.length / 320;
//                            int remainder = buffer.length % 320;
//                            for(int i = 0; i < count; i++ ){
//                                Log.d("Copy recording audio", "320");
//                                sink.write(buffer, i * 320, 320);
//                            }
//                            if(remainder > 0){
//                                Log.d("Copy recording audio", String.valueOf(remainder));
//                                sink.write(buffer, count*320, remainder);
//                            }
//                        }
//                    }
                    Log.d("Polling", "Audio Streaming Finished!!!!!!!!!!!!!!!!!!!");
                }
            };
            builder.addFormDataPart("audio", null, requestStream);

        }
        return builder.build();
    }

    private short[] getAudioSample(){
        InputStream is = null;
        byte[] data = new byte[0];
        try {
            is = new FileInputStream(new File("/sdcard/recording.pcm"));
            data = IOUtils.toByteArray(is);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] samples = new short[sb.limit()];
        sb.get(samples);
        return samples;
    }

    private void handleRequestException(Throwable e) {
        Log.d(TAG, "Response with Error", e);
        if(shouldExceptionCauseShutdown(e)){
            closeConnection();
            connectionListener.onCloseConnection(e);
        }
        else if(e instanceof MultipartStream.MalformedStreamException){

        }
        else {
            closeConnection();
            createConnection();
        }
    }

    private void closeConnection() {
        if(downChannel != null){
            downChannel.close();
            downChannel = null;
        }
    }

    private void sendRequest(AVSRequest avsRequest) {
        Response response = null;

        try {
            response = sendRequestActual(avsRequest.getRequest());
            avsRequest.getRequestListener().onRequestSuccess(response);
        }
        catch (Exception e){
            Log.d(TAG, "Response with Error", e);
            if(response != null){
                response.close();
            }
            avsRequest.getRequestListener().onRequestError(e);
        }
    }

    private Response sendRequestActual(Request request) throws IOException {
        synchronized (this){
            Call call = httpClient.newCall(request);
            Response response = call.execute();
            Log.d(TAG, request.toString() + " ==> HTTP response code: " + response.code());
            return response;
        }
    }

    private OkHttpClient initializeHttpClient(PreferenceService preferenceService) {
        OkHttpClient httpClient;
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

//        HttpLoggingInterceptor networkInterceptor = new HttpLoggingInterceptor();
//        networkInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//        httpClientBuilder.addInterceptor(networkInterceptor);

//         Security Interceptor
        SecurityInterceptor securityInterceptor = new SecurityInterceptor(preferenceService);
        httpClientBuilder.addInterceptor(securityInterceptor);

        List<Protocol> protocols = new ArrayList<>();
        protocols.add(Protocol.HTTP_2);
        protocols.add(Protocol.HTTP_1_1);

        httpClient = httpClientBuilder
                .protocols(protocols)
                .readTimeout(0, TimeUnit.MINUTES)
                .writeTimeout(0, TimeUnit.MINUTES)
                .connectTimeout(0, TimeUnit.MINUTES)
                .pingInterval(5, TimeUnit.MINUTES)
                .build();
        return httpClient;
    }

    private String getHeaderParameter(String headerValue, String key) {
        if ((headerValue == null) || (key == null)) {
            return null;
        }

        String[] parts = headerValue.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith(key)) {
                return part.substring(key.length() + 1).replaceAll("(^\")|(\"$)", "").trim();
            }
        }

        return null;
    }

    private void parseException(InputStream inputStream, MultipartParser parser) throws IOException, AlexaSystemException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, data);
        Directive message = parser.parseServerMessage(data.toByteArray());
        if (message instanceof AlexaExceptionResponse){
            ((AlexaExceptionResponse)message).throwException();
        }
    }

    private void handleException(InputStream inputStream) throws IOException, AlexaSystemException {
        // This code assumes that System.Exception is only sent as a non-multipart response
        // This should throw an exception
        parseException(inputStream, multipartParser);

        // If the above doesn't throw the expected exception,
        // throw this exception instead
        throw new MultipartStream.MalformedStreamException(
                "A boundary is missing from the response headers. "
                        + "Unable to parse multipart stream.");
    }

    private boolean shouldExceptionCauseShutdown(Throwable e) {
        return (e instanceof AlexaSystemException) && (AlexaSystemExceptionCode.UNAUTHORIZED_REQUEST_EXCEPTION == ((AlexaSystemException) e).getExceptionCode());
    }
}
