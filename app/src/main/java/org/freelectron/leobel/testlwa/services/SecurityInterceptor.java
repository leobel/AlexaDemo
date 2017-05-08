package org.freelectron.leobel.testlwa.services;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by leobel on 2/20/17.
 */
public class SecurityInterceptor implements Interceptor {
    private final PreferenceService mPreferenceService;

    public SecurityInterceptor(PreferenceService preferenceService) {
        this.mPreferenceService = preferenceService;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        // Need to authenticate
        // Build new request
        Request.Builder builder = original.newBuilder();
        builder.header("AUTHORIZATION", "Bearer " + mPreferenceService.getAccessToken());
        if(original.method().toUpperCase().equals("POST")){
            builder.header("Content-Type", "multipart/form-data");
        }
        Request request = builder.build();

        return chain.proceed(request);
    }
}
