package io.vespucci;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.heinrichreimersoftware.androidissuereporter.IssueReporterActivity;
import com.heinrichreimersoftware.androidissuereporter.model.DeviceInfo;
import com.heinrichreimersoftware.androidissuereporter.model.Report;
import com.heinrichreimersoftware.androidissuereporter.model.github.ExtraInfo;
import com.heinrichreimersoftware.androidissuereporter.model.github.GithubTarget;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import io.vespucci.R;
import io.vespucci.contract.Github;
import io.vespucci.contract.Urls;
import io.vespucci.osm.OsmXml;
import io.vespucci.osm.Server;
import io.vespucci.osm.UserDetails;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.KeyDatabaseHelper;
import io.vespucci.resources.KeyDatabaseHelper.EntryType;
import io.vespucci.util.ActivityResultHandler;
import io.vespucci.util.ExecutorTask;
import io.vespucci.util.Util;

/**
 * Open an issue on github, an anonymous submission requires the current OSM display name
 * 
 * Lots of hacks around limitation in the underlying library
 * 
 * @author Simon
 *
 */
public class Feedback extends IssueReporterActivity implements ActivityResultHandler {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Feedback.class.getSimpleName().length());
    private static final String DEBUG_TAG = Feedback.class.getSimpleName().substring(0, TAG_LEN);

    static final String         VESPUCCI_REPORTER_ENTRY = "VESPUCCI_REPORTER";
    private static final String REPO_USER_KEY           = "repo_user";
    private static final String REPO_NAME_KEY           = "repo_name";
    private static final String GITHUB_API_KEY          = "github_api_key";

    private static final String EMPTY_BUG_REPORT = "bug_report_empty.md";

    java.util.Map<Integer, ActivityResultHandler.Listener> activityResultListeners = new HashMap<>();

    String displayName = null;
    Server server      = null;

    String repoUser = Github.CODE_REPO_USER;
    String repoName = Github.CODE_REPO_NAME;

    /**
     * Start this Activity
     * 
     * @param context Android Context
     * @param useUrl if true don't use the builtin reporter, if the github app is installed this is ignored
     */
    public static void start(@NonNull Context context, boolean useUrl) {
        start(context, Github.CODE_REPO_USER, Github.CODE_REPO_NAME, useUrl);
    }

    /**
     * Start this Activity or alternatively an external app via Url
     * 
     * @param context Android Context
     * @param repoUser github repository user
     * @param repoName github repository name
     * @param useUrl if true don't use the builtin reporter, if the github app is installed this is ignored
     */
    public static void start(@NonNull Context context, @NonNull String repoUser, @NonNull String repoName, boolean useUrl) {
        try (KeyDatabaseHelper keys = new KeyDatabaseHelper(context); SQLiteDatabase db = keys.getReadableDatabase()) {
            String apiKey = KeyDatabaseHelper.getKey(db, VESPUCCI_REPORTER_ENTRY, EntryType.API_KEY);
            if (useUrl || Util.isPackageInstalled(Github.APP, context.getPackageManager()) || Util.isEmpty(apiKey)) {
                reportViaUrl(context, repoUser, repoName);
            } else {
                Intent intent = new Intent(context, Feedback.class);
                intent.putExtra(REPO_USER_KEY, repoUser);
                intent.putExtra(REPO_NAME_KEY, repoName);
                intent.putExtra(GITHUB_API_KEY, apiKey);
                context.startActivity(intent);
            }
        }
    }

    /**
     * Simply use an URL instead of the builtin reporter
     * 
     * This makes sense when the user has a github account, note that it assumes that there is a template called
     * "bug_report_empty.md"
     * 
     * @param context an Android Context
     * @param repoUser the owner of the target repo
     * @param repoName the target repo
     */
    private static void reportViaUrl(Context context, String repoUser, String repoName) {
        Report report = new Report("", "", new DeviceInfo(context), new ExtraInfo(), "");
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Urls.GITHUB + repoUser + "/" + repoName + "/issues/new?template=" + EMPTY_BUG_REPORT
                    + "&body=" + URLEncoder.encode(report.getDescription(), OsmXml.UTF_8))));
        } catch (UnsupportedEncodingException e) {
            Log.e(DEBUG_TAG, "Unsupported encoding " + e.getMessage());
        }
    }

    @Override
    public GithubTarget getTarget() {
        return new GithubTarget(repoUser, repoName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences prefs = App.getPreferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_IssueReporter_Light);
        }

        super.onCreate(savedInstanceState);

        String s = Util.getSerializableExtra(getIntent(), REPO_USER_KEY, String.class);
        if (s != null) {
            repoUser = s;
        }
        s = Util.getSerializableExtra(getIntent(), REPO_NAME_KEY, String.class);
        if (s != null) {
            repoName = s;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.feedback_title);
        }

        String k = Util.getSerializableExtra(getIntent(), GITHUB_API_KEY, String.class);
        if (Util.notEmpty(k)) {
            setGuestToken(k);
        }

        // two line for the description is not enough
        final TextInputEditText description = (TextInputEditText) findViewById(R.id.air_inputDescription);
        description.setLines(10);
        description.setMaxLines(10);
        description.setGravity(Gravity.TOP);

        // make device info selectable
        ((TextView) findViewById(R.id.air_textDeviceInfo)).setTextIsSelectable(true);

        // as as side effect this disables e-mail validation
        setGuestEmailRequired(false);

        ((TextInputLayout) findViewById(R.id.air_inputEmailParent)).setHint("");
        final TextInputEditText inputEmail = findViewById(R.id.air_inputEmail);
        inputEmail.setInputType(InputType.TYPE_NULL);
        inputEmail.setHint(R.string.feedback_displayname_hint);
        inputEmail.setBackground(null);

        final FloatingActionButton buttonSend = findViewById(R.id.air_buttonSend);

        server = prefs.getServer();

        // hack so that the login layout isn't displayed
        ((View) findViewById(R.id.air_layoutLogin).getParent().getParent()).setVisibility(View.GONE);
        // but set anonymous to true
        ((RadioButton) findViewById(R.id.air_optionAnonymous)).setChecked(true);

        final PostAsyncActionHandler action = new PostAsyncActionHandler() {
            @Override
            public void onSuccess() {
                server = prefs.getServer(); // should be authenticated now

                new ExecutorTask<Void, UserDetails, UserDetails>() {

                    @Override
                    protected UserDetails doInBackground(Void param) {
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
            public void onError(@Nullable AsyncResult result) {
                buttonSend.setEnabled(false);
            }
        };

        action.onSuccess(); // if we are already authenticated checkOsmAuthentication won't do anything
        buttonSend.setEnabled(Server.checkOsmAuthentication(Feedback.this, server, action));
        setMinimumDescriptionLength(20);
    }

    @Override
    public void onSaveExtraInfo(ExtraInfo extraInfo) {
        if (displayName != null) {
            extraInfo.put("OSM display name", "<A href=\"" + Urls.OSM + "/user/" + displayName + "\"/>" + displayName + "</A>");
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
