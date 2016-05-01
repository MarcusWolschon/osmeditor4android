package de.blau.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
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
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;
import de.blau.android.GeoUrlActivity.GeoUrlData;
import de.blau.android.Logic.CursorPaddirection;
import de.blau.android.Logic.Mode;
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
import de.blau.android.dialogs.SaveFile;
import de.blau.android.dialogs.SearchForm;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.exception.OsmException;
import de.blau.android.imageryoffset.BackgroundAlignmentActionModeCallback;
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
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.propertyeditor.PropertyEditorData;
import de.blau.android.resources.DataStyle;
import de.blau.android.services.TrackerService;
import de.blau.android.services.TrackerService.TrackerBinder;
import de.blau.android.services.TrackerService.TrackerLocationListener;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.BugFixedAppCompatActivity;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.OAuthHelper;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Search.SearchResult;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.voice.Commands;

/**
 * This is the main Activity from where other Activities will be started.
 * 
 * @author mb
 */
public class Main extends BugFixedAppCompatActivity implements ServiceConnection, TrackerLocationListener, UpdateViewListener {

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
	public static final int REQUEST_EDIT_TAG = 1;
	
	/**
	 * Requests a file as an activity-result.
	 */
	public static final int READ_OSM_FILE_SELECT_CODE = 2;
	
	/**
	 * Requests a file as an activity-result.
	 */
	public static final int WRITE_OSM_FILE_SELECT_CODE = 3;
	
	/**
	 * Requests a file as an activity-result.
	 */
	public static final int READ_GPX_FILE_SELECT_CODE = 4;
	
	/**
	 * Requests an activity-result.
	 */
	public static final int REQUEST_IMAGE_CAPTURE = 5;
	
	/**
	 * Requests voice recognition.
	 */
	public static final int VOICE_RECOGNITION_REQUEST_CODE = 6;
	
	private static final String EASY_TAG = "EASY";
	private static final String TAG_TAG = "TAG";
	
	private static final double DEFAULT_BOUNDING_BOX_RADIUS = 4000000.0D;

	/**
	 * Date pattern used for the image file name.
	 */
	private static final String DATE_PATTERN_IMAGE_FILE_NAME_PART = "yyyyMMdd_HHmmss";
	
	/**
	 * Where we install the current version of vespucci
	 */
	private static final String VERSION_FILE = "version.dat"; 
	
