package de.blau.android.net;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.util.ExecutorTask;
import oauth.signpost.exception.OAuthException;

/**
 * Helper class for oAuth implementations
 * 
 */
public abstract class OAuthHelper {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OAuthHelper.class.getSimpleName().length());
    private static final String DEBUG_TAG = OAuthHelper.class.getSimpleName().substring(0, TAG_LEN);

    protected static final int TIMEOUT = 10;

    public static class OAuthConfiguration {
        private String key;
        private String secret;
        private String oauthUrl;

        /**
         * @param key the key to set
         */
        public void setKey(@Nullable String key) {
            this.key = key;
        }

        /**
         * @return the key
         */
        @Nullable
        public String getKey() {
            return key;
        }

        /**
         * @param secret the secret to set
         */
        public void setSecret(@Nullable String secret) {
            this.secret = secret;
        }

        /**
         * @return the secret
         */
        @Nullable
        public String getSecret() {
            return secret;
        }

        /**
         * @param oauthUrl the oauthUrl to set
         */
        public void setOauthUrl(@Nullable String oauthUrl) {
            this.oauthUrl = oauthUrl;
        }

        /**
         * @return the oauthUrl
         */
        @Nullable
        public String getOauthUrl() {
            return oauthUrl;
        }
    }

    /**
     * Create a log message for an unmatched api
     * 
     * @param apiName the api url
     */
    protected void logMissingApi(@Nullable String apiName) {
        Log.d(DEBUG_TAG, "No matching API for " + apiName + "found");
    }

    abstract ExecutorTask<Void, Void, ?> getAccessTokenTask(@NonNull Context context, @NonNull Uri data, @NonNull PostAsyncActionHandler handler);

    /**
     * Set the access tokens
     * 
     * @param context an Android context
     * @param accessToken the access token
     * @param secret secret if necessary
     */
    protected void setAccessToken(@NonNull final Context context, @Nullable String accessToken, @Nullable String secret) {
        try (AdvancedPrefDatabase prefDb = new AdvancedPrefDatabase(context)) {
            prefDb.setAPIAccessToken(accessToken, secret);
            AdvancedPrefDatabase.resetCurrentServer();
        }
    }

    /**
     * Run a task to retrieve and set in the configuration the access token for the authentication method
     * 
     * @param context
     * @param data
     * @param handler
     * @throws TimeoutException if the task timeouts
     * @throws ExecutionException if the Task couldn't be exceuted
     */
    public void getAccessToken(@NonNull final Context context, @NonNull Uri data, @NonNull PostAsyncActionHandler handler)
            throws TimeoutException, ExecutionException {
        ExecutorTask<Void, Void, ?> requester = getAccessTokenTask(context, data, handler);
        requester.execute();
        try {
            requester.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) { // NOSONAR cancel does interrupt the thread in question
            requester.cancel();
            throw new TimeoutException(e.getMessage());
        }
    }

    /**
     * Get a fitting error message for an OAuthException
     * 
     * @param context Android Context
     * @param e the OAuthException or null
     * @return a String containing an error message
     */
    public static String getErrorMessage(@NonNull Context context, @Nullable OAuthException e) {
        if (e == null) {
            return context.getString(R.string.toast_oauth_communication);
        }
        return context.getString(R.string.toast_oauth_handshake_failed, e.getMessage());
    }
}
