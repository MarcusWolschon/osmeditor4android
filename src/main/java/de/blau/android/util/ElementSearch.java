package de.blau.android.util;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import de.blau.android.App;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

public class ElementSearch {
    
    private static final String DEBUG_PLACE_TAG = "PlaceTagValues...";
	private static final String DEBUG_STREET_TAG = "StreetTagValues...";
	private static final double MAX_DISTANCE = 20000D; // this is just a very rough number to stop including stuff that is very far away
	private String[] streetNames = null;
    private Map<String, Long> idsByStreetNames = new HashMap<String, Long>();
    
    private String[] placeNames = null;
    private Map<String, Long> idsByPlaceNames = new HashMap<String, Long>();
    private Map<String, String> typeByPlaceNames = new HashMap<String, String>();

    private final int[] location;
    private final  boolean distanceFilter;
    
    
    public ElementSearch(final int[] location, boolean distanceFilter) {
    	this.location = location;
    	this.distanceFilter = distanceFilter;
    }
    
	/**
     * Get all distance sorted street-names in the area
     * @param delegator
     * @param location
     * @return all street-names
     */
    private String[] getStreetArray(final int[] location) {
		// build list of names with their closest distance to location
    	final StorageDelegator delegator = App.getDelegator();
		Map<String, Double> distancesByNames = new HashMap<String, Double>();
		String[] nameTags = {Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME, Tags.KEY_NAME_LEFT, Tags.KEY_NAME_RIGHT};
		
		for (Way way : delegator.getCurrentStorage().getWays()) {
			if (way.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
				double distance = -1D;
				long iD = way.getOsmId();
				for (String tag:nameTags) { 
					// Log.d("StreetTagValueAutocompletionAdapter","Search for " + tag);
					String name = way.getTagWithKey(tag);
					if (name != null) {
						// Log.d("StreetTagValueAutocompletionAdapter","Name " + name);
						if (distance == -1D) { // only calc once
							distance = way.getDistance(location);
							if (distanceFilter && distance > MAX_DISTANCE) {
								break;
							}
							// Log.d("ElementSearch","distance " + distance);
						}
						if (distancesByNames.containsKey(name)) {
							// way already in list - keep shortest distance
							if (distance <  distancesByNames.get(name)) {
								distancesByNames.put(name, distance);
								idsByStreetNames.put(name,Long.valueOf(iD));
							}
						} else {
							distancesByNames.put(name, distance);
							idsByStreetNames.put(name,Long.valueOf(iD));
						}
					}
				}
			}
		}
		// sort names by distance
		MultiHashMap<Double, String> retval = new MultiHashMap<Double, String>(true); // true == sorted
		for (String name : distancesByNames.keySet()) {
			retval.add(distancesByNames.get(name), name);
		}	 
		return retval.getValues().toArray(new String[retval.getValues().size()]);
	}

	public synchronized String[] getStreetNames() {
		if (streetNames == null) {
		   	streetNames = getStreetArray(location);
		}
		return streetNames;
	}
	
	public long getStreetId(String name) throws OsmException {
		if (streetNames == null) {
		   	streetNames = getStreetArray(location);
		}
		Log.d(DEBUG_STREET_TAG,"looking for " + name);
		Long iD = idsByStreetNames.get(name);
		if (iD != null) {
			return iD.longValue();
		}
		else {
			throw new OsmException("way not found in adapter");
		}
	}
	
	   
    /**
     * Get all distance sorted place-names in the area
     * @param delegator
     * @param location
     * @return all place-names
     */
    private String[] getPlaceArray(final int[] location) {
		// build list of names with their closest distance to location
    	final StorageDelegator delegator = App.getDelegator(); 
		Map<String, Double> distancesByName = new HashMap<String, Double>();
		String[] nameTags = {Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME};
		Log.d(DEBUG_PLACE_TAG,"searching for place ways...");
		for (Way way : delegator.getCurrentStorage().getWays()) {
			if (way.getTagWithKey(Tags.KEY_PLACE) != null) {
				double distance = -1D;
				long iD = way.getOsmId();
				
				for (String tag:nameTags) { 
					String name = way.getTagWithKey(tag);
					if (name != null) {
						if (distance == -1D) { // only calc once
							distance = way.getDistance(location);
							if (distanceFilter && distance > MAX_DISTANCE) {
								break;
							}
						}
						if (distancesByName.containsKey(name)) {
							// way already in list - keep shortest distance
							if (distance <  distancesByName.get(name)) {
								distancesByName.put(name, distance);
								idsByPlaceNames.put(name,Long.valueOf(iD));
								typeByPlaceNames.put(name,Way.NAME);
							}
						} else {
							distancesByName.put(name, distance);
							idsByPlaceNames.put(name,Long.valueOf(iD));
							typeByPlaceNames.put(name,Way.NAME);
						}
					}
				}
			}
		}
		Log.d(DEBUG_PLACE_TAG,"searching for place nodes...");
		for (Node node : delegator.getCurrentStorage().getNodes()) {
			if (node.getTagWithKey(Tags.KEY_PLACE) != null) {
				double distance = -1D;
				long iD = node.getOsmId();

				for (String tag:nameTags) {
					String name = node.getTagWithKey(tag);
					Log.d(DEBUG_PLACE_TAG,"adding " + name);
					if (name != null) {
						if (distance == -1D) { // only calc once
							distance = node.getDistance(location);
							if (distanceFilter && distance > MAX_DISTANCE) {
								break;
							}
						}
						if (distancesByName.containsKey(name)) {
							// way already in list - keep shortest distance
							if (distance <  distancesByName.get(name)) {
								distancesByName.put(name, distance);
								idsByPlaceNames.put(name,Long.valueOf(iD));
								typeByPlaceNames.put(name,Node.NAME);
							}
						} else {
							distancesByName.put(name, distance);
							idsByPlaceNames.put(name,Long.valueOf(iD));
							typeByPlaceNames.put(name,Node.NAME);
						}
					}
				}
			}
		}
		// sort names by distance
		MultiHashMap<Double, String> retval = new MultiHashMap<Double, String>(true);
		for (String name : distancesByName.keySet()) {
			retval.add(distancesByName.get(name), name);
		}
		 
		return retval.getValues().toArray(new String[retval.getValues().size()]);
	}

	public String[] getPlaceNames() {
		if (placeNames == null) {
		   	placeNames = getPlaceArray(location);
		}
		return placeNames;
	}
	
	public long getPlaceId(String name) throws OsmException {
		Log.d(DEBUG_PLACE_TAG,"looking for " + name);
		if (placeNames == null) {
		   	placeNames = getPlaceArray(location);
		}
		Long iD = idsByPlaceNames.get(name);
		if (iD != null) {
			return iD.longValue();
		}
		else {
			throw new OsmException("object not found in adapter");
		}
	}
}
