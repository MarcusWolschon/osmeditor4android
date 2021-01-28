package de.blau.android.propertyeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ElementSearch;
import de.blau.android.util.GeoContext;
import de.blau.android.util.GeoContext.CountryAndStateIso;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Geometry;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StreetPlaceNamesAdapter;
import de.blau.android.util.Util;

/**
 * Store coordinates and address information for use in address prediction
 * 
 * @author simon
 *
 */
public final class Address implements Serializable {
    private static final long serialVersionUID = 5L;

    private static final String DEBUG_TAG = Address.class.getSimpleName();

    public static final int NO_HYSTERESIS      = 0;
    public static final int DEFAULT_HYSTERESIS = 2;

    private static final String ADDRESS_TAGS_FILE   = "addresstags.dat";
    private static final int    MAX_SAVED_ADDRESSES = 100;

    private static final double MAX_LAST_ADDRESS_DISTANCE = 200D; // maximum distance in meters the last address can be
                                                                  // away to be used

    private static SavingHelper<LinkedList<Address>> savingHelperAddress = new SavingHelper<>();

    public enum Side {
        LEFT, RIGHT, UNKNOWN;

        /**
         * Return the other road side
         * 
         * @param side side we want to find the opposite
         * @return the other side
         */
        public static Side opposite(Side side) {
            switch (side) {
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            default:
                return UNKNOWN;
            }
        }
    }

    private Side                                side = Side.UNKNOWN;
    private float                               lat;
    private float                               lon;
    private LinkedHashMap<String, List<String>> tags;

    private static LinkedList<Address> lastAddresses = null;

    /**
     * Create empty address object
     */
    private Address() {
        tags = new LinkedHashMap<>();
    }

    /**
     * Create an address object from an OSM element
     * 
     * @param type type of element
     * @param id its ID
     * @param tags the relevant address tags
     */
    private Address(@NonNull String type, long id, @NonNull LinkedHashMap<String, List<String>> tags) {
        OsmElement e = App.getDelegator().getOsmElement(type, id);
        if (e == null) {
            Log.e(DEBUG_TAG, type + " " + id + " doesn't exist in storage");
            throw new IllegalStateException(type + " " + id + " doesn't exist in storage");
        }
        init(e, tags);
    }

    /**
     * Create an address object from an OSM element
     * 
     * @param e the OSM element
     * @param tags the relevant address tags
     */
    private Address(@NonNull OsmElement e, @NonNull LinkedHashMap<String, List<String>> tags) {
        init(e, tags);
    }

    /**
     * Initialize an address object from an OSM element
     * 
     * @param e the OSM element
     * @param tags the relevant address tags
     */
    private void init(@NonNull OsmElement e, @NonNull LinkedHashMap<String, List<String>> tags) {
        switch (e.getType()) {
        case NODE:
            lat = ((Node) e).getLat() / 1E7F;
            lon = ((Node) e).getLon() / 1E7F;
            break;
        case WAY:
        case CLOSEDWAY:
        case AREA:
            if (Way.NAME.equals(e.getName())) {
                double[] center = Geometry.centroidLonLat((Way) e);
                if (center != null) {
                    lat = (float) center[1];
                    lon = (float) center[0];
                }
            } else {
                // MP and maybe one day an area type
            }
            break;
        case RELATION:
            // doing nothing is probably best for now
        default:
            break;
        }
        this.tags = new LinkedHashMap<>(tags);
    }

    /**
     * Set which side of the road this address is on
     * 
     * @param wayId OSM ID
     */
    private void setSide(long wayId) {
        side = Side.UNKNOWN;
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, wayId);
        if (w == null) {
            return;
        }
        double distance = Double.MAX_VALUE;

        // to avoid rounding errors we translate the bb to 0,0
        BoundingBox bb = w.getBounds();
        double latOffset = GeoMath.latE7ToMercatorE7(bb.getBottom());
        double lonOffset = bb.getLeft();
        double ny = GeoMath.latToMercator(lat) - latOffset / 1E7D;
        double nx = lon - lonOffset / 1E7D;