	private class ConnectivityChangedReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
	        	Log.d("ConnectivityChangedReceiver","Received broadcast");
	        	if (easyEditManager.isProcessingAction()) {
	    			easyEditManager.invalidate();
	    		} else {
	    			supportInvalidateOptionsMenu();
	    		}
	        }
		}
	}
	
	private ConnectivityChangedReceiver connectivityChangedReceiver;
	
	/** Objects to handle showing device orientation. */
	private SensorManager sensorManager;
	private Sensor sensor;
	private final SensorEventListener sensorListener = new SensorEventListener() {
		float lastOrientation = -9999;
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
		@Override
		public void onSensorChanged(SensorEvent event) {
			float orientation = event.values[0];
			map.setOrientation(orientation);
			// Repaint map only if orientation changed by at least 1 degree since last repaint
			if (Math.abs(orientation - lastOrientation) > 1) {
				lastOrientation = orientation;
				map.invalidate();
			}
		}
	};
	
	/**
	 * webview for logging in and authorizing OAuth
	 */
	private WebView oAuthWebView;
	
	/**
	 * out main layout
	 */
	private RelativeLayout rl;

	/** The map View. */
	private Map map;
	/** Detector for taps, drags, and scaling. */
	private VersionedGestureDetector mDetector;
	/** Onscreen map zoom controls. */
	private ZoomControls zoomControls;
	/**
	 * Our user-preferences.
	 */
	private Preferences prefs;

	/**
	 * The manager for the EasyEdit mode
	 */
	public EasyEditManager easyEditManager;

	/**
	 * Flag indicating whether the map will be re-downloaded once the activity resumes
	 */
	private static boolean redownloadOnResume;

	/**
	 * Flag indicating whether data should be loaded from a file when the activity resumes.
	 * Set by {@link #onCreate(Bundle)}.
	 * Overridden by {@link #redownloadOnResume}.
	 */
	private boolean loadOnResume;

	private boolean showGPS;
	private boolean followGPS;
	private boolean autoDownload;
	private boolean bugAutoDownload;
	/**
	 * a local copy of the desired value for {@link TrackerService#setListenerNeedsGPS(boolean)}.
	 * Will be automatically given to the tracker service on connect.
	 */
	private boolean wantLocationUpdates = false;
	
	private GeoUrlData geoData = null;
	private RemoteControlUrlData rcData = null;

	/**
	 * The current instance of the tracker service
	 */
	private TrackerService tracker = null;

	public UndoListener undoListener;
	
	private BackgroundAlignmentActionModeCallback backgroundAlignmentActionModeCallback = null; // hack to protect against weird state

	private Location lastLocation = null;
	
	private Location locationForIntent = null;

	/**
	 * file we asked the camera app to create (ugly) 
	 */
	File imageFile = null;

	private PostAsyncActionHandler restart; // if set this is called to restart post authentication

	private boolean gpsChecked = false; // flag to ensure that we only check once per activity life cycle

	protected boolean saveSync = false; // save synchronously instead of async
	
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
		Log.i(DEBUG_TAG, "onCreate " + (savedInstanceState != null?" no saved state " : " saved state exists"));
		// minimal support for geo: uris and JOSM style remote control
		geoData = (GeoUrlData)getIntent().getSerializableExtra(GeoUrlActivity.GEODATA);
		rcData = (RemoteControlUrlData)getIntent().getSerializableExtra(RemoteControlUrlActivity.RCDATA);
		
		prefs = new Preferences(this);
		
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customMain_Light);
		}
		
		super.onCreate(savedInstanceState);
		Application.mainActivity = this;
		
		sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager != null) {
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			if (sensor == null) {
				sensorManager = null;
			}
		}
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		if (prefs.splitActionBarEnabled()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW, ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW); // this might need to be set with bit ops
			}
			// besides hacking ABS, there is no equivalent method to enable this for ABS
		} else {
			requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		}
		
		RelativeLayout ml = (RelativeLayout) getLayoutInflater().inflate(R.layout.main, null);
		rl = (RelativeLayout) ml.findViewById(R.id.mainMap);// new RelativeLayout(getApplicationContext());
		
		if (map != null) {
			map.onDestroy();
		}
		map = new Map(getApplicationContext());
		map.setId(R.id.map_view);
		
		//Register some Listener
		MapTouchListener mapTouchListener = new MapTouchListener();
		map.setOnTouchListener(mapTouchListener);
		map.setOnCreateContextMenuListener(mapTouchListener);
		map.setOnKeyListener(new MapKeyListener());
		
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) { // 12 upwards
			map.setOnGenericMotionListener(new MotionEventListener());
		}
		
		rl.addView(map); 
		
		mDetector = VersionedGestureDetector.newInstance(getApplicationContext(), mapTouchListener);
		
		// Set up the zoom in/out controls
		zoomControls = new ZoomControls(getApplicationContext());
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Application.getLogic().zoom(Logic.ZOOM_IN);
				updateZoomControls();
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Application.getLogic().zoom(Logic.ZOOM_OUT);
				updateZoomControls();
			}
		});

		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.addView(zoomControls, rlp);
		
		setContentView(ml);
		
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
		
		// check if first time user and display something if yes
		SavingHelper<String> savingHelperVersion = new SavingHelper<String>();
		String lastVersion = savingHelperVersion.load(this,VERSION_FILE, false);
		boolean newInstall = (lastVersion == null || lastVersion.equals(""));
		
		loadOnResume = false;
		
		if (Application.getLogic()==null) {
			Log.i(DEBUG_TAG, "onCreate - creating new logic");
			Application.newLogic(map);
		}
		DataStyle p = new DataStyle(getApplicationContext()); // this has side effects and needs to be done now (for now)
		
		Log.d(DEBUG_TAG,"StorageDelegator dirty is " + Application.getDelegator().isDirty());
		if (isLastActivityAvailable() && !Application.getDelegator().isDirty()) { // data was modified while we were stopped if isDirty is true
			// Start loading after resume to ensure loading dialog can be removed afterwards
			loadOnResume = true;
		} else { // the following code should likely be moved to onStart or onResume 
			if (geoData == null && rcData == null && Application.getDelegator().isEmpty()) {
				// check if we have a position
				Location loc = getLastLocation();
				BoundingBox box = null;
				if (loc != null) {
					try {
						box = GeoMath.createBoundingBoxForCoordinates(loc.getLatitude(),
								loc.getLongitude(), DEFAULT_BOUNDING_BOX_RADIUS, true); // km hardwired for now
					} catch (OsmException e) {
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(e);
					}
				} else { // create a largish bb centered on 51.48,0
					try {
						box = GeoMath.createBoundingBoxForCoordinates(51.48,0, DEFAULT_BOUNDING_BOX_RADIUS, false); // km hardwired for now
					} catch (OsmException e) {
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(e);
					}
				}
				openEmptyMap(box);

				// only show box picker if we are not showing welcome dialog
				if (!newInstall) {
					gotoBoxPicker();
				}
			}
		}
	
		easyEditManager = new EasyEditManager(this);
		
		// show welcome dialog
		if (newInstall) {
			// newbie, display welcome dialog
			Log.d(DEBUG_TAG,"showing welcome dialog");
			Newbie.showDialog(this);
		} else {
			String currentVersion = getString(R.string.app_version);
			if (lastVersion.length()<5 || !lastVersion.subSequence(0,5).equals(currentVersion.subSequence(0,5))) { // lastVersion already checked against null
				Log.d(DEBUG_TAG,"new version");
				NewVersion.showDialog(this);
			}
		}
		savingHelperVersion.save(this,VERSION_FILE, getString(R.string.app_version), false);
	}
	
	/**
	 * Get the best last position
	 */
	private Location getLastLocation() {
		LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(true);
		Location bestLocation = null;
		for (String provider : providers) {
			try {
				Location location = locationManager.getLastKnownLocation(provider);
				if (bestLocation == null || !bestLocation.hasAccuracy() ||
						(location != null && location.hasAccuracy() &&
								location.getAccuracy() < bestLocation.getAccuracy())) {
					bestLocation = location;
				}
			} catch (IllegalArgumentException e) {
			} catch (SecurityException e) {
			}
		}
		return bestLocation;
	}
	
	/**
	 * Loads the preferences into {@link #map} and {@link #logic}, triggers new {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		Log.d(DEBUG_TAG, "onStart");
		super.onStart();

		prefs = new Preferences(this);
		Application.getLogic().setPrefs(prefs);
		
		// if we have been stopped delegator and viewbox willt not be set if our original Logic instance is still around
		map.setDelegator(Application.getDelegator());
		map.setViewBox(Application.getLogic().getViewBox());
		
		map.setPrefs(prefs);
		map.createOverlays();
		map.requestFocus();
		
		undoListener = new UndoListener();

		showActionBar();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(DEBUG_TAG, "onNewIntent storage dirty " + Application.getDelegator().isDirty());
		setIntent(intent);
		geoData = (GeoUrlData)getIntent().getSerializableExtra(GeoUrlActivity.GEODATA);
		rcData = (RemoteControlUrlData)getIntent().getSerializableExtra(RemoteControlUrlActivity.RCDATA);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(DEBUG_TAG, "onResume");
		final Logic logic = Application.getLogic();
		
		bindService(new Intent(this, TrackerService.class), this, BIND_AUTO_CREATE);
		
		// register received for changes in connectivity
		IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		connectivityChangedReceiver = new ConnectivityChangedReceiver();
		registerReceiver(connectivityChangedReceiver, filter);
		
		if (redownloadOnResume) {
			redownloadOnResume = false;
			logic.downloadLast();
		} else if (loadOnResume) {
			loadOnResume = false;
			PostAsyncActionHandler	postLoad = new PostAsyncActionHandler() {
				@Override
				public void execute() {
					if (rcData != null|| geoData != null) {
						processIntents();
					}
					setupLockButton(getSupportActionBar());
					updateActionbarEditMode();
					if (logic.getMode()==Mode.MODE_EASYEDIT 
							&& (logic.getSelectedNode() != null 
								|| logic.getSelectedWay() != null 
								|| (logic.getSelectedRelations() != null && logic.getSelectedRelations().size() > 0)
								|| logic.getSelectedBug() != null)) {
						// need to restart whatever we were doing
						Log.d(DEBUG_TAG,"restarting action mode");
						Task t = logic.getSelectedBug();
						if (t==null) {
							easyEditManager.editElements();
						} else {
							performBugEdit(t);
						}		
					}
				}
			};
			logic.loadFromFile(this,postLoad);
			logic.loadBugsFromFile(this,null);
		} else { // loadFromFile already does this
			Application.getLogic().loadEditingState();
			processIntents();
			setupLockButton(getSupportActionBar());
			updateActionbarEditMode();
			map.invalidate();
		}
		
		logic.updateProfile();
		map.updateProfile();
		
		runningInstance = this;
	
		if (getTracker() != null) {
			getTracker().setListener(this);
			lastLocation = getTracker().getLastLocation();
		}
		
		setShowGPS(showGPS); // reactive GPS listener if needed, this is probably not necessary since already done when reading the saved edit state
		setFollowGPS(followGPS);
		setAutoDownload(autoDownload);
		
		map.setKeepScreenOn(prefs.isKeepScreenOnEnabled());
	}
	
	/**
	 * Process geo an JOSM remote control intents
	 */
	protected void processIntents() {
		final Logic logic = Application.getLogic();
		if (geoData != null) {
			Log.d(DEBUG_TAG,"got position from geo: url " + geoData.getLat() + "/" + geoData.getLon() + " storage dirty is " + Application.getDelegator().isDirty());
			if (prefs.getDownloadRadius() != 0) { // download
				BoundingBox bbox;
				try {
					bbox = GeoMath.createBoundingBoxForCoordinates(geoData.getLat(), geoData.getLon(), prefs.getDownloadRadius(), true);
					ArrayList<BoundingBox> bbList = new ArrayList<BoundingBox>(Application.getDelegator().getBoundingBoxes());
					ArrayList<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, bbox); 
					if (bboxes != null && bboxes.size() > 0) {
						logic.downloadBox(bbox, true, null); 
					} else {
						logic.getViewBox().setBorders(bbox);
						map.invalidate();
					}
				} catch (OsmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				Log.d(DEBUG_TAG,"moving to position");
				map.getViewBox().moveTo((int)(geoData.getLon()*1E7), (int)(geoData.getLat()*1E7));
				map.invalidate();
			}
			geoData=null; // zap to stop repeated downloads
		} 
		if (rcData != null) {
			Log.d(DEBUG_TAG,"got bbox from remote control url " + rcData.getBox() + " load " + rcData.load());
			if (rcData.load()) { // download
				ArrayList<BoundingBox> bbList = new ArrayList<BoundingBox>(Application.getDelegator().getBoundingBoxes());
				ArrayList<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, rcData.getBox()); 
				if (bboxes != null && bboxes.size() > 0) {
					logic.downloadBox(rcData.getBox(), true /* logic.delegator.isDirty() */, new PostAsyncActionHandler(){
						@Override
						public void execute(){
							rcDataEdit(rcData);
							rcData=null; // zap to stop repeated downloads
						}
					});
				} else {
					rcDataEdit(rcData);
					rcData=null; // zap to stop repeated downloads
				}
			} else {
				Log.d(DEBUG_TAG,"moving to position");
				map.getViewBox().setBorders(rcData.getBox());
				map.invalidate();
				rcData=null; // zap to stop repeated downloads
			}
		}
	}
	
	/**
	 * Parse the parameters of a JOSM remote control URL and select and edit the OSM objects.
	 * @param rcData Data of a remote control data URL.
	 */
	void rcDataEdit(RemoteControlUrlData rcData ) {
		final Logic logic = Application.getLogic();
		if (rcData.getSelect() != null) {
			// need to actually switch to easyeditmode
			if (logic.getMode() != Mode.MODE_EASYEDIT) { // TODO there might be states in which we don't want to exit which ever mode we are in
				setMode(Mode.MODE_EASYEDIT);
			}
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			logic.setSelectedRelation(null);
			StorageDelegator storageDelegator = Logic.getDelegator();
			for (String s:rcData.getSelect().split(",")) { // see http://wiki.openstreetmap.org/wiki/JOSM/Plugins/RemoteControl
				if (s!=null) {
					Log.d(DEBUG_TAG,"rc select: " + s);
					try {
						if (s.startsWith("node")) {
							long id = Long.valueOf(s.substring(Node.NAME.length()));
							Node n = (Node) storageDelegator.getOsmElement(Node.NAME, id);
							if (n != null) {
								logic.addSelectedNode(n);
							}
						} else if (s.startsWith("way")) {
							long id = Long.valueOf(s.substring(Way.NAME.length()));
							Way w = (Way) storageDelegator.getOsmElement(Way.NAME, id);
							if (w != null) {
								logic.addSelectedWay(w);
							}
						} else if (s.startsWith("relation")) {
							long id = Long.valueOf(s.substring(Relation.NAME.length()));
							Relation r = (Relation) storageDelegator.getOsmElement(Relation.NAME, id);
							if (r != null) {
								logic.addSelectedRelation(r);
							}
						}
					} catch (NumberFormatException nfe) {
						Log.d(DEBUG_TAG,"Parsing " + s + " caused " + nfe);
						// not much more we can do here
					}
				}	
			}
			easyEditManager.editElements();		
		}
	}

	@Override
	protected void onPause() {
		Log.d(DEBUG_TAG, "onPause mode " + Application.getLogic().getMode());
		runningInstance = null;
		try {
			unregisterReceiver(connectivityChangedReceiver);
		} catch (Exception e) {
			// FIXME if onPause gets called before onResume has registered the Receiver
			// unregisterReceiver will throw an exception, a better fix would likely to 
			// register earlier, but that may not help
		}
		disableLocationUpdates();
		if (getTracker() != null) getTracker().setListener(null);

		// always save editing state
		Application.getLogic().saveEditingState();
		// onPause is the last lifecycle callback guaranteed to be called on pre-honeycomb devices
		// on honeycomb and later, onStop is also guaranteed to be called, so we can defer saving.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) saveData();
		
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		Log.d(DEBUG_TAG, "onStop");
		// editing state has been saved in onPause
	
		// On devices with Android versions before Honeycomb, we already save data in onPause
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) saveData();
		
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(DEBUG_TAG, "onDestroy");
		map.onDestroy();
		if (getTracker() != null) getTracker().setListener(null);
		try {
			unbindService(this);
		} catch (Exception ignored) {} // ignore errors, this is just cleanup
		super.onDestroy();
	}

	/**
	 * Save current data (state, downloaded data, changes, ...) to file(s)
	 */
	private void saveData() {
		Log.i(DEBUG_TAG, "saving data sync="+saveSync);
		final Logic logic = Application.getLogic();
		if (saveSync) {
			logic.save();
		} else {
			logic.saveAsync();
		}
	}

	/**
	 * Update the state of the onscreen zoom controls to reflect their ability
	 * to zoom in/out.
	 */
	private void updateZoomControls() {
		final Logic logic = Application.getLogic();
		zoomControls.setIsZoomInEnabled(logic.canZoom(Logic.ZOOM_IN));
		zoomControls.setIsZoomOutEnabled(logic.canZoom(Logic.ZOOM_OUT));
	}
	
