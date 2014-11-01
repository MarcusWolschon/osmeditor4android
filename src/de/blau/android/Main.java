package de.blau.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.ResourcesCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import de.blau.android.GeoUrlActivity.GeoUrlData;
import de.blau.android.Logic.CursorPaddirection;
import de.blau.android.Logic.Mode;
import de.blau.android.RemoteControlUrlActivity.RemoteControlUrlData;
import de.blau.android.TagEditor.TagEditorData;
import de.blau.android.actionbar.ModeDropdownAdapter;
import de.blau.android.actionbar.UndoDialogFactory;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.imageryoffset.BackgroundAlignmentActionModeCallback;
import de.blau.android.osb.Bug;
import de.blau.android.osb.CommitTask;
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
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.resources.Profile;
import de.blau.android.services.TrackerService;
import de.blau.android.services.TrackerService.TrackerBinder;
import de.blau.android.services.TrackerService.TrackerLocationListener;
import de.blau.android.util.GeoMath;
import de.blau.android.util.OAuthHelper;
import de.blau.android.util.SavingHelper;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;
import de.blau.android.views.util.OpenStreetMapTileServer;

/**
 * This is the main Activity from where other Activities will be started.
 * 
 * @author mb
 */
public class Main extends SherlockFragmentActivity implements OnNavigationListener, ServiceConnection, TrackerLocationListener {

	/**
	 * Tag used for Android-logging.
	 */
	private static final String DEBUG_TAG = Main.class.getName();

	/**
	 * Requests a {@link BoundingBox} as an activity-result.
	 */
	public static final int REQUEST_BOUNDINGBOX = 0;

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
	 * Where we install the current version of vespucci
	 */
	private static final String VERSION_FILE = "version.dat"; 

	private DialogFactory dialogFactory;
	
