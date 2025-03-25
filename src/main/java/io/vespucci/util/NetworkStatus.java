package io.vespucci.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Wrapper around ConnectivityManager
 * 
 * @author simon
 *
 */
public class NetworkStatus {

    private static final String DEBUG_TAG = NetworkStatus.class.getSimpleName().substring(0, Math.min(23, NetworkStatus.class.getSimpleName().length()));

    private final ConnectivityManager connectivityManager;

    /**
     * Construct a new NetworkStatus instance
     * 
     * @param ctx an Android Context
     */
    public NetworkStatus(@NonNull Context ctx) {
        connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        try {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Exception getting network status " + e);
            return false;
        }
    }

    /**
     * 
     * @return true if connected or in the process of connecting
     */
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
