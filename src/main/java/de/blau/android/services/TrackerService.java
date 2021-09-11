package de.blau.android.services;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.gpx.Track;
import de.blau.android.gpx.TrackPoint;
import de.blau.android.gpx.WayPoint;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.ExtendedLocation;
import de.blau.android.services.util.Nmea;
import de.blau.android.services.util.NmeaTcpClient;
import de.blau.android.services.util.NmeaTcpClientServer;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Notifications;
import de.blau.android.util.SavingHelper.Exportable;
import de.blau.android.util.Snack;
import de.blau.android.util.egm96.EGM96;
import de.blau.android.validation.Validator;

public class TrackerService extends Service implements Exportable {

    private static final String DEBUG_TAG = "TrackerService";

    private static final float TRACK_LOCATION_MIN_ACCURACY = 200f;

    private static final int LOCATION_UPDATE    = 0;
    public static final int  CONNECTION_FAILED  = 1;
    public static final int  CONNECTION_MESSAGE = 2;
    public static final int  CONNECTION_CLOSED  = 3;

    private static final String TRACK_KEY            = "track";
    private static final String AUTODOWNLOAD_KEY     = "autodownload";
    private static final String BUGAUTODOWNLOAD_KEY  = "bugautodownload";
    public static final String  CALIBRATE_KEY        = "calibrate";
    public static final String  CALIBRATE_HEIGHT_KEY = "height";
    public static final String  CALIBRATE_P0_KEY     = "p0";

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

    private NmeaTcpClient       tcpClient = null;
    private NmeaTcpClientServer tcpServer = null;

    private enum GpsSource {
        INTERNAL, NMEA, TCP
    }

    private GpsSource source = GpsSource.INTERNAL;

    private String prefInternal;
    private String prefNmea;
    private String prefTcpClient;
    private String prefTcpServer;

    private ExtendedLocation nmeaLocation;

    private ConnectivityManager connectivityManager;

    private Validator validator;

    Handler handler = new Handler(Looper.getMainLooper());

    /**
     * For no apparent sane reason google has deprecated the NmeaListener interface
     */
    OldNmeaListener oldNmeaListener = null;
    NewNmeaListener newNmeaListener = null;
    private boolean useOldNmea      = false;

    private long staleGPSMilli = 20000L;              // 20 seconds
    private long staleGPSNano  = staleGPSMilli * 1000;

    private Method addNmeaListener    = null;
    private Method removeNmeaListener = null;

    private SensorManager       sensorManager;
    private PressureListener    pressureListener;
    private boolean             useBarometricHeight = false;
    private EGM96               egm;
    private TemperatureListener temperatureListener;
    private boolean             egmLoaded           = false;

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
        nmeaLocation = new ExtendedLocation(prefNmea);
        useOldNmea = Build.VERSION.SDK_INT < Build.VERSION_CODES.N;

        if (useOldNmea) {
            oldNmeaListener = new OldNmeaListener();
        } else {
            newNmeaListener = new NewNmeaListener();
        }

        // see https://issuetracker.google.com/issues/141019880
        try {
            // noinspection JavaReflectionMemberAccess
            addNmeaListener = LocationManager.class.getMethod("addNmeaListener", GpsStatus.NmeaListener.class);
            removeNmeaListener = LocationManager.class.getMethod("removeNmeaListener", GpsStatus.NmeaListener.class);
        } catch (Exception e) { // NOSONAR
            Log.e(DEBUG_TAG, "reflection didn't find addNmeaListener or removeNmeaListener " + e.getMessage());
        }

