package de.blau.android.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.UnsupportedFormatException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.osm.Track.WayPoint;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.NmeaTcpClient;
import de.blau.android.services.util.NmeaTcpClientServer;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Notifications;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SavingHelper.Exportable;
import de.blau.android.util.Snack;
import de.blau.android.validation.Validator;

public class TrackerService extends Service implements Exportable {

    private static final float TRACK_LOCATION_MIN_ACCURACY = 200f;

    private static final String DEBUG_TAG = "TrackerService";

    private static final int LOCATION_UPDATE   = 0;
    public static final int  CONNECTION_FAILED = 1;

    private static final String AUTODOWNLOAD_KEY = "autodownload";

    private static final String BUGAUTODOWNLOAD_KEY = "bugautodownload";

    private static final String TRACK_KEY = "track";

    private final TrackerBinder mBinder = new TrackerBinder();

    private LocationManager locationManager = null;

    private boolean tracking = false;

    private boolean downloading = false;

    private boolean downloadingBugs = false;

    private Track track;

    private TrackerLocationListener externalListener = null;

    private boolean listenerNeedsGPS = false;

    private Location lastLocation        = null;
    private Location previousLocation    = null;
    private Location previousBugLocation = null;

    private boolean gpsEnabled = false;

    private Preferences prefs = null;

    private Handler mHandler = null;

    private NmeaTcpClient tcpClient = null;

    private NmeaTcpClientServer tcpServer = null;

    private enum GpsSource {
        INTERNAL, NMEA, TCP
    }

    private GpsSource source = GpsSource.INTERNAL;

    private String prefInternal;
    private String prefNmea;
    private String prefTcpClient;
    private String prefTcpServer;

    private Location nmeaLocation;

    private ConnectivityManager connectivityManager;

    private Validator validator;

    Handler handler = new Handler(Looper.getMainLooper());

    /**
     * For no apparent sane reason google has deprecated the NmeaListener interface
     */
    OldNmeaListener oldNmeaListener = null;
    NewNmeaListener newNmeaListener = null;

    private long staleGPSMilli = 20000L;              // 20 seconds
    private long staleGPSNano  = staleGPSMilli * 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(DEBUG_TAG, "onCreate");
        track = new Track(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        prefs = new Preferences(this);
        validator = App.getDefaultValidator(this);
        prefInternal = getString(R.string.gps_source_internal);
        prefNmea = getString(R.string.gps_source_nmea);
        prefTcpClient = getString(R.string.gps_source_tcpclient);
        prefTcpServer = getString(R.string.gps_source_tcpserver);
        nmeaLocation = new Location(prefNmea);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            oldNmeaListener = new OldNmeaListener();
        } else {
            newNmeaListener = new NewNmeaListener();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        stopTracking(false);
        track.close();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(DEBUG_TAG, "Received null intent"); //
            return START_STICKY; // NOTE not clear how or if we should return an
                                 // error here
        }
        if (intent.getBooleanExtra(TRACK_KEY, false)) {
            startTrackingInternal();
        } else if (intent.getBooleanExtra(AUTODOWNLOAD_KEY, false)) {
            startAutoDownloadInternal();
        } else if (intent.getBooleanExtra(BUGAUTODOWNLOAD_KEY, false)) {
            startBugAutoDownloadInternal();
        } else {
            Log.d(DEBUG_TAG, "Received intent with unknown meaning");
        }

