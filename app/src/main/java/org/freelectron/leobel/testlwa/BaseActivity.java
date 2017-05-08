package org.freelectron.leobel.testlwa;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.Listener;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.workflow.RequestContext;

import org.freelectron.leobel.testlwa.services.PreferenceService;

import javax.inject.Inject;

/**
 * Created by leobel on 3/8/17.
 */

public class BaseActivity extends AppCompatActivity implements LoginResultListener {

    public static final String ALEXA_ALL_SCOPE = "alexa:all";
    public static final String PRODUCT_DSN = "123456";
    public static final String PRODUCT_ID = "testalexa";


    protected RequestContext mRequestContext;
    protected AuthorizeListenerImpl authorizeListener;

    @Inject
    public PreferenceService preferenceService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TestLWAApp.getInstance().getComponent().inject(this);

        mRequestContext = RequestContext.create(this);
        authorizeListener = new AuthorizeListenerImpl(this, this);
        mRequestContext.registerListener(authorizeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRequestContext.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        authorizeListener.unSubscribe();
    }

    @Override
    public void onLoginSuccess(String accessToken) {
        Log.d("ACCESS TOKEN:", accessToken);
        preferenceService.setAccessToken(accessToken);
    }

    @Override
    public void onLoginError(Throwable throwable) {

    }

    public class AuthorizeListenerImpl extends AuthorizeListener {


        public String ACCESS_TOKEN = "ACCESS_TOKEN";
        public String AUTH_ERROR = "AUTH_ERROR";

        private final LoginResultListener listener;
        private Handler subscription;

        public AuthorizeListenerImpl(Context context, final LoginResultListener listener){
            this.listener = listener;
            subscription = new Handler(context.getMainLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    Bundle data = message.getData();
                    Exception error = data.getParcelable(AUTH_ERROR);
                    if(error != null){
                        listener.onLoginError(error);
                    }
                    else{
                        String accessToken = data.getString(ACCESS_TOKEN);
                        if(accessToken != null && !accessToken.isEmpty()){
                            listener.onLoginSuccess(data.getString(ACCESS_TOKEN));
                        }
                        else{
                            listener.onLoginError(new Exception("Unexpected error. Can't login to Amazon"));
                        }
                    }
                    return false;
                }
            });

        }


        public void checkAccessToken(){
            AuthorizationManager.getToken(BaseActivity.this, new Scope[] { ScopeFactory.scopeNamed(ALEXA_ALL_SCOPE) }, new TokenListener(this));
        }


        /* Authorization was completed successfully. */
        @Override
        public void onSuccess(final AuthorizeResult authorizeResult) {
            AuthorizationManager.getToken(BaseActivity.this, new Scope[] { ScopeFactory.scopeNamed(ALEXA_ALL_SCOPE) }, new TokenListener(this));
        }


        /* There was an error during the attempt to authorize the application. */
        @Override
        @MainThread
        public void onError(final AuthError authError) {
            sendMessage(authError);
        }

        /* Authorization was cancelled before it could be completed. */
        @Override
        @MainThread
        public void onCancel(final AuthCancellation authCancellation) {
            Throwable cause = new Exception(authCancellation.getDescription());
            sendMessage(new AuthError(authCancellation.getDescription(), cause, AuthError.ERROR_TYPE.ERROR_UNKNOWN));

        }

        void unSubscribe(){
            subscription.removeCallbacksAndMessages(null);
        }

        void sendMessage(Parcelable parcelable){
            Bundle data = new Bundle();
            data.putParcelable(AUTH_ERROR, parcelable);
            Message message = new Message();
            message.setData(data);
            subscription.sendMessage(message);
        }

        void sendMessage(String accessToken){
            Bundle data = new Bundle();
            data.putString(ACCESS_TOKEN, accessToken);
            Message message = new Message();
            message.setData(data);
            subscription.sendMessage(message);
        }
    }

    public class TokenListener implements Listener<AuthorizeResult, AuthError> {

        private final AuthorizeListenerImpl subscriber;

        public TokenListener(AuthorizeListenerImpl subscriber){
            this.subscriber = subscriber;
        }


        /* getToken completed successfully. */
        @Override
        @MainThread
        public void onSuccess(AuthorizeResult authorizeResult) {
            String accessToken = authorizeResult.getAccessToken();
            subscriber.sendMessage(accessToken);
        }

        /* There was an error during the attempt to get the token. */
        @Override
        @MainThread
        public void onError(AuthError authError) {
            subscriber.sendMessage(authError);
        }
    }
}
