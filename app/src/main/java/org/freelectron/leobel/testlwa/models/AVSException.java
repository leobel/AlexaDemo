package org.freelectron.leobel.testlwa.models;

/**
 * Created by leobel on 2/22/17.
 */

public class AVSException extends Exception {

    public static final String UNAUTHORIZED_REQUEST_EXCEPTION = "UNAUTHORIZED_REQUEST_EXCEPTION";

    private final String code;

    public AVSException(String code, String description){
        super(description);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
