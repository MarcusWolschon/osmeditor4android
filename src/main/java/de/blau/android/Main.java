package de.blau.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.LayerDrawable;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import de.blau.android.Logic.CursorPaddirection;
import de.blau.android.RemoteControlUrlActivity.RemoteControlUrlData;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Flavors;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Ui;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.ConfirmUpload;
import de.blau.android.dialogs.DataLossActivity;
import de.blau.android.dialogs.DownloadCurrentWithChanges;
import de.blau.android.dialogs.ElementInfo;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.GpxUpload;
import de.blau.android.dialogs.ImportTrack;
import de.blau.android.dialogs.Layers;
import de.blau.android.dialogs.NewVersion;
import de.blau.android.dialogs.Newbie;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.SearchForm;
import de.blau.android.dialogs.Tip;
import de.blau.android.dialogs.UndoDialog;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.SimpleActionModeCallback;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.filter.Filter;
import de.blau.android.filter.PresetFilter;
import de.blau.android.filter.TagFilter;
import de.blau.android.geocode.CoordinatesOrOLC;
import de.blau.android.geocode.Search.SearchResult;
import de.blau.android.imageryoffset.BackgroundAlignmentActionModeCallback;
import de.blau.android.imageryoffset.ImageryOffsetUtils;
import de.blau.android.javascript.EvalCallback;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.listener.UpdateViewListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Server;
import de.blau.android.osm.Server.Visibility;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.osm.Track.WayPoint;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.photos.PhotoIndex;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.propertyeditor.Address;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.propertyeditor.PropertyEditorData;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerDatabaseView;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.search.Search;
import de.blau.android.services.TrackerService;
import de.blau.android.services.TrackerService.TrackerBinder;
import de.blau.android.services.TrackerService.TrackerLocationListener;
import de.blau.android.tasks.MapRouletteApiKey;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.BadgeDrawable;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.DownloadActivity;
import de.blau.android.util.FileUtil;
import de.blau.android.util.FullScreenAppCompatActivity;
import de.blau.android.util.GeoMath;
import de.blau.android.util.GeoUrlData;
import de.blau.android.util.LatLon;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SaveFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Screen;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.Version;
import de.blau.android.views.ZoomControls;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.voice.Commands;

/**
 * This is the main Activity from where other Activities will be started.
 * 
 * @author mb
 * @author Simon Poole
 */
