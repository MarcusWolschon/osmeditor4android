package io.vespucci.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.App;
import io.vespucci.exception.OsmException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Storage;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Tags;
import io.vespucci.osm.Way;
import io.vespucci.util.collections.MultiHashMap;

public class ElementSearch {

    private static final String DEBUG_PLACE_TAG  = "PlaceTagValues...";
    private static final String DEBUG_STREET_TAG = "StreetTagValues...";
    // this is just a very rough number to stop including stuff that is very far away
    private static final double   MAX_DISTANCE     = 200D;
    private static final String[] STREET_NAME_TAGS = { Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME, Tags.KEY_NAME_LEFT, Tags.KEY_NAME_RIGHT };
    private static final String[] PLACE_NAME_TAGS  = { Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME };

    private String[]          streetNames      = null;
    private Map<String, Long> idsByStreetNames = new HashMap<>();

    private String[] placeNames = null;

    private final IntCoordinates location;
    private final boolean        distanceFilter;

    /**
     * Search for OSM elements around a location
     * 
     * @param location location coordinates in WGS84°*1E7
     * @param distanceFilter if true ignore objects further away than MAX_DISTANCE
     */
    public ElementSearch(@NonNull final IntCoordinates location, boolean distanceFilter) {
        this.location = location;
        this.distanceFilter = distanceFilter;
    }

    /**
     * Get all distance sorted street-names in the area
     * 
     * @param location position we want the names nearby to
     * @return all street-names
     */
    private String[] getStreetArray(@NonNull final IntCoordinates location) {
        // build list of names with their closest distance to location
        final StorageDelegator delegator = App.getDelegator();
        Map<String, Double> distancesByNames = new HashMap<>();
        List<Way> ways;
        try {
            ways = distanceFilter ? delegator.getCurrentStorage().getWays(GeoMath.createBoundingBoxForCoordinates(location, MAX_DISTANCE))
                    : delegator.getCurrentStorage().getWays();
        } catch (OsmException e) {
            Log.e(DEBUG_STREET_TAG, "BoundingBox caclulation failed with " + e.getMessage());
            ways = delegator.getCurrentStorage().getWays();
        }
        for (Way way : ways) {
            if (way.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
                long iD = way.getOsmId();
                for (String tag : STREET_NAME_TAGS) {
                    String name = way.getTagWithKey(tag);
                    if (name != null) {
                        int[] l = new int[] { location.lon, location.lat };
                        double distance = way.getMinDistance(l);
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
        return retval.getValues().toArray(new String[0]);
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
    private String[] getPlaceArray(@NonNull final IntCoordinates location) {
        // build list of names with their closest distance to location
        Map<String, Double> distancesByName = new HashMap<>();
        try {
            final BoundingBox box = GeoMath.createBoundingBoxForCoordinates(location, MAX_DISTANCE);
            Log.d(DEBUG_PLACE_TAG, "searching for place ways...");
            final Storage currentStorage = App.getDelegator().getCurrentStorage();
            List<Way> ways = distanceFilter ? currentStorage.getWays(box) : currentStorage.getWays();
            List<Node> nodes = distanceFilter ? currentStorage.getNodes(box) : currentStorage.getNodes();
            processElementsForPlace(location, ways, distancesByName);
            Log.d(DEBUG_PLACE_TAG, "searching for place nodes...");
            processElementsForPlace(location, nodes, distancesByName);
            Log.d(DEBUG_PLACE_TAG, "searching for place relations...");
            processElementsForPlace(location, currentStorage.getRelations(), distancesByName);
        } catch (OsmException oex) {
            Log.e(DEBUG_PLACE_TAG, "BoundingBox caclulation failed with " + oex.getMessage());
        }
        // sort names by distance
        MultiHashMap<Double, String> retval = new MultiHashMap<>(true);
        for (Entry<String, Double> entry : distancesByName.entrySet()) {
            retval.add(entry.getValue(), entry.getKey());
        }
        return retval.getValues().toArray(new String[0]);
    }

    /**
     * Search through a list of OsmElements and find elements with a place tag
     * 
     * @param <T> The sub type of OsmElement being processed (currently
     * @param location the current location as a coordinate tupel in WGS84*1E7 degrees
     * @param elements the List of OsmElements
     * @param distancesByName a Map in which the found names are returned in
     */
    private <T extends OsmElement> void processElementsForPlace(final IntCoordinates location, List<T> elements, Map<String, Double> distancesByName) {
        for (T e : elements) {
            if (e.getTagWithKey(Tags.KEY_PLACE) != null) {
                double distance = -1D;
                for (String tag : PLACE_NAME_TAGS) {
                    String name = e.getTagWithKey(tag);
                    if (name != null) {
                        Log.d(DEBUG_PLACE_TAG, "adding " + name);
                        if (distanceFilter) {
                            if (distance == -1D) { // only calc once
                                distance = e.getMinDistance(new int[] { location.lon, location.lat });
                            }
                            if (distance > MAX_DISTANCE) {
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
