package de.blau.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.acra.ACRA;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.StateListDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import de.blau.android.GeoUrlActivity.GeoUrlData;
import de.blau.android.Logic.CursorPaddirection;
import de.blau.android.RemoteControlUrlActivity.RemoteControlUrlData;
import de.blau.android.actionbar.UndoDialogFactory;
import de.blau.android.contract.Paths;
import de.blau.android.dialogs.BackgroundProperties;
import de.blau.android.dialogs.ConfirmUpload;
import de.blau.android.dialogs.DataLossActivity;
import de.blau.android.dialogs.DownloadCurrentWithChanges;
import de.blau.android.dialogs.ElementInfo;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.GpxUpload;
import de.blau.android.dialogs.ImportTrack;
import de.blau.android.dialogs.NewVersion;
import de.blau.android.dialogs.Newbie;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.SearchForm;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.filter.Filter;
import de.blau.android.filter.PresetFilter;
import de.blau.android.filter.TagFilter;
import de.blau.android.imageryoffset.BackgroundAlignmentActionModeCallback;
import de.blau.android.javascript.EvalCallback;
import de.blau.android.listener.UpdateViewListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Server;
import de.blau.android.osm.Server.Visibility;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.Way;
import de.blau.android.photos.Photo;
import de.blau.android.photos.PhotoIndex;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.Address;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.propertyeditor.PropertyEditorData;
import de.blau.android.resources.DataStyle;
import de.blau.android.services.TrackerService;
import de.blau.android.services.TrackerService.TrackerBinder;
import de.blau.android.services.TrackerService.TrackerLocationListener;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.FileUtil;
import de.blau.android.util.FullScreenAppCompatActivity;
import de.blau.android.util.GeoMath;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.OAuthHelper;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SaveFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Search.SearchResult;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.ZoomControls;
import de.blau.android.voice.Commands;
import oauth.signpost.exception.OAuthException;

/**
 * This is the main Activity from where other Activities will be started.
 * 
 * @author mb
 */