public class Main extends FullScreenAppCompatActivity
        implements ServiceConnection, TrackerLocationListener, UpdateViewListener, de.blau.android.geocode.SearchItemSelectedCallback, ActivityResultHandler {

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

    public static final String ACTION_EXIT   = "de.blau.android.EXIT";
    public static final String ACTION_UPDATE = "de.blau.android.UPDATE";

    /**
     * Alpha value for floating action buttons workaround We should probably find a better place for this
     */
    public static final float FABALPHA = 0.90f;

    /**
     * Date pattern used for the image file name.
     */
    private static final String DATE_PATTERN_IMAGE_FILE_NAME_PART = "yyyyMMdd_HHmmss";

    /**
     * Id for requesting permissions
     */
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 54321;

    /**
     * Minimum change in azimuth before we redraw
     */
    private static final int MIN_AZIMUT_CHANGE = 5;

    private class ConnectivityChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d("ConnectivityChanged...", "Received broadcast");
                if (getEasyEditManager().isProcessingAction()) {
                    getEasyEditManager().invalidate();
                } else {
                    invalidateOptionsMenu();
                }
            }
        }
    }

    private ConnectivityChangedReceiver connectivityChangedReceiver;

    /** Objects to handle showing device orientation. */
    private SensorManager sensorManager;
    @SuppressWarnings("unused")
    private Sensor        magnetometer;
    @SuppressWarnings("unused")
    private Sensor        accelerometer;
    private Sensor        rotation;

    /**
     * @see <a href=
     *      "https://web.archive.org/web/20110415003722/http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html">Using
     *      orientation sensors: Simple Compass sample</a>
     * @see <a href="https://www.deviantdev.com/journal/android-compass-azimuth-calculating">Android: Compass
     *      Implementation - Calculating the Azimuth</a>
     */
    private final SensorEventListener sensorListener = new SensorEventListener() {
        float   lastAzimut = -9999;
        @SuppressWarnings("unused")
        float[] acceleration;
        @SuppressWarnings("unused")
        float[] geomagnetic;
        float[] truncatedRotationVector;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // unused
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] orientation = new float[3];
            float[] rotationMatrix = new float[9];
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                if (event.values.length > 4) {
                    // See
                    // https://groups.google.com/forum/#!topic/android-developers/U3N9eL5BcJk
                    // for more information
                    // on this
                    //
                    // On some Samsung devices
                    // SensorManager.getRotationMatrixFromVector
                    // appears to throw an exception if rotation vector
                    // has
                    // length > 4.
                    // For the purposes of this class the first 4 values
                    // of the
                    // rotation vector are sufficient (see
                    // crbug.com/335298 for
                    // details).
                    if (truncatedRotationVector == null) {
                        truncatedRotationVector = new float[4];
                    }
                    System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, truncatedRotationVector);
                } else {
                    // calculate the rotation matrix
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                }
            }
            SensorManager.getOrientation(rotationMatrix, orientation);
            float azimut = (int) (Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360) % 360;
            map.setOrientation(azimut);
            // Repaint map only if orientation changed by at least MIN_AZIMUT_CHANGE
            // degrees since last repaint
            if (Math.abs(azimut - lastAzimut) > MIN_AZIMUT_CHANGE) {
                ViewBox viewBox = map.getViewBox();
                Location current = map.getLocation();
                // only invalidate if GPS position is or was in the ViewBox
                if (current == null || lastLocation == null || viewBox.contains(current.getLongitude(), current.getLatitude())
                        || viewBox.contains(lastLocation.getLongitude(), lastLocation.getLatitude())) {
                    lastAzimut = azimut;
                    map.invalidate();
                }
            }
        }
    };

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

    private GeoUrlData           geoData        = null;
    private final Object         geoDataLock    = new Object();
    private RemoteControlUrlData rcData         = null;
    private final Object         rcDataLock     = new Object();
    private Uri                  contentUri     = null;
    private String               contentUriType = null;
    private final Object         contentUriLock = new Object();

    /**
     * Optional bottom toolbar
     */
    private androidx.appcompat.widget.ActionMenuView bottomBar = null;

    /**
     * Layer control
     */
    private FloatingActionButton layers;

    /**
     * GPS FAB
     */
    private FloatingActionButton follow;

    /**
     * Simple actions FAB
     */
    private FloatingActionButton simpleActionsButton;

    /**
     * The current instance of the tracker service
     */
    private TrackerService tracker = null;

    private UndoListener undoListener;

    // hack to protect against weird state
    private BackgroundAlignmentActionModeCallback backgroundAlignmentActionModeCallback = null;

    private Location lastLocation = null;

    private Location locationForIntent = null;

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
     * 
     */
    private transient NetworkStatus networkStatus;

    /**
     * file we asked the camera app to create (ugly)
     */
    private File imageFile = null;

    // flag to ensure that we only check once per activity life cycle
    private boolean gpsChecked = false;

    // save synchronously instead of async
    private boolean saveSync = false;

    // true if we have a camera
    private boolean haveCamera = false;

    private boolean newInstall = false;
    private boolean newVersion = false;

    java.util.Map<Integer, ActivityResultHandler.Listener> activityResultListeners = new HashMap<>();

    private Runnable whenPermissionsGranted;

    private static final float LARGE_FAB_ELEVATION = 16; // used for renabling elevation on the FABs

    /**
     * {@inheritDoc}
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.i(DEBUG_TAG, "onCreate " + (savedInstanceState != null ? " no saved state " : " saved state exists"));
        getIntentData();
        App.initGeoContext(this);
        updatePrefs(new Preferences(this));

        int layout = R.layout.main;
        if (useFullScreen(prefs)) {
            Log.d(DEBUG_TAG, "using full screen layout");
            if (!statusBarHidden()) {
                layout = R.layout.main_fullscreen;
            }
        }
        if (prefs.lightThemeEnabled()) {
            if (statusBarHidden()) {
                setTheme(R.style.Theme_customMain_Light_FullScreen);
            } else {
                setTheme(R.style.Theme_customMain_Light);
            }
        } else {
            if (statusBarHidden()) {
                setTheme(R.style.Theme_customMain_FullScreen);
            }
        }

        super.onCreate(savedInstanceState);

        setScreenOrientation();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotation == null) {
                sensorManager = null;
            }
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
        map.setOnGenericMotionListener(new MotionEventListener());

        mapLayout.addView(map, 0); // index 0 so that anything in the layout
                                   // comes after it/on top

        mDetector = VersionedGestureDetector.newInstance(this, mapTouchListener);

        // follow GPS button setup
        follow = (FloatingActionButton) mapLayout.findViewById(R.id.follow);

        follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFollowGPS(true);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // currently can't be set in layout
            ColorStateList followTint = ContextCompat.getColorStateList(this, R.color.follow);
            Util.setBackgroundTintList(follow, followTint);
        }
        Util.setAlpha(follow, Main.FABALPHA);

        // Set up the zoom in/out controls
        zoomControls = mapLayout.findViewById(R.id.zoom_controls);

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

        // simple actions mode button
        simpleActionsButton = (FloatingActionButton) getLayoutInflater().inflate(R.layout.simple_button, null);
        Util.setAlpha(simpleActionsButton, Main.FABALPHA);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.ABOVE, R.id.zoom_controls);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        if (prefs.areSimpleActionsEnabled()) {
            showSimpleActionsButton();
        } else {
            hideSimpleActionsButton();
        }
        mapLayout.addView(simpleActionsButton, rlp);
        simpleActionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Logic logic = App.getLogic();
                if (!logic.isInEditZoomRange()) {
                    Snack.barInfoShort(Main.this, R.string.toast_not_in_edit_range);
                } else {
                    PopupMenu popup = SimpleActionModeCallback.getMenu(Main.this, simpleActionsButton);
                    popup.show();
                }
            }
        });

        // layers button setup
        layers = (FloatingActionButton) mapLayout.findViewById(R.id.layers);

        layers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                descheduleAutoLock();
                Layers.showDialog(Main.this);
            }
        });

        DataStyle.getStylesFromFiles(this); // needs to happen before
                                            // setContentView

        setContentView(ml);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        setSupportActionBar(toolbar);

        if (prefs.splitActionBarEnabled()) {
            setBottomBar((androidx.appcompat.widget.ActionMenuView) findViewById(R.id.bottomToolbar));
        } else {
            findViewById(R.id.bottomBar).setVisibility(View.GONE);
        }

        // check if first time user and display something if yes
        Version version = new Version(this);
        newInstall = version.isNewInstall();
        newVersion = version.isNewVersion();
        version.save(this);

        loadOnResume = false;

        if (App.getLogic() == null) {
            Log.i(DEBUG_TAG, "onCreate - creating new logic");
            App.newLogic();
        }
        Log.i(DEBUG_TAG, "onCreate - setting new map");

        App.getLogic().setPrefs(prefs);
        App.getLogic().setMap(map, true);

        Log.d(DEBUG_TAG, "StorageDelegator dirty is " + App.getDelegator().isDirty());
        if (isLastActivityAvailable() && !App.getDelegator().isDirty()) {
            // data was modified while we were stopped if isDirty is true
            // Start loading after resume to ensure loading dialog can be
            // removed afterwards
            loadOnResume = true;
        }

        easyEditManager = new EasyEditManager(this);

        haveCamera = checkForCamera(); // we recall this in onResume just to be
    }

    /**
     * Set how we should handle screen orientation changes based of our preferences
     */
    private void setScreenOrientation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            switch (prefs.getMapOrientation()) {
            case "CURRENT":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                break;
            case "PORTRAIT":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case "LANDSCAPE":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case "AUTO":
            default:
                // do nothing
                break;
            }
        }
    }

    /**
     * Get the best last position
     * 
     * @return a Location object
     */
    @Nullable
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
            } catch (IllegalArgumentException | SecurityException e) {
                // do nothing
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

        updatePrefs(new Preferences(this));

        App.getLogic().setPrefs(prefs);

        // if we have been stopped delegator and viewbox will not be set if our
        // original Logic instance is still around
        map.setDelegator(App.getDelegator());
        map.setViewBox(App.getLogic().getViewBox());

        map.setPrefs(this, prefs);
        map.requestFocus();

        // available tileservers may have changed
        TileLayerDatabaseView.updateLayerConfig(this, prefs, map.getBackgroundLayer());
        TileLayerDatabaseView.updateLayerConfig(this, prefs, map.getOverlayLayer());

        undoListener = new UndoListener();

        showActionBar();

        Util.clearCaches(this, App.getConfiguration());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(DEBUG_TAG, "onNewIntent storage dirty " + App.getDelegator().isDirty());
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
            case ACTION_EXIT:
                exit();
                return;
            case ACTION_UPDATE:
                updatePrefs(new Preferences(this));
                App.getLogic().setPrefs(prefs);
                if (map != null) {
                    map.setPrefs(this, prefs);
                    map.invalidate();
                }
                return;
            default:
                // carry on
                Log.d(DEBUG_TAG, "Intent action " + action);
            }
        }
        setIntent(intent);
        getIntentData();
    }

    /**
     * Get the relevant data from any intents we were started with
     */
    private void getIntentData() {
        synchronized (geoDataLock) {
            geoData = (GeoUrlData) getIntent().getSerializableExtra(GeoUrlActivity.GEODATA);
        }
        synchronized (rcDataLock) {
            rcData = (RemoteControlUrlData) getIntent().getSerializableExtra(RemoteControlUrlActivity.RCDATA);
        }
        synchronized (contentUriLock) {
            Uri uri = getIntent().getData();
            if (uri != null && ("content".equals(uri.getScheme()) || (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && "file".equals(uri.getScheme())))) {
                contentUri = uri;
            } else {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    try {
                        Uri streamUri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                        Log.d(DEBUG_TAG, "getIntentData EXTRA_STREAM " + streamUri);
                        if (streamUri != null) {
                            contentUri = streamUri;
                            contentUriType = getIntent().getType();
                        }
                    } catch (ClassCastException e) {
                        Log.e(DEBUG_TAG, "getIntentData " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");

        final Logic logic = App.getLogic();

        App.initGeoContext(this);

        // register received for changes in connectivity
        connectivityChangedReceiver = new ConnectivityChangedReceiver();
        registerReceiver(connectivityChangedReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        haveCamera = checkForCamera();

        PostAsyncActionHandler postLoadData = new PostAsyncActionHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSuccess() {
                if (rcData != null || geoData != null || contentUri != null) {
                    setShowGPS(false);
                    processIntents();
                } else {
                    if (getTracker() != null) {
                        lastLocation = getTracker().getLastLocation();
                    }
                    setShowGPS(prefs.getShowGPS());
                }
                map.setPrefs(Main.this, prefs); // set again as ViewBox may have
                                                // changed
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
                if (easyEditManager != null && mode.elementsGeomEditiable() && (logic.getSelectedNode() != null || logic.getSelectedWay() != null
                        || (logic.getSelectedRelations() != null && !logic.getSelectedRelations().isEmpty()))) {
                    // need to restart whatever we were doing
                    Log.d(DEBUG_TAG, "restarting element action mode");
                    easyEditManager.editElements();
                } else if (mode.elementsEditable()) {
                    // de-select everything
                    logic.deselectAll();
                }
            }

            @Override
            public void onError() {
                // unused
            }
        };
        PostAsyncActionHandler postLoadTasks = new PostAsyncActionHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSuccess() {
                Log.d(DEBUG_TAG, "postLoadTasks onSuccess");
                Mode mode = logic.getMode();
                de.blau.android.layer.tasks.MapOverlay layer = map.getTaskLayer();
                if (layer != null) {
                    Task t = layer.getSelected();
                    if (mode.elementsGeomEditiable() && t != null && map.getTaskLayer() != null) {
                        Log.d(DEBUG_TAG, "restarting task action mode");
                        layer.onSelected(Main.this, t);
                    } else {
                        layer.deselectObjects();
                    }
                }
                map.invalidate();
            }

            @Override
            public void onError() {
                // unused
                Log.d(DEBUG_TAG, "postLoadTasks onError");
            }
        };
        synchronized (loadOnResumeLock) {
            if (redownloadOnResume) {
                redownloadOnResume = false;
                logic.downloadLast(this);
            } else if (loadOnResume) {
                // this is fairly convoluted as we need to have permissions before we can load
                // the layers which in turn need to be loaded before we retrieve the task data
                loadOnResume = false;

                logic.loadStateFromFile(this, postLoadData);

                checkPermissions(new Runnable() {
                    @Override
                    public void run() {
                        logic.loadLayerState(Main.this, new PostAsyncActionHandler() {

                            @Override
                            public void onSuccess() {
                                logic.loadTasksFromFile(Main.this, postLoadTasks);
                                setupFollowButton();
                            }

                            @Override
                            public void onError() {
                                Log.d(DEBUG_TAG, "error loading layers");
                                // always try to load tasks
                                onSuccess();
                            }
                        });
                        if (newVersion) {
                            Log.d(DEBUG_TAG, "post load state new version");
                            newVersion = false;
                            NewVersion.showDialog(Main.this);
                        }
                    }
                });
            } else {
                synchronized (setViewBoxLock) {
                    // loadStateFromFile does this above
                    App.getLogic().loadEditingState(this, setViewBox);
                }
                logic.loadLayerState(this, postLoadTasks);
                map.invalidate();
                checkPermissions(new Runnable() {
                    @Override
                    public void run() {
                        postLoadData.onSuccess();
                        if (newInstall) {
                            // newbie, display welcome dialog
                            Log.d(DEBUG_TAG, "showing welcome dialog");
                            newInstall = false;
                            Newbie.showDialog(Main.this);
                        } else if (newVersion) {
                            Log.d(DEBUG_TAG, "new version");
                            newVersion = false;
                            NewVersion.showDialog(Main.this);
                        }
                    }
                });
            }
        }
        synchronized (setViewBoxLock) {
            // reset in any case
            setViewBox = true;
        }
        logic.updateStyle();

        // start listening for location updates
        if (getTracker() != null) {
            getTracker().setListener(Main.this);
        }

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
     * Set a new instance of Preferences
     * 
     * As the in device shared preferences may have been changed we need to be able to update our copy
     * 
     * @param prefs the new Preferences instance
     */
    public void updatePrefs(@NonNull Preferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Ceck if this device has an active camera
     * 
     * @return true is a camera is present
     */
    private boolean checkForCamera() {
        // determine if we have a camera
        PackageManager pm = getPackageManager();
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && cameraIntent.resolveActivity(getPackageManager()) != null;
    }

    /**
     * Check if we have fine location permission and ask for it if not Side effect: binds to TrackerService
     * 
     * @param whenDone run this when finished, use this if you need permissions before an operation is executed
     */
    private void checkPermissions(Runnable whenDone) {
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
            this.whenPermissionsGranted = whenDone;
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } else {
            whenDone.run();
        }
    }

    /**
     * Process geo and JOSM remote control intents
     */
    private void processIntents() {
        final Logic logic = App.getLogic();
        synchronized (geoDataLock) {
            if (geoData != null) {
                final double lon = geoData.getLon();
                final double lat = geoData.getLat();
                final int lonE7 = geoData.getLonE7();
                final int latE7 = geoData.getLatE7();
                final boolean hasZoom = geoData.hasZoom();
                final int zoom = geoData.getZoom();
                geoData = null; // zap so that we don't re-download
                Log.d(DEBUG_TAG, "got position from geo: url " + geoData + " storage dirty is " + App.getDelegator().isDirty());
                if (prefs.getDownloadRadius() != 0) { // download
                    BoundingBox bbox;
                    try {
                        bbox = GeoMath.createBoundingBoxForCoordinates(lat, lon, prefs.getDownloadRadius(), true);
                        List<BoundingBox> bbList = new ArrayList<>(App.getDelegator().getBoundingBoxes());
                        List<BoundingBox> bboxes = null;
                        if (App.getDelegator().isEmpty()) {
                            bboxes = new ArrayList<>();
                            bboxes.add(bbox);
                        } else {
                            bboxes = BoundingBox.newBoxes(bbList, bbox);
                        }

                        PostAsyncActionHandler handler = new PostAsyncActionHandler() {
                            @Override
                            public void onSuccess() {
                                if (hasZoom) {
                                    getMap().getViewBox().setZoom(getMap(), zoom);
                                    getMap().getViewBox().moveTo(getMap(), lonE7, latE7);
                                } else {
                                    logic.getViewBox().fitToBoundingBox(getMap(), bbox);
                                }
                                map.invalidate();
                            }

                            @Override
                            public void onError() {
                                // Ignore
                            }
                        };
                        if (bboxes != null && !bboxes.isEmpty()) {
                            logic.downloadBox(this, bbox, true, handler);
                            if (prefs.areBugsEnabled()) {
                                // always add bugs for now
                                downLoadBugs(bbox);
                            }
                        } else {
                            handler.onSuccess();
                        }
                    } catch (OsmException e) {
                        Log.d(DEBUG_TAG, "processIntents got " + e.getMessage());
                    }
                } else {
                    Log.d(DEBUG_TAG, "moving to position");
                    if (hasZoom) {
                        getMap().getViewBox().setZoom(getMap(), zoom);
                    }
                    getMap().getViewBox().moveTo(getMap(), lonE7, latE7);
                    getMap().invalidate();
                }
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
                                            rcData = null; // zap to stop repeated/ downloads
                                        }
                                    }
                                }

                                @Override
                                public void onError() {
                                    // Ignore
                                }
                            });
                        } else {
                            rcDataEdit(rcData);
                            rcData = null; // zap to stop repeated downloads
                        }
                    } else { // zoom
                        map.getViewBox().fitToBoundingBox(getMap(), rcData.getBox());
                        map.invalidate();
                        rcData = null; // zap to stop repeated/
                                       // downloads
                    }
                } else {
                    Log.d(DEBUG_TAG, "RC box is null");
                    rcDataEdit(rcData);
                    rcData = null; // zap to stop repeated/ downloads
                }
            }
        }
        synchronized (contentUriLock) {
            Log.d(DEBUG_TAG, "Processing content uri");
            if (contentUri != null) {
                try {
                    Log.d(DEBUG_TAG, "contentUriType " + contentUriType);
                    if (contentUriType != null) {
                        if (MimeTypes.JPEG.equals(contentUriType) || MimeTypes.ALL_IMAGE_FORMATS.equals(contentUriType)) {
                            if (!prefs.isPhotoLayerEnabled()) {
                                prefs.setPhotoLayerEnabled(true);
                                map.setPrefs(this, prefs); // sets up layers too
                                de.blau.android.layer.photos.MapOverlay photoLayer = map.getPhotoLayer();
                                if (photoLayer != null) {
                                    photoLayer.createIndex(new PhotoUriHandler(this, contentUri));
                                } else {
                                    Snack.toastTopError(this, getString(R.string.toast_error_accessing_photo, contentUri));
                                }
                            } else {
                                (new PhotoUriHandler(this, contentUri)).onSuccess();
                            }
                        }
                    } else {
                        switch (FileUtil.getExtension(contentUri.getLastPathSegment())) {
                        case FileExtensions.GPX:
                            if (getTracker() != null) {
                                // load now
                                loadGPXFile(contentUri);
                            }
                            break;
                        case FileExtensions.JSON:
                        case FileExtensions.GEOJSON:
                            de.blau.android.layer.geojson.MapOverlay geojsonLayer = App.getLogic().getMap().getGeojsonLayer();
                            if (geojsonLayer != null) {
                                try {
                                    geojsonLayer.resetStyling();
                                    if (geojsonLayer.loadGeoJsonFile(this, contentUri)) {
                                        BoundingBox extent = geojsonLayer.getExtent();
                                        if (extent != null) {
                                            map.getViewBox().fitToBoundingBox(map, extent);
                                            setFollowGPS(false);
                                            map.setFollowGPS(false);
                                        }
                                        geojsonLayer.invalidate();
                                    }
                                } catch (IOException e) {
                                    // display a toast?
                                }
                            }
                            break;
                        default:
                            Log.e(DEBUG_TAG, "Unknown uri " + contentUri);
                        }
                    }
                } finally {
                    contentUri = null;
                }
            }
        }
    }

    /**
     * Load a GPX file from an Uri, showing a dialog if it would overwrite an existing one
     * 
     * Zooms to the first trackpoint if one exists
     * 
     * @param uri the Uri
     */
    private void loadGPXFile(@NonNull final Uri uri) {
        if (!getTracker().getTrackPoints().isEmpty()) {
            ImportTrack.showDialog(Main.this, uri);
        } else {
            getTracker().stopTracking(false);
            try {
                getTracker().importGPXFile(Main.this, uri, new PostAsyncActionHandler() {
                    @Override
                    public void onSuccess() {
                        gotoFirstTrackPoint();
                    }

                    @Override
                    public void onError() {
                        // do nothing
                    }
                });
                map.invalidate();
            } catch (FileNotFoundException e) {
                try {
                    Snack.barError(Main.this, getResources().getString(R.string.toast_file_not_found, uri.toString()));
                } catch (Exception ex) {
                    // protect against translation errors
                    Log.d(DEBUG_TAG, "read got " + e.getMessage());
                }
            }
        }
    }

    /**
     * Download bugs/tasks for a BoundingBox
     * 
     * @param bbox the BoundingBox
     */
    public void downLoadBugs(BoundingBox bbox) {
        if (isConnected()) { // don't try if we are not connected
            Progress.showDialog(this, Progress.PROGRESS_DOWNLOAD);
            TransferTasks.downloadBox(this, prefs.getServer(), bbox, true, new PostAsyncActionHandler() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSuccess() {
                    Progress.dismissDialog(Main.this, Progress.PROGRESS_DOWNLOAD);
                    de.blau.android.layer.tasks.MapOverlay taskLayer = map.getTaskLayer();
                    if (taskLayer != null) {
                        taskLayer.setVisible(true);
                    }
                    getMap().invalidate();
                }

                @Override
                public void onError() {
                    Progress.dismissDialog(Main.this, Progress.PROGRESS_DOWNLOAD);
                }
            });
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
            map.getViewBox().fitToBoundingBox(getMap(), box);
        }
        final Logic logic = App.getLogic();
        if (rcData.getSelect() != null) {
            // need to actually switch to easyeditmode
            if (!logic.getMode().elementsGeomEditiable()) {
                // TODO there might be states in which we don't
                // want to exit which ever mode we are in
                logic.setMode(this, Mode.MODE_EASYEDIT);
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
            if (easyEditManager != null) {
                easyEditManager.editElements();
            }
        }
        if (rcData.getChangesetComment() != null) {
            logic.setDraftComment(rcData.getChangesetComment());
        }
        if (rcData.getChangesetSource() != null) {
            logic.setDraftSourceComment(rcData.getChangesetSource());
        }
    }

    @Override
    protected void onPause() {
        descheduleAutoLock();
        Log.d(DEBUG_TAG, "onPause mode " + App.getLogic().getMode());
        try {
            unregisterReceiver(connectivityChangedReceiver);
        } catch (Exception e) {
            // FIXME if onPause gets called before onResume has registered the
            // Receiver
            // unregisterReceiver will throw an exception, a better fix would
            // likely to
            // register earlier, but that may not help
        }
        disableLocationUpdates();
        if (getTracker() != null) {
            getTracker().setListener(null);
        }

        // always save editing state
        App.getLogic().saveEditingState(this);
        // save tag clipboard
        App.getTagClipboard(this).save(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(DEBUG_TAG, "onStop");
        // editing state has been saved in onPause
        saveData();
        App.getMruTags().save(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        map.onDestroy();
        if (getTracker() != null) {
            getTracker().setListener(null);
        }
        try {
            unbindService(this);
        } catch (Exception ignored) {
            Log.d(DEBUG_TAG, "Ignored " + ignored);
        }
        disableLocationUpdates();
        if (getTracker() != null) {
            getTracker().setListener(null);
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
     * 
     * Will trigger an update of any menubars
     */
    private void updateZoomControls() {
        final Logic logic = App.getLogic();
        getControls().setIsZoomInEnabled(logic.canZoom(Logic.ZOOM_IN));
        getControls().setIsZoomOutEnabled(logic.canZoom(Logic.ZOOM_OUT));
        triggerMenuInvalidation();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        if (App.getLogic().getMap() == null) {
            App.getLogic().setMap(map, false);
        } else {
            map.invalidate();
        }
        if (easyEditManager != null && easyEditManager.isProcessingAction()) {
            easyEditManager.invalidate();
        }
        Util.clearCaches(this, newConfig);
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
            synchronized (this) {
                if (whenPermissionsGranted != null) {
                    whenPermissionsGranted.run();
                    whenPermissionsGranted = null;
                }
            }
            break;
        }
        triggerMenuInvalidation(); // update menus
    }

    /**
     * Sets up the Action Bar and the "follow" button
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
    }

    /**
     * Setup the GPS follow button
     * 
     * This needs to called after GPS permissions have been enabled
     */
    private void setupFollowButton() {
        FloatingActionButton follow = getFollowButton();
        if (follow != null) {
            String[] locationProviders = getEnabledLocationProviders();
            if (locationProviders != null) {
                RelativeLayout.LayoutParams params = (LayoutParams) follow.getLayoutParams();
                String followGPSbuttonPosition = prefs.followGPSbuttonPosition();
                boolean isVisible = true;
                if (getString(R.string.follow_GPS_left).equals(followGPSbuttonPosition)) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                } else if (getString(R.string.follow_GPS_right).equals(followGPSbuttonPosition)) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                } else if (getString(R.string.follow_GPS_none).equals(followGPSbuttonPosition)) {
                    follow.hide();
                    isVisible = false;
                }
                follow.setLayoutParams(params);
                // only show GPS symbol if we only have GPS
                setFollowImage(locationProviders.length == 1 && LocationManager.GPS_PROVIDER.equals(locationProviders[0]), isVisible);
            } else {
                follow.hide();
            }
        }
    }

    /**
     * Set the icon on the follow button
     * 
     * @param gps if true the GPS icon will be displayed
     * @param isVisible true if the FAB is currently being shown
     */
    private void setFollowImage(boolean gps, boolean isVisible) {
        FloatingActionButton follow = getFollowButton();
        int buttonRes = R.drawable.ic_filter_tilt_shift_black_36dp;
        if (gps) {
            buttonRes = R.drawable.ic_gps_fixed_black_36dp;
        }
        follow.setImageResource(buttonRes);
        if (isVisible) {
            follow.hide(); // workaround https://issuetracker.google.com/issues/117476935
            follow.show();
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
        //
        final FloatingActionButton lock = setLock(mode);
        if (lock == null) {
            return; // FIXME not good but no other alternative right now,
                    // already logged in setLock
        }
        lock.setTag(mode.tag());

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, ContextCompat.getDrawable(this, mode.iconResourceId()));
        states.addState(new int[] { 0 }, ContextCompat.getDrawable(this, R.drawable.locked_opaque));
        lock.setImageDrawable(states);
        lock.hide(); // workaround https://issuetracker.google.com/issues/117476935
        lock.show();

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
                    enableSimpleActionsButton();
                } else {
                    logic.setLocked(true);
                    ((FloatingActionButton) b).setImageState(new int[] { 0 }, false);
                    disableSimpleActionsButton();
                }
                updateActionbarEditMode();
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
                        SpannableString s = new SpannableString(newMode.getName(Main.this));
                        if (mode == newMode) {
                            s.setSpan(new ForegroundColorSpan(ThemeUtils.getStyleAttribColorValue(Main.this, R.attr.colorAccent, 0)), 0, s.length(), 0);
                        }
                        MenuItem item = popup.getMenu().add(s);
                        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                logic.setMode(Main.this, newMode);
                                b.setTag(newMode.tag());
                                StateListDrawable states = new StateListDrawable();
                                states.addState(new int[] { android.R.attr.state_pressed }, ContextCompat.getDrawable(Main.this, newMode.iconResourceId()));
                                states.addState(new int[] {}, ContextCompat.getDrawable(Main.this, R.drawable.locked_opaque));
                                lock.setImageDrawable(states);
                                lock.hide(); // workaround https://issuetracker.google.com/issues/117476935
                                lock.show();
                                if (logic.isLocked()) {
                                    ((FloatingActionButton) b).setImageState(new int[] { 0 }, false);
                                } else {
                                    ((FloatingActionButton) b).setImageState(new int[] { android.R.attr.state_pressed }, false);
                                }
                                updateActionbarEditMode();
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

    /**
     * Get the lock button
     * 
     * @return the lock
     */
    public FloatingActionButton getLock() {
        return (FloatingActionButton) findViewById(R.id.floatingLock);
    }

    /**
     * Set lock button to locked or unlocked depending on the edit mode
     * 
     * Side effect disables/enables the simple actions button too
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
            disableSimpleActionsButton();
        } else {
            lock.setImageState(new int[] { android.R.attr.state_pressed }, false);
            enableSimpleActionsButton();
        }
        logic.setMode(this, mode);
        return lock; // for convenience
    }

    /**
     * Force update any UI elements that are mode dependent
     */
    synchronized void updateActionbarEditMode() {
        Log.d(DEBUG_TAG, "updateActionbarEditMode");
        setLock(App.getLogic().getMode());
        invalidateOptionsMenu();
    }

    BottomBarClickListener bottomBarListener;

    @Override
    public boolean onPrepareOptionsMenu(final Menu m) {
        if (bottomBarListener == null && getBottomBar() != null) {
            // NOTE BottomBarClickListener tries to keep a valid reference to the activity
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
            if (getBottomBar() != null && Screen.isLarge(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Flavors.LEGACY.equals(BuildConfig.FLAVOR)) {
                inflater.inflate(R.menu.main_menu_nosubmenus, menu);
            } else {
                inflater.inflate(R.menu.main_menu, menu);
            }
        }

        boolean networkConnected = isConnected();
        String[] locationProviders = getEnabledLocationProviders();
        boolean gpsProviderEnabled = haveLocationProvider(locationProviders, LocationManager.GPS_PROVIDER) && locationPermissionGranted;
        boolean locationProviderEnabled = gpsProviderEnabled || (haveLocationProvider(locationProviders, LocationManager.NETWORK_PROVIDER)
                && prefs.isNetworkLocationFallbackAllowed() && locationPermissionGranted);

        final Server server = prefs.getServer();
        boolean hasMapSplitSource = server.hasMapSplitSource();

        // just as good as any other place to check this
        if (locationProviderEnabled) {
            showFollowButton();
        } else {
            hideFollowButton();
        }
        menu.findItem(R.id.menu_gps_show).setEnabled(locationProviderEnabled).setChecked(showGPS);
        menu.findItem(R.id.menu_gps_follow).setEnabled(locationProviderEnabled).setChecked(followGPS);
        menu.findItem(R.id.menu_gps_goto).setEnabled(locationProviderEnabled);
        menu.findItem(R.id.menu_gps_start).setEnabled(getTracker() != null && !getTracker().isTracking() && gpsProviderEnabled);
        menu.findItem(R.id.menu_gps_pause).setEnabled(getTracker() != null && getTracker().isTracking() && gpsProviderEnabled);
        menu.findItem(R.id.menu_enable_gps_autodownload).setEnabled(getTracker() != null && locationProviderEnabled && (networkConnected || hasMapSplitSource))
                .setChecked(prefs.getAutoDownload());
        menu.findItem(R.id.menu_enable_pan_and_zoom_auto_download).setEnabled(networkConnected || hasMapSplitSource)
                .setChecked(prefs.getPanAndZoomAutoDownload());
        menu.findItem(R.id.menu_transfer_bugs_autodownload).setEnabled(getTracker() != null && locationProviderEnabled && networkConnected)
                .setChecked(prefs.getBugAutoDownload());

        boolean trackerHasTrackPoints = getTracker() != null && getTracker().hasTrackPoints();
        boolean trackerHasWayPoints = getTracker() != null && getTracker().hasWayPoints();
        menu.findItem(R.id.menu_gps_clear).setEnabled(trackerHasTrackPoints || trackerHasWayPoints);
        menu.findItem(R.id.menu_gps_goto_start).setEnabled(trackerHasTrackPoints);
        menu.findItem(R.id.menu_gps_goto_first_waypoint).setEnabled(trackerHasWayPoints);
        menu.findItem(R.id.menu_gps_import).setEnabled(getTracker() != null);
        menu.findItem(R.id.menu_gps_upload).setEnabled(trackerHasTrackPoints && networkConnected);

        final Logic logic = App.getLogic();
        MenuItem undo = menu.findItem(R.id.menu_undo);
        UndoStorage undoStorage = logic.getUndo();
        undo.setVisible(!logic.isLocked() && (undoStorage.canUndo() || undoStorage.canRedo()));
        View undoView = undo.getActionView();
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);

        menu.findItem(R.id.menu_gps_goto_last_edit).setEnabled(undoStorage.canUndo());

        LayerDrawable transfer = (LayerDrawable) menu.findItem(R.id.menu_transfer).getIcon();
        final StorageDelegator delegator = App.getDelegator();
        BadgeDrawable.setBadgeWithCount(this, transfer, delegator.getApiElementCount(), prefs.getUploadOkLimit(), prefs.getUploadWarnLimit());

        menu.findItem(R.id.menu_transfer_close_changeset).setVisible(server.hasOpenChangeset());

        if (hasMapSplitSource) {
            menu.findItem(R.id.menu_transfer_download_current).setEnabled(true).setTitle(R.string.menu_transfer_load_current);
            menu.findItem(R.id.menu_transfer_download_replace).setEnabled(true).setTitle(R.string.menu_transfer_load_replace);
        } else {
            menu.findItem(R.id.menu_transfer_download_current).setEnabled(networkConnected).setTitle(R.string.menu_transfer_download_current);
            menu.findItem(R.id.menu_transfer_download_replace).setEnabled(networkConnected).setTitle(R.string.menu_transfer_download_replace);
        }
        // note: isDirty is not a good indicator of if if there is really
        // something to upload
        menu.findItem(R.id.menu_transfer_upload).setEnabled(networkConnected && !delegator.getApiStorage().isEmpty());
        menu.findItem(R.id.menu_transfer_bugs_download_current).setEnabled(networkConnected);
        menu.findItem(R.id.menu_transfer_bugs_upload).setEnabled(networkConnected && App.getTaskStorage().hasChanges());
        menu.findItem(R.id.menu_voice).setVisible(false); // don't display
                                                          // button for now
        // experimental
        // menu.findItem(R.id.menu_voice).setEnabled(networkConnected &&
        // prefs.voiceCommandsEnabled()).setVisible(prefs.voiceCommandsEnabled());

        // the following depends on us having permission to write to "external"
        // storage
        menu.findItem(R.id.menu_transfer_export).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_transfer_save_file).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_transfer_save_notes_all).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_transfer_save_notes_new_and_changed).setEnabled(storagePermissionGranted);
        menu.findItem(R.id.menu_gps_export).setEnabled(storagePermissionGranted);

        // main menu items
        menu.findItem(R.id.menu_search_objects).setEnabled(!logic.isLocked());

        Filter filter = logic.getFilter();
        if (filter instanceof TagFilter && !prefs.getEnableTagFilter()) {
            // something is wrong, try to sync
            prefs.enableTagFilter(true);
            Log.d(DEBUG_TAG, "had to resync tagfilter pref");
        }

        menu.findItem(R.id.menu_enable_tagfilter).setEnabled(logic.getMode().supportsFilters())
                .setChecked(prefs.getEnableTagFilter() && logic.getFilter() instanceof TagFilter);
        menu.findItem(R.id.menu_enable_presetfilter).setEnabled(logic.getMode().supportsFilters())
                .setChecked(prefs.getEnablePresetFilter() && logic.getFilter() instanceof PresetFilter);

        menu.findItem(R.id.menu_simple_actions).setChecked(prefs.areSimpleActionsEnabled());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || !BuildConfig.FLAVOR.equals(Flavors.CURRENT)) {
            // the library providing the UI is not supported under SDK 15, in reality 15 doesn't work
            menu.findItem(R.id.menu_feedback).setVisible(false);
        } else { // only works with network
            menu.findItem(R.id.menu_feedback).setEnabled(networkConnected);
        }

        // enable the JS console menu entry
        menu.findItem(R.id.tag_menu_js_console).setEnabled(prefs.isJsConsoleEnabled());

        menuUtil.setShowAlways(menu);
        // only show camera icon if we have a camera, and a camera app is
        // installed
        if (haveCamera) {
            MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_camera),
                    prefs.showCameraAction() ? MenuItemCompat.SHOW_AS_ACTION_ALWAYS : MenuItemCompat.SHOW_AS_ACTION_NEVER);
        } else {
            MenuItem mi = menu.findItem(R.id.menu_camera).setVisible(false);
            MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_NEVER);
        }

        menu.findItem(R.id.menu_tools_background_align).setEnabled(map.getBackgroundLayer() != null);

        if (getBottomBar() != null) {
            // menuUtil.evenlyDistributedToolbar(getBottomToolbar());
        }

        return true;
    }

    /**
     * Check if we have a specific location provider
     * 
     * @param providers array holding the provider names
     * @param provider the provider
     * @return true if the provider is present
     */
    private boolean haveLocationProvider(@Nullable String[] providers, @Nullable String provider) {
        if (providers != null) {
            for (String p : providers) {
                if (p != null && p.equals(provider)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onItemSelected(SearchResult sr) {
        // turn this off or else we get bounced back to our current GPS position
        setFollowGPS(false);
        getMap().setFollowGPS(false);
        App.getLogic().setZoom(getMap(), Ui.ZOOM_FOR_ZOOMTO - 2); // we typically want to see a bit more of places
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
        switch (item.getItemId()) {
        case R.id.menu_config:
            PrefEditor.start(this);
            return true;

        case R.id.menu_find:
            descheduleAutoLock();
            SearchForm.showDialog(this, map.getViewBox());
            return true;
        case R.id.menu_search_objects:
            descheduleAutoLock();
            Search.search(this);
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

        case R.id.menu_simple_actions:
            if (prefs.areSimpleActionsEnabled()) {
                prefs.enableSimpleActions(false);
                item.setChecked(false);
                hideSimpleActionsButton();
            } else {
                prefs.enableSimpleActions(true);
                item.setChecked(true);
                showSimpleActionsButton();
            }
            break;

        case R.id.menu_share:
            Util.sharePosition(this, map.getViewBox().getCenter(), map.getZoomLevel());
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
                Uri photoUri = FileProvider.getUriForFile(this, getString(R.string.content_provider), imageFile);
                if (photoUri != null) {
                    startCamera.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(startCamera, REQUEST_IMAGE_CAPTURE);
                }
            } catch (Exception ex) {
                try {
                    Snack.barError(this, getResources().getString(R.string.toast_camera_error, ex.getMessage()));
                    Log.e(DEBUG_TAG, ex.getMessage());
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
            gotoCurrentLocation();
            return true;

        case R.id.menu_gps_goto_coordinates:
            descheduleAutoLock();
            CoordinatesOrOLC.get(this, new CoordinatesOrOLC.HandleResult() {
                @Override
                public void onSuccess(LatLon ll) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logic.setZoom(getMap(), Ui.ZOOM_FOR_ZOOMTO);
                            setFollowGPS(false);
                            map.setFollowGPS(false);
                            map.getViewBox().moveTo(map, (int) (ll.getLon() * 1E7d), (int) (ll.getLat() * 1E7d));
                            map.invalidate();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snack.toastTopError(Main.this, message);
                        }
                    });
                }
            });
            return true;

        case R.id.menu_gps_goto_last_edit:
            BoundingBox box = logic.getUndo().getCurrentBounds();
            if (box != null) {
                setFollowGPS(false);
                if (box.isEmpty()) {
                    logic.setZoom(getMap(), Ui.ZOOM_FOR_ZOOMTO);
                    map.getViewBox().moveTo(map, box.getRight(), box.getTop());
                } else {
                    map.getViewBox().fitToBoundingBox(map, box);
                }
                map.invalidate();
            }
            return true;

        case R.id.menu_gps_start:
            if (getTracker() != null && haveLocationProvider(getEnabledLocationProviders(), LocationManager.GPS_PROVIDER)) {
                getTracker().startTracking();
                setFollowGPS(true);
            }
            return true;

        case R.id.menu_gps_pause:
            if (getTracker() != null && haveLocationProvider(getEnabledLocationProviders(), LocationManager.GPS_PROVIDER)) {
                getTracker().stopTracking(false);
                triggerMenuInvalidation();
            }
            return true;

        case R.id.menu_gps_clear:
            if (getTracker() != null) {
                if (!getTracker().isEmpty()) {
                    new AlertDialog.Builder(this).setTitle(R.string.menu_gps_clear).setMessage(R.string.clear_track_description)
                            .setPositiveButton(R.string.clear_anyway, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (getTracker() != null) {
                                        getTracker().stopTracking(true);
                                    }
                                    triggerMenuInvalidation();
                                    map.invalidate();
                                }
                            }).setNeutralButton(R.string.cancel, null).show();
                } else {
                    getTracker().stopTracking(true);
                    triggerMenuInvalidation();
                    map.invalidate();
                }
            }
            return true;

        case R.id.menu_gps_upload:
            if (server != null) {
                descheduleAutoLock();
                PostAsyncActionHandler restartAction = new PostAsyncActionHandler() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSuccess() {
                        GpxUpload.showDialog(Main.this);
                    }

                    @Override
                    public void onError() {
                        // Ignore
                    }
                };
                if (Server.checkOsmAuthentication(this, server, restartAction)) {
                    GpxUpload.showDialog(this);
                }
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
                        loadGPXFile(fileUri);
                        SelectFile.savePref(prefs, R.string.config_gpxPreferredDir_key, fileUri);
                    }
                    map.invalidate();
                    return true;
                }
            });
            return true;

        case R.id.menu_gps_goto_start:
            gotoFirstTrackPoint();
            return true;

        case R.id.menu_gps_goto_first_waypoint:
            List<WayPoint> w = tracker.getWayPoints();
            if (w != null && !w.isEmpty()) {
                gotoTrackPoint(logic, w.get(0));
            }
            return true;

        case R.id.menu_enable_gps_autodownload:
            Log.d(DEBUG_TAG, "gps auto download menu");
            if (prefs.getAutoDownload()) {
                // already selected turn off
                prefs.setAutoDownload(false);
                item.setChecked(false);
            } else {
                prefs.setAutoDownload(true);
                item.setChecked(true);
                prefs.setPanAndZoomAutoDownload(false);
            }
            startStopAutoDownload();
            map.setPrefs(this, prefs);
            break;

        case R.id.menu_enable_pan_and_zoom_auto_download:
            Log.d(DEBUG_TAG, "pan and zoom auto download menu");
            if (prefs.getPanAndZoomAutoDownload()) {
                // already selected turn off
                prefs.setPanAndZoomAutoDownload(false);
                item.setChecked(false);
            } else {
                prefs.setPanAndZoomAutoDownload(true);
                item.setChecked(true);
                prefs.setAutoDownload(false);
                Tip.showDialog(this, R.string.tip_pan_and_zoom_auto_download_key, R.string.tip_pan_and_zoom_auto_download);
            }
            startStopAutoDownload();
            map.setPrefs(this, prefs);
            break;

        case R.id.menu_transfer_download_current:
            onMenuDownloadCurrent(true);
            return true;

        case R.id.menu_transfer_download_replace:
            onMenuDownloadCurrent(false);
            return true;

        case R.id.menu_transfer_upload:
            confirmUpload();
            return true;

        case R.id.menu_transfer_close_changeset:
            if (server.hasOpenChangeset()) {
                // fail silently if it doesn't work, next upload will open a new
                // changeset in any case
                new AsyncTask<Void, Integer, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            server.closeChangeset();
                        } catch (IOException e) {
                            // Never fail
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

        case R.id.menu_transfer_export:
            SavingHelper.asyncExport(this, delegator);
            return true;
        case R.id.menu_transfer_apply_osc_file:
            descheduleAutoLock();
            SelectFile.read(this, R.string.config_osmPreferredDir_key, new ReadFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean read(Uri fileUri) {
                    try {
                        logic.applyOscFile(Main.this, fileUri, null);
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

        case R.id.menu_transfer_read_file:
        case R.id.menu_transfer_read_pbf_file:
            descheduleAutoLock();
            // showFileChooser(READ_OSM_FILE_SELECT_CODE);
            final ReadFile readFile = new ReadFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean read(Uri fileUri) {
                    try {
                        if (item.getItemId() == R.id.menu_transfer_read_file) {
                            logic.readOsmFile(Main.this, fileUri, false);
                        } else {
                            logic.readPbfFile(Main.this, fileUri, false);
                        }
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
            };
            if (logic != null && logic.hasChanges()) {
                Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(ThemeUtils.getResIdFromAttribute(this, R.attr.alert_dialog));
                builder.setTitle(R.string.unsaved_data_title);
                builder.setMessage(R.string.unsaved_data_message);
                builder.setPositiveButton(R.string.unsaved_data_proceed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectFile.read(Main.this, R.string.config_osmPreferredDir_key, readFile);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
            } else {
                SelectFile.read(this, R.string.config_osmPreferredDir_key, readFile);
            }
            return true;

        case R.id.menu_transfer_save_file:
            descheduleAutoLock();
            SelectFile.save(this, R.string.config_osmPreferredDir_key, new SaveFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean save(Uri fileUri) {
                    App.getLogic().writeOsmFile(Main.this, fileUri, null);
                    SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                    return true;
                }
            });
            return true;
        case R.id.menu_transfer_download_msf:
            descheduleAutoLock();
            DownloadActivity.start(this, Urls.MSF_SERVER);
            break;
        case R.id.menu_transfer_bugs_download_current:
            downLoadBugs(map.getViewBox().copy());
            return true;

        case R.id.menu_transfer_bugs_upload:
            if (App.getTaskStorage().hasChanges()) {
                TransferTasks.upload(this, server, null);
            } else {
                Snack.barInfo(this, R.string.toast_no_changes);
            }
            return true;

        case R.id.menu_transfer_bugs_clear:
            if (App.getTaskStorage().hasChanges()) { // FIXME show a dialog and
                                                     // allow override
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
            prefs.setBugAutoDownload(!prefs.getBugAutoDownload());
            startStopBugAutoDownload();
            return true;
        case R.id.menu_transfer_save_notes_all:
        case R.id.menu_transfer_save_notes_new_and_changed:
            descheduleAutoLock();
            SelectFile.save(this, R.string.config_notesPreferredDir_key, new SaveFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean save(Uri fileUri) {
                    TransferTasks.writeOsnFile(Main.this, item.getItemId() == R.id.menu_transfer_save_notes_all, fileUri, null);
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
                    TransferTasks.writeCustomBugFile(Main.this, fileUri, null);
                    SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                    return true;
                }
            });
            return true;

        case R.id.menu_undo:
            // should not happen
            undoListener.onClick(null);
            return true;

        case R.id.menu_tools_flush_all_tile_caches:
            Snack.barWarning(this, getString(R.string.toast_flus_all_caches), R.string.Yes, new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    MapTilesLayer backgroundLayer = map.getBackgroundLayer();
                    if (backgroundLayer != null) {
                        backgroundLayer.flushTileCache(Main.this, null);
                    }
                    map.invalidate();
                }
            });
            return true;

        case R.id.menu_tools_background_align:
            // protect against weird state
            Mode oldMode = logic.getMode() != Mode.MODE_ALIGN_BACKGROUND ? logic.getMode() : Mode.MODE_EASYEDIT;
            try {
                backgroundAlignmentActionModeCallback = new BackgroundAlignmentActionModeCallback(this, oldMode);
                // NOTE needs to be after instance creation
                logic.setMode(this, Mode.MODE_ALIGN_BACKGROUND);
                startSupportActionMode(getBackgroundAlignmentActionModeCallback());
            } catch (IllegalStateException isex) {
                Log.e(DEBUG_TAG, isex.getMessage());
            }
            return true;

        case R.id.menu_tools_apply_local_offset:
            ImageryOffsetUtils.applyImageryOffsets(this, map.getBackgroundLayer().getTileLayerConfiguration(), null);
            return true;

        case R.id.menu_tools_update_imagery_configuration:
            new AsyncTask<Void, Void, Void>() {
                TileLayerDatabase db = new TileLayerDatabase(Main.this);

                @Override
                protected void onPreExecute() {
                    Progress.showDialog(Main.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        TileLayerServer.updateFromJOSMImagery(Main.this, db.getWritableDatabase());
                    } catch (IOException e) {
                        Log.e(DEBUG_TAG, "Update imagery conf. " + e.getMessage());
                        Util.toastDowloadError(Main.this, e);
                    } finally {
                        db.close();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    Progress.dismissDialog(Main.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                }
            }.execute();
            return true;

        case R.id.tag_menu_reset_address_prediction:
            Address.resetLastAddresses(this);
            return true;

        case R.id.menu_tools_oauth_reset: // reset the current OAuth tokens
            if (server.getOAuth()) {
                try (AdvancedPrefDatabase prefdb = new AdvancedPrefDatabase(this)) {
                    prefdb.setAPIAccessToken(null, null);
                }
            } else {
                Snack.barError(this, R.string.toast_oauth_not_enabled);
            }
            return true;

        case R.id.menu_tools_oauth_authorisation: // immediately start
                                                  // authorization handshake
            if (server.getOAuth()) {
                Authorize.startForResult(this, null);
            } else {
                Snack.barError(this, R.string.toast_oauth_not_enabled);
            }

            return true;
        case R.id.menu_tools_set_maproulette_apikey:
            MapRouletteApiKey.set(this, server, true);
            return true;
        case R.id.menu_tools_clear_clipboard:
            App.getDelegator().clearClipboard();
            return true;
        case R.id.tag_menu_js_console:
            Main.showJsConsole(this);
            return true;
        case R.id.menu_authors:
            startActivity(new Intent(this, LicenseViewer.class));
            return true;
        case R.id.menu_privacy:
            HelpViewer.start(this, R.string.help_privacy);
            return true;
        case R.id.menu_feedback:
            Feedback.start(this);
            return true;
        case R.id.menu_debug:
            startActivity(new Intent(this, DebugInformation.class));
            return true;
        }
        return false;

    }

    /**
     * Pan and zoom to the current location if any
     */
    private void gotoCurrentLocation() {
        Location gotoLoc = null;
        if (getTracker() != null) {
            gotoLoc = getTracker().getLastLocation();
        } else if (getEnabledLocationProviders() != null) {
            gotoLoc = getLastLocation();
        } // else moan? without GPS enabled this shouldn't be selectable
          // currently
        if (gotoLoc != null) {
            App.getLogic().setZoom(getMap(), Ui.ZOOM_FOR_ZOOMTO);
            map.getViewBox().moveTo(getMap(), (int) (gotoLoc.getLongitude() * 1E7d), (int) (gotoLoc.getLatitude() * 1E7d));
            map.setLocation(gotoLoc);
            map.invalidate();
        }
    }

    /**
     * Zoom to the GPS TrackPoint
     * 
     * @param logic the current Login instance
     * @param trackPoint the TrackPoint
     */
    public void gotoTrackPoint(final Logic logic, TrackPoint trackPoint) {
        Log.d(DEBUG_TAG, "Going to first waypoint");
        setFollowGPS(false);
        map.setFollowGPS(false);
        logic.setZoom(getMap(), Ui.ZOOM_FOR_ZOOMTO);
        map.getViewBox().moveTo(getMap(), trackPoint.getLon(), trackPoint.getLat());
        map.invalidate();
    }

    /**
     * Goto the first track point if any
     */
    private void gotoFirstTrackPoint() {
        TrackerService ts = getTracker();
        if (ts != null) {
            List<TrackPoint> l = ts.getTrackPoints();
            if (!l.isEmpty()) {
                gotoTrackPoint(App.getLogic(), l.get(0));
            }
        }
    }

    /**
     * Show the JS console
     * 
     * @param main the current instance of Main
     */
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

    /**
     * Start voice recognition
     */
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

    /**
     * Get a new File for storing an image
     * 
     * @return a File object
     * @throws IOException if reading the file went wrong
     */
    @NonNull
    private File getImageFile() throws IOException {
        File outDir = FileUtil.getPublicDirectory();
        outDir = FileUtil.getPublicDirectory(outDir, Paths.DIRECTORY_PATH_PICTURES);
        String imageFileName = DateFormatter.getFormattedString(DATE_PATTERN_IMAGE_FILE_NAME_PART);
        File imageFile = File.createTempFile(imageFileName, Paths.FILE_EXTENSION_IMAGE, outDir);
        Log.d(DEBUG_TAG, "getImageFile " + imageFile.getAbsolutePath());
        return imageFile;
    }

    /**
     * Toggle position based data auto-download
     */
    private void startStopAutoDownload() {
        Log.d(DEBUG_TAG, "autoDownload");
        if (getTracker() != null && getEnabledLocationProviders() != null) {
            if (prefs.getAutoDownload()) {
                getTracker().startAutoDownload();
            } else {
                getTracker().stopAutoDownload();
            }
        }
    }

    /**
     * Toggle position based tasks auto-download
     */
    private void startStopBugAutoDownload() {
        Log.d(DEBUG_TAG, "bugAutoDownload");
        if (getTracker() != null && getEnabledLocationProviders() != null) {
            if (prefs.getBugAutoDownload()) {
                getTracker().startBugAutoDownload();
            } else {
                getTracker().stopBugAutoDownload();
            }
        }
    }

    /**
     * If show is true start locations updates and start following the GPS position otherwise turn location updates off
     * 
     * @param show turn location updates on or off
     */
    private void setShowGPS(boolean show) {
        if (show && getEnabledLocationProviders() == null) {
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

    /**
     * Checks if GPS is enabled in the settings. If not, returns false and shows location settings.
     * 
     * @return the provider if a usable on is enabled, null if not
     */
    private String[] getEnabledLocationProviders() {
        List<String> temp = new ArrayList<>();
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsChecked = false;
                temp.add(LocationManager.GPS_PROVIDER);
            }
            if (prefs.isNetworkLocationFallbackAllowed() && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                gpsChecked = false;
                temp.add(LocationManager.NETWORK_PROVIDER);
            }
            if (!temp.isEmpty()) {
                return temp.toArray(new String[temp.size()]);
            }
            if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
                // check if there is a GPS provider at all
                if (!gpsChecked && !prefs.leaveGpsDisabled()) {
                    gpsChecked = true;
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
                return null;
            }
            return null;
        } catch (Exception e) {
            Log.d(DEBUG_TAG, "Error when checking for GPS, assuming GPS not available", e);
            Snack.barInfo(this, R.string.gps_failure);
            return null;
        }
    }

    /**
     * Set if we are centering the map on the current location
     * 
     * @param follow if true center on current location
     */
    public synchronized void setFollowGPS(boolean follow) {
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
            if (!follow || !onScreen) {
                followButton.setEnabled(true);
            } else { // this is hack around the elevation vanishing when disabled
                float elevation = followButton.getCompatElevation();
                followButton.setEnabled(!follow || !onScreen);
                followButton.setCompatElevation(elevation);
            }
        }
        if (follow && lastLocation != null) { // update if we are returning from
                                              // pause/stop
            Log.d(DEBUG_TAG, "Setting lastLocation");
            onLocationChanged(lastLocation);
        }
    }

    /**
     * Check if the screen should be centered on the current location or not
     * 
     * @return true if we should be following the location
     */
    public synchronized boolean getFollowGPS() {
        return followGPS;
    }

    /**
     * Toggle if we should show the location on screen
     */
    private void toggleShowGPS() {
        boolean newState = !showGPS;
        setShowGPS(newState);
    }

    /**
     * Toggle the follow location status
     */
    private void toggleFollowGPS() {
        boolean newState = !followGPS;
        setFollowGPS(newState);
        map.setFollowGPS(newState);
    }

    /**
     * Turn on location updates
     */
    private void enableLocationUpdates() {
        if (wantLocationUpdates) {
            return;
        }
        if (sensorManager != null) {
            sensorManager.registerListener(sensorListener, rotation, SensorManager.SENSOR_DELAY_UI);
        }
        wantLocationUpdates = true;
        if (getTracker() != null) {
            getTracker().setListenerNeedsGPS(true);
        }
    }

    /**
     * Turn off location updates
     */
    private void disableLocationUpdates() {
        if (!wantLocationUpdates) {
            return;
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
        }
        wantLocationUpdates = false;
        if (getTracker() != null) {
            getTracker().setListenerNeedsGPS(false);
        }
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (imageFile != null) {
                try (PhotoIndex pi = new PhotoIndex(this)) {
                    pi.addPhoto(imageFile);
                }
                if (prefs.isPhotoLayerEnabled()) {
                    map.invalidate();
                }
            } else {
                Log.e(DEBUG_TAG, "imageFile == null");
            }
        } else if (data != null) {
            if (requestCode == REQUEST_BOUNDING_BOX) {
                handleBoxPickerResult(resultCode, data);
            } else if (requestCode == REQUEST_EDIT_TAG && resultCode == RESULT_OK) {
                handlePropertyEditorResult(data);
            } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
                if (easyEditManager != null && easyEditManager.isProcessingAction()) {
                    easyEditManager.handleActivityResult(requestCode, resultCode, data);
                } else {
                    (new Commands(this)).processIntentResult(data, locationForIntent);
                    locationForIntent = null;
                    map.invalidate();
                }
            } else if ((requestCode == SelectFile.READ_FILE || requestCode == SelectFile.READ_FILE_OLD || requestCode == SelectFile.SAVE_FILE)
                    && resultCode == RESULT_OK) {
                SelectFile.handleResult(requestCode, data);
            } else {
                ActivityResultHandler.Listener listener = activityResultListeners.get(requestCode);
                if (listener != null) {
                    listener.processResult(resultCode, data);
                } else {
                    Log.w(DEBUG_TAG, "Received activity result without listener, code " + requestCode);
                }
            }
        }
        scheduleAutoLock();
    }

    /**
     * Handle the result of the BoxPicker Activity
     * 
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

        BoundingBox box = new BoundingBox(left, bottom, right, top);
        if (resultCode == RESULT_OK) {
            App.getLogic().downloadBox(this, box, false, null);
        } else if (resultCode == RESULT_CANCELED) { //
            synchronized (setViewBoxLock) {
                setViewBox = false; // stop setting the view box in onResume
                Log.d(DEBUG_TAG, "opening empty map on " + box.toString());
                App.getLogic().newEmptyMap(this, ViewBox.getMaxMercatorExtent());
            }
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
            // FIXME Problem saved data may not be read at this point, load
            // here, probably we should load editing state
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
                    Relation updatedRelation = (Relation) App.getDelegator().getOsmElement(Relation.NAME, editorData.osmId);
                    if (logic.isSelected(updatedRelation)) { // This might be unnecessary
                        logic.removeSelectedRelation(updatedRelation);
                        logic.setSelectedRelation(updatedRelation);
                    }
                }
            }
            // this is very expensive: getLogic().saveAsync(); // if nothing was
            // changed the dirty flag wont be set and
            // the save wont actually happen
        }
        if ((logic.getMode().elementsGeomEditiable() && easyEditManager != null && !easyEditManager.isProcessingAction())
                || logic.getMode() == Mode.MODE_TAG_EDIT) {
            // not in an easy edit mode, de-select objects avoids inconsistent
            // visual state
            logic.deselectAll();
        } else {
            // invalidate the action mode menu ... updates the state of the undo
            // button
            // for visual feedback reasons we leave selected elements selected
            // (tag edit mode)
            invalidateOptionsMenu();
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
        try (FileInputStream in = openFileInput(StorageDelegator.FILENAME)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Download OSM data for the currently displayed area
     * 
     * Will include Tasks for the same if enabled
     * 
     * @param main the instance of Main calling this
     * @param add if true merge the data with the current contents, if false replace
     */
    public static void performCurrentViewHttpLoad(final Main main, boolean add) {
        final Map map = main.getMap();
        App.getLogic().downloadBox(main, map.getViewBox().copy(), add, null);
        Preferences prefs = main.prefs;
        if (prefs.areBugsEnabled()) { // always adds bugs for now
            main.downLoadBugs(map.getViewBox().copy());
        }
    }

    /**
     * Upload changes to the OSM data and tasks to the API, if there are changes
     * 
     * @param comment Textual comment associated with the change set.
     * @param source Source of the change.
     * @param closeOpenChangeset If true try to close any open changeset first
     * @param closeChangeset Boolean flag indicating whether the change set should be closed or kept open.
     * @param extraTags Additional tags to add to the changeset
     */
    public void performUpload(@Nullable final String comment, @Nullable final String source, final boolean closeOpenChangeset, final boolean closeChangeset,
            @Nullable java.util.Map<String, String> extraTags) {
        final Logic logic = App.getLogic();
        final Server server = prefs.getServer();
        if (server != null && server.isLoginSet()) {
            boolean hasDataChanges = logic.hasChanges();
            boolean hasBugChanges = !App.getTaskStorage().isEmpty() && App.getTaskStorage().hasChanges();
            if (hasDataChanges || hasBugChanges) {
                if (hasDataChanges) {
                    logic.upload(this, comment, source, closeOpenChangeset, closeChangeset, extraTags);
                }
                if (hasBugChanges) {
                    TransferTasks.upload(this, server, null);
                }
                logic.checkForMail(this, server);
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
            logic.checkForMail(this, server);
        } else {
            ErrorAlert.showDialog(this, ErrorCodes.NO_LOGIN_DATA);
        }
    }

    /**
     * Check if there are changes present and then show the upload dialog, getting authorisation if necessary
     */
    public void confirmUpload() {
        PostAsyncActionHandler restartAction = new PostAsyncActionHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSuccess() {
                ConfirmUpload.showDialog(Main.this);
            }

            @Override
            public void onError() {
                // Ignore
            }
        };
        final Server server = prefs.getServer();
        if (App.getLogic().hasChanges()) {
            if (Server.checkOsmAuthentication(this, server, restartAction)) {
                ConfirmUpload.showDialog(this);
            }
        } else {
            Snack.barInfo(this, R.string.toast_no_changes);
        }
    }

    /**
     * Hide the bottom bar (if any)
     */
    public void hideBottomBar() {
        ActionMenuView bottomToolbar = getBottomBar();
        if (bottomToolbar != null) {
            bottomToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Show the bottom bar (if any)
     */
    public void showBottomBar() {
        ActionMenuView bottomToolbar = getBottomBar();
        if (bottomToolbar != null) {
            bottomToolbar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide the lock
     */
    public void hideLock() {
        FloatingActionButton lock = getLock();
        if (lock != null) {
            lock.hide();
        }
    }

    /**
     * Show the lock
     */
    public void showLock() {
        FloatingActionButton lock = getLock();
        if (lock != null) {
            lock.show();
        }
    }

    /**
     * Hide the layers control
     */
    public void hideLayersControl() {
        if (layers != null) {
            layers.hide();
        }
    }

    /**
     * Hide the layers control
     */
    public void showLayersControl() {
        if (layers != null) {
            layers.show();
        }
    }

    /**
     * Hide the simple actions button
     */
    public void hideSimpleActionsButton() {
        if (simpleActionsButton != null) {
            simpleActionsButton.hide();
        }
    }

    /**
     * Display the simple actions button
     */
    public void showSimpleActionsButton() {
        if (simpleActionsButton != null) {
            simpleActionsButton.show();
        }
    }

    /**
     * Enable the simple actions button and change color to the normal value
     */
    public void enableSimpleActionsButton() {
        if (simpleActionsButton != null) {
            changeSimpleActionsButtonState(true, ContextCompat.getColorStateList(Main.this, ThemeUtils.getResIdFromAttribute(Main.this, R.attr.colorAccent)));
        }
    }

    /**
     * Disable the simple actions button and change color
     */
    public void disableSimpleActionsButton() {
        if (simpleActionsButton != null) {
            changeSimpleActionsButtonState(false, ContextCompat.getColorStateList(Main.this, R.color.dark_grey));
        }
    }

    /**
     * Change the sate of the simple actions button
     * 
     * @param enabled the new state
     * @param stateList the ColorStateList
     */
    private void changeSimpleActionsButtonState(boolean enabled, @NonNull ColorStateList stateList) {
        simpleActionsButton.setEnabled(enabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            simpleActionsButton.setBackgroundTintList(stateList);
            simpleActionsButton.setCompatElevation(LARGE_FAB_ELEVATION);
            ViewGroup.LayoutParams lp = simpleActionsButton.getLayoutParams();
            if (enabled) {
                ((RelativeLayout.LayoutParams) lp).setMargins(0, 0, 0, 0);
            } else {
                ((RelativeLayout.LayoutParams) lp).setMargins((int) LARGE_FAB_ELEVATION, 0, (int) LARGE_FAB_ELEVATION, (int) LARGE_FAB_ELEVATION);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Util.setBackgroundTintList(simpleActionsButton, stateList);
        }
    }

    /**
     * Starts the LocationPicker activity for requesting a location.
     * 
     * @param titleResId a string resource id for the title
     */
    public void gotoBoxPicker(int titleResId) {
        descheduleAutoLock();
        if (App.getLogic().hasChanges()) {
            Intent intent = new Intent(this, BoxPicker.class);
            DataLossActivity.showDialog(this, intent, REQUEST_BOUNDING_BOX);
        } else {
            BoxPicker.startForResult(this, titleResId, REQUEST_BOUNDING_BOX);
        }
    }

    /**
     * Start the PropertyEditor for the element in question, single element version
     * 
     * @param selectedElement Selected OpenStreetMap element.
     * @param focusOn if not null focus on the value field of this key.
     * @param applyLastAddressTags add address tags to the object being edited.
     * @param showPresets show the preset tab on start up.
     */
    public void performTagEdit(final OsmElement selectedElement, String focusOn, boolean applyLastAddressTags, boolean showPresets) {
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
                PropertyEditor.startForResult(this, single, applyLastAddressTags, showPresets, logic.getMode().getExtraTags(logic, selectedElement),
                        logic.getMode().getPresetItems(this, selectedElement), REQUEST_EDIT_TAG);
            }
        }
    }

    /**
     * Start the PropertyEditor for the element in question, single element version
     * 
     * @param selectedElement Selected OpenStreetMap element.
     * @param presetPath path to preset to apply
     * @param tags any existing tags to apply
     * @param showPresets show the preset tab on start up.
     */
    public void performTagEdit(final OsmElement selectedElement, @Nullable PresetElementPath presetPath, @Nullable HashMap<String, String> tags,
            boolean showPresets) {
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
                single[0] = new PropertyEditorData(selectedElement, null);
                ArrayList<PresetElementPath> presetPathList = new ArrayList<>();
                if (presetPath != null) {
                    presetPathList.add(presetPath);
                }
                PropertyEditor.startForResult(this, single, false, showPresets, tags, presetPathList, REQUEST_EDIT_TAG);
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
    public void performTagEdit(final List<OsmElement> selection, boolean applyLastAddressTags, boolean showPresets) {
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
        PropertyEditor.startForResult(this, multipleArray, applyLastAddressTags, showPresets, null, null, REQUEST_EDIT_TAG);
    }

    /**
     * potentially do some special stuff for invoking undo and exiting
     */
    @Override
    public void onBackPressed() {
        Log.d(DEBUG_TAG, "onBackPressed()");
        if (prefs.useBackForUndo()) {
            String name = App.getLogic().undo();
            if (name != null) {
                Snack.barInfo(this, getResources().getString(R.string.undo) + ": " + name);
            } else {
                exit();
            }
        } else {
            exit();
        }
    }

    /**
     * pop up a dialog asking for confirmation and if confirmed exit
     */
    private void exit() {
        new AlertDialog.Builder(this).setTitle(R.string.exit_title)
                .setMessage(getTracker() != null && getTracker().isTracking() ? R.string.pause_exit_text : R.string.exit_text)
                .setNegativeButton(R.string.no, null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // if we actually exit, stop the auto downloads, for now
                        // allow GPS tracks to carry on
                        if (getTracker() != null) {
                            getTracker().stopAutoDownload();
                            getTracker().stopBugAutoDownload();
                            getTracker().stopTracking(false);
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
                } else { // note to avoid tons of error messages we need to
                         // consume both events
                    return actionResult;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public class UndoListener implements OnClickListener, OnLongClickListener {

        private final String DEBUG_TAG = UndoListener.class.getName();

        @Override
        public void onClick(View view) {
            Log.d(DEBUG_TAG, "normal click");
            final Logic logic = App.getLogic();
            BoundingBox undoBox = logic.getUndo().getLastBounds();
            if (undoBox != null && !map.getViewBox().intersects(undoBox)) {
                // undo location is not in view
                new AlertDialog.Builder(Main.this).setTitle(R.string.undo_location_title).setMessage(R.string.undo_location_text)
                        .setNeutralButton(R.string.cancel, null).setNegativeButton(R.string.undo_location_undo_anyway, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                undo(logic);
                            }
                        }).setPositiveButton(R.string.undo_location_zoom, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                map.getViewBox().fitToBoundingBox(map, undoBox);
                                undo(logic);
                            }
                        }).create().show();
                return;
            }
            undo(logic);
        }

        /**
         * Actually undo
         * 
         * @param logic the current Logic instance
         */
        void undo(@NonNull final Logic logic) {
            String name = logic.undo();
            if (name != null) {
                Snack.toastTopInfo(Main.this, getResources().getString(R.string.undo) + ": " + name);
            } else {
                Snack.toastTopInfo(Main.this, R.string.undo_nothing);
            }
            resync(logic);
            map.invalidate();
            triggerMenuInvalidation();
            Tip.showOptionalDialog(Main.this, R.string.tip_undo_key, R.string.tip_undo);
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(DEBUG_TAG, "long click");
            descheduleAutoLock();
            final Logic logic = App.getLogic();
            UndoStorage undo = logic.getUndo();
            if (undo.canUndo() || undo.canRedo()) {
                UndoDialog.showDialog(Main.this);
            } else {
                Snack.barInfoShort(Main.this, R.string.undo_nothing);
            }
            map.invalidate();
            return true;
        }
    }

    /**
     * If an undo/redo deleted an element we need to resync selection
     * 
     * @param logic the current instance of Logic
     */
    public void resync(final Logic logic) {
        // check that we haven't just removed a selected element
        if (logic.resyncSelected()) {
            // only need to test if anything at all is still selected
            if (logic.selectedNodesCount() + logic.selectedWaysCount() + logic.selectedRelationsCount() == 0) {
                getEasyEditManager().finish();
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

        class ClickedObject {
            final ClickableInterface layer;
            final Object             object;

            /**
             * Consrtuct a new container for objects that were clicked on a layer
             * 
             * @param layer the layer the object is on
             * @param object the object
             */
            ClickedObject(@NonNull ClickableInterface layer, @NonNull Object object) {
                this.layer = layer;
                this.object = object;
            }
        }

        private List<OsmElement> clickedNodesAndWays;

        private List<ClickedObject> clickedObjects = new ArrayList<>();

        private boolean doubleTap = false;

        @Override
        public boolean onTouch(final View v, final MotionEvent m) {
            descheduleAutoLock();
            // Log.d("MapTouchListener", "onTouch");
            if (m.getAction() == MotionEvent.ACTION_DOWN) {
                // Log.d("MapTouchListener", "onTouch ACTION_DOWN");
                clickedObjects.clear();
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
            // unused
        }

        @Override
        public void onClick(View v, float x, float y) {
            boolean elementsOnly = App.getLogic().getClickableElements() != null;
            if (!elementsOnly) {
                getClickedObjects(x, y);
            }

            final Logic logic = App.getLogic();
            Mode mode = logic.getMode();
            boolean isInEditZoomRange = logic.isInEditZoomRange();

            if (isInEditZoomRange) {
                if (logic.isLocked()) {
                    if (isConnectedOrConnecting() && prefs.voiceCommandsEnabled()) {
                        locationForIntent = lastLocation; // location when we
                                                          // touched the
                                                          // screen
                        startVoiceRecognition();
                    } else {
                        Snack.barInfoShort(Main.this, R.string.toast_unlock_to_edit);
                        Tip.showOptionalDialog(Main.this, R.string.tip_locked_mode_key, R.string.tip_locked_mode);
                    }
                } else {
                    if (mode.elementsEditable()) {
                        performEdit(mode, v, x, y);
                    }
                }
                map.invalidate();
            } else {
                switch (clickedObjects.size()) {
                case 0:
                    if (!isInEditZoomRange && !logic.isLocked()) {
                        Snack.barInfoShort(v, R.string.toast_not_in_edit_range);
                    }
                    break;
                case 1:
                    descheduleAutoLock();
                    ClickedObject co = clickedObjects.get(0);
                    co.layer.onSelected(Main.this, co.object);
                    break;
                default:
                    v.showContextMenu();
                    break;
                }
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
                // display context menu
                getClickedObjects(x, y);
                boolean dataIsVisible = map.getDataLayer() != null && map.getDataLayer().isVisible();
                clickedNodesAndWays = dataIsVisible ? App.getLogic().getClickedNodesAndWays(x, y) : new ArrayList<>();
                int elementCount = clickedNodesAndWays.size();
                int clickedObjectsCount = clickedObjects.size();
                int itemCount = elementCount + clickedObjectsCount;
                if (itemCount == 1) {
                    if (clickedObjectsCount == 1) {
                        ClickedObject co = clickedObjects.get(0);
                        co.layer.onSelected(Main.this, co.object);
                    } else if (elementCount == 1) {
                        ElementInfo.showDialog(Main.this, clickedNodesAndWays.get(0));
                    }
                } else if (itemCount > 0) {
                    v.showContextMenu();
                }
                return true;
            }
            if (!prefs.areSimpleActionsEnabled()) {
                if (logic.isInEditZoomRange()) {
                    // editing with the screen moving under you is a pain
                    setFollowGPS(false);
                    return getEasyEditManager().handleLongClick(v, x, y);
                } else {
                    Snack.barWarningShort(Main.this, R.string.toast_not_in_edit_range);
                }
            } else {
                Snack.barWarningShort(Main.this, R.string.toast_long_press_simple_mode);
            }

            return true; // long click handled
        }

        /**
         * Get clicked objects from layers (with the exception of the data layer)
         * 
         * 
         * @param x screen x coordinate of click position
         * @param y screen y coordinate of click position
         */
        private void getClickedObjects(final float x, final float y) {
            ViewBox viewBox = map.getViewBox();
            for (MapViewLayer layer : map.getLayers()) {
                if (layer instanceof ClickableInterface && layer.isVisible()) {
                    List<?> objects = ((ClickableInterface) layer).getClicked(x, y, viewBox);
                    for (Object o : objects) {
                        clickedObjects.add(new ClickedObject((ClickableInterface) layer, o));
                    }
                }
            }
        }

        @Override
        public void onDrag(View v, float x, float y, float dx, float dy) {
            // Log.d("MapTouchListener", "onDrag dx " + dx + " dy " + dy );
            try {
                App.getLogic().handleTouchEventMove(Main.this, x, y, -dx, dy);
            } catch (OsmIllegalOperationException ex) {
                Snack.barError(Main.this, ex.getMessage());
            }
            setFollowGPS(false);
        }

        @Override
        public void onScale(View v, float scaleFactor, float prevSpan, float curSpan, float focusX, float focusY) {
            // after zooming this translates the viewbox in a fashion that the focus location remains
            // at the same on screen position
            ViewBox viewBox = map.getViewBox();
            int focusLon = GeoMath.xToLonE7(map.getWidth(), viewBox, focusX);
            int focusLat = GeoMath.yToLatE7(map.getHeight(), map.getWidth(), viewBox, focusY);
            Logic logic = App.getLogic();
            synchronized (logic) {
                viewBox.zoom(Math.max(0.8f, Math.min(1.2f, scaleFactor)) - 1.0f);
                int newfocusLon = GeoMath.xToLonE7(map.getWidth(), viewBox, focusX);
                int newfocusLat = GeoMath.yToLatE7(map.getHeight(), map.getWidth(), viewBox, focusY);
                try {
                    viewBox.translate(map, focusLon - newfocusLon, focusLat - newfocusLat);
                } catch (OsmException e) {
                    // ignored
                }
            }
            DataStyle.updateStrokes(logic.strokeWidth(viewBox.getWidth()));
            if (logic.isRotationMode()) {
                logic.showCrosshairsForCentroid();
            }
            map.postInvalidate();
            setFollowGPS(followGPS);
            updateZoomControls();
        }

        /**
         * Perform edit touch processing.
         * 
         * @param mode mode we are in
         * @param v View affected by the touch event.
         * @param x the click-position on the display.
         * @param y the click-position on the display.
         */
        public void performEdit(Mode mode, final View v, final float x, final float y) {
            if (!getEasyEditManager().actionModeHandledClick(x, y)) {
                boolean dataIsVisible = map.getDataLayer() != null && map.getDataLayer().isVisible();
                clickedNodesAndWays = dataIsVisible ? App.getLogic().getClickedNodesAndWays(x, y) : new ArrayList<>();
                Logic logic = App.getLogic();
                Filter filter = logic.getFilter();
                if (filter != null) { // filter elements
                    clickedNodesAndWays = filterElements(clickedNodesAndWays);
                }
                int elementCount = clickedNodesAndWays.size();
                int clickedObjectsCount = clickedObjects.size();
                int itemCount = elementCount + clickedObjectsCount;
                boolean inEasyEditMode = logic.getMode().elementsGeomEditiable();
                switch (itemCount) {
                case 0:
                    // no elements were touched
                    if (inEasyEditMode) {
                        getEasyEditManager().nothingTouched(false);
                    }
                    break;
                case 1:
                    // exactly one element touched
                    if (clickedObjects.size() == 1) {
                        descheduleAutoLock();
                        ClickedObject co = clickedObjects.get(0);
                        co.layer.onSelected(Main.this, co.object);
                    } else if (clickedNodesAndWays.size() == 1) {
                        if (inEasyEditMode) {
                            getEasyEditManager().editElement(clickedNodesAndWays.get(0));
                        } else {
                            performTagEdit(clickedNodesAndWays.get(0), null, false, false);
                        }
                    } else {
                        String debugString = "performEdit can't find what was clicked " + filter;
                        Log.e(DEBUG_TAG, debugString);
                        ACRAHelper.nocrashReport(null, debugString);
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
                            performTagEdit(clickedNodesAndWays.get(0), null, false, false);
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
        private List<OsmElement> filterElements(List<OsmElement> elements) {
            List<OsmElement> tmp = new ArrayList<>();
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
            if (!getEasyEditManager().createContextMenu(menu)) {
                onCreateDefaultContextMenu(menu);
            }
        }

        /**
         * Creates a context menu with the objects near where the screen was touched
         * 
         * @param menu Menu object to add our entries to
         */
        public void onCreateDefaultContextMenu(final ContextMenu menu) {
            int id = 0;
            if (!clickedObjects.isEmpty()) {
                for (final ClickedObject co : clickedObjects) {
                    final ClickableInterface layer = co.layer;
                    menu.add(Menu.NONE, id++, Menu.NONE, layer.getDescription(co.object)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem arg0) {
                            descheduleAutoLock();
                            layer.onSelected(Main.this, co.object);
                            return true;
                        }
                    });
                }
            }
            if (clickedNodesAndWays != null) {
                Logic logic = App.getLogic();
                for (OsmElement e : clickedNodesAndWays) {
                    String description = descriptionForContextMenu(e);
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
         * Checks if a menu should be shown based on clickedNodesAndWays and clickedBugs. ClickedNodesAndWays needs to
         * contain nodes first, then ways, ordered by distance from the click. Assumes multiple elements have been
         * clicked, i.e. a choice is necessary unless heuristics work.
         * 
         * @return true if a selection menu should be shown
         */
        private boolean menuRequired() {
            // If the context menu setting requires the menu, show it instead of
            // guessing.
            if (prefs.getForceContextMenu()) {
                return true;
            }

            // If any object on a layer is clicked always show menu
            if (!clickedObjects.isEmpty()) {
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
                    return true; // otherwise a relation that only has nodes as
                                 // member is not selectable
                }
                final Logic logic = App.getLogic();
                float nodeX = logic.getNodeScreenX(candidate);
                float nodeY = logic.getNodeScreenY(candidate);
                for (int i = 1; i < clickedNodesAndWays.size(); i++) {
                    if (!(clickedNodesAndWays.get(i) instanceof Node)) {
                        break;
                    }
                    Node possibleNeighbor = (Node) clickedNodesAndWays.get(i);
                    float node2X = logic.getNodeScreenX(possibleNeighbor);
                    float node2Y = logic.getNodeScreenY(possibleNeighbor);
                    // Fast "square" checking is good enough
                    if (Math.abs(nodeX - node2X) < DataStyle.NODE_OVERLAP_TOLERANCE_VALUE
                            || Math.abs(nodeY - node2Y) < DataStyle.NODE_OVERLAP_TOLERANCE_VALUE) {
                        // The first node has an EXTREMELY close neighbour. Show
                        // context menu
                        return true;
                    }
                }
                return false; // no colliding neighbours found
            }

            // No nodes means we have at least two ways. Since the tolerance for
            // ways is tiny, show the menu.
            return true;
        }

        @Override
        public boolean onMenuItemClick(final android.view.MenuItem item) {
            int itemId = item.getItemId() - clickedObjects.size();
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
                        performTagEdit(element, null, false, false);
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
                boolean dataIsVisible = map.getDataLayer() != null && map.getDataLayer().isVisible();
                clickedNodesAndWays = dataIsVisible ? App.getLogic().getClickedNodesAndWays(x, y) : new ArrayList<>();
                switch (clickedNodesAndWays.size()) {
                case 0:
                    // no elements were touched
                    if (inEasyEditMode) {
                        // short cut to finishing multi-select
                        getEasyEditManager().nothingTouched(true);
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
                            // menuRequired tells us it's ok to just take the
                            // first one
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
                        // this stops the piercing beep related to volume
                        // adjustments
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
                        if (event.isCtrlPressed()) {
                            // get rid of Ctrl key
                            char shortcut = Character.toLowerCase((char) event.getUnicodeChar(0));
                            // menu based shortcuts don't seem to work (anymore) so we do this on foot
                            if (getEasyEditManager().isProcessingAction()) {
                                if (getEasyEditManager().processShortcut(shortcut)) {
                                    return true;
                                }
                            } else if (logic.getMode().elementsSelectable()) {
                                if (shortcut == Util.getShortCut(Main.this, R.string.shortcut_help)) {
                                    HelpViewer.start(Main.this, R.string.help_main);
                                    return true;
                                } else if (shortcut == Util.getShortCut(Main.this, R.string.shortcut_undo)) {
                                    Main.this.undoListener.onClick(null);
                                    return true;
                                } else if (shortcut == Util.getShortCut(Main.this, R.string.shortcut_gps_follow)) {
                                    Main.this.toggleFollowGPS();
                                    return true;
                                } else if (shortcut == Util.getShortCut(Main.this, R.string.shortcut_gps_goto)) {
                                    Main.this.gotoCurrentLocation();
                                    return true;
                                } else if (shortcut == Util.getShortCut(Main.this, R.string.shortcut_download)) {
                                    Main.this.onMenuDownloadCurrent(true);
                                    return true;
                                } else if (shortcut == Util.getShortCut(Main.this, R.string.shortcut_bugs_download)) {
                                    Main.this.downLoadBugs(map.getViewBox().copy());
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

        /**
         * Pan the map
         * 
         * @param direction pan direction
         */
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
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) > 0.0f) {
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

    /**
     * Check if we have network connectivity
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        if (networkStatus == null) {
            networkStatus = new NetworkStatus(this);
        }
        return networkStatus.isConnected();
    }

    /**
     * Check if we are network connected or in the process of connecting
     * 
     * @return true if either state is true
     */
    public boolean isConnectedOrConnecting() {
        if (networkStatus == null) {
            networkStatus = new NetworkStatus(this);
        }
        return networkStatus.isConnectedOrConnecting();
    }

    /**
     * Get the current Map instance
     * 
     * @return the current Map instance
     */
    @Nullable
    public Map getMap() {
        return map;
    }

    /**
     * Sets the activity to re-download the last downloaded area on startup (use e.g. when the API URL is changed)
     */
    public static void prepareRedownload() {
        // redownloadOnResume = true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(DEBUG_TAG, "Service " + name.getClassName() + " connected");
        if (TrackerService.class.getCanonicalName().equals(name.getClassName())) {
            Log.i(DEBUG_TAG, "Tracker service connected");
            setTracker((((TrackerBinder) service).getService()));
            map.setTracker(getTracker());
            getTracker().setListener(this);
            getTracker().setListenerNeedsGPS(wantLocationUpdates);
            startStopAutoDownload();
            startStopBugAutoDownload();
            triggerMenuInvalidation();
            synchronized (contentUriLock) {
                if (contentUri != null) {
                    Log.d(DEBUG_TAG, "Processing content uri for a gpx file");
                    if (FileExtensions.GPX.equals(FileUtil.getExtension(contentUri.getLastPathSegment()))) {
                        loadGPXFile(contentUri);
                        contentUri = null;
                    }
                }
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // should never happen, but just to be sure
        if (TrackerService.class.getCanonicalName().equals(name.getClassName())) {
            Log.i(DEBUG_TAG, "Tracker service disconnected");
            setTracker(null);
            map.setTracker(null);
            triggerMenuInvalidation();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (followGPS) {
            ViewBox viewBox = map.getViewBox();
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
        invalidateOptionsMenu();
    }

    /**
     * Simply calls {@link #invalidateOptionsMenu()}. MUST BE CALLED FROM THE MAIN/UI THREAD!
     */
    public void triggerMenuInvalidation() {
        Log.d(DEBUG_TAG, "triggerMenuInvalidation called");
        invalidateOptionsMenu();
        if (easyEditManager != null && easyEditManager.isProcessingAction()) {
            easyEditManager.invalidate();
        }
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

    /**
     * Zoom to a location and start editing the OsmElement
     * 
     * @param lonE7 longitude in WGS84*1E7 coordinates
     * @param latE7 latitude in WGS84*1E7 coordinates
     * @param e the OsmElement to edit
     */
    public void zoomToAndEdit(int lonE7, int latE7, @NonNull OsmElement e) {
        Log.d(DEBUG_TAG, "zoomToAndEdit Zoom " + map.getZoomLevel());
        zoomTo(lonE7, latE7, e);
        edit(e);
    }

    /**
     * Programmatically start editing an element
     * 
     * @param e the OemElement
     */
    public void edit(@NonNull OsmElement e) {
        final Logic logic = App.getLogic();
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
        if (easyEditManager != null && logic.getMode().elementsGeomEditiable()) {
            easyEditManager.editElement(e);
            map.invalidate();
        } else { // tag edit mode
            performTagEdit(e, null, false, false);
        }
    }

    /**
     * Zoom to the coordinates and try and set the viewbox size to something reasonable
     * 
     * @param lonE7 longitude * 1E7
     * @param latE7 latitude * 1E7
     * @param e OsmElement we want to show
     */
    private void zoomTo(int lonE7, int latE7, @NonNull OsmElement e) {
        setFollowGPS(false); // otherwise the screen could move around
        if (e instanceof Node && map.getZoomLevel() < Ui.ZOOM_FOR_ZOOMTO) {
            // FIXME this doesn't seem to work as expected
            App.getLogic().setZoom(getMap(), Ui.ZOOM_FOR_ZOOMTO);
        } else {
            map.getViewBox().fitToBoundingBox(getMap(), e.getBounds());
        }
        map.getViewBox().moveTo(getMap(), lonE7, latE7);
    }

    /**
     * Zoom to the element and try and set the viewbox size to something reasonable
     * 
     * @param e OsmElement we want to show
     */
    public void zoomTo(@NonNull OsmElement e) {
        zoomTo(Util.wrapInList(e));
    }

    /**
     * Zoom to the elements and try and set the viewbox size to something reasonable
     * 
     * @param elements OsmElements we want to show
     */
    public void zoomTo(@NonNull List<OsmElement> elements) {
        if (elements.isEmpty()) {
            Log.e(DEBUG_TAG, "zoomTo called with empty list");
            return;
        }
        setFollowGPS(false);
        BoundingBox result = null;
        Map map = getMap();
        if (elements.size() > 1 || !(elements.get(0) instanceof Node)) {
            for (OsmElement e : elements) {
                if (e != null) {
                    BoundingBox box = e.getBounds();
                    if (result == null) {
                        result = box;
                    } else {
                        if (box != null) {
                            result.union(box);
                        }
                    }
                }
            }
        } else {
            if (map.getZoomLevel() < Ui.ZOOM_FOR_ZOOMTO) {
                App.getLogic().setZoom(map, Ui.ZOOM_FOR_ZOOMTO);
            }
            map.getViewBox().moveTo(map, ((Node) elements.get(0)).getLon(), ((Node) elements.get(0)).getLat());
        }
        if (result != null) {
            map.getViewBox().fitToBoundingBox(map, result);
        }
    }

    /**
     * Workaround for bug mentioned below
     */
    @Override
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
        map.invalidate();
    }

    /**
     * @return the bottomToolbar
     */
    public androidx.appcompat.widget.ActionMenuView getBottomBar() {
        return bottomBar;
    }

    /**
     * @param bottomBar the bottomToolbar to set
     */
    private void setBottomBar(androidx.appcompat.widget.ActionMenuView bottomBar) {
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
        if (follow != null && getEnabledLocationProviders() != null && locationPermissionGranted && !"NONE".equals(prefs.followGPSbuttonPosition())) {
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
                EasyEditManager manager = getEasyEditManager();
                boolean elementSelected = manager.inElementSelectedMode() || manager.inNewNoteSelectedMode();
                if (!manager.isProcessingAction() || elementSelected) {
                    View lock = getLock();
                    if (lock != null) {
                        lock.performClick();
                    }
                    if (elementSelected) {
                        App.getLogic().deselectAll();
                        map.deselectObjects();
                        manager.finish();
                    }
                } else {
                    // can't lock now, reschedule
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
    public void descheduleAutoLock() {
        map.removeCallbacks(autoLock);
    }

    /**
     * Get the Layout that should hold the map
     * 
     * @return the Layout that should hold the map or null if it hasn't been set
     */
    @Nullable
    public RelativeLayout getMapLayout() {
        return mapLayout;
    }

    /**
     * Get the current UndoListener instance
     * 
     * @return current UndoListener instance or null if it hasn't been set
     */
    @Nullable
    public UndoListener getUndoListener() {
        return undoListener;
    }

    /**
     * Get the current EasyEditManager instance
     * 
     * @return the current EasyEditManager instance
     */
    @NonNull
    public EasyEditManager getEasyEditManager() {
        if (easyEditManager == null) {
            throw new IllegalStateException("called before EasyEditManager was constructed");
        }
        return easyEditManager;
    }

    @Override
    public void setResultListener(int code, Listener listener) {
        activityResultListeners.put(code, listener);
    }

    /**
     * Get a description of an element suitable for display in a context menu
     * 
     * @param e the OsmELement
     * @return the description
     */
    @NonNull
    public String descriptionForContextMenu(@NonNull OsmElement e) {
        StringBuilder description = new StringBuilder(e.getDescription(this));
        List<Relation> relations = e.getParentRelations();
        boolean hasRelations = relations != null && !relations.isEmpty();
        if (e instanceof Node) {
            List<Way> ways = App.getLogic().getWaysForNode((Node) e);
            boolean hasWays = ways != null && !ways.isEmpty();
            if (hasRelations) {
                description.append(" (");
                for (Relation r : relations) {
                    description.append(r.getDescription(this));
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
                    description.append(w.getDescription(this));
                    if (!lastMember(ways, w)) {
                        description.append(", ");
                    }
                }
                description.append(")");
            }
        } else if (hasRelations) {
            description.append(" (");
            for (Relation r : relations) {
                description.append(r.getDescription(this));
                if (!lastMember(relations, r)) {
                    description.append(", ");
                }
            }
            description.append(")");
        }
        return description.toString();
    }

    /**
     * Check if this is the last member of a list
     * 
     * @param <T> type of the List member
     * @param l the list
     * @param o the member we are checking
     * @return true if it is the last item in the list
     */
    private <T> boolean lastMember(@NonNull List<T> l, @NonNull T o) {
        return l.indexOf(o) == (l.size() - 1);
    }
}
