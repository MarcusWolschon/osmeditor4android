package de.blau.android.net;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.util.ExecutorTask;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthProvider;

/**
 * Helper class for signpost oAuth more or less based on text below
 * 
 * @author http://nilvec.com/implementing-client-side-oauth-on-android.html
 *
 */
public class OAuthHelper {
    private static final String DEBUG_TAG = "OAuthHelper";

    private static final String CALLBACK_URL       = "vespucci:/oauth/";
    private static final String AUTHORIZE_PATH     = "oauth/authorize";
    private static final String ACCESS_TOKEN_PATH  = "oauth/access_token";
    private static final String REQUEST_TOKEN_PATH = "oauth/request_token";

    private static OAuthConsumer mConsumer;
    private static OAuthProvider mProvider;
    private static String        mCallbackUrl;

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
     * Construct a new helper instance
     * 
     * @param context an Android Context
     * @param apiName the base URL for the API instance
     * 
     * @throws OsmException if no configuration could be found for the API instance
     */
    public OAuthHelper(@NonNull Context context, @NonNull String apiName) throws OsmException {
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(context)) {
            OAuthConfiguration configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), apiName);
            if (configuration != null) {
                init(configuration.getKey(), configuration.getSecret(), configuration.getOauthUrl());
                return;
            }
            logMissingApi(apiName);
            throw new OsmException("No matching OAuth configuration found for this API");
        }
    }

    /**
     * Initialize the fields
     * 
     * @param key OAuth 1a key
     * @param secret OAuth 1a secret
     * @param oauthUrl URL to use for authorization
     */
    private static void init(String key, String secret, String oauthUrl) {
        mConsumer = new OkHttpOAuthConsumer(key, secret);
        mProvider = new OkHttpOAuthProvider(oauthUrl + REQUEST_TOKEN_PATH, oauthUrl + ACCESS_TOKEN_PATH, oauthUrl + AUTHORIZE_PATH, App.getHttpClient());
        mProvider.setOAuth10a(true);
        mCallbackUrl = CALLBACK_URL;
    }

    /**
     * this constructor is for access to the singletons
     */
    public OAuthHelper() {
    }

    /**
     * Returns an OAuthConsumer initialized with the consumer keys for the API in question
     * 
     * @param context an Android Context
     * @param apiName the name of the API configuration
     * 
     * @return an initialized OAuthConsumer or null if something blows up
     */
    @Nullable
    public OkHttpOAuthConsumer getOkHttpConsumer(Context context, @NonNull String apiName) {
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(context)) {
            OAuthConfiguration configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), apiName);
            if (configuration != null) {
                return new OkHttpOAuthConsumer(configuration.getKey(), configuration.getSecret());
            }
            logMissingApi(apiName);
            return null;
        }
    }

    /**
     * Create a log message for an unmatched api
     * 
     * @param apiName the api url
     */
    private void logMissingApi(@Nullable String apiName) {
        Log.d(DEBUG_TAG, "No matching API for " + apiName + "found");
    }

    /**
     * Get the request token
     * 
     * @return the token or null
     * @throws OAuthException if an error happened during the OAuth handshake
     * @throws TimeoutException if we waited too long for a response
     * @throws ExecutionException
     */
    public String getRequestToken() throws OAuthException, TimeoutException, ExecutionException {
        Log.d(DEBUG_TAG, "getRequestToken");
        class RequestTokenTask extends ExecutorTask<Void, Void, String> {
            private OAuthException ex = null;

            /**
             * Create a new RequestTokenTask
             * 
             * @param executorService ExecutorService to run this on
             * @param handler an Handler
             */
            RequestTokenTask(@NonNull ExecutorService executorService, @NonNull Handler handler) {
                super(executorService, handler);
            }

            @Override
            protected String doInBackground(Void param) {
                try {
                    return mProvider.retrieveRequestToken(mConsumer, mCallbackUrl);
                } catch (OAuthException e) {
                    Log.d(DEBUG_TAG, "getRequestToken " + e);
                    ex = e;
                }
                return null;
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

        Logic logic = App.getLogic();
        RequestTokenTask requester = new RequestTokenTask(logic.getExecutorService(), logic.getHandler());
        requester.execute();
        String result = null;
        try {
            result = requester.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) { // NOSONAR cancel does interrupt the thread in question
            requester.cancel();
            throw new TimeoutException(e.getMessage());
        }
        if (result == null) {
            OAuthException ex = requester.getException();
            if (ex != null) {
                throw ex;
            }
        }
        return result;
    }

    /**
     * Queries the service provider for an access token.
     * 
     * @param verifier OAuth 1.0a verification code
     * @return the access token
     * @throws OAuthMessageSignerException
     * @throws OAuthNotAuthorizedException
     * @throws OAuthExpectationFailedException
     * @throws OAuthCommunicationException
     */
    public String[] getAccessToken(String verifier)
            throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
        Log.d(DEBUG_TAG, "verifier: " + verifier);
        if (mProvider == null || mConsumer == null) {
            throw new OAuthExpectationFailedException("OAuthHelper not initialized!");
        }
        mProvider.retrieveAccessToken(mConsumer, verifier);
        return new String[] { mConsumer.getToken(), mConsumer.getTokenSecret() };
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
