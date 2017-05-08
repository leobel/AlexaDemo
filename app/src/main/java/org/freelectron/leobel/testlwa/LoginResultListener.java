package org.freelectron.leobel.testlwa;

/**
 * Created by leobel on 2/17/17.
 */

public interface LoginResultListener {
    void onLoginSuccess(String accessToken);
    void onLoginError(Throwable throwable);
}
