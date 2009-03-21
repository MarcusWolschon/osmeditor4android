package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.location.Location;

public class Track implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final ArrayList<Location> track;

	public Track() {
		this.track = new ArrayList<Location>();
	}

	public void reset() {
		track.clear();
	}

	public void addTrackPoint(final Location location) {
	    // TODO Sometimes null is added to the Track und leads to a NPE later on. Checking for null
        // here is just a workaround. It would be better to avoid added null in the first place.
	    if (location != null)
	        track.add(location); 
	}

	public List<Location> getTrackPoints() {
		return Collections.unmodifiableList(track);
	}

	@Override
	public String toString() {
		String str = "";
		for (Location loc : track) {
			str += loc.toString() + '\n';
		}
		return str;
	}
}
