package de.blau.android.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import androidx.annotation.NonNull;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.collections.MultiHashMap;

public class ElementSearch {

    private static final String DEBUG_PLACE_TAG  = "PlaceTagValues...";
    private static final String DEBUG_STREET_TAG = "StreetTagValues...";
    private static final double MAX_DISTANCE     = 20000D;              // this is just a very rough number to stop
                                                                        // including stuff that is very far away
    private String[]            streetNames      = null;
    private Map<String, Long>   idsByStreetNames = new HashMap<>();

    private String[] placeNames = null;

    private final int[]   location;
    private final boolean distanceFilter;

    /**
     * Search for OSM elements around a location
     * 
     * @param location location coordinates in WGS84°*1E7
     * @param distanceFilter if true ignore objects further away than MAX_DISTANCE
     */
    public ElementSearch(@NonNull final int[] location, boolean distanceFilter) {
        this.location = location;
        this.distanceFilter = distanceFilter;
    }

    /**
     * Get all distance sorted street-names in the area
     * 
     * @param location position we want the names nearby to
     * @return all street-names
     */
    private String[] getStreetArray(@NonNull final int[] location) {
        // build list of names with their closest distance to location
        final StorageDelegator delegator = App.getDelegator();
        Map<String, Double> distancesByNames = new HashMap<>();
        String[] nameTags = { Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME, Tags.KEY_NAME_LEFT, Tags.KEY_NAME_RIGHT };

        for (Way way : delegator.getCurrentStorage().getWays()) {
            if (way.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
                double distance = -1D;
                long iD = way.getOsmId();
                for (String tag : nameTags) {
                    // Log.d("StreetTagValueAutocompletionAdapter","Search for " + tag);
                    String name = way.getTagWithKey(tag);
                    if (name != null) {
                        // Log.d("StreetTagValueAutocompletionAdapter","Name " + name);
                        if (distance == -1D) { // only calc once
                            distance = way.getMinDistance(location);
                            if (distanceFilter && distance > MAX_DISTANCE) {
                                break;
                            }
                            // Log.d("ElementSearch","distance " + distance);
                        }
                        if (distancesByNames.containsKey(name)) {
                            // way already in list - keep shortest distance
                            if (distance < distancesByNames.get(name)) {
                                distancesByNames.put(name, distance);
                                idsByStreetNames.put(name, iD);
                            }
                        } else {
                            distancesByNames.put(name, distance);
                            idsByStreetNames.put(name, iD);
                        }
                    }
                }
            }
        }
        // sort names by distance
        MultiHashMap<Double, String> retval = new MultiHashMap<>(true); // true == sorted
        for (Entry<String, Double> entry : distancesByNames.entrySet()) {
            retval.add(entry.getValue(), entry.getKey());
        }
        return retval.getValues().toArray(new String[retval.getValues().size()]);
    }

    /**
     * Get a distance sorted list of street names relative to the location given in the constructor
     * 
     * @return array containing the names
     */
    public synchronized String[] getStreetNames() {
        if (streetNames == null) {
            streetNames = getStreetArray(location);
        }
        return streetNames;
    }

    /**
     * Given a street name return the OSM id of the way
     * 
     * @param name the street name
     * @return the OSM id of the way
     * @throws OsmException if the name is not found in the index
     */
    public long getStreetId(@NonNull String name) throws OsmException {
        if (streetNames == null) {
            streetNames = getStreetArray(location);
        }
        Log.d(DEBUG_STREET_TAG, "looking for " + name);
        Long iD = idsByStreetNames.get(name);
        if (iD != null) {
            return iD;
        } else {
            throw new OsmException("way not found in adapter");
        }
    }

    /**
     * Get all distance sorted place-names in the area
     * 
     * @param location coordinates in WGS84°*1E7
     * @return all place-names
     */
    private String[] getPlaceArray(@NonNull final int[] location) {
        // build list of names with their closest distance to location
        final StorageDelegator delegator = App.getDelegator();
        Map<String, Double> distancesByName = new HashMap<>();
        Log.d(DEBUG_PLACE_TAG, "searching for place ways...");
        processElementsForPlace(location, delegator.getCurrentStorage().getWays(), distancesByName);
        Log.d(DEBUG_PLACE_TAG, "searching for place nodes...");
        processElementsForPlace(location, delegator.getCurrentStorage().getNodes(), distancesByName);
        Log.d(DEBUG_PLACE_TAG, "searching for place relations...");
        processElementsForPlace(location, delegator.getCurrentStorage().getRelations(), distancesByName);
        // sort names by distance
        MultiHashMap<Double, String> retval = new MultiHashMap<>(true);
        for (Entry<String, Double> entry : distancesByName.entrySet()) {
            retval.add(entry.getValue(), entry.getKey());
        }
        return retval.getValues().toArray(new String[retval.getValues().size()]);
    }

    /**
     * Search through a list of OsmElements and find elements with a place tag
     * 
     * @param <T> The sub type of OsmElement being processed (currently
     * @param location the current location as a coordinate tupel in WGS84*1E7 degrees
     * @param elements the List of OsmElements
     * @param distancesByName a Map in which the found names are returned in
     */
    public <T extends OsmElement> void processElementsForPlace(final int[] location, List<T> elements, Map<String, Double> distancesByName) {
        String[] nameTags = { Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME };
        for (T e : elements) {
            if (e.getTagWithKey(Tags.KEY_PLACE) != null) {
                double distance = -1D;
                for (String tag : nameTags) {
                    String name = e.getTagWithKey(tag);
                    if (name != null) {
                        Log.d(DEBUG_PLACE_TAG, "adding " + name);
                        if (distance == -1D) { // only calc once
                            distance = e.getMinDistance(location);
                            if (distanceFilter && distance > MAX_DISTANCE) {
                                break;
                            }
                        }
                        if (distancesByName.containsKey(name)) {
                            // element already in list - keep shortest distance
                            if (distance < distancesByName.get(name)) {
                                distancesByName.put(name, distance);
                            }
                        } else {
                            distancesByName.put(name, distance);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get a distance sorted list of place names relative to the location given in the constructor
     * 
     * @return array containing the names
     */
    public String[] getPlaceNames() {
        if (placeNames == null) {
            placeNames = getPlaceArray(location);
        }
        return placeNames;
    }
}
