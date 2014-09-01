package de.blau.android.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.osm.Server;
import de.blau.android.presets.Preset;
import de.blau.android.resources.Profile;

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
	
	private final boolean depreciatedModesEnabled;
	
	private final boolean useBackForUndo;
	
	private final boolean largeDragArea;
	
	private final String backgroundLayer;
	
	private final String overlayLayer;
	
	private final String mapProfile;
	
	private int gpsInterval;
	
	private float gpsDistance;
	
	private float maxStrokeWidth;
	
	private int tileCacheSize; // in MB

	private int downloadRadius; // in m
	
	private final boolean forceContextMenu;
	
	private final boolean enableNameSuggestions;
	
	private final boolean enableAutoPreset;
	
	private final boolean closeChangesetOnSave;
	
	private final boolean splitActionBarEnabled;
	
	private final String gpsSource;
	private final String gpsTcpSource;
	
	private final String offsetServer;
	
	private final static String DEFAULT_MAP_PROFILE = "Color Round Nodes";
	
	/**
	 * @param prefs
	 * @param r
	 * @throws IllegalArgumentException
	 * @throws NotFoundException
	 */
	public Preferences(Context ctx) throws IllegalArgumentException, NotFoundException {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		final Resources r = ctx.getResources();
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
		isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), false);
		isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
		isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
		isOpenStreetBugsEnabled = prefs.getBoolean(r.getString(R.string.config_enableOpenStreetBugs_key), false);
		isPhotoLayerEnabled = prefs.getBoolean(r.getString(R.string.config_enablePhotoLayer_key), false);
		isKeepScreenOnEnabled = prefs.getBoolean(r.getString(R.string.config_enableKeepScreenOn_key), false);
		depreciatedModesEnabled = prefs.getBoolean(r.getString(R.string.config_enableDepreciatedModes_key), false);
		useBackForUndo = prefs.getBoolean(r.getString(R.string.config_use_back_for_undo_key), false);
		largeDragArea = prefs.getBoolean(r.getString(R.string.config_largeDragArea_key), false);
		enableNameSuggestions = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestions_key), true);
		enableAutoPreset = prefs.getBoolean(r.getString(R.string.config_enableAutoPreset_key), true);
		closeChangesetOnSave = prefs.getBoolean(r.getString(R.string.config_closeChangesetOnSave_key), true);
		splitActionBarEnabled = prefs.getBoolean(r.getString(R.string.config_splitActionBarEnabled_key), true);
		backgroundLayer = prefs.getString(r.getString(R.string.config_backgroundLayer_key), null);
		overlayLayer = prefs.getString(r.getString(R.string.config_overlayLayer_key), null);
		String tempMapProfile = prefs.getString(r.getString(R.string.config_mapProfile_key), null);
		// check if we actually still have the profile
		if (Profile.getProfile(tempMapProfile) == null) {
			if (Profile.getProfile(DEFAULT_MAP_PROFILE) == null) {
				Log.w(getClass().getName(), "Using builtin default profile instead of " + tempMapProfile + " and " + DEFAULT_MAP_PROFILE);
				mapProfile = Profile.getBuiltinProfileName(); // built-in fall back
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
		forceContextMenu = prefs.getBoolean(r.getString(R.string.config_forceContextMenu_key), true);
		offsetServer = prefs.getString(r.getString(R.string.config_offsetServer_key), "http://offsets.textual.ru/");
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
	 * 
	 * @return
	 */
	public boolean isKeepScreenOnEnabled() {
		return isKeepScreenOnEnabled;
	}
	
	/**
	 * @return
	 */
	public boolean depreciatedModesEnabled() {
		return depreciatedModesEnabled;
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
	
	/**
	 * @return
	 */
	public String overlayLayer() {
		return overlayLayer;
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
		return advancedPrefs.getCurrentAPI().showicon;
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
}
