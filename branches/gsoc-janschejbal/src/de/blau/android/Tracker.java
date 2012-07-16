package de.blau.android;

import java.util.List;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import de.blau.android.exception.FollowGpsException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Track;
import de.blau.android.prefs.Preferences;

/**
 * GPS-Tracker. Holds a {@link Track} and manages it.
 * 
 * @author mb
 */
public class Tracker implements LocationListener {

    /**
     * Tag used for Android-logging.
     */
	private static final String DEBUG_TAG = Tracker.class.getName();

	private static final float LOCATION_MIN_ACCURACY = 200f;

	public static final byte STATE_START = 0;

	public static final byte STATE_PAUSE = 1;

	public static final byte STATE_STOP = 2;

	private int trackingState = STATE_START;

	private boolean followGps;

	private LocationManager locationManager;

	private Preferences prefs;

	private final Map map;

	private final Track track;

	Tracker(final LocationManager locationManager, final Map map) {
		this.locationManager = locationManager;
		this.map = map;
		track = new Track();
		followGps = true;
	}

	boolean setFollowGps(final boolean followGps) throws FollowGpsException {
		this.followGps = followGps;
		if (followGps) {
			List<Location> trackPoints = track.getTrackPoints();
			int trackPointCount = trackPoints.size();

			if (trackPointCount > 0) {
				moveScreenToLocation(trackPoints.get(trackPointCount - 1));
				return true;
			} else {
				throw new FollowGpsException("Got no current location");
			}
		} else {
			return false;
		}
	}

	void setPrefs(final Preferences prefs) {
		this.prefs = prefs;
		setTrackingState(trackingState);
	}

	void resetTrack() {
		track.reset();
	}

	public Track getTrack() {
		return track;
	}

	void setTrackingState(int newTrackingState) {
		if (prefs != null) {
			//PAUSE is only a toggle-flag
			if (newTrackingState == STATE_PAUSE && (trackingState == STATE_PAUSE || trackingState == STATE_STOP)) {
				newTrackingState = STATE_START;
			}
			switch (newTrackingState) {
			case STATE_START:
				Location lastKnownLocation = null;
				try {
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefs.getGpsInterval(),
							prefs.getGpsDistance(), this);
					lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				} catch (IllegalArgumentException e) {
					// do nothing - leave null
				} catch (SecurityException e) {
					// do nothing - leave null
				}
				if (lastKnownLocation != null) {
					track.addTrackPoint(lastKnownLocation);
				}
				break;

			case STATE_STOP:
				resetTrack();
				locationManager.removeUpdates(this);
				break;

			case STATE_PAUSE:
				locationManager.removeUpdates(this);
				break;
			}
			trackingState = newTrackingState;
			map.invalidate();
		}
	}

	/**
	 * used for disable updates but not to set a new tracking state (e.g. on exiting)
	 */
	void removeUpdates() {
		locationManager.removeUpdates(this);
	}

	/**
	 * @param location
	 */
	private void moveScreenToLocation(final Location location) {
		BoundingBox viewBox = map.getViewBox();
		// ensure the view is zoomed in to at least the most zoomed-out
		while (!viewBox.canZoomOut() && viewBox.canZoomIn()) {
			viewBox.zoomIn();
		}
		viewBox.moveTo((int) (location.getLongitude() * 1E7d), (int) (location.getLatitude() * 1E7d));
		map.invalidate();
	}

	@Override
	public void onLocationChanged(final Location location) {
		Log.v(DEBUG_TAG, "onLocationChanged() Got location: " + location);
		if (!location.hasAccuracy() || location.getAccuracy() <= LOCATION_MIN_ACCURACY) {
			track.addTrackPoint(location);
			if (followGps) {
				moveScreenToLocation(location);
			} else {
				map.invalidate();
			}
		}
	}

	@Override
	public void onProviderDisabled(final String provider) {}

	@Override
	public void onProviderEnabled(final String provider) {}

	@Override
	public void onStatusChanged(final String provider, final int status, final Bundle extras) {}

}
