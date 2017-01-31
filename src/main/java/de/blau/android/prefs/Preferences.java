package de.blau.android.prefs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.osm.Server;
import de.blau.android.presets.Preset;
import de.blau.android.resources.DataStyle;

/**
 * Convenience class for parsing and holding the application's SharedPreferences.
 * 
 * @author mb
 */
public class Preferences {
	
	private final AdvancedPrefDatabase advancedPrefs;
	
	private final boolean isStatsVisible;
	
	private final boolean isToleranceVisible;
	
	private final boolean isAntiAliasingEnabled;
	
	private final boolean isOpenStreetBugsEnabled;
	
	private final boolean isPhotoLayerEnabled;
	
	private final boolean isKeepScreenOnEnabled;
	
	private final boolean useBackForUndo;
	
	private final boolean largeDragArea;
	
	private final boolean tagFormEnabled;
	
	private String backgroundLayer;
	
	private final String overlayLayer;
	
	private final String scaleLayer;
	
	private final String mapProfile;
	
	private final String followGPSbutton;
	
	private final String fullscreenMode;
	
	private int gpsInterval;
	
	private float gpsDistance;
	
	private float maxStrokeWidth;
	
	private int tileCacheSize; // in MB

	private int downloadRadius; // in m
	private float maxDownloadSpeed; // in km/h
	private int bugDownloadRadius;
	private float maxBugDownloadSpeed; // in km/h
	private Set<String> taskFilter; // can't be final
	
	private final boolean forceContextMenu;
	
	private final boolean enableNameSuggestions;
	
	private final boolean enableAutoPreset;
	
	private final boolean closeChangesetOnSave;
	
	private final boolean splitActionBarEnabled;
	
	private final String gpsSource;
	private final String gpsTcpSource;
	
	private final String offsetServer;
	
	private final boolean showCameraAction;
	
	private final boolean generateAlerts;
	
	private int maxAlertDistance;
	
	private final boolean lightThemeEnabled;
	
	private Set<String> addressTags; // can't be final

	private final boolean voiceCommandsEnabled;
	
	private final boolean leaveGpsDisabled;
	
	private final boolean showIcons;
	
	private final boolean showWayIcons;
	
	private int maxInlineValues;
	
	private int maxTileDownloadThreads;

	private int notificationCacheSize;
	
	private int autoLockDelay;
	
	private final boolean alwaysDrawBoundingBoxes;
	
	private final static String DEFAULT_MAP_PROFILE = "Color Round Nodes";
	
	private final SharedPreferences prefs;
	
	private final Resources r;
	
