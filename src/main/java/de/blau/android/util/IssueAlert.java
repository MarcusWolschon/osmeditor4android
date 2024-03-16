package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.RemoteControlUrlActivity;
import de.blau.android.contract.Schemes;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.validation.Validator;

/**
 * Generate an Android notification for OSM elements that have an issue and for Notes and other QA "bugs"
 * 
 * @author simon
 *
 */
public final class IssueAlert {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, IssueAlert.class.getSimpleName().length());
    private static final String DEBUG_TAG = IssueAlert.class.getSimpleName().substring(0, TAG_LEN);

    private static final String QA_CHANNEL      = "qa";
    private static final String PACKAGE_NAME    = "de.blau.android";
    private static final String GROUP_DATA      = PACKAGE_NAME + ".Data";
    private static final int    GROUP_DATA_ID   = GROUP_DATA.hashCode();
    private static final String GROUP_NOTES     = PACKAGE_NAME + ".Notes";
    private static final int    GROUP_NOTES_ID  = GROUP_NOTES.hashCode();
    private static final String GROUP_OSMOSE    = PACKAGE_NAME + ".Osmose";
    private static final int    GROUP_OSMOSE_ID = GROUP_OSMOSE.hashCode();

    /**
     * Private constructor to avoid instantiation of the class
     */
    private IssueAlert() {
    }

    /**
     * Generate an alert/notification if something is problematic about the OSM object
     * 
     * @param context Android Context
     * @param e OsmElement we are generating an alert for
     */
    public static void alert(@NonNull Context context, @NonNull OsmElement e) {
        Preferences prefs = App.getPreferences(context);
        if (!prefs.generateAlerts()) { // don't generate alerts
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        try {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException sex) {
            // can be safely ignored
        }

        double[] centroid = Geometry.centroid(e);
        if (centroid.length != 2) {
            return;
        }
        double eLon = centroid[0];
        double eLat = centroid[1];
        String title = context.getString(R.string.alert_data_issue);
        String ticker = title;
        StringBuilder message = new StringBuilder("");
        if (location != null) {
            // if we know where we are we can provide better information
            long distance = 0;
            if (Node.NAME.equals(e.getName())) {
                distance = Math.round(GeoMath.haversineDistance(location.getLongitude(), location.getLatitude(), eLon, eLat));
            } else if (Way.NAME.equals(e.getName())) {
                ClosestPoint cp = getClosestDistance(location.getLongitude(), location.getLatitude(), (Way) e);
                distance = Math.round(cp.distance);
                eLon = cp.lon;
                eLat = cp.lat;
            }
            // filter
            if (distance > prefs.getMaxAlertDistance()) {
                return;
            }

            message.append(context.getString(R.string.alert_distance_direction, distance,
                    Util.getBearingString(context, location.getLongitude(), location.getLatitude(), eLon, eLat)) + "\n");
            ticker = ticker + " " + message;
        }
        Validator validator = App.getDefaultValidator(context);
        String[] descriptions = validator.describeProblem(context, e);
        int len = descriptions.length;
        for (int i = 0; i < len; i++) {
            message.append(descriptions[i]);
            if (i < len - 1) {
                message.append(" ");
            }
        }
        Notifications.initChannel(context, QA_CHANNEL, R.string.qa_channel_name, R.string.qa_channel_description);
        NotificationCompat.Builder mBuilder;
        try {
            mBuilder = Notifications.builder(context, QA_CHANNEL).setSmallIcon(R.drawable.logo_simplified).setContentTitle(title)
                    .setContentText(message.toString()).setPriority(NotificationCompat.PRIORITY_HIGH).setTicker(ticker).setAutoCancel(true).setGroup(GROUP_DATA)
                    .setColor(ContextCompat.getColor(context, R.color.osm_green));
            Notifications.setGroupAlertBehavior(prefs, mBuilder);
        } catch (RuntimeException re) {
            // NotificationCompat.Builder seems to be flaky instead of crashing we produce a
            // crash dump and return
            ACRAHelper.nocrashReport(re, re.getMessage());
            return;
        }
        Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        try {
            BoundingBox box = GeoMath.createBoundingBoxForCoordinates(eLat, eLon, prefs.getDownloadRadius());
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(Schemes.JOSM).appendPath(RemoteControlUrlActivity.LOAD_AND_ZOOM_COMMAND)
                    .appendQueryParameter(RemoteControlUrlActivity.LEFT_PARAMETER, Double.toString(box.getLeft() / 1E7D))
                    .appendQueryParameter(RemoteControlUrlActivity.RIGHT_PARAMETER, Double.toString(box.getRight() / 1E7D))
                    .appendQueryParameter(RemoteControlUrlActivity.TOP_PARAMETER, Double.toString(box.getTop() / 1E7D))
                    .appendQueryParameter(RemoteControlUrlActivity.BOTTOM_PARAMETER, Double.toString(box.getBottom() / 1E7D))
                    .appendQueryParameter(RemoteControlUrlActivity.SELECT_PARAMETER, e.getName() + e.getOsmId());
            Uri rc = builder.build();
            Log.d(DEBUG_TAG, rc.toString());
            resultIntent.setData(rc);
            mBuilder.setContentIntent(Notifications.createPendingIntent(context, Main.class, resultIntent));

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            addGroupNotification(context, QA_CHANNEL, GROUP_DATA, GROUP_DATA_ID, title, mNotificationManager);

            // mId allows you to update the notification later on.
            mNotificationManager.notify(id(e), mBuilder.build());
            App.getOsmDataNotifications(context).save(mNotificationManager, id(e));
        } catch (OsmException e1) {
            Log.d(DEBUG_TAG, "Illegal BB created from lat " + eLat + " lon " + eLon + " r " + prefs.getDownloadRadius());
        }
    }

    /**
     * Generate an unique id for an OsmElement
     * 
     * @param e the OsmElement
     * @return an unique id
     */
    private static int id(@NonNull OsmElement e) {
        return (e.getName() + e.getOsmId()).hashCode();
    }

    /**
     * Generate an alert/notification if we found a task object nearby.
     * 
     * 
     * @param context Android Context
     * @param prefs a Preference instance
     * @param b the Task
     */
    public static void alert(@NonNull Context context, @NonNull Preferences prefs, @NonNull Task b) {
        if (!prefs.generateAlerts()) { // don't generate alerts
            return;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        try {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException sex) {
            // can be safely ignored
        }
        double eLon = b.getLon() / 1E7D;
        double eLat = b.getLat() / 1E7D;

        String title = b instanceof Note ? context.getString(R.string.alert_note) : context.getString(R.string.alert_bug);
        String ticker = title;
        String message = "";
        if (location != null) {
            // if we know where we are we can provide better information
            long distance = Math.round(GeoMath.haversineDistance(location.getLongitude(), location.getLatitude(), eLon, eLat));

            // filter
            if (distance > Math.sqrt(8D * prefs.getBugDownloadRadius() * prefs.getBugDownloadRadius())) {
                // diagonal of auto download box
                return;
            }

            message = context.getString(R.string.alert_distance_direction, distance,
                    Util.getBearingString(context, location.getLongitude(), location.getLatitude(), eLon, eLat)) + "\n";
            ticker = ticker + " " + message;
        }
        message = message + b.getDescription();
        Notifications.initChannel(context, QA_CHANNEL, R.string.qa_channel_name, R.string.qa_channel_description);
        NotificationCompat.Builder mBuilder;
        try {
            mBuilder = Notifications.builder(context, QA_CHANNEL).setSmallIcon(R.drawable.logo_simplified).setContentTitle(title).setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH).setTicker(ticker).setAutoCancel(true)
                    .setGroup(b instanceof Note ? GROUP_NOTES : GROUP_OSMOSE).setColor(ContextCompat.getColor(context, R.color.osm_green));
            Notifications.setGroupAlertBehavior(prefs, mBuilder);
        } catch (RuntimeException re) {
            // NotificationCompat.Builder seems to be flaky instead of crashing we produce a
            // crash dump and return
            ACRAHelper.nocrashReport(re, re.getMessage());
            return;
        }
        Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        resultIntent.setData(Uri.fromParts(Schemes.GEO, eLat + "," + eLon, null));
        mBuilder.setContentIntent(Notifications.createPendingIntent(context, Main.class, resultIntent));

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (b instanceof Note) {
            addGroupNotification(context, QA_CHANNEL, GROUP_NOTES, GROUP_NOTES_ID, title, mNotificationManager);
        } else {
            addGroupNotification(context, QA_CHANNEL, GROUP_OSMOSE, GROUP_OSMOSE_ID, title, mNotificationManager);
        }
        // mId allows you to update the notification later on.
        int id = id(b);
        mNotificationManager.notify(id, mBuilder.build());
        App.getTaskNotifications(context).save(mNotificationManager, id);
    }

    /**
     * Generate an unique id for a Task
     * 
     * @param b the Task
     * @return an unique id
     */
    private static int id(@NonNull Task b) {
        return (b.getClass().getSimpleName() + b.hashCode()).hashCode();
    }

    /**
     * Check if we have already shown a group notification
     * 
     * Note using this requires API 23 or higher
     * 
     * @param notificationManager a NotificationManager instance
     * @param groupId the group id we a rechecking
     * @return true if present
     */
    @SuppressLint("NewApi")
    private static boolean hasGroupNotification(@NonNull NotificationManager notificationManager, int groupId) {
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == groupId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancel a notification for a specific OsmElement
     * 
     * @param context Android Context
     * @param e the OsmElement
     */
    public static void cancel(@NonNull Context context, @NonNull OsmElement e) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        App.getOsmDataNotifications(context).remove(mNotificationManager, id(e));
    }

    /**
     * Cancel a notification for a specific task
     * 
     * @param context Android Context, if null we do nothing
     * @param b Task we want to remove the notification for
     */
    public static void cancel(@Nullable Context context, @NonNull Task b) {
        if (context != null) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            App.getTaskNotifications(context).remove(mNotificationManager, id(b)); // cancels and removes from cache
        }
    }

    static class ClosestPoint {
        double distance = Double.MAX_VALUE;
        double lat;
        double lon;
    }

    /**
     * Get the closest distance from the coordinates to a way
     * 
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param w the Way
     * @return a ClosestPoint instance
     */
    @NonNull
    private static ClosestPoint getClosestDistance(double lon, double lat, @NonNull Way w) {
        ClosestPoint closest = new IssueAlert.ClosestPoint();

        double ny = GeoMath.latToMercator(lat);
        double nx = lon;

        ArrayList<Node> nodes = new ArrayList<>(w.getNodes());
        for (int i = 0; i <= nodes.size() - 2; i++) {
            double bx = nodes.get(i).getLon() / 1E7D;
            double by = GeoMath.latE7ToMercator(nodes.get(i).getLat());
            double ax = nodes.get(i + 1).getLon() / 1E7D;
            double ay = GeoMath.latE7ToMercator(nodes.get(i + 1).getLat());
            float[] newClosest = GeoMath.closestPoint((float) nx, (float) ny, (float) bx, (float) by, (float) ax, (float) ay);
            double newDistance = GeoMath.haversineDistance(nx, ny, newClosest[0], newClosest[1]);
            if (newDistance < closest.distance) {
                closest.distance = newDistance;
                closest.lon = newClosest[0];
                closest.lat = GeoMath.mercatorToLat(newClosest[1]);
            }
        }
        return closest;
    }

    /**
     * Add a group notification so that the alerts can be grouped
     * 
     * This is things actually work and not how google has them in the documentation, see
     * https://stackoverflow.com/questions/36058887/setgroup-in-notification-not-working
     * 
     * @param context Android Context
     * @param channel the name of the NotificationChannel
     * @param title the titles for this group
     * @param group the group name
     * @param groupId an unique id for the group
     * @param notificationManager a NotificationManager instance
     */
    private static void addGroupNotification(@NonNull Context context, @NonNull String channel, @NonNull String group, int groupId, @NonNull String title,
            @NonNull NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !hasGroupNotification(notificationManager, groupId)) {
            NotificationCompat.Builder groupBuilder = null;
            try {
                groupBuilder = Notifications.builder(context, channel).setSmallIcon(R.drawable.logo_simplified).setContentTitle(title)
                        .setPriority(NotificationCompat.PRIORITY_HIGH).setGroup(group).setGroupSummary(true)
                        .setColor(ContextCompat.getColor(context, R.color.osm_green));
            } catch (RuntimeException re) {
                // don't do anything
            }
            if (groupBuilder != null) {
                notificationManager.notify(groupId, groupBuilder.build());
            }
        }
    }
}