//	@Override
//	public Object onRetainNonConfigurationInstance() {
//		Log.d(DEBUG_TAG, "onRetainNonConfigurationInstance");
//		return logic;
//	}

	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(DEBUG_TAG, "onConfigurationChanged");
		if (easyEditManager.isProcessingAction()) {
			easyEditManager.invalidate();
		}
	}

	
	/**
	 * Sets up the Action Bar.
	 */
	private void showActionBar() {
		Log.d(DEBUG_TAG, "showActionBar");
		final ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM|ActionBar.DISPLAY_SHOW_HOME);
		setupLockButton(actionbar);
		actionbar.show();
		setSupportProgressBarIndeterminateVisibility(false);
		Util.resetProgressBarShown();
	}
	
	/**
	 * slightly byzantine code for mode switching follows
	 * @param actionbar Action bar of this Activity.
	 */
	@SuppressLint("InflateParams")
	void setupLockButton(final ActionBar actionbar)	{
		final Logic logic = Application.getLogic();
		// inflating will crash without themed context
		Context context =  new ContextThemeWrapper(this, prefs.lightThemeEnabled() ? R.style.Theme_customMain_Light : R.style.Theme_customMain);
		final View lockLayout =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.lock, null);
		actionbar.setCustomView(lockLayout);
		Mode mode = logic.getMode();
		Log.d(DEBUG_TAG, "setupLockButton mode " + mode);
		ToggleButton lock = setLock(mode);
		if (lock == null) {
			return; //FIXME not good but no other alternative right now, already logged in setLock
		}
		if (mode == Mode.MODE_EASYEDIT) {
			lock.setButtonDrawable(ThemeUtils.getResIdFromAttribute(Application.mainActivity,R.attr.lock));
			lock.setTag(EASY_TAG);
		} else if ((mode == Mode.MODE_TAG_EDIT)) {
			lock.setButtonDrawable(ThemeUtils.getResIdFromAttribute(Application.mainActivity,R.attr.lock_tag));
			lock.setTag(TAG_TAG);
		} else {
			lock.setButtonDrawable(ThemeUtils.getResIdFromAttribute(Application.mainActivity,R.attr.lock));
			lock.setTag(EASY_TAG);
		}
		
		findViewById(R.id.lock).setVisibility(View.VISIBLE);
		lock.setLongClickable(true);
		lock.setOnClickListener(new View.OnClickListener() {
		    @Override
			public void onClick(View b) {
		        Log.d(DEBUG_TAG, "Lock pressed");
		        if(((ToggleButton)b).isChecked()) {
		        	if (b.getTag().equals(EASY_TAG)) { 
		        		logic.setMode(Logic.Mode.MODE_EASYEDIT);
		        	} else {
		        		logic.setMode(Logic.Mode.MODE_TAG_EDIT);
		        	}
		        } else {
		        	logic.setMode(Logic.Mode.MODE_MOVE);
		        }
		        onEditModeChanged();
		    }
		});
		lock.setOnLongClickListener(new View.OnLongClickListener() {
		    @Override
			public boolean onLongClick(View b) {
		        Log.d(DEBUG_TAG, "Lock long pressed"); 
		        final Logic logic = Application.getLogic();
		        if(((ToggleButton)b).isChecked()) {
		        	if (logic.getMode() == Logic.Mode.MODE_TAG_EDIT) {
		        		logic.setMode(Logic.Mode.MODE_EASYEDIT);
		        		((ToggleButton)b).setButtonDrawable(ThemeUtils.getResIdFromAttribute(Application.mainActivity,R.attr.lock));
		        		((ToggleButton)b).setChecked(true);
		        		b.setTag(EASY_TAG);
		        	} else {
		        		logic.setMode(Logic.Mode.MODE_TAG_EDIT);
		        		((ToggleButton)b).setButtonDrawable(ThemeUtils.getResIdFromAttribute(Application.mainActivity,R.attr.lock_tag));
		        		((ToggleButton)b).setChecked(true);
		        		b.setTag(TAG_TAG);
		        	}
		        } else {
		        	if (b.getTag().equals(EASY_TAG)) {
		        		logic.setMode(Logic.Mode.MODE_TAG_EDIT);
	        			((ToggleButton)b).setButtonDrawable(ThemeUtils.getResIdFromAttribute(Application.mainActivity,R.attr.lock_tag));
	        			((ToggleButton)b).setChecked(true);
	        			b.setTag(TAG_TAG);
		        	} else {
		        		logic.setMode(Logic.Mode.MODE_EASYEDIT);
		        		((ToggleButton)b).setButtonDrawable(ThemeUtils.getResIdFromAttribute(Application.mainActivity,R.attr.lock));
		        		((ToggleButton)b).setChecked(true);
		        		b.setTag(EASY_TAG);
		        	}
		        }
		        onEditModeChanged();
		        return true;
		    }
		});
	}	
		
	/**
	 * Set lock button to locked or unlocked depending on the edit mode
	 * @param mode Program mode.
	 * @return Button to display checked/unchecked states.
	 */
	private ToggleButton setLock(Logic.Mode mode) {
		ToggleButton lock = (ToggleButton) findViewById(R.id.lock);
		if (lock==null) {
			Log.d(DEBUG_TAG, "couldn't find lock button");
			return null;
		}
		switch (mode) {
		case MODE_EASYEDIT:
		case MODE_ALIGN_BACKGROUND:
		case MODE_TAG_EDIT:
			lock.setChecked(true);
			break;
		default: 
			mode = Mode.MODE_MOVE;
			lock.setChecked(false);
		}
		Application.getLogic().setMode(mode); 
		return lock; // for convenience
	}

	public void setMode(Logic.Mode mode) {
		Application.getLogic().setMode(mode); 
	}
	
	public void updateActionbarEditMode() {
		Log.d(DEBUG_TAG, "updateActionbarEditMode");
		setLock(Application.getLogic().getMode());
		supportInvalidateOptionsMenu();
	}
	
	public static void onEditModeChanged() {
		Log.d(DEBUG_TAG, "onEditModeChanged");
		if (runningInstance != null) runningInstance.updateActionbarEditMode();
	}
	
	/**
	 * Creates the menu from the XML file "main_menu.xml".<br> {@inheritDoc}
	 * 
	 * Note for not entirely clear reasons *:setShowAsAction doesn't work in the menu definition and has to be done programmatically here.
	 */
 	@SuppressLint("InflateParams")
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		Log.d(DEBUG_TAG, "onCreateOptionsMenu");
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		// only show camera icon if we have a camera, and a camera app is installed 
		PackageManager pm = getPackageManager();
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && cameraIntent.resolveActivity(getPackageManager()) != null) {
			MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_camera),prefs.showCameraAction() ? MenuItemCompat.SHOW_AS_ACTION_ALWAYS: MenuItemCompat.SHOW_AS_ACTION_NEVER);
		} else {
			menu.findItem(R.id.menu_camera).setVisible(false);
		}
		
		boolean networkConnected = NetworkStatus.isConnected(this);
		boolean gpsProviderEnabled = ensureGPSProviderEnabled();
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_gps),MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menu.findItem(R.id.menu_gps_show).setEnabled(gpsProviderEnabled).setChecked(showGPS);
		menu.findItem(R.id.menu_gps_follow).setEnabled(gpsProviderEnabled).setChecked(followGPS);
		menu.findItem(R.id.menu_gps_goto).setEnabled(gpsProviderEnabled);
		menu.findItem(R.id.menu_gps_start).setEnabled(getTracker() != null && !getTracker().isTracking() && gpsProviderEnabled);
		menu.findItem(R.id.menu_gps_pause).setEnabled(getTracker() != null && getTracker().isTracking() && gpsProviderEnabled);
		menu.findItem(R.id.menu_gps_autodownload).setEnabled(getTracker() != null && gpsProviderEnabled && networkConnected).setChecked(autoDownload);
		menu.findItem(R.id.menu_transfer_bugs_autodownload).setEnabled(getTracker() != null && gpsProviderEnabled && networkConnected).setChecked(bugAutoDownload);
		
		menu.findItem(R.id.menu_gps_clear).setEnabled(getTracker() != null && getTracker().getTrackPoints() != null && getTracker().getTrackPoints().size() > 0);
		menu.findItem(R.id.menu_gps_goto_start).setEnabled(getTracker() != null && getTracker().getTrackPoints() != null && getTracker().getTrackPoints().size() > 0);
		menu.findItem(R.id.menu_gps_import).setEnabled(getTracker() != null);
		menu.findItem(R.id.menu_gps_upload).setEnabled(getTracker() != null && getTracker().getTrackPoints() != null && getTracker().getTrackPoints().size() > 0 && NetworkStatus.isConnected(this));
		
		final Logic logic = Application.getLogic();
		MenuItem undo = menu.findItem(R.id.menu_undo);
		MenuItemCompat.setShowAsAction(undo, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		undo.setVisible(logic.getMode() != Mode.MODE_MOVE && (logic.getUndo().canUndo() || logic.getUndo().canRedo()));
		View undoView = MenuItemCompat.getActionView(undo);
		if (undoView == null) { // FIXME this is a temp workaround for pre-11 Android, we could probably simply always do the following 
			Log.d(DEBUG_TAG,"undoView null");
			Context context =  new ContextThemeWrapper(this, prefs.lightThemeEnabled() ? R.style.Theme_customMain_Light : R.style.Theme_customMain);
			undoView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.undo_action_view, null);
			MenuItemCompat.setActionView(undo, undoView);
		}
		undoView.setOnClickListener(undoListener);
		undoView.setOnLongClickListener(undoListener);
		
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_transfer), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		final Server server = prefs.getServer();
		if (server.hasOpenChangeset()) {
			menu.findItem(R.id.menu_transfer_close_changeset).setVisible(true);
		} else {
			menu.findItem(R.id.menu_transfer_close_changeset).setVisible(false);
		}
			
		menu.findItem(R.id.menu_transfer_download_current).setEnabled(networkConnected);
		menu.findItem(R.id.menu_transfer_download_current_add).setEnabled(networkConnected);
		menu.findItem(R.id.menu_transfer_download_other).setEnabled(networkConnected);
		menu.findItem(R.id.menu_transfer_upload).setEnabled(networkConnected);
		menu.findItem(R.id.menu_transfer_bugs_download_current).setEnabled(networkConnected);
		menu.findItem(R.id.menu_transfer_bugs_upload).setEnabled(networkConnected);
		menu.findItem(R.id.menu_voice).setVisible(false); // don't display button for now
