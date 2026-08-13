package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.contract.OpenStreetMap;
import de.blau.android.exception.NoOAuthConfigurationException;
import de.blau.android.exception.OsmException;
import de.blau.android.net.OAuth1aHelper;
import de.blau.android.net.OAuth2Helper;
import de.blau.android.net.OAuthHelper;
import de.blau.android.net.OAuthHelper.OAuthConfiguration;
import de.blau.android.osm.Server;
import de.blau.android.prefs.API.Auth;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.ConfigurationChangeAwareActivity;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import oauth.signpost.exception.OAuthException;

/**
 * Perform OAuth 1/2 authorisation of this app
 * 
 * @author simon
 *
 */
public class Authorize extends ConfigurationChangeAwareActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Authorize.class.getSimpleName().length());
    private static final String DEBUG_TAG = Authorize.class.getSimpleName().substring(0, TAG_LEN);

    public static final String ACTION_FINISH_OAUTH = "de.blau.android.FINISH_OAUTH";

    private Handler             timeoutHandler         = new Handler(Looper.getMainLooper());
    private static final long   OAUTH_TIMEOUT          = 100;
    private static final String CUSTOM_TAB_STARTED_KEY = "customTabStarted";

    private boolean customTabStarted = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate " + (savedInstanceState != null ? " saved state present" : " no saved state"));
        if (App.getPreferences(this).lightThemeEnabled()) {
            setTheme(R.style.Theme_customMain_Light);
        }
        super.onCreate(savedInstanceState);

        customTabStarted = ACTION_FINISH_OAUTH.equals(getIntent().getAction())
                || (savedInstanceState != null && savedInstanceState.getBoolean(CUSTOM_TAB_STARTED_KEY, false));
    }

    /**
     * Show the user a list of possible configs and if they choose one, retry
     * 
     * @param auth type of Authorisation
     */
    private void selectConfigAndRetry(@NonNull String apiName, @NonNull Auth auth) {
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(this)) {
            // get list of possible configs
            List<String> configNames = new ArrayList<>();
            for (OAuthConfiguration configuration : KeyDatabaseHelper.getOAuthConfigurations(keyDatabase.getReadableDatabase(), auth)) {
                configNames.add(configuration.getName());
            }
            ThemeUtils.getAlertDialogBuilder(this).setTitle(R.string.choose_oauth_config)
                    .setItems(configNames.toArray(new String[0]), (DialogInterface dialog, int which) -> {
                        Log.d(DEBUG_TAG, "api selection");
                        try (KeyDatabaseHelper keyDatabase2 = new KeyDatabaseHelper(this)) {
                            KeyDatabaseHelper.copyKey(keyDatabase2.getWritableDatabase(), configNames.get(which),
                                    auth == Auth.OAUTH1A ? EntryType.API_OAUTH1_KEY : EntryType.API_OAUTH2_KEY, apiName);
                        }
                        try {
                            openCustomTab(apiName, auth);
                        } catch (OsmException | NoOAuthConfigurationException | OAuthException | TimeoutException | ExecutionException e) {
                            String message = getString(R.string.toast_no_oauth, apiName);
                            Log.e(DEBUG_TAG, "still no config found " + message);
                            new Handler(Looper.getMainLooper()).post(() -> ScreenMessage.barError(Authorize.this, message));
                            finish();
                        }
                    }).setNegativeButton(R.string.abort, (DialogInterface dialog, int which) -> finish()).create().show();
        }
    }

    /**
     * Start authorisation and open a custom tab
     * 
     * @param auth Auth type
     * 
     * @throws OsmException
     * @throws OAuthException error during the oauth handshake
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws NoOAuthConfigurationException no oauth configuration found
     */
    private void openCustomTab(@NonNull String apiName, @NonNull Auth auth)
            throws OsmException, OAuthException, TimeoutException, ExecutionException, NoOAuthConfigurationException {
        Log.d(DEBUG_TAG, "openCustomTab " + apiName);
        String authUrl = null;
        if (auth == Auth.OAUTH1A) {
            OAuth1aHelper oa = new OAuth1aHelper(this, apiName);
            authUrl = oa.getRequestToken();
        } else if (auth == Auth.OAUTH2) {
            OAuth2Helper oa = new OAuth2Helper(this, apiName, OpenStreetMap.AUTHORIZE_PATH, OpenStreetMap.ACCESS_TOKEN_PATH, OpenStreetMap.OSM_REDIRECT_URI);
            authUrl = oa.getAuthorisationUrl(this, OpenStreetMap.getScopes());
        }
        if (authUrl == null) {
            throw new OsmException("authUrl is null");
        }
        customTabStarted = true;
        Util.launchInCustomTabOrBrowser(this, Uri.parse(authUrl));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (customTabStarted) {
            // this is bit of a hack, we assume that if are resumed, the custom tabs have finished but
            // unsuccessfully, the timeout probably need tweaking.
            // Strictly speaking there is no hard reason why we even do this in a separate activity
            timeoutHandler.postDelayed(() -> {
                Log.d(DEBUG_TAG, "OAuth flow appears to have been cancelled");
                finish(); // Close the blank activity
            }, OAUTH_TIMEOUT);
            return;
        }
        Server server = App.getPreferences(this).getServer();
        String apiName = server.getApiName();
        Auth auth = server.getAuthentication();
        Log.d(DEBUG_TAG, "onResume oauth auth for " + apiName + " " + auth + " " + customTabStarted);
        try {
            openCustomTab(apiName, auth);
        } catch (NoOAuthConfigurationException nex) {
            selectConfigAndRetry(apiName, auth);
        } catch (OsmException oe) {
            showErrorAndFinish(getString(R.string.toast_no_oauth, apiName));
        } catch (OAuthException e) {
            showErrorAndFinish(OAuthHelper.getErrorMessage(this, e));
        } catch (ExecutionException e) {
            showErrorAndFinish(getString(R.string.toast_oauth_communication));
        } catch (TimeoutException e) {
            showErrorAndFinish(getString(R.string.toast_oauth_timeout));
        }
    }

    /**
     * Display an error message and then call finish
     * 
     * @param errorMessage the error message
     */
    private void showErrorAndFinish(@NonNull String errorMessage) {
        Log.e(DEBUG_TAG, "onResume error " + errorMessage);
        ScreenMessage.barError(this, errorMessage);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        timeoutHandler.removeCallbacksAndMessages(null);
        finish(intent);
    }

    /**
     * If the Intent has the correct action finish
     * 
     * @param intent the Intent
     * @return true if we are finishing
     */
    private boolean finish(@NonNull Intent intent) {
        if (ACTION_FINISH_OAUTH.equals(intent.getAction())) {
            Log.d(DEBUG_TAG, "intent calling finishOAuth");
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putBoolean(CUSTOM_TAB_STARTED_KEY, customTabStarted);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(DEBUG_TAG, "onRestoreInstanceState");
        customTabStarted = savedInstanceState.getBoolean(CUSTOM_TAB_STARTED_KEY, false);
    }
}