	/**
	 * @param prefs
	 * @param r
	 * @throws IllegalArgumentException
	 * @throws NotFoundException
	 */
	@SuppressLint("NewApi")
	public Preferences(Context ctx) throws IllegalArgumentException, NotFoundException {
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		r = ctx.getResources();
		advancedPrefs = new AdvancedPrefDatabase(ctx);
		
		// we're not using acra.disable - ensure it isn't present
		if (prefs.contains("acra.disable")) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove("acra.disable");
			editor.commit();
		}
		// we *are* using acra.enable
		if (!prefs.contains("acra.enable")) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("acra.enable", true);
			editor.commit();
		}
		
		try {
			maxStrokeWidth = Float.parseFloat(prefs.getString(r.getString(R.string.config_maxStrokeWidth_key), "16"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_maxStrokeWidth_key=" + prefs.getString(r.getString(R.string.config_maxStrokeWidth_key), "10"));
			maxStrokeWidth = 16;
		}
		try {
			tileCacheSize = Integer.parseInt(prefs.getString(r.getString(R.string.config_tileCacheSize_key), "10"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_tileCacheSize_key=" + prefs.getString(r.getString(R.string.config_tileCacheSize_key), "10"));
			tileCacheSize = 100;
		}
		try {
			downloadRadius = Integer.parseInt(prefs.getString(r.getString(R.string.config_extTriggeredDownloadRadius_key), "50"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_extTriggeredDownloadRadius_key=" + prefs.getString(r.getString(R.string.config_extTriggeredDownloadRadius_key), "50"));
			downloadRadius = 50;
		}
		try {
			maxDownloadSpeed = Float.parseFloat(prefs.getString(r.getString(R.string.config_maxDownloadSpeed_key), "6"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_maxDownloadSpeed_key=" + prefs.getString(r.getString(R.string.config_maxDownloadSpeed_key), "6"));
			maxDownloadSpeed = 6f;
		}
		try {
			bugDownloadRadius = Integer.parseInt(prefs.getString(r.getString(R.string.config_bugDownloadRadius_key), "200"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_extTriggeredDownloadRadius_key=" + prefs.getString(r.getString(R.string.config_bugDownloadRadius_key), "200"));
			bugDownloadRadius = 200;
		}
		try {
			maxBugDownloadSpeed = Float.parseFloat(prefs.getString(r.getString(R.string.config_maxBugDownloadSpeed_key), "30"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_maxDownloadSpeed_key=" + prefs.getString(r.getString(R.string.config_maxBugDownloadSpeed_key), "30"));
			maxBugDownloadSpeed = 30f;
		}
		taskFilter = new HashSet<String>(Arrays.asList(r.getStringArray(R.array.bug_filter_defaults)));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			taskFilter = prefs.getStringSet(r.getString(R.string.config_bugFilter_key), taskFilter);
		}

		isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), false);
		isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
		isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
		isOpenStreetBugsEnabled = prefs.getBoolean(r.getString(R.string.config_enableOpenStreetBugs_key), false);
		isPhotoLayerEnabled = prefs.getBoolean(r.getString(R.string.config_enablePhotoLayer_key), false);
		tagFormEnabled = prefs.getBoolean(r.getString(R.string.config_tagFormEnabled_key), true);
		isKeepScreenOnEnabled = prefs.getBoolean(r.getString(R.string.config_enableKeepScreenOn_key), false);
		useBackForUndo = prefs.getBoolean(r.getString(R.string.config_use_back_for_undo_key), false);
		largeDragArea = prefs.getBoolean(r.getString(R.string.config_largeDragArea_key), false);
		enableNameSuggestions = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestions_key), true);
		enableAutoPreset = prefs.getBoolean(r.getString(R.string.config_enableAutoPreset_key), true);
		closeChangesetOnSave = prefs.getBoolean(r.getString(R.string.config_closeChangesetOnSave_key), true);
		splitActionBarEnabled = prefs.getBoolean(r.getString(R.string.config_splitActionBarEnabled_key), true);
		backgroundLayer = prefs.getString(r.getString(R.string.config_backgroundLayer_key), null);
		overlayLayer = prefs.getString(r.getString(R.string.config_overlayLayer_key), null);
		scaleLayer = prefs.getString(r.getString(R.string.config_scale_key), "SCALE_METRIC");
		String tempMapProfile = prefs.getString(r.getString(R.string.config_mapProfile_key), null);
		// check if we actually still have the profile
		if (DataStyle.getStyle(tempMapProfile) == null) {
			if (DataStyle.getStyle(DEFAULT_MAP_PROFILE) == null) {
				Log.w(getClass().getName(), "Using builtin default profile instead of " + tempMapProfile + " and " + DEFAULT_MAP_PROFILE);
				mapProfile = DataStyle.getBuiltinStyleName(); // built-in fall back
			}
			else {
				Log.w(getClass().getName(), "Using default profile");
				mapProfile = DEFAULT_MAP_PROFILE;
			}
		} else {
			mapProfile = tempMapProfile;
		}
		gpsSource = prefs.getString(r.getString(R.string.config_gps_source_key), "internal");
		gpsTcpSource = prefs.getString(r.getString(R.string.config_gps_source_tcp_key), "127.0.0.1:1958");
		try {
			gpsDistance = Float.parseFloat(prefs.getString(r.getString(R.string.config_gps_distance_key), "2.0"));
			gpsInterval = Integer.parseInt(prefs.getString(r.getString(R.string.config_gps_interval_key), "1000"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_gps_distance_key or config_gps_interval_key");
			gpsDistance = 2.0f;
			gpsInterval = 1000;
		}
		forceContextMenu = prefs.getBoolean(r.getString(R.string.config_forceContextMenu_key), false);
		offsetServer = prefs.getString(r.getString(R.string.config_offsetServer_key), Urls.DEFAULT_OFFSET_SERVER);
		showCameraAction = prefs.getBoolean(r.getString(R.string.config_showCameraAction_key), true);
		generateAlerts = prefs.getBoolean(r.getString(R.string.config_generateAlerts_key), false);
		try {
			maxAlertDistance = Integer.parseInt(prefs.getString(r.getString(R.string.config_maxAlertDistance_key), "100"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_maxAlertDistance_key");
			maxAlertDistance = 100;
		}
		// light theme now always default
		lightThemeEnabled = prefs.getBoolean(r.getString(R.string.config_enableLightTheme_key), true);
		
		addressTags = new HashSet<String>(Arrays.asList(r.getStringArray(R.array.address_tags_defaults)));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			addressTags = prefs.getStringSet(r.getString(R.string.config_addressTags_key), addressTags);
		}
		
		voiceCommandsEnabled = prefs.getBoolean(r.getString(R.string.config_voiceCommandsEnabled_key), false);
		
		leaveGpsDisabled = prefs.getBoolean(r.getString(R.string.config_leaveGpsDisabled_key), false);

		showIcons = prefs.getBoolean(r.getString(R.string.config_showIcons_key), true);
		
		showWayIcons = prefs.getBoolean(r.getString(R.string.config_showWayIcons_key), true);
		
		followGPSbutton = prefs.getString(r.getString(R.string.config_followGPSbutton_key), "LEFT");
		
		fullscreenMode = prefs.getString(r.getString(R.string.config_fullscreenMode_key), Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? r.getString(R.string.full_screen_auto) : r.getString(R.string.full_screen_never));
	
		try {
			maxInlineValues = Integer.parseInt(prefs.getString(r.getString(R.string.config_maxInlineValues_key), "4"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_maxInlineValues_key=" + prefs.getString(r.getString(R.string.config_maxInlineValues_key), "4"));
			maxInlineValues = 4;
		}
		
		try {
			maxTileDownloadThreads = Integer.parseInt(prefs.getString(r.getString(R.string.config_maxTileDownloadThreads_key), "2"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_maxTileDownloadThreads_key=" + prefs.getString(r.getString(R.string.config_maxTileDownloadThreads_key), "2"));
			maxTileDownloadThreads = 2;
		}
		
		try {
			notificationCacheSize = Integer.parseInt(prefs.getString(r.getString(R.string.config_notificationCacheSize_key), "5"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_notificationCacheSize_key=" + prefs.getString(r.getString(R.string.config_notificationCacheSize_key), "5"));
			notificationCacheSize = 5;
		}
		
		try {
			autoLockDelay = Integer.parseInt(prefs.getString(r.getString(R.string.config_autoLockDelay_key), "60"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_autoLockDelay_key=" + prefs.getString(r.getString(R.string.config_autoLockDelay_key), "60"));
			autoLockDelay = 60;
		}
		
		alwaysDrawBoundingBoxes = prefs.getBoolean(r.getString(R.string.config_alwaysDrawBoundingBoxes_key), true);
	}
	
	/**
	 * @return the maximum width of a stroke
	 */
	public float getMaxStrokeWidth() {
		return maxStrokeWidth;
	}
	
	/**
	 * @return the size of the tile cache in MB
	 */
	public int getTileCacheSize() {
		return tileCacheSize;
	}
	
	/**
	 * @return
	 */
	public boolean isStatsVisible() {
		return isStatsVisible;
	}
	
	/**
	 * @return
	 */
	public boolean isToleranceVisible() {
		return isToleranceVisible;
	}
	
	/**
	 * @return
	 */
	public boolean isAntiAliasingEnabled() {
		return isAntiAliasingEnabled;
	}
	
	/**
	 * @return
	 */
	public boolean isOpenStreetBugsEnabled() {
		return isOpenStreetBugsEnabled;
	}
	
	/**
	 * @return
	 */
	public boolean isPhotoLayerEnabled() {
		return isPhotoLayerEnabled;
	}
	
	/**
	 * @return
	 */
	public boolean tagFormEnabled() {
		return tagFormEnabled;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isKeepScreenOnEnabled() {
		return isKeepScreenOnEnabled;
	}
	
	/**
	 * @return
	 */
	public boolean useBackForUndo() {
		return useBackForUndo;
	}
	
	/**
	 * @return
	 */
	public boolean largeDragArea() {
		return largeDragArea;
	}
	
	/**
	 * @return
	 */
	public String backgroundLayer() {
		return backgroundLayer;
	}
	
	public void setBackGroundLayer(String name) {
		backgroundLayer = name;
		prefs.edit().putString(r.getString(R.string.config_backgroundLayer_key), name).commit();
	}
	
	/**
	 * @return
	 */
	public String overlayLayer() {
		return overlayLayer;
	}
	
	/**
	 * @return
	 */
	public String scaleLayer() {
		return scaleLayer;
	}
	
	/**
	 * @return
	 */
	public String getMapProfile() {
		return mapProfile;
	}
	
	/**
	 * @return
	 */
	public Server getServer() {
		return advancedPrefs.getServerObject();
	}
	
	public Preset[] getPreset() {
		return advancedPrefs.getCurrentPresetObject();
	}
	
	public boolean getShowIcons() {
		return showIcons;
	}
	
	public boolean getShowWayIcons() {
		return showWayIcons;
	}

	/**
	 * @return
	 */
	public String getGpsSource() {
		return gpsSource;
	}
	
	/**
	 * @return
	 */
	public String getGpsTcpSource() {
		return gpsTcpSource;
	}
	
	/**
	 * @return
	 */
	public int getGpsInterval() {
		return gpsInterval;
	}
	
	/**
	 * @return
	 */
	public float getGpsDistance() {
		return gpsDistance;
	}
	
	public boolean getForceContextMenu() {
		return forceContextMenu;
	}
	
	public boolean getEnableNameSuggestions() {
		return enableNameSuggestions;
	}
	
	/**
	 * @return
	 */
	public int getDownloadRadius() {
		return downloadRadius;
	}
	
	/**
	 * @return
	 */
	public float getMaxDownloadSpeed() {
		return maxDownloadSpeed;
	}

	public int getBugDownloadRadius() {
		return bugDownloadRadius;
	}
	
	public float getMaxBugDownloadSpeed() {
		return maxBugDownloadSpeed;
	}
	
	public Set<String> taskFilter() {
		return taskFilter;
	}
	
	public boolean enableAutoPreset() {
		// 
		return enableAutoPreset;
	}
	
	/**
	 * @return
	 */
	public boolean closeChangesetOnSave() {
		return closeChangesetOnSave;
	}

	public boolean splitActionBarEnabled() {
		return splitActionBarEnabled;
	}

	public String getOffsetServer() {
		return offsetServer;
	}
	
	public boolean showCameraAction() {
		return showCameraAction;
	}
	
	public boolean generateAlerts() {
		return generateAlerts;
	}
	
	public boolean lightThemeEnabled() {
		return lightThemeEnabled;
	}
	
	public Set<String> addressTags() {
		return addressTags;
	}

	public int getMaxAlertDistance() {
		return maxAlertDistance;
	}

	public boolean voiceCommandsEnabled() {
		return voiceCommandsEnabled;
	}

	public boolean leaveGpsDisabled() {
		return leaveGpsDisabled;
	}
	
	public String followGPSbuttonPosition() {
		return followGPSbutton;
	}
	
	public String getFullscreenMode() {
		return fullscreenMode;
	}
	
	public int getMaxInlineValues() {
		return maxInlineValues;
	}
	
	public int getMaxTileDownloadThreads() {
		return maxTileDownloadThreads;
	}

	public int getNotificationCacheSize() {
		return notificationCacheSize;
	}

	public int getAutolockDelay() {
		return 1000*autoLockDelay;
	}
	
	public void setAutoDownload(boolean on) {
		prefs.edit().putBoolean(r.getString(R.string.config_autoDownload_key), on).commit();
	}


	public boolean getAutoDownload() {
		String key = r.getString(R.string.config_autoDownload_key);
		if (!prefs.contains(key)) {
			// create the entry
			setAutoDownload(false);
		}
		return prefs.getBoolean(key, false);
	}

	public void setContrastValue(float cValue){
		prefs.edit().putFloat(r.getString(R.string.config_contrastValue_key),cValue).commit();
	}

	public float getContrastValue(){
		String key = r.getString(R.string.config_contrastValue_key);
		if (!prefs.contains(key)) {
			// create the entry
			setContrastValue(0);
		}
		return prefs.getFloat(key, 0);
	}

	public void setBugAutoDownload(boolean on) {
		prefs.edit().putBoolean(r.getString(R.string.config_bugAutoDownload_key), on).commit();
	}
	
	public boolean getBugAutoDownload() {
		String key = r.getString(R.string.config_bugAutoDownload_key);
		if (!prefs.contains(key)) {
			// create the entry
			setBugAutoDownload(false);
		}
		return prefs.getBoolean(key, false);
	}
	
	public void setShowGPS(boolean on) {
		prefs.edit().putBoolean(r.getString(R.string.config_showGPS_key), on).commit();
	}
	
	public boolean getShowGPS() {
		String key = r.getString(R.string.config_showGPS_key);
		if (!prefs.contains(key)) {
			// create the entry
			setShowGPS(true);
		}
		return prefs.getBoolean(key, true);
	}
	
	public boolean getAlwaysDrawBoundingBoxes() {	
		return alwaysDrawBoundingBoxes;
	}
	
	public void enableTagFilter(boolean on) {
		prefs.edit().putBoolean(r.getString(R.string.config_tagFilter_key), on).commit();
	}
	
	public boolean getEnableTagFilter() {
		String key = r.getString(R.string.config_tagFilter_key);
		if (!prefs.contains(key)) {
			// create the entry
			enableTagFilter(false);
		}
		return prefs.getBoolean(key, false);
	}
	
	public void setGeocoder(int index) {
		prefs.edit().putInt(r.getString(R.string.config_geocoder_key), index).commit();
	}
	
	public int getGeocoder() {
		String key = r.getString(R.string.config_geocoder_key);
		if (!prefs.contains(key)) {
			// create the entry
			setGeocoder(0);
		}
		return prefs.getInt(key, 0);
	}
}