//		menu.findItem(R.id.menu_voice).setEnabled(networkConnected && prefs.voiceCommandsEnabled()).setVisible(prefs.voiceCommandsEnabled());
		
		// set showAsAction value for rest of top level menus
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_voice), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_config), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_tools), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_find), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_help), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		
		return true;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d(DEBUG_TAG, "onOptionsItemSelected");
		final Server server = prefs.getServer();
		final Logic logic = Application.getLogic();
		switch (item.getItemId()) {
		case R.id.menu_config:
			PrefEditor.start(this);
			return true;
			
		case R.id.menu_find:
			SearchForm.showDialog(this,  new de.blau.android.util.SearchItemFoundCallback() {
				private static final long serialVersionUID = 1L;

				@Override
				public void onItemFound(SearchResult sr) {
					// turn this off or else we get bounced back to our current GPS position
					setFollowGPS(false);
					getMap().setFollowGPS(false);
					logic.setZoom(19);
					getMap().getViewBox().moveTo((int) (sr.getLon() * 1E7d), (int)(sr.getLat()* 1E7d));
					getMap().invalidate();
				}
			});
			return true;
			
		case R.id.menu_help:
			HelpViewer.start(this, R.string.help_main);
			return true;
			
//		case R.id.menu_voice:		
//
//			return true;
			
		case R.id.menu_camera:
			Intent startCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			try {
				imageFile = getImageFile();
				startCamera.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(imageFile));
				startActivityForResult(startCamera, REQUEST_IMAGE_CAPTURE);	
			} catch (Exception ex) {
				try {
					Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_camera_error, ex.getMessage()), Toast.LENGTH_LONG).show();
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
				map.getViewBox().moveTo((int) (gotoLoc.getLongitude() * 1E7d), (int) (gotoLoc.getLatitude() * 1E7d));
				logic.setZoom(19);
				map.setLocation(gotoLoc);
				map.invalidate();
			}
			return true;

		case R.id.menu_gps_start:
			if (getTracker() != null && ensureGPSProviderEnabled()) {
				getTracker().startTracking();
				setFollowGPS(true);
				triggerMenuInvalidation();
			}
			return true;

		case R.id.menu_gps_pause:
			if (getTracker() != null && ensureGPSProviderEnabled()) {
				getTracker().stopTracking(false);
				triggerMenuInvalidation();
			}
			return true;

		case R.id.menu_gps_clear:
			if (getTracker() != null) getTracker().stopTracking(true);
			triggerMenuInvalidation();
			map.invalidate();
			return true;
			
		case R.id.menu_gps_upload:
			if (server != null && server.isLoginSet()) {
				if (server.needOAuthHandshake()) {
					oAuthHandshake(server, new PostAsyncActionHandler() {
						@Override
						public void execute() {
							GpxUpload.showDialog(Main.this);
						}
					});
					if (server.getOAuth()) { // if still set
						Toast.makeText(getApplicationContext(), R.string.toast_oauth, Toast.LENGTH_LONG).show();
						return true;
					} 
				}	
				GpxUpload.showDialog(this);
				// performTrackUpload("Test","Test",Visibility.PUBLIC);
			} else {
				ErrorAlert.showDialog(this,ErrorCodes.NO_LOGIN_DATA);
			}
			return true;

		case R.id.menu_gps_export:
			if (getTracker() != null) {
				SavingHelper.asyncExport(this, getTracker());
			}
			return true;
			
		case R.id.menu_gps_import:
			if (Application.getDelegator() == null) return true;
			showFileChooser(READ_GPX_FILE_SELECT_CODE);
			return true;
			
		case R.id.menu_gps_goto_start:
			List<TrackPoint> l = tracker.getTrackPoints();
			if (l != null && l.size() > 0) {
				Log.d(DEBUG_TAG,"Going to start of track");
				setFollowGPS(false);
				map.setFollowGPS(false);
				map.getViewBox().moveTo(l.get(0).getLon(), l.get(0).getLat());
				logic.setZoom(19);
				map.invalidate();
			}
			return true;

		case R.id.menu_gps_autodownload:
			setAutoDownload(!autoDownload);
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
			StorageDelegator storageDelegator = Application.getDelegator();
			if (storageDelegator == null) return true;
			SavingHelper.asyncExport(this, storageDelegator);
			return true;
		}
		case R.id.menu_transfer_read_file:
			if (Application.getDelegator() == null) return true;
			showFileChooser(READ_OSM_FILE_SELECT_CODE);
			return true;
			
		case R.id.menu_transfer_save_file:
			if (Application.getDelegator() == null) return true;
			SaveFile.showDialog(this);
//			showFileChooser(WRITE_OSM_FILE_SELECT_CODE);
			return true;
		
		case R.id.menu_transfer_bugs_download_current:
			TransferTasks.downloadBox(this, prefs.getServer(), map.getViewBox().copy(), true, new PostAsyncActionHandler() {
				@Override
				public void execute() {
					map.invalidate();
				}
			});
			return true;
			
		case R.id.menu_transfer_bugs_upload:
			if (Application.getTaskStorage().hasChanges()) {
				TransferTasks.upload(this, server);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}
			return true;
			
		case R.id.menu_transfer_bugs_clear:
			if (Application.getTaskStorage().hasChanges()) { // FIXME show a dialog and allow override
				Toast.makeText(getApplicationContext(), R.string.toast_unsaved_changes, Toast.LENGTH_LONG).show();
				return true;
			}
			Application.getTaskStorage().reset();
			map.invalidate();
			return true;
			
		case R.id.menu_transfer_bugs_autodownload:
			setBugAutoDownload(!bugAutoDownload);
			return true;
			
		case R.id.menu_undo:
			// should not happen
			undoListener.onClick(null);
			return true;
			
		case R.id.menu_tools_flush_background_tile_cache:
			map.getOpenStreetMapTilesOverlay().flushTileCache();
			return true;
			
		case R.id.menu_tools_flush_overlay_tile_cache:
			map.getOpenStreetMapOverlayTilesOverlay().flushTileCache();
			return true;
			
		case R.id.menu_tools_background_align:
			Mode oldMode = logic.getMode() != Mode.MODE_ALIGN_BACKGROUND ? logic.getMode() : Mode.MODE_MOVE; // protect against weird state
			backgroundAlignmentActionModeCallback = new BackgroundAlignmentActionModeCallback(oldMode);
			logic.setMode(Mode.MODE_ALIGN_BACKGROUND); //NOTE needs to be after instance creation
			startSupportActionMode(getBackgroundAlignmentActionModeCallback());
			return true;
			
		case R.id.menu_tools_background_properties:
			BackgroundProperties.showDialog(this);
			return true;
			
		case R.id.menu_tools_oauth_reset: // reset the current OAuth tokens
			if (server.getOAuth()) {
				AdvancedPrefDatabase prefdb = new AdvancedPrefDatabase(this);	
				prefdb.setAPIAccessToken(null, null);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_oauth_not_enabled, Toast.LENGTH_LONG).show();
			}
			return true;
			
		case R.id.menu_tools_oauth_authorisation: // immediately start authorization handshake
			if (server.getOAuth()) {
				oAuthHandshake(server, null);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_oauth_not_enabled, Toast.LENGTH_LONG).show();
			}
			return true;
		}	
		return false;
	}

	private void startVoiceRecognition() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		try {
			startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
		} catch (Exception ex) {
			Log.d(DEBUG_TAG,"Caught exception " + ex);
			Toast.makeText(getApplicationContext(),R.string.toast_no_voice, Toast.LENGTH_LONG).show();
		}
	}
	
	private File getImageFile() throws IOException {
	    File outDir = FileUtil.getPublicDirectory();
	    outDir = FileUtil.getPublicDirectory(outDir, Paths.DIRECTORY_PATH_PICTURES);
	    String imageFileName = DateFormatter.getFormattedString(DATE_PATTERN_IMAGE_FILE_NAME_PART);
	    File imageFile = File.createTempFile(imageFileName, Paths.FILE_EXTENSION_IMAGE, outDir);
	    Log.d(DEBUG_TAG, "createImageFile " + imageFile.getAbsolutePath());
	    return imageFile;
	}
	
	private void showFileChooser(int purpose) {
	    Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
	    intent.setType("*/*"); 
	    intent.addCategory(Intent.CATEGORY_OPENABLE);

	    try {
//	        startActivityForResult(
//	                Intent.createChooser(intent, purpose == WRITE_OSM_FILE_SELECT_CODE ? getString(R.string.save_file) : getString(R.string.read_file)),
//	                purpose);
	    	startActivityForResult(intent,purpose);
	    } catch (android.content.ActivityNotFoundException ex) {
	        // Potentially direct the user to the Market with a Dialog
	        Toast.makeText(getApplicationContext(), R.string.toast_missing_filemanager, 
	                Toast.LENGTH_SHORT).show();
	    }
	}
	
	public void setAutoDownload(boolean b) {
		Log.d(DEBUG_TAG, "autoDownload: "+ b);
		autoDownload = b;
		if (getTracker() != null && ensureGPSProviderEnabled()) {
			if (autoDownload) {
				getTracker().startAutoDownload();
			} else {
				getTracker().stopAutoDownload();
			}
		}
		Log.d(DEBUG_TAG,"Setting autoDownload to " + autoDownload);
		triggerMenuInvalidation();
	}
	
	protected boolean getAutoDownload() {
		return autoDownload;
	}
	
	public void setBugAutoDownload(boolean b) {
		Log.d(DEBUG_TAG, "bugAutoDownload: "+ b);
		bugAutoDownload = b;
		if (getTracker() != null && ensureGPSProviderEnabled()) {
			if (bugAutoDownload) {
				getTracker().startBugAutoDownload();
			} else {
				getTracker().stopBugAutoDownload();
			}
		}
		Log.d(DEBUG_TAG,"Setting bugAutoDownload to " + bugAutoDownload);
		triggerMenuInvalidation();
	}
	
	protected boolean getBugAutoDownload() {
		return bugAutoDownload;
	}
	
	public void setShowGPS(boolean show) {
		if (show && !ensureGPSProviderEnabled()) {
			show = false;
		}
		showGPS = show;
		Log.d(DEBUG_TAG, "showGPS: "+ show);
		if (show) {
			enableLocationUpdates();
		} else {
			setFollowGPS(false);
			map.setLocation(null);
			disableLocationUpdates();
		}
		map.invalidate();
		triggerMenuInvalidation();
	}
	
	protected boolean getShowGPS() {
		return showGPS;
	}
	
	/**
	 * Checks if GPS is enabled in the settings.
	 * If not, returns false and shows location settings.
	 * @return true if GPS is enabled, false if not
	 */
	private boolean ensureGPSProviderEnabled() {
		LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		try {
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				gpsChecked = false;
				return true;
			} else if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null){ // check if there is a GPS providers at all
				if (!gpsChecked && !prefs.leaveGpsDisabled()) {
					gpsChecked = true;
					startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				}
				return false;
			} 
			//
