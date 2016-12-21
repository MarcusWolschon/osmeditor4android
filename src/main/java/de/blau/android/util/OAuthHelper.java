package de.blau.android.util;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

/**
 * Helper class for signpost oAuth more or less based on text below
 * @author http://nilvec.com/implementing-client-side-oauth-on-android.html
 *
 */
public class OAuthHelper {
	private final static Object lock = new Object();
	private static OAuthConsumer mConsumer;
	private static OAuthProvider mProvider;
	private static String mCallbackUrl;
	
	public OAuthHelper(String osmBaseUrl) throws OsmException {
		Resources r = Application.mainActivity.getResources();
		String urls[] = r.getStringArray(R.array.api_urls);
		String keys[] = r.getStringArray(R.array.api_consumer_keys);
		String secrets[] = r.getStringArray(R.array.api_consumer_secrets);
		String oauth_urls[] = r.getStringArray(R.array.api_oauth_urls);
		synchronized (lock) {
			for (int i=0;i < urls.length;i++ ) {
				if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
					mConsumer = new CommonsHttpOAuthConsumer(keys[i], secrets[i]);
					Log.d("OAuthHelper", "Using " + osmBaseUrl + "oauth/request_token " + osmBaseUrl + "oauth/access_token " + osmBaseUrl + "oauth/authorize");
					Log.d("OAuthHelper", "With key " + keys[i] + " secret " + secrets[i]);
					mProvider = new CommonsHttpOAuthProvider(
							oauth_urls[i] + "oauth/request_token",
							oauth_urls[i] + "oauth/access_token",
							oauth_urls[i] + "oauth/authorize");
					mProvider.setOAuth10a(true);
					mCallbackUrl = "vespucci://oauth/"; //OAuth.OUT_OF_BAND; //
					return;
				}
			}
		}
		Log.d("OAuthHelper", "No matching API for " + osmBaseUrl + "found");
		throw new OsmException("No matching OAuth configuration found for this API");
	}
	
	public OAuthHelper(String osmBaseUrl, String consumerKey, String consumerSecret, String callbackUrl)
	throws UnsupportedEncodingException {
		synchronized (lock) {
			mConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
			mProvider = new CommonsHttpOAuthProvider(
					osmBaseUrl + "oauth/request_token",
					osmBaseUrl + "oauth/access_token",
					osmBaseUrl + "oauth/authorize");
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
	 * @param osmBaseUrl
	 * @return
	 */
	public OAuthConsumer getConsumer(String osmBaseUrl) {
		Resources r = Application.mainActivity.getResources();

		String urls[] = r.getStringArray(R.array.api_urls);
		String keys[] = r.getStringArray(R.array.api_consumer_keys);
		String secrets[] = r.getStringArray(R.array.api_consumer_secrets);
		for (int i=0;i < urls.length;i++ ) {
			if (urls[i].equalsIgnoreCase(osmBaseUrl)) {
				return new DefaultOAuthConsumer(keys[i], secrets[i]);
			}
		}
		Log.d("OAuthHelper", "No matching API for " + osmBaseUrl + "found");
		//TODO protect against failure
		return null;
	}
	
	/**
	 * 
	 * @return null if fails
	 */
	public String getRequestToken() throws OAuthException, InterruptedException, ExecutionException,TimeoutException {	 
		Log.d("OAuthHelper", "getRequestToken");
		class MyTask extends AsyncTask<Void, Void, String> {
			private OAuthException ex = null;
			
			@Override
			protected String doInBackground(Void... params) {	
				try {
					String authUrl = mProvider.retrieveRequestToken(mConsumer, mCallbackUrl);
					return authUrl;
				} catch (OAuthException e) {
					Log.d("OAuthHelper", "getRequestToken " + e);
					ex = e;
				} 
				return null;
			}
			
			OAuthException getException() {
				return ex;
			}
		}
		MyTask loader = new MyTask();
		loader.execute();	
		String result = loader.get(10, TimeUnit.SECONDS);
		if (result == null) {
			OAuthException ex = loader.getException();
			if (ex != null) {
				throw ex;
			}
		}
		return result;
	}
		
	public String[] getAccessToken(String verifier)
			throws OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		Log.d("OAuthHelper", "verifier: " + verifier);
		if (mProvider == null || mConsumer == null)
			throw new OAuthExpectationFailedException("OAuthHelper not initialized!");
		mProvider.retrieveAccessToken(mConsumer, verifier);
		return new String[] {
				mConsumer.getToken(), mConsumer.getTokenSecret()
		};
	}

	public static String getErrorMessage(Context context, OAuthException e) {
		if (e instanceof OAuthMessageSignerException) {
			return context.getString(R.string.toast_oauth_handshake_failed,e.getMessage());
		} else if (e instanceof OAuthNotAuthorizedException) {
			return context.getString(R.string.toast_oauth_handshake_failed,e.getMessage());
		} else if (e instanceof OAuthExpectationFailedException) {
			return context.getString(R.string.toast_oauth_handshake_failed,e.getMessage());
		} else {
			return context.getString(R.string.toast_oauth_handshake_failed,e.getMessage());
		}
	}
	
}
