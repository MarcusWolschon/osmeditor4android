package de.blau.android.util;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
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
    private final static Object  lock = new Object();
    private static OAuthConsumer mConsumer;
    private static OAuthProvider mProvider;
    private static String        mCallbackUrl;

    public OAuthHelper(String osmBaseUrl) throws OsmException {
        Resources r = App.resources();
        String urls[] = r.getStringArray(R.array.api_urls);
        String keys[] = r.getStringArray(R.array.api_consumer_keys);
        String secrets[] = r.getStringArray(R.array.api_consumer_secrets);
        String oauth_urls[] = r.getStringArray(R.array.api_oauth_urls);
        synchronized (lock) {
            for (int i = 0; i < urls.length; i++) {
                if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
                    mConsumer = new OkHttpOAuthConsumer(keys[i], secrets[i]);
                    Log.d("OAuthHelper", "Using " + osmBaseUrl + "oauth/request_token " + osmBaseUrl + "oauth/access_token " + osmBaseUrl + "oauth/authorize");
                    Log.d("OAuthHelper", "With key " + keys[i] + " secret " + secrets[i]);
                    mProvider = new OkHttpOAuthProvider(oauth_urls[i] + "oauth/request_token", oauth_urls[i] + "oauth/access_token",
                            oauth_urls[i] + "oauth/authorize");
                    mProvider.setOAuth10a(true);
                    mCallbackUrl = "vespucci:/oauth/"; // OAuth.OUT_OF_BAND; //
                    return;
                }
            }
        }
        Log.d("OAuthHelper", "No matching API for " + osmBaseUrl + "found");
        throw new OsmException("No matching OAuth configuration found for this API");
    }

    public OAuthHelper(String osmBaseUrl, String consumerKey, String consumerSecret, String callbackUrl) throws UnsupportedEncodingException {
        synchronized (lock) {
            mConsumer = new OkHttpOAuthConsumer(consumerKey, consumerSecret);
            mProvider = new OkHttpOAuthProvider(osmBaseUrl + "oauth/request_token", osmBaseUrl + "oauth/access_token", osmBaseUrl + "oauth/authorize");
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
     * @param osmBaseUrl
     * @return an initialized OAuthConsumer
     */
    public OAuthConsumer getConsumer(String osmBaseUrl) {
        Resources r = App.resources();

        String urls[] = r.getStringArray(R.array.api_urls);
        String keys[] = r.getStringArray(R.array.api_consumer_keys);
        String secrets[] = r.getStringArray(R.array.api_consumer_secrets);
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
                return new DefaultOAuthConsumer(keys[i], secrets[i]);
            }
        }
        Log.d("OAuthHelper", "No matching API for " + osmBaseUrl + "found");
        // TODO protect against failure
        return null;
    }
    
    /**
     * Returns an OAuthConsumer initialized with the consumer keys for the API in question
     * 
     * @param osmBaseUrl
     * @return an initialized OAuthConsumer
     */
    public OkHttpOAuthConsumer getOkHttpConsumer(String osmBaseUrl) {
        Resources r = App.resources();

        String urls[] = r.getStringArray(R.array.api_urls);
        String keys[] = r.getStringArray(R.array.api_consumer_keys);
        String secrets[] = r.getStringArray(R.array.api_consumer_secrets);
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
                return new OkHttpOAuthConsumer(keys[i], secrets[i]);
            }
        }
        Log.d("OAuthHelper", "No matching API for " + osmBaseUrl + "found");
        // TODO protect against failure
        return null;
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
        Log.d("OAuthHelper", "getRequestToken");
        class RequestTokenTask extends AsyncTask<Void, Void, String> {
            private OAuthException ex = null;

            @Override
            protected String doInBackground(Void... params) {
                try {
                    return mProvider.retrieveRequestToken(mConsumer, mCallbackUrl);
                } catch (OAuthException e) {
                    Log.d("OAuthHelper", "getRequestToken " + e);
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
            requester.cancel(true);
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
     * @param verifier
     * @return
     * @throws OAuthMessageSignerException
     * @throws OAuthNotAuthorizedException
     * @throws OAuthExpectationFailedException
     * @throws OAuthCommunicationException
     */
    public String[] getAccessToken(String verifier)
            throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
        Log.d("OAuthHelper", "verifier: " + verifier);
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
        if (e instanceof OAuthMessageSignerException) {
            return context.getString(R.string.toast_oauth_handshake_failed, e.getMessage());
        } else if (e instanceof OAuthNotAuthorizedException) {
            return context.getString(R.string.toast_oauth_handshake_failed, e.getMessage());
        } else if (e instanceof OAuthExpectationFailedException) {
            return context.getString(R.string.toast_oauth_handshake_failed, e.getMessage());
        } else {
            return context.getString(R.string.toast_oauth_handshake_failed, e.getMessage());
        }
    }

}
