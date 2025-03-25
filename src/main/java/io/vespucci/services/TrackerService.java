package io.vespucci.services;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import io.vespucci.BuildConfig;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.AsyncResult;
import io.vespucci.ErrorCodes;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.PostAsyncActionHandler;
import io.vespucci.gpx.Track;
import io.vespucci.layer.data.MapOverlay;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.ViewBox;
import io.vespucci.prefs.Preferences;
import io.vespucci.sensors.PressureEventListener;
import io.vespucci.services.util.ExtendedLocation;
import io.vespucci.services.util.Nmea;
import io.vespucci.services.util.NmeaTcpClient;
import io.vespucci.services.util.NmeaTcpClientServer;
import io.vespucci.tasks.TaskStorage;
import io.vespucci.tasks.TransferTasks;
import io.vespucci.util.GeoMath;
import io.vespucci.util.Notifications;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.egm96.EGM96;
import io.vespucci.validation.Validator;

public class TrackerService extends Service {

    private static final String WAKELOCK_TAG = BuildConfig.APPLICATION_ID + ":gpx_recording";
    private static final int    TAG_LEN      = Math.min(LOG_TAG_LEN, TrackerService.class.getSimpleName().length());
    private static final String DEBUG_TAG    = TrackerService.class.getSimpleName().substring(0, TAG_LEN);

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

    private boolean tracking        = false;
    private boolean downloading     = false;
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

    private ScheduledThreadPoolExecutor autosaveExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?>          autosaveFuture   = null;

    /**
     * For no apparent sane reason google has deprecated the NmeaListener interface
     */
    private OldNmeaListener oldNmeaListener = null;
    private NewNmeaListener newNmeaListener = null;
    private boolean         useOldNmea      = false;

    private long staleGPSMilli = 20000L;              // 20 seconds
    private long staleGPSNano  = staleGPSMilli * 1000;

    private Method addNmeaListener    = null;
    private Method removeNmeaListener = null;

    private SensorManager            sensorManager;
    private PressureEventListener    pressureListener;
    private boolean                  useBarometricHeight = false;
    private EGM96                    egm;
    private TemperatureEventListener temperatureListener;
    private boolean                  egmLoaded           = false;

    private Sensor pressure    = null;
    private Sensor temperature = null;

