package org.freelectron.leobel.testlwa.services;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.freelectron.leobel.testlwa.models.AVSException;
import org.freelectron.leobel.testlwa.models.message.request.AVSRequest;
import org.freelectron.leobel.testlwa.models.Response;

import java.io.IOException;
import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by leobel on 2/20/17.
 */

public class AlexaServiceImpl implements AlexaService {

    private final AlexaServiceTemplate template;
    private final ObjectMapper objectMapper;

    public AlexaServiceImpl(Retrofit retrofit, ObjectMapper objectMapper){
        template = retrofit.create(AlexaServiceTemplate.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public Observable<Response<okhttp3.Response>> establishDownChanelStream() {
//        return template.establishDowChannel()
//                .map(new Func1<retrofit2.Response<Message>, Response<okhttp3.Response>>() {
//                    @Override
//                    public Response<okhttp3.Response> call(retrofit2.Response<Message> response) {
//                        if(response.isSuccessful())
//                            return new Response<>(response.raw());
//                        else{
//                            try {
//                                String content = response.errorBody().string();
//                                Message errorInfo = objectMapper.readValue(content, Message.class);
//                                Exception exception = new AVSException(errorInfo.getPayload().getCode(), errorInfo.getPayload().getDescription());
//                                return new Response<>(exception);
//                            } catch (IOException e) {
//                                return new Response<>(e);
//                            }
//
//                        }
//                    }
//                })
//                .onErrorReturn(new Func1<Throwable, Response<okhttp3.Response>>() {
//                    @Override
//                    public Response<okhttp3.Response> call(Throwable throwable) {
//                        return new Response<>(throwable);
//                    }
//                });
        return null;
    }


    @Override
    public Observable<Response<Void>> expectSpeechTimedOutEvent() {
        return null;
    }


    interface AlexaServiceTemplate{

        @Multipart
        @POST("events")
        Observable<retrofit2.Response<String>> recognizeEvent(@Part("metadata") AVSRequest request, @Part("audio") RequestBody audio);


    }
}
