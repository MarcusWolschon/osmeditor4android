package de.blau.android;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
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

	private final Server server;

	private int gpsInterval;

	private float gpsDistance;

	/**
	 * @param prefs
	 * @param r
	 * @throws IllegalArgumentException
	 * @throws NotFoundException
	 */
	public Preferences(final SharedPreferences prefs, final Resources r) throws IllegalArgumentException,
			NotFoundException {
		isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), true);
		isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
		isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
		String username = prefs.getString(r.getString(R.string.config_username_key), null);
		String password = prefs.getString(r.getString(R.string.config_password_key), null);
		try {
			gpsDistance = Float.parseFloat(prefs.getString(r.getString(R.string.config_gps_distance_key), "5.0"));
			gpsInterval = Integer.parseInt(prefs.getString(r.getString(R.string.config_gps_interval_key), "1000"));
		} catch (NumberFormatException e) {
			gpsDistance = 5.0f;
			gpsInterval = 1000;
		}
		server = new Server(username, password, r.getString(R.string.app_name) + r.getString(R.string.app_version));
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
