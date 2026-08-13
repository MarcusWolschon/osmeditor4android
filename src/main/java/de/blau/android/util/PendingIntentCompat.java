package de.blau.android.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;

public class PendingIntentCompat {
    
    private PendingIntentCompat() {
        /* This utility class should not be instantiated */
    }

    /**
     * Compatibility method for Android SDK Versions < 26
     * 
     * @param context The Context in which this PendingIntent should start the service.
     * @param requestCode Private request code for the sender
     * @param intent An Intent describing the service to be started.
     * @param flags May be FLAG_ONE_SHOT, FLAG_NO_CREATE, FLAG_CANCEL_CURRENT, FLAG_UPDATE_CURRENT, FLAG_IMMUTABLE or
     *            any of the flags as supported by Intent.fillIn() to control which unspecified parts of the intent that
     *            can be supplied when the actual send happens.
     * @return an existing or new PendingIntent matching the given parameters. May return null only if FLAG_NO_CREATE
     *         has been supplied.
     */
    public static PendingIntent getForegroundService(@NonNull Context context, int requestCode, @NonNull Intent intent, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(context, requestCode, intent, flags);
        } else {
            return PendingIntent.getService(context, requestCode, intent, flags);
        }
    }
}