    private WakeLock wakeLock = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(DEBUG_TAG, "onCreate");
        track = new Track(this, true);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        prefs = App.getPreferences(this);
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
            addNmeaListener = LocationManager.class.getMethod("addNmeaListener", GpsStatus.NmeaListener.class); // NOSONAR
            removeNmeaListener = LocationManager.class.getMethod("removeNmeaListener", GpsStatus.NmeaListener.class); // NOSONAR
        } catch (Exception e) { // NOSONAR
            Log.e(DEBUG_TAG, "reflection didn't find addNmeaListener or removeNmeaListener " + e.getMessage());
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Uri egmFile = prefs.getEgmFile();
        if (egmFile != null) {
            try {
                egm = new EGM96(egmFile.getPath());
                egmLoaded = true;
            } catch (IOException ioex) {
                String egmError = getString(R.string.toast_error_loading_egm, ioex.getMessage());
                Log.e(DEBUG_TAG, egmError);
                ScreenMessage.toastTopError(this, egmError);
            }
        }
    }

    /**
     * Setup the pressure and temp sensors
     * 
     * @param sensorManager a SensorManager instance
     */
    private void setupPressureSensor(@NonNull SensorManager sensorManager) {
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (prefs.useBarometricHeight() && pressure != null && pressureListener == null) {
            Log.d(DEBUG_TAG, "Installing pressure listener");
            pressureListener = new PressureEventListener(lastLocation);
            sensorManager.registerListener(pressureListener, pressure, 1000);
            temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            if (temperature != null) {
                temperatureListener = new TemperatureEventListener();
                sensorManager.registerListener(temperatureListener, temperature, 1000);
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
            sensorManager.unregisterListener(pressureListener, pressure);
        }
        if (temperatureListener != null) {
            sensorManager.unregisterListener(temperatureListener, temperature);
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
            return 0; // NOTE not clear how or if we should return an error here
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
            calibratePressureListener(intent);
        } else {
            Log.d(DEBUG_TAG, "Received intent with unknown meaning");
        }
        return START_STICKY;
    }

    /**
     * Process a calibration intent
     * 
     * @param intent the Intent
     */
    private void calibratePressureListener(@NonNull Intent intent) {
        Log.d(DEBUG_TAG, "Calibrate height");
        if (pressureListener == null) {
            Log.e(DEBUG_TAG, "Calibration attempted but no pressure listener");
            return;
        }
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
                } else if (lastLocation.hasAltitude() && egmLoaded) {
                    double offset = getGeoidOffset(lastLocation.getLongitude(), lastLocation.getLatitude());
                    Log.d(DEBUG_TAG, "Geoid offset " + offset);
                    pressureListener.calibrate((float) (lastLocation.getAltitude() - offset));
                }
            }
        }
        ScreenMessage.toastTopInfo(this, getString(R.string.toast_pressure_calibration, pressureListener.getBarometricHeight(),
                pressureListener.getMillibarsOfPressure(), pressureListener.getPressureAtSeaLevel()));
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
    private synchronized void startTrackingInternal() {
        Log.i(DEBUG_TAG, "Start tracking");
        if (startInternal()) {
            tracking = true;
            track.markNewSegment();
            startAutosave();
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
            wakeLock.acquire();
        }
    }

    /**
     * Start auto save for edits
     */
    private void startAutosave() {
        Log.i(DEBUG_TAG, "Starting autosave");
        final int interval = PreferenceManager.getDefaultSharedPreferences(this).getInt(getString(R.string.config_gpxAutosaveInterval_key), 5);
        autosaveFuture = autosaveExecutor.scheduleAtFixedRate(() -> {
            if (tracking) {
                track.save();
            }
        }, interval, interval, TimeUnit.MINUTES);
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startAutoDownloadInternal() {
        Log.i(DEBUG_TAG, "Start auto download");
        if (startInternal()) {
            downloading = true;
        }
    }

    /**
     * Actually starts tracking. Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
     * See {@link #startTracking()} for the public method to call when tracking should be started.
     */
    private void startBugAutoDownloadInternal() {
        Log.i(DEBUG_TAG, "Start bug auto download");
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
            ScreenMessage.toastTopError(TrackerService.this, R.string.toast_default_channel_needs_to_be_enabled);
            return false;
        }
        NotificationCompat.Builder notificationBuilder = Notifications.builder(this);

        Intent appStartIntent = new Intent();
        appStartIntent.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(Main.class.getPackage().getName(), Main.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingAppIntent = PendingIntent.getActivity(this, 0, appStartIntent, PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder.setContentTitle(getString(R.string.tracking_active_title_short))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.tracking_long_text)));
        Intent exitIntent = new Intent(this, Main.class);
        exitIntent.setAction(Main.ACTION_EXIT);
        PendingIntent pendingExitIntent = PendingIntent.getActivity(this, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder.setSmallIcon(R.drawable.logo_simplified).setOngoing(true).setUsesChronometer(true).setContentIntent(pendingAppIntent)
                .setColor(ContextCompat.getColor(this, R.color.osm_green))
                .addAction(R.drawable.logo_simplified, getString(R.string.exit_title), pendingExitIntent);
        ServiceCompat.startForeground(this, R.id.notification_tracker, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
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
    public synchronized void stopTracking(boolean deleteTrack) {
        Log.d(DEBUG_TAG, "Stop tracking");
        if (autosaveFuture != null) {
            Log.i(DEBUG_TAG, "Cancelling autosave");
            autosaveFuture.cancel(false);
        }
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
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
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
    @SuppressWarnings("deprecation")
    private void stop() {
        if (!tracking && !downloading && !downloadingBugs) {
            Log.d(DEBUG_TAG, "Stopping service");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                stopForeground(Service.STOP_FOREGROUND_LEGACY);
            } else {
                stopForeground(true);
            }
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
        return track != null && !track.getTrackPoints().isEmpty();
    }

    /**
     * Check if we have any WayPoints
     * 
     * @return true is WayPoints are stored
     */
    public boolean hasWayPoints() {
        return track != null && !track.getWayPoints().isEmpty();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    /**
     * Set the current location
     * 
     * @param location the Location to set
     */
    public void setGpsLocation(@NonNull Location location) {
        gpsListener.onLocationChanged(location);
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

    private LocationListener gpsListener = new LocationListener() {
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
                if (pressureListener != null) {
                    if (useBarometricHeight) {
                        loc.setUseBarometricHeight();
                    }
                    loc.setBarometricHeight(pressureListener.getBarometricHeight());
                }

                // Only use GPS provided locations for generating tracks
                if (tracking && (!location.hasAccuracy() || location.getAccuracy() <= TRACK_LOCATION_MIN_ACCURACY)) {
                    track.addTrackPoint(location);
                }
                if (lastLocation != null && LocationManager.NETWORK_PROVIDER.equals(lastLocation.getProvider())) {
                    ScreenMessage.toastTopInfo(TrackerService.this, R.string.toast_using_gps_location);
                }
            }
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { // NOSONAR longer term we should
                                                                                  // replace this with LocationCompat
            // unused
        }

        @Override
        public void onProviderEnabled(String provider) {
            // unused
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (tracking) {
                ScreenMessage.toastTopInfo(TrackerService.this, R.string.toast_using_gps_disabled_tracking_stopped);
            }
        }
    };

    @SuppressWarnings("NewApi")
    private LocationListener networkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (source != GpsSource.INTERNAL) {
                return; // ignore updates
            }
            if (lastLocation != null) {
                boolean lastIsGpsLocation = LocationManager.GPS_PROVIDER.equals(lastLocation.getProvider());
                if (lastIsGpsLocation) {
                    if (location.getElapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos() < staleGPSNano) {
                        // ignore - last GPS time is still reasonably current
                        return;
                    }
                    ScreenMessage.toastTopInfo(TrackerService.this, R.string.toast_using_network_location);
                }
            }
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { // NOSONAR longer term we should
                                                                                  // replace this with LocationCompat
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
                ScreenMessage.toastTopError(TrackerService.this, (String) inputMessage.obj);
                break;
            case CONNECTION_MESSAGE:
                ScreenMessage.toastTopInfo(TrackerService.this, getString(R.string.toast_remote_nmea_connection, (String) inputMessage.obj));
                break;
            case CONNECTION_CLOSED:
                ScreenMessage.toastTopInfo(TrackerService.this, R.string.toast_remote_nmea_connection_closed);
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
    @SuppressLint("MissingPermission")
    @TargetApi(24)
    private void init() {
        prefs = App.getPreferences(this);
        String gpsSource = prefs.getGpsSource();
        final boolean useTcpClient = gpsSource.equals(prefTcpClient);
        final boolean useTcpServer = gpsSource.equals(prefTcpServer);
        final boolean useTcp = useTcpClient || useTcpServer;

        boolean needed = listenerNeedsGPS || tracking || downloading || downloadingBugs;

        setupPressureSensor(sensorManager);
        useBarometricHeight = pressureListener != null;

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
                        staleGPSMilli = prefs.getGnssTimeToStale() * 1000L;
                        staleGPSNano = staleGPSMilli * 1000; // convert to nanoseconds
                        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
                            // internal NMEA resource only works if normal updates are turned on
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefs.getGpsInterval(), prefs.getGpsDistance(), gpsListener);
                            if (useNema) {
                                source = GpsSource.NMEA;
                                if (useOldNmea) {
                                    addNmeaListenerWIthReflection(oldNmeaListener);
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
            } catch (RuntimeException rex) {
                Log.e(DEBUG_TAG, "Failed to enable location service", rex);
                ScreenMessage.toastTopError(this, R.string.gps_failure);
            }
        } else if (!needed && gpsEnabled) {
            Log.d(DEBUG_TAG, "Disabling GPS updates");
            try {
                locationManager.removeUpdates(gpsListener);
                if (useOldNmea) {
                    if (removeNmeaListener != null) {
                        removeNmeaListener.invoke(locationManager, oldNmeaListener);
                    }
                } else {
                    locationManager.removeNmeaListener(newNmeaListener);
                }
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                // can be safely ignored
            }
            gpsEnabled = false;
        }
    }

    /**
     * 
     * see https://issuetracker.google.com/issues/141019880
     * 
     * @param listener the OldNmeaListener listener
     * 
     */
    private void addNmeaListenerWIthReflection(OldNmeaListener listener) {
        if (addNmeaListener != null) {
            try {
                addNmeaListener.invoke(locationManager, listener);
            } catch (Exception e) { // NOSONAR
                // IGNORE
            }
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
        Log.d(DEBUG_TAG, "setListener " + listener);
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
        final StorageDelegator delegator = App.getDelegator();
        autoDownload(location, previousLocation, prefs.getDownloadRadius(), prefs.getMaxDownloadSpeed(), delegator.getBoundingBoxes(), new DownloadBox() {

            @Override
            public void download(BoundingBox box) {
                delegator.addBoundingBox(box); // will be filled once download is complete
                final Logic logic = App.getLogic();
                logic.autoDownloadBox(TrackerService.this, prefs.getServer(), validator, box, new PostAsyncActionHandler() {
                    @Override
                    public void onSuccess() {
                        logic.reselectRelationMembers();
                    }

                    @Override
                    public void onError(@Nullable AsyncResult result) {
                        if (result == null) {
                            Log.e(DEBUG_TAG, "null AsyncResult");
                            return;
                        }
                        int code = result.getCode();
                        if (MapOverlay.PAUSE_AUTO_DOWNLOAD.contains(code)) {
                            prefs.setAutoDownload(false);
                            stopAutoDownload();
                            int messageRes = R.string.unknown_error_message;
                            switch (code) {
                            case ErrorCodes.CORRUPTED_DATA:
                                messageRes = R.string.corrupted_data_message;
                                break;
                            case ErrorCodes.DATA_CONFLICT:
                                messageRes = R.string.data_conflict_message;
                                break;
                            case ErrorCodes.OUT_OF_MEMORY:
                                messageRes = R.string.out_of_memory_message;
                                break;
                            case ErrorCodes.DOWNLOAD_LIMIT_EXCEEDED:
                                messageRes = R.string.download_limit_message;
                                break;
                            default:
                                // do nothing
                            }
                            ScreenMessage.toastTopError(TrackerService.this, getString(messageRes), true);
                            ScreenMessage.toastTopError(TrackerService.this, getString(R.string.autodownload_has_been_paused), true);
                        }
                    }
                });
            }

            @Override
            public void saveLocation(Location location) {
                previousLocation = location;
            }
        });
    }

    /**
     * Check if the supplied coordinates are in one of the BoundingBoxes
     * 
     * @param bbs a List of BoundingBox
     * @param lonE7 longitude in WGS84*10E7
     * @param latE7 latitude in WGS84*10E7
     * @return true if one of the BoundingBoxes cover the coordinate
     */
    private static boolean bbLoaded(@NonNull List<BoundingBox> bbs, int lonE7, int latE7) {
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
    private static BoundingBox getNextBox(@NonNull List<BoundingBox> bbs, Location prevLocation, @NonNull Location location, int radius) {
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
        final TaskStorage taskStorage = App.getTaskStorage();
        autoDownload(location, previousBugLocation, prefs.getBugDownloadRadius(), prefs.getMaxBugDownloadSpeed(), taskStorage.getBoundingBoxes(),
                new DownloadBox() {

                    @Override
                    public void download(BoundingBox box) {
                        taskStorage.addBoundingBox(box); // will be filled once download is complete
                        TransferTasks.downloadBox(TrackerService.this, prefs.getServer(), box, true, TransferTasks.MAX_PER_REQUEST, null);
                        if (prefs.autoPrune() && taskStorage.reachedPruneLimits(prefs.getAutoPruneNodeLimit(), prefs.getAutoPruneBoundingBoxLimit())) {
                            ViewBox pruneBox = new ViewBox(App.getLogic().getViewBox());
                            pruneBox.scale(1.6);
                            taskStorage.prune(pruneBox);
                        }
                    }

                    @Override
                    public void saveLocation(Location location) {
                        previousBugLocation = location;

                    }
                });
    }

    interface DownloadBox {
        /**
         * Download data in box
         * 
         * @param box the BoundingBox to download
         */
        void download(@NonNull BoundingBox box);

        /**
         * Save the new location
         * 
         * @param location the new Location
         */
        void saveLocation(@NonNull Location location);
    }

    /**
     * Calculate the missing bounding boxes and then actually download
     * 
     * @param location the current Location
     * @param prevLocation the previous Location
     * @param radius 1/2 of a side of the box to download
     * @param maxSpeed maximum speed at which we still download
     * @param boxes current list of coverage bounding boxes
     * @param downloadBox callback to do the actual downloading
     */
    public static void autoDownload(@NonNull Location location, @Nullable Location prevLocation, int radius, float maxSpeed, @NonNull List<BoundingBox> boxes,
            @NonNull DownloadBox downloadBox) {
        // some heuristics for now to keep downloading to a minimum
        if ((location.getSpeed() < maxSpeed / 3.6f) && (prevLocation == null || location.distanceTo(prevLocation) > radius / 8)) {
            List<BoundingBox> bbList = new ArrayList<>(boxes);
            BoundingBox newBox = getNextBox(bbList, prevLocation, location, radius);
            if (newBox == null) {
                return;
            }
            if (radius != 0) { // download
                List<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, newBox);
                for (BoundingBox b : bboxes) {
                    if (b.getWidth() > 1 && b.getHeight() > 1) {
                        // ignore super small bb likely due to rounding errors
                        downloadBox.download(b);
                    }
                }
            }
            downloadBox.saveLocation(location);
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
        Log.d(DEBUG_TAG, "calling onLocationChanged " + location + " " + externalListener);
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
        if (downloading && (prefs.getServer().hasMapSplitSource() || activeNetwork)) {
            autoDownload(location, validator);
        }
        if (downloadingBugs && activeNetwork) {
            bugAutoDownload(location);
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
                    loc.setBarometricHeight(pressureListener.getBarometricHeight());
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

    class TemperatureEventListener implements SensorEventListener {

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
