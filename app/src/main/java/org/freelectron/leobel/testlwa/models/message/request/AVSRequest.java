package org.freelectron.leobel.testlwa.models.message.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.freelectron.leobel.testlwa.models.StreamContentProvider;
import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import okhttp3.Request;
import rx.Observable;

/**
 * Created by leobel on 2/20/17.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AVSRequest implements Serializable{

    @JsonProperty("context")
    private List<ComponentState> context;

    @JsonProperty("event")
    private Event event;

    @JsonIgnore
    private Request request;

    @JsonIgnore
    private RequestListener requestListener;

    @JsonIgnore
    private Observable<byte[]> streamProvider;

    @JsonIgnore
    private StreamContentProvider streamContentProvider;

    public AVSRequest(){

    }

    public AVSRequest(List<ComponentState> context, Event event){
        this.context = context;
        this.event = event;
    }

    public AVSRequest(Event event) {
        this.event = event;
    }

    public List<ComponentState> getContext() {
        return context;
    }

    public void setContext(List<ComponentState> context) {
        this.context = context;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public RequestListener getRequestListener() {
        return requestListener;
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public Observable<byte[]> getStreamProvider() {
        return streamProvider;
    }

    public void setStreamProvider(Observable<byte[]> streamProvider) {
        this.streamProvider = streamProvider;
    }

    public void setStreamProvider(StreamContentProvider streamProvider){
       this.streamContentProvider = streamProvider;
    }

    public StreamContentProvider getStreamContentProvider() {
        return streamContentProvider;
    }

}