public class Main extends FullScreenAppCompatActivity
        implements ServiceConnection, TrackerLocationListener, UpdateViewListener, de.blau.android.util.SearchItemFoundCallback {

    private static final int ZOOM_FOR_ZOOMTO = 22;

    /**
     * Tag used for Android-logging.
     */
    private static final String DEBUG_TAG = Main.class.getName();

    /**
     * Requests a {@link BoundingBox} as an activity-result.
     */
    public static final int REQUEST_BOUNDING_BOX = 0;

    /**
     * Requests a list of {@link Tag Tags} as an activity-result.
     */
    private static final int REQUEST_EDIT_TAG = 1;

    /**
     * Requests an activity-result.
     */
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    /**
     * Requests voice recognition.
     */
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 3;

    private static final double DEFAULT_BOUNDING_BOX_RADIUS = 4000000.0D;

    public static final String ACTION_FINISH_OAUTH = "de.blau.android.FINISH_OAUTH";

    /**
     * Alpha value for floating action buttons workaround We should probably find a better place for this
     */
    public static final float FABALPHA = 0.90f;

    /**
     * Date pattern used for the image file name.
     */
    private static final String DATE_PATTERN_IMAGE_FILE_NAME_PART = "yyyyMMdd_HHmmss";

    /**
     * Where we install the current version of vespucci
     */
    private static final String VERSION_FILE = "version.dat";

    /**
     * Id for requesting permissions
     */
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 54321;

    private class ConnectivityChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d("ConnectivityChanged...", "Received broadcast");
                if (getEasyEditManager().isProcessingAction()) {
                    getEasyEditManager().invalidate();
                } else {
                    supportInvalidateOptionsMenu();
                }
            }
        }
    }

    private ConnectivityChangedReceiver connectivityChangedReceiver;

    /** Objects to handle showing device orientation. */
    private SensorManager sensorManager;
    private Sensor        magnetometer;
    private Sensor        accelerometer;
    private Sensor        rotation;

    /**
     * @see http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html and
     *      http://www.journal.deviantdev.com/android-compass-azimuth-calculating/
     */
    private final SensorEventListener sensorListener = new SensorEventListener() {
        float   lastAzimut = -9999;
        float[] acceleration;
        float[] geomagnetic;
        float[] truncatedRotationVector;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float orientation[] = new float[3];
            float R[] = new float[9];
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    acceleration = event.values;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    geomagnetic = event.values;
                    break;
                default:
                    return;
                }
                if (acceleration != null && geomagnetic != null) {
                    float I[] = new float[9];
                    if (!SensorManager.getRotationMatrix(R, I, acceleration, geomagnetic)) {
                        return;
                    }
                }
            } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                if (event.values.length > 4) {
                    // See https://groups.google.com/forum/#!topic/android-developers/U3N9eL5BcJk for more information
                    // on this
                    //
                    // On some Samsung devices SensorManager.getRotationMatrixFromVector
                    // appears to throw an exception if rotation vector has length > 4.
                    // For the purposes of this class the first 4 values of the
                    // rotation vector are sufficient (see crbug.com/335298 for details).
                    if (truncatedRotationVector == null) {
                        truncatedRotationVector = new float[4];
                    }
                    System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                    SensorManager.getRotationMatrixFromVector(R, truncatedRotationVector);
                } else {
                    // calculate the rotation matrix
                    SensorManager.getRotationMatrixFromVector(R, event.values);
                }

            }
            SensorManager.getOrientation(R, orientation);
            float azimut = (int) (Math.toDegrees(SensorManager.getOrientation(R, orientation)[0]) + 360) % 360;
            map.setOrientation(azimut);
            // Repaint map only if orientation changed by at least 1 degree since last repaint
            if (Math.abs(azimut - lastAzimut) > 1) {
                lastAzimut = azimut;
                map.invalidate();
            }
        }
    };

    /**
     * webview for logging in and authorizing OAuth
     */
    private WebView oAuthWebView;
    private Object  oAuthWebViewLock = new Object();

    /**
     * our map layout
     */
    private RelativeLayout mapLayout;

    /** The map View. */
    private Map                                map;
    /** Detector for taps, drags, and scaling. */
    private VersionedGestureDetector           mDetector;
    /** Onscreen map zoom controls. */
    private de.blau.android.views.ZoomControls zoomControls;
    /**
     * Our user-preferences.
     */
    private Preferences                        prefs;

    /**
     * The manager for the EasyEdit mode
     */
    private EasyEditManager easyEditManager;

    /**
     * Flag indicating whether the map will be re-downloaded once the activity resumes
     */
    private static boolean redownloadOnResume;

    /**
     * Flag indicating whether data should be loaded from a file when the activity resumes. Lock is needed because we
     * potentially are processing results of intents before onResume runs Set by {@link #onCreate(Bundle)}. Overridden
     * by {@link #redownloadOnResume}.
     */
    private boolean      loadOnResume;
    private final Object loadOnResumeLock = new Object();

    /**
     * Flag indicating if we should set the view box bounding box in onResume Again we may be already setting the view
     * box by an intent and don't want to overwrite it
     */
    private boolean      setViewBox     = true;
    private final Object setViewBoxLock = new Object();

    private boolean showGPS;
    private boolean followGPS;

    /**
     * a local copy of the desired value for {@link TrackerService#setListenerNeedsGPS(boolean)}. Will be automatically
     * given to the tracker service on connect.
     */
    private boolean wantLocationUpdates = false;

    private GeoUrlData           geoData     = null;
    private final Object         geoDataLock = new Object();
    private RemoteControlUrlData rcData      = null;
    private final Object         rcDataLock  = new Object();

    /**
     * Optional bottom toolbar
     */
    private android.support.v7.widget.ActionMenuView bottomBar = null;

    /**
     * GPS FAB
     */
    private FloatingActionButton follow;

    /**
     * The current instance of the tracker service
     */
    private TrackerService tracker = null;

    private UndoListener undoListener;

    private BackgroundAlignmentActionModeCallback backgroundAlignmentActionModeCallback = null; // hack to protect
                                                                                                // against weird state

    private Location lastLocation = null;

    private Location locationForIntent = null;
    
    private boolean controlsHidden = false;
    private Object controlsHiddenLock = new Object();

    /**
     * Status of permissions
     */
    private boolean      locationPermissionGranted  = false;
    private boolean      askedForLocationPermission = false;
    private final Object locationPermissionLock     = new Object();

    private boolean      storagePermissionGranted  = false;
    private boolean      askedForStoragePermission = false;
    private final Object storagePermissionLock     = new Object();

    /**
     * file we asked the camera app to create (ugly)
     */
    private File imageFile = null;

    private PostAsyncActionHandler restart; // if set this is called to restart post authentication

    private boolean gpsChecked = false; // flag to ensure that we only check once per activity life cycle

    private boolean saveSync = false; // save synchronously instead of async

    /**
     * While the activity is fully active (between onResume and onPause), this stores the currently active instance
     */
    private static Main runningInstance;

    /**
     * {@inheritDoc}
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.i(DEBUG_TAG, "onCreate " + (savedInstanceState != null ? " no saved state " : " saved state exists"));
        // minimal support for geo: uris and JOSM style remote control
        synchronized (geoDataLock) {
            geoData = (GeoUrlData) getIntent().getSerializableExtra(GeoUrlActivity.GEODATA);
        }
        synchronized (rcDataLock) {
            rcData = (RemoteControlUrlData) getIntent().getSerializableExtra(RemoteControlUrlActivity.RCDATA);
        }

        prefs = new Preferences(this);

        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customMain_Light);
        }

        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (magnetometer == null || accelerometer == null) {
                    sensorManager = null;
                }
            } else {
                rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                if (rotation == null) {
                    sensorManager = null;
                }
            }
        }

        int layout = R.layout.main;
        if (useFullScreen(prefs)) {
            Log.d(DEBUG_TAG, "using full screen layout");
            layout = R.layout.main_fullscreen;
        }
        LinearLayout ml = (LinearLayout) getLayoutInflater().inflate(layout, null);
        mapLayout = (RelativeLayout) ml.findViewById(R.id.mainMap);

        if (map != null) {
            Log.d(DEBUG_TAG, "map exists .. destroying");
            map.onDestroy();
        }
        map = new Map(this);
        map.setId(R.id.map_view);

        // Register some Listener
        MapTouchListener mapTouchListener = new MapTouchListener();
        map.setOnTouchListener(mapTouchListener);
        map.setOnCreateContextMenuListener(mapTouchListener);
        map.setOnKeyListener(new MapKeyListener());

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) { // 12 upwards
            map.setOnGenericMotionListener(new MotionEventListener());
        }

        mapLayout.addView(map, 0); // index 0 so that anything in the layout comes after it/on top

        mDetector = VersionedGestureDetector.newInstance(this, mapTouchListener);

        // Set up the zoom in/out controls
        zoomControls = new de.blau.android.views.ZoomControls(this);

        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.getLogic().zoom(Logic.ZOOM_IN);
                setFollowGPS(followGPS);
                updateZoomControls();
            }
        });
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.getLogic().zoom(Logic.ZOOM_OUT);
                setFollowGPS(followGPS);
                updateZoomControls();
            }
        });

        // follow GPS button setup
        follow = (FloatingActionButton) mapLayout.findViewById(R.id.follow);

        follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFollowGPS(true);
            }
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // currently can't be set in layout
            ColorStateList followTint = ContextCompat.getColorStateList(this, R.color.follow);
            Util.setBackgroundTintList(follow, followTint);
        }
        Util.setAlpha(follow, Main.FABALPHA);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mapLayout.addView(zoomControls, rlp);

        DataStyle.getStylesFromFiles(this); // needs to happen before setContentView

        setContentView(ml);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        setSupportActionBar(toolbar);

        if (prefs.splitActionBarEnabled()) {
            setBottomBar((android.support.v7.widget.ActionMenuView) findViewById(R.id.bottomToolbar));
        } else {
            findViewById(R.id.bottomBar).setVisibility(View.GONE);
        }

        // check if first time user and display something if yes
        SavingHelper<String> savingHelperVersion = new SavingHelper<>();
        String lastVersion = savingHelperVersion.load(this, VERSION_FILE, false);
        boolean newInstall = (lastVersion == null || lastVersion.equals(""));
        String currentVersion = getString(R.string.app_version);
        boolean newVersion = (lastVersion != null) && (lastVersion.length() < 6 || !lastVersion.subSequence(0, 6).equals(currentVersion.subSequence(0, 6)));

        loadOnResume = false;

        if (App.getLogic() == null) {
            Log.i(DEBUG_TAG, "onCreate - creating new logic");
            App.newLogic();
        }
        Log.i(DEBUG_TAG, "onCreate - setting new map");

        App.getLogic().setPrefs(prefs);
        App.getLogic().setMap(map);

        Log.d(DEBUG_TAG, "StorageDelegator dirty is " + App.getDelegator().isDirty());
        if (isLastActivityAvailable() && !App.getDelegator().isDirty()) { // data was modified while we were stopped if
                                                                          // isDirty is true
            // Start loading after resume to ensure loading dialog can be removed afterwards
            loadOnResume = true;
        } else { // the following code should likely be moved to onStart or onResume
            if (geoData == null && rcData == null && App.getDelegator().isEmpty()) {
                // check if we have a position
                Location loc = getLastLocation();
                BoundingBox box = null;
                if (loc != null) {
                    try {
                        box = GeoMath.createBoundingBoxForCoordinates(loc.getLatitude(), loc.getLongitude(), DEFAULT_BOUNDING_BOX_RADIUS, true); // km
                                                                                                                                                 // hardwired
                                                                                                                                                 // for
                                                                                                                                                 // now
                    } catch (OsmException e) {
                        ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                        ACRA.getErrorReporter().handleException(e);
                    }
                } else { // create a largish bb centered on 51.48,0
                    try {
                        box = GeoMath.createBoundingBoxForCoordinates(51.48, 0, DEFAULT_BOUNDING_BOX_RADIUS, false); // km
                                                                                                                     // hardwired
                                                                                                                     // for
                                                                                                                     // now
                    } catch (OsmException e) {
                        ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
                openEmptyMap(box);

                // only show box picker if we are not showing welcome dialog
                if (!(newInstall || newVersion)) {
                    gotoBoxPicker();
                }
            }
        }

        easyEditManager = new EasyEditManager(this);

        // show welcome dialog
        if (newInstall) {
            // newbie, display welcome dialog
            Log.d(DEBUG_TAG, "showing welcome dialog");
            Newbie.showDialog(this);
        } else if (newVersion) {
            Log.d(DEBUG_TAG, "new version");
            NewVersion.showDialog(this);
        }
        savingHelperVersion.save(this, VERSION_FILE, getString(R.string.app_version), false);
    }

    /**
     * Get the best last position
     */
    private Location getLastLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            try {
                Location location = locationManager.getLastKnownLocation(provider);
                if (bestLocation == null || !bestLocation.hasAccuracy()
                        || (location != null && location.hasAccuracy() && location.getAccuracy() < bestLocation.getAccuracy())) {
                    bestLocation = location;
                }
            } catch (IllegalArgumentException e) {
            } catch (SecurityException e) {
            }
        }
        return bestLocation;
    }

    /**
     * Loads the preferences into {@link #map}, triggers new {@inheritDoc}
     */
    @Override
    protected void onStart() {
        Log.d(DEBUG_TAG, "onStart");
        super.onStart();

        prefs = new Preferences(this);
        App.getLogic().setPrefs(prefs);

        // if we have been stopped delegator and viewbox will not be set if our original Logic instance is still around
        map.setDelegator(App.getDelegator());
        map.setViewBox(App.getLogic().getViewBox());

        map.setPrefs(this, prefs);
        map.createOverlays(this);
        map.getOpenStreetMapTilesOverlay().setContrast(prefs.getContrastValue());
        map.requestFocus();

        undoListener = new UndoListener();

        showActionBar();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(DEBUG_TAG, "onNewIntent storage dirty " + App.getDelegator().isDirty());
        if (ACTION_FINISH_OAUTH.equals(intent.getAction())) {
            Log.d(DEBUG_TAG, "onNewIntent calling finishOAuth");
            finishOAuth();
            return;
        }
        setIntent(intent);
        synchronized (geoDataLock) {
            geoData = (GeoUrlData) getIntent().getSerializableExtra(GeoUrlActivity.GEODATA);
        }

        synchronized (rcDataLock) {
            rcData = (RemoteControlUrlData) getIntent().getSerializableExtra(RemoteControlUrlActivity.RCDATA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        final Logic logic = App.getLogic();

        checkPermissions();

        // register received for changes in connectivity
        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        connectivityChangedReceiver = new ConnectivityChangedReceiver();
        registerReceiver(connectivityChangedReceiver, filter);

        PostAsyncActionHandler postLoadData = new PostAsyncActionHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSuccess() {
                if (rcData != null || geoData != null) {
                    setShowGPS(false);
                    processIntents();
                } else {
                    if (getTracker() != null) {
                        lastLocation = getTracker().getLastLocation();
                    }
                    setShowGPS(prefs.getShowGPS());
                }
                setupLockButton();
                if (logic.getFilter() != null) {
                    logic.getFilter().addControls(mapLayout, new Filter.Update() {
                        @Override
                        public void execute() {
                            map.invalidate();
                            scheduleAutoLock();
                        }
                    });
                    logic.getFilter().showControls();
                }
                updateActionbarEditMode();
                Mode mode = logic.getMode();
                if (mode.elementsGeomEditiable() && (logic.getSelectedNode() != null || logic.getSelectedWay() != null
                        || (logic.getSelectedRelations() != null && !logic.getSelectedRelations().isEmpty()))) {
                    // need to restart whatever we were doing
                    Log.d(DEBUG_TAG, "restarting action mode");
                    easyEditManager.editElements();
                } else if (mode.elementsEditable()) {
                    // de-select everything
                    logic.deselectAll();
                }
            }

            @Override
            public void onError() {
            }
        };
        synchronized (loadOnResumeLock) {
            if (redownloadOnResume) {
                redownloadOnResume = false;
                logic.downloadLast(this);
            } else if (loadOnResume) {
                loadOnResume = false;
                PostAsyncActionHandler postLoadTasks = new PostAsyncActionHandler() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSuccess() {
                        Mode mode = logic.getMode();
                        Task t = logic.getSelectedBug();
                        if (mode.elementsGeomEditiable() && t != null) {
                            performBugEdit(t);
                        }
                    }

                    @Override
                    public void onError() {
                    }
                };
                logic.loadStateFromFile(this, postLoadData);
                logic.loadBugsFromFile(this, postLoadTasks);
            } else { // loadFromFile already does this
                synchronized (setViewBoxLock) {
                    App.getLogic().loadEditingState(this, setViewBox);
                }
                postLoadData.onSuccess();
                map.invalidate();
            }
        }
        synchronized (setViewBoxLock) {
            // reset in any case
            setViewBox = true;
        }
        logic.updateProfile();
        map.updateProfile();

        // start listening for location updates
        if (getTracker() != null) {
            getTracker().setListener(Main.this);
        }

        runningInstance = this;

        map.setKeepScreenOn(prefs.isKeepScreenOnEnabled());
        scheduleAutoLock();

        if (prefs.getEnableTagFilter() && logic.getMode() != Mode.MODE_INDOOR) {
            Filter.Update updater = new Filter.Update() {
                @Override
                public void execute() {
                    map.invalidate();
                    scheduleAutoLock();
                }
            };
            logic.setFilter(new TagFilter(this));
            logic.getFilter().addControls(getMapLayout(), updater);
        }
    }

    /**
     * Check if we have fine location permission and ask for it if not Side effect binds to TrackerService
     */
    private void checkPermissions() {
        final List<String> permissionsList = new ArrayList<>();
        synchronized (locationPermissionLock) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = false;
                if (askedForLocationPermission) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // for now we just repeat the request (max once)
                        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                } else {
                    permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                    askedForLocationPermission = true;
                }
            } else { // permission was already given
                bindService(new Intent(this, TrackerService.class), this, BIND_AUTO_CREATE);
                locationPermissionGranted = true;
            }
        }
        synchronized (storagePermissionLock) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                storagePermissionGranted = false;
                // Should we show an explanation?
                if (askedForStoragePermission) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // for now we just repeat the request (max once)
                        permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    askedForStoragePermission = true;
                }
            } else { // permission was already given
                storagePermissionGranted = true;
            }
        }
        if (!permissionsList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
    }

    /**
     * Check if we have write to external permission and ask for it if not
     */
    void checkStoragePermission() {

    }

    /**
     * Process geo and JOSM remote control intents
     */
    private void processIntents() {
        final Logic logic = App.getLogic();
        synchronized (geoDataLock) {
            if (geoData != null) {
                Log.d(DEBUG_TAG,
                        "got position from geo: url " + geoData.getLat() + "/" + geoData.getLon() + " storage dirty is " + App.getDelegator().isDirty());
                if (prefs.getDownloadRadius() != 0) { // download
                    BoundingBox bbox;
                    try {
                        bbox = GeoMath.createBoundingBoxForCoordinates(geoData.getLat(), geoData.getLon(), prefs.getDownloadRadius(), true);
                        List<BoundingBox> bbList = new ArrayList<>(App.getDelegator().getBoundingBoxes());
                        List<BoundingBox> bboxes = null;
                        if (App.getDelegator().isEmpty()) {
                            bboxes = new ArrayList<>();
                            bboxes.add(bbox);
                        } else {
                            bboxes = BoundingBox.newBoxes(bbList, bbox);
                        }
                        if (bboxes != null && !bboxes.isEmpty()) {
                            logic.downloadBox(this, bbox, true, null);
                            if (prefs.areBugsEnabled()) { // always adds bugs for now
                                TransferTasks.downloadBox(this, prefs.getServer(), bbox, true, new PostAsyncActionHandler() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void onSuccess() {
                                        getMap().invalidate();
                                    }

                                    @Override
                                    public void onError() {
                                    }
                                });
                            }
                        } else {
                            Log.d(DEBUG_TAG, "no bbox to download");
                            logic.getViewBox().setBorders(getMap(), bbox);
                            map.invalidate();
                        }
                    } catch (OsmException e) {
                        Log.d(DEBUG_TAG, e.getMessage());
                    }
                } else {
                    Log.d(DEBUG_TAG, "moving to position");
                    map.getViewBox().moveTo(getMap(), (int) (geoData.getLon() * 1E7), (int) (geoData.getLat() * 1E7));
                    map.invalidate();
                }
                geoData = null; // zap to stop repeated downloads
            }
        }
        synchronized (rcDataLock) {
            if (rcData != null) {
                Log.d(DEBUG_TAG, "got data from remote control url " + rcData.getBox() + " load " + rcData.load());
                StorageDelegator delegator = App.getDelegator();
                ArrayList<BoundingBox> bbList = new ArrayList<>(delegator.getBoundingBoxes());
                BoundingBox loadBox = rcData.getBox();
                if (loadBox != null) {
                    if (rcData.load()) { // download
                        List<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, loadBox);
                        if (bboxes != null && (!bboxes.isEmpty() || delegator.isEmpty())) {
                            // only download if we haven't yet
                            logic.downloadBox(this, rcData.getBox(), true /* logic.delegator.isDirty() */, new PostAsyncActionHandler() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onSuccess() {
                                    synchronized (rcDataLock) {
                                        if (rcData != null) {
                                            rcDataEdit(rcData);
                                            rcData = null; // zap to stop repeated downloads
                                        }
                                    }
                                }

                                @Override
                                public void onError() {
                                }
                            });
                        } else {
                            rcDataEdit(rcData);
                            rcData = null; // zap to stop repeated downloads
                        }
                    } else { // zoom
                        map.getViewBox().setBorders(getMap(), rcData.getBox());
                        map.invalidate();
                        rcData = null; // zap to stop repeated downloads
                    }
                } else {
                    Log.d(DEBUG_TAG, "RC box is null");
                    rcDataEdit(rcData);
                    rcData = null; // zap to stop repeated downloads
                }
            }
        }
    }

    /**
     * Parse the parameters of a JOSM remote control URL and select and edit the OSM objects.
     * 
     * @param rcData Data of a remote control data URL.
     */
    private void rcDataEdit(RemoteControlUrlData rcData) {
        BoundingBox box = rcData.getBox();
        if (box != null) {
            map.getViewBox().setBorders(getMap(), box);
        }
        final Logic logic = App.getLogic();
        if (rcData.getSelect() != null) {
            // need to actually switch to easyeditmode
            if (!logic.getMode().elementsGeomEditiable()) { // TODO there might be states in which we don't want to exit
                                                            // which ever mode we are in
                setMode(this, Mode.MODE_EASYEDIT);
            }
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setSelectedRelation(null);
            StorageDelegator storageDelegator = App.getDelegator();
            for (String s : rcData.getSelect().split(",")) { // see
                                                             // http://wiki.openstreetmap.org/wiki/JOSM/Plugins/RemoteControl
                if (s != null) {
                    Log.d(DEBUG_TAG, "rc select: " + s);
                    try {
                        if (s.startsWith("node")) {
                            long id = Long.parseLong(s.substring(Node.NAME.length()));
                            Node n = (Node) storageDelegator.getOsmElement(Node.NAME, id);
                            if (n != null) {
                                logic.addSelectedNode(n);
                            }
                        } else if (s.startsWith("way")) {
                            long id = Long.parseLong(s.substring(Way.NAME.length()));
                            Way w = (Way) storageDelegator.getOsmElement(Way.NAME, id);
                            if (w != null) {
                                logic.addSelectedWay(w);
                            }
                        } else if (s.startsWith("relation")) {
                            long id = Long.parseLong(s.substring(Relation.NAME.length()));
                            Relation r = (Relation) storageDelegator.getOsmElement(Relation.NAME, id);
                            if (r != null) {
                                logic.addSelectedRelation(r);
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        Log.d(DEBUG_TAG, "Parsing " + s + " caused " + nfe);
                        // not much more we can do here
                    }
                }
            }
            FloatingActionButton lock = getLock();
            if (logic.isLocked() && lock != null) {
                lock.performClick();
            }
            easyEditManager.editElements();
        }
    }

    @Override
    protected void onPause() {
        descheduleAutoLock();
        Log.d(DEBUG_TAG, "onPause mode " + App.getLogic().getMode());
        runningInstance = null;
        try {
            unregisterReceiver(connectivityChangedReceiver);
        } catch (Exception e) {
            // FIXME if onPause gets called before onResume has registered the Receiver
            // unregisterReceiver will throw an exception, a better fix would likely to
            // register earlier, but that may not help
        }
        disableLocationUpdates();
        if (getTracker() != null)
            getTracker().setListener(null);

        // always save editing state
        App.getLogic().saveEditingState(this);
        // onPause is the last lifecycle callback guaranteed to be called on pre-honeycomb devices
        // on honeycomb and later, onStop is also guaranteed to be called, so we can defer saving.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            saveData();

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(DEBUG_TAG, "onStop");
        // editing state has been saved in onPause

        // On devices with Android versions before Honeycomb, we already save data in onPause
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            saveData();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        map.onDestroy();
        if (getTracker() != null)
            getTracker().setListener(null);
        try {
            unbindService(this);
        } catch (Exception ignored) {
            Log.d(DEBUG_TAG, "Ignored " + ignored);
        }
        super.onDestroy();
    }

    /**
     * Save current data (state, downloaded data, changes, ...) to file(s)
     */
    private void saveData() {
        Log.i(DEBUG_TAG, "saving data sync=" + saveSync);
        final Logic logic = App.getLogic();
        if (saveSync) {
            logic.save(this);
        } else {
            logic.saveAsync(this);
        }
    }

    /**
     * Update the state of the onscreen zoom controls to reflect their ability to zoom in/out.
     */
    private void updateZoomControls() {
        final Logic logic = App.getLogic();
        getControls().setIsZoomInEnabled(logic.canZoom(Logic.ZOOM_IN));
        getControls().setIsZoomOutEnabled(logic.canZoom(Logic.ZOOM_OUT));
    }

    // @Override
    // public Object onRetainNonConfigurationInstance() {
    // Log.d(DEBUG_TAG, "onRetainNonConfigurationInstance");
    // return logic;
    // }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        App.getLogic().setMap(map);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        if (easyEditManager.isProcessingAction()) {
            easyEditManager.invalidate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(DEBUG_TAG, "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
        case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted :)
                    bindService(new Intent(this, TrackerService.class), this, BIND_AUTO_CREATE);
                    synchronized (locationPermissionLock) {
                        locationPermissionGranted = true;
                    }
                } // if not granted do nothing for now
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted :)
                    synchronized (storagePermissionLock) {
                        storagePermissionGranted = true;
                    }
                } // if not granted do nothing for now
            }
            break;
        }
        triggerMenuInvalidation(); // update menus
    }

    /**
     * Sets up the Action Bar.
     */
    private void showActionBar() {
        Log.d(DEBUG_TAG, "showActionBar");
        final ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        setupLockButton();
        if (prefs.splitActionBarEnabled()) {
            actionbar.hide();
        } else {
            actionbar.show();
        }
        FloatingActionButton follow = getFollowButton();
        if (follow != null) {
            if (ensureGPSProviderEnabled()) {
                RelativeLayout.LayoutParams params = (LayoutParams) follow.getLayoutParams();
                if (getString(R.string.follow_GPS_left).equals(prefs.followGPSbuttonPosition())) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                } else if (getString(R.string.follow_GPS_right).equals(prefs.followGPSbuttonPosition())) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                } else if (getString(R.string.follow_GPS_none).equals(prefs.followGPSbuttonPosition())) {
                    follow.hide();
                }
                follow.setLayoutParams(params);
            } else {
                follow.hide();
            }
        }
    }

    /**
     * Setups up the listeners for click and longclick on the lock icon including mode switching logic
     */
    @SuppressLint("InflateParams")
    private void setupLockButton() {
        final Logic logic = App.getLogic();
        Mode mode = logic.getMode();
        Log.d(DEBUG_TAG, "setupLockButton mode " + mode);
        // ToggleButton lock = setLock(mode);
        final FloatingActionButton lock = setLock(mode);
        if (lock == null) {
            return; // FIXME not good but no other alternative right now, already logged in setLock
        }
        lock.setTag(mode.tag());

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, ContextCompat.getDrawable(this, mode.iconResourceId()));
        states.addState(new int[] { 0 }, ContextCompat.getDrawable(this, R.drawable.locked_opaque));
        lock.setImageDrawable(states);

        //
        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View b) {
                Log.d(DEBUG_TAG, "Lock pressed " + b.getClass().getName());
                int[] drawableState = ((FloatingActionButton) b).getDrawableState();
                Log.d(DEBUG_TAG, "Lock state length " + drawableState.length + " " + (drawableState.length == 1 ? Integer.toHexString(drawableState[0]) : ""));
                if (drawableState.length == 0 || drawableState[0] != android.R.attr.state_pressed) {
                    Mode mode = Mode.modeForTag((String) b.getTag());
                    logic.setMode(Main.this, mode);
                    ((FloatingActionButton) b).setImageState(new int[] { android.R.attr.state_pressed }, false);
                    logic.setLocked(false);
                } else {
                    logic.setLocked(true);
                    ((FloatingActionButton) b).setImageState(new int[] { 0 }, false);
                }
                onEditModeChanged();
                map.invalidate();
            }
        });
        lock.setLongClickable(true);
        lock.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View b) {
                Log.d(DEBUG_TAG, "Lock long pressed " + b.getClass().getName());
                final Logic logic = App.getLogic();

                Mode mode = logic.getMode();

                PopupMenu popup = new PopupMenu(Main.this, lock);

                // per mode menu items
                ArrayList<Mode> allModes = new ArrayList<>(Arrays.asList(Mode.values()));
                // add menu entries for all proper modes
                for (final Mode newMode : allModes) {
                    if (newMode.isSubModeOf() == null && newMode.isEnabled()) {
                        MenuItem item = popup.getMenu().add(newMode.getName(Main.this));
                        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                logic.setMode(Main.this, newMode);
                                b.setTag(newMode.tag());
                                StateListDrawable states = new StateListDrawable();
                                states.addState(new int[] { android.R.attr.state_pressed }, ContextCompat.getDrawable(Main.this, newMode.iconResourceId()));
                                states.addState(new int[] {}, ContextCompat.getDrawable(Main.this, R.drawable.locked_opaque));
                                lock.setImageDrawable(states);
                                if (logic.isLocked()) {
                                    ((FloatingActionButton) b).setImageState(new int[] { 0 }, false);
                                } else {
                                    ((FloatingActionButton) b).setImageState(new int[] { android.R.attr.state_pressed }, false);
                                }
                                onEditModeChanged();
                                return true;
                            }
                        });
                    }
                }
                popup.show();
                return true;
            }
        });
    }

    public FloatingActionButton getLock() {
        return (FloatingActionButton) findViewById(R.id.floatingLock);
    }

    /**
     * Set lock button to locked or unlocked depending on the edit mode
     * 
     * @param mode Program mode.
     * @return Button to display checked/unchecked states.
     */
    private FloatingActionButton setLock(Mode mode) {
        FloatingActionButton lock = getLock();
        if (lock == null) {
            Log.d(DEBUG_TAG, "couldn't find lock button");
            return null;
        }
        Logic logic = App.getLogic();
        if (logic.isLocked()) {
            lock.setImageState(new int[] { 0 }, false);
        } else {
            lock.setImageState(new int[] { android.R.attr.state_pressed }, false);
        }
        logic.setMode(this, mode);
        return lock; // for convenience
    }

    public void setMode(Main main, Mode mode) {
        App.getLogic().setMode(main, mode);
    }

    private void updateActionbarEditMode() {
        Log.d(DEBUG_TAG, "updateActionbarEditMode");
        Mode mode = App.getLogic().getMode();
        setLock(mode);
        supportInvalidateOptionsMenu();
    }

    public static void onEditModeChanged() {
        Log.d(DEBUG_TAG, "onEditModeChanged");
        if (runningInstance != null)
            runningInstance.updateActionbarEditMode();
    }

    BottomBarClickListener bottomBarListener;

    @Override
    public boolean onPrepareOptionsMenu(final Menu m) {
        if (bottomBarListener == null && getBottomBar() != null) {
            // NOTE doing this here tries to keep a valid reference to the activity
            // doing it here should guarantee that it always works
            bottomBarListener = new BottomBarClickListener(this);
            getBottomBar().setOnMenuItemClickListener(bottomBarListener);
        }
        return true;
    }

    /**
     * Creates the menu from the XML file "main_menu.xml".<br>
     * {@inheritDoc}
     * 
     * Note for not entirely clear reasons *:setShowAsAction doesn't work in the menu definition and has to be done
     * programmatically here.
     */
    @SuppressLint("InflateParams")
    @Override
    public boolean onCreateOptionsMenu(final Menu m) {
        Log.d(DEBUG_TAG, "onCreateOptionsMenu");
        // determine how man icons have room
        MenuUtil menuUtil = new MenuUtil(this);
        Menu menu = m;
        if (getBottomBar() != null) {
            menu = getBottomBar().getMenu();
            Log.d(DEBUG_TAG, "inflated main menu on to bottom toolbar");
        }
        if (menu.size() == 0) {
            menu.clear();
            final MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
        }

        boolean networkConnected = NetworkStatus.isConnected(this);
        boolean gpsProviderEnabled = ensureGPSProviderEnabled() && locationPermissionGranted;
        // just as good as any other place to check this
        synchronized (controlsHiddenLock) {
            if (!controlsHidden) {
                if (gpsProviderEnabled) {
                    showFollowButton();
                } else {
                    hideFollowButton();
                }
            }
        }
        menu.findItem(R.id.menu_gps_show).setEnabled(gpsProviderEnabled).setChecked(showGPS);
        menu.findItem(R.id.menu_gps_follow).setEnabled(gpsProviderEnabled).setChecked(followGPS);
        menu.findItem(R.id.menu_gps_goto).setEnabled(gpsProviderEnabled);
        menu.findItem(R.id.menu_gps_start).setEnabled(getTracker() != null && !getTracker().isTracking() && gpsProviderEnabled);
        menu.findItem(R.id.menu_gps_pause).setEnabled(getTracker() != null && getTracker().isTracking() && gpsProviderEnabled);
        menu.findItem(R.id.menu_gps_autodownload).setEnabled(getTracker() != null && gpsProviderEnabled && networkConnected).setChecked(autoDownload());
        menu.findItem(R.id.menu_transfer_bugs_autodownload).setEnabled(getTracker() != null && gpsProviderEnabled && networkConnected)
                .setChecked(bugAutoDownload());

        boolean trackerHasPoints = getTracker() != null && getTracker().getTrackPoints() != null && !getTracker().getTrackPoints().isEmpty();
        menu.findItem(R.id.menu_gps_clear).setEnabled(trackerHasPoints);
        menu.findItem(R.id.menu_gps_goto_start).setEnabled(trackerHasPoints);
        menu.findItem(R.id.menu_gps_import).setEnabled(getTracker() != null);
        menu.findItem(R.id.menu_gps_upload).setEnabled(trackerHasPoints && NetworkStatus.isConnected(this));

        final Logic logic = App.getLogic();
        MenuItem undo = menu.findItem(R.id.menu_undo);
        undo.setVisible(!logic.isLocked() && (logic.getUndo().canUndo() || logic.getUndo().canRedo()));
        View undoView = MenuItemCompat.getActionView(undo);
        if (undoView == null) { // FIXME this is a temp workaround for pre-11 Android, we could probably simply always
                                // do the following
            Log.d(DEBUG_TAG, "undoView null");
            Context context = new ContextThemeWrapper(this, prefs.lightThemeEnabled() ? R.style.Theme_customMain_Light : R.style.Theme_customMain);
            undoView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.undo_action_view, null);
        }
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);

        final Server server = prefs.getServer();
        if (server.hasOpenChangeset()) {
            menu.findItem(R.id.menu_transfer_close_changeset).setVisible(true);
        } else {
            menu.findItem(R.id.menu_transfer_close_changeset).setVisible(false);
        }

        menu.findItem(R.id.menu_transfer_download_current).setEnabled(networkConnected);
        menu.findItem(R.id.menu_transfer_download_current_add).setEnabled(networkConnected);
        menu.findItem(R.id.menu_transfer_download_other).setEnabled(networkConnected);
        // note: isDirty is not a good indicator of if if there is really something to upload
        menu.findItem(R.id.menu_transfer_upload).setEnabled(networkConnected && !App.getDelegator().getApiStorage().isEmpty());
        menu.findItem(R.id.menu_transfer_bugs_download_current).setEnabled(networkConnected);
        menu.findItem(R.id.menu_transfer_bugs_upload).setEnabled(networkConnected && App.getTaskStorage().hasChanges());
        menu.findItem(R.id.menu_voice).setVisible(false); // don't display button for now
        // experimental menu.findItem(R.id.menu_voice).setEnabled(networkConnected &&
        // prefs.voiceCommandsEnabled()).setVisible(prefs.voiceCommandsEnabled());

        // the following depends on us having permission to write to "external" storage
        menu.findItem(R.id.menu_transfer_export).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_transfer_save_file).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_transfer_save_notes_all).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_transfer_save_notes_new_and_changed).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_gps_export).setEnabled(storagePermissionGranted);

        Filter filter = logic.getFilter();
        if (filter != null && filter instanceof TagFilter && !prefs.getEnableTagFilter()) {
            // something is wrong, try to sync
            prefs.enableTagFilter(true);
            Log.d(DEBUG_TAG, "had to resync tagfilter pref");
        }

        menu.findItem(R.id.menu_enable_tagfilter).setEnabled(logic.getMode().supportsFilters())
                .setChecked(prefs.getEnableTagFilter() && logic.getFilter() instanceof TagFilter);
        menu.findItem(R.id.menu_enable_presetfilter).setEnabled(logic.getMode().supportsFilters())
                .setChecked(prefs.getEnablePresetFilter() && logic.getFilter() instanceof PresetFilter);

        // enable the JS console menu entry
        menu.findItem(R.id.tag_menu_js_console).setEnabled(prefs.isJsConsoleEnabled());

        menuUtil.setShowAlways(menu);
        // only show camera icon if we have a camera, and a camera app is installed
        PackageManager pm = getPackageManager();
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && cameraIntent.resolveActivity(getPackageManager()) != null) {
            MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_camera),
                    prefs.showCameraAction() ? MenuItemCompat.SHOW_AS_ACTION_ALWAYS : MenuItemCompat.SHOW_AS_ACTION_NEVER);
        } else {
            MenuItem mi = menu.findItem(R.id.menu_camera).setVisible(false);
            MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_NEVER);
        }

        if (getBottomBar() != null) {
            // menuUtil.evenlyDistributedToolbar(getBottomToolbar());
        }

        return true;
    }

    @Override
    public void onItemFound(SearchResult sr) {
        // turn this off or else we get bounced back to our current GPS position
        setFollowGPS(false);
        getMap().setFollowGPS(false);
        App.getLogic().setZoom(getMap(), 19);
        getMap().getViewBox().moveTo(getMap(), (int) (sr.getLon() * 1E7d), (int) (sr.getLat() * 1E7d));
        getMap().invalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(DEBUG_TAG, "onOptionsItemSelected");
        final Server server = prefs.getServer();
        final Logic logic = App.getLogic();
        StorageDelegator delegator = App.getDelegator();
        if (delegator == null) { // very unlikely error condition
            Log.e(DEBUG_TAG, "onOptionsItemSelected delegator null");
            return true;
        }
        switch (item.getItemId()) {
        case R.id.menu_config:
            PrefEditor.start(this, getMap().getViewBox());
            return true;

        case R.id.menu_find:
            SearchForm.showDialog(this, map.getViewBox());
            return true;
        case R.id.menu_enable_tagfilter:
        case R.id.menu_enable_presetfilter:
            Filter newFilter = null;
            switch (item.getItemId()) {
            case R.id.menu_enable_tagfilter:
                Log.d(DEBUG_TAG, "filter menu tag");
                if (prefs.getEnableTagFilter()) {
                    // already selected turn off
                    prefs.enableTagFilter(false);
                    item.setChecked(false);
                } else {
                    prefs.enableTagFilter(true);
                    item.setChecked(true);
                    prefs.enablePresetFilter(false);
                    newFilter = new TagFilter(this);
                }
                break;
            case R.id.menu_enable_presetfilter:
                Log.d(DEBUG_TAG, "filter menu preset");
                if (prefs.getEnablePresetFilter()) {
                    // already selected turn off
                    prefs.enablePresetFilter(false);
                    item.setChecked(false);
                } else {
                    prefs.enablePresetFilter(true);
                    item.setChecked(true);
                    prefs.enableTagFilter(false);
                    newFilter = new PresetFilter(this);
                }
                break;
            }
            Filter currentFilter = logic.getFilter();
            if (currentFilter != null) {
                currentFilter.saveState();
                currentFilter.hideControls();
                currentFilter.removeControls();
                logic.setFilter(null);
            }
            if (newFilter != null) {
                Filter.Update updater = new Filter.Update() {
                    @Override
                    public void execute() {
                        map.invalidate();
                        scheduleAutoLock();
                    }
                };
                logic.setFilter(newFilter);
                logic.getFilter().addControls(getMapLayout(), updater);
                logic.getFilter().showControls();
            }
            triggerMenuInvalidation();
            map.invalidate();
            return true;

        case R.id.menu_share:
            BoundingBox box = map.getViewBox();
            double[] lonLat = new double[2];
            lonLat[0] = ((box.getRight() - box.getLeft()) / 2 + box.getLeft()) / 1E7;
            lonLat[1] = ((box.getTop() - box.getBottom()) / 2 + box.getBottom()) / 1E7; // rough
            Util.sharePosition(this, lonLat);
            break;

        case R.id.menu_help:
            HelpViewer.start(this, R.string.help_main);
            return true;

        // case R.id.menu_voice:
        //
        // return true;

        case R.id.menu_camera:
            Intent startCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                imageFile = getImageFile();
                Uri photoUri = FileProvider.getUriForFile(this, "de.blau.android.osmeditor4android.provider", imageFile);
                if (photoUri != null) {
                    startCamera.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(startCamera, REQUEST_IMAGE_CAPTURE);
                }
            } catch (Exception ex) {
                try {
                    Snack.barError(this, getResources().getString(R.string.toast_camera_error, ex.getMessage()));
                } catch (Exception e) {
                    // protect against translation errors
                }
            }
            return true;

        case R.id.menu_gps_show:
            toggleShowGPS();
            return true;

        case R.id.menu_gps_follow:
            toggleFollowGPS();
            return true;

        case R.id.menu_gps_goto:
            Location gotoLoc = null;
            if (getTracker() != null) {
                gotoLoc = getTracker().getLastLocation();
            } else if (ensureGPSProviderEnabled()) {
                gotoLoc = getLastLocation();
            } // else moan? without GPS enabled this shouldn't be selectable currently
            if (gotoLoc != null) {
                map.getViewBox().moveTo(getMap(), (int) (gotoLoc.getLongitude() * 1E7d), (int) (gotoLoc.getLatitude() * 1E7d));
                logic.setZoom(getMap(), 19);
                map.setLocation(gotoLoc);
                map.invalidate();
            }
            return true;

        case R.id.menu_gps_start:
            if (getTracker() != null && ensureGPSProviderEnabled()) {
                getTracker().startTracking();
                setFollowGPS(true);
            }
            return true;

        case R.id.menu_gps_pause:
            if (getTracker() != null && ensureGPSProviderEnabled()) {
                getTracker().stopTracking(false);
                triggerMenuInvalidation();
            }
            return true;

        case R.id.menu_gps_clear:
            if (getTracker() != null)
                getTracker().stopTracking(true);
            triggerMenuInvalidation();
            map.invalidate();
            return true;

        case R.id.menu_gps_upload:
            if (server != null && server.isLoginSet()) {
                if (server.needOAuthHandshake()) {
                    oAuthHandshake(server, new PostAsyncActionHandler() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onSuccess() {
                            GpxUpload.showDialog(Main.this);
                        }

                        @Override
                        public void onError() {
                        }
                    });
                    if (server.getOAuth()) { // if still set
                        Snack.barError(this, R.string.toast_oauth);
                        return true;
                    }
                }
                GpxUpload.showDialog(this);
                // performTrackUpload("Test","Test",Visibility.PUBLIC);
            } else {
                ErrorAlert.showDialog(this, ErrorCodes.NO_LOGIN_DATA);
            }
            return true;

        case R.id.menu_gps_export:
            if (getTracker() != null) {
                SavingHelper.asyncExport(this, getTracker());
            }
            return true;

        case R.id.menu_gps_import:
            descheduleAutoLock();
            SelectFile.read(this, R.string.config_gpxPreferredDir_key, new ReadFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean read(Uri fileUri) {
                    // Get the Uri of the selected file
                    Log.d(DEBUG_TAG, "Read gpx file Uri: " + fileUri.toString());
                    if (getTracker() != null) {
                        if (!getTracker().getTrackPoints().isEmpty()) {
                            ImportTrack.showDialog(Main.this, fileUri);
                        } else {
                            getTracker().stopTracking(false);
                            try {
                                getTracker().importGPXFile(Main.this, fileUri);
                            } catch (FileNotFoundException e) {
                                try {
                                    Snack.barError(Main.this, getResources().getString(R.string.toast_file_not_found, fileUri.toString()));
                                } catch (Exception ex) {
                                    // protect against translation errors
                                    Log.d(DEBUG_TAG, e.getMessage());
                                }
                            }
                        }
                        SelectFile.savePref(prefs, R.string.config_gpxPreferredDir_key, fileUri);
                    }
                    map.invalidate();
                    return true;
                }
            });
            return true;

        case R.id.menu_gps_goto_start:
            List<TrackPoint> l = tracker.getTrackPoints();
            if (l != null && !l.isEmpty()) {
                Log.d(DEBUG_TAG, "Going to start of track");
                setFollowGPS(false);
                map.setFollowGPS(false);
                map.getViewBox().moveTo(getMap(), l.get(0).getLon(), l.get(0).getLat());
                logic.setZoom(getMap(), 19);
                map.invalidate();
            }
            return true;

        case R.id.menu_gps_autodownload:
            prefs.setAutoDownload(!autoDownload());
            startStopAutoDownload();
            return true;

        case R.id.menu_transfer_download_current:
            onMenuDownloadCurrent(false);
            return true;

        case R.id.menu_transfer_download_current_add:
            onMenuDownloadCurrent(true);
            return true;

        case R.id.menu_transfer_download_other:
            gotoBoxPicker();
            return true;

        case R.id.menu_transfer_upload:
            confirmUpload();
            return true;

        case R.id.menu_transfer_close_changeset:
            if (server.hasOpenChangeset()) {
                // fail silently if it doesn't work, next upload will open a new changeset in any case
                new AsyncTask<Void, Integer, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            server.closeChangeset();
                        } catch (MalformedURLException e) {
                        } catch (ProtocolException e) {
                        } catch (IOException e) {
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        triggerMenuInvalidation();
                    }
                }.execute();
            }
            return true;

        case R.id.menu_transfer_export: {
            SavingHelper.asyncExport(this, delegator);
            return true;
        }
        case R.id.menu_transfer_read_file:
            descheduleAutoLock();
            // showFileChooser(READ_OSM_FILE_SELECT_CODE);
            SelectFile.read(this, R.string.config_osmPreferredDir_key, new ReadFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean read(Uri fileUri) {
                    try {
                        logic.readOsmFile(Main.this, fileUri, false);
                    } catch (FileNotFoundException e) {
                        try {
                            Snack.barError(Main.this, getResources().getString(R.string.toast_file_not_found, fileUri.toString()));
                        } catch (Exception ex) {
                            // protect against translation errors
                        }
                    }
                    SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                    map.invalidate();
                    return true;
                }
            });
            return true;

        case R.id.menu_transfer_save_file:
            descheduleAutoLock();
            SelectFile.save(this, R.string.config_osmPreferredDir_key, new SaveFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean save(Uri fileUri) {
                    App.getLogic().writeOsmFile(Main.this, fileUri.getPath(), null);
                    SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                    return true;
                }
            });
            return true;

        case R.id.menu_transfer_bugs_download_current:
            TransferTasks.downloadBox(this, prefs.getServer(), map.getViewBox().copy(), true, new PostAsyncActionHandler() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSuccess() {
                    map.invalidate();
                }

                @Override
                public void onError() {
                }
            });
            return true;

        case R.id.menu_transfer_bugs_upload:
            if (App.getTaskStorage().hasChanges()) {
                TransferTasks.upload(this, server, null);
            } else {
                Snack.barInfo(this, R.string.toast_no_changes);
            }
            return true;

        case R.id.menu_transfer_bugs_clear:
            if (App.getTaskStorage().hasChanges()) { // FIXME show a dialog and allow override
                Snack.barError(this, R.string.toast_unsaved_changes, R.string.clear_anyway, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        App.getTaskStorage().reset();
                        map.invalidate();
                    }
                });
                return true;
            }
            App.getTaskStorage().reset();
            map.invalidate();
            return true;

        case R.id.menu_transfer_bugs_autodownload:
            prefs.setBugAutoDownload(!bugAutoDownload());
            startStopBugAutoDownload();
            return true;
        case R.id.menu_transfer_save_notes_all:
        case R.id.menu_transfer_save_notes_new_and_changed:
            if (App.getTaskStorage() == null)
                return true;
            descheduleAutoLock();
            SelectFile.save(this, R.string.config_notesPreferredDir_key, new SaveFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean save(Uri fileUri) {
                    TransferTasks.writeOsnFile(Main.this, item.getItemId() == R.id.menu_transfer_save_notes_all, fileUri.getPath(), null);
                    SelectFile.savePref(prefs, R.string.config_notesPreferredDir_key, fileUri);
                    return true;
                }
            });
            return true;
        case R.id.menu_transfer_read_custom_bugs:
            descheduleAutoLock();
            // showFileChooser(READ_OSM_FILE_SELECT_CODE);
            SelectFile.read(this, R.string.config_osmPreferredDir_key, new ReadFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean read(Uri fileUri) {
                    TransferTasks.readCustomBugs(Main.this, fileUri, false, null);
                    SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                    map.invalidate();
                    return true;
                }
            });
            return true;
        case R.id.menu_transfer_write_custom_bugs:
            descheduleAutoLock();
            SelectFile.save(this, R.string.config_osmPreferredDir_key, new SaveFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean save(Uri fileUri) {
                    TransferTasks.writeCustomBugFile(Main.this, fileUri.getPath(), null);
                    SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                    return true;
                }
            });
            return true;

        case R.id.menu_undo:
            // should not happen
            undoListener.onClick(null);
            return true;

        case R.id.menu_tools_flush_background_tile_cache:
            map.getOpenStreetMapTilesOverlay().flushTileCache(this);
            return true;

        case R.id.menu_tools_flush_overlay_tile_cache:
            map.getOpenStreetMapOverlayTilesOverlay().flushTileCache(this);
            return true;

        case R.id.menu_tools_background_align:
            Mode oldMode = logic.getMode() != Mode.MODE_ALIGN_BACKGROUND ? logic.getMode() : Mode.MODE_EASYEDIT; // protect
                                                                                                                 // against
                                                                                                                 // weird
                                                                                                                 // state
            backgroundAlignmentActionModeCallback = new BackgroundAlignmentActionModeCallback(this, oldMode);
            logic.setMode(this, Mode.MODE_ALIGN_BACKGROUND); // NOTE needs to be after instance creation
            startSupportActionMode(getBackgroundAlignmentActionModeCallback());
            return true;

        case R.id.menu_tools_background_properties:
            BackgroundProperties.showDialog(this);
            return true;
        case R.id.tag_menu_reset_address_prediction:
            Address.resetLastAddresses(this);
            return true;
        case R.id.menu_tools_oauth_reset: // reset the current OAuth tokens
            if (server.getOAuth()) {
                AdvancedPrefDatabase prefdb = new AdvancedPrefDatabase(this);
                prefdb.setAPIAccessToken(null, null);
            } else {
                Snack.barError(this, R.string.toast_oauth_not_enabled);
            }
            return true;

        case R.id.menu_tools_oauth_authorisation: // immediately start authorization handshake
            if (server.getOAuth()) {
                oAuthHandshake(server, null);
            } else {
                Snack.barError(this, R.string.toast_oauth_not_enabled);
            }
            return true;
        case R.id.tag_menu_js_console:
            Main.showJsConsole(this);
            return true;
        }
        return false;
    }

    public static void showJsConsole(final Main main) {
        main.descheduleAutoLock();
        de.blau.android.javascript.Utils.jsConsoleDialog(main, R.string.js_console_msg_live, new EvalCallback() {
            @Override
            public String eval(String input) {
                String result = de.blau.android.javascript.Utils.evalString(main, "JS Console", input, App.getLogic());
                main.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        main.getMap().invalidate();
                        main.scheduleAutoLock();
                    }
                });
                return result;
            }
        });
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, "Caught exception " + ex);
            Snack.barError(this, R.string.toast_no_voice);
        }
    }

    private File getImageFile() throws IOException {
        File outDir = FileUtil.getPublicDirectory();
        outDir = FileUtil.getPublicDirectory(outDir, Paths.DIRECTORY_PATH_PICTURES);
        String imageFileName = DateFormatter.getFormattedString(DATE_PATTERN_IMAGE_FILE_NAME_PART);
        File imageFile = File.createTempFile(imageFileName, Paths.FILE_EXTENSION_IMAGE, outDir);
        Log.d(DEBUG_TAG, "getImageFile " + imageFile.getAbsolutePath());
        return imageFile;
    }

    private void showFileChooser(int purpose) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            // startActivityForResult(
            // Intent.createChooser(intent, purpose == WRITE_OSM_FILE_SELECT_CODE ? getString(R.string.save_file) :
            // getString(R.string.read_file)),
            // purpose);
            startActivityForResult(intent, purpose);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Snack.barError(this, R.string.toast_missing_filemanager);
        }
    }

    private void startStopAutoDownload() {
        Log.d(DEBUG_TAG, "autoDownload");
        if (getTracker() != null && ensureGPSProviderEnabled()) {
            if (autoDownload()) {
                getTracker().startAutoDownload();
            } else {
                getTracker().stopAutoDownload();
            }
        }
    }

    private boolean autoDownload() {
        return prefs.getAutoDownload();
    }

    private void startStopBugAutoDownload() {
        Log.d(DEBUG_TAG, "bugAutoDownload");
        if (getTracker() != null && ensureGPSProviderEnabled()) {
            if (bugAutoDownload()) {
                getTracker().startBugAutoDownload();
            } else {
                getTracker().stopBugAutoDownload();
            }
        }
    }

    private boolean bugAutoDownload() {
        return prefs.getBugAutoDownload();
    }

    /**
     * If show is true start locations updates and start following the GPS position otherwise turn location updates off
     * 
     * @param show turn location updates on or off
     */
    private void setShowGPS(boolean show) {
        if (show && !ensureGPSProviderEnabled()) {
            show = false;
        }
        showGPS = show;
        Log.d(DEBUG_TAG, "showGPS: " + show);
        if (show) {
            enableLocationUpdates();
        } else {
            setFollowGPS(false);
            map.setLocation(null);
            disableLocationUpdates();
        }
        prefs.setShowGPS(show);
        map.invalidate();
        triggerMenuInvalidation();
    }

    private boolean getShowGPS() {
        return showGPS;
    }

    /**
     * Checks if GPS is enabled in the settings. If not, returns false and shows location settings.
     * 
     * @return true if GPS is enabled, false if not
     */
    private boolean ensureGPSProviderEnabled() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsChecked = false;
                return true;
            } else if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) { // check if there is a GPS
                                                                                            // providers at all
                if (!gpsChecked && !prefs.leaveGpsDisabled()) {
                    gpsChecked = true;
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
                return false;
            }
            //
            // Log.d(DEBUG_TAG,"No GPS provider");
            // List<String> providers = locationManager.getAllProviders();
            // for (String p:providers) {
            // Log.d(DEBUG_TAG,"Provider: " + p);
            // }
            return false;
        } catch (Exception e) {
            Log.d(DEBUG_TAG, "Error when checking for GPS, assuming GPS not available", e);
            Snack.barInfo(this, R.string.gps_failure);
            return false;
        }
    }

    public void setFollowGPS(boolean follow) {
        // Log.d(DEBUG_TAG,"setFollowGPS from " + followGPS + " to " + follow);
        if (followGPS != follow) {
            followGPS = follow;
            if (follow) {
                setShowGPS(true);
            }
            map.setFollowGPS(follow);
            triggerMenuInvalidation();
        }
        FloatingActionButton followButton = getFollowButton();
        if (followButton != null) {
            Location mapLocation = map.getLocation();
            boolean onScreen = mapLocation != null && map.getViewBox().contains(mapLocation.getLongitude(), mapLocation.getLatitude());
            followButton.setEnabled(!follow || !onScreen);
        }
        if (follow && lastLocation != null) { // update if we are returning from pause/stop
            Log.d(DEBUG_TAG, "Setting lastLocation");
            onLocationChanged(lastLocation);
        }
    }

    public boolean getFollowGPS() {
        return followGPS;
    }

    private void toggleShowGPS() {
        boolean newState = !getShowGPS();
        setShowGPS(newState);
    }

    private void toggleFollowGPS() {
        boolean newState = !followGPS;
        setFollowGPS(newState);
        map.setFollowGPS(newState);
    }

    private void enableLocationUpdates() {
        // noinspection PointlessBooleanExpression
        if (wantLocationUpdates == true)
            return;
        if (sensorManager != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
                sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
            } else {
                sensorManager.registerListener(sensorListener, rotation, SensorManager.SENSOR_DELAY_UI);
            }
        }
        wantLocationUpdates = true;
        if (getTracker() != null)
            getTracker().setListenerNeedsGPS(true);
    }

    private void disableLocationUpdates() {
        // noinspection PointlessBooleanExpression
        if (wantLocationUpdates == false)
            return;
        if (sensorManager != null)
            sensorManager.unregisterListener(sensorListener);
        wantLocationUpdates = false;
        if (getTracker() != null)
            getTracker().setListenerNeedsGPS(false);
    }

    /**
     * Handles the menu click on "download current view".<br>
     * When no {@link #delegator} is set, the user will be redirected to AreaPicker.<br>
     * When the user made some changes, {@link #DIALOG_TRANSFER_DOWNLOAD_CURRENT_WITH_CHANGES} will be shown.<br>
     * Otherwise the current viewBox will be re-downloaded from the server.
     * 
     * @param add Boolean flag indicating to handle changes (true) or not (false).
     */
    private void onMenuDownloadCurrent(boolean add) {
        Log.d(DEBUG_TAG, "onMenuDownloadCurrent");
        if (App.getLogic().hasChanges() && !add) {
            DownloadCurrentWithChanges.showDialog(this);
        } else {
            performCurrentViewHttpLoad(this, add);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(DEBUG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BOUNDING_BOX && data != null) {
            handleBoxPickerResult(resultCode, data);
        } else if (requestCode == REQUEST_EDIT_TAG && resultCode == RESULT_OK && data != null) {
            handlePropertyEditorResult(data);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // reindexPhotos();
            if (imageFile != null) {
                PhotoIndex pi = new PhotoIndex(this);
                pi.addPhoto(imageFile);
                if (prefs.isPhotoLayerEnabled()) {
                    map.invalidate();
                }
            } else {
                Log.e(DEBUG_TAG, "imageFile == null");
            }
        } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            if (easyEditManager.isProcessingAction()) {
                easyEditManager.handleActivityResult(requestCode, resultCode, data);
            } else {
                (new Commands(this)).processIntentResult(data, locationForIntent);
                locationForIntent = null;
                map.invalidate();
            }
        } else if ((requestCode == SelectFile.READ_FILE || requestCode == SelectFile.READ_FILE_OLD || requestCode == SelectFile.SAVE_FILE)
                && resultCode == RESULT_OK) {
            SelectFile.handleResult(requestCode, data);
        }
        scheduleAutoLock();
    }

    /**
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent
     *            "extras").
     */
    private void handleBoxPickerResult(final int resultCode, final Intent data) {
        Bundle b = data.getExtras();
        int left = b.getInt(BoxPicker.RESULT_LEFT);
        int bottom = b.getInt(BoxPicker.RESULT_BOTTOM);
        int right = b.getInt(BoxPicker.RESULT_RIGHT);
        int top = b.getInt(BoxPicker.RESULT_TOP);

        try {
            BoundingBox box = new BoundingBox(left, bottom, right, top);
            if (resultCode == RESULT_OK) {
                performHttpLoad(box);
            } else if (resultCode == RESULT_CANCELED) { //
                synchronized (setViewBoxLock) {
                    setViewBox = false; // stop setting the view box in onResume
                    Log.d(DEBUG_TAG, "opening empty map on " + box.toString());
                    openEmptyMap(box); // we may have a valid box
                }
            }
        } catch (OsmException e) {
            // Values should be done checked in LocationPicker.
            Log.e(DEBUG_TAG, "OsmException", e);
        }
    }

    /**
     * Handle the result of the property editor
     * 
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent
     *            "extras").
     */
    private void handlePropertyEditorResult(final Intent data) {
        final Logic logic = App.getLogic();
        Bundle b = data.getExtras();
        if (b != null && b.containsKey(PropertyEditor.TAGEDIT_DATA)) {
            // Read data from extras
            PropertyEditorData[] result = PropertyEditorData.deserializeArray(b.getSerializable(PropertyEditor.TAGEDIT_DATA));
            // FIXME Problem saved data may not be read at this point, load here, probably we should load editing state
            // too
            synchronized (loadOnResumeLock) {
                if (loadOnResume) {
                    loadOnResume = false;
                    Log.d(DEBUG_TAG, "handlePropertyEditorResult loading data");
                    logic.syncLoadFromFile(this); // sync load
                    App.getTaskStorage().readFromFile(this);
                }
            }
            for (PropertyEditorData editorData : result) {
                if (editorData == null) {
                    Log.d(DEBUG_TAG, "handlePropertyEditorResult null result");
                    continue;
                }
                if (editorData.tags != null) {
                    Log.d(DEBUG_TAG, "handlePropertyEditorResult setting tags");
                    try {
                        logic.setTags(this, editorData.type, editorData.osmId, editorData.tags);
                    } catch (OsmIllegalOperationException e) {
                        Snack.barError(this, e.getMessage());
                    }
                }
                if (editorData.parents != null) {
                    Log.d(DEBUG_TAG, "handlePropertyEditorResult setting parents");
                    logic.updateParentRelations(this, editorData.type, editorData.osmId, editorData.parents);
                }
                if (editorData.members != null && editorData.type.equals(Relation.NAME)) {
                    Log.d(DEBUG_TAG, "handlePropertyEditorResult setting members");
                    logic.updateRelation(this, editorData.osmId, editorData.members);
                }
            }
            // this is very expensive: getLogic().saveAsync(); // if nothing was changed the dirty flag wont be set and
            // the save wont actually happen
        }
        if ((logic.getMode().elementsGeomEditiable() && easyEditManager != null && !easyEditManager.isProcessingAction())
                || logic.getMode() == Mode.MODE_TAG_EDIT) {
            // not in an easy edit mode, de-select objects avoids inconsistent visual state
            logic.deselectAll();
        } else {
            // invalidate the action mode menu ... updates the state of the undo button
            // for visual feedback reasons we leave selected elements selected (tag edit mode)
            supportInvalidateOptionsMenu();
            if (easyEditManager != null) {
                easyEditManager.invalidate();
            }
        }
        map.invalidate();
    }

    /**
     * Restore the file name for a photograph
     * 
     * @param savedImageFileName Image file name.
     */
    public void setImageFileName(String savedImageFileName) {
        if (savedImageFileName != null) {
            Log.d(DEBUG_TAG, "setting imageFIleName to " + savedImageFileName);
            imageFile = new File(savedImageFileName);
        }
    }

    /**
     * Return the file name for a photograph
     * 
     * @return Image file name.
     */
    public String getImageFileName() {
        if (imageFile != null) {
            return imageFile.getAbsolutePath();
        }
        return null;
    }

    @Override
    public void onLowMemory() {
        Log.d(DEBUG_TAG, "onLowMemory");
        super.onLowMemory();
        map.onLowMemory();
    }

    /**
     * TODO: put this in Logic!!! Checks if a serialized {@link StorageDelegator} file is available.
     * 
     * @return true, when the file is available, otherwise false.
     */
    private boolean isLastActivityAvailable() {
        FileInputStream in = null;
        try {
            in = openFileInput(StorageDelegator.FILENAME);
            return true;
        } catch (final FileNotFoundException e) {
            return false;
        } finally {
            SavingHelper.close(in);
        }
    }

    public static void performCurrentViewHttpLoad(final Main main, boolean add) {
        App.getLogic().downloadCurrent(main, add);
        Preferences prefs = main.prefs;
        if (prefs.areBugsEnabled()) { // always adds bugs for now
            final Map map = main.getMap();
            TransferTasks.downloadBox(main, prefs.getServer(), map.getViewBox().copy(), true, new PostAsyncActionHandler() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSuccess() {
                    map.invalidate();
                }

                @Override
                public void onError() {
                }
            });
        }
    }

    private void performHttpLoad(final BoundingBox box) {
        App.getLogic().downloadBox(this, box, false, null);
    }

    private void openEmptyMap(final BoundingBox box) {
        App.getLogic().newEmptyMap(this, box);
    }

    /**
     * Upload changes to the OSM data and tasks to the API, if there are changes
     * 
     * @param comment Textual comment associated with the change set.
     * @param source Source of the change.
     * @param closeChangeset Boolean flag indicating whether the change set should be closed or kept open.
     */
    public void performUpload(final String comment, final String source, final boolean closeChangeset) {
        final Logic logic = App.getLogic();
        final Server server = prefs.getServer();

        if (server != null && server.isLoginSet()) {
            boolean hasDataChanges = logic.hasChanges();
            boolean hasBugChanges = !App.getTaskStorage().isEmpty() && App.getTaskStorage().hasChanges();
            if (hasDataChanges || hasBugChanges) {
                if (hasDataChanges) {
                    logic.upload(this, comment, source, closeChangeset);
                }
                if (hasBugChanges) {
                    TransferTasks.upload(this, server, null);
                }
                logic.checkForMail(this);
            } else {
                Snack.barInfo(this, R.string.toast_no_changes);
            }
        } else {
            ErrorAlert.showDialog(this, ErrorCodes.NO_LOGIN_DATA);
        }
    }

    /**
     * Check login parameters and start the track upload
     * 
     * @param description OSM GPX API description value
     * @param tags OSM GPX API tags
     * @param visibility OSM GPX API visibility value
     */
    public void performTrackUpload(final String description, final String tags, final Visibility visibility) {

        final Logic logic = App.getLogic();
        final Server server = prefs.getServer();

        if (server != null && server.isLoginSet()) {
            logic.uploadTrack(this, getTracker().getTrack(), description, tags, visibility);
            logic.checkForMail(this);
        } else {
            ErrorAlert.showDialog(this, ErrorCodes.NO_LOGIN_DATA);
        }
    }

    /**
     * Check if there are changes present and then show the upload dialog, getting authorisation if necessary
     */
    public void confirmUpload() {
        final Server server = prefs.getServer();

        if (server != null && server.isLoginSet()) {
            if (App.getLogic().hasChanges()) {
                if (server.needOAuthHandshake()) {
                    oAuthHandshake(server, new PostAsyncActionHandler() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onSuccess() {
                            ConfirmUpload.showDialog(Main.this);
                        }

                        @Override
                        public void onError() {
                        }
                    });
                    if (server.getOAuth()) // if still set
                        Snack.barError(this, R.string.toast_oauth);
                    return;
                }
                ConfirmUpload.showDialog(Main.this);
            } else {
                Snack.barInfo(this, R.string.toast_no_changes);
            }
        } else {
            ErrorAlert.showDialog(this, ErrorCodes.NO_LOGIN_DATA);
        }
    }

    public void hideBottomBar() {
        ActionMenuView bottomToolbar = getBottomBar();
        if (bottomToolbar != null) {
            bottomToolbar.setVisibility(View.GONE);
        }
    }

    public void showBottomBar() {
        ActionMenuView bottomToolbar = getBottomBar();
        if (bottomToolbar != null) {
            bottomToolbar.setVisibility(View.VISIBLE);
        }
    }

    public void hideLock() {
        FloatingActionButton lock = getLock();
        if (lock != null) {
            lock.hide();
        }
    }

    public void showLock() {
        FloatingActionButton lock = getLock();
        if (lock != null) {
            lock.show();
        }
    }

    private void hideControls() {
        synchronized(controlsHiddenLock) {
            ActionBar actionbar = getSupportActionBar();
            if (actionbar != null) {
                actionbar.hide();
            }
            hideBottomBar();
            hideLock();
            ZoomControls zoomControls = getControls();
            if (zoomControls != null) {
                zoomControls.hide();
            }
            hideFollowButton();
            if (App.getLogic().getFilter() != null) {
                App.getLogic().getFilter().hideControls();
            }
            controlsHidden = true;
        }
    }

    private void showControls() {
        synchronized(controlsHiddenLock) {
            ActionBar actionbar = getSupportActionBar();
            if (actionbar != null && !prefs.splitActionBarEnabled()) {
                actionbar.show();
            }
            showBottomBar();
            showLock();
            ZoomControls zoomControls = getControls();
            if (zoomControls != null) {
                zoomControls.show();
            }
            showFollowButton();
            if (App.getLogic().getFilter() != null) {
                App.getLogic().getFilter().showControls();
            }
            controlsHidden = false;
        }
    }

    /**
     * @param server Server properties.
     * @param restart Handler to be executed after asynchronous action have been performed.
     */
    @SuppressLint({ "SetJavaScriptEnabled", "InlinedApi", "NewApi" })
    public void oAuthHandshake(Server server, PostAsyncActionHandler restart) {
        descheduleAutoLock();
        this.restart = restart;
        hideControls();

        String url = Server.getBaseUrl(server.getReadWriteUrl());
        OAuthHelper oa;
        try {
            oa = new OAuthHelper(url);
        } catch (OsmException oe) {
            server.setOAuth(false); // ups something went wrong turn oauth off
            showControls();
            Snack.barError(this, R.string.toast_no_oauth);
            return;
        }
        Log.d(DEBUG_TAG, "oauth auth url " + url);

        String authUrl = null;
        String errorMessage = null;
        try {
            authUrl = oa.getRequestToken();
        } catch (OAuthException e) {
            errorMessage = OAuthHelper.getErrorMessage(this, e);
        } catch (InterruptedException e) {
            errorMessage = getString(R.string.toast_oauth_communication);
        } catch (ExecutionException e) {
            errorMessage = getString(R.string.toast_oauth_communication);
        } catch (TimeoutException e) {
            errorMessage = getString(R.string.toast_oauth_timeout);
        }
        if (authUrl == null) {
            Snack.barError(this, errorMessage);
            showControls();
            return;
        }
        Log.d(DEBUG_TAG, "authURl " + authUrl);
        synchronized (oAuthWebViewLock) {
            oAuthWebView = new WebView(this);
            mapLayout.addView(oAuthWebView);
            oAuthWebView.getSettings().setJavaScriptEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                oAuthWebView.getSettings().setAllowContentAccess(true);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                oAuthWebView.getLayoutParams().height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                oAuthWebView.getLayoutParams().width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            }
            oAuthWebView.requestFocus(View.FOCUS_DOWN);
            class MyWebViewClient extends WebViewClient {
                Object   progressLock  = new Object();
                boolean  progressShown = false;
                Runnable dismiss       = new Runnable() {
                                           @Override
                                           public void run() {
                                               Progress.dismissDialog(Main.this, Progress.PROGRESS_OAUTH);
                                           }
                                       };

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (!url.contains("vespucci")) {
                        // load in in this webview
                        view.loadUrl(url);
                    } else {
                        // vespucci URL
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                    return true;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    synchronized (progressLock) {
                        if (!progressShown) {
                            progressShown = true;
                            Progress.showDialog(Main.this, Progress.PROGRESS_OAUTH);
                        }
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    synchronized (progressLock) {
                        synchronized (oAuthWebViewLock) {
                            if (progressShown && oAuthWebView != null) {
                                oAuthWebView.removeCallbacks(dismiss);
                                oAuthWebView.postDelayed(dismiss, 500);
                            }
                        }
                    }
                }
                
                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    finishOAuth();
                    Snack.toastTopError(view.getContext(), description);
                }

                @TargetApi(android.os.Build.VERSION_CODES.M)
                @Override
                public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                    // Redirect to deprecated method, so you can use it in all SDK versions
                    onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
                }
            }

            oAuthWebView.setWebViewClient(new MyWebViewClient());
            oAuthWebView.loadUrl(authUrl);
        }
    }

    /**
     * Remove the OAuth webview
     */
    public void finishOAuth() {
        Log.d(DEBUG_TAG, "finishOAuth");
        synchronized (oAuthWebViewLock) {
            if (oAuthWebView != null) {
                mapLayout.removeView(oAuthWebView);
                showControls();
                try {
                    // the below loadUrl, even though the "official" way to do it,
                    // seems to be prone to crash on some devices.
                    oAuthWebView.loadUrl("about:blank"); // workaround clearView issues
                    oAuthWebView.setVisibility(View.GONE);
                    oAuthWebView.removeAllViews();
                    oAuthWebView.destroy();
                    oAuthWebView = null;
                    if (restart != null) {
                        restart.onSuccess();
                    }
                } catch (Exception ex) {
                    ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                    ACRA.getErrorReporter().handleException(ex);
                }
            }
        }
    }

    /**
     * Starts the LocationPicker activity for requesting a location.
     */
    public void gotoBoxPicker() {
        descheduleAutoLock();
        Intent intent = new Intent(this, BoxPicker.class);
        if (App.getLogic().hasChanges()) {
            DataLossActivity.showDialog(this, intent, REQUEST_BOUNDING_BOX);
        } else {
            startActivityForResult(intent, REQUEST_BOUNDING_BOX);
        }
    }

    /**
     * Start the PropertyEditor for the element in question, single element version
     * 
     * @param selectedElement Selected OpenStreetMap element.
     * @param focusOn if not null focus on the value field of this key.
     * @param applyLastAddressTags add address tags to the object being edited.
     * @param showPresets show the preset tab on start up.
     * @param askForName ask for a value for the name tag
     */
    public void performTagEdit(final OsmElement selectedElement, String focusOn, boolean applyLastAddressTags, boolean showPresets, boolean askForName) {
        descheduleAutoLock();
        final Logic logic = App.getLogic();
        logic.deselectAll();
        if (selectedElement instanceof Node) {
            logic.setSelectedNode((Node) selectedElement);
        } else if (selectedElement instanceof Way) {
            logic.setSelectedWay((Way) selectedElement);
        } else if (selectedElement instanceof Relation) {
            logic.setSelectedRelation((Relation) selectedElement);
        }

        if (selectedElement != null) {
            StorageDelegator storageDelegator = App.getDelegator();
            if (storageDelegator.getOsmElement(selectedElement.getName(), selectedElement.getOsmId()) != null) {
                PropertyEditorData[] single = new PropertyEditorData[1];
                single[0] = new PropertyEditorData(selectedElement, focusOn);
                PropertyEditor.startForResult(this, single, applyLastAddressTags, showPresets, askForName, logic.getMode().getExtraTags(logic, selectedElement),
                        logic.getMode().getPresetItems(this, selectedElement), REQUEST_EDIT_TAG);
            }
        }
    }

    /**
     * Start the PropertyEditor for the element in question, multiple element version
     * 
     * @param selection list of selected elements
     * @param applyLastAddressTags add address tags to the object being edited.
     * @param showPresets show the preset tab on start up.
     */
    public void performTagEdit(final ArrayList<OsmElement> selection, boolean applyLastAddressTags, boolean showPresets) {
        descheduleAutoLock();
        ArrayList<PropertyEditorData> multiple = new ArrayList<>();
        StorageDelegator storageDelegator = App.getDelegator();
        for (OsmElement e : selection) {
            if (storageDelegator.getOsmElement(e.getName(), e.getOsmId()) != null) {
                multiple.add(new PropertyEditorData(e, null));
            }
        }
        if (multiple.isEmpty()) {
            Log.d(DEBUG_TAG, "performTagEdit no valid elements");
            return;
        }
        PropertyEditorData[] multipleArray = multiple.toArray(new PropertyEditorData[multiple.size()]);
        PropertyEditor.startForResult(this, multipleArray, applyLastAddressTags, showPresets, false, null, null, REQUEST_EDIT_TAG);
    }

    /**
     * Edit an OpenStreetBug (now called a Task)
     * 
     * @param bug The bug to edit.
     */
    private void performBugEdit(@NonNull final Task bug) {
        Log.d(DEBUG_TAG, "editing bug:" + bug);
        descheduleAutoLock();
        App.getLogic().setSelectedBug(bug);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("fragment_bug");
        try {
            if (prev != null) {
                ft.remove(prev);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "performBugEdit removing dialog ", isex);
        }
        TaskFragment bugDialog = TaskFragment.newInstance(bug);
        try {
            bugDialog.show(fm, "fragment_bug");
        } catch (IllegalStateException isex) {
            // FIXME properly
            Log.e(DEBUG_TAG, "performBugEdit showing dialog ", isex);
        }
    }

    /**
     * potentially do some special stuff for invoking undo and exiting
     */
    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        Log.d(DEBUG_TAG, "onBackPressed()");
        synchronized (oAuthWebViewLock) {
            if (oAuthWebView != null && oAuthWebView.canGoBack()) {
                // we are displaying the oAuthWebView and somebody might want to navigate back
                oAuthWebView.goBack();
                return;
            }
        }
        if (prefs.useBackForUndo()) {
            String name = App.getLogic().undo();
            if (name != null) {
                Snack.barInfo(this, getResources().getString(R.string.undo) + ": " + name);
            } else {
                exitOnBackPressed();
            }
        } else {
            exitOnBackPressed();
        }
    }

    /**
     * pop up a dialog asking for confirmation and exit
     */
    private void exitOnBackPressed() {
        new AlertDialog.Builder(this).setTitle(R.string.exit_title).setMessage(R.string.exit_text).setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // if we actually exit, stop the auto downloads, for now allow GPS tracks to carry on
                        if (getTracker() != null) {
                            getTracker().stopAutoDownload();
                            getTracker().stopBugAutoDownload();
                        }
                        try {
                            saveSync = true;
                            Main.super.onBackPressed();
                        } catch (Exception e) {
                            // silently ignore .. might be Android confusion
                        }
                    }
                }).create().show();
    }

    private boolean actionResult = false;

    /**
     * catch back button in action modes where onBackPressed is not invoked this is probably not guaranteed to work
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (easyEditManager != null && easyEditManager.isProcessingAction()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    Log.d(DEBUG_TAG, "calling handleBackPressed");
                    actionResult = easyEditManager.handleBackPressed();
                    return actionResult;
                } else { // note to avoid tons of error messages we need to consume both events
                    return actionResult;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public class UndoListener implements OnClickListener, OnLongClickListener {

        private final String DEBUG_TAG = UndoListener.class.getName();

        @Override
        public void onClick(View arg0) {
            Log.d(DEBUG_TAG, "normal click");
            final Logic logic = App.getLogic();
            String name = logic.undo();
            if (name != null) {
                Snack.toastTopInfo(Main.this, getResources().getString(R.string.undo) + ": " + name);
            } else {
                Snack.toastTopInfo(Main.this, R.string.undo_nothing);
            }
            resync(logic);
            map.invalidate();
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(DEBUG_TAG, "long click");
            final Logic logic = App.getLogic();
            UndoStorage undo = logic.getUndo();
            if (undo.canUndo() || undo.canRedo()) {
                UndoDialogFactory.showUndoDialog(Main.this, logic, undo);
            } else {
                Snack.barInfoShort(Main.this, R.string.undo_nothing);
            }
            resync(logic);
            map.invalidate();
            return true;
        }

        void resync(final Logic logic) {
            // check that we haven't just removed a selected element
            if (logic.resyncSelected()) {
                // only need to test if anything at all is still selected
                if (logic.selectedNodesCount() + logic.selectedWaysCount() + logic.selectedRelationsCount() == 0) {
                    getEasyEditManager().finish();
                }
            }
        }
    }

    /**
     * A TouchListener for all gestures made on the touchscreen.
     * 
     * @author mb
     */
    private class MapTouchListener
            implements OnTouchListener, VersionedGestureDetector.OnGestureListener, OnCreateContextMenuListener, OnMenuItemClickListener {

        private List<OsmElement> clickedNodesAndWays;
        private List<Task>       clickedBugs;
        private List<Photo>      clickedPhotos;

        private boolean doubleTap = false;

        @Override
        public boolean onTouch(final View v, final MotionEvent m) {
            descheduleAutoLock();
            // Log.d("MapTouchListener", "onTouch");
            if (m.getAction() == MotionEvent.ACTION_DOWN) {
                // Log.d("MapTouchListener", "onTouch ACTION_DOWN");
                clickedBugs = null;
                clickedPhotos = null;
                clickedNodesAndWays = null;
                App.getLogic().handleTouchEventDown(Main.this, m.getX(), m.getY());
            }
            if (m.getAction() == MotionEvent.ACTION_UP) {
                App.getLogic().handleTouchEventUp(m.getX(), m.getY());
                scheduleAutoLock();
            }
            mDetector.onTouchEvent(v, m);
            return v.onTouchEvent(m);
        }

        @Override
        public void onDown(View v, float x, float y) {
        }

        @Override
        public void onClick(View v, float x, float y) {
            de.blau.android.tasks.MapOverlay osbo = map.getOpenStreetBugsOverlay();
            clickedBugs = (osbo != null) ? osbo.getClickedTasks(x, y, map.getViewBox()) : null;

            de.blau.android.photos.MapOverlay photos = map.getPhotosOverlay();
            clickedPhotos = (photos != null) ? photos.getClickedPhotos(x, y, map.getViewBox()) : null;

            final Logic logic = App.getLogic();
            Mode mode = logic.getMode();
            boolean isInEditZoomRange = logic.isInEditZoomRange();

            if (isInEditZoomRange) {
                if (logic.isLocked()) {
                    if (NetworkStatus.isConnected(Main.this) && prefs.voiceCommandsEnabled()) {
                        locationForIntent = lastLocation; // location when we touched the screen
                        startVoiceRecognition();
                    } else {
                        Snack.barInfoShort(Main.this, R.string.toast_unlock_to_edit);
                    }
                } else {
                    if (mode.elementsEditable()) {
                        performEdit(mode, v, x, y);
                    }
                }
                map.invalidate();
            } else {
                switch (((clickedBugs == null) ? 0 : clickedBugs.size()) + ((clickedPhotos == null) ? 0 : clickedPhotos.size())) {
                case 0:
                    if (!isInEditZoomRange && !logic.isLocked()) {
                        Snack.barInfoShort(v, R.string.toast_not_in_edit_range);
                    }
                    break;
                case 1:
                    if ((clickedBugs != null) && !clickedBugs.isEmpty())
                        performBugEdit(clickedBugs.get(0));
                    else if ((clickedPhotos != null) && !clickedPhotos.isEmpty())
                        viewPhoto(clickedPhotos.get(0));
                    break;
                default:
                    v.showContextMenu();
                    break;
                }
            }
        }

        @SuppressLint("InlinedApi")
        private void viewPhoto(Photo photo) {
            try {
                Intent myIntent = new Intent(Intent.ACTION_VIEW);
                int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK;
                }
                myIntent.setFlags(flags);
                Uri photoUri = photo.getRefUri(Main.this);
                if (photoUri != null) {
                    myIntent.setDataAndType(photoUri, "image/jpeg"); // black magic only works this way
                    startActivity(myIntent);
                    map.getPhotosOverlay().setSelected(photo);
                    // TODO may need a map.invalidate() here
                } else {
                    Snack.barError(Main.this, Main.this.getResources().getString(R.string.toast_error_accessing_photo, photo.getRef()));
                }
            } catch (Exception ex) {
                Log.d(DEBUG_TAG, "viewPhoto exception starting intent: " + ex);
                ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                ACRA.getErrorReporter().handleException(ex);
            }
        }

        @Override
        public void onUp(View v, float x, float y) {
            if (App.getLogic().getMode().elementsGeomEditiable()) {
                getEasyEditManager().invalidate();
            }
        }

        @Override
        public boolean onLongClick(final View v, final float x, final float y) {
            final Logic logic = App.getLogic();
            if (logic.isLocked()) {
                if (logic.getMode().elementsGeomEditiable()) {
                    // display context menu
                    de.blau.android.tasks.MapOverlay osbo = map.getOpenStreetBugsOverlay();
                    clickedBugs = (osbo != null) ? osbo.getClickedTasks(x, y, map.getViewBox()) : null;
                    de.blau.android.photos.MapOverlay photos = map.getPhotosOverlay();
                    clickedPhotos = (photos != null) ? photos.getClickedPhotos(x, y, map.getViewBox()) : null;
                    clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
                    int bugCount = clickedBugs != null ? clickedBugs.size() : 0;
                    int photoCount = clickedPhotos != null ? clickedPhotos.size() : 0;
                    int elementCount = clickedNodesAndWays != null ? clickedNodesAndWays.size() : 0;
                    int itemCount = bugCount + photoCount + elementCount;
                    if (itemCount == 1) {
                        if (photoCount == 1) {
                            viewPhoto(clickedPhotos.get(0));
                        } else if (bugCount == 1) {
                            performBugEdit(clickedBugs.get(0));
                        } else if (elementCount == 1) {
                            ElementInfo.showDialog(Main.this, clickedNodesAndWays.get(0));
                        }
                    } else if (itemCount > 0) {
                        v.showContextMenu();
                    }
                    return true;
                } else {
                    // other modes
                    return false; // ignore long clicks
                }
            }

            if (logic.isInEditZoomRange()) {
                setFollowGPS(false); // editing with the screen moving under you is a pain
                return getEasyEditManager().handleLongClick(v, x, y);
            } else {
                Snack.barWarningShort(Main.this, R.string.toast_not_in_edit_range);
            }

            return true; // long click handled
        }

        @Override
        public void onDrag(View v, float x, float y, float dx, float dy) {
            // Log.d("MapTouchListener", "onDrag dx " + dx + " dy " + dy );
            App.getLogic().handleTouchEventMove(Main.this, x, y, -dx, dy);
            setFollowGPS(false);
        }

        @Override
        public void onScale(View v, float scaleFactor, float prevSpan, float curSpan) {
            App.getLogic().zoom((curSpan - prevSpan) / prevSpan);
            setFollowGPS(followGPS);
            updateZoomControls();
        }

        /**
         * Perform edit touch processing.
         * 
         * @param mode mode we are in, either EASYEDIT or TAG_EDIT
         * @param v View affected by the touch event.
         * @param x the click-position on the display.
         * @param y the click-position on the display.
         */
        public void performEdit(Mode mode, final View v, final float x, final float y) {
            if (!getEasyEditManager().actionModeHandledClick(x, y)) {
                clickedNodesAndWays = App.getLogic().getClickedNodesAndWays(x, y);
                Logic logic = App.getLogic();
                Filter filter = logic.getFilter();
                if (filter != null) { // filter elements
                    clickedNodesAndWays = filterElements(clickedNodesAndWays);
                }
                boolean inEasyEditMode = logic.getMode().elementsGeomEditiable();
                switch (((clickedBugs == null) ? 0 : clickedBugs.size()) + clickedNodesAndWays.size() + ((clickedPhotos == null) ? 0 : clickedPhotos.size())) {
                case 0:
                    // no elements were touched
                    if (inEasyEditMode) {
                        getEasyEditManager().nothingTouched(false);
                    }
                    break;
                case 1:
                    // exactly one element touched
                    if (clickedBugs != null && clickedBugs.size() == 1) {
                        performBugEdit(clickedBugs.get(0));
                    } else if (clickedPhotos != null && clickedPhotos.size() == 1) {
                        viewPhoto(clickedPhotos.get(0));
                    } else {
                        if (inEasyEditMode) {
                            getEasyEditManager().editElement(clickedNodesAndWays.get(0));
                        } else {
                            performTagEdit(clickedNodesAndWays.get(0), null, false, false, false);
                        }
                    }
                    break;
                default:
                    // multiple possible elements touched - show menu
                    if (menuRequired()) {
                        v.showContextMenu();
                    } else {
                        // menuRequired tells us it's ok to just take the first one
                        if (inEasyEditMode) {
                            getEasyEditManager().editElement(clickedNodesAndWays.get(0));
                        } else {
                            performTagEdit(clickedNodesAndWays.get(0), null, false, false, false);
                        }
                    }
                    break;
                }
            }
        }

        /**
         * Filter for elements
         * 
         * NOTE expensive for a large number of elements
         * 
         * @param elements List of elements to filter
         * @return List of elements that have passed the filter
         */
        private ArrayList<OsmElement> filterElements(List<OsmElement> elements) {
            ArrayList<OsmElement> tmp = new ArrayList<>();
            Logic logic = App.getLogic();
            Filter filter = logic.getFilter();
            for (OsmElement e : elements) {
                if (filter.include(e, false)) {
                    tmp.add(e);
                }
            }
            return tmp;
        }

        @Override
        public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
            if (getEasyEditManager().needsCustomContextMenu()) {
                getEasyEditManager().createContextMenu(menu);
            } else {
                onCreateDefaultContextMenu(menu);
            }
        }

        /**
         * Creates a context menu with the objects near where the screen was touched
         * 
         * @param menu
         */
        public void onCreateDefaultContextMenu(final ContextMenu menu) {
            int id = 0;
            if (clickedPhotos != null) {
                for (Photo p : new ArrayList<>(clickedPhotos)) {
                    Uri photoUri = p.getRefUri(Main.this);
                    if (photoUri != null) {
                        menu.add(Menu.NONE, id++, Menu.NONE, photoUri.getLastPathSegment()).setOnMenuItemClickListener(this);
                    } else {
                        // remove photos with failed Uri generation from the list
                        clickedPhotos.remove(p);
                    }
                }
            }
            if (clickedBugs != null) {
                for (Task b : clickedBugs) {
                    menu.add(Menu.NONE, id++, Menu.NONE, b.getDescription()).setOnMenuItemClickListener(this);
                }
            }
            if (clickedNodesAndWays != null) {
                Logic logic = App.getLogic();
                for (OsmElement e : clickedNodesAndWays) {
                    StringBuilder description = new StringBuilder(e.getDescription(Main.this));
                    List<Relation> relations = e.getParentRelations();
                    boolean hasRelations = relations != null && !relations.isEmpty();
                    if (e instanceof Node) {
                        List<Way> ways = App.getLogic().getWaysForNode((Node) e);
                        boolean hasWays = ways != null && !ways.isEmpty();
                        if (hasRelations) {
                            description.append(" (");
                            for (Relation r : relations) {
                                description.append(r.getDescription(Main.this));
                                if (!lastMember(relations, r) || hasWays) {
                                    description.append(", ");
                                }
                            }
                            if (!hasWays) {
                                description.append(")");
                            }
                        }
                        if (hasWays) {
                            if (!hasRelations) {
                                description.append(" (");
                            }
                            for (Way w : ways) {
                                description.append(w.getDescription(Main.this));
                                if (!lastMember(ways, w)) {
                                    description.append(", ");
                                }
                            }
                            description.append(")");
                        }
                    } else if (hasRelations) {
                        description.append(" (");
                        for (Relation r : relations) {
                            description.append(r.getDescription(Main.this));
                            if (!lastMember(relations, r)) {
                                description.append(", ");
                            }
                        }
                        description.append(")");
                    }
                    if (logic.isSelected(e)) {
                        SpannableString s = new SpannableString(description);
                        s.setSpan(new ForegroundColorSpan(ThemeUtils.getStyleAttribColorValue(Main.this, R.attr.colorAccent, 0)), 0, s.length(), 0);
                        menu.add(Menu.NONE, id++, Menu.NONE, s).setOnMenuItemClickListener(this);
                    } else {
                        menu.add(Menu.NONE, id++, Menu.NONE, description).setOnMenuItemClickListener(this);
                    }
                }
            }
        }

        /**
         * Check if this is the last member of a list
         * 
         * @param <T>
         * @param l the list
         * @param o the member we are checking
         * @return true if it is the last item in the list
         */
        private <T> boolean lastMember(@NonNull List<T> l, @NonNull T o) {
            return l.indexOf(o) == (l.size() - 1);
        }

        /**
         * Checks if a menu should be shown based on clickedNodesAndWays and clickedBugs. ClickedNodesAndWays needs to
         * contain nodes first, then ways, ordered by distance from the click. Assumes multiple elements have been
         * clicked, i.e. a choice is necessary unless heuristics work.
         */
        private boolean menuRequired() {
            // If the context menu setting requires the menu, show it instead of guessing.
            if (prefs.getForceContextMenu())
                return true;

            // If bugs are clicked, user should always choose
            if (clickedBugs != null && !clickedBugs.isEmpty()) {
                return true;
            }

            // If photos are clicked, user should always choose
            if (clickedPhotos != null && !clickedPhotos.isEmpty()) {
                return true;
            }

            if (clickedNodesAndWays.size() < 2) {
                Log.e(DEBUG_TAG, "WTF? menuRequired called for single item?");
                return true;
            }

            // No bugs were clicked. Do we have nodes?
            if (clickedNodesAndWays.get(0) instanceof Node) {
                // Usually, we just take the first node.
                // However, check for *very* closely overlapping nodes first.
                Node candidate = (Node) clickedNodesAndWays.get(0);
                if (candidate.hasParentRelations()) {
                    return true; // otherwise a relation that only has nodes as member is not selectable
                }
                final Logic logic = App.getLogic();
                float nodeX = logic.getNodeScreenX(candidate);
                float nodeY = logic.getNodeScreenY(candidate);
                for (int i = 1; i < clickedNodesAndWays.size(); i++) {
                    if (!(clickedNodesAndWays.get(i) instanceof Node))
                        break;
                    Node possibleNeighbor = (Node) clickedNodesAndWays.get(i);
                    float node2X = logic.getNodeScreenX(possibleNeighbor);
                    float node2Y = logic.getNodeScreenY(possibleNeighbor);
                    // Fast "square" checking is good enough
                    if (Math.abs(nodeX - node2X) < DataStyle.NODE_OVERLAP_TOLERANCE_VALUE
                            || Math.abs(nodeY - node2Y) < DataStyle.NODE_OVERLAP_TOLERANCE_VALUE) {
                        // The first node has an EXTREMELY close neighbour. Show context menu
                        return true;
                    }
                }
                return false; // no colliding neighbours found
            }

            // No nodes means we have at least two ways. Since the tolerance for ways is tiny, show the menu.
            return true;
        }

        @Override
        public boolean onMenuItemClick(final android.view.MenuItem item) {
            int itemId = item.getItemId();
            int bugsItemId = itemId - ((clickedPhotos == null) ? 0 : clickedPhotos.size());
            if ((clickedPhotos != null) && (itemId < clickedPhotos.size())) {
                viewPhoto(clickedPhotos.get(itemId));
            } else if (clickedBugs != null && bugsItemId >= 0 && bugsItemId < clickedBugs.size()) {
                performBugEdit(clickedBugs.get(bugsItemId));
            } else {
                // this is dependent on which order items where added to the context menu
                itemId -= (((clickedBugs == null) ? 0 : clickedBugs.size()) + ((clickedPhotos == null) ? 0 : clickedPhotos.size()));

                if ((itemId >= 0) && (clickedNodesAndWays != null) && (itemId < clickedNodesAndWays.size())) {
                    final OsmElement element = clickedNodesAndWays.get(itemId);
                    if (App.getLogic().isLocked()) {
                        ElementInfo.showDialog(Main.this, element);
                    } else {
                        Mode mode = App.getLogic().getMode();
                        if (mode.elementsGeomEditiable()) {
                            if (doubleTap) {
                                doubleTap = false;
                                getEasyEditManager().startExtendedSelection(element);
                            } else {
                                getEasyEditManager().editElement(element);
                            }
                        } else if (mode.elementsEditable()) {
                            performTagEdit(element, null, false, false, false);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(View v, float x, float y) {
            final Logic logic = App.getLogic();
            if (!logic.isLocked()) {
                boolean inEasyEditMode = logic.getMode().elementsGeomEditiable();
                clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
                switch (clickedNodesAndWays.size()) {
                case 0:
                    // no elements were touched
                    if (inEasyEditMode) {
                        getEasyEditManager().nothingTouched(true); // short cut to finishing multi-select
                    }
                    break;
                case 1:
                    if (inEasyEditMode) {
                        getEasyEditManager().startExtendedSelection(clickedNodesAndWays.get(0));
                    }
                    break;
                default:
                    // multiple possible elements touched - show menu
                    if (inEasyEditMode) {
                        if (menuRequired()) {
                            Log.d(DEBUG_TAG, "onDoubleTap displaying menu");
                            doubleTap = true; // ugly flag
                            v.showContextMenu();
                        } else {
                            // menuRequired tells us it's ok to just take the first one
                            getEasyEditManager().startExtendedSelection(clickedNodesAndWays.get(0));
                        }
                    }
                    break;
                }
            } else {
                Snack.barInfoShort(Main.this, R.string.toast_unlock_to_edit);
            }
            return true;
        }
    }

    /**
     * A KeyListener for all key events.
     * 
     * @author mb
     */
    public class MapKeyListener implements OnKeyListener {

        @SuppressLint("NewApi")
        @Override
        public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
            scheduleAutoLock();
            final Logic logic = App.getLogic();
            switch (event.getAction()) {
            case KeyEvent.ACTION_UP:
                if (!v.onKeyUp(keyCode, event)) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        // this stops the piercing beep related to volume adjustments
                        return true;
                    }
                }
                break;
            case KeyEvent.ACTION_DOWN:
                if (!v.onKeyDown(keyCode, event)) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        setFollowGPS(true);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        translate(Logic.CursorPaddirection.DIRECTION_UP);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        translate(Logic.CursorPaddirection.DIRECTION_DOWN);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        translate(Logic.CursorPaddirection.DIRECTION_LEFT);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        translate(Logic.CursorPaddirection.DIRECTION_RIGHT);
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_UP:
                    case KeyEvent.KEYCODE_SEARCH:
                        logic.zoom(Logic.ZOOM_IN);
                        updateZoomControls();
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        logic.zoom(Logic.ZOOM_OUT);
                        updateZoomControls();
                        return true;
                    default:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            Character c = Character.toLowerCase((char) event.getUnicodeChar());
                            if (c == Util.getShortCut(Main.this, R.string.shortcut_zoom_in)) {
                                logic.zoom(Logic.ZOOM_IN);
                                updateZoomControls();
                                return true;
                            } else if (c == Util.getShortCut(Main.this, R.string.shortcut_zoom_out)) {
                                logic.zoom(Logic.ZOOM_OUT);
                                updateZoomControls();
                                return true;
                            }
                            if (getEasyEditManager().isProcessingAction() && event.isCtrlPressed()) { // shortcuts not
                                                                                                      // supported in
                                                                                                      // action modes
                                                                                                      // arghhh
                                char shortcut = Character.toLowerCase((char) event.getUnicodeChar(0)); // get rid of
                                                                                                       // Ctrl key
                                if (getEasyEditManager().processShortcut(shortcut)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                break;
            }
            return false;
        }

        private void translate(final CursorPaddirection direction) {
            setFollowGPS(false);
            App.getLogic().translate(direction);
        }
    }

    /**
     * Mouse scroll wheel support
     * 
     * @author simon
     *
     */
    @SuppressLint("NewApi")
    private class MotionEventListener implements OnGenericMotionListener {
        @SuppressLint("NewApi")
        @Override
        public boolean onGenericMotion(View arg0, MotionEvent event) {
            final Logic logic = App.getLogic();
            if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL:
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        logic.zoom(Logic.ZOOM_IN);

                    } else {
                        logic.zoom(Logic.ZOOM_OUT);
                    }
                    updateZoomControls();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Invalidates (redraws) the map
     */
    public void invalidateMap() {
        map.invalidate();
    }

    public Map getMap() {
        return map;
    }

    public static boolean hasChanges() {
        final Logic logic = App.getLogic();
        // noinspection SimplifiableIfStatement
        if (logic == null)
            return false;
        return logic.hasChanges();
    }

    /**
     * Sets the activity to re-download the last downloaded area on startup (use e.g. when the API URL is changed)
     */
    public static void prepareRedownload() {
        redownloadOnResume = true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(DEBUG_TAG, "Tracker service connected");
        setTracker((((TrackerBinder) service).getService()));
        map.setTracker(getTracker());
        getTracker().setListener(this);
        getTracker().setListenerNeedsGPS(wantLocationUpdates);
        startStopAutoDownload();
        startStopBugAutoDownload();
        triggerMenuInvalidation();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // should never happen, but just to be sure
        Log.i(DEBUG_TAG, "Tracker service disconnected");
        setTracker(null);
        map.setTracker(null);
        triggerMenuInvalidation();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (followGPS) {
            BoundingBox viewBox = map.getViewBox();
            // ensure the view is zoomed in to at least the most zoomed-out
            while (!viewBox.canZoomOut() && viewBox.canZoomIn()) {
                viewBox.zoomIn();
            }
            // re-center on current position
            viewBox.moveTo(getMap(), (int) (location.getLongitude() * 1E7d), (int) (location.getLatitude() * 1E7d));
        }
        lastLocation = location;
        if (showGPS) {
            map.setLocation(location);
        }
        map.invalidate();
    }

    @Override
    public void onStateChanged() {
        supportInvalidateOptionsMenu();
    }

    /**
     * Simply calls {@link #invalidateOptionsMenu()}. MUST BE CALLED FROM THE MAIN/UI THREAD!
     */
    public void triggerMenuInvalidation() {
        Log.d(DEBUG_TAG, "triggerMenuInvalidation called");
        super.supportInvalidateOptionsMenu(); // TODO delay or make conditional to work around android bug?
    }

    /**
     * @return the backgroundAlignmentActionModeCallback
     */
    public BackgroundAlignmentActionModeCallback getBackgroundAlignmentActionModeCallback() {
        return backgroundAlignmentActionModeCallback;
    }

    /**
     * @return the tracker
     */
    public TrackerService getTracker() {
        return tracker;
    }

    /**
     * @param tracker the tracker to set
     */
    private void setTracker(TrackerService tracker) {
        this.tracker = tracker;
    }

    public void zoomToAndEdit(int lonE7, int latE7, OsmElement e) {
        Log.d(DEBUG_TAG, "zoomToAndEdit Zoom " + map.getZoomLevel());
        final Logic logic = App.getLogic();
        // if (logic.getMode()==Mode.MODE_MOVE) { // avoid switching to the wrong mode
        // FloatingActionButton lock = setLock(Mode.MODE_MOVE); // NOP to get button
        // if (EASY_TAG.equals(lock.getTag())) {
        // setLock(Mode.MODE_EASYEDIT);
        // } else if (INDOOR_TAG.equals(lock.getTag())) {
        // setLock(Mode.MODE_INDOOR);
        // } else{
        // setLock(Mode.MODE_TAG_EDIT);
        // }
        // }
        zoomTo(lonE7, latE7, e);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        logic.setSelectedRelation(null);
        switch (e.getType()) {
        case NODE:
            logic.setSelectedNode((Node) e);
            break;
        case WAY:
        case CLOSEDWAY:
            logic.setSelectedWay((Way) e);
            break;
        case RELATION:
            logic.setSelectedRelation((Relation) e);
            break;
        case AREA:
            if (Way.NAME.equals(e.getName())) {
                logic.setSelectedWay((Way) e);
            } else {
                logic.setSelectedRelation((Relation) e);
            }
            break;
        }
        if (logic.getMode().elementsGeomEditiable()) {
            easyEditManager.editElement(e);
            map.invalidate();
        } else { // tag edit mode
            performTagEdit(e, null, false, false, false);
        }
    }

    /**
     * Zoom to the coordinates and try and set the viewbox size to something reasonable
     * 
     * @param lonE7 longitude * 10E/
     * @param latE7 latitude " 10E/
     * @param e OsmElement we want to show
     */
    private void zoomTo(int lonE7, int latE7, OsmElement e) {
        setFollowGPS(false); // otherwise the screen could move around
        if (e instanceof Node && map.getZoomLevel() < ZOOM_FOR_ZOOMTO) {
            App.getLogic().setZoom(getMap(), ZOOM_FOR_ZOOMTO); // FIXME this doesn't seem to work as expected
        } else {
            map.getViewBox().setBorders(getMap(), e.getBounds(), false);
        }
        map.getViewBox().moveTo(getMap(), lonE7, latE7);
    }

    /**
     * Zoom to the element and try and set the viewbox size to something reasonable
     * 
     * @param e OsmElement we want to show
     */
    public void zoomTo(OsmElement e) {
        setFollowGPS(false); // otherwise the screen could move around
        if (e instanceof Node && map.getZoomLevel() < ZOOM_FOR_ZOOMTO) {
            App.getLogic().setZoom(getMap(), ZOOM_FOR_ZOOMTO); // FIXME this doesn't seem to work as expected
            map.getViewBox().moveTo(getMap(), ((Node) e).getLon(), ((Node) e).getLat());
        } else {
            map.getViewBox().setBorders(getMap(), e.getBounds(), false);
        }

    }

    @Override
    /**
     * Workaround for bug mentioned below
     */
    public ActionMode startSupportActionMode(@NonNull final ActionMode.Callback callback) {
        // Fix for bug https://code.google.com/p/android/issues/detail?id=159527
        final ActionMode mode = super.startSupportActionMode(callback);
        if (mode != null) {
            mode.invalidate();
        }
        return mode;
    }

    @Override
    // currently this is only called by the task UI
    public void update() {
        if (App.getLogic().getSelectedBug() != null) {
            App.getLogic().setSelectedBug(null);
        }
        map.invalidate();
    }

    /**
     * @return the bottomToolbar
     */
    public android.support.v7.widget.ActionMenuView getBottomBar() {
        return bottomBar;
    }

    /**
     * @param bottomBar the bottomToolbar to set
     */
    private void setBottomBar(android.support.v7.widget.ActionMenuView bottomBar) {
        MenuUtil.setupBottomBar(this, bottomBar, isFullScreen(), prefs.lightThemeEnabled());
        this.bottomBar = bottomBar;
    }

    /**
     * @return the view containing the zoom + and - buttons
     */
    private ZoomControls getControls() {
        return zoomControls;
    }

    /**
     * @return the "center on GPS position button"
     */
    private FloatingActionButton getFollowButton() {
        return follow;
    }

    /**
     * Display the "center on GPS position" button
     */
    private void hideFollowButton() {
        FloatingActionButton follow = getFollowButton();
        if (follow != null) {
            follow.hide();
        }
    }

    /**
     * Display the "center on GPS position" button, checks if GPS is actually on
     */
    private void showFollowButton() {
        FloatingActionButton follow = getFollowButton();
        if (follow != null && ensureGPSProviderEnabled() && locationPermissionGranted && !"NONE".equals(prefs.followGPSbuttonPosition())) {
            follow.show();
        }
    }

    /**
     * Lock screen if we are in a mode in which that can reasonably be done
     */
    private Runnable autoLock = new Runnable() {
        @Override
        public void run() {
            if (!App.getLogic().isLocked()) {
                if (!easyEditManager.isProcessingAction() || easyEditManager.inElementSelectedMode()) {
                    View lock = getLock();
                    if (lock != null) {
                        lock.performClick();
                    }
                    if (easyEditManager.inElementSelectedMode()) {
                        App.getLogic().deselectAll();
                        easyEditManager.finish();
                    }
                } else { // can't lock now, reschedule
                    if (prefs != null) {
                        int delay = prefs.getAutolockDelay();
                        if (delay > 0) {
                            map.postDelayed(autoLock, delay);
                        }
                    }
                }
            }
        }
    };

    /**
     * Schedule automatic locking of the screen in a configurable time in the future
     */
    void scheduleAutoLock() {
        map.removeCallbacks(autoLock);
        if (prefs != null) {
            int delay = prefs.getAutolockDelay();
            if (delay > 0) {
                map.postDelayed(autoLock, delay);
            }
        }
    }

    /**
     * Remove any pending automatic lock tasks
     */
    private void descheduleAutoLock() {
        map.removeCallbacks(autoLock);
    }

    public RelativeLayout getMapLayout() {
        return mapLayout;
    }

    public UndoListener getUndoListener() {
        return undoListener;
    }

    public EasyEditManager getEasyEditManager() {
        return easyEditManager;
    }

}
