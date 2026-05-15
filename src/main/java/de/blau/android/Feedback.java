package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import de.blau.android.contract.Github;
import de.blau.android.contract.Urls;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Server;
import de.blau.android.osm.UserDetails;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.GithubOAuth;
import de.blau.android.util.Util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;

/**
 * Open an issue on github or Forgejo/Gitea.
 * An anonymous submission requires the current OSM display name.
 */
public class Feedback extends AppCompatActivity implements ActivityResultHandler {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Feedback.class.getSimpleName().length());
    private static final String DEBUG_TAG = Feedback.class.getSimpleName().substring(0, TAG_LEN);

    static final String VESPUCCI_REPORTER_ENTRY = "VESPUCCI_REPORTER";
    private static final String REPO_HOST_KEY   = "repo_host";
    private static final String REPO_USER_KEY   = "repo_user";
    private static final String REPO_NAME_KEY   = "repo_name";
    private static final String GITHUB_API_KEY  = "github_api_key";

    private static final String EMPTY_BUG_REPORT = "bug_report_empty.md";

    private static final String PREF_CUSTOM_REPO_HOST = "custom_repo_host";
    private static final String PREF_CUSTOM_REPO_USER = "custom_repo_user";
    private static final String PREF_CUSTOM_REPO_NAME = "custom_repo_name";
    private static final String PREF_CUSTOM_REPO_CLIENT_ID = "custom_repo_client_id";

    private MaterialCheckBox checkboxDeviceInfo;
    private TextView deviceInfoText;
    private Button buttonSend;
    private Button buttonGithubLogin;
    private Button buttonOsmLogin;
    private TextView textGithubStatus;
    private Button buttonAdvancedSettings;
    private View layoutAdvancedSettings;
    private TextInputEditText repoHostInput;
    private TextInputEditText repoOwnerInput;
    private TextInputEditText repoNameInput;
    private TextInputEditText repoClientIdInput;

    private String repoHost = Github.GITHUB_HOST;
    private String repoUser = Github.CODE_REPO_USER;
    private String repoName = Github.CODE_REPO_NAME;
    private String mCustomClientId = null;
    private String githubApiKey;

    private Server server;
    private String displayName = null;

    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private RadioGroup senderRadioGroup;
    private MaterialRadioButton radioAnonymous;
    private MaterialRadioButton radioDisplayName;
    private com.google.android.material.textfield.TextInputLayout nameInputLayout;
    private TextInputEditText nameInput;

    java.util.Map<Integer, ActivityResultHandler.Listener> activityResultListeners = new java.util.HashMap<>();

    /**
     * Start this Activity
     * 
     * @param context Android Context
     * @param useUrl  if true don't use the builtin reporter, if the github app is
     *                installed this is ignored
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
     * @param useUrl   if true don't use the builtin reporter
     */
    public static void start(@androidx.annotation.NonNull Context context, @androidx.annotation.NonNull String repoUser,
            @androidx.annotation.NonNull String repoName,
            boolean useUrl) {
        start(context, Github.GITHUB_HOST, repoUser, repoName, useUrl);
    }

    /**
     * Start this Activity or alternatively an external app via Url
     * 
     * @param context  Android Context
     * @param host     repository host (e.g. github.com)
     * @param repoUser repository user
     * @param repoName repository name
     * @param useUrl   if true don't use the builtin reporter
     */
    public static void start(@NonNull Context context, @NonNull String host, @NonNull String repoUser,
            @NonNull String repoName, boolean useUrl) {
        try (KeyDatabaseHelper keys = new KeyDatabaseHelper(context); SQLiteDatabase db = keys.getReadableDatabase()) {
            String apiKey = KeyDatabaseHelper.getKey(db, VESPUCCI_REPORTER_ENTRY, EntryType.API_KEY);
            if (useUrl) {
                // User explicitly wants browser
                reportViaUrl(context, host, repoUser, repoName);
            } else {
                // Open native screen. Activity will handle missing apiKey by showing Login
                // button.
                Intent intent = new Intent(context, Feedback.class);
                intent.putExtra(REPO_HOST_KEY, host);
                intent.putExtra(REPO_USER_KEY, repoUser);
                intent.putExtra(REPO_NAME_KEY, repoName);
                if (!Util.isEmpty(apiKey)) {
                    intent.putExtra(GITHUB_API_KEY, apiKey);
                }
                context.startActivity(intent);
            }
        }
    }

    /**
     * Starts GitHub Device Flow OAuth2. On success, saves the token and updates the
     * UI.
     *
     * @param context  Android context
     * @param repoUser GitHub repo user
     * @param repoName GitHub repo name
     * @param activity Optional Feedback activity instance to update on success
     */
    private static AlertDialog authDialog;

    /**
     * Starts the OAuth 2 Device Flow for GitHub, Forgejo, or Gitea.
     * If no Client ID is provided, it falls back to the host's default Client ID.
     *
     * @param context  Android context
     * @param host     The repository host (e.g., github.com, codeberg.org)
     * @param repoUser The owner of the target repository
     * @param repoName The name of the target repository
     * @param clientId Optional custom OAuth Client ID
     * @param activity The Feedback activity instance to update on success
     */
    private static void triggerDeviceFlowLogin(Context context, String host, String repoUser,
            String repoName, String clientId, Feedback activity) {
        String effectiveClientId = clientId;
        if (Util.isEmpty(effectiveClientId)) {
            effectiveClientId = Github.getOAuthClientId(host);
        }

        if (Util.isEmpty(effectiveClientId)) {
            // No client_id configured - fall back to browser
            reportViaUrl(context, host, repoUser, repoName);
            return;
        }

        if (host.equalsIgnoreCase(Github.GITHUB_HOST) && effectiveClientId.startsWith("Ov23liXXX")) {
            // placeholder client_id - fall back to browser
            reportViaUrl(context, host, repoUser, repoName);
            return;
        }

        GithubOAuth.startDeviceFlow(context, host, effectiveClientId, new GithubOAuth.DeviceFlowCallback() {
            @Override
            public void onShowCode(@NonNull String userCode) {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (authDialog != null && authDialog.isShowing()) {
                            authDialog.dismiss();
                        }
                        authDialog = new AlertDialog.Builder(activity)
                                .setTitle(R.string.github_auth_title)
                                .setMessage(context.getString(R.string.github_auth_message, userCode))
                                .setPositiveButton(R.string.github_auth_button_copy_open, (dialog, which) -> {
                                    // Copy to clipboard
                                    ClipboardManager clipboard = (ClipboardManager) context
                                            .getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(
                                            context.getString(R.string.github_auth_clipboard_label), userCode);
                                    clipboard.setPrimaryClip(clip);

                                    // Open browser
                                    try {
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                                Uri.parse(Github.getUserAuthUrl(host)));
                                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(browserIntent);
                                    } catch (Exception e) {
                                        Log.e(DEBUG_TAG, "Could not open browser: " + e.getMessage());
                                    }
                                })
                                .setCancelable(false)
                                .show();
                    });
                }
            }

            @Override
            public void onSuccess(@NonNull String accessToken) {
                if (authDialog != null) {
                    authDialog.dismiss();
                    authDialog = null;
                }
                // Save token for future use
                try (KeyDatabaseHelper keys = new KeyDatabaseHelper(context);
                        SQLiteDatabase db = keys.getWritableDatabase()) {
                    KeyDatabaseHelper.replaceOrDeleteKey(db, VESPUCCI_REPORTER_ENTRY, EntryType.API_KEY,
                            accessToken, true, true, null, null);
                }

                if (activity != null) {
                    // Update current Activity UI
                    activity.runOnUiThread(() -> {
                        activity.githubApiKey = accessToken;
                        activity.updateGithubStatus();
                    });
                } else {
                    // Open native Feedback screen
                    Intent intent = new Intent(context, Feedback.class);
                    intent.putExtra(REPO_HOST_KEY, host);
                    intent.putExtra(REPO_USER_KEY, repoUser);
                    intent.putExtra(REPO_NAME_KEY, repoName);
                    intent.putExtra(GITHUB_API_KEY, accessToken);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }

            @Override
            public void onError(@NonNull String reason) {
                if (authDialog != null) {
                    authDialog.dismiss();
                    authDialog = null;
                }
                Log.e(DEBUG_TAG, "Device Flow failed: " + reason);
                reportViaUrl(context, host, repoUser, repoName);
            }
        });
    }

    /**
     * Simply use an URL instead of the builtin reporter
     * 
     * @param context  an Android Context
     * @param host     the repository host
     * @param repoUser the owner of the target repo
     * @param repoName the target repo
     */
    private static void reportViaUrl(Context context, String host, String repoUser, String repoName) {
        String description = "";
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://" + host + "/" + repoUser + "/" + repoName + "/issues/new?template="
                            + EMPTY_BUG_REPORT
                            + "&body=" + URLEncoder.encode(description, OsmXml.UTF_8))));
        } catch (UnsupportedEncodingException e) {
            Log.e(DEBUG_TAG, "Unsupported encoding " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences prefs = App.getPreferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customLight);
        }
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
        senderRadioGroup = findViewById(R.id.senderRadioGroup);
        radioAnonymous = findViewById(R.id.radioAnonymous);
        radioDisplayName = findViewById(R.id.radioDisplayName);
        nameInputLayout = findViewById(R.id.feedbackNameLayout);
        nameInput = findViewById(R.id.feedbackName);
        checkboxDeviceInfo = findViewById(R.id.checkboxDeviceInfo);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        buttonSend = findViewById(R.id.buttonSend);
        buttonGithubLogin = findViewById(R.id.buttonGithubLogin);
        buttonOsmLogin = findViewById(R.id.buttonOsmLogin);
        textGithubStatus = findViewById(R.id.textGithubStatus);
        buttonAdvancedSettings = findViewById(R.id.buttonAdvancedSettings);
        layoutAdvancedSettings = findViewById(R.id.layoutAdvancedSettings);
        repoHostInput = findViewById(R.id.repoHostInput);
        repoOwnerInput = findViewById(R.id.repoOwnerInput);
        repoNameInput = findViewById(R.id.repoNameInput);
        repoClientIdInput = findViewById(R.id.repoClientIdInput);

        // Load custom repo settings if saved
        android.content.SharedPreferences commonPrefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);
        repoHost = commonPrefs.getString(PREF_CUSTOM_REPO_HOST, Github.GITHUB_HOST);
        repoUser = commonPrefs.getString(PREF_CUSTOM_REPO_USER, Github.CODE_REPO_USER);
        repoName = commonPrefs.getString(PREF_CUSTOM_REPO_NAME, Github.CODE_REPO_NAME);
        mCustomClientId = commonPrefs.getString(PREF_CUSTOM_REPO_CLIENT_ID, null);

        repoHostInput.setText(repoHost);
        repoOwnerInput.setText(repoUser);
        repoNameInput.setText(repoName);
        if (mCustomClientId != null) {
            repoClientIdInput.setText(mCustomClientId);
        }

        buttonAdvancedSettings.setOnClickListener(v -> {
            if (layoutAdvancedSettings.getVisibility() == View.VISIBLE) {
                layoutAdvancedSettings.setVisibility(View.GONE);
            } else {
                layoutAdvancedSettings.setVisibility(View.VISIBLE);
            }
        });

        android.text.TextWatcher repoWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                repoHost = repoHostInput.getText().toString().trim();
                repoUser = repoOwnerInput.getText().toString().trim();
                repoName = repoNameInput.getText().toString().trim();
                mCustomClientId = repoClientIdInput.getText().toString().trim();
                if (mCustomClientId.isEmpty()) {
                    mCustomClientId = null;
                }

                commonPrefs.edit()
                        .putString(PREF_CUSTOM_REPO_HOST, repoHost)
                        .putString(PREF_CUSTOM_REPO_USER, repoUser)
                        .putString(PREF_CUSTOM_REPO_NAME, repoName)
                        .putString(PREF_CUSTOM_REPO_CLIENT_ID, mCustomClientId)
                        .apply();

                updateGithubStatus();
            }
        };

        repoHostInput.addTextChangedListener(repoWatcher);
        repoOwnerInput.addTextChangedListener(repoWatcher);
        repoNameInput.addTextChangedListener(repoWatcher);
        repoClientIdInput.addTextChangedListener(repoWatcher);

        githubApiKey = getIntent().getStringExtra(GITHUB_API_KEY);
        updateGithubStatus();

        final String h = repoHost;
        final String u = repoUser;
        final String n = repoName;
        final String cid = mCustomClientId;
        final Feedback act = Feedback.this;
        buttonGithubLogin
                .setOnClickListener(
                        v -> triggerDeviceFlowLogin(act, h, u, n, cid, act));

        buttonOsmLogin.setOnClickListener(v -> {
            Server.checkOsmAuthentication(this, server, new PostAsyncActionHandler() {
                @Override
                public void onSuccess() {
                    fetchOsmDisplayName();
                }
            });
        });

        buttonSend.setOnClickListener(v -> submitBugReport());

        // Fetch basic device info
        String deviceInfo = getString(R.string.feedback_device_info_format,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                android.os.Build.VERSION.RELEASE, android.os.Build.VERSION.SDK_INT,
                android.os.Build.MANUFACTURER, android.os.Build.MODEL);
        deviceInfoText.setText(deviceInfo);

        checkboxDeviceInfo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            deviceInfoText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

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
     * Fetches the OpenStreetMap display name for the currently authenticated user.
     */
    private void fetchOsmDisplayName() {
        if (server != null && server.getDisplayName() != null) {
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
                    } else {
                        // Auth failure or other error: hide name UI and show login button
                        displayName = null;
                        if (radioDisplayName.isChecked()) {
                            nameInputLayout.setVisibility(View.GONE);
                            buttonOsmLogin.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }.execute();
        } else {
            if (radioDisplayName.isChecked()) {
                nameInputLayout.setVisibility(View.GONE);
                buttonOsmLogin.setVisibility(View.VISIBLE);
            } else {
                nameInputLayout.setVisibility(View.GONE);
                buttonOsmLogin.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Updates the UI to reflect the current GitHub/Forgejo login status.
     * Updates the status text and the visibility of the login button.
     */
    private void updateGithubStatus() {
        if (Util.isEmpty(githubApiKey)) {
            textGithubStatus.setText(getString(R.string.github_status_not_logged_in));
            String buttonText = repoHost.equalsIgnoreCase(Github.GITHUB_HOST)
                    ? getString(R.string.github_login_with_github)
                    : getString(R.string.github_login_with_host, repoHost);
            buttonGithubLogin.setText(buttonText);
            buttonGithubLogin.setVisibility(View.VISIBLE);
            buttonSend.setEnabled(true);
            buttonSend.setAlpha(0.6f);
        } else {
            String status = repoHost.equalsIgnoreCase(Github.GITHUB_HOST)
                    ? getString(R.string.github_status_authenticated)
                    : getString(R.string.github_status_authenticated_with_host, repoHost);
            textGithubStatus.setText(status);
            buttonGithubLogin.setVisibility(View.GONE);
            buttonSend.setEnabled(true);
            buttonSend.setAlpha(1.0f);
        }
    }

    /**
     * Submits the bug report to the configured repository using the Issues API.
     */
    private void submitBugReport() {
        if (Util.isEmpty(githubApiKey)) {
            android.widget.Toast
                    .makeText(this, R.string.github_login_required_toast, android.widget.Toast.LENGTH_LONG)
                    .show();
            return;
        }

        String title = titleInput.getText().toString();
        String description = descriptionInput.getText().toString();

        if (Util.isEmpty(title)) {
            titleInput.setError(getString(R.string.feedback_title_required));
            return;
        }

        if (checkboxDeviceInfo.isChecked()) {
            description += "\n\n---\n" + deviceInfoText.getText().toString();
        }

        if (radioDisplayName.isChecked()) {
            description += getString(R.string.feedback_reporter_label, nameInput.getText().toString());
        }

        final String finalTitle = title;
        final String finalDescription = description;

        new ExecutorTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void param) {
                OkHttpClient client = App.getHttpClient();
                // GitHub/Forgejo Issue API: POST /repos/{owner}/{repo}/issues
                String url = Github.getApiBaseUrl(repoHost) + "repos/" + repoUser + "/" + repoName + "/issues";

                try {
                    JSONObject body = new JSONObject();
                    body.put("title", finalTitle);
                    body.put("body", finalDescription);

                    okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                            okhttp3.MediaType.parse(Github.MIME_TYPE_JSON),
                            body.toString());

                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", Github.AUTH_HEADER_PREFIX + githubApiKey)
                            .addHeader("Accept", Github.ACCEPT_HEADER_GITHUB_V3)
                            .post(requestBody)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            return "success";
                        } else {
                            return getString(R.string.feedback_error_code, response.code(), response.message());
                        }
                    }
                } catch (Exception e) {
                    return getString(R.string.feedback_exception, e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if ("success".equals(result)) {
                    new AlertDialog.Builder(Feedback.this)
                            .setTitle(R.string.feedback_success_title)
                            .setMessage(R.string.feedback_success_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .show();
                } else {
                    new AlertDialog.Builder(Feedback.this)
                            .setTitle(R.string.feedback_failure_title)
                            .setMessage(result)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
        }.execute();
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