	/** Objects to handle showing device orientation. */
	private SensorManager sensorManager;
	private Sensor sensor;
	private SensorEventListener sensorListener = new SensorEventListener() {
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
	 * Adapter providing items for the mode selection dropdown in the ActionBar
	 */
	private ModeDropdownAdapter modeDropdown;

	/**
	 * The manager for the EasyEdit mode
	 */
	public EasyEditManager easyEditManager;

	/**
	 * The logic that manipulates the model. (non-UI)<br/>
	 * This is created in {@link #onCreate(Bundle)} and never changed afterwards.<br/>
	 * If may be null or not reflect the current state if accessed from outside this activity.
	 */
	protected static Logic logic;
	
	/**
	 * The currently selected preset
	 */
	private static Preset[] currentPresets;

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

	/** Initialized in onCreate - this empty file indicates by its existence that showGPS should be enabled on start */
	private File showGPSFlagFile = null;
	private boolean showGPS;
	private boolean followGPS;
	private boolean autoDownload;
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

	private Location previousLocation = null;

	private Location lastLocation = null;

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
		Log.i("Main", "onCreate " + (savedInstanceState != null?" no saved state " : " saved state exists"));
		// minimal support for geo: uris and JOSM styl remote control
		geoData = (GeoUrlData)getIntent().getSerializableExtra(GeoUrlActivity.GEODATA);
		rcData = (RemoteControlUrlData)getIntent().getSerializableExtra(RemoteControlUrlActivity.RCDATA);
		
		setTheme(R.style.Theme_customMain);
		
		super.onCreate(savedInstanceState);
		Application.mainActivity = this;
		
		showGPSFlagFile = new File(getFilesDir(), "showgps.flag");
		showGPS = showGPSFlagFile.exists();
		
		sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager != null) {
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			if (sensor == null) {
				sensorManager = null;
			}
		}
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		prefs = new Preferences(this);
		if (prefs.splitActionBarEnabled()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW); // this might need to be set with bit ops
			}
			// besides hacking ABS, there is no equivalent method to enable this for ABS
		} else {
			requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		}
		
		
		rl = new RelativeLayout(getApplicationContext());
		
		if (map != null) {
			map.onDestroy();
		}
		map = new Map(getApplicationContext());
		map.setId(1);
		dialogFactory = new DialogFactory(this);
		
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
				logic.zoom(Logic.ZOOM_IN);
				updateZoomControls();
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				logic.zoom(Logic.ZOOM_OUT);
				updateZoomControls();
			}
		});

		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.addView(zoomControls, rlp);
		
		setContentView(rl);
		
		// check if first time user and display something if yes
		SavingHelper<String> savingHelperVersion = new SavingHelper<String>();
		String lastVersion = savingHelperVersion.load(VERSION_FILE, false);
		boolean newInstall = (lastVersion == null || lastVersion.equals(""));
		
		loadOnResume = false;
		
		Log.i("Main", "onCreate - creating new logic");
		logic = new Logic(map, new Profile(getApplicationContext()));
		if (isLastActivityAvailable()) {
			// Start loading after resume to ensure loading dialog can be removed afterwards
			loadOnResume = true;
		} else {
			if (geoData == null && rcData == null) {
				// check if we have a position
				Location loc = getLastLocation();
				BoundingBox box = null;
				if (loc != null) {
					try {
						box = GeoMath.createBoundingBoxForCoordinates(loc.getLatitude(),
								loc.getLongitude(), 1000); // a km hardwired for now
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
	
		easyEditManager = new EasyEditManager(this, logic);
		
		// show welcome dialog
		if (newInstall) {
			// newbie, display welcome dialog
			Log.d("Main","showing welcome dialog");
			showDialog(DialogFactory.NEWBIE);
		} else {
			// for now simply current version
			// TODO display change log or similar on major changes
		}
		savingHelperVersion.save(VERSION_FILE, getString(R.string.app_version), false);
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
		Log.d("Main", "onStart");
		super.onStart();

		prefs = new Preferences(this);
		logic.setPrefs(prefs);
		map.setPrefs(prefs);
		map.createOverlays();
		map.requestFocus();
		
		undoListener = new UndoListener();

		showActionBar();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d("Main", "onNewIntent storage dirty " + logic.delegator.isDirty());
		setIntent(intent);
		geoData = (GeoUrlData)getIntent().getSerializableExtra(GeoUrlActivity.GEODATA);
		rcData = (RemoteControlUrlData)getIntent().getSerializableExtra(RemoteControlUrlActivity.RCDATA);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("Main", "onResume");

		bindService(new Intent(this, TrackerService.class), this, BIND_AUTO_CREATE);
		
		if (redownloadOnResume) {
			redownloadOnResume = false;
			logic.downloadLast();
		} else if (loadOnResume) {
			loadOnResume = false;
			logic.loadFromFile(getApplicationContext());
		} else { // loadFromFile already does this
			logic.loadEditingState();
			map.invalidate();
		}
		if (currentPresets == null) {
			currentPresets = prefs.getPreset();
		}
		
		logic.updateProfile();
		map.updateProfile();
		
		runningInstance = this;
		
		updateActionbarEditMode();
		if (!prefs.isOpenStreetBugsEnabled() && logic.getMode() == Mode.MODE_OPENSTREETBUG) {
			logic.setMode(Mode.MODE_MOVE);
		}
		if (modeDropdown != null)
			modeDropdown.setShowOpenStreetBug(prefs.isOpenStreetBugsEnabled());
		
		if (getTracker() != null) getTracker().setListener(this);
		
		setShowGPS(showGPS); // reactive GPS listener if needed
		setFollowGPS(followGPS);
		
		map.setKeepScreenOn(prefs.isKeepScreenOnEnabled());
		
		if (geoData != null) {
			Log.d("Main","got position from geo: url " + geoData.getLat() + "/" + geoData.getLon() + " storage dirty is " + logic.delegator.isDirty());
			if (prefs.getDownloadRadius() != 0) { // download
				BoundingBox bbox;
				try {
					bbox = GeoMath.createBoundingBoxForCoordinates(geoData.getLat(), geoData.getLon(), prefs.getDownloadRadius());
					logic.downloadBox(bbox, true /* logic.delegator.isDirty() */, false); // TODO find out why isDirty doesn't work in this context
				} catch (OsmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				//TODO this currently will only work if loading the data from the saved state has already completed, could be fixed with a lock or similar
				Log.d("Main","moving to position");
				map.getViewBox().moveTo((int)(geoData.getLon()*1E7), (int)(geoData.getLat()*1E7));
				map.invalidate();
			}
			geoData=null; // zap to stop repeated downloads
		} 
		if (rcData != null) {
			Log.d("Main","got bbox from remote control url " + rcData.getBox() + " load " + rcData.load());
			if (rcData.load()) { // download
				logic.downloadBox(rcData.getBox(), true /* logic.delegator.isDirty() */, false); // TODO find out why isDirty doesn't work in this context
			} else {
				//TODO this currently will only work if loading the data from the saved state has already completed, could be fixed with a lock or similar
				Log.d("Main","moving to position");
				map.getViewBox().setBorders(rcData.getBox());
				map.invalidate();
			}
			rcData=null; // zap to stop repeated downloads
		}
	}

	@Override
	protected void onPause() {
		Log.d("Main", "onPause mode " + logic.getMode());
		runningInstance = null;
		disableLocationUpdates();
		if (getTracker() != null) getTracker().setListener(null);

		// always save editing state
		logic.saveEditingState();
		// onPause is the last lifecycle callback guaranteed to be called on pre-honeycomb devices
		// on honeycomb and later, onStop is also guaranteed to be called, so we can defer saving.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) saveData();
		
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		Log.d("Main", "onStop");
		// editing state has been saved in onPause
	
		// On devices with Android versions before Honeycomb, we already save data in onPause
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) saveData();
		
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d("Main", "onDestroy");
		map.onDestroy();
		if (getTracker() != null) getTracker().setListener(null);
		try {
			unbindService(this);
		} catch (Exception e) {} // ignore errors, this is just cleanup
		super.onDestroy();
	}

	/**
	 * Save current data (state, downloaded data, changes, ...) to file(s)
	 */
	private void saveData() {
		Log.i("Main", "saving data");
		logic.save();
		if (showGPS) {
			try {
				showGPSFlagFile.createNewFile();
			} catch (IOException e) {
				Log.e("Main", "failed to create showGPS flag file");
			}
		} else {
			showGPSFlagFile.delete();
		}
		// if something was selected save that
		
	}

	/**
	 * Update the state of the onscreen zoom controls to reflect their ability
	 * to zoom in/out.
	 */
	private void updateZoomControls() {
		zoomControls.setIsZoomInEnabled(logic.canZoom(Logic.ZOOM_IN));
		zoomControls.setIsZoomOutEnabled(logic.canZoom(Logic.ZOOM_OUT));
	}
	
//	@Override
//	public Object onRetainNonConfigurationInstance() {
//		Log.d("Main", "onRetainNonConfigurationInstance");
//		return logic;
//	}

	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (easyEditManager.isProcessingAction()) {
			easyEditManager.invalidate();
		}
	}

	
	/**
	 * Sets up the Action Bar.
	 */
	private void showActionBar() {
		Log.d("Main", "showActionBar");
		ActionBar actionbar = getSupportActionBar();
		actionbar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_bg)));
		actionbar.setSplitBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_bg)));
		actionbar.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_bg))); // this probably isn't ever necessary
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setDisplayShowTitleEnabled(false);

		if (prefs.depreciatedModesEnabled()) {
			actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			modeDropdown = new ModeDropdownAdapter(this, prefs.isOpenStreetBugsEnabled(), prefs.depreciatedModesEnabled());
			actionbar.setListNavigationCallbacks(modeDropdown, this);	
			ToggleButton lock = (ToggleButton) findViewById(R.id.lock);
			if (lock != null) lock.setVisibility(View.GONE);
		} else {
			actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM|ActionBar.DISPLAY_SHOW_HOME);
			
			View lockLayout = View.inflate(getApplicationContext(), R.layout.lock, null);
			actionbar.setCustomView(lockLayout);
			ToggleButton lock = setLock(logic.getMode());
			findViewById(R.id.lock).setVisibility(View.VISIBLE);
			lock.setOnClickListener(new View.OnClickListener() {
			    @Override
				public void onClick(View b) {
			        Log.d("Main", "Lock pressed");
			        if(((ToggleButton)b).isChecked()) {
			        	logic.setMode(Logic.Mode.MODE_EASYEDIT);
			        } else {
			        	logic.setMode(Logic.Mode.MODE_MOVE);
			        }
			        onEditModeChanged();
			    }
			});
		}	
	
		actionbar.show();
		setSupportProgressBarIndeterminateVisibility(false);
	}
	
	/**
	 * Set lock button to locked or unlocked depending on the edit mode
	 * @param mode
	 * @return
	 */
	private ToggleButton setLock(Logic.Mode mode) {
		ToggleButton lock = (ToggleButton) findViewById(R.id.lock);
		switch (mode) {
		case MODE_EASYEDIT:
		case MODE_ALIGN_BACKGROUND:
			lock.setChecked(true);
			break;
		default: 
			mode = Mode.MODE_MOVE;
			lock.setChecked(false);
		}
		logic.setMode(mode); 
		return lock; // for convenience
	}

	public void setMode(Logic.Mode mode) {
		logic.setMode(mode); 
	}
	
	public void updateActionbarEditMode() {
		Log.d("Main", "updateActionbarEditMode");
		if (modeDropdown != null && (prefs!=null && prefs.depreciatedModesEnabled())) 
			getSupportActionBar().setSelectedNavigationItem(modeDropdown.getIndexForMode(logic.getMode()));
		else { 
			setLock(logic.getMode());
		}
	}
	
	public static void onEditModeChanged() {
		Log.d("Main", "onEditModeChanged");
		if (runningInstance != null) runningInstance.updateActionbarEditMode();
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		Log.d("Main", "onNavigationItemSelected");
		Mode mode = modeDropdown.getModeForItem(itemPosition);
		logic.setMode(mode);
		if (mode == Mode.MODE_TAG_EDIT) {
			// if something is already/still selected, edit its tags
			// prefer ways over nodes, and deselect what we're *not* editing
			OsmElement e = logic.getSelectedWay();
			if (e == null) e = logic.getSelectedNode();
			else logic.setSelectedNode(null);
			if (e != null) performTagEdit(e, null, false);
		}
		return true;
	}
	
	
	/**
	 * Creates the menu from the XML file "main_menu.xml".<br> {@inheritDoc}
	 */
 	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		Log.d("Main", "onCreateOptionsMenu");
		final MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		
		// only show camera icon if we have a camera, and a camera app is installed 
		PackageManager pm = getPackageManager();
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && cameraIntent.resolveActivity(getPackageManager()) != null) {
			menu.findItem(R.id.menu_camera).setShowAsAction(prefs.showCameraAction() ? MenuItem.SHOW_AS_ACTION_ALWAYS: MenuItem.SHOW_AS_ACTION_NEVER);
		} else {
			menu.findItem(R.id.menu_camera).setVisible(false);
		}
		
		menu.findItem(R.id.menu_gps_show).setChecked(showGPS);
		menu.findItem(R.id.menu_gps_follow).setChecked(followGPS);
		menu.findItem(R.id.menu_gps_start).setEnabled(getTracker() != null && !getTracker().isTracking());
		menu.findItem(R.id.menu_gps_pause).setEnabled(getTracker() != null && getTracker().isTracking());
		menu.findItem(R.id.menu_gps_import).setEnabled(getTracker() != null);
		menu.findItem(R.id.menu_gps_upload).setEnabled(getTracker() != null && getTracker().getTrackPoints() != null && getTracker().getTrackPoints().size() > 0);
		menu.findItem(R.id.menu_gps_goto_start).setEnabled(getTracker() != null && getTracker().getTrackPoints() != null && getTracker().getTrackPoints().size() > 0);
		menu.findItem(R.id.menu_gps_autodownload).setChecked(autoDownload);

		MenuItem undo = menu.findItem(R.id.menu_undo);
		undo.setVisible(logic.getUndo().canUndo() || logic.getUndo().canRedo());
		View undoView = undo.getActionView();
		undoView.setOnClickListener(undoListener);
		undoView.setOnLongClickListener(undoListener);

		final Server server = prefs.getServer();
		if (server.hasOpenChangeset()) {
			menu.findItem(R.id.menu_transfer_close_changeset).setVisible(true);
		} else {
			menu.findItem(R.id.menu_transfer_close_changeset).setVisible(false);
		}
		return true;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d("Main", "onOptionsItemSelected");
		final Server server = prefs.getServer();
		switch (item.getItemId()) {
		case R.id.menu_confing:
			startActivity(new Intent(getApplicationContext(), PrefEditor.class));
			return true;
			
		case R.id.menu_find:
			showDialog(DialogFactory.SEARCH);
			return true;
			
		case R.id.menu_help:
			Intent startHelpViewer = new Intent(getApplicationContext(), HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, "Main");
			startActivity(startHelpViewer);
			return true;
			
		case R.id.menu_camera:
			Intent startCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			try {
				startCamera.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(getImageFile()));
				startActivityForResult(startCamera, REQUEST_IMAGE_CAPTURE);	
			} catch (Exception ex) {
				try {
					Toast.makeText(this,getResources().getString(R.string.toast_camera_error, ex.getMessage()), Toast.LENGTH_LONG).show();
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
			setFollowGPS(true);
			map.setFollowGPS(true);
			logic.setZoom(19);
			map.invalidate();
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
					oAuthHandshake(server);
					if (server.getOAuth()) { // if still set
						Toast.makeText(getApplicationContext(), R.string.toast_oauth_retry, Toast.LENGTH_LONG).show();
						return true;
					} 
				}	
				showDialog(DialogFactory.GPX_UPLOAD);
				// performTrackUpload("Test","Test",Visibility.PUBLIC);
			} else {
				showDialog(DialogFactory.NO_LOGIN_DATA);
			}
			return true;

		case R.id.menu_gps_export:
			if (getTracker() != null) {
				SavingHelper.asyncExport(this, getTracker());
			}
			return true;
			
		case R.id.menu_gps_import:
			if (logic == null || logic.delegator == null) return true;
			showFileChooser(READ_GPX_FILE_SELECT_CODE);
			return true;
			
		case R.id.menu_gps_goto_start:
			List<TrackPoint> l = tracker.getTrackPoints();
			if (l != null && l.size() > 0) {
				Log.d("Main","Going to start of track");
				setFollowGPS(false);
				map.setFollowGPS(false);
				map.getViewBox().moveTo(l.get(0).getLon(), l.get(0).getLat());
				logic.setZoom(19);
				map.invalidate();
			}
			return true;

		case R.id.menu_gps_autodownload:
			setAutoDownload(!autoDownload);
			Log.d("Main","Setting autoDownload to " + autoDownload);
			triggerMenuInvalidation();
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
		
		case R.id.menu_transfer_export:
			if (logic == null || logic.delegator == null) return true;
			SavingHelper.asyncExport(this, logic.delegator);
			return true;
			
		case R.id.menu_transfer_read_file:
			if (logic == null || logic.delegator == null) return true;
			showFileChooser(READ_OSM_FILE_SELECT_CODE);
			return true;
			
		case R.id.menu_transfer_save_file:
			if (logic == null || logic.delegator == null) return true;
			showDialog(DialogFactory.SAVE_FILE);
