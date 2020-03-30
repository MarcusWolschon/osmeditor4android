package de.blau.android.prefs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import de.blau.android.Authorize;
import de.blau.android.R;
import de.blau.android.net.OAuthHelper;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.Snack;
import oauth.signpost.exception.OAuthException;

/**
 * Will process vespucci:// URLs. Accepts the following URL parameters:<br>
 * 
 * preseturl - preset URL to add to the preset list<br>
 * presetname - name for the preset (if it gets added)<br>
 * 
 * oauth_token = oauth token, used during retrieving oauth access tokens<br>
 * oauth_verifier - oauth verifier, used during retrieving oauth access tokens<br>
 * 
 * @author Jan
 *
 */
public class VespucciURLActivity extends AppCompatActivity implements OnClickListener {
    private static final String DEBUG_TAG          = "VespucciURLActivity";
    private static final int    REQUEST_PRESETEDIT = 0;

    private String               command;
    private String               preseturl;
    private String               presetname;
    private PresetInfo           existingPreset    = null;
    private String               oauth_token;
    private String               oauth_verifier;
    private AdvancedPrefDatabase prefdb;
    private boolean              downloadSucessful = false;

    private View mainView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customHelpViewer_Light);
        } else {
            setTheme(R.style.Theme_customHelpViewer);
        }
        super.onCreate(savedInstanceState);
        mainView = View.inflate(this, R.layout.url_activity, null);
        setContentView(mainView);
        prefdb = new AdvancedPrefDatabase(this);
    }

    @Override
    protected void onStart() {
        Uri data = getIntent().getData();
        if (data != null) {
            try {
                command = data.getPath();
                Log.d(DEBUG_TAG, "Command " + command);
                preseturl = data.getQueryParameter("preseturl");
                presetname = data.getQueryParameter("presetname");
                oauth_token = data.getQueryParameter("oauth_token");
                oauth_verifier = data.getQueryParameter("oauth_verifier");
            } catch (Exception ex) {
                Log.e(DEBUG_TAG, "Uri " + data + " caused " + ex);
                ACRAHelper.nocrashReport(ex, ex.getMessage());
                finish();
            }
        } else {
            Log.e(DEBUG_TAG, "Received null Uri, ignoring");
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(DEBUG_TAG, "onResume");
        // determining what activity to do based purely on the parameters is rather hackish
        if ((oauth_token != null) && (oauth_verifier != null)) {
            mainView.setVisibility(View.GONE);
            Log.i(DEBUG_TAG, "got oauth verifier " + oauth_token + " " + oauth_verifier);
            String errorMessage = null;
            try {
                oAuthHandshake(oauth_verifier);
            } catch (OAuthException e) {
                errorMessage = OAuthHelper.getErrorMessage(this, e);
            } catch (ExecutionException e) {
                errorMessage = getString(R.string.toast_oauth_communication);
            } catch (TimeoutException e) {
                errorMessage = getString(R.string.toast_oauth_timeout);
            }
            if (errorMessage != null) {
                Snack.toastTopError(this, errorMessage);
            }
            setResult(RESULT_OK);
            finish();
        } else {
            mainView.findViewById(R.id.urldialog_nodata).setVisibility(preseturl == null ? View.VISIBLE : View.GONE);

            if (preseturl != null) {
                ActionBar actionbar = getSupportActionBar();
                if (actionbar != null) {
                    actionbar.setDisplayShowHomeEnabled(true);
                    actionbar.setDisplayHomeAsUpEnabled(true);
                    actionbar.setTitle(R.string.preset_download_title);
                    actionbar.setDisplayShowTitleEnabled(true);
                    actionbar.show();
                }
                mainView.findViewById(R.id.urldialog_layoutPreset).setVisibility(View.VISIBLE);

                ((TextView) mainView.findViewById(R.id.urldialog_textPresetName)).setText(presetname);
                ((TextView) mainView.findViewById(R.id.urldialog_textPresetURL)).setText(preseturl);
                existingPreset = prefdb.getPresetByURL(preseturl);
                if (downloadSucessful) {
                    mainView.findViewById(R.id.urldialog_textPresetSuccessful).setVisibility(View.VISIBLE);
                    mainView.findViewById(R.id.urldialog_textPresetExists).setVisibility(View.GONE);
                } else {
                    mainView.findViewById(R.id.urldialog_textPresetExists).setVisibility(existingPreset != null ? View.VISIBLE : View.GONE);
                    mainView.findViewById(R.id.urldialog_textPresetSuccessful).setVisibility(View.GONE);
                }
                mainView.findViewById(R.id.urldialog_checkboxEnable).setVisibility(existingPreset == null ? View.VISIBLE : View.GONE);
                mainView.findViewById(R.id.urldialog_buttonAddPreset).setVisibility(existingPreset == null ? View.VISIBLE : View.GONE);
                ((Button) mainView.findViewById(R.id.urldialog_buttonAddPreset)).setOnClickListener(this);
            }
        }
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.urldialog_buttonAddPreset:
            CheckBox enableCheckBox = (CheckBox) mainView.findViewById(R.id.urldialog_checkboxEnable);
            boolean enable = enableCheckBox != null && enableCheckBox.isChecked();
            PresetEditorActivity.startForResult(this, presetname, preseturl, enable, REQUEST_PRESETEDIT);
            break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(DEBUG_TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PRESETEDIT && resultCode == RESULT_OK) {
            downloadSucessful = true;
        }
    }

    /**
     * Process the OAuth callback
     * 
     * @param verifier the verifier
     * @throws OAuthException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    private void oAuthHandshake(@NonNull String verifier) throws OAuthException, TimeoutException, ExecutionException {
        String[] s = { verifier };
        class OAuthAccessTokenTask extends AsyncTask<String, Void, Boolean> {
            private OAuthException ex = null;

            @Override
            protected Boolean doInBackground(String... s) {
                OAuthHelper oa = new OAuthHelper(); // if we got here it has already been initialized once
                try {
                    String[] access = oa.getAccessToken(s[0]);
                    prefdb.setAPIAccessToken(access[0], access[1]);
                } catch (OAuthException e) {
                    Log.d(DEBUG_TAG, "oAuthHandshake: " + e);
                    ex = e;
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                Log.d(DEBUG_TAG, "oAuthHandshake onPostExecute");
                Intent intent = new Intent(VespucciURLActivity.this, Authorize.class);
                intent.setAction(Authorize.ACTION_FINISH_OAUTH);
                startActivity(intent);
            }

            /**
             * Get the any OAuthException that was thrown
             * 
             * @return the exception
             */
            OAuthException getException() {
                return ex;
            }
        }

        OAuthAccessTokenTask requester = new OAuthAccessTokenTask();
        requester.execute(s);
        try {
            if (Boolean.FALSE.equals(requester.get(60, TimeUnit.SECONDS))) {
                OAuthException ex = requester.getException();
                if (ex != null) {
                    throw ex;
                }
            }
        } catch (InterruptedException e) { // NOSONAR cancel does interrupt the thread in question
            requester.cancel(true);
            throw new TimeoutException(e.getMessage());
        }
    }
}
