package io.vespucci.net;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.App;
import io.vespucci.PostAsyncActionHandler;
import io.vespucci.exception.OsmException;
import io.vespucci.prefs.API.Auth;
import io.vespucci.resources.KeyDatabaseHelper;
import io.vespucci.util.ExecutorTask;
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
 */
public class OAuth1aHelper extends OAuthHelper {
    private static final String DEBUG_TAG = OAuth1aHelper.class.getSimpleName().substring(0, Math.min(23, OAuth1aHelper.class.getSimpleName().length()));

    private static final String CALLBACK_URL       = "vespucci:/oauth/";
    private static final String AUTHORIZE_PATH     = "oauth/authorize";
    private static final String ACCESS_TOKEN_PATH  = "oauth/access_token";
    private static final String REQUEST_TOKEN_PATH = "oauth/request_token";

    private static final String OAUTH_VERIFIER_PARAMTER = "oauth_verifier";
    private static final String OAUTH_TOKEN_PARAMETER   = "oauth_token";

    private static OAuthConsumer mConsumer;
    private static OAuthProvider mProvider;
    private static String        mCallbackUrl;

    /**
     * Construct a new helper instance
     * 
     * @param context an Android Context
     * @param apiName the base URL for the API instance
     * 
     * @throws OsmException if no configuration could be found for the API instance
     */
    public OAuth1aHelper(@NonNull Context context, @NonNull String apiName) throws OsmException {
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(context)) {
            OAuthConfiguration configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), apiName, Auth.OAUTH1A);
            if (configuration != null) {
                init(configuration.getKey(), configuration.getSecret(), configuration.getOauthUrl());
                return;
            }
            logMissingApi(apiName);
            throw new OsmException("No matching OAuth configuration found for API " + apiName);
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
    public OAuth1aHelper() {
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
            OAuthConfiguration configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), apiName, Auth.OAUTH1A);
            if (configuration != null) {
                return new OkHttpOAuthConsumer(configuration.getKey(), configuration.getSecret());
            }
            logMissingApi(apiName);
            return null;
        }
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
            RequestTokenTask() {
                super();
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

        RequestTokenTask requester = new RequestTokenTask();
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

    @Override
    protected ExecutorTask<Void, Void, ?> getAccessTokenTask(Context context, Uri data, PostAsyncActionHandler handler) {
        String oauthToken = data.getQueryParameter(OAUTH_TOKEN_PARAMETER);
        final String oauthVerifier = data.getQueryParameter(OAUTH_VERIFIER_PARAMTER);

        if ((oauthToken == null) && (oauthVerifier == null)) {
            Log.i(DEBUG_TAG, "got oauth verifier " + oauthToken + " " + oauthVerifier);
            throw new IllegalArgumentException("No token or verifier");
        }
        return new ExecutorTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void arg)
                    throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
                if (mProvider == null || mConsumer == null) {
                    throw new OAuthExpectationFailedException("OAuthHelper not initialized!");
                }
                mProvider.retrieveAccessToken(mConsumer, oauthVerifier);
                setAccessToken(context, mConsumer.getToken(), mConsumer.getTokenSecret());
                return true;
            }

            @Override
            protected void onBackgroundError(Exception e) {
                handler.onError(null);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                Log.d(DEBUG_TAG, "oAuthHandshake onPostExecute");
                if (success != null && success) {
                    handler.onSuccess();
                } else {
                    handler.onError(null);
                }
            }
        };
    }
}
