package de.blau.android.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import de.blau.android.BuildConfig;
import de.blau.android.R;

/**
 * 
 * Pre support lib 26 version
 * 
 * @author simon
 *
 */
public final class Notifications {

    private static final String DEFAULT_CHANNEL = "default";
    
    /**
     * Private constructor
     */
    private Notifications() {
        // do nothing
    }

    /**
     * Create a new instance of NotificationCompat.Builder in a support lib and os version independent way
     * 
     * @param context Android Context
     * @return a NotificationCompat.Builder instance
     */
    public static NotificationCompat.Builder builder(Context context) {
        return builder(context, DEFAULT_CHANNEL);
    }

    /**
     * Create a new instance of NotificationCompat.Builder in a support lib and os version independent way
     * 
     * @param context Android Context
     * @param channelId the NotificationChannel id, the channel has to exist
     * @return a NotificationCompat.Builder instance
     */
    public static NotificationCompat.Builder builder(@NonNull Context context, @NonNull String channelId) {
        return new NotificationCompat.Builder(context); // NOSONAR
    }

    /**
     * Create a default notification channel Does nothing if run on a pre-NofiticationChannel OS
     * 
     * @param context Android Context
     * @param channelId the id we will to use to refer to this channel
     * @param nameRes the resource id for the name of the channel
     * @param descriptionRes the resource id for the description of the channel
     */
    public static void initChannel(@NonNull Context context, @NonNull String channelId, int nameRes, int descriptionRes) {
    }
}
