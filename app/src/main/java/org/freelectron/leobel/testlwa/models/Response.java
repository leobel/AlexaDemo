package org.freelectron.leobel.testlwa.models;

/**
 * Created by leobel on 2/20/17.
 */

public class Response<T> {

    private T value;

    private boolean successful;

    private Throwable error;

    private int source;

    public static final int CACHE = 1;
    public static final int NETWORK = 2;

    public Response(T value) {
        this(value,CACHE);
    }

    public Response(T value, int source) {
        this.value = value;
        this.successful = true;
        this.source = source;
    }

    public Response(Throwable error) {
        this.error = error;
        this.successful = false;
        this.source = CACHE;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public boolean isRemote(){
        return source == NETWORK;
    }
}
