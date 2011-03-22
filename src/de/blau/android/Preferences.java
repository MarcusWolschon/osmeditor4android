package de.blau.android;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.util.Log;
import de.blau.android.osm.Server;

/**
 * Convenience class for parsing and holding the application's SharedPreferences.
 * 
 * @author mb
 */
public class Preferences {
	
	private final boolean isStatsVisible;
	
	private final boolean isToleranceVisible;
	
	private final boolean isAntiAliasingEnabled;
	
	private final boolean isOpenStreetBugsEnabled;
	
	private final String backgroundLayer;
	
	/**
	 * Credentials (username and password) for the
	 * OpenStreetMap API(0.6)-Server
	 */
	private final Server server;
	
	private int gpsInterval;
	
	private float gpsDistance;
	
	private float maxStrokeWidth;
	
	/**
	 * @param prefs
	 * @param r
	 * @throws IllegalArgumentException
	 * @throws NotFoundException
	 */
	public Preferences(final SharedPreferences prefs, final Resources r) throws IllegalArgumentException,
			NotFoundException {
		try {
			maxStrokeWidth = Float.parseFloat(prefs.getString(r.getString(R.string.config_maxStrokeWidth_key), "10"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsind config_maxStrokeWidth_key=" + prefs.getString(r.getString(R.string.config_maxStrokeWidth_key), "10"));
			maxStrokeWidth = 10;
		}
		isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), true);
		isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
		isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
		isOpenStreetBugsEnabled = prefs.getBoolean(r.getString(R.string.config_enableOpenStreetBugs_key), false);
		backgroundLayer = prefs.getString(r.getString(R.string.config_backgroundLayer_key), null);
		String username = prefs.getString(r.getString(R.string.config_username_key), null);
		String password = prefs.getString(r.getString(R.string.config_password_key), null);
		try {
			gpsDistance = Float.parseFloat(prefs.getString(r.getString(R.string.config_gps_distance_key), "5.0"));
			gpsInterval = Integer.parseInt(prefs.getString(r.getString(R.string.config_gps_interval_key), "1000"));
		} catch (NumberFormatException e) {
			Log.w(getClass().getName(), "error parsind config_gps_distance_key or config_gps_interval_key");
			gpsDistance = 5.0f;
			gpsInterval = 1000;
		}
		server = new Server(username, password, r.getString(R.string.app_name) + " "
				+ r.getString(R.string.app_version));
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
	public Server getServer() {
		return server;
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
	
}