        ArrayList<Node> nodes = new ArrayList<>(w.getNodes());
        for (int i = 0; i <= nodes.size() - 2; i++) {
            double bx = (nodes.get(i).getLon() - lonOffset) / 1E7D;
            double by = (GeoMath.latE7ToMercatorE7(nodes.get(i).getLat()) - latOffset) / 1E7D;
            double ax = (nodes.get(i + 1).getLon() - lonOffset) / 1E7D;
            double ay = (GeoMath.latE7ToMercatorE7(nodes.get(i + 1).getLat()) - latOffset) / 1E7D;
            float[] closest = GeoMath.closestPoint((float) nx, (float) ny, (float) bx, (float) by, (float) ax, (float) ay);
            double newDistance = GeoMath.haversineDistance(nx, ny, closest[0], closest[1]);
            if (newDistance < distance) {
                distance = newDistance;
                double determinant = (bx - ax) * (ny - ay) - (by - ay) * (nx - ax);
                if (determinant < 0) {
                    side = Side.LEFT;
                } else if (determinant > 0) {
                    side = Side.RIGHT;
                }
            }
        }
    }

    /**
     * Get the side of the street this address is on
     * 
     * @return a Side value
     */
    private Side getSide() {
        return side;
    }

    /**
     * Predict address tags
     * 
     * This uses a file to cache/save the address information over invocations, if the cache doesn't have entries for a
     * specific street/place an attempt to extract the information from the downloaded data is made
     *
     * @param context Android context
     * @param elementType element type (node, way, relation)
     * @param elementOsmId osm object id
     * @param es ElementSearch instance for finding street and place names
     * @param current current tags
     * @param maxRank determines how far away from the nearest street the last address street can be, 0 will always use
     *            the nearest, higher numbers will provide some hysteresis
     * @return map containing the predicted address tags
     */
    public static synchronized Map<String, List<String>> predictAddressTags(@NonNull Context context, @NonNull final String elementType,
            final long elementOsmId, @Nullable final ElementSearch es, @NonNull final Map<String, List<String>> current, int maxRank) {
        Address newAddress = null;

        loadLastAddresses(context);

        if (lastAddresses != null && !lastAddresses.isEmpty()) {
            Log.d(DEBUG_TAG, "initializing with last addresses");
            Address lastAddress = lastAddresses.get(0);
            try {
                newAddress = new Address(elementType, elementOsmId, lastAddress.tags); // last address we added
            } catch (IllegalStateException isex) {
                // handle this below
            }
            if (newAddress != null) {
                double distance = GeoMath.haversineDistance(newAddress.lon, newAddress.lat, lastAddress.lon, lastAddress.lat);
                if (distance > MAX_LAST_ADDRESS_DISTANCE) { // if the last address was too far away don't use its tags
                    // check if we have a better candidate
                    Address candidate = null;
                    double candidateDistance = MAX_LAST_ADDRESS_DISTANCE;
                    for (int i = 1; i < lastAddresses.size(); i++) {
                        Address a = lastAddresses.get(i);
                        double d = GeoMath.haversineDistance(newAddress.lon, newAddress.lat, a.lon, a.lat);
                        if (d < candidateDistance) {
                            candidate = a;
                            candidateDistance = d;
                        }
                    }
                    if (candidate != null) {
                        // better candidate found
                        newAddress.tags = new LinkedHashMap<>(candidate.tags);
                        Log.d(DEBUG_TAG, "better candidate found " + candidate);
                    } else {
                        // zap the tags from the last address
                        newAddress.tags = new LinkedHashMap<>();
                        Log.d(DEBUG_TAG, "no nearby addresses found");
                    }
                }
                //
                if (newAddress.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
                    newAddress.tags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.wrapInList(""));
                }
            }
        }

        if (newAddress == null) { // make sure we have the address object
            LinkedHashMap<String, List<String>> defaultTags = new LinkedHashMap<>();
            fillWithDefaultAddressTags(context, defaultTags);
            try {
                newAddress = new Address(elementType, elementOsmId, defaultTags);
                setCountryAndState(context, newAddress.lon, newAddress.lat, newAddress.tags);
                Log.d(DEBUG_TAG, "nothing to start with, creating new");
            } catch (IllegalStateException isex) {
                // this is fatal
                return defaultTags;
            }
        }
        // merge in any existing tags
        for (Entry<String, List<String>> entry : current.entrySet()) {
            Log.d(DEBUG_TAG, "Adding in existing tag " + entry.getKey());
            List<String> values = entry.getValue();
            if (listNotEmpty(values)) {
                newAddress.tags.put(entry.getKey(), values);
            }
        }

        boolean hasPlace = newAddress.tags.containsKey(Tags.KEY_ADDR_PLACE) && !newAddress.tags.containsKey(Tags.KEY_ADDR_STREET);
        // check if the object already has a number so that we don't overwrite it
        boolean hasNumber = listNotEmpty(current.get(Tags.KEY_ADDR_HOUSENUMBER));
        StorageDelegator storageDelegator = App.getDelegator();
        if (es != null) {
            // the arrays should now be calculated, retrieve street names if any
            List<String> streetNames = new ArrayList<>(Arrays.asList(es.getStreetNames()));
            if ((streetNames != null && !streetNames.isEmpty()) || hasPlace) {
                LinkedHashMap<String, List<String>> tags = newAddress.tags;
                Log.d(DEBUG_TAG, "tags.get(Tags.KEY_ADDR_STREET)) " + tags.get(Tags.KEY_ADDR_STREET));
                String street;
                if (!hasPlace) {
                    List<String> addrStreetValues = tags.get(Tags.KEY_ADDR_STREET);
                    int rank = -1;
                    boolean hasAddrStreet = addrStreetValues != null && !addrStreetValues.isEmpty() && !"".equals(addrStreetValues.get(0));
                    if (hasAddrStreet) {
                        rank = streetNames.indexOf(addrStreetValues.get(0)); // FIXME this and the following could
                                                                             // consider other values in multi select
                    }
                    Log.d(DEBUG_TAG, (hasAddrStreet ? "rank " + rank + " for " + addrStreetValues.get(0) : "no addrStreet tag"));
                    if (!hasAddrStreet || rank > maxRank || rank < 0) { // check if has street and still in the top 3
                                                                        // nearest
                        // nope -> zap
                        tags.put(Tags.KEY_ADDR_STREET, Util.wrapInList(streetNames.get(0)));
                    }
                    addrStreetValues = tags.get(Tags.KEY_ADDR_STREET);
                    if (addrStreetValues != null && !addrStreetValues.isEmpty()) {
                        street = tags.get(Tags.KEY_ADDR_STREET).get(0); // should now have the final suggestion for a
                                                                        // street
                    } else {
                        street = ""; // FIXME
                    }
                    try {
                        newAddress.setSide(es.getStreetId(street));
                    } catch (OsmException e) { // street not in adapter
                        newAddress.side = Side.UNKNOWN;
                    }
                } else { // ADDR_PLACE minimal support, don't overwrite with street
                    List<String> addrPlaceValues = tags.get(Tags.KEY_ADDR_PLACE);
                    if (addrPlaceValues != null && !addrPlaceValues.isEmpty()) {
                        street = tags.get(Tags.KEY_ADDR_PLACE).get(0);
                    } else {
                        street = ""; // FIXME
                    }
                    newAddress.side = Side.UNKNOWN;
                }
                Log.d(DEBUG_TAG, "side " + newAddress.getSide());
                Side side = newAddress.getSide();
                // find the addresses corresponding to the current street
                if (!hasNumber && street != null && lastAddresses != null) {
                    SortedMap<Integer, Address> list = getHouseNumbers(street, side, lastAddresses);
                    if (list.size() == 0) { // try to seed lastAddresses from OSM data
                        try {
                            Log.d(DEBUG_TAG, "Seeding from street " + street);
                            long streetId = -1;
                            if (!hasPlace) {
                                streetId = es.getStreetId(street);
                            }
                            // nodes
                            for (Node n : storageDelegator.getCurrentStorage().getNodes()) {
                                seedAddressList(context, street, streetId, n, lastAddresses);
                            }
                            // ways
                            for (Way w : storageDelegator.getCurrentStorage().getWays()) {
                                seedAddressList(context, street, streetId, w, lastAddresses);
                            }
                            // and try again
                            list = getHouseNumbers(street, side, lastAddresses);
                        } catch (OsmException e) {
                            Log.d(DEBUG_TAG, "predictAddressTags got " + e.getMessage());
                        }
                    }
                    newAddress.tags = predictNumber(newAddress, tags, street, side, list, false, null);
                }
            } else { // last ditch attempt
                fillWithDefaultAddressTags(context, newAddress.tags);
            }
        }

        // if this is a node on a building outline, we add entrance=yes if it doesn't already exist
        if (elementType.equals(Node.NAME)) {
            boolean isOnBuilding = false;
            // we can't call wayForNodes here because Logic may not be around
            Node node = (Node) storageDelegator.getOsmElement(Node.NAME, elementOsmId);
            if (node != null) { // null shouldn't happen
                for (Way w : storageDelegator.getCurrentStorage().getWays(node)) {
                    if (w.hasTagKey(Tags.KEY_BUILDING)) {
                        isOnBuilding = true;
                    } else if (w.getParentRelations() != null) { // need to check relations too
                        for (Relation r : w.getParentRelations()) {
                            if (r.hasTagKey(Tags.KEY_BUILDING) || r.hasTag(Tags.KEY_TYPE, Tags.VALUE_BUILDING)) {
                                isOnBuilding = true;
                                break;
                            }
                        }
                    }
                    if (isOnBuilding) {
                        break;
                    }
                }
                if (isOnBuilding && !newAddress.tags.containsKey(Tags.KEY_ENTRANCE)) {
                    newAddress.tags.put(Tags.KEY_ENTRANCE, Util.wrapInList(Tags.VALUE_YES));
                }
            } else {
                Log.e(DEBUG_TAG, "Node " + elementOsmId + " is null");
            }
        }
        return newAddress.tags;
    }

    /**
     * Fill tags with the default address tags
     * 
     * @param context an Android Context
     * @param tags the map for the tags
     */
    private static void fillWithDefaultAddressTags(@NonNull Context context, @NonNull LinkedHashMap<String, List<String>> tags) {
        Preferences prefs = new Preferences(context);
        Set<String> addressTags = prefs.addressTags();
        for (String key : addressTags) {
            tags.put(key, Util.wrapInList(""));
        }
    }

    /**
     * If the tags contain country or state tags that are empty, fill them in
     * 
     * @param context an Android Context
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param tags the tags
     */
    private static void setCountryAndState(@NonNull Context context, double lon, double lat, @NonNull LinkedHashMap<String, List<String>> tags) {
        final boolean hasCountry = tags.containsKey(Tags.KEY_ADDR_COUNTRY);
        final boolean hasState = tags.containsKey(Tags.KEY_ADDR_STATE);
        if (hasCountry || hasState) {
            try {
                GeoContext geoContext = App.getGeoContext(context);
                if (geoContext != null) {
                    CountryAndStateIso casi = geoContext.getCountryAndStateIso(lon, lat);
                    if (casi != null) {
                        if (hasCountry && !listNotEmpty(tags.get(Tags.KEY_ADDR_COUNTRY))) {
                            tags.put(Tags.KEY_ADDR_COUNTRY, Util.wrapInList(casi.getCountry()));
                        }
                        if (hasState && casi.getState() != null && !listNotEmpty(tags.get(Tags.KEY_ADDR_STATE))) {
                            // note this assumes that the ISO code actually makes sense here
                            tags.put(Tags.KEY_ADDR_STATE, Util.wrapInList(casi.getState()));
                        }
                    }
                }
            } catch (IllegalArgumentException iaex) {
                Log.e(DEBUG_TAG, "setCountryAndState " + iaex);
            }
        }
    }

    /**
     * Check if a List of Strings has at least one non-empty String entry
     * 
     * @param values the List
     * @return true if a non empty String was found
     */
    private static boolean listNotEmpty(@Nullable List<String> values) {
        if (values != null && !values.isEmpty()) {
            for (String v : values) {
                if (!"".equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Try to predict the next number - get all existing numbers for the side of the street we are on - determine if the
     * increment per number is 1 or 2 (for now everything else is ignored) - determine the nearest address node - if it
     * is the last or first node and we are at one side use that and add or subtract the increment - if the nearest node
     * is somewhere in the middle determine on which side of it we are, - inc/dec in that direction If everything works
     * out correctly even if a prediction is wrong, entering the correct number should improve the next prediction
     * 
     * TODO the above assumes that the road is not doubling back or similar, aka that the addresses are more or less in
     * a straight line, use the length along the way defined by the addresses instead
     * 
     * @param newAddress the address object for the new address
     * @param originalTags tags for this object
     * @param street the street name
     * @param side side which we are on
     * @param list list of existing addresses on this side
     * @param oppositeSide try to predict a number for the other side of the street
     * @param otherSideList numbers found on the other side only used if otherSide is true
     * @return the tags for the object
     */
    @NonNull
    private static LinkedHashMap<String, List<String>> predictNumber(@NonNull Address newAddress, @NonNull LinkedHashMap<String, List<String>> originalTags,
            @NonNull String street, @NonNull Side side, @NonNull SortedMap<Integer, Address> list, boolean oppositeSide,
            @Nullable SortedMap<Integer, Address> otherSideList) {
        LinkedHashMap<String, List<String>> newTags = new LinkedHashMap<>(originalTags);
        if (list.size() >= 2) {
            try {
                //
                // determine increment
                //
                int inc = 1;
                float incTotal = 0;
                float incCount = 0;
                List<Integer> numbers = new ArrayList<>(list.keySet());
                for (int i = 0; i < numbers.size() - 1; i++) {
                    int diff = numbers.get(i + 1) - numbers.get(i);
                    if (diff > 0 && diff <= 2) {
                        incTotal = incTotal + diff;
                        incCount++;
                    }
                }
                inc = Math.round(incTotal / incCount);

                int firstNumber = list.firstKey();
                int lastNumber = list.lastKey();

                //
                // find the most appropriate next address
                //
                int nearest = -1;
                int prev = -1;
                int post = -1;
                double distanceFirst = 0;
                double distanceLast = 0;
                double distance = Double.MAX_VALUE;
                for (int i = 0; i < numbers.size(); i++) {
                    // determine the nearest existing address
                    int number = numbers.get(i);
                    Address a = list.get(number);
                    double newDistance = GeoMath.haversineDistance(newAddress.lon, newAddress.lat, a.lon, a.lat);
                    if (newDistance <= distance) {
                        // if distance is the same replace with values for the
                        // current number which will be larger
                        distance = newDistance;
                        nearest = number;
                        prev = numbers.get(Math.max(0, i - 1));
                        post = numbers.get(Math.min(numbers.size() - 1, i + 1));
                    }
                    if (i == 0) {
                        distanceFirst = newDistance;
                    } else if (i == numbers.size() - 1) {
                        distanceLast = newDistance;
                    }
                }
                //
                double distanceTotal = GeoMath.haversineDistance(list.get(firstNumber).lon, list.get(firstNumber).lat, list.get(lastNumber).lon,
                        list.get(lastNumber).lat);
                if (nearest == firstNumber) {
                    if (distanceLast > distanceTotal) {
                        inc = -inc;
                    }
                } else if (nearest == lastNumber) {
                    if (distanceFirst < distanceTotal) {
                        inc = -inc;
                    }
                } else {
                    double distanceNearestFirst = GeoMath.haversineDistance(list.get(firstNumber).lon, list.get(firstNumber).lat, list.get(nearest).lon,
                            list.get(nearest).lat);
                    if (distanceFirst < distanceNearestFirst) {
                        inc = -inc;
                    } // else already correct
                }
                // first apply tags from nearest address if they don't already exist
                copyTags(list.get(nearest), newTags);

                if (oppositeSide) { // try to predict address on the other road side
                    Log.d(DEBUG_TAG, "Predicting for other side inc=" + inc + " nearest " + nearest);
                    if (Math.abs(inc) > 1) {
                        int newNumber = Math.max(1, otherSideList.size() == 0 ? nearest + (inc / Math.abs(inc)) : otherSideList.firstKey() + inc);
                        Log.d(DEBUG_TAG, "final predicted result for the other side " + newNumber);
                        newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.wrapInList(Integer.toString(newNumber)));
                    } else { // no sense to guess pattern
                        Log.d(DEBUG_TAG, "giving up");
                        newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.wrapInList(""));
                    }
                } else { // predict on this side
                    int newNumber = Math.max(1, nearest + inc);
                    Log.d(DEBUG_TAG, "Predicted " + newNumber + " first " + firstNumber + " last " + lastNumber + " nearest " + nearest + " inc " + inc
                            + " prev " + prev + " post " + post + " side " + side);
                    if (numbers.contains(newNumber)) {
                        // try one inc more and one less, if they both fail use the original number
                        if (!numbers.contains(Math.max(1, newNumber + inc))) {
                            newNumber = Math.max(1, newNumber + inc);
                        } else if (!numbers.contains(Math.max(1, newNumber - inc))) {
                            newNumber = Math.max(1, newNumber - inc);
                        }
                    }
                    Log.d(DEBUG_TAG, "final predicted result " + newNumber);
                    newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.wrapInList(Integer.toString(newNumber)));
                }
            } catch (NumberFormatException nfe) {
                Log.d(DEBUG_TAG, "exception " + nfe);
                newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.wrapInList(""));
            }
        } else if (list.size() == 1) {
            Log.d(DEBUG_TAG, "only one number on this side");
            // can't do prediction with only one value
            // apply tags from sole existing address if they don't already exist
            SortedMap<Integer, Address> otherList = getHouseNumbers(street, Side.opposite(side), lastAddresses);
            if (otherList.size() >= 2) {
                newTags = predictNumber(newAddress, originalTags, street, side, otherList, true, list);
            } else {
                copyTags(list.get(list.firstKey()), newTags);
            }
        } else if (list.size() == 0) {
            Log.d(DEBUG_TAG, "no numbers on this side");
            SortedMap<Integer, Address> otherList = getHouseNumbers(street, Side.opposite(side), lastAddresses);
            if (otherList.size() >= 2) {
                newTags = predictNumber(newAddress, originalTags, street, side, otherList, true, list);
            } else if (otherList.size() == 1) {
                copyTags(otherList.get(otherList.firstKey()), newTags);
            } else {
                newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.wrapInList(""));
            }
        }
        return newTags;
    }

    /**
     * Copy tags from an Address to an existing Map holding tags
     * 
     * @param address the Address object
     * @param tags the Map with tags
     */
    private static void copyTags(@Nullable Address address, @NonNull LinkedHashMap<String, List<String>> tags) {
        if (address != null) {
            for (Entry<String, List<String>> entry : address.tags.entrySet()) {
                String key = entry.getKey();
                if (!tags.containsKey(key)) {
                    tags.put(key, entry.getValue());
                }
            }
        } else {
            Log.e(DEBUG_TAG, "address shoudn't be null");
            // maybe a crash dump would be a good idea
        }
    }

    /**
     * Try to extract an integer number from a string
     * 
     * Simply ignores any non-digits
     * 
     * @param hn input string
     * @return an integer
     * @throws NumberFormatException if what was extracted could not be parsed as an int
     */
    private static int getNumber(@NonNull String hn) throws NumberFormatException {
        StringBuilder sb = new StringBuilder();
        for (Character c : hn.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        if ("".equals(sb.toString())) {
            return 0;
        } else {
            return Integer.parseInt(sb.toString());
        }
    }

    /**
     * Return a sorted map of house numbers and the associated address objects from a List of Addresses
     * 
     * The original addr:housenumber tag is split on ",", ";" and "-"
     * 
     * @param street the street name
     * @param side side of the street that should be considered
     * @param addresses the list of addresses
     * @return a sorted map with the house numbers as key
     */
    @NonNull
    private static synchronized SortedMap<Integer, Address> getHouseNumbers(@Nullable String street, @Nullable Address.Side side,
            @NonNull LinkedList<Address> addresses) {
        SortedMap<Integer, Address> result = new TreeMap<>(); // list sorted by house numbers
        for (Address a : addresses) {
            if (a != null && a.tags != null) {
                List<String> addrStreetValues = a.tags.get(Tags.KEY_ADDR_STREET);
                List<String> addrPlaceValues = a.tags.get(Tags.KEY_ADDR_PLACE);
                if (((addrStreetValues != null && !addrStreetValues.isEmpty() && addrStreetValues.get(0).equals(street)) // FIXME
                        || (addrPlaceValues != null && !addrPlaceValues.isEmpty() && addrPlaceValues.get(0).equals(street)))
                        && a.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER) && a.getSide() == side) {
                    Log.d(DEBUG_TAG, "Number " + a.tags.get(Tags.KEY_ADDR_HOUSENUMBER));
                    List<String> addrHousenumberValues = a.tags.get(Tags.KEY_ADDR_HOUSENUMBER);
                    if (addrHousenumberValues != null && !addrHousenumberValues.isEmpty()) {
                        String[] numbers = addrHousenumberValues.get(0).split("[\\,;\\-]");
                        for (String n : numbers) {
                            Log.d(DEBUG_TAG, "add number  " + n);
                            // noinspection EmptyCatchBlock
                            try {
                                result.put(getNumber(n), a);
                            } catch (NumberFormatException nfe) {
                                // empty
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Add an address from OSM data to the address cache
     * 
     * @param context Android Context
     * @param street the street name
     * @param streetId the osm id of the street
     * @param e the OsmElement
     * @param addresses the list of addresses
     */
    private static void seedAddressList(@NonNull Context context, @NonNull String street, long streetId, @NonNull OsmElement e,
            @NonNull LinkedList<Address> addresses) {
        if (e.hasTag(Tags.KEY_ADDR_STREET, street) && e.hasTagKey(Tags.KEY_ADDR_HOUSENUMBER)) {
            Address seed = new Address(e, getAddressTags(context, new LinkedHashMap<>(Util.getListMap(e.getTags()))));
            if (streetId > 0) {
                seed.setSide(streetId);
            }
            if (addresses.size() >= MAX_SAVED_ADDRESSES) { // arbitrary limit for now
                addresses.removeLast();
            }
            addresses.addFirst(seed);
            Log.d("TagEditor", "seedAddressList added " + seed.tags.toString());
        }
    }

    /**
     * Retrieve address tags from a map of tags taking the address preferences in to account
     * 
     * @param context Android Context
     * @param sortedMap LinkedHashMap containing the tags
     * @return a LinkedHashMap containing only the relevant address tags
     */
    private static LinkedHashMap<String, List<String>> getAddressTags(@NonNull Context context, @NonNull LinkedHashMap<String, List<String>> sortedMap) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        Preferences prefs = new Preferences(context);
        Set<String> addressTags = prefs.addressTags();
        for (Entry<String, List<String>> entry : sortedMap.entrySet()) {
            // include everything except interpolation related tags
            final String key = entry.getKey();
            if (addressTags.contains(key)) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    /**
     * Flush the cache both in memory and on disk
     * 
     * @param context Android context
     */
    public static synchronized void resetLastAddresses(@NonNull Context context) {
        savingHelperAddress.save(context, ADDRESS_TAGS_FILE, new LinkedList<>(), false);
        lastAddresses = null;
    }

    /**
     * Update and save any address tags to persistent storage
     * 
     * @param caller calling TagEditorFragment
     * @param tags current tags
     */
    public static synchronized void updateLastAddresses(@NonNull Context context, @Nullable StreetPlaceNamesAdapter streetAdapter, @NonNull String type,
            long id, @NonNull LinkedHashMap<String, List<String>> tags, boolean save) {
        LinkedHashMap<String, List<String>> addressTags = getAddressTags(context, tags);
        // this needs to be done after the edit again in case the street name or whatever has changed
        if (addressTags.size() > 0) {
            if (lastAddresses == null) {
                lastAddresses = new LinkedList<>();
            }
            if (lastAddresses.size() >= MAX_SAVED_ADDRESSES) { // arbitrary limit for now
                lastAddresses.removeLast();
            }
            try {
                Address current = new Address(type, id, addressTags);
                if (streetAdapter != null) {
                    List<String> values = addressTags.get(Tags.KEY_ADDR_STREET);
                    String streetName = values != null && !values.isEmpty() ? values.get(0) : null;
                    if (streetName != null) {
                        try {
                            current.setSide(streetAdapter.getStreetId(streetName));
                        } catch (OsmException e) {
                            current.side = Side.UNKNOWN;
                        }
                    }
                }
                lastAddresses.addFirst(current);
            } catch (IllegalStateException isex) {
                Log.e(DEBUG_TAG, "updateLastAddresses " + isex.getMessage());
            }
            if (save) {
                saveLastAddresses(context);
            }
        }
    }

    /**
     * Save the address list to persistent storage
     * 
     * @param context Android Context
     */
    protected static synchronized void saveLastAddresses(@NonNull Context context) {
        if (lastAddresses != null) {
            savingHelperAddress.save(context, ADDRESS_TAGS_FILE, lastAddresses, false);
        }
    }

    /**
     * Read the address list from persistent storage
     * 
     * @param context Android Context
     */
    static synchronized void loadLastAddresses(@NonNull Context context) {
        if (lastAddresses == null) {
            try {
                lastAddresses = savingHelperAddress.load(context, ADDRESS_TAGS_FILE, false);
                Log.d("TagEditor", "onResume read " + lastAddresses.size() + " addresses");
            } catch (Exception e) {
                // never crash
            }
        }
    }

    /**
     * Convert a multi-valued Map to one with single values
     * 
     * @param multi a Map<String, List<String>>
     * @return a Map<String, String>
     */
    public static Map<String, String> multiValueToSingle(@NonNull Map<String, List<String>> multi) {
        Map<String, String> simple = new TreeMap<>();
        for (Entry<String, List<String>> entry : multi.entrySet()) {
            simple.put(entry.getKey(), entry.getValue().get(0));
        }
        return simple;
    }
}
