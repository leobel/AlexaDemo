package org.freelectron.leobel.testlwa;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.Listener;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;

import org.freelectron.leobel.testlwa.services.PreferenceService;

import javax.inject.Inject;

public class ThingToTryActivity extends AppCompatActivity {

    public static final String JUST_AUTHENTICATE = "JUST_AUTHENTICATE";
    private boolean justAuthenticate;


    @Inject
    public PreferenceService preferenceService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thing_to_try);

        TestLWAApp.getInstance().getComponent().inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.alexa_thing_to_try);
        setSupportActionBar(toolbar);

        justAuthenticate = getIntent().getBooleanExtra(JUST_AUTHENTICATE, false);

        TextView alexaLinkApp = (TextView) findViewById(R.id.alexa_app_link);
        alexaLinkApp.setMovementMethod(LinkMovementMethod.getInstance());

        stripUnderlines(alexaLinkApp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.alexa, menu);

        MenuItem menuItem = menu.findItem(R.id.alexa_action);
        if(justAuthenticate){
            menuItem.setTitle(R.string.things_to_try_done);
        }
        else{
            menuItem.setTitle(R.string.things_to_try_sign_out);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.alexa_action){
            if(justAuthenticate){ // Click on Done option
                Intent intent = new Intent(this, AlexaSpeechRecognizerActivity.class);
                startActivity(intent);
            }
            else { // Click on Sign Out option
                AuthorizationManager.signOut(this, new Listener<Void, AuthError>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        preferenceService.setAccessToken("");
                        Intent intent = new Intent(getApplication(), SplashActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(AuthError authError) {
                        new AlertDialog.Builder(getApplication())
                                .setMessage(R.string.signout_error)
                                .create()
                                .show();
                    }
                });

            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void stripUnderlines(TextView textView) {
        Spannable s = new SpannableString(textView.getText());
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span: spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            URLSpan spanNoUnderline = new URLSpan(span.getURL()){
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            s.setSpan(spanNoUnderline, start, end, 0);
        }
        textView.setText(s);
    }
}
