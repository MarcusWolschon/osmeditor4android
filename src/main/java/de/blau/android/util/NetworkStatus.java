package de.blau.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

public class NetworkStatus {

    private static final String DEBUG_TAG = "NetworkStatus";

    private final ConnectivityManager connectivityManager;
    
    public NetworkStatus(@NonNull Context ctx) {
        connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    public boolean isConnected() {
        try {           
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Exception getting network status " + e);
            return false;
        }
    }
    
    public boolean isConnectedOrConnecting() {
        try {           
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Exception getting network status " + e);
            return false;
        }
    }
}