        // pressure
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (prefs.useBarometricHeight() && pressure != null) {
            pressureListener = new PressureListener();
            sensorManager.registerListener(pressureListener, pressure, 1000);
            Sensor temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            if (temperature != null) {
                temperatureListener = new TemperatureListener();
                sensorManager.registerListener(temperatureListener, temperature, 1000);
            }
        }
        Uri egmFile = prefs.getEgmFile();
        if (egmFile != null) {
            try {
                egm = new EGM96(egmFile.getPath());
                egmLoaded = true;
            } catch (IOException ioex) {
                Log.e(DEBUG_TAG, "Error loading EGM " + ioex.getMessage());
                Snack.toastTopInfo(this, "Error loading EGM " + ioex.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        stopTracking(false);
        track.close();
        cancelNmeaClients();
        if (pressureListener != null) {
            sensorManager.unregisterListener(pressureListener);
        }
        if (temperatureListener != null) {
            sensorManager.unregisterListener(temperatureListener);
        }
        super.onDestroy();
    }

    /**
     * Start one of the NMEA clients
     * 
     * @param useTcpClient start the tcp reader
     * @param useTcpServer start the tcp server
     */
    private void startNmeaClients(boolean useTcpClient, boolean useTcpServer) {
        Log.d(DEBUG_TAG, "Starting Nmea Clients");
        source = GpsSource.TCP;
        if (useTcpClient && tcpClient == null) {
            if (useOldNmea) {
                tcpClient = new NmeaTcpClient(prefs.getGpsTcpSource(), oldNmeaListener, mHandler);
            } else {
                tcpClient = new NmeaTcpClient(prefs.getGpsTcpSource(), newNmeaListener, mHandler);
            }
            Thread t = new Thread(null, tcpClient, "TcpClient");
            t.start();
        } else if (useTcpServer && tcpServer == null) {
            if (useOldNmea) {
                tcpServer = new NmeaTcpClientServer(prefs.getGpsTcpSource(), oldNmeaListener, mHandler);
            } else {
                tcpServer = new NmeaTcpClientServer(prefs.getGpsTcpSource(), newNmeaListener, mHandler);
            }
            Thread t = new Thread(null, tcpServer, "TcpClientServer");
            t.start();
        }
    }

    /**
     * Stop the NMEA clients from running
     */
    private void cancelNmeaClients() {
        Log.d(DEBUG_TAG, "Canceling Nmea Clients");
        if (tcpClient != null) {
            tcpClient.cancel();
            tcpClient = null;
            gpsEnabled = false;
        }
        if (tcpServer != null) {
            tcpServer.cancel();
            tcpServer = null;
            gpsEnabled = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(DEBUG_TAG, "Received null intent"); //
            return START_STICKY; // NOTE not clear how or if we should return an error here
        }
        if (intent.getBooleanExtra(TRACK_KEY, false)) {
            Log.d(DEBUG_TAG, "Start tracking");
            startTrackingInternal();
        } else if (intent.getBooleanExtra(AUTODOWNLOAD_KEY, false)) {
            Log.d(DEBUG_TAG, "Start autodownload");
            startAutoDownloadInternal();
        } else if (intent.getBooleanExtra(BUGAUTODOWNLOAD_KEY, false)) {
            Log.d(DEBUG_TAG, "Start task autodownload");
            startBugAutoDownloadInternal();
        } else if (intent.getBooleanExtra(CALIBRATE_KEY, false)) {
            Log.d(DEBUG_TAG, "Calibrate height");
            if (pressureListener != null) {
                int height = intent.getIntExtra(CALIBRATE_HEIGHT_KEY, Integer.MIN_VALUE);
                if (height != Integer.MIN_VALUE) {
                    pressureListener.calibrate(height);
                } else {
                    float p0 = intent.getFloatExtra(CALIBRATE_P0_KEY, 0);
                    if (p0 != 0) {
                        pressureListener.setP0(p0);
                    } else if (lastLocation != null) { // calibrate from GPS
                        if (lastLocation instanceof ExtendedLocation && ((ExtendedLocation) lastLocation).hasGeoidHeight()) {
                            pressureListener.calibrate((float) ((ExtendedLocation) lastLocation).getGeoidHeight());
                        } else if (lastLocation.hasAltitude()) {
                            double offset = getGeoidOffset(lastLocation.getLongitude(), lastLocation.getLatitude());
                            Log.d(DEBUG_TAG, "Geoid offset " + offset);
                            pressureListener.calibrate((float) (lastLocation.getAltitude() - offset));
                        }
                    }
                }
                Snack.toastTopInfo(this, "New height " + pressureListener.barometricHeight + "m\nCurrent pressure " + pressureListener.millibarsOfPressure
                        + " hPa\nReference pressure " + pressureListener.pressureAtSeaLevel + " hPa");
            } else {
                Log.e(DEBUG_TAG, "Calibration attemped but no pressure listener");
            }
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
        Log.d(DEBUG_TAG, "Start tracking");
        if (startInternal()) {
            tracking = true;
            track.markNewSegment();
        }
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startAutoDownloadInternal() {
        Log.d(DEBUG_TAG, "Start auto download");
        if (startInternal()) {
            downloading = true;
        }
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startBugAutoDownloadInternal() {
        Log.d(DEBUG_TAG, "Start bug auto download");
        if (startInternal()) {
            downloadingBugs = true;
        }
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * 
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     * 
     * @return true if tracking could be started
     */
    private boolean startInternal() {
        if (tracking || downloading || downloadingBugs) {
            return true; // already running
        }
        if (!Notifications.channelEnabled(this, Notifications.DEFAULT_CHANNEL)) {
            Snack.toastTopError(TrackerService.this, R.string.toast_default_channel_needs_to_be_enabled);
            return false;
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
        init();
        if (externalListener != null) {
            externalListener.onStateChanged();
        }
        return true;
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
            Log.d(DEBUG_TAG, "Stopping service");
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
    @NonNull
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
        return FileExtensions.GPX;
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
            if (source == GpsSource.INTERNAL) {
                location = new ExtendedLocation(location);
                ExtendedLocation loc = ((ExtendedLocation) location);
                if (egmLoaded) {
                    double offset = getGeoidOffset(location.getLongitude(), location.getLatitude());
                    loc.setGeoidCorrection(offset);
                    if (loc.hasAltitude()) {
                        loc.setGeoidHeight(loc.getAltitude() - offset);
                    }
                }
                if (pressureListener != null && pressureListener.barometricHeight != 0) {
                    if (useBarometricHeight) {
                        loc.setUseBarometricHeight();
                    }
                    loc.setBarometricHeight(pressureListener.barometricHeight);
                }

                // Only use GPS provided locations for generating tracks
                if (tracking && (!location.hasAccuracy() || location.getAccuracy() <= TRACK_LOCATION_MIN_ACCURACY)) {
                    track.addTrackPoint(location);
                }
                if (lastLocation != null && LocationManager.NETWORK_PROVIDER.equals(lastLocation.getProvider())) {
                    Snack.toastTopInfo(TrackerService.this, R.string.toast_using_gps_location);
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
            if (tracking) {
                Snack.toastTopInfo(TrackerService.this, R.string.toast_using_gps_disabled_tracking_stopped);
            }
        }
    };

    @SuppressWarnings("NewApi")
    LocationListener networkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (source != GpsSource.INTERNAL) {
                return; // ignore updates
            }
            if (lastLocation != null) {
                boolean lastIsGpsLocation = LocationManager.GPS_PROVIDER.equals(lastLocation.getProvider());
                if (lastIsGpsLocation) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                        if (location.getElapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos() < staleGPSNano) {
                            // ignore - last GPS time is still reasonably current
                            return;
                        }
                    } else {
                        if (location.getTime() - lastLocation.getTime() < staleGPSMilli) {
                            return; // this is not as reliable as the above
                                    // but likely still OK
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
     * Used to pass messages to the UI thread
     *
     */
    class MessageHandler extends Handler {

        /**
         * Construct a new Handler
         */
        MessageHandler() {
            super(Looper.getMainLooper());
        }

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
            case CONNECTION_MESSAGE:
                Snack.toastTopInfo(TrackerService.this, getString(R.string.toast_remote_nmea_connection, (String) inputMessage.obj));
                break;
            case CONNECTION_CLOSED:
                Snack.toastTopInfo(TrackerService.this, R.string.toast_remote_nmea_connection_closed);
                break;
            default:
                // ignore
            }
        }
    }

    /**
     * If required, initialize the Location sources and start updating, also check for source configuration changes
     */
    @SuppressWarnings("deprecation")
    @TargetApi(24)
    private void init() {
        prefs = new Preferences(this);
        String gpsSource = prefs.getGpsSource();
        final boolean useTcpClient = gpsSource.equals(prefTcpClient);
        final boolean useTcpServer = gpsSource.equals(prefTcpServer);
        final boolean useTcp = useTcpClient || useTcpServer;

        useBarometricHeight = pressureListener != null && prefs.useBarometricHeight();

        boolean needed = listenerNeedsGPS || tracking || downloading || downloadingBugs;

        // update configuration
        if ((needed && !gpsEnabled) || (gpsEnabled && (useTcp && source != GpsSource.TCP) || (!useTcp && source == GpsSource.TCP))) {
            Log.d(DEBUG_TAG, "Enabling GPS updates");
            nmeaLocation.removeSpeed(); // NOSONAR be sure that these are not set
            nmeaLocation.removeBearing(); // NOSONAR
            Nmea.reset();
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
                mHandler = new MessageHandler();
                if (useTcp) {
                    startNmeaClients(useTcpClient, useTcpServer);
                } else {
                    cancelNmeaClients(); // not needed any more
                    boolean useNema = gpsSource.equals(prefNmea);
                    if (useNema || gpsSource.equals(prefInternal)) {
                        source = GpsSource.INTERNAL;
                        staleGPSMilli = prefs.getGpsInterval() * 20L; // 20 times the intended interval
                        staleGPSNano = staleGPSMilli * 1000; // convert to nanoseconds
                        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
                            // internal NMEA resource only works if normal updates are turned on
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefs.getGpsInterval(), prefs.getGpsDistance(), gpsListener);
                            if (useNema) {
                                source = GpsSource.NMEA;
                                if (useOldNmea) {
                                    if (addNmeaListener != null) {
                                        try {
                                            addNmeaListener.invoke(locationManager, oldNmeaListener);
                                        } catch (Exception e) { // NOSONAR
                                            // IGNORE
                                        }
                                    }
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
            if (useOldNmea) {
                if (removeNmeaListener != null) {
                    try {
                        removeNmeaListener.invoke(locationManager, oldNmeaListener);
                    } catch (Exception e) { // NOSONAR
                        // IGNORE
                    }
                }
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
        init();
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
     * Get the Track object that is currently being used for recording
     * 
     * @return the Track object held by the TrackerService
     */
    public Track getTrack() {
        return track;
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
                        final Logic logic = App.getLogic();
                        logic.autoDownloadBox(this, prefs.getServer(), validator, b, logic::reselectRelationMembers);
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
                        App.getTaskStorage().addBoundingBox(b); // will be filled once
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

    /**
     * Process incoming NMEA sentences
     * 
     * @param sentence the NMEA sentence
     */
    private void processNmeaSentence(@NonNull String sentence) {
        ExtendedLocation loc = Nmea.processSentence(sentence, nmeaLocation);
        if (loc != null) { // we could do filtering etc here
            // can't call something on the UI thread directly
            // need to send a message
            Message newLocation = mHandler.obtainMessage(LOCATION_UPDATE, loc);
            newLocation.sendToTarget();
            if (tracking) {
                if (pressureListener != null) {
                    if (useBarometricHeight) {
                        loc.setUseBarometricHeight();
                    }
                    loc.setBarometricHeight(pressureListener.barometricHeight);
                }
                track.addTrackPoint(loc);
            }
            autoLoadDataAndBugs(new Location(loc));
            lastLocation = loc;
        }
    }

    @SuppressWarnings("deprecation")
    class OldNmeaListener implements NmeaListener { // NOSONAR
        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            processNmeaSentence(nmea);
        }
    }

    @SuppressWarnings("NewApi")
    class NewNmeaListener implements OnNmeaMessageListener {
        @Override
        public void onNmeaMessage(String message, long timestamp) {
            processNmeaSentence(message);
        }
    }

    class PressureListener implements SensorEventListener {
        static final double ZERO_CELSIUS = 273.15;

        float millibarsOfPressure = 0;
        float barometricHeight    = 0;
        float pressureAtSeaLevel  = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
        float temperature         = 15;

        float tempP  = 0;
        float mCount = 0;

        /**
         * Calibrate barometric height from current height
         * 
         * @param calibrationHeight current height from external source or GPS
         * 
         * @see <a href="https://en.wikipedia.org/wiki/Barometric_formula">Barometric formula</A>
         */
        private void calibrate(float calibrationHeight) {
            // p0 = ph * (Th / (Th + 0.0065 * h))^-5.255
            double temp = ZERO_CELSIUS + temperature;
            pressureAtSeaLevel = (float) (millibarsOfPressure * Math.pow(temp / (temp + 0.0065 * calibrationHeight), -5.255));
            recalc();
            Log.d(DEBUG_TAG, "Calibration new p0 " + pressureAtSeaLevel + " current h " + calibrationHeight + " ambient temperature " + temp
                    + " current pressure " + millibarsOfPressure);
        }

        /**
         * Recalculate the current height after calibration
         */
        private void recalc() {
            barometricHeight = SensorManager.getAltitude(pressureAtSeaLevel, millibarsOfPressure);
            if (lastLocation instanceof ExtendedLocation) {
                ((ExtendedLocation) lastLocation).setBarometricHeight(barometricHeight);
            }
        }

        /**
         * Set the reference sea level pressure
         * 
         * @param p0 the pressure in hPa
         */
        private void setP0(float p0) {
            pressureAtSeaLevel = p0;
            recalc();
        }

        /**
         * Set the ambient temperature
         * 
         * @param temp the temperature in ° celsius
         */
        public void setTemperature(float temp) {
            temperature = temp;
        }

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
            // Ignore
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mCount == 10) {
                millibarsOfPressure = tempP / 10;
                recalc();
                tempP = event.values[0];
                mCount = 1;
            } else {
                tempP = tempP + event.values[0];
                mCount++;
            }
        }
    }

    class TemperatureListener implements SensorEventListener {

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
            // Ignore
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (pressureListener != null) {
                pressureListener.setTemperature(event.values[0]);
            }
        }
    }

    /**
     * Get the offset vs. msl altitude
     * 
     * Will lazily instantiate the model.
     * 
     * @param lon the WGS84 longitude
     * @param lat the WGS84 latitude
     * @return the offset in meters
     */
    private double getGeoidOffset(double lon, double lat) {
        return egm.getOffset(lat, lon);
    }
}
