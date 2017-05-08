package org.freelectron.leobel.testlwa.models.message.request;

import okhttp3.Response;

/**
 * Created by leobel on 2/28/17.
 */

public interface RequestListener{
    void onRequestSuccess(Response response);
    void onRequestError(Throwable e);
}
