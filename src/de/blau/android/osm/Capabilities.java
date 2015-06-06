package de.blau.android.osm;

import java.util.ArrayList;

/**
 * Capabilities of the API server we are connected too
 * Default values are as of September 2014
 * @author simon
 */
public class Capabilities {

	public static enum Status {
		ONLINE,
		READONLY,
		OFFLINE
	}

	// API related
	public String minVersion = "0.6";
	public String maxVersion = "0.6";
	float areaMax = 0.25f;
	int maxTracepointsPerPage = 5000;
	int maxWayNodes = 2000;
	int maxElementsInChangeset = 50000;
	int timeout = 300;
	public Status dbStatus = Status.OFFLINE;
	public Status apiStatus = Status.OFFLINE;
	public Status gpxStatus = Status.OFFLINE;
	// policy
	public ArrayList<String> imageryBlacklist = new ArrayList<String>();


	public Status stringToStatus(String s) {
		if (s == null) {
			return Status.OFFLINE;
		}
		if (s.equals("online")) {
			return Status.ONLINE;
		} else if (s.equals("readonly")) {
			return Status.READONLY;
		} else {
			return Status.OFFLINE;
		}
	}

	public static Capabilities getDefault() {
		Capabilities d = new Capabilities();
		d.imageryBlacklist.add(".*\\.google\\.ru/.*");
		d.imageryBlacklist.add(".*\\.google\\.com/.*");
		d.imageryBlacklist.add(".*\\.googleapis\\.com/.*");
		// this is wishful thinking
		// TODO add unknown status
		d.dbStatus = Status.ONLINE;
		d.apiStatus = Status.ONLINE;
		d.gpxStatus = Status.ONLINE;
		return d;
	}

	/**
	 * update the limits used in various places to what we have got currently
	 * likely it would be better for these places to refer to a static version
	 * of the values here 
	 */
	public void updateLimits() {
		Way.maxWayNodes = maxWayNodes;
	}
}
