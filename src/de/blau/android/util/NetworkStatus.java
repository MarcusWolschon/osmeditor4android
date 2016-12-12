package de.blau.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkStatus {
	
	private static final String DEBUG_TAG = "NetworkStatus";

	public static boolean isConnected(Context ctx) {
		if (ctx==null) {
			Log.e(DEBUG_TAG,"Context null");
			return false;
		}
	    try {
			ConnectivityManager connectivityManager 
			      = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
			return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
		} catch (Exception e) {
			Log.e(DEBUG_TAG,"Exception getting network status " + e);
			return false;
		}
	}
}