//			showFileChooser(WRITE_OSM_FILE_SELECT_CODE);
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
			startActionMode(getBackgroundAlignmentActionModeCallback());
			return true;
			
		case R.id.menu_tools_background_properties:
			showDialog(DialogFactory.BACKGROUND_PROPERTIES);
			return true;
			
		}	
		return false;
	}

	@SuppressLint("NewApi")
	private File getImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = timeStamp;
	    File outdir = null;
//	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
//	    	outdir = Environment.getExternalStoragePublicDirectory(
//	                Environment.DIRECTORY_PICTURES);
//	    	outdir.mkdir();
//	    } else { // oS version 8
	    	File sdcard = Environment.getExternalStorageDirectory();
	    	outdir = new File(sdcard, "Vespucci");
	    	outdir.mkdir(); // ensure directory exists;
	    	outdir = new File(outdir,"Pictures");
	    	outdir.mkdir();
//	    }
	    
	    File imageFile = File.createTempFile(imageFileName,".jpg",outdir);
	    Log.d("Main","createImageFile " + imageFile.getAbsolutePath());
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
	        Toast.makeText(this, R.string.toast_missing_filemanager, 
	                Toast.LENGTH_SHORT).show();
	    }
	}
	
	protected void setAutoDownload(boolean b) {
		Log.d("Main", "autoDownload: "+ b);
		autoDownload = b;
	}
	
	protected void setShowGPS(boolean show) {
		if (show && !ensureGPSProviderEnabled()) {
			show = false;
		}
		showGPS = show;
		Log.d("Main", "showGPS: "+ show);
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
	
	/**
	 * Checks if GPS is enabled in the settings.
	 * If not, returns false and shows location settings.
	 * @return true if GPS is enabled, false if not
	 */
	private boolean ensureGPSProviderEnabled() {
		LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		try {
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				return true;
			} else {
				startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				return false;
			}
		} catch (Exception e) {
			Log.e("Main", "Error when checking for GPS, assuming GPS not available", e);
			Toast.makeText(this, R.string.gps_failure, Toast.LENGTH_SHORT).show();
			return false;
		}
	}

	public void setFollowGPS(boolean follow) {
		// Log.d("Main","setFollowGPS");
		if (followGPS != follow) {
			followGPS = follow;
			if (follow) {
				setShowGPS(true);
				if (lastLocation != null) onLocationChanged(lastLocation);
			}
			map.setFollowGPS(follow);
			triggerMenuInvalidation();
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
		if (wantLocationUpdates == true) return;
		if (sensorManager != null) {
			sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_UI);
		}
		wantLocationUpdates = true;
		if (getTracker() != null) getTracker().setListenerNeedsGPS(true);
	}
	
	private void disableLocationUpdates() {
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
	 * @param add 
	 */
	private void onMenuDownloadCurrent(boolean add) {
		Log.d("Main", "onMenuDownloadCurrent");
		if (logic.hasChanges() && !add) {
			showDialog(DialogFactory.DOWNLOAD_CURRENT_WITH_CHANGES);
		} else {
			performCurrentViewHttpLoad(add);
		}
	}

	/**
	 * Uses {@link DialogFactory} to create Dialogs<br> {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		Log.d("Main", "onCreateDialog");
		Dialog dialog = dialogFactory.create(id);
		if (dialog != null) {
			return dialog;
		}
		return super.onCreateDialog(id);
	}
	
	/**
	 * Prepare the fields of dialogs before they are shown. Only some need this special
	 * handling.
	 * @param id Dialog ID number.
	 * @param dialog Dialog object.
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		Log.d("Main", "onPrepareDialog");
		super.onPrepareDialog(id, dialog);
		if (dialog instanceof AlertDialog) {
			AlertDialog ad = (AlertDialog)dialog;
			switch (id) {
			case DialogFactory.CONFIRM_UPLOAD:
				TextView changes = (TextView)ad.findViewById(R.id.upload_changes);
				changes.setText(getString(R.string.confirm_upload_text, getPendingChanges()));
				break;
			case DialogFactory.SEARCH:
				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				break;
			case DialogFactory.OPENSTREETBUG_EDIT:
				Bug bug = logic.getSelectedBug();
				if (bug==null) { // a quick hack to see if this stops this crashing now and then
					Log.e("Main","onPrepareDialog bug is null");
					// try to local editstate here ...
					logic.loadEditingState();
					if (bug==null) {
						Log.e("Main","onPrepareDialog bug is null - invalid state");
						Toast.makeText(getApplicationContext(), "Invalid editing state, please dismiss the dialog and report the problem!", Toast.LENGTH_LONG).show(); //TODO externalize the text
						return;
					}
				}
				ad.setTitle(getString((bug.getId() == 0) ? R.string.openstreetbug_new_title : R.string.openstreetbug_edit_title));
				TextView comments = (TextView)ad.findViewById(R.id.openstreetbug_comments);
				comments.setText(Html.fromHtml(bug.getComment())); // ugly
				EditText comment = (EditText)ad.findViewById(R.id.openstreetbug_comment);
				comment.setText("");
				comment.setFocusable( true);
				comment.setFocusableInTouchMode(true);
				comment.setEnabled(true);
				CheckBox close = (CheckBox)ad.findViewById(R.id.openstreetbug_close);
				close.setChecked(bug.isClosed());
				if (bug.isClosed()) {
					close.setText(R.string.openstreetbug_edit_closed);
				} else {
					close.setText(R.string.openstreetbug_edit_close);
				}
				close.setEnabled(/* !bug.isClosed() && */ bug.getId() != 0);
				Button commit = ad.getButton(DialogInterface.BUTTON_POSITIVE);
				commit.setEnabled(/* !bug.isClosed() */ true);
				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		Log.d("Main", "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_BOUNDINGBOX && data != null) {
			handleBoxPickerResult(resultCode, data);
		} else if (requestCode == REQUEST_EDIT_TAG && resultCode == RESULT_OK && data != null) {
			handleTagEditorResult(data);
		} else if (requestCode == READ_OSM_FILE_SELECT_CODE && resultCode == RESULT_OK) {
			// Get the Uri of the selected file 
	        Uri uri = data.getData();
	        Log.d("Main", "Read file Uri: " + uri.toString());
	        try {
				logic.readOsmFile(uri, false);
			} catch (FileNotFoundException e) {
				try {
					Toast.makeText(this,getResources().getString(R.string.toast_file_not_found, uri.toString()), Toast.LENGTH_LONG).show();
				} catch (Exception ex) {
					// protect against translation errors
				}
			}
	        map.invalidate();
		} else if (requestCode == WRITE_OSM_FILE_SELECT_CODE && resultCode == RESULT_OK) {
			// Get the Uri of the selected file 
	        Uri uri = data.getData();
	        Log.d("Main", "Write file Uri: " + uri.toString());
	        logic.writeOsmFile(uri.getPath());
	        map.invalidate();
		} else if (requestCode == READ_GPX_FILE_SELECT_CODE && resultCode == RESULT_OK) {
			// Get the Uri of the selected file 
	        Uri uri = data.getData();
	        Log.d("Main", "Read gpx file Uri: " + uri.toString());
	        if (getTracker() != null) {
	        	if (getTracker().getTrackPoints().size() > 0 ) {
	        		DialogFactory.createExistingTrackDialog(this, uri).show();
	        	} else {
	        		getTracker().stopTracking(false);
	        		try {
						getTracker().importGPXFile(uri);
					} catch (FileNotFoundException e) {
						try {
							Toast.makeText(this,getResources().getString(R.string.toast_file_not_found, uri.toString()), Toast.LENGTH_LONG).show();
						} catch (Exception ex) {
							// protect against translation errors
						}
					}
	        	}
	        }
	        map.invalidate();
		} else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && prefs.isPhotoLayerEnabled()) {
			reindexPhotos();
		}
	}

	/**
	 * @param resultCode
	 * @param data
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
				Log.d("Main","opening empty map on " + (box != null ? box.toString() : " null bbox"));
				openEmptyMap(box); // we may have a valid box
			}
		} catch (OsmException e) {
			//Values should be done checked in LocationPciker.
			Log.e(DEBUG_TAG, "OsmException", e);
		}
	}

	/**
	 * @param data
	 */
	private void handleTagEditorResult(final Intent data) {
		Bundle b = data.getExtras();
		if (b != null && b.containsKey(TagEditor.TAGEDIT_DATA)) {
			// Read data from extras
			TagEditorData editorData = (TagEditorData) b.getSerializable(TagEditor.TAGEDIT_DATA);
			if (editorData.tags != null) {
				logic.setTags(editorData.type, editorData.osmId, editorData.tags);
			}
			if (editorData.parents != null) {
				logic.updateParentRelations(editorData.type, editorData.osmId, editorData.parents);
			}
			if (editorData.members != null && editorData.type.equals(Relation.NAME)) {
				logic.updateRelation(editorData.osmId, editorData.members);
			}
		}
		if (logic.getMode()==Mode.MODE_EASYEDIT && easyEditManager != null && !easyEditManager.isProcessingAction()) {
			// not in an easy edit mode, de-select objects avoids inconsistent visual state 
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			logic.setSelectedRelation(null);
		}
		map.invalidate();
	}
	
	void reindexPhotos() {
		new AsyncTask<Void, Integer, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				PhotoIndex pi = new PhotoIndex(Application.mainActivity);
				publishProgress(0);
				pi.createOrUpdateIndex();
				publishProgress(1);
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer ... progress) {
				if (progress[0] == 0)
					Toast.makeText(Application.mainActivity, R.string.toast_photo_indexing_started, Toast.LENGTH_SHORT).show();
				if (progress[0] == 1)
					Toast.makeText(Application.mainActivity, R.string.toast_photo_indexing_finished, Toast.LENGTH_SHORT).show();
			}

			@Override
			protected void onPostExecute(Void result) {
				map.getPhotosOverlay().resetRect();
				map.invalidate(); 
			}

		}.execute();
	}
	
	@Override
	public void onLowMemory() {
		Log.d("Main", "onLowMemory");
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
		logic.downloadCurrent(add);
	}

	private void performHttpLoad(final BoundingBox box) {
		logic.downloadBox(box, false, false);
	}

	private void openEmptyMap(final BoundingBox box) {
		logic.newEmptyMap(box);
	}

	/**
	 * @param closeChangeset 
	 * 
	 */
	public void performUpload(final String comment, final String source, final boolean closeChangeset) {
		dismissDialog(DialogFactory.CONFIRM_UPLOAD);
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			if (logic.hasChanges()) {
				logic.upload(comment, source, closeChangeset);
				logic.checkForMail();
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}
		} else {
			showDialog(DialogFactory.NO_LOGIN_DATA);
		}
	}
	
	/**
	 * Commit changes to the currently selected OpenStreetBug.
	 * @param comment Comment to add to the bug.
	 * @param close Flag to indicate if the bug is to be closed.
	 */
	public void performOpenStreetBugCommit(final String comment, final boolean close) {
		Log.d("Vespucci", "Main.performOpenStreetBugCommit");
		dismissDialog(DialogFactory.OPENSTREETBUG_EDIT);
		new CommitTask(logic.getSelectedBug(), comment, close) {
			
			/** Flag to track if the bug is new. */
			private boolean newBug;
			
			@Override
			protected void onPreExecute() {
				newBug = (bug.getId() == 0);
				setSupportProgressBarIndeterminateVisibility(true);
			}
			
			@Override
			protected Boolean doInBackground(Server... args) {
				// execute() is called below with no arguments (args will be empty)
				// getDisplayName() is deferred to here in case a lengthy OSM query
				// is required to determine the nickname
				
				Server server = prefs.getServer();
				
				return super.doInBackground(server);
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				if (result && newBug) {
					for (OpenStreetMapViewOverlay o : map.getOverlays()) {
						if (o instanceof de.blau.android.osb.MapOverlay) {
							((de.blau.android.osb.MapOverlay)o).addBug(bug);
						}
					}
				}
				setSupportProgressBarIndeterminateVisibility(false);
				Toast.makeText(getApplicationContext(), result ? R.string.openstreetbug_commit_ok : R.string.openstreetbug_commit_fail, Toast.LENGTH_SHORT).show();
				map.invalidate();
			}
			
		}.execute();
	}
	
	/**
	 * 
	 */
	public void performTrackUpload(final String description, final String tags, final Visibility visibility) {
		
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			logic.uploadTrack(getTracker().getTrack(), description, tags, visibility);
			logic.checkForMail();
		} else {
			showDialog(DialogFactory.NO_LOGIN_DATA);
		}
	}
	

	/**
	 * 
	 */
	public void confirmUpload() {
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			if (logic.hasChanges()) {
				if (server.needOAuthHandshake()) {
					oAuthHandshake(server);
					if (server.getOAuth()) // if still set
						Toast.makeText(getApplicationContext(), R.string.toast_oauth_retry, Toast.LENGTH_LONG).show();
					return;
				} 
				showDialog(DialogFactory.CONFIRM_UPLOAD);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}		
		} else {
			showDialog(DialogFactory.NO_LOGIN_DATA);
		}
	}
	
	
	/**
	 * 
	 * @param server
	 */
	@SuppressLint({ "SetJavaScriptEnabled", "InlinedApi", "NewApi" })
	public void oAuthHandshake(Server server) {
		ActionBar actionbar = getSupportActionBar();
		actionbar.hide();
		Server[] s = {server};
		String url = s[0].getBaseURL();
		OAuthHelper oa;
		try {
			oa = new OAuthHelper(url);
		}
		catch (OsmException oe) {
			server.setOAuth(false); // upps something went wrong turn oauth off
			actionbar.show();
			Toast.makeText(Main.this, getResources().getString(R.string.toast_no_oauth), Toast.LENGTH_LONG).show();
			return;
		}
		Log.d("Main", "oauth auth url " + url);
	
		String authUrl = oa.getRequestToken();
		if (authUrl == null) {
			Toast.makeText(Main.this, getResources().getString(R.string.toast_oauth_handshake_failed), Toast.LENGTH_LONG).show();
			actionbar.show();
			return;
		}
		Log.d("Main", "authURl " + authUrl);
		oAuthWebView = new WebView(Application.mainActivity);
		rl.addView(oAuthWebView);
		oAuthWebView.getSettings().setJavaScriptEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			oAuthWebView.getSettings().setAllowContentAccess(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			oAuthWebView.getLayoutParams().height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
			oAuthWebView.getLayoutParams().width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
		}
		oAuthWebView.requestFocus(View.FOCUS_DOWN);
		class MyWebViewClient extends WebViewClient {
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
		}
		
		oAuthWebView.setWebViewClient(new MyWebViewClient());
		oAuthWebView.loadUrl(authUrl);
	}
	
	/**
	 * removes the webview
	 */
	public void finishOAuth() {
		Log.d("Main","finishOAuth");
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
		if (logic.hasChanges()) {
			DialogFactory.createDataLossActivityDialog(this, intent, REQUEST_BOUNDINGBOX).show();
		} else {
			startActivityForResult(intent, REQUEST_BOUNDINGBOX);
		}
	}

	private enum AppendMode {
		APPEND_START, APPEND_APPEND
	}

	/**
	 * @param selectedElement
	 * @param focusOn if not null focus on the value field of this key
	 * @param applyLastAddressTags add address tags to the object being edited
	 */
	public void performTagEdit(final OsmElement selectedElement, String focusOn, boolean applyLastAddressTags) {
		if (selectedElement instanceof Node) {
			logic.setSelectedNode((Node) selectedElement);
		} else if (selectedElement instanceof Way) {
			logic.setSelectedWay((Way) selectedElement);
		}
	
		if (selectedElement != null) {
			if (logic.delegator.getOsmElement(selectedElement.getName(), selectedElement.getOsmId()) != null) {
				Intent startTagEditor = new Intent(getApplicationContext(), TagEditor.class);
				startTagEditor.putExtra(TagEditor.TAGEDIT_DATA, new TagEditorData(selectedElement, focusOn));
				startTagEditor.putExtra(TagEditor.TAGEDIT_LAST_ADDRESS_TAGS, Boolean.valueOf(applyLastAddressTags));
				startActivityForResult(startTagEditor, Main.REQUEST_EDIT_TAG);
			}
		}
	}

	/**
	 * potentially do some special stuff for invoking undo and exiting
	 */
	@Override
	public void onBackPressed() {
		// super.onBackPressed();
		Log.d("Main","onBackPressed()");
		if (prefs.useBackForUndo()) {
			String name = logic.getUndo().undo();
			if (name != null)
				Toast.makeText(Main.this, getResources().getString(R.string.undo) + ": " + name, Toast.LENGTH_SHORT).show();
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
	                Main.super.onBackPressed();
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

		@Override
		public void onClick(View arg0) {
			String name = logic.getUndo().undo();
			if (name != null) {
				Toast.makeText(Main.this, getResources().getString(R.string.undo) + ": " + name, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(Main.this, getResources().getString(R.string.undo_nothing), Toast.LENGTH_SHORT).show();
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
			UndoStorage undo = logic.getUndo();
			if (undo.canUndo() || undo.canRedo()) {
				UndoDialogFactory.showUndoDialog(Main.this, undo);
			} else {
				Toast.makeText(Main.this, getResources().getString(R.string.undo_nothing), Toast.LENGTH_SHORT).show();
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
		private List<Bug> clickedBugs;
		private List<Photo> clickedPhotos;

		private boolean touching;
		
		@Override
		public boolean onTouch(final View v, final MotionEvent m) {
			if (m.getAction() == MotionEvent.ACTION_DOWN) {
				clickedBugs = null;
				clickedPhotos = null;
				clickedNodesAndWays = null;
				logic.handleTouchEventDown(m.getX(), m.getY());
			}
			mDetector.onTouchEvent(v, m);
			return v.onTouchEvent(m);
		}
		
		@Override
		public void onDown(View v, float x, float y) {
			touching=true;
		}
		
		@Override
		public void onClick(View v, float x, float y) {
			de.blau.android.osb.MapOverlay osbo = map.getOpenStreetBugsOverlay();
			clickedBugs = (osbo != null) ? osbo.getClickedBugs(x, y, map.getViewBox()) : null;
			
			de.blau.android.photos.MapOverlay photos = map.getPhotosOverlay();
			clickedPhotos = (photos != null) ? photos.getClickedPhotos(x, y, map.getViewBox()) : null;
			
			Mode mode = logic.getMode();
			boolean isInEditZoomRange = logic.isInEditZoomRange();
			
			if (showGPS && !followGPS && map.getLocation() != null) {
				// check if this was a click on the GPS mark use the same calculations we use all over the place ... really belongs in a separate method 
				final float tolerance = Profile.getCurrent().nodeToleranceValue;				
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
					Toast.makeText(getApplicationContext(), R.string.toast_unlock_to_edit, Toast.LENGTH_SHORT).show();
					break;
				case MODE_OPENSTREETBUG:
					switch ((clickedBugs == null) ? 0 : clickedBugs.size()) {
					case 0:
						performBugEdit(logic.makeNewBug(x, y));
						break;
					case 1:
						performBugEdit(clickedBugs.get(0));
						break;
					default:
						v.showContextMenu();
						break;
					}
					break;
				case MODE_ADD:
					try {
						logic.performAdd(x, y);
					} catch (OsmIllegalOperationException e) {
						Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
					}
					break;
				case MODE_TAG_EDIT:
					selectElementForTagEdit(v, x, y);
					break;
				case MODE_ERASE:
					selectElementForErase(v, x, y);
					break;
				case MODE_SPLIT:
					selectElementForSplit(v, x, y);
					break;
				case MODE_EASYEDIT:
					performEasyEdit(v, x, y);
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
						case MODE_ADD:
						case MODE_ERASE:
						case MODE_SPLIT:
						case MODE_TAG_EDIT:
						case MODE_EASYEDIT:
							res = R.string.toast_not_in_edit_range;
							break;
						case MODE_OPENSTREETBUG:
							res = R.string.toast_not_in_bug_range;
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
				Log.d("Main", "viewPhoto exception starting intent: " + ex);	
			}
		}

		@Override
		public void onUp(View v, float x, float y) {
			if (logic.getMode() == Mode.MODE_EASYEDIT) {
				easyEditManager.invalidate();
			}
			touching=false;
		}
		
		@Override
		public boolean onLongClick(final View v, final float x, final float y) {
			if (logic.getMode() != Mode.MODE_EASYEDIT) {
				if (logic.getMode() == Mode.MODE_MOVE) {
					// display context menu
					de.blau.android.osb.MapOverlay osbo = map.getOpenStreetBugsOverlay();
					clickedBugs = (osbo != null) ? osbo.getClickedBugs(x, y, map.getViewBox()) : null;
					de.blau.android.photos.MapOverlay photos = map.getPhotosOverlay();
					clickedPhotos = (photos != null) ? photos.getClickedPhotos(x, y, map.getViewBox()) : null;
					clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
					return true; 
				}
				return false; // ignore long clicks
			}
			
			if (logic.isInEditZoomRange()) {
				return easyEditManager.handleLongClick(v, x, y);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_not_in_edit_range, Toast.LENGTH_LONG).show();
			}
			
			return true; // long click handled
		}

		@Override
		public void onDrag(View v, float x, float y, float dx, float dy) {
			logic.handleTouchEventMove(x, y, -dx, dy);
			setFollowGPS(false);
		}
		
		@Override
		public void onScale(View v, float scaleFactor, float prevSpan, float curSpan) {
			logic.zoom((curSpan - prevSpan) / prevSpan);
			updateZoomControls();
		}
		
		private void selectElementForTagEdit(final View v, final float x, final float y) {
			clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
			switch (((clickedBugs == null) ? 0 : clickedBugs.size()) + clickedNodesAndWays.size()) {
			case 0:
				// no elements were touched, ignore
				break;
			case 1:
				// exactly one element touched
				if (clickedBugs != null && clickedBugs.size() == 1) {
					performBugEdit(clickedBugs.get(0));
				} else {
					performTagEdit(clickedNodesAndWays.get(0), null, false);
				}
				break;
			default:
				// multiple possible elements touched - show menu
				v.showContextMenu();
				break;
			}
		}

		private void selectElementForErase(final View v, final float x, final float y) {
			clickedNodesAndWays = logic.getClickedNodes(x, y);
			switch (clickedNodesAndWays.size()) {
			case 0:
				// no elements were touched, ignore
				break;
			case 1:
				Log.i("Delete mode", "delete node");
				if (clickedNodesAndWays.get(0).hasParentRelations()) {
					Log.i("Delete mode", "node has relations");
					new AlertDialog.Builder(runningInstance)
						.setTitle(R.string.delete)
						.setMessage(R.string.deletenode_relation_description)
						.setPositiveButton(R.string.deletenode,
							new DialogInterface.OnClickListener() {	
								@Override
								public void onClick(DialogInterface dialog, int which) {
									logic.performEraseNode((Node)clickedNodesAndWays.get(0));
								}
							})
						.show();
				} else {
					logic.performEraseNode((Node)clickedNodesAndWays.get(0));
				}
				break;
			default:
				v.showContextMenu();
				break;
			}
		}

		private void selectElementForSplit(final View v, final float x, final float y) {
			clickedNodesAndWays = logic.getClickedNodes(x, y);

			//TODO remove nodes with no ways from list
//			for (Iterator iterator = clickedNodesAndWays.iterator(); iterator.hasNext();) {
//				Node node = (Node) iterator.next();
//				if (node.getWaysCount() < 1) {
//					iterator.remove();
//				}
//			}

			switch (clickedNodesAndWays.size()) {
			case 0:
				// no elements were touched, ignore
				break;
			case 1:
				logic.performSplit((Node)clickedNodesAndWays.get(0));
				break;
			default:
				v.showContextMenu();
				break;
			}
		}
		
		/**
		 * Perform easy edit touch processing.
		 * @param v
		 * @param x the click-position on the display.
		 * @param y the click-position on the display.
		 */
		public void performEasyEdit(final View v, final float x, final float y) {
			if (!easyEditManager.actionModeHandledClick(x, y)) {
				clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
				switch (((clickedBugs == null) ? 0 : clickedBugs.size()) + clickedNodesAndWays.size() + ((clickedPhotos == null)? 0 : clickedPhotos.size())) {
				case 0:
					// no elements were touched
					easyEditManager.nothingTouched();
					break;
				case 1:
					// exactly one element touched
					if (clickedBugs != null && clickedBugs.size() == 1) {
						performBugEdit(clickedBugs.get(0));
					}
					else if (clickedPhotos != null && clickedPhotos.size() == 1) {
						viewPhoto(clickedPhotos.get(0));
					} else {
						easyEditManager.editElement(clickedNodesAndWays.get(0));
					}
					break;
				default:
					// multiple possible elements touched - show menu
					if (menuRequired()) {
						v.showContextMenu();
					} else {
						// menuRequired tells us it's ok to just take the first one
						easyEditManager.editElement(clickedNodesAndWays.get(0));
					}
					break;
				}
			}
		}

		/**
		 * Edit an OpenStreetBug.
		 * @param bug The bug to edit.
		 */
		private void performBugEdit(final Bug bug) {
			Log.d("Vespucci", "editing bug:"+bug);
			final Server server = prefs.getServer();
			if (server != null && server.isLoginSet()) {
				if (server.needOAuthHandshake()) {
					oAuthHandshake(server);
					if (server.getOAuth()) // if still set
						Toast.makeText(getApplicationContext(), R.string.toast_oauth, Toast.LENGTH_LONG).show();
				} else {
					logic.setSelectedBug(bug);
					showDialog(DialogFactory.OPENSTREETBUG_EDIT);
				}
			} else {
				showDialog(DialogFactory.NO_LOGIN_DATA);
			}
		}
		
		@Override
		public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			onCreateDefaultContextMenu(menu, v, menuInfo);
		}
			
		public void onCreateDefaultContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			int id = 0;
			if (clickedPhotos != null) {
				for (Photo p : clickedPhotos) {
					menu.add(Menu.NONE, id++, Menu.NONE, p.getRef().getLastPathSegment()).setOnMenuItemClickListener(this);
				}
			}
			if (clickedBugs != null) {
				for (Bug b : clickedBugs) {
					menu.add(Menu.NONE, id++, Menu.NONE, b.getDescription()).setOnMenuItemClickListener(this);
				}
			}
			if (clickedNodesAndWays != null) {
				Mode mode = logic.getMode();
				for (OsmElement e : clickedNodesAndWays) {
					android.view.MenuItem mi = menu.add(Menu.NONE, id++, Menu.NONE, e.getDescription()).setOnMenuItemClickListener(this);
					// mi.setEnabled(mode != Mode.MODE_MOVE);
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
				Log.e("Main", "WTF? menuRequired called for single item?");
				return true;
			}
			
			// No bugs were clicked. Do we have nodes?
			if (clickedNodesAndWays.get(0) instanceof Node) {
				// Usually, we just take the first node.
				// However, check for *very* closely overlapping nodes first.
				Node candidate = (Node) clickedNodesAndWays.get(0);
				float nodeX = logic.getNodeScreenX(candidate);
				float nodeY = logic.getNodeScreenY(candidate);
				for (int i = 1; i < clickedNodesAndWays.size(); i++) {
					if (!(clickedNodesAndWays.get(i) instanceof Node)) break;
					Node possibleNeighbor = (Node)clickedNodesAndWays.get(i);
					float node2X = logic.getNodeScreenX(possibleNeighbor);
					float node2Y = logic.getNodeScreenY(possibleNeighbor);
					// Fast "square" checking is good enough
					if (Math.abs(nodeX-node2X) < Profile.NODE_OVERLAP_TOLERANCE_VALUE ||
						Math.abs(nodeY-node2Y) < Profile.NODE_OVERLAP_TOLERANCE_VALUE ) {
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
					switch (logic.getMode()) {
					case MODE_MOVE:
						showElementInfo(element);
						break;
					case MODE_TAG_EDIT:
						performTagEdit(element, null, false);
						break;
					case MODE_ERASE:
						if (element.hasParentRelations()) {
							Log.i("Delete mode", "node has relations");
							new AlertDialog.Builder(runningInstance)
								.setTitle(R.string.delete)
								.setMessage(R.string.deletenode_relation_description)
								.setPositiveButton(R.string.deletenode,
									new DialogInterface.OnClickListener() {	
										@Override
										public void onClick(DialogInterface dialog, int which) {
											logic.performEraseNode((Node)element);
										}
									})
								.show();
						} else {
							logic.performEraseNode((Node)element);
						}
						break;
					case MODE_SPLIT:
						logic.performSplit((Node) element);
						break;
					case MODE_EASYEDIT:
						easyEditManager.editElement(element);
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
			clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
			// clieckedPhotos and 
			if (clickedPhotos != null) {
				for (Photo p : clickedPhotos) {
					Toast.makeText(getApplicationContext(), p.getRef().getLastPathSegment(), Toast.LENGTH_SHORT).show();
				}
			}
			if (clickedBugs != null) {
				for (Bug b : clickedBugs) {
					Toast.makeText(getApplicationContext(), b.getDescription(), Toast.LENGTH_SHORT).show();
				}
			}
			if (clickedNodesAndWays != null) {
				for (OsmElement e : clickedNodesAndWays) {
					String toast = e.getDescription();
					if (e.hasProblem()) {
						String problem = e.describeProblem();
						toast = !problem.equals("") ? toast + "\n" + problem : toast;
					}
					Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
				}
			}	
		}
	}

	/**
	 * A KeyListener for all key events.
	 * 
	 * @author mb
	 */
	public class MapKeyListener implements OnKeyListener {

		@Override
		public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
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
					case KeyEvent.KEYCODE_SHIFT_LEFT:
					case KeyEvent.KEYCODE_SHIFT_RIGHT:
						logic.zoom(Logic.ZOOM_OUT);
						updateZoomControls();
						return true;
					}
				}
				break;
			}
			return false;
		}
		
		private void translate(final CursorPaddirection direction) {
			setFollowGPS(false);
			logic.translate(direction);
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
		List<String> changes = logic.getPendingChanges(this);
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
	
	public void triggerMapContextMenu() {
		map.showContextMenu();
	}
	
	public static boolean hasChanges() {
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

	/**
	 * @return the currentPreset
	 */
	public static Preset[] getCurrentPresets() {
		return currentPresets;
	}

	/**
	 * Resets the current preset, causing it to be re-parsed
	 */
	public static void resetPreset() {
		currentPresets = null; 
	}
	
	public String getBaseURL() {
		return prefs.getServer().getBaseURL();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.i("Main", "Tracker service connected");
		setTracker((((TrackerBinder)service).getService()));
		map.setTracker(getTracker());
		getTracker().setListener(this);
		getTracker().setListenerNeedsGPS(wantLocationUpdates);
		triggerMenuInvalidation();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// should never happen, but just to be sure
		Log.i("Main", "Tracker service disconnected");
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
			// some heuristics for now to keep downloading to a minimum
			// speed needs to be <= 6km/h (aka brisk walking speed) 
			if (autoDownload && (location.getSpeed() < 6000f/3600f) && (previousLocation==null || location.distanceTo(previousLocation) > prefs.getDownloadRadius()/8)) {
				ArrayList<BoundingBox> bbList = new ArrayList<BoundingBox>(logic.delegator.getBoundingBoxes());
				BoundingBox newBox = getNextBox(bbList,previousLocation, location);
				if (newBox != null) {
					if (prefs.getDownloadRadius() != 0) { // download
//						logic.delegator.addBoundingBox(newBox); // will be filled once download is complete
//						logic.downloadBox(newBox, true, true);
						// This is likely not worth the trouble
						ArrayList<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, newBox); 
						for (BoundingBox b:bboxes) {
							if (b.getWidth() < 0.000001 || b.getHeight() < 0.000001) {
								// ignore super small bb likely due to rounding errors
								Log.d("Main","getNextCenter very small bb " + b.toString());
								continue;
							}
							logic.delegator.addBoundingBox(b);  // will be filled once download is complete
							Log.d("Main","getNextCenter loading " + b.toString());
							logic.downloadBox(b, true, true); // TODO find out why isDirty doesn't work in this context
						}
					}
					previousLocation  = location;
				}

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
	
	boolean bbLoaded(ArrayList<BoundingBox> bbs, int lonE7, int latE7) {
	
		for (BoundingBox b:bbs) {
			if (b.isIn(latE7, lonE7)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return a suitable next BB, simply creates a raster of the download radius size
	 * @param location
	 * @return
	 */
	private BoundingBox getNextBox(ArrayList<BoundingBox> bbs,Location prevLocation, Location location) {
	
		
		double lon = location.getLongitude();
		double lat = location.getLatitude();
		double mlat = GeoMath.latToMercator(lat);
		double width = 2*GeoMath.convertMetersToGeoDistance(prefs.getDownloadRadius());
		
		int currentLeftE7 = (int) (Math.floor(lon / width)*width * 1E7);
		double currentMBottom = Math.floor(mlat/ width)*width;
		int currentBottomE7 = (int) (GeoMath.mercatorToLat(currentMBottom) * 1E7);
		int widthE7 = (int) (width*1E7);
		
		try {
			BoundingBox b = new BoundingBox(currentLeftE7, currentBottomE7, currentLeftE7 + widthE7, currentBottomE7 + widthE7);

			if (!bbLoaded(bbs, (int)(lon*1E7D), (int)(lat*1E7D))) {
				return b;
			}

			double bRight = b.getRight()/1E7d;
			double bLeft = b.getLeft()/1E7d;
			double mBottom = GeoMath.latE7ToMercator(b.getBottom());
			double mHeight = GeoMath.latE7ToMercator(b.getTop()) - mBottom;
			double dLeft = lon - bLeft;
			double dRight = bRight - lon;
			
			double dTop = mBottom + mHeight - mlat;
			double dBottom = mlat - mBottom;
			
			Log.d("Main","getNextCenter dLeft " + dLeft + " dRight " + dRight + " dTop " + dTop + " dBottom " + dBottom);
			Log.d("Main","getNextCenter " + b.toString());


			// top or bottom is closest
			if (dTop < dBottom) { // top closest
				if (dLeft < dRight) {
					return new BoundingBox(b.getLeft()-widthE7, b.getBottom(), b.getRight(), b.getTop() + widthE7);
				} else {
					return new BoundingBox(b.getLeft(), b.getBottom(), b.getRight() + widthE7, b.getTop() + widthE7);
				}	
			} else {
				if (dLeft < dRight) {
					return new BoundingBox(b.getLeft()-widthE7, b.getBottom()-widthE7, b.getRight(), b.getTop() );
				} else {
					return new BoundingBox(b.getLeft(), b.getBottom()-widthE7, b.getRight() + widthE7, b.getTop());
				}	
			}


		} catch (OsmException e) {
			// TODO Auto-generated catch block
			return null;
		}	
	}
	
	
	@Override
	/**
	 * DO NOT CALL DIRECTLY in custom code.
	 * Use {@link #triggerMenuInvalidation()} to make it easier to debug and implement workarounds for android bugs.
	 * Must be called from the main thread.
	 */
	public void invalidateOptionsMenu() { 
		// Log.d(DEBUG_TAG, "invalidateOptionsMenu called");
		super.invalidateOptionsMenu();
	}
	
	/**
	 * Simply calls {@link #invalidateOptionsMenu()}.
	 * Used to make it easier to implement workarounds.
	 * MUST BE CALLED FROM THE MAIN/UI THREAD!
	 */
	public void triggerMenuInvalidation() {
		Log.d(DEBUG_TAG, "triggerMenuInvalidation called");
		super.invalidateOptionsMenu(); // TODO delay or make conditional to work around android bug?
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

	/**
	 * Display some information about the element, for now simply as Dialog
	 * @param element
	 */
	public void showElementInfo(OsmElement element) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment prev = fm.findFragmentByTag("fragment_element_info");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.commit();

        ElementInfoFragment elementInfoDialog = ElementInfoFragment.newInstance(element);
        elementInfoDialog.show(fm, "fragment_element_info");
	}
}