//			Log.d(DEBUG_TAG,"No GPS provider");
//			List<String> providers = locationManager.getAllProviders();
//			for (String p:providers) {
//				Log.d(DEBUG_TAG,"Provider: " + p);
//			}
			return false;
		} catch (Exception e) {
			Log.d(DEBUG_TAG, "Error when checking for GPS, assuming GPS not available", e);
			Toast.makeText(getApplicationContext(), R.string.gps_failure, Toast.LENGTH_SHORT).show();
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
		if (follow && lastLocation != null) { // update if we are returning from pause/stop
			Log.d(DEBUG_TAG,"Setting lastLocation");
			onLocationChanged(lastLocation);
		}
	}
	
	private void toggleShowGPS() {
		boolean newState = !showGPS;
		setShowGPS(newState);
	}
	
	
	private void toggleFollowGPS() {
		boolean newState = !followGPS;
		setFollowGPS(newState);
		map.setFollowGPS(newState);
	}
	
	private void enableLocationUpdates() {
		//noinspection PointlessBooleanExpression
		if (wantLocationUpdates == true) return;
		if (sensorManager != null) {
			sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_UI);
		}
		wantLocationUpdates = true;
		if (getTracker() != null) getTracker().setListenerNeedsGPS(true);
	}
	
	private void disableLocationUpdates() {
		//noinspection PointlessBooleanExpression
		if (wantLocationUpdates == false) return;
		if (sensorManager != null) sensorManager.unregisterListener(sensorListener);
		wantLocationUpdates  = false;
		if (getTracker() != null) getTracker().setListenerNeedsGPS(false);
	}

	/**
	 * Handles the menu click on "download current view".<br>
	 * When no {@link #delegator} is set, the user will be redirected to AreaPicker.<br>
	 * When the user made some changes, {@link #DIALOG_TRANSFER_DOWNLOAD_CURRENT_WITH_CHANGES} will be shown.<br>
	 * Otherwise the current viewBox will be re-downloaded from the server.
	 * @param add Boolean flag indicating to handle changes (true) or not (false).
	 */
	private void onMenuDownloadCurrent(boolean add) {
		Log.d(DEBUG_TAG, "onMenuDownloadCurrent");
		if (Application.getLogic().hasChanges() && !add) {
			DownloadCurrentWithChanges.showDialog(this);
		} else {
			performCurrentViewHttpLoad(add);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		Log.d(DEBUG_TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		final Logic logic = Application.getLogic();
		if (requestCode == REQUEST_BOUNDING_BOX && data != null) {
			handleBoxPickerResult(resultCode, data);
		} else if (requestCode == REQUEST_EDIT_TAG && resultCode == RESULT_OK && data != null) {
			handlePropertyEditorResult(data);
		} else if (requestCode == READ_OSM_FILE_SELECT_CODE && resultCode == RESULT_OK) {
			// Get the Uri of the selected file 
	        Uri uri = data.getData();
	        Log.d(DEBUG_TAG, "Read file Uri: " + uri.toString());
	        try {
				logic.readOsmFile(uri, false);
			} catch (FileNotFoundException e) {
				try {
					Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_file_not_found, uri.toString()), Toast.LENGTH_LONG).show();
				} catch (Exception ex) {
					// protect against translation errors
				}
			}
	        map.invalidate();
		} else if (requestCode == WRITE_OSM_FILE_SELECT_CODE && resultCode == RESULT_OK) {
			// Get the Uri of the selected file 
	        Uri uri = data.getData();
	        Log.d(DEBUG_TAG, "Write file Uri: " + uri.toString());
	        logic.writeOsmFile(uri.getPath());
	        map.invalidate();
		} else if (requestCode == READ_GPX_FILE_SELECT_CODE && resultCode == RESULT_OK) {
			// Get the Uri of the selected file 
	        Uri uri = data.getData();
	        Log.d(DEBUG_TAG, "Read gpx file Uri: " + uri.toString());
	        if (getTracker() != null) {
	        	if (getTracker().getTrackPoints().size() > 0 ) {
	        		ImportTrack.showDialog(this, uri);
	        	} else {
	        		getTracker().stopTracking(false);
	        		try {
						getTracker().importGPXFile(uri);
					} catch (FileNotFoundException e) {
						try {
							Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_file_not_found, uri.toString()), Toast.LENGTH_LONG).show();
						} catch (Exception ex) {
							// protect against translation errors
						}
					}
	        	}
	        }
	        map.invalidate();
		} else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK ) {
			// reindexPhotos();
			if (imageFile != null) {
				PhotoIndex pi = new PhotoIndex(this);
				pi.addPhoto(imageFile);
				if (prefs.isPhotoLayerEnabled()) {
					map.invalidate();
				}
			} else {
				Log.e(DEBUG_TAG,"imageFile == null");
			}
		} else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
			if (easyEditManager.isProcessingAction()) {
				easyEditManager.handleActivityResult(requestCode, resultCode, data);
			} else {
				(new Commands(this)).processIntentResult(data,locationForIntent);
				locationForIntent = null;
				map.invalidate();
			}
		}
	}

	/**
	 * @param resultCode The integer result code returned by the child activity
	 *                   through its setResult().
	 * @param data An Intent, which can return result data to the caller
	 *             (various data can be attached to Intent "extras").
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
				Log.d(DEBUG_TAG,"opening empty map on " + (box != null ? box.toString() : " null bbox"));
				openEmptyMap(box); // we may have a valid box
			}
		} catch (OsmException e) {
			//Values should be done checked in LocationPicker.
			Log.e(DEBUG_TAG, "OsmException", e);
		}
	}

	/**
	 * Handle the result of the property editor
	 * @param data An Intent, which can return result data to the caller
	 *             (various data can be attached to Intent "extras").
	 */
	private void handlePropertyEditorResult(final Intent data) {
		final Logic logic = Application.getLogic();
		Bundle b = data.getExtras();
		if (b != null && b.containsKey(PropertyEditor.TAGEDIT_DATA)) {
			// Read data from extras
			PropertyEditorData[] result = PropertyEditorData.deserializeArray(b.getSerializable(PropertyEditor.TAGEDIT_DATA));
			// FIXME Problem saved data may not be read at this point, load here 
			if (loadOnResume) { 
				loadOnResume = false;
				Log.d(DEBUG_TAG,"handlePropertyEditorResult loading data");
				logic.syncLoadFromFile(); // sync load
				Application.getTaskStorage().readFromFile(this);
			}
			for (PropertyEditorData editorData:result) {
				if (editorData == null) {
					Log.d(DEBUG_TAG,"handlePropertyEditorResult null result");
					continue;
				}
				if (editorData.tags != null) {
					Log.d(DEBUG_TAG,"handlePropertyEditorResult setting tags");
					logic.setTags(editorData.type, editorData.osmId, editorData.tags);		
				}
				if (editorData.parents != null) {
					Log.d(DEBUG_TAG,"handlePropertyEditorResult setting parents");
					logic.updateParentRelations(editorData.type, editorData.osmId, editorData.parents);		
				}
				if (editorData.members != null && editorData.type.equals(Relation.NAME)) {
					Log.d(DEBUG_TAG,"handlePropertyEditorResult setting members");
					logic.updateRelation(editorData.osmId, editorData.members);
				}
			}
			// this is very expensive: getLogic().saveAsync(); // if nothing was changed the dirty flag wont be set and the save wont actually happen 
		}
		if ((logic.getMode()==Mode.MODE_EASYEDIT && easyEditManager != null && !easyEditManager.isProcessingAction()) || logic.getMode()==Mode.MODE_TAG_EDIT) {
			// not in an easy edit mode, de-select objects avoids inconsistent visual state 
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			logic.setSelectedRelation(null);
		} else {
			// invalidate the action mode menu ... updates the state of the undo button
			supportInvalidateOptionsMenu();
			if (easyEditManager != null) {
				easyEditManager.invalidate();
			}
		}
		map.invalidate();
	}
	
	/**
	 * Restore the file name for a photograph
	 * @param savedImageFileName Image file name.
	 */
	public void setImageFileName(String savedImageFileName) {
		if (savedImageFileName != null) {
			imageFile = new File(savedImageFileName);
		}
	}

	/**
	 * Return the file name for a photograph
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

	public void performCurrentViewHttpLoad(boolean add) {
		Application.getLogic().downloadCurrent(add);
		if (prefs.isOpenStreetBugsEnabled()) { // always adds bugs for now
			TransferTasks.downloadBox(this, prefs.getServer(), map.getViewBox().copy(), true, new PostAsyncActionHandler() {
				@Override
				public void execute() {
					map.invalidate();
				}
			});
		}
	}

	private void performHttpLoad(final BoundingBox box) {
		Application.getLogic().downloadBox(box, false, null);
	}

	private void openEmptyMap(final BoundingBox box) {
		Application.getLogic().newEmptyMap(box);
	}

	/**
	 * @param comment Textual comment associated with the change set.
	 * @param source Source of the change.
	 * @param closeChangeset Boolean flag indicating whether the change set
	 *                       should be closed or kept open.
	 */
	public void performUpload(final String comment, final String source, final boolean closeChangeset) {
		final Logic logic = Application.getLogic();
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			boolean hasDataChanges = logic.hasChanges();
			boolean hasBugChanges = !Application.getTaskStorage().isEmpty() && Application.getTaskStorage().hasChanges();
			if (hasDataChanges || hasBugChanges) {
				if (hasDataChanges) {
					logic.upload(comment, source, closeChangeset);
				}
				if (hasBugChanges) {
					TransferTasks.upload(this, server);
				}
				logic.checkForMail();
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}
		} else {
			ErrorAlert.showDialog(this,ErrorCodes.NO_LOGIN_DATA);
		}
	}
	
	/**
	 * 
	 */
	public void performTrackUpload(final String description, final String tags, final Visibility visibility) {
		
		final Logic logic = Application.getLogic();
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			logic.uploadTrack(getTracker().getTrack(), description, tags, visibility);
			logic.checkForMail();
		} else {
			ErrorAlert.showDialog(this,ErrorCodes.NO_LOGIN_DATA);
		}
	}
	

	/**
	 * 
	 */
	public void confirmUpload() {
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			if (Application.getLogic().hasChanges()) {
				if (server.needOAuthHandshake()) {
					oAuthHandshake(server, new PostAsyncActionHandler() {
						@Override
						public void execute() {
							ConfirmUpload.showDialog(Main.this);
						}
					});
					if (server.getOAuth()) // if still set
						Toast.makeText(getApplicationContext(), R.string.toast_oauth, Toast.LENGTH_LONG).show();
					return;
				} 
				ConfirmUpload.showDialog(Main.this);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}		
		} else {
			ErrorAlert.showDialog(this,ErrorCodes.NO_LOGIN_DATA);
		}
	}
	
	
	/**
	 * @param server Server properties.
	 * @param restart Handler to be executed after asynchronous action have been performed.
	 */
	@SuppressLint({ "SetJavaScriptEnabled", "InlinedApi", "NewApi" })
	public void oAuthHandshake(Server server, PostAsyncActionHandler restart) {
		this.restart = restart;
		ActionBar actionbar = getSupportActionBar();
		actionbar.hide();
		Server[] s = {server};
		String url = s[0].getBaseURL();
		OAuthHelper oa;
		try {
			oa = new OAuthHelper(url);
		}
		catch (OsmException oe) {
			server.setOAuth(false); // ups something went wrong turn oauth off
			actionbar.show();
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_no_oauth), Toast.LENGTH_LONG).show();
			return;
		}
		Log.d(DEBUG_TAG, "oauth auth url " + url);
	
		String authUrl = oa.getRequestToken();
		if (authUrl == null) {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_oauth_handshake_failed), Toast.LENGTH_LONG).show();
			actionbar.show();
			return;
		}
		Log.d(DEBUG_TAG, "authURl " + authUrl);
		oAuthWebView = new WebView(Application.mainActivity);
		rl.addView(oAuthWebView);
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
			final int LOADS = 2;
			int times = 0; // counter to avoid redisplaying dialog after first complete load
			
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
		    	if (!url.contains("vespucci")) {
		            // load in in this webview
		            view.loadUrl(url);
		            return true;
		        }
		        // vespucci URL
		        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		        startActivity(intent);
		        return true;
		    }

		    @Override
		    public void onPageStarted(WebView view, String url, Bitmap favicon){
		    	if (times < LOADS) {
		    		Progress.showDialog(Main.this, Progress.PROGRESS_OAUTH);
		    	}
		    }
		    
		    @Override
		    public void onPageFinished(WebView view, String url){
		    	if (times < LOADS) {
		    		times++;
		    		Progress.dismissDialog(Main.this, Progress.PROGRESS_OAUTH);
		    	}
		    }
		}
		
		oAuthWebView.setWebViewClient(new MyWebViewClient());
		oAuthWebView.loadUrl(authUrl);
	}
	
	/**
	 * removes the webview
	 */
	public void finishOAuth() {
		Log.d(DEBUG_TAG,"finishOAuth");
		rl.removeView(oAuthWebView);
		ActionBar actionbar = getSupportActionBar();
		actionbar.show();
		try {
			// the below loadUrl, even though the "official" way to do it,
			// seems to be prone to crash on some devices.
			oAuthWebView.loadUrl("about:blank"); // workaround clearView issues
			oAuthWebView.setVisibility(View.GONE);
			oAuthWebView.removeAllViews();
			oAuthWebView.destroy();
			oAuthWebView = null; 
			if (restart != null) {
				restart.execute();
			}
		} catch (Exception ex) { 
			ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(ex);
		}
	}

	/**
	 * Starts the LocationPicker activity for requesting a location.
	 */
	public void gotoBoxPicker() {
		Intent intent = new Intent(getApplicationContext(), BoxPicker.class);
		if (Application.getLogic().hasChanges()) {
			DataLossActivity.showDialog(this, intent, REQUEST_BOUNDING_BOX);
		} else {
			startActivityForResult(intent, REQUEST_BOUNDING_BOX);
		}
	}

	/**
	 * @param selectedElement Selected OpenStreetMap element.
	 * @param focusOn if not null focus on the value field of this key.
	 * @param applyLastAddressTags add address tags to the object being edited.
	 * @param showPresets Boolean flag indication to show or hide presets.
	 */
	public void performTagEdit(final OsmElement selectedElement, String focusOn, boolean applyLastAddressTags, boolean showPresets) {
		final Logic logic = Application.getLogic();
		if (selectedElement instanceof Node) {
			logic.setSelectedNode((Node) selectedElement);
		} else if (selectedElement instanceof Way) {
			logic.setSelectedWay((Way) selectedElement);
		}
	
		if (selectedElement != null) {
			StorageDelegator storageDelegator = Application.getDelegator();
			if (storageDelegator.getOsmElement(selectedElement.getName(), selectedElement.getOsmId()) != null) {
				PropertyEditorData[] single = new PropertyEditorData[1];
				single[0] = new PropertyEditorData(selectedElement, focusOn);
				PropertyEditor.startForResult(this, single, applyLastAddressTags,
						showPresets, REQUEST_EDIT_TAG);
			}
		}
	}
	
	public void performTagEdit(final ArrayList<OsmElement> selection, boolean applyLastAddressTags, boolean showPresets) {
		
		ArrayList<PropertyEditorData> multiple = new ArrayList<PropertyEditorData>();
		StorageDelegator storageDelegator = Application.getDelegator();
		for (OsmElement e:selection) {
			if (storageDelegator.getOsmElement(e.getName(), e.getOsmId()) != null) {
				multiple.add(new PropertyEditorData(e, null));
			}
		}
		if (multiple.isEmpty()) {
			Log.d(DEBUG_TAG, "performTagEdit no valid elements");
			return;
		}
		PropertyEditorData[] multipleArray = multiple.toArray(new PropertyEditorData[multiple.size()]);
		PropertyEditor.startForResult(this, multipleArray, applyLastAddressTags,
				showPresets, REQUEST_EDIT_TAG);
	}

	/**
	 * Edit an OpenStreetBug.
	 * @param bug The bug to edit.
	 */
	public void performBugEdit(final Task bug) {
		Log.d(DEBUG_TAG, "editing bug:"+bug);
		Application.getLogic().setSelectedBug(bug);
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prev = fm.findFragmentByTag("fragment_bug");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.commit();

		TaskFragment bugDialog = TaskFragment.newInstance(bug);
		bugDialog.show(fm, "fragment_bug");
	}
	
	/**
	 * potentially do some special stuff for invoking undo and exiting
	 */
	@Override
	public void onBackPressed() {
		// super.onBackPressed();
		Log.d(DEBUG_TAG,"onBackPressed()");
		if (oAuthWebView != null && oAuthWebView.canGoBack()) { 
			// we are displaying the oAuthWebView and somebody might want to navigate back
			oAuthWebView.goBack();
			return;
		}
		if (prefs.useBackForUndo()) {
			String name = Application.getLogic().undo();
			if (name != null)
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.undo) + ": " + name, Toast.LENGTH_SHORT).show();
			else
				exitOnBackPressed();
		} else 
			exitOnBackPressed();
	}
		
	/**
	 * pop up a dialog asking for confirmation and exit
	 */
	void exitOnBackPressed() {
	    new AlertDialog.Builder(this)
        .setTitle(R.string.exit_title)
        .setMessage(R.string.exit_text)
        .setNegativeButton(R.string.no, null)
        .setPositiveButton(R.string.yes, 
        	new DialogInterface.OnClickListener() {
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
		
	
	/**
	 * catch back button in action modes where onBackPressed is not invoked
	 * this is probably not guaranteed to work and will not in android 3.something
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	    if(easyEditManager.isProcessingAction()) {
	        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
	        	if (easyEditManager.handleBackPressed())
	        		return true;
	        }
	    }
	    return super.dispatchKeyEvent(event);
	}
	
	public class UndoListener implements OnClickListener, OnLongClickListener {

		private final String DEBUG_TAG = UndoListener.class.getName();
		
		@Override
		public void onClick(View arg0) {
			Log.d(DEBUG_TAG,"normal click");
			final Logic logic = Application.getLogic();
			String name = logic.undo();
			if (name != null) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.undo) + ": " + name, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.undo_nothing), Toast.LENGTH_SHORT).show();
			}
			// check that we haven't just removed a selected element
			if (logic.resyncSelected()) {
				// only need to test if anything at all is still selected
				if (logic.selectedNodesCount() + logic.selectedWaysCount() + logic.selectedRelationsCount() == 0 ) {
					easyEditManager.finish();
				}
			}
			map.invalidate();
		}

		@Override
		public boolean onLongClick(View v) {
			Log.d(DEBUG_TAG,"long click");
			final Logic logic = Application.getLogic();
			UndoStorage undo = logic.getUndo();
			if (undo.canUndo() || undo.canRedo()) {
				UndoDialogFactory.showUndoDialog(Main.this, logic, undo);
			} else {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.undo_nothing), Toast.LENGTH_SHORT).show();
			}
			return true;
		}
	}

	/**
	 * A TouchListener for all gestures made on the touchscreen.
	 * 
	 * @author mb
	 */
	private class MapTouchListener implements OnTouchListener, VersionedGestureDetector.OnGestureListener, OnCreateContextMenuListener, OnMenuItemClickListener {

		private List<OsmElement> clickedNodesAndWays;
		private List<Task> clickedBugs;
		private List<Photo> clickedPhotos;

		private boolean doubleTap = false;
		
		@Override
		public boolean onTouch(final View v, final MotionEvent m) {
			// Log.d("MapTouchListener", "onTouch");
			if (m.getAction() == MotionEvent.ACTION_DOWN) {
				// Log.d("MapTouchListener", "onTouch ACTION_DOWN");
				clickedBugs = null;
				clickedPhotos = null;
				clickedNodesAndWays = null;
				Application.getLogic().handleTouchEventDown(m.getX(), m.getY());
			}
			mDetector.onTouchEvent(v, m);
			return v.onTouchEvent(m);
		}
		
		@Override
		public void onDown(View v, float x, float y) {}
		
		@Override
		public void onClick(View v, float x, float y) {
			de.blau.android.tasks.MapOverlay osbo = map.getOpenStreetBugsOverlay();
			clickedBugs = (osbo != null) ? osbo.getClickedTasks(x, y, map.getViewBox()) : null;
			
			de.blau.android.photos.MapOverlay photos = map.getPhotosOverlay();
			clickedPhotos = (photos != null) ? photos.getClickedPhotos(x, y, map.getViewBox()) : null;
			
			final Logic logic = Application.getLogic();
			Mode mode = logic.getMode();
			boolean isInEditZoomRange = logic.isInEditZoomRange();
			
			if (showGPS && !followGPS && map.getLocation() != null) {
				// check if this was a click on the GPS mark use the same calculations we use all over the place ... really belongs in a separate method 
				final float tolerance = DataStyle.getCurrent().nodeToleranceValue;				
				float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), map.getViewBox(), (int)(map.getLocation().getLongitude() * 1E7)) - x);
				float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), map.getViewBox(), (int)(map.getLocation().getLatitude() * 1E7)) - y);
				if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
					if (Math.hypot(differenceX, differenceY) <= tolerance) {
						setFollowGPS(true);
						map.invalidate();
						return;
					}
				}
			}
			
			if (isInEditZoomRange) {
				switch (mode) {
				case MODE_MOVE:
					if (NetworkStatus.isConnected(Application.mainActivity) && prefs.voiceCommandsEnabled()) {
						locationForIntent = lastLocation; // location when we touched the screen
						startVoiceRecognition();
					} else {
						Toast.makeText(getApplicationContext(), R.string.toast_unlock_to_edit, Toast.LENGTH_SHORT).show();
					}
					break;
				case MODE_TAG_EDIT:
				case MODE_EASYEDIT:
					performEdit(mode, v, x, y);
					break;
				default:
					break;
				}
				map.invalidate();
			} else {
				switch (((clickedBugs == null) ? 0 : clickedBugs.size()) + ((clickedPhotos == null) ? 0 : clickedPhotos.size())) {
				case 0:
					if (!isInEditZoomRange) {
						int res;
						switch (mode) {
						case MODE_TAG_EDIT:
						case MODE_EASYEDIT:
							res = R.string.toast_not_in_edit_range;
							break;
						case MODE_MOVE:
						default:
							res = 0;
							break;
						}
						if (res != 0) {
							Toast.makeText(getApplicationContext(), res, Toast.LENGTH_LONG).show();
						}
					}
					break;
				case 1:
					if ((clickedBugs != null) && (clickedBugs.size() > 0))
						performBugEdit(clickedBugs.get(0));
					else if ((clickedPhotos != null) && (clickedPhotos.size() > 0))
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) 
					myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				else
					myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				myIntent.setDataAndType(photo.getRef(), "image/jpeg"); // black magic only works this way
				startActivity(myIntent);
				map.getPhotosOverlay().setSelected(photo);
				//TODO may need a map.invalidate() here
			} catch (Exception ex) {
				Log.d(DEBUG_TAG, "viewPhoto exception starting intent: " + ex);	
			}
		}

		@Override
		public void onUp(View v, float x, float y) {
			if (Application.getLogic().getMode() == Mode.MODE_EASYEDIT) {
				easyEditManager.invalidate();
			}
		}
		
		@Override
		public boolean onLongClick(final View v, final float x, final float y) {
			final Logic logic = Application.getLogic();
			if (logic.getMode() != Mode.MODE_EASYEDIT) {
				if (logic.getMode() == Mode.MODE_MOVE) {
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
						if (photoCount==1) {
							viewPhoto(clickedPhotos.get(0));
						} else if (bugCount==1) {
							performBugEdit(clickedBugs.get(0));
						} else if (elementCount==1) {
							ElementInfo.showDialog(Main.this,clickedNodesAndWays.get(0));
						}
					} else if (itemCount > 0) {
						v.showContextMenu();
					}
					return true; 
				}
				return false; // ignore long clicks
			}
			
			if (logic.isInEditZoomRange()) {
				setFollowGPS(false); // editing with the screen moving under you is a pain
				return easyEditManager.handleLongClick(v, x, y);
			} else {
				Toast.makeText(v.getContext(), R.string.toast_not_in_edit_range, Toast.LENGTH_LONG).show();
			}
			
			return true; // long click handled
		}

		@Override
		public void onDrag(View v, float x, float y, float dx, float dy) {
			// Log.d("MapTouchListener", "onDrag dx " + dx + " dy " + dy );
			Application.getLogic().handleTouchEventMove(x, y, -dx, dy);
			setFollowGPS(false);
		}
		
		@Override
		public void onScale(View v, float scaleFactor, float prevSpan, float curSpan) {
			Application.getLogic().zoom((curSpan - prevSpan) / prevSpan);
			updateZoomControls();
		}
		
		/**
		 * Perform edit touch processing.
		 * @param mode mode we are in, either EASYEDIT or TAG_EDIT
		 * @param v View affected by the touch event.
		 * @param x the click-position on the display.
		 * @param y the click-position on the display.
		 */
		public void performEdit(Mode mode, final View v, final float x, final float y) {
			if (!easyEditManager.actionModeHandledClick(x, y)) {
				clickedNodesAndWays = Application.getLogic().getClickedNodesAndWays(x, y);
				switch (((clickedBugs == null) ? 0 : clickedBugs.size()) + clickedNodesAndWays.size() + ((clickedPhotos == null)? 0 : clickedPhotos.size())) {
				case 0:
					// no elements were touched
					if (mode==Mode.MODE_EASYEDIT) {
						easyEditManager.nothingTouched(false);
					}
					break;
				case 1:
					// exactly one element touched
					if (clickedBugs != null && clickedBugs.size() == 1) {
						performBugEdit(clickedBugs.get(0));
					}
					else if (clickedPhotos != null && clickedPhotos.size() == 1) {
						viewPhoto(clickedPhotos.get(0));
					} else {
						if (mode==Mode.MODE_EASYEDIT) {
							easyEditManager.editElement(clickedNodesAndWays.get(0));
						} else {
							performTagEdit(clickedNodesAndWays.get(0), null, false, false);
						}
					}
					break;
				default:
					// multiple possible elements touched - show menu
					if (menuRequired()) {
						v.showContextMenu();
					} else {
						// menuRequired tells us it's ok to just take the first one
						if (mode==Mode.MODE_EASYEDIT) {
							easyEditManager.editElement(clickedNodesAndWays.get(0));
						} else {
							performTagEdit(clickedNodesAndWays.get(0), null, false, false);
						}
					}
					break;
				}
			}
		}
		
		@Override
		public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			onCreateDefaultContextMenu(menu);
		}
			
		public void onCreateDefaultContextMenu(final ContextMenu menu) {
			int id = 0;
			if (clickedPhotos != null) {
				for (Photo p : clickedPhotos) {
					menu.add(Menu.NONE, id++, Menu.NONE, p.getRef().getLastPathSegment()).setOnMenuItemClickListener(this);
				}
			}
			if (clickedBugs != null) {
				for (Task b : clickedBugs) {
					menu.add(Menu.NONE, id++, Menu.NONE, b.getDescription()).setOnMenuItemClickListener(this);
				}
			}
			if (clickedNodesAndWays != null) {
				for (OsmElement e : clickedNodesAndWays) {
					menu.add(Menu.NONE, id++, Menu.NONE, e.getDescription(Application.mainActivity)).setOnMenuItemClickListener(this);
				}
			}
		}

		/**
		 * Checks if a menu should be shown based on clickedNodesAndWays and clickedBugs.
		 * ClickedNodesAndWays needs to contain nodes first, then ways, ordered by distance from the click.
		 * Assumes multiple elements have been clicked, i.e. a choice is necessary unless heuristics work.
		 */
		private boolean menuRequired() {
			// If the context menu setting requires the menu, show it instead of guessing.
			if (prefs.getForceContextMenu()) return true;
			
			// If bugs are clicked, user should always choose
			if (clickedBugs != null && clickedBugs.size() > 0) return true;
			
			// If photos are clicked, user should always choose
			if (clickedPhotos != null && clickedPhotos.size() > 0) return true;
			
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
				final Logic logic = Application.getLogic();
				float nodeX = logic.getNodeScreenX(candidate);
				float nodeY = logic.getNodeScreenY(candidate);
				for (int i = 1; i < clickedNodesAndWays.size(); i++) {
					if (!(clickedNodesAndWays.get(i) instanceof Node)) break;
					Node possibleNeighbor = (Node)clickedNodesAndWays.get(i);
					float node2X = logic.getNodeScreenX(possibleNeighbor);
					float node2Y = logic.getNodeScreenY(possibleNeighbor);
					// Fast "square" checking is good enough
					if (Math.abs(nodeX-node2X) < DataStyle.NODE_OVERLAP_TOLERANCE_VALUE ||
						Math.abs(nodeY-node2Y) < DataStyle.NODE_OVERLAP_TOLERANCE_VALUE ) {
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
				itemId -= (((clickedBugs == null) ? 0 : clickedBugs.size() ) + ((clickedPhotos == null) ? 0 : clickedPhotos.size()));
				
				if ((itemId >= 0) && (clickedNodesAndWays != null) && (itemId < clickedNodesAndWays.size())) {
					final OsmElement element = clickedNodesAndWays.get(itemId);
					switch (Application.getLogic().getMode()) {
					case MODE_MOVE:
						ElementInfo.showDialog(Main.this,element);
						break;
					case MODE_TAG_EDIT:
						performTagEdit(element, null, false, false);
						break;
					case MODE_EASYEDIT:
						if (doubleTap) {
							doubleTap = false;
							easyEditManager.startExtendedSelection(element);
						} else {
							easyEditManager.editElement(element);
						}
						break;
					default:
						break;
					}
				}
			}
			return true;
		}
		
		/**
		 * Show toasts displaying the info on nearby objects
		 */
		void displayInfo(final float x, final float y) {
			clickedNodesAndWays = Application.getLogic().getClickedNodesAndWays(x, y);
			// clickedPhotos and
			if (clickedPhotos != null) {
				for (Photo p : clickedPhotos) {
					Toast.makeText(getApplicationContext(), p.getRef().getLastPathSegment(), Toast.LENGTH_SHORT).show();
				}
			}
			if (clickedBugs != null) {
				for (Task b : clickedBugs) {
					Toast.makeText(getApplicationContext(), b.getDescription(), Toast.LENGTH_SHORT).show();
				}
			}
			if (clickedNodesAndWays != null) {
				for (OsmElement e : clickedNodesAndWays) {
					String toast = e.getDescription();
					if (e.hasProblem(getApplicationContext())) {
						String problem = e.describeProblem();
						toast = !problem.equals("") ? toast + "\n" + problem : toast;
					}
					Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
				}
			}	
		}

		@Override
		public boolean onDoubleTap(View v, float x, float y) {
			final Logic logic = Application.getLogic();
			clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
			switch (clickedNodesAndWays.size()) {
			case 0:
				// no elements were touched
				if (logic.getMode() == Mode.MODE_EASYEDIT) {
					easyEditManager.nothingTouched(true); // short cut to finishing multi-select
				}
				break;
			case 1:
				if (logic.getMode() == Mode.MODE_EASYEDIT) {
					easyEditManager.startExtendedSelection(clickedNodesAndWays.get(0));
				}
				break;
			default:
				// multiple possible elements touched - show menu
				if (logic.getMode() == Mode.MODE_EASYEDIT) {
					if (menuRequired()) {
						Log.d(DEBUG_TAG,"onDoubleTap displaying menu");
						doubleTap  = true; // ugly flag
						v.showContextMenu();
					} else  {
						// menuRequired tells us it's ok to just take the first one
						easyEditManager.startExtendedSelection(clickedNodesAndWays.get(0));
					}
				}
				break;
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
			final Logic logic = Application.getLogic();
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
							if (c == Util.getShortCut(getApplicationContext() , R.string.shortcut_zoom_in)) {
								logic.zoom(Logic.ZOOM_IN);
								updateZoomControls();
								return true;
							} else if (c == Util.getShortCut(getApplicationContext(), R.string.shortcut_zoom_out)) {
								logic.zoom(Logic.ZOOM_OUT);
								updateZoomControls();
								return true;
							}
							if (easyEditManager.isProcessingAction() && event.isCtrlPressed()) { // shortcuts not supported in action modes arghhh
								char shortcut = Character.toLowerCase((char) event.getUnicodeChar(0)); // get rid of Ctrl key
								if (easyEditManager.processShortcut(shortcut)) {
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
			Application.getLogic().translate(direction);
		}
	}
	
	/**
	 * Mouse scroll wheel support
	 * @author simon
	 *
	 */
	@SuppressLint("NewApi")
	public class MotionEventListener implements OnGenericMotionListener
	{
		@SuppressLint("NewApi")
		@Override
		public boolean onGenericMotion(View arg0,MotionEvent event) {
			final Logic logic = Application.getLogic();
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
	 * @return a list of all pending changes to upload (contains newlines)
	 */
	public String getPendingChanges() {
		List<String> changes = Application.getLogic().getPendingChanges(this);
		StringBuilder retval = new StringBuilder();
		for (String change : changes) {
			retval.append(change).append('\n');
		}
		return retval.toString();
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
		final Logic logic = Application.getLogic();
		//noinspection SimplifiableIfStatement
		if (logic == null) return false;
		return logic.hasChanges();
	}
	
	/**
	 * Sets the activity to re-download the last downloaded area on startup
	 * (use e.g. when the API URL is changed)
	 */
	public static void prepareRedownload() {
		redownloadOnResume = true;
	}
	
	public String getBaseURL() {
		return prefs.getServer().getBaseURL();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.i(DEBUG_TAG, "Tracker service connected");
		setTracker((((TrackerBinder)service).getService()));
		map.setTracker(getTracker());
		getTracker().setListener(this);
		getTracker().setListenerNeedsGPS(wantLocationUpdates);
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
			viewBox.moveTo((int) (location.getLongitude() * 1E7d), (int) (location.getLatitude() * 1E7d));
		}
		lastLocation  = location;
		if (showGPS) {		
			map.setLocation(location);
		}
		map.invalidate();
	}	
	
	@SuppressWarnings("EmptyMethod")
	@Override
	/**
	 * DO NOT CALL DIRECTLY in custom code.
	 * Use {@link #triggerMenuInvalidation()} to make it easier to debug and implement workarounds for android bugs.
	 * Must be called from the main thread.
	 */
	public void invalidateOptionsMenu() { 
		// Log.d(DEBUG_TAG, "invalidateOptionsMenu called");
		super.supportInvalidateOptionsMenu();
	}
	
	/**
	 * Simply calls {@link #invalidateOptionsMenu()}.
	 * Used to make it easier to implement workarounds.
	 * MUST BE CALLED FROM THE MAIN/UI THREAD!
	 */
	public void triggerMenuInvalidation() {
		Log.d(DEBUG_TAG, "triggerMenuInvalidation called");
		super.supportInvalidateOptionsMenu(); // TODO delay or make conditional to work around android bug?
	}
	
	/**
	 * Invalidates the options menu of the main activity if such an activity exists.
	 * MUST BE CALLED FROM THE MAIN/UI THREAD!
	 */
	public static void triggerMenuInvalidationStatic() {
		if (Application.mainActivity == null) return;
		// DO NOT IGNORE "wrong thread" EXCEPTIONS FROM THIS.
		// It *will* mess up your menu in many creative ways.
		Application.mainActivity.triggerMenuInvalidation();
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
	public void setTracker(TrackerService tracker) {
		this.tracker = tracker;
	}
	
	public void zoomToAndEdit(int lonE7, int latE7, OsmElement e) {
		Log.d(DEBUG_TAG,"zoomToAndEdit Zoom " + map.getZoomLevel());
		final Logic logic = Application.getLogic();
		if (logic.getMode()==Mode.MODE_MOVE) { // avoid switiching to the wronf mode
			ToggleButton lock = setLock(Mode.MODE_MOVE); // NOP to get button
			if (EASY_TAG.equals(lock.getTag())) {
				setLock(Mode.MODE_EASYEDIT);
			} else {
				setLock(Mode.MODE_TAG_EDIT);
			}
		}
		setFollowGPS(false); // otherwise the screen could move around
		if (e instanceof Node && map.getZoomLevel() < 22) {
			Log.d(DEBUG_TAG,"zoomToAndEdit setting Zoom to 22");
			logic.setZoom(22); // FIXME this doesn't seem to work as expected
		} else {
			map.getViewBox().setBorders(e.getBounds());
		}
		map.getViewBox().moveTo(lonE7, latE7);
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
		if (logic.getMode()==Mode.MODE_EASYEDIT) {
			easyEditManager.editElement(e);
			map.invalidate();
		} else { // tag edit mode
			performTagEdit(e, null, false, false);
		}
	}
	
	@Override
	/**
	 * Workaround for bug mentioned below
	 */
	public ActionMode startSupportActionMode(final ActionMode.Callback callback) {
	  // Fix for bug https://code.google.com/p/android/issues/detail?id=159527
	  final ActionMode mode = super.startSupportActionMode(callback);
	  if (mode != null) {
	    mode.invalidate();
	  }
	  return mode;
	}

	@Override
	public void update() {
		map.invalidate();
	}	
}
