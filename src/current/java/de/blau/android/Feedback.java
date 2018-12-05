package de.blau.android;

import java.util.HashMap;

import com.heinrichreimersoftware.androidissuereporter.IssueReporterActivity;
import com.heinrichreimersoftware.androidissuereporter.model.github.ExtraInfo;
import com.heinrichreimersoftware.androidissuereporter.model.github.GithubTarget;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import de.blau.android.osm.Server;
import de.blau.android.osm.Server.UserDetails;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ActivityResultHandler;

/**
 * Open an issue on github, an anonymous submission requires the current OSM display name
 * 
 * Lots of hacks around limitation in the underlying library
 * 
 * @author simon
 *
 */
public class Feedback extends IssueReporterActivity implements ActivityResultHandler {

    private static final String DEBUG_TAG = "Feedback";

    java.util.Map<Integer, ActivityResultHandler.Listener> activityResultListeners = new HashMap<>();

    RadioButton anonymous;
    String      displayName = null;

    @Override
    public GithubTarget getTarget() {
        return new GithubTarget("MarcusWolschon", "osmeditor4android");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_IssueReporter_Light);
        }

        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.feedback_title);
        }

        String k = getString(R.string.reporter);
        if (k != null && !"".equals(k)) {
            setGuestToken(k);
        }

        // as as side effect this disables e-mail validation
        setGuestEmailRequired(false);

        ((TextInputLayout) findViewById(R.id.air_inputEmailParent)).setHint("");
        final TextInputEditText inputEmail = findViewById(R.id.air_inputEmail);
        inputEmail.setInputType(InputType.TYPE_NULL);
        inputEmail.setHint(R.string.feedback_displayname_hint);
        inputEmail.setBackground(null);

        final FloatingActionButton buttonSend = findViewById(R.id.air_buttonSend);

        final Server server = prefs.getServer();
        anonymous = findViewById(R.id.air_optionAnonymous);
        final PostAsyncActionHandler action = new PostAsyncActionHandler() {
            @Override
            public void onSuccess() {

                new AsyncTask<Void, UserDetails, UserDetails>() {

                    @Override
                    protected UserDetails doInBackground(Void... params) {
                        return server.getUserDetails();
                    }

                    @Override
                    protected void onPostExecute(UserDetails userDetails) {
                        if (userDetails != null) {
                            displayName = userDetails.getDisplayName();
                            inputEmail.setText(displayName);
                            buttonSend.setEnabled(true);
                        }
                    }
                }.execute();
            }

            @Override
            public void onError() {
                buttonSend.setEnabled(false);
            }
        };

        if (anonymous.isChecked()) { // button shoudn't be checked anyway
            buttonSend.setEnabled(Server.checkOsmAuthentication(Feedback.this, server, action));
        }

        anonymous.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked && Server.checkOsmAuthentication(Feedback.this, server, action)) {
                    action.onSuccess();
                } else if (!checked) {
                    buttonSend.setEnabled(true);
                }
            }

        });
        anonymous.setText(R.string.feedback_with_displayname);

        setMinimumDescriptionLength(20);
    }

    @Override
    public void onSaveExtraInfo(ExtraInfo extraInfo) {
        if (displayName != null && anonymous.isChecked()) {
            extraInfo.put("OSM display name", displayName);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(DEBUG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        ActivityResultHandler.Listener listener = activityResultListeners.get(requestCode);
        if (listener != null) {
            listener.processResult(resultCode, data);
        } else {
            Log.w(DEBUG_TAG, "Received activity result without listener, code " + requestCode);
        }
    }

    @Override
    public void setResultListener(int code, Listener listener) {
        activityResultListeners.put(code, listener);
    }
}
