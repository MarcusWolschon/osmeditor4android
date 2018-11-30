package de.blau.android.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import de.blau.android.BuildConfig;
import de.blau.android.R;
import de.blau.android.contract.Flavors;
import de.blau.android.prefs.Preferences;

/**
 * Wrapper and utils for accessing NotificationCompat and NotificationChannels for support lib 26 and later
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
        initDefaultChannel(context);
        return builder(context, DEFAULT_CHANNEL);
    }

    /**
     * Create a new instance of NotificationCompat.Builder in a support lib and os version independent way
     * 
     * @param context Android Context
     * @param channelId the NotificationChannel id, the channel has to exist
     * @return a NotificationCompat.Builder instance
     */
    @SuppressWarnings("deprecation")
    public static NotificationCompat.Builder builder(@NonNull Context context, @NonNull String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            if (DEFAULT_CHANNEL.equals(channelId)) {
                return new NotificationCompat.Builder(context, DEFAULT_CHANNEL);
            } else {
                return new NotificationCompat.Builder(context, channelId); // NOSONAR
            }
        } else {
            return new NotificationCompat.Builder(context); // NOSONAR
        }
    }

    /**
     * Create the default notification channel Does nothing if run on a pre-NofiticationChannel OS
     * 
     * @param context Android Context
     */
    private static void initDefaultChannel(@NonNull Context context) {
        initChannel(context, DEFAULT_CHANNEL, R.string.default_channel_name, R.string.default_channel_description);
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
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel channel = new NotificationChannel(channelId, context.getString(nameRes), NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(context.getString(descriptionRes));
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Set the group alert behaviour 
     * 
     * @param prefs a Preferences instance
     * @param mBuilder the NotificationCompat.Builder we want to change
     */
    public static void setGroupAlertBehavior(Preferences prefs, NotificationCompat.Builder mBuilder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && prefs.groupAlertOnly()) {
            mBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
        }
    }
}
