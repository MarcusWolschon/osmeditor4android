package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;
import static de.blau.android.net.HttpHeaders.ACCEPT_HEADER;
import static de.blau.android.net.HttpHeaders.AUTHORIZATION_HEADER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.json.JSONObject;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import de.blau.android.contract.Github;
import de.blau.android.net.OAuth2Helper;
import de.blau.android.net.OAuth2Interceptor;
import de.blau.android.net.OAuthHelper.OAuthConfiguration;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Server;
import de.blau.android.osm.UserDetails;
import de.blau.android.prefs.API.Auth;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.AfterTextChangedWatcher;
import de.blau.android.util.AuthorisationEnabledActivity;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Open an issue on github or Forgejo/Gitea. An anonymous submission requires the current OSM display name.
 */
public class Feedback extends AuthorisationEnabledActivity implements ActivityResultHandler {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Feedback.class.getSimpleName().length());
    private static final String DEBUG_TAG = Feedback.class.getSimpleName().substring(0, TAG_LEN);

    public static final String REPO_KEY                = "github";
    static final String        VESPUCCI_REPORTER_ENTRY = "VESPUCCI_REPORTER";
    public static final String GITHUB_BEARER_TOKEN     = "GITHUB_BEARER_TOKEN";

    private static final String DIVIDER     = "\n\n---\n";
    private static final String ISSUE_BODY  = "body";
    private static final String ISSUE_TITLE = "title";

    private static final String REPO_HOST_KEY = "repo_host";
    private static final String REPO_USER_KEY = "repo_user";
    private static final String REPO_NAME_KEY = "repo_name";

    private static final String EMPTY_BUG_REPORT = "bug_report_empty.md";

    private MaterialCheckBox checkboxDeviceInfo;
    private TextView         deviceInfoText;
    private Button           buttonSend;
    private Button           buttonGithubLogin;
    private Button           buttonOsmLogin;
    private RelativeLayout   githubStatus;
    private Button           buttonReset;

    private String repoHost = Github.GITHUB_HOST;
    private String repoUser = Github.CODE_REPO_USER;
    private String repoName = Github.CODE_REPO_NAME;

    private String githubPersonalAccessToken;
    private String githubBearerToken;

    private Server server;
    private String displayName = null;

    private TextInputEditText                                     titleInput;
    private TextInputEditText                                     descriptionInput;
    private MaterialRadioButton                                   radioDisplayName;
    private com.google.android.material.textfield.TextInputLayout nameInputLayout;
    private TextInputEditText                                     nameInput;

    java.util.Map<Integer, ActivityResultHandler.Listener> activityResultListeners = new java.util.HashMap<>();

    /**
     * Start this Activity
     * 
     * @param context Android Context
     * @param useUrl if true don't use the builtin reporter, if the github app is installed this is ignored
     */
    public static void start(@NonNull Context context, boolean useUrl) {
        start(context, Github.GITHUB_HOST, Github.CODE_REPO_USER, Github.CODE_REPO_NAME, useUrl);
    }

    /**
     * Start this Activity or alternatively an external app via Url
     * 
     * @param context Android Context
     * @param repoUser github repository user
     * @param repoName github repository name
     * @param useUrl if true don't use the builtin reporter
     */
    public static void start(@androidx.annotation.NonNull Context context, @androidx.annotation.NonNull String repoUser,
            @androidx.annotation.NonNull String repoName, boolean useUrl) {
        start(context, Github.GITHUB_HOST, repoUser, repoName, useUrl);
    }

    /**
     * Start this Activity or alternatively an external app via Url
     * 
     * @param context Android Context
     * @param useUrl if true don't use the builtin reporter
     */
    public static void start(@NonNull Context context, @NonNull String host, @NonNull String repoUser, @NonNull String repoName, boolean useUrl) {
        if (useUrl) {
            // User explicitly wants browser
            reportViaUrl(context, Github.GITHUB_HOST, Github.CODE_REPO_USER, Github.CODE_REPO_NAME);
            return;
        }

        // Open native screen. Activity will handle missing apiKey by showing Login button.
        Intent intent = new Intent(context, Feedback.class);
        intent.putExtra(REPO_HOST_KEY, host);
        intent.putExtra(REPO_USER_KEY, repoUser);
        intent.putExtra(REPO_NAME_KEY, repoName);
        context.startActivity(intent);
    }

    /**
     * Simply use an URL instead of the builtin reporter
     * 
     * @param context an Android Context
     * @param host the repository host
     * @param repoUser the owner of the target repo
     * @param repoName the target repo
     */
    private static void reportViaUrl(@NonNull Context context, @NonNull String host, @NonNull String repoUser, @NonNull String repoName) {
        String description = "";
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + host + "/" + repoUser + "/" + repoName + "/issues/new?template="
                    + EMPTY_BUG_REPORT + "&body=" + URLEncoder.encode(description, OsmXml.UTF_8))));
        } catch (UnsupportedEncodingException e) {
            Log.e(DEBUG_TAG, "Unsupported encoding " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate " + savedInstanceState);
        Preferences prefs = App.getPreferences(this);
        server = prefs.getServer();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        String s = Util.getSerializableExtra(getIntent(), REPO_HOST_KEY, String.class);
        if (s != null) {
            repoHost = s;
        }
        s = Util.getSerializableExtra(getIntent(), REPO_USER_KEY, String.class);
        if (s != null) {
            repoUser = s;
        }
        s = Util.getSerializableExtra(getIntent(), REPO_NAME_KEY, String.class);
        if (s != null) {
            repoName = s;
        }

        updateTokens();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.feedback_title);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        titleInput = findViewById(R.id.feedbackTitle);
        descriptionInput = findViewById(R.id.feedbackDescription);
        descriptionInput.setMovementMethod(new ScrollingMovementMethod());
        descriptionInput.setScrollBarStyle(TAG_LEN);
        RadioGroup senderRadioGroup = findViewById(R.id.senderRadioGroup);
        radioDisplayName = findViewById(R.id.radioDisplayName);
        nameInputLayout = findViewById(R.id.feedbackNameLayout);
        nameInput = findViewById(R.id.feedbackName);
        checkboxDeviceInfo = findViewById(R.id.checkboxDeviceInfo);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        buttonSend = findViewById(R.id.buttonSend);
        buttonGithubLogin = findViewById(R.id.buttonGithubLogin);
        buttonOsmLogin = findViewById(R.id.buttonOsmLogin);
        githubStatus = findViewById(R.id.githubStatus);
        buttonReset = findViewById(R.id.buttonReset);

        updateStatus();

        TextWatcher watcher = new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateStatus();
            }
        };

        titleInput.addTextChangedListener(watcher);
        descriptionInput.addTextChangedListener(watcher);

        buttonGithubLogin.setOnClickListener(v -> triggerWebFlowLogin(this, REPO_KEY));

        buttonOsmLogin.setOnClickListener(v -> Server.checkOsmAuthentication(this, server, this::fetchOsmDisplayName));

        buttonSend.setOnClickListener(v -> submitBugReport());

        buttonReset.setOnClickListener(v -> {
            try (KeyDatabaseHelper keys = new KeyDatabaseHelper(this); SQLiteDatabase db = keys.getReadableDatabase()) {
                KeyDatabaseHelper.updateField(db, GITHUB_BEARER_TOKEN, EntryType.API_KEY, KeyDatabaseHelper.KEY_FIELD, null);
            }
            githubBearerToken = null;
            updateStatus();
        });

        // Fetch basic device info
        String deviceInfo = getString(R.string.feedback_device_info_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                android.os.Build.VERSION.RELEASE, android.os.Build.VERSION.SDK_INT, android.os.Build.MANUFACTURER, android.os.Build.MODEL);
        deviceInfoText.setText(deviceInfo);

        checkboxDeviceInfo.setOnCheckedChangeListener((buttonView, isChecked) -> deviceInfoText.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        senderRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioDisplayName) {
                if (Util.isEmpty(displayName)) {
                    nameInputLayout.setVisibility(View.GONE);
                    buttonOsmLogin.setVisibility(View.VISIBLE);
                } else {
                    nameInputLayout.setVisibility(View.VISIBLE);
                    buttonOsmLogin.setVisibility(View.GONE);
                }
            } else {
                nameInputLayout.setVisibility(View.GONE);
                buttonOsmLogin.setVisibility(View.GONE);
            }
        });

        fetchOsmDisplayName();
    }

    /**
     * Get current tokens from the key database
     */
    private void updateTokens() {
        try (KeyDatabaseHelper keys = new KeyDatabaseHelper(this); SQLiteDatabase db = keys.getReadableDatabase()) {
            githubPersonalAccessToken = KeyDatabaseHelper.getKey(db, VESPUCCI_REPORTER_ENTRY, EntryType.API_KEY);
            githubBearerToken = KeyDatabaseHelper.getKey(db, GITHUB_BEARER_TOKEN, EntryType.API_KEY);
        }
    }

    /**
     * Fetches the OpenStreetMap display name for the currently authenticated user.
     */
    private void fetchOsmDisplayName() {
        if (server == null || server.getDisplayName() == null) {
            if (radioDisplayName.isChecked()) {
                nameInputLayout.setVisibility(View.GONE);
                buttonOsmLogin.setVisibility(View.VISIBLE);
            } else {
                nameInputLayout.setVisibility(View.GONE);
                buttonOsmLogin.setVisibility(View.GONE);
            }
            return;
        }

        new ExecutorTask<Void, UserDetails, UserDetails>() {
            @Override
            protected UserDetails doInBackground(Void param) {
                try {
                    return server.getUserDetails();
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Problem accessing user details", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(UserDetails userDetails) {
                if (userDetails != null) {
                    displayName = userDetails.getDisplayName();
                    nameInput.setText(displayName);
                    if (radioDisplayName.isChecked()) {
                        nameInputLayout.setVisibility(View.VISIBLE);
                        buttonOsmLogin.setVisibility(View.GONE);
                    }
                    return;
                }
                // Auth failure or other error: hide name UI and show login button
                displayName = null;
                if (radioDisplayName.isChecked()) {
                    nameInputLayout.setVisibility(View.GONE);
                    buttonOsmLogin.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    /**
     * Updates the UI to reflect the current GitHub/Forgejo login status. Updates the status text and the visibility of
     * the login button.
     */
    private void updateStatus() {
        Log.d(DEBUG_TAG, "Updating status");
        if (Util.isEmpty(githubBearerToken)) {
            githubStatus.setVisibility(View.GONE);
            buttonReset.setVisibility(View.GONE);
            buttonGithubLogin.setText(getString(R.string.github_login_with_github));
            buttonGithubLogin.setVisibility(View.VISIBLE);

        } else {
            githubStatus.setVisibility(View.VISIBLE);
            buttonReset.setVisibility(View.VISIBLE);
            buttonGithubLogin.setVisibility(View.GONE);

        }
        buttonSend.setEnabled(!isEmpty(descriptionInput.getText()) && !isEmpty(titleInput.getText()));
    }

    /**
     * Check if an editable is empty
     * 
     * @return true if empty
     */
    private boolean isEmpty(@Nullable Editable editable) {
        return editable == null || "".equals(editable.toString());
    }

    private static void triggerWebFlowLogin(@NonNull Feedback activity, @NonNull final String host) {

        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(activity)) {
            OAuthConfiguration configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), REPO_KEY, Auth.OAUTH2);
            if (configuration == null) {
                ScreenMessage.toastTopError(activity, "No configuration found for " + REPO_KEY);
                return;
            }
            String clientId = configuration.getKey();

            if (Util.isEmpty(clientId)) {
                // No client_id configured - fall back to browser
                ScreenMessage.toastTopError(activity, "No client id found for " + REPO_KEY);
                return;
            }

            startWebFlow(activity, host);
        }
    }

    /**
     * Submits the bug report to the configured repository using the Issues API.
     */
    private void submitBugReport() {

        String title = titleInput.getText().toString();
        String description = descriptionInput.getText().toString();

        if (Util.isEmpty(title)) {
            titleInput.setError(getString(R.string.feedback_title_required));
            return;
        }

        if (checkboxDeviceInfo.isChecked()) {
            description += DIVIDER + deviceInfoText.getText().toString();
        }

        if (radioDisplayName.isChecked()) {
            description += getString(R.string.feedback_reporter_label, nameInput.getText().toString());
        }

        final String finalTitle = title;
        final String finalDescription = description;

        new ExecutorTask<Void, AsyncResult, AsyncResult>() {
            @Override
            protected AsyncResult doInBackground(Void param) {
                OkHttpClient client = App.getHttpClient();
                // GitHub/Forgejo Issue API: POST /repos/{owner}/{repo}/issues
                String url = Github.getApiBaseUrl(repoHost) + "repos/" + repoUser + "/" + repoName + "/issues";

                try {
                    JSONObject body = new JSONObject();
                    body.put(ISSUE_TITLE, finalTitle);
                    body.put(ISSUE_BODY, finalDescription);

                    okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse(Github.MIME_TYPE_JSON), body.toString());

                    Request.Builder builder = new Request.Builder().url(url).addHeader(ACCEPT_HEADER, Github.ACCEPT_HEADER_GITHUB_V3);
                    if (githubBearerToken != null) {
                        client = client.newBuilder().addInterceptor(new OAuth2Interceptor(githubBearerToken)).build();
                    } else {
                        builder.addHeader(AUTHORIZATION_HEADER, Github.AUTH_HEADER_PREFIX + githubPersonalAccessToken);
                    }

                    Request request = builder.post(requestBody).build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, getString(R.string.feedback_error_code, response.code(), response.message()));
                        }
                        return parseApiResponse(response);
                    }
                } catch (Exception e) {
                    return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, getString(R.string.feedback_exception, e.getMessage()));
                }
            }

            @Override
            protected void onPostExecute(AsyncResult result) {
                if (ErrorCodes.OK == result.getCode()) {
                    AlertDialog dialog = new AlertDialog.Builder(Feedback.this).setTitle(R.string.feedback_success_title)
                            .setMessage(Util.fromHtml(getString(R.string.feedback_success_message, getIssueLink(repoUser, repoName, result.getMessage()))))
                            .setPositiveButton(android.R.string.ok, (d, which) -> finish()).create();
                    dialog.setOnShowListener(
                            d -> ((TextView) ((AlertDialog) d).findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance()));
                    dialog.show();
                } else {
                    new AlertDialog.Builder(Feedback.this).setTitle(R.string.feedback_failure_title).setMessage(result.getMessage())
                            .setPositiveButton(android.R.string.ok, null).show();
                }
            }
        }.execute();
    }

    /**
     * Parse the response from the issue API
     * 
     * @param response the Response
     * @return an AsyncResult holding an error message or the issue URL
     */
    @NonNull
    private AsyncResult parseApiResponse(@NonNull Response response) {
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(response.body().byteStream(), Charset.forName(OsmXml.UTF_8)))) {
            JsonElement root = JsonParser.parseReader(rd);
            if (root.isJsonObject()) {
                JsonElement issue = ((JsonObject) root).get(Github.ISSUE_URL);
                if (issue instanceof JsonElement) {
                    return new AsyncResult(ErrorCodes.OK, issue.getAsString());
                }
            }
            return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, root.toString());
        } catch (IOException | JsonSyntaxException e) {
            Log.e(DEBUG_TAG, "Error reading response " + e.getMessage());
            return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * Get the UI issue URL from, the API issue URL
     * 
     * @param repoUser owner of the repo
     * @param repoName name of the repo
     * @param url the API issue url
     * @return the UI issue URL
     */
    @NonNull
    private static String getIssueLink(@NonNull String repoUser, @NonNull String repoName, @NonNull String url) {
        try {
            URL issue = new URI(url).toURL();
            String[] segments = issue.getPath().split("/");
            if (segments.length > 0) {
                return Github.getIssueUrl(repoUser, repoName, segments[segments.length - 1]);
            }
        } catch (MalformedURLException | URISyntaxException e) {
            // fall through
        }
        Log.e(DEBUG_TAG, "Unparseable issue response " + url);
        return "";
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
    public void setResultListener(int code, Listener listener) {
        activityResultListeners.put(code, listener);
    }

    /**
     * Initiates the GitHub/Forgejo/Gitea Web Application Flow OAuth 2 authentication.
     *
     * @param context Android context
     * @param host The repository host (e.g., github.com, codeberg.org)
     * @param callback Callback to handle the final authentication result
     */
    private static void startWebFlow(@NonNull Context context, @NonNull String host) {

        try {
            String authUrl = new OAuth2Helper(context, host, Github.AUTHORIZE_PATH, Github.ACCESS_TOKEN_PATH, Github.WEB_FLOW_REDIRECT_URI)
                    .getAuthorisationUrl(context, Util.wrapInList(Github.SCOPE_PUBLIC_REPO));
            Util.launchInCustomTabOrBrowser((Activity) context, Uri.parse(authUrl));
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Could not open browser: " + e.getMessage());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Authorize.ACTION_FINISH_OAUTH.equals(intent.getAction())) {
            updateTokens();
            updateStatus();
        }
    }
}
