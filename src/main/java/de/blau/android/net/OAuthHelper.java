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
    private static final String AUTHORIZE_PATH = "oauth/authorize";

    private static final String ACCESS_TOKEN_PATH = "oauth/access_token";

    private static final String REQUEST_TOKEN_PATH = "oauth/request_token";

    private static final String DEBUG_TAG = "OAuthHelper";

    private static final Object  lock = new Object();
    private static OAuthConsumer mConsumer;
    private static OAuthProvider mProvider;
    private static String        mCallbackUrl;

    /**
     * Construct a new helper instance
     * 
     * @param context an Android Context
     * @param osmBaseUrl the base URL for the API instance
     * 
     * @throws OsmException if no configuration could be found for the API instance
     */
    public OAuthHelper(@NonNull Context context, @NonNull String osmBaseUrl) throws OsmException {
        Resources r = context.getResources();
        String[] urls = r.getStringArray(R.array.api_urls);
        String[] keys = r.getStringArray(R.array.api_consumer_keys);
        String[] secrets = r.getStringArray(R.array.api_consumer_secrets);
        String[] oauthUrls = r.getStringArray(R.array.api_oauth_urls);
        synchronized (lock) {
            for (int i = 0; i < urls.length; i++) {
                if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
                    mConsumer = new OkHttpOAuthConsumer(keys[i], secrets[i]);
                    mProvider = new OkHttpOAuthProvider(oauthUrls[i] + REQUEST_TOKEN_PATH, oauthUrls[i] + ACCESS_TOKEN_PATH, oauthUrls[i] + AUTHORIZE_PATH,
                            App.getHttpClient());
                    mProvider.setOAuth10a(true);
                    mCallbackUrl = "vespucci:/oauth/";
                    return;
                }
            }
        }
        logMissingApi(osmBaseUrl);
        throw new OsmException("No matching OAuth configuration found for this API");
    }

    /**
     * Construct a new helper instance
     * 
     * @param osmBaseUrl the base URL for the API instance
     * @param consumerKey the consumer key
     * @param consumerSecret the consumer secret
     * @param callbackUrl the URL to call back to or null
     */
    public OAuthHelper(@NonNull String osmBaseUrl, @NonNull String consumerKey, @NonNull String consumerSecret, @Nullable String callbackUrl) {
        synchronized (lock) {
            mConsumer = new OkHttpOAuthConsumer(consumerKey, consumerSecret);
            mProvider = new OkHttpOAuthProvider(osmBaseUrl + REQUEST_TOKEN_PATH, osmBaseUrl + ACCESS_TOKEN_PATH, osmBaseUrl + AUTHORIZE_PATH,
                    App.getHttpClient());
            mProvider.setOAuth10a(true);
            mCallbackUrl = (callbackUrl == null ? OAuth.OUT_OF_BAND : callbackUrl);
        }
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
     * @param osmBaseUrl the base URL for the API instance
     * 
     * @return an initialized OAuthConsumer or null if something blows up
     */
    @Nullable
    public OkHttpOAuthConsumer getOkHttpConsumer(Context context, @NonNull String osmBaseUrl) {
        Resources r = context.getResources();

        String[] urls = r.getStringArray(R.array.api_urls);
        String[] keys = r.getStringArray(R.array.api_consumer_keys);
        String[] secrets = r.getStringArray(R.array.api_consumer_secrets);
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
                return new OkHttpOAuthConsumer(keys[i], secrets[i]);
            }
        }
        logMissingApi(osmBaseUrl);
        return null;
    }

    /**
     * Create a log message for an unmatched api
     * 
     * @param osmBaseUrl the api url
     */
    private void logMissingApi(@Nullable String osmBaseUrl) {
        Log.d(DEBUG_TAG, "No matching API for " + osmBaseUrl + "found");
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
