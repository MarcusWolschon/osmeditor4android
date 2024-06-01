package de.blau.android.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 * Wrapper and utils for accessing NotificationCompat and NotificationChannels for support lib 26 and later
 * 
 * @author simon
 *
 */
public final class Notifications {

    public static final String DEFAULT_CHANNEL = "default";

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
            return new NotificationCompat.Builder(context, channelId);
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
     * Create a default notification channel Does nothing if run on a pre-NotificationChannel OS
     * 
     * @param context Android Context
     * @param channelId the id we will to use to refer to this channel
     * @param nameRes the resource id for the name of the channel
     * @param descriptionRes the resource id for the description of the channel
     */
    public static void initChannel(@NonNull Context context, @NonNull String channelId, int nameRes, int descriptionRes) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel channel = new NotificationChannel(channelId, context.getString(nameRes), NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(context.getString(descriptionRes));
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Check if a specific channel is enabled (and present) or not
     * 
     * On pre-NofiticationChannel OS version this will always return true
     * 
     * @param context Android Context
     * @param channelId the channel id
     * @return true if enabled (or running on a pre-channel OS)
     */
    public static boolean channelEnabled(@NonNull Context context, @NonNull String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel != null) {
                return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
            }
            return true; // channel doesn't exist yet
        }
        return true; // channel doesn't exist yet
    }

    /**
     * Set the group alert behaviour
     * 
     * @param prefs a Preferences instance
     * @param mBuilder the NotificationCompat.Builder we want to change
     */
    public static void setGroupAlertBehavior(Preferences prefs, NotificationCompat.Builder mBuilder) {
        if (prefs.groupAlertOnly()) {
            mBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
        }
    }

    /**
     * Display a top prio notification for errors etc
     * 
     * @param ctx an Android Context
     * @param titleRes String resource for title
     * @param message the message
     * @param id unique notification id
     */
    public static void error(@NonNull Context ctx, int titleRes, @NonNull final String message, int id) {
        coloredNotification(ctx, titleRes, message, null, id, R.attr.snack_error, R.color.material_red);
    }

    /**
     * Display a notification for warnings
     * 
     * @param ctx an Android Context
     * @param titleRes String resource for title
     * @param message the message
     * @param id unique notification id
     */
    public static void warning(@NonNull Context ctx, int titleRes, @NonNull final String message, int id) {
        coloredNotification(ctx, titleRes, message, null, id, R.attr.snack_warning, R.color.material_orange);
    }

    /**
     * Display a notification for warnings
     * 
     * @param ctx an Android Context
     * @param titleRes String resource for title
     * @param message the message
     * @param id unique notification id
     * @param pendingIntent PendingIntent to use on click
     */
    public static void warning(@NonNull Context ctx, int titleRes, @NonNull final String message, int id, @Nullable PendingIntent pendingIntent) {
        coloredNotification(ctx, titleRes, message, pendingIntent, id, R.attr.snack_warning, R.color.material_orange);
    }

    /**
     * Display a colored notification
     * 
     * @param ctx an Android Context
     * @param titleRes String resource for title
     * @param message the message
     * @param pendingIntent optional PendingIntent to use on click
     * @param id unique notification id
     * @param colorAttr themed color attribute
     * @param fallbackColorRes fallback color resource id
     */
    private static void coloredNotification(@NonNull Context ctx, int titleRes, @NonNull final String message, @Nullable PendingIntent pendingIntent, int id,
            int colorAttr, int fallbackColorRes) {
        NotificationCompat.Builder builder = Notifications.builder(ctx).setSmallIcon(R.drawable.logo_simplified).setContentTitle(ctx.getString(titleRes))
                .setColorized(true).setColor(ThemeUtils.getStyleAttribColorValue(ctx, colorAttr, fallbackColorRes));
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message)).setPriority(NotificationCompat.PRIORITY_MAX);
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }
        NotificationManagerCompat nManager = NotificationManagerCompat.from(ctx);
        nManager.notify(id, builder.build());
    }

    /**
     * Create a pending intent
     * 
     * @param context an Android Context
     * @param sourceActivityClass the source Activity class
     * @param intent the Intent
     * @return a PendingIntent
     */
    @NonNull
    public static PendingIntent createPendingIntent(@NonNull Context context, @NonNull Class<?> sourceActivityClass, @NonNull Intent intent) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(sourceActivityClass);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