        return START_STICKY;
    }

    /**
     * Starts the tracker service (which invokes {@link #onStartCommand(Intent, int, int)}, which invokes
     * {@link #startTrackingInternal()}, which does the actual work. To start tracking, bind the service, then call
     * this.
     */
    public void startTracking() {
        Intent intent = new Intent(this, TrackerService.class);
        intent.putExtra(TRACK_KEY, true);
        startService(intent);
    }

    /**
     * Starts the tracker service (which invokes {@link #onStartCommand(Intent, int, int)}, which invokes
     * {@link #startTrackingInternal()}, which does the actual work. To start tracking, bind the service, then call
     * this.
     */
    public void startAutoDownload() {
        Intent intent = new Intent(this, TrackerService.class);
        intent.putExtra(AUTODOWNLOAD_KEY, true);
        startService(intent);
    }

    /**
     * Starts the tracker service (which invokes {@link #onStartCommand(Intent, int, int)}, which invokes
     * {@link #startTrackingInternal()}, which does the actual work. To start tracking, bind the service, then call
     * this.
     */
    public void startBugAutoDownload() {
        Intent intent = new Intent(this, TrackerService.class);
        intent.putExtra(BUGAUTODOWNLOAD_KEY, true);
        startService(intent);
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startTrackingInternal() {
        if (tracking) {
            track.markNewSegment();
            return;
        }
        startInternal();
        tracking = true;
        track.markNewSegment();
        updateGPSState();
        if (externalListener != null) {
            externalListener.onStateChanged();
        }
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startAutoDownloadInternal() {
        Log.d(DEBUG_TAG, "Start auto download");
        if (downloading) {
            return;
        }
        startInternal();
        downloading = true;
        updateGPSState();
        if (externalListener != null) {
            externalListener.onStateChanged();
        }
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startBugAutoDownloadInternal() {
        if (downloadingBugs) {
            return;
        }
        startInternal();
        downloadingBugs = true;
        updateGPSState();
        if (externalListener != null) {
            externalListener.onStateChanged();
        }
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startInternal() {
        if (tracking || downloading || downloadingBugs) {
            return; // all ready running
        }
        NotificationCompat.Builder notificationBuilder = Notifications.builder(this);

        Intent appStartIntent = new Intent();
        appStartIntent.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(Main.class.getPackage().getName(), Main.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingAppIntent = PendingIntent.getActivity(this, 0, appStartIntent, 0);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setContentTitle(getString(R.string.tracking_active_title)).setContentText(getString(R.string.tracking_active_text));
        } else {
            notificationBuilder.setContentTitle(getString(R.string.tracking_active_title_short))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.tracking_long_text)));
        }
        Intent exitIntent = new Intent(this, Main.class);
        exitIntent.setAction(Main.ACTION_EXIT);
        PendingIntent pendingExitIntent = PendingIntent.getActivity(this, 0, exitIntent, 0);
        notificationBuilder.setSmallIcon(R.drawable.logo_simplified).setOngoing(true).setUsesChronometer(true).setContentIntent(pendingAppIntent)
                .setColor(ContextCompat.getColor(this, R.color.osm_green))
                .addAction(R.drawable.logo_simplified, getString(R.string.exit_title), pendingExitIntent);
        startForeground(R.id.notification_tracker, notificationBuilder.build());
    }

    /**
     * Stops tracking
     * 
     * @param deleteTrack true if the track should be deleted, false if it should be kept
     */
    public void stopTracking(boolean deleteTrack) {
        Log.d(DEBUG_TAG, "Stop tracking");
        if (!tracking) {
            if (deleteTrack) {
                track.reset();
            }
            return;
        }
        if (deleteTrack) {
            track.reset();
        } else {
            track.save();
        }
        tracking = false;
        stop();
    }

    /**
     * Stops autodownloading OSM data
     */
    public void stopAutoDownload() {
        Log.d(DEBUG_TAG, "Stop auto-download");
        downloading = false;
        stop();
    }

    /**
     * Stops autodownloading bugs
     */
    public void stopBugAutoDownload() {
        Log.d(DEBUG_TAG, "Stop auto-download");
        downloadingBugs = false;
        stop();
    }

    /**
     * Halt the service if we are not doing anything important aka we are not recording a track or autodownloading
     * something
     */
    private void stop() {
        if (!tracking && !downloading && !downloadingBugs) {
            Log.d(DEBUG_TAG, "Stopping auto-service");
            updateGPSState();
            stopForeground(true);
            stopSelf();
        }
        if (externalListener != null) {
            externalListener.onStateChanged();
        }
    }

    /**
     * Get the tracking status
     * 
     * @return true if we are tracking
     */
    public boolean isTracking() {
        return tracking;
    }

    /**
     * Get the list of recorded TrackPoints
     * 
     * @return a List of TrackPoint
     */
    public List<TrackPoint> getTrackPoints() {
        return track.getTrackPoints();
    }

    /**
     * Get the list of WayPoints
     * 
     * @return a List of WayPoint
     */
    public List<WayPoint> getWayPoints() {
        return Arrays.asList(track.getWayPoints());
    }

    /**
     * Check if we've stored any GPX elements
     * 
     * @return true is we have a track or way point stored
     */
    public boolean isEmpty() {
        return track == null || track.isEmpty();
    }

    /**
     * Check if we have any TrackPoints
     * 
     * @return true is TrackPoints are stored
     */
    public boolean hasTrackPoints() {
        return track != null && track.getTrackPoints() != null && !track.getTrackPoints().isEmpty();
    }

    /**
     * Check if we have any WayPoints
     * 
     * @return true is WayPoints are stored
     */
    public boolean hasWayPoints() {
        return track != null && track.getWayPoints() != null && track.getWayPoints().length > 0;
    }

    /**
     * Exports the GPX data
     */
    @Override
    public void export(OutputStream outputStream) throws Exception {
        track.exportToGPX(outputStream);
    }

    @Override
    public String exportExtension() {
        return "gpx";
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class TrackerBinder extends Binder {

        /**
         * Get this instance of the TrackerService
         * 
         * @return this instance of the TrackerService
         */
        public TrackerService getService() {
            return TrackerService.this;
        }
    }

    LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (source != GpsSource.INTERNAL) {
                return; // ignore updates from device
            }

            // Only use GPS provided locations for generating tracks
            if (tracking && (!location.hasAccuracy() || location.getAccuracy() <= TRACK_LOCATION_MIN_ACCURACY)) {
                track.addTrackPoint(location);
            }
            if (lastLocation != null && LocationManager.NETWORK_PROVIDER.equals(lastLocation.getProvider())) {
                Snack.toastTopInfo(TrackerService.this, R.string.toast_using_gps_location);
            }
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // unused
        }

        @Override
        public void onProviderEnabled(String provider) {
            // unused
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (tracking) {
                stopTracking(false);
                Snack.toastTopInfo(TrackerService.this, R.string.toast_using_gps_disabled_tracking_stopped);
            }
        }
    };

    @SuppressWarnings("NewApi")
    LocationListener networkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (source != GpsSource.INTERNAL) {
                return; // ignore updates from device
            }
            if (lastLocation != null) {
                boolean lastIsGpsLocation = LocationManager.GPS_PROVIDER.equals(lastLocation.getProvider());
                if (lastIsGpsLocation) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                        if (location.getElapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos() < staleGPSNano) {
                            return; // ignore - last GPS time is still reasonably current
                        }
                    } else {
                        if (location.getTime() - lastLocation.getTime() < staleGPSMilli) {
                            return; // this is not as reliable as the above but likely still OK
                        }
                    }
                    Snack.toastTopInfo(TrackerService.this, R.string.toast_using_network_location);
                }
            }
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // unused
        }

        @Override
        public void onProviderEnabled(String provider) {
            // unused
        }

        @Override
        public void onProviderDisabled(String provider) {
            // unused
        }
    };

    public interface TrackerLocationListener {
        /**
         * Call this when the Location has been updated
         * 
         * @param location the update Location
         */
        void onLocationChanged(@NonNull Location location);

        /**
         * Call on state change
         */
        void onStateChanged();
    }

    /**
     * If required initialize the Location sources and start updating
     */
    @SuppressWarnings("deprecation")
    private void updateGPSState() {
        boolean needed = listenerNeedsGPS || tracking || downloading || downloadingBugs;
        if (needed && !gpsEnabled) {
            Log.d(DEBUG_TAG, "Enabling GPS updates");
            prefs = new Preferences(this);
            nmeaLocation.removeSpeed(); // be sure that these are not set
            nmeaLocation.removeBearing();
            try {
                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (last != null) {
                        networkListener.onLocationChanged(last);
                    }
                } else if (last != null) {
                    gpsListener.onLocationChanged(last);
                }
            } catch (SecurityException | IllegalArgumentException ex) {
                // Ignore
            }
            try {
                // used to pass updates to UI thread
                mHandler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message inputMessage) {
                        switch (inputMessage.what) {
                        case LOCATION_UPDATE:
                            Location l = (Location) inputMessage.obj;
                            gpsListener.onLocationChanged(l);
                            break;
                        case CONNECTION_FAILED:
                            Snack.toastTopError(TrackerService.this, (String) inputMessage.obj);
                            break;
                        default:
                            // ignore
                        }
                    }
                };
                boolean useTcpClient = prefs.getGpsSource().equals(prefTcpClient);
                if (useTcpClient || prefs.getGpsSource().equals(prefTcpServer)) {
                    source = GpsSource.TCP;
                    if (useTcpClient && tcpClient == null) {
                        tcpClient = new NmeaTcpClient(prefs.getGpsTcpSource(), oldNmeaListener, mHandler);
                        Thread t = new Thread(null, tcpClient, "TcpClient");
                        t.start();
                    } else if (tcpServer == null) {
                        tcpServer = new NmeaTcpClientServer(prefs.getGpsTcpSource(), oldNmeaListener, mHandler);
                        Thread t = new Thread(null, tcpServer, "TcpClientServer");
                        t.start();
                    }
                } else {
                    boolean useNema = prefs.getGpsSource().equals(prefNmea);
                    if (useNema || prefs.getGpsSource().equals(prefInternal)) {
                        source = GpsSource.INTERNAL;
                        staleGPSMilli = prefs.getGpsInterval() * 20L; // 20 times the intended interval
                        staleGPSNano = staleGPSMilli * 1000; // convert to nanoseconds
                        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) { // just
                            // internal NMEA resource only works if normal updates are turned on
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefs.getGpsInterval(), prefs.getGpsDistance(), gpsListener);
                            if (useNema) {
                                source = GpsSource.NMEA;
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                    locationManager.addNmeaListener(oldNmeaListener); // NOSONAR
                                } else {
                                    locationManager.addNmeaListener(newNmeaListener);
                                }
                            }
                        }
                        if (locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null && prefs.isNetworkLocationFallbackAllowed()) {
                            // if the network provider is available listen there
                            Log.d(DEBUG_TAG, "Listening for NETWORK_PROVIDER");
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, prefs.getGpsInterval(), prefs.getGpsDistance(),
                                    networkListener);
                        }
                    }
                }
                gpsEnabled = true;
            } catch (SecurityException sex) {
                // note there is no way we can ask for permission here so we do
                // that in the main
                // activity before actually creating this service
                Log.e(DEBUG_TAG, "Permission missing for location service ", sex);
                Snack.toastTopError(this, R.string.gps_failure);
            } catch (RuntimeException rex) {
                Log.e(DEBUG_TAG, "Failed to enable location service", rex);
                Snack.toastTopError(this, R.string.gps_failure);
            }
        } else if (!needed && gpsEnabled) {
            Log.d(DEBUG_TAG, "Disabling GPS updates");
            try {
                locationManager.removeUpdates(gpsListener);
            } catch (SecurityException sex) {
                // can be safely ignored
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                locationManager.removeNmeaListener(oldNmeaListener); // NOSONAR
            } else {
                locationManager.removeNmeaListener(newNmeaListener);
            }
            gpsEnabled = false;
        }
    }

    /**
     * If true the listener wants to receive Location updates
     * 
     * @param listenerNeedsGPS true if the listener wants to receive Location updates
     */
    public void setListenerNeedsGPS(boolean listenerNeedsGPS) {
        this.listenerNeedsGPS = listenerNeedsGPS;
        updateGPSState();
        if (listenerNeedsGPS && externalListener != null && lastLocation != null) {
            externalListener.onLocationChanged(lastLocation);
        }
    }

    /**
     * Set the listener for Location updates
     * 
     * @param listener the listener
     */
    public void setListener(@Nullable TrackerLocationListener listener) {
        if (listener == null) {
            setListenerNeedsGPS(false);
        }
        externalListener = listener;
    }

    /**
     * Read a file in GPX format from device
     * 
     * @param activity activity this was called from, if null no messages will be displayed, and menus will not be
     *            updated
     * @param uri Uri for the file to read
     * @throws FileNotFoundException if we couldn't locate the file
     */
    public void importGPXFile(@Nullable final FragmentActivity activity, final Uri uri) throws FileNotFoundException {

        new AsyncTask<Void, Void, Integer>() {

            static final int FILENOTFOUND = -1;
            static final int OK           = 0;

            @Override
            protected void onPreExecute() {
                if (activity != null) {
                    Progress.showDialog(activity, Progress.PROGRESS_LOADING);
                }
            }

            @Override
            protected Integer doInBackground(Void... arg) {
                int result = OK;
                try (InputStream is = getContentResolver().openInputStream(uri); BufferedInputStream in = new BufferedInputStream(is)) {
                    track.importFromGPX(in);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Error reading file: ", e);
                    result = FILENOTFOUND;
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                try {
                    if (activity != null) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_LOADING);
                        int trackPointCount = track.getTrackPoints() != null ? track.getTrackPoints().size() : 0;
                        int wayPointCount = track.getWayPoints() != null ? track.getWayPoints().length : 0;
                        String message = activity.getResources().getQuantityString(R.plurals.toast_imported_track_points, wayPointCount, trackPointCount,
                                wayPointCount);
                        Snack.barInfo(activity, message);
                        activity.supportInvalidateOptionsMenu();
                        if (result == FILENOTFOUND) {
                            Snack.barError(activity, R.string.toast_file_not_found);
                        }
                    }
                } catch (IllegalStateException e) {
                    // Avoid crash if activity is paused
                    Log.e(DEBUG_TAG, "onPostExecute", e);
                }
            }

        }.execute();
    }

    /**
     * Get the Track object that is currently being used for recording
     * 
     * @return the Track object held by the TrackerService
     */
    public Track getTrack() {
        return track;
    }

    /**
     * Try to update our position from NMEA input using some heuristics
     * 
     * @param sentance
     */
    enum GnssSystem {
        NONE, // pre-fix
        BEIDOU, GLONASS, GPS, GALILEO, MULTIPLE
    }

    private GnssSystem system = GnssSystem.NONE;

    /**
     * Minimal parsing of two NMEA 0183 sentences TODO add fix type filtering TODO do something with the hdop value
     * 
     * @param sentence the NMEA sentence including checksum
     */
    private void processNmeaSentance(String sentence) {
        boolean posUpdate = false;
        try {
            if (sentence.length() > 9) { // everything shorter is invalid
                int star = sentence.indexOf('*');
                if (star > 5 && sentence.length() >= star + 3) {
                    String withoutChecksum = sentence.substring(1, star);
                    int receivedChecksum = Integer.parseInt(sentence.substring(star + 1, star + 3), 16);
                    int checksum = 0;
                    for (byte b : withoutChecksum.getBytes()) {
                        checksum = checksum ^ b;
                    }
                    if (receivedChecksum == checksum) {
                        String talker = withoutChecksum.substring(0, 2);
                        String s = withoutChecksum.substring(2, 5);

                        double lat = Double.NaN;
                        double lon = Double.NaN;
                        // double hdop = Double.NaN; currently unused
                        double height = Double.NaN;
                        try {
                            if (s.equals("GNS")) {
                                String[] values = withoutChecksum.split(",", -12); // java
                                                                                   // magic
                                if (values.length == 13) {
                                    String value6 = values[6].toUpperCase(Locale.US);
                                    if ((!value6.startsWith("NN") || !value6.equals("N")) && Integer.parseInt(values[7]) >= 4) {
                                        // at least one "good" system needs a
                                        // fix
                                        lat = nmeaLatToDecimal(values[2]) * (values[3].equalsIgnoreCase("N") ? 1 : -1);
                                        lon = nmeaLonToDecimal(values[4]) * (values[5].equalsIgnoreCase("E") ? 1 : -1);
                                        // hdop = Double.parseDouble(values[8]);
                                        height = Double.parseDouble(values[9]);
                                        posUpdate = true;
                                    }
                                } else {
                                    throw new UnsupportedFormatException(Integer.toString(values.length));
                                }
                            } else if (s.equals("GGA")) {
                                String[] values = withoutChecksum.split(",", -14); // java
                                                                                   // magic
                                if (values.length == 15) {
                                    // we need a fix
                                    if (!values[6].equals("0") && Integer.parseInt(values[7]) >= 4) {
                                        lat = nmeaLatToDecimal(values[2]) * (values[3].equalsIgnoreCase("N") ? 1 : -1);
                                        lon = nmeaLonToDecimal(values[4]) * (values[5].equalsIgnoreCase("E") ? 1 : -1);
                                        // hdop = Double.parseDouble(values[8]);
                                        height = Double.parseDouble(values[9]);
                                        posUpdate = true;
                                    }
                                } else {
                                    throw new UnsupportedFormatException(Integer.toString(values.length));
                                }
                            } else if (s.equals("VTG")) {
                                String[] values = withoutChecksum.split(",", -11); // java
                                                                                   // magic
                                if (values.length == 12) {
                                    if (!values[9].toUpperCase(Locale.US).startsWith("N")) {
                                        double course = Double.parseDouble(values[1]);
                                        nmeaLocation.setBearing((float) course);
                                        double speed = Double.parseDouble(values[7]);
                                        nmeaLocation.setSpeed((float) (speed / 3.6D));
                                    }
                                } else {
                                    throw new UnsupportedFormatException(Integer.toString(values.length));
                                }
                            } else {
                                // unsupported sentence
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Log.d(DEBUG_TAG, "Invalid number format in " + sentence);
                            return;
                        } catch (UnsupportedFormatException e) {
                            Log.d(DEBUG_TAG, "Invalid number " + e.getMessage() + " of values in " + sentence);
                            return;
                        }
                        // the following assumes that the behaviour of the GPS
                        // receiver will not change
                        // and that multiple systems are better than GPS which
                        // in turn is better than GLONASS ... this
                        // naturally may not be really true
                        if (system == GnssSystem.NONE) { // take whatever we get
                            switch (talker) {
                            case "GP":
                                system = GnssSystem.GPS;
                                break;
                            case "GL":
                                system = GnssSystem.GLONASS;
                                break;
                            case "GN":
                                system = GnssSystem.MULTIPLE;
                                break;
                            case "BD": // Beidou
                            case "GA": // Galileo
                            default:
                                // new system we don't know about? BEIDOU
                                // probably best ignored for now
                                return;
                            }
                        } else if (system == GnssSystem.GLONASS) {
                            if (talker.equals("GP")) {
                                system = GnssSystem.GPS;
                            } else if (talker.equals("GN")) {
                                system = GnssSystem.MULTIPLE;
                            }
                        } else if (system == GnssSystem.GPS) {
                            if (talker.equals("GL")) {
                                // ignore
                                return;
                            } else if (talker.equals("GN")) {
                                system = GnssSystem.MULTIPLE;
                            }
                        } else if (system == GnssSystem.MULTIPLE) {
                            if (!talker.equals("GN")) {
                                return;
                            }
                        }

                        if (posUpdate) { // we could do filtering etc here
                            nmeaLocation.setAltitude(height);
                            nmeaLocation.setLatitude(lat);
                            nmeaLocation.setLongitude(lon);
                            // can't call something on the UI thread directly
                            // need to send a message
                            Message newLocation = mHandler.obtainMessage(LOCATION_UPDATE, nmeaLocation);
                            newLocation.sendToTarget();
                            if (tracking) {
                                track.addTrackPoint(nmeaLocation);
                            }
                            autoLoadDataAndBugs(new Location(nmeaLocation));
                            lastLocation = nmeaLocation;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "NMEA sentance " + sentence + " caused exception " + e);
            ACRAHelper.nocrashReport(e, e.getMessage());
        }
    }

    /**
     * Convert from NMEA format to decimal (there is already a method in Location so this is not really necessary)
     * 
     * @param nmea longitude value
     * @return the longitude
     * @throws NumberFormatException if the value can't be parsed
     */
    private Double nmeaLonToDecimal(@NonNull String nmea) throws NumberFormatException {
        int deg = Integer.parseInt(nmea.substring(0, 3));
        Double min = Double.parseDouble(nmea.substring(3));
        return deg + min / 60d;
    }

    /**
     * Convert from NMEA format to decimal (there is already a method in Location so this is not really necessary)
     * 
     * @param nmea latitude value
     * @return the latitude
     * @throws NumberFormatException if the value can't be parsed
     */
    private Double nmeaLatToDecimal(@NonNull String nmea) throws NumberFormatException {
        int deg = Integer.parseInt(nmea.substring(0, 2));
        Double min = Double.parseDouble(nmea.substring(2));
        return deg + min / 60d;
    }

    /**
     * Tiled based download of OSM data
     * 
     * This downloads a tile, or the the missing bits of (2*download radius)^2 size when we are in it or near it, the
     * origin of the tiles is where ever we started off at.
     * 
     * @param location the current Location
     * @param validator a Validator to use for any new data
     */
    private void autoDownload(@NonNull Location location, @NonNull Validator validator) {
        // some heuristics for now to keep downloading to a minimum
        int radius = prefs.getDownloadRadius();
        if ((location.getSpeed() < prefs.getMaxDownloadSpeed() / 3.6f) && (previousLocation == null || location.distanceTo(previousLocation) > radius / 8)) {
            StorageDelegator storageDelegator = App.getDelegator();
            List<BoundingBox> bbList = new ArrayList<>(storageDelegator.getBoundingBoxes());
            BoundingBox newBox = getNextBox(bbList, previousLocation, location, radius);
            if (newBox != null) {
                if (radius != 0) { // download
                    List<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, newBox);
                    for (BoundingBox b : bboxes) {
                        if (b.getWidth() <= 1 || b.getHeight() <= 1) {
                            // ignore super small bb likely due to rounding
                            // errors
                            Log.d(DEBUG_TAG, "getNextCenter very small bb " + b.toString());
                            continue;
                        }
                        storageDelegator.addBoundingBox(b); // will be filled
                                                            // once download is
                                                            // complete
                        Log.d(DEBUG_TAG, "getNextCenter loading " + b.toString());
                        App.getLogic().autoDownloadBox(this, prefs.getServer(), validator, b);
                    }
                }
                previousLocation = location;
            }
        }
    }

    /**
     * Check if the supplied coordinates are in one of the BoundingBoxes
     * 
     * @param bbs a List of BoundingBox
     * @param lonE7 longitude in WGS84*10E7
     * @param latE7 latitude in WGS84*10E7
     * @return true if one of the BoundingBoxes cover the coordinate
     */
    private boolean bbLoaded(@NonNull List<BoundingBox> bbs, int lonE7, int latE7) {
        for (BoundingBox b : bbs) {
            if (b.isIn(lonE7, latE7)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a suitable next bounding box, simply creates a raster of the download radius size
     * 
     * @param bbs List of already used/downloaded BoundingBox
     * @param prevLocation previous Location (unused)
     * @param location current Location
     * @param radius "radius" of the BoundingBox
     * @return the next BoundingBox
     */
    @Nullable
    private BoundingBox getNextBox(@NonNull List<BoundingBox> bbs, Location prevLocation, @NonNull Location location, int radius) {
        double lon = location.getLongitude();
        double lat = location.getLatitude();
        double mlat = GeoMath.latToMercator(lat);
        double width = 2 * GeoMath.convertMetersToGeoDistance(radius);

        int currentLeftE7 = (int) (Math.floor(lon / width) * width * 1E7);
        double currentMBottom = Math.floor(mlat / width) * width;
        int currentBottomE7 = (int) (GeoMath.mercatorToLat(currentMBottom) * 1E7);
        int widthE7 = (int) (width * 1E7);

        BoundingBox b = new BoundingBox(currentLeftE7, currentBottomE7, currentLeftE7 + widthE7, currentBottomE7 + widthE7);

        if (!bbLoaded(bbs, (int) (lon * 1E7D), (int) (lat * 1E7D))) {
            return b;
        }

        double bRight = b.getRight() / 1E7d;
        double bLeft = b.getLeft() / 1E7d;
        double mBottom = GeoMath.latE7ToMercator(b.getBottom());
        double mHeight = GeoMath.latE7ToMercator(b.getTop()) - mBottom;
        double dLeft = lon - bLeft;
        double dRight = bRight - lon;

        double dTop = mBottom + mHeight - mlat;
        double dBottom = mlat - mBottom;

        Log.d(DEBUG_TAG, "getNextCenter dLeft " + dLeft + " dRight " + dRight + " dTop " + dTop + " dBottom " + dBottom);
        Log.d(DEBUG_TAG, "getNextCenter " + b.toString());

        BoundingBox result;
        // top or bottom is closest
        if (dTop < dBottom) { // top closest
            if (dLeft < dRight) {
                result = new BoundingBox(b.getLeft() - widthE7, b.getBottom(), b.getRight(), b.getTop() + widthE7);
            } else {
                result = new BoundingBox(b.getLeft(), b.getBottom(), b.getRight() + widthE7, b.getTop() + widthE7);
            }
        } else {
            if (dLeft < dRight) {
                result = new BoundingBox(b.getLeft() - widthE7, b.getBottom() - widthE7, b.getRight(), b.getTop());
            } else {
                result = new BoundingBox(b.getLeft(), b.getBottom() - widthE7, b.getRight() + widthE7, b.getTop());
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Tiled based download of task data
     * 
     * This downloads a tile, or the the missing bits of (2*download radius)^2 size when we are in it or near it, the
     * origin of the tiles is where ever we started off at.
     * 
     * @param location the current Location
     */
    private void bugAutoDownload(@NonNull Location location) {
        // some heuristics for now to keep downloading to a minimum
        int radius = prefs.getBugDownloadRadius();
        if ((location.getSpeed() < prefs.getMaxBugDownloadSpeed() / 3.6f)
                && (previousBugLocation == null || location.distanceTo(previousBugLocation) > radius / 8)) {
            ArrayList<BoundingBox> bbList = new ArrayList<>(App.getTaskStorage().getBoundingBoxes());
            BoundingBox newBox = getNextBox(bbList, previousBugLocation, location, radius);
            if (newBox != null) {
                if (radius != 0) { // download
                    List<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, newBox);
                    for (BoundingBox b : bboxes) {
                        if (b.getWidth() <= 1 || b.getHeight() <= 1) {
                            // ignore super small bb likely due to rounding
                            // errors
                            Log.d(DEBUG_TAG, "bugAutoDownload very small bb " + b.toString());
                            continue;
                        }
                        App.getTaskStorage().add(b); // will be filled once
                                                     // download is complete
                        Log.d(DEBUG_TAG, "bugAutoDownloads loading " + b.toString());
                        TransferTasks.downloadBox(this, prefs.getServer(), b, true, null);
                    }
                }
                previousBugLocation = location;
            } else {
                Log.d(DEBUG_TAG, "bugAutoDownload no bb");
            }
        }
    }

    /**
     * Get the last Location we processed
     * 
     * @return the last Location
     */
    @Nullable
    public Location getLastLocation() {
        return lastLocation;
    }

    /**
     * Call all Listeners and download data and tasks
     * 
     * @param location current Location
     */
    public void updateLocation(@Nullable Location location) {
        if (location == null) {
            return;
        }
        autoLoadDataAndBugs(location);
        if (externalListener != null) {
            externalListener.onLocationChanged(location);
        }
        lastLocation = location;
    }

    /**
     * Call the download/load methods for both data and bugs for the specified location
     * 
     * @param location the Location instance
     */
    private void autoLoadDataAndBugs(@NonNull Location location) {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        // only attempt to download if we have a network or a mapsplit source
        boolean activeNetwork = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        if (prefs.getServer().hasMapSplitSource() || activeNetwork) {
            if (downloading) {
                autoDownload(location, validator);
            }
        }
        if (activeNetwork) {
            if (downloadingBugs) {
                bugAutoDownload(location);
            }
        }
    }

    @SuppressWarnings("deprecation")
    class OldNmeaListener implements NmeaListener { // NOSONAR
        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            processNmeaSentance(nmea);
        }
    }

    @SuppressWarnings("NewApi")
    class NewNmeaListener implements OnNmeaMessageListener {
        @Override
        public void onNmeaMessage(String message, long timestamp) {
            processNmeaSentance(message);
        }
    }
}
