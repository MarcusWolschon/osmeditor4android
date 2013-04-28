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
	
	private final String backgroundLayer;
	
	private final String mapProfile;
	
	private int gpsInterval;
	
	private float gpsDistance;
	
	private float maxStrokeWidth;

	private boolean forceContextMenu;
	
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
			maxStrokeWidth = Float.parseFloat(prefs.getString(r.getString(R.string.config_maxStrokeWidth_key), "10"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsing config_maxStrokeWidth_key=" + prefs.getString(r.getString(R.string.config_maxStrokeWidth_key), "10"));
			maxStrokeWidth = 10;
		}
		isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), true);
		isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
		isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
		isOpenStreetBugsEnabled = prefs.getBoolean(r.getString(R.string.config_enableOpenStreetBugs_key), false);
		backgroundLayer = prefs.getString(r.getString(R.string.config_backgroundLayer_key), null);
		String tempMapProfile = prefs.getString(r.getString(R.string.config_mapProfile_key), null);
		// check if we actually still have the profile
		if (Profile.getProfile(tempMapProfile) == null) {
			mapProfile = Profile.getDefaultProfileName(); //TODO should store this as new pref
		} else {
			mapProfile = tempMapProfile;
		}
		try {
			gpsDistance = Float.parseFloat(prefs.getString(r.getString(R.string.config_gps_distance_key), "5.0"));
			gpsInterval = Integer.parseInt(prefs.getString(r.getString(R.string.config_gps_interval_key), "1000"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsind config_gps_distance_key or config_gps_interval_key");
			gpsDistance = 5.0f;
			gpsInterval = 1000;
		}
		forceContextMenu = prefs.getBoolean(r.getString(R.string.config_forceContextMenu_key), true);
	}
	
	/**
	 * @return the maximum width of a stroke
	 */
	public float getMaxStrokeWidth() {
		return maxStrokeWidth;
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
	public String backgroundLayer() {
		return backgroundLayer;
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
	
	public Preset getPreset() {
		return advancedPrefs.getCurrentPresetObject();
	}
	
	public boolean getShowIcons() {
		return advancedPrefs.getCurrentAPI().showicon;
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
	
}
