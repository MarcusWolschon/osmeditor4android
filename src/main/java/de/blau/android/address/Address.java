package de.blau.android.address;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ElementSearch;
import de.blau.android.util.GeoContext;
import de.blau.android.util.GeoContext.CountryAndStateIso;
import de.blau.android.util.GeoContext.Properties;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Geometry;
import de.blau.android.util.IntCoordinates;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StreetPlaceNamesAdapter;
import de.blau.android.util.Util;
import de.blau.android.util.collections.LongOsmElementMap;

/**
 * Store coordinates and address information for use in address prediction
 * 
 * @author simon
 *
 */
public final class Address implements Serializable {

    private static final long serialVersionUID = 6L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Address.class.getSimpleName().length());
    private static final String DEBUG_TAG = Address.class.getSimpleName().substring(0, TAG_LEN);

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

    private Side                side     = Side.UNKNOWN;
    private long                streetId = 0;           // note: 0 indicates place
    private float               lat;
    private float               lon;
    private Map<String, String> tags;

    /**
     * 
     */
    private static LinkedList<Address> lastAddresses = null;
    private static GeoContext          geoContext    = null;

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
    private Address(@NonNull String type, long id, @Nullable Map<String, String> tags) {
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
    private Address(@NonNull OsmElement e, @Nullable Map<String, String> tags) {
        init(e, tags);
    }

    /**
     * Initialize an address object from an OSM element
     * 
     * @param e the OSM element
     * @param tags the relevant address tags
     */
    private void init(@NonNull OsmElement e, @Nullable Map<String, String> tags) {
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
                if (center.length == 2) {
                    lat = (float) center[1];
                    lon = (float) center[0];
                }
            } else {
                getRelationCenter(e);
            }
            break;
        case RELATION:
            getRelationCenter(e);
            break;
        default:
            break;
        }
        this.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
    }

    /**
     * Get a (very) rough center location for a MP
     * 
     * @param e the OsmElement
     */
    void getRelationCenter(@NonNull OsmElement e) {
        if (e.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON)) {
            BoundingBox bbox = e.getBounds();
            if (bbox != null) {
                ViewBox box = new ViewBox(bbox);
                double[] center = box.getCenter();
                lat = (float) center[1];
                lon = (float) center[0];
            }
        } else {
            Log.w(DEBUG_TAG, "Unexpected element " + e.getDescription(true));
        }
    }

    /**
     * Set which side of the road this address is on
     * 
     * @param wayId OSM ID
     */
    private void setSide(long wayId) {
        streetId = wayId;
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

        Node[] nodes = w.getNodes().toArray(new Node[0]);
        double bx = (nodes[0].getLon() - lonOffset) / 1E7D;
        double by = (GeoMath.latE7ToMercatorE7(nodes[0].getLat()) - latOffset) / 1E7D;
        for (int i = 0; i <= nodes.length - 2; i++) {
            double ax = (nodes[i + 1].getLon() - lonOffset) / 1E7D;
            double ay = (GeoMath.latE7ToMercatorE7(nodes[i + 1].getLat()) - latOffset) / 1E7D;
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
            bx = ax;
            by = ay;
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
     * @param current current tags potentially from a multi-select
     * @param maxRank determines how far away from the nearest street the last address street can be, 0 will always use
     *            the nearest, higher numbers will provide some hysteresis
     * @return map containing the predicted address tags, tags with empty values removed
     */
    public static synchronized Map<String, List<String>> predictAddressTags(@NonNull Context context, @NonNull final String elementType,
            final long elementOsmId, @Nullable final ElementSearch es, @NonNull final Map<String, List<String>> current, int maxRank) {
        return predictAddressTags(context, elementType, elementOsmId, es, current, maxRank, false);
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
     * @param current current tags potentially from a multi-select
     * @param maxRank determines how far away from the nearest street the last address street can be, 0 will always use
     *            the nearest, higher numbers will provide some hysteresis
     * @param keepEmpty keep empty tags
     * @return map containing the predicted address tags
     */
    public static synchronized Map<String, List<String>> predictAddressTags(@NonNull Context context, @NonNull final String elementType,
            final long elementOsmId, @Nullable final ElementSearch es, @NonNull final Map<String, List<String>> current, int maxRank, boolean keepEmpty) {
        getGeoContext(context);
        Address newAddress = null;

        loadLastAddresses(context);
        if (lastAddresses == null) {
            lastAddresses = new LinkedList<>();
        }

        if (!lastAddresses.isEmpty()) {
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
                    // check if we have a better (that is nearer) candidate
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
                    newAddress.tags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
                }
            }
        }

        if (newAddress == null) { // make sure we have an address object
            try {
                newAddress = new Address(elementType, elementOsmId, null);
                LinkedHashMap<String, String> defaultTags = new LinkedHashMap<>();
                fillWithDefaultAddressTags(context, newAddress.lon, newAddress.lat, defaultTags);
                setCountryAndState(newAddress.lon, newAddress.lat, newAddress.tags);
                newAddress.setTags(defaultTags);
                Log.d(DEBUG_TAG, "nothing to start with, creating new");
            } catch (IllegalStateException isex) {
                // this is fatal
                Log.e(DEBUG_TAG, "Aborting with " + isex.getMessage());
                return current;
            }
        }
        // merge in any existing address tags
        for (Entry<String, List<String>> entry : current.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if ((key.startsWith(Tags.KEY_ADDR_BASE) || Tags.KEY_ENTRANCE.equals(key)) && listNotEmpty(values)) {
                Log.d(DEBUG_TAG, "Adding in existing address tag " + key);
                newAddress.tags.put(key, values.get(0));
            }
        }
        Map<String, String> tags = newAddress.tags;
        String addrPlaceValue = tags.get(Tags.KEY_ADDR_PLACE);
        String addrStreetValue = tags.get(Tags.KEY_ADDR_STREET);
        boolean hasStreet = !isEmpty(addrStreetValue);
        boolean hasPlace = !isEmpty(addrPlaceValue) && !hasStreet;
        StorageDelegator storageDelegator = App.getDelegator();
        if (es != null) {
            // the arrays should now be calculated, retrieve street names if any
            List<String> streetNames = new ArrayList<>(Arrays.asList(es.getStreetNames()));
            if ((streetNames != null && !streetNames.isEmpty()) || hasPlace) {
                String street;
                if (!hasPlace) {
                    Log.d(DEBUG_TAG, "tags.get(Tags.KEY_ADDR_STREET)) " + addrStreetValue);
                    int rank = -1;
                    if (hasStreet) {
                        rank = streetNames.indexOf(addrStreetValue);
                    }
                    Log.d(DEBUG_TAG, (hasStreet ? "rank " + rank + " for " + addrStreetValue : "no addr:street tag"));
                    // check if has street and still in the top 3 nearest
                    if (!hasStreet || rank > maxRank || rank < 0) {
                        // nope -> get nearest street
                        final String tempStreet = streetNames.get(0);
                        tags.put(Tags.KEY_ADDR_STREET, tempStreet);
                        addrStreetValue = tempStreet;
                        Log.d(DEBUG_TAG, "Using nearest street " + addrStreetValue);
                    }
                    street = addrStreetValue != null ? addrStreetValue : "";
                    // should now have the final suggestion for a street
                    try {
                        newAddress.setSide(es.getStreetId(street));
                    } catch (OsmException e) { // street not in adapter
                        newAddress.side = Side.UNKNOWN;
                    }
                } else { // ADDR_PLACE minimal support, don't overwrite with street
                    street = addrPlaceValue != null ? addrPlaceValue : "";
                    newAddress.side = Side.UNKNOWN;
                }
                Log.d(DEBUG_TAG, "side " + newAddress.getSide());
                Side side = newAddress.getSide();
                // find the addresses corresponding to the current street
                // check if the object already has a number so that we don't overwrite it
                String houseNumberValue = newAddress.tags.get(Tags.KEY_ADDR_HOUSENUMBER);
                if (isEmpty(houseNumberValue) && street != null) {
                    try {
                        long streetId = hasPlace ? 0L : es.getStreetId(street);
                        SortedMap<Integer, Address> houseNumbers = getHouseNumbers(street, streetId, side, lastAddresses);
                        if (houseNumbers.size() == 0) { // try to seed lastAddresses from OSM data
                            Log.d(DEBUG_TAG, "Seeding from street " + street);
                            // nodes
                            for (Node n : storageDelegator.getCurrentStorage().getNodes()) {
                                seedAddressList(context, street, n, lastAddresses);
                            }
                            // ways
                            for (Way w : storageDelegator.getCurrentStorage().getWays()) {
                                seedAddressList(context, street, w, lastAddresses);
                            }
                            // and try again
                            houseNumbers = getHouseNumbers(street, streetId, side, lastAddresses);
                        }
                        newAddress.tags = predictNumber(context, newAddress, tags, street, streetId, side, houseNumbers, null);
                    } catch (OsmException e) {
                        Log.d(DEBUG_TAG, "predictAddressTags got " + e.getMessage());
                    }
                }
            }
        }

        fillWithDefaultAddressTags(context, newAddress.lon, newAddress.lat, newAddress.tags);
        setCountryAndState(newAddress.lon, newAddress.lat, newAddress.tags);

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
                    newAddress.tags.put(Tags.KEY_ENTRANCE, Tags.VALUE_YES);
                }
            } else {
                Log.e(DEBUG_TAG, "Node " + elementOsmId + " is null");
            }
        }

        // merge address tags back
        for (Entry<String, String> entry : newAddress.tags.entrySet()) {
            String value = entry.getValue();
            if (keepEmpty || !"".equals(value)) {
                current.put(entry.getKey(), Util.wrapInList(value));
            }
        }
        return current;
    }

    /**
     * Check that a String is either null or empty
     * 
     * @param s the String
     * @return true if the String doesn't have a non-empty value
     */
    private static boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    /**
     * Get a GeoContext object
     * 
     * @param context an Android Context
     */
    static void getGeoContext(@NonNull Context context) {
        if (geoContext == null) {
            geoContext = App.getGeoContext(context);
        }
    }

    /**
     * Fill tags with the default address tags, if not present
     * 
     * @param context an Android Context
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param tags the map for the tags
     */
    private static void fillWithDefaultAddressTags(@NonNull Context context, double lon, double lat, @NonNull Map<String, String> tags) {
        Set<String> addressTags = getAddressKeys(context, lon, lat);
        boolean hasStreet = hasTagWithValue(tags, Tags.KEY_ADDR_STREET);
        boolean hasPlace = hasTagWithValue(tags, Tags.KEY_ADDR_PLACE);
        boolean hasNumber = hasTagWithValue(tags, Tags.KEY_ADDR_HOUSENUMBER);
        boolean hasName = hasTagWithValue(tags, Tags.KEY_ADDR_HOUSENAME);
        for (String key : addressTags) {
            // addr:place and addr:street are mutually exclusive
            // addr:housename and addr:housenumber maybe not, but it is clearly less confusing
            if ((Tags.KEY_ADDR_PLACE.equals(key) && hasStreet) || (Tags.KEY_ADDR_STREET.equals(key) && hasPlace)
                    || (Tags.KEY_ADDR_HOUSENUMBER.equals(key) && hasName) || (Tags.KEY_ADDR_HOUSENAME.equals(key) && hasNumber)) {
                continue;
            }
            if (!tags.containsKey(key)) { // NOSONAR
                tags.put(key, "");
            }
        }
    }

    /**
     * Check for a non-empty value for a key
     * 
     * @param tags the tags
     * @param key the key
     * @return true if tags has a non-empty mapping for key
     */
    private static boolean hasTagWithValue(@NonNull Map<String, String> tags, @NonNull String key) {
        String value = tags.get(key);
        return value != null && !"".equals(value);
    }

    /**
     * Get our default address keys from the preferences of from geocontext
     * 
     * @param context an Android Context
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @return a Set of the address keys
     */
    public static Set<String> getAddressKeys(@NonNull Context context, double lon, double lat) {
        getGeoContext(context);
        Preferences prefs = App.getPreferences(context);
        Properties prop = null;
        if (geoContext != null) {
            prop = geoContext.getProperties(geoContext.getIsoCodes(lon, lat));
        }
        return prop == null || prop.getAddressKeys() == null || prefs.overrideCountryAddressTags() ? prefs.addressTags()
                : new HashSet<>(Arrays.asList(prop.getAddressKeys()));
    }

    /**
     * If the tags contain country or state tags that are empty, fill them in
     * 
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param tags the tags
     */
    private static void setCountryAndState(double lon, double lat, @NonNull Map<String, String> tags) {
        final String country = tags.get(Tags.KEY_ADDR_COUNTRY);
        final String state = tags.get(Tags.KEY_ADDR_STATE);
        if (country == null && state == null) {
            return;
        }
        try {
            if (geoContext != null) {
                CountryAndStateIso casi = geoContext.getCountryAndStateIso(lon, lat);
                if (casi != null) {
                    if ("".equals(country)) { // will be null if empty
                        tags.put(Tags.KEY_ADDR_COUNTRY, casi.getCountry());
                    }
                    if (casi.getState() != null && "".equals(state)) {
                        // note this assumes that the ISO code actually makes sense here
                        tags.put(Tags.KEY_ADDR_STATE, casi.getState());
                    }
                }
            }
        } catch (IllegalArgumentException iaex) {
            Log.e(DEBUG_TAG, "setCountryAndState " + iaex);
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
     * is somewhere in the middle determine on which side of it we are, - inc/dec in that direction. If everything works
     * out correctly even if a prediction is wrong, entering the correct number should improve the next prediction
     * 
     * TODO the above assumes that the road is not doubling back or similar, aka that the addresses are more or less in
     * a straight line, use the length along the way defined by the addresses instead
     * 
     * @param context an Android Context
     * @param newAddress the address object for the new address
     * @param originalTags tags for this object
     * @param street the street name
     * @param currentStreetId id of the current street
     * @param side side which we are on
     * @param houseNumbers a Map of existing addresses on this side
     * @param otherSideList optional list of numbers found on the other side
     * @return the tags for the object
     */
    @NonNull
    private static Map<String, String> predictNumber(@NonNull Context context, @NonNull Address newAddress, @NonNull Map<String, String> originalTags,
            @NonNull String street, long currentStreetId, @NonNull Side side, @NonNull SortedMap<Integer, Address> houseNumbers,
            @Nullable SortedMap<Integer, Address> otherSideList) {
        Map<String, String> newTags = new LinkedHashMap<>(originalTags);
        if (houseNumbers.size() >= 2) {
            try {
                List<Integer> numbers = new ArrayList<>(houseNumbers.keySet());
                int inc = calcIncrement(context, numbers, houseNumbers);

                int firstNumber = houseNumbers.firstKey();
                int lastNumber = houseNumbers.lastKey();

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
                    Address a = houseNumbers.get(number);
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
                double distanceTotal = addressDistance(houseNumbers, firstNumber, lastNumber);
                if (nearest == firstNumber) {
                    if (distanceLast > distanceTotal) {
                        inc = -inc;
                    }
                } else if (nearest == lastNumber) {
                    if (distanceFirst < distanceTotal) {
                        inc = -inc;
                    }
                } else {
                    double distanceNearestFirst = addressDistance(houseNumbers, firstNumber, nearest);
                    if (distanceFirst < distanceNearestFirst) {
                        inc = -inc;
                    } // else already correct
                }
                // first apply tags from nearest address if they don't already exist
                copyTags(houseNumbers.get(nearest), newTags);

                if (otherSideList != null) { // try to predict address on the other road side
                    Log.d(DEBUG_TAG, "Predicting for other side inc=" + inc + " nearest " + nearest);
                    if (Math.abs(inc) > 1) {
                        int newNumber = Math.max(1, otherSideList.size() == 0 ? nearest + (inc / Math.abs(inc)) : otherSideList.firstKey() + inc);
                        Log.d(DEBUG_TAG, "final predicted result for the other side " + newNumber);
                        newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Integer.toString(newNumber));
                    } else { // no sense to guess pattern
                        Log.d(DEBUG_TAG, "giving up");
                        newTags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
                    }
                } else { // predict on this side
                    int newNumber = Math.max(1, nearest + inc);
                    Log.d(DEBUG_TAG, "Predicted " + newNumber + " first " + firstNumber + " last " + lastNumber + " nearest " + nearest + " inc " + inc
                            + " prev " + prev + " post " + post + " side " + side);
                    if (numbers.contains(newNumber)) {
                        // try one inc more and one less, if they both fail use the largest number + inc
                        if (!numbers.contains(Math.max(1, newNumber + inc))) {
                            newNumber = Math.max(1, newNumber + inc);
                        } else if (!numbers.contains(Math.max(1, newNumber - inc))) {
                            newNumber = Math.max(1, newNumber - inc);
                        } else {
                            newNumber = Math.max(1, numbers.get(numbers.size() - 1) + Math.abs(inc));
                        }
                    }
                    Log.d(DEBUG_TAG, "final predicted result " + newNumber);
                    newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Integer.toString(newNumber));
                }
            } catch (NumberFormatException nfe) {
                Log.d(DEBUG_TAG, "exception " + nfe);
                newTags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
            }
        } else if (houseNumbers.size() == 1) {
            Log.d(DEBUG_TAG, "only one number on this side");
            // can't do prediction with only one value
            // apply tags from sole existing address if they don't already exist
            SortedMap<Integer, Address> otherList = getHouseNumbers(street, currentStreetId, Side.opposite(side), lastAddresses);
            if (otherList.size() >= 2) {
                newTags = predictNumber(context, newAddress, originalTags, street, currentStreetId, side, otherList, houseNumbers);
            } else {
                copyTags(houseNumbers.get(houseNumbers.firstKey()), newTags);
            }
        } else if (houseNumbers.size() == 0) {
            Log.d(DEBUG_TAG, "no numbers on this side");
            SortedMap<Integer, Address> otherList = getHouseNumbers(street, currentStreetId, Side.opposite(side), lastAddresses);
            if (otherList.size() >= 2) {
                newTags = predictNumber(context, newAddress, originalTags, street, currentStreetId, side, otherList, houseNumbers);
            } else if (otherList.size() == 1) {
                copyTags(otherList.get(otherList.firstKey()), newTags);
            } else {
                newTags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
            }
        }
        return newTags;
    }

    /**
     * Get the haversine distance between two addresses
     * 
     * @param map Map of addresses indexed by house number
     * @param n1 first house number
     * @param n2 2nd house number
     * @return distance between n1 and n2
     */
    private static double addressDistance(SortedMap<Integer, Address> map, int n1, int n2) {
        return GeoMath.haversineDistance(map.get(n1).lon, map.get(n1).lat, map.get(n2).lon, map.get(n2).lat);
    }

    /**
     * Calculate a likely increment between addresses
     * 
     * @param context an Anroid Context
     * @param numbers a sorted List of house numbers
     * @param map a map of addresses indexed by the house number
     * @return an estimated increment of one number to the next
     */
    private static int calcIncrement(@NonNull Context context, @NonNull List<Integer> numbers, @NonNull SortedMap<Integer, Address> map) {
        //
        // determine increment
        //
        int inc = 1;
        int neighbourDistance = App.getPreferences(context).getNeighbourDistance();
        float incTotal = 0;
        float incCount = 0;
        final int size = numbers.size();
        for (int i = 0; i < size - 1; i++) {
            final Integer n1 = numbers.get(i + 1);
            final Integer n2 = numbers.get(i);
            int diff = n1 - n2;
            if (diff > 0) {
                if (diff > 2) {
                    double dist = addressDistance(map, n1, n2);
                    if (dist > neighbourDistance) { // unlikely to be a neighbour
                        continue;
                    }
                }
                incTotal = incTotal + diff;
                incCount++;
            }
        }
        inc = incCount != 0 ? Math.round(incTotal / incCount) : 1;
        return inc;
    }

    /**
     * Copy tags from an Address to an existing Map holding tags
     * 
     * @param address the Address object
     * @param tags the Map with tags
     */
    private static void copyTags(@Nullable Address address, @NonNull Map<String, String> tags) {
        if (address != null) {
            for (Entry<String, String> entry : address.tags.entrySet()) {
                String key = entry.getKey();
                if (!tags.containsKey(key)) { // NOSONAR
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
     * The original addr:housenumber tag is split on ",", ";" and "-". Currently it will use addresses associated with
     * directly neighboring way segments, and correctly adjust what it considers the side the address is on.
     * 
     * @param street the street name
     * @param side side of the street that should be considered
     * @param currentStreetId the id of the current street
     * @param addresses the list of addresses
     * @return a sorted map with the house numbers as key
     */
    @NonNull
    private static synchronized SortedMap<Integer, Address> getHouseNumbers(@Nullable String street, long currentStreetId, @Nullable Address.Side side,
            @NonNull LinkedList<Address> addresses) {
        Log.d(DEBUG_TAG, "getHouseNumbers for " + street + " " + currentStreetId);
        LongOsmElementMap<Way> wayCache = new LongOsmElementMap<>();
        SortedMap<Integer, Address> result = new TreeMap<>(); // list sorted by house numbers
        for (Address a : addresses) {
            if (a == null || a.tags == null) {
                continue;
            }
            String addrStreetValue = a.tags.get(Tags.KEY_ADDR_STREET);
            String addrPlaceValue = a.tags.get(Tags.KEY_ADDR_PLACE);
            String addrHousenumberValue = a.tags.get(Tags.KEY_ADDR_HOUSENUMBER);
            if (((addrStreetValue != null && addrStreetValue.equals(street)) || (addrPlaceValue != null && addrPlaceValue.equals(street)))
                    && !isEmpty(addrHousenumberValue)) {
                final boolean sidesDiffer = a.getSide() != side;
                if (currentStreetId != a.streetId) {
                    try {
                        Way current = getWay(currentStreetId, wayCache);
                        Way addressStreet = getWay(a.streetId, wayCache);
                        final Node currentLast = current.getLastNode();
                        final Node currentFirst = current.getFirstNode();
                        final Node addressFirst = addressStreet.getFirstNode();
                        final Node addressLast = addressStreet.getLastNode();
                        if (current.hasCommonNode(addressStreet) && ((currentLast == addressFirst || currentFirst == addressLast) && !sidesDiffer)
                                || ((currentLast == addressLast || currentFirst == addressFirst) && sidesDiffer)) {
                            addToResult(result, a, addrHousenumberValue);
                        }
                    } catch (IllegalStateException isex) {
                        // already logged
                    }
                } else if (!sidesDiffer) {
                    addToResult(result, a, addrHousenumberValue);
                }
            }
        }
        return result;
    }

    /**
     * Extract house number and add to result
     * 
     * @param result Collection with the results
     * @param a the Address
     * @param housenumberValue value from the addr:housenumber tag
     */
    static void addToResult(@NonNull SortedMap<Integer, Address> result, @NonNull Address a, @NonNull String housenumberValue) {
        String[] numbers = housenumberValue.split("[\\,;\\-]");
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

    /**
     * Get a Way based on its id and store in cache
     * 
     * @param streetId the ID
     * @param wayCache the cache
     * @return the Way
     */
    @NonNull
    private static Way getWay(long streetId, LongOsmElementMap<Way> wayCache) {
        Way current = wayCache.get(streetId);
        if (current == null) {
            current = (Way) App.getDelegator().getOsmElement(Way.NAME, streetId);
            if (current != null) {
                wayCache.put(streetId, current);
            } else {
                Log.e(DEBUG_TAG, "Way " + streetId + " not found in storage");
                throw new IllegalStateException();
            }
        }
        return current;
    }

    /**
     * Add an address from OSM data to the address cache
     * 
     * @param context Android Context
     * @param street the street name
     * @param e the OsmElement
     * @param addresses the list of addresses
     */
    private static void seedAddressList(@NonNull Context context, @NonNull String street, @NonNull OsmElement e, @NonNull LinkedList<Address> addresses) {
        if (e.hasTag(Tags.KEY_ADDR_STREET, street) && e.hasTagKey(Tags.KEY_ADDR_HOUSENUMBER)) {
            Address seed = new Address(e, null);
            seed.setTags(getAddressTags(context, seed.lon, seed.lat, new LinkedHashMap<>(e.getTags())));
            ElementSearch es = new ElementSearch(new IntCoordinates((int) (seed.lon * 1E7), (int) (seed.lat * 1E7)), true);
            try {
                seed.setSide(es.getStreetId(street));
            } catch (OsmException ex) {
                Log.e(DEBUG_TAG, "seedAddressList " + ex.getMessage());
                return;
            }
            if (addresses.size() >= MAX_SAVED_ADDRESSES) { // arbitrary limit for now
                addresses.removeLast();
            }
            addresses.addFirst(seed);
            Log.d(DEBUG_TAG, "seedAddressList added " + seed.tags.toString());
        }
    }

    /**
     * Retrieve address tags from a map of tags taking the address preferences in to account
     * 
     * @param context Android Context
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param sortedMap LinkedHashMap containing the tags
     * @return a Map containing only the relevant address tags
     */
    private static Map<String, String> getAddressTags(@NonNull Context context, double lon, double lat, @NonNull Map<String, String> sortedMap) {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> addressTags = getAddressKeys(context, lon, lat);
        for (Entry<String, String> entry : sortedMap.entrySet()) {
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
     * @param context an Android Context
     * @param streetAdapter an instance of a StreetPlaceNameAdapter or null
     * @param type type of element
     * @param id the element id
     * @param tags tags current tags
     * @param save if true save
     */
    public static synchronized void updateLastAddresses(@NonNull Context context, @Nullable StreetPlaceNamesAdapter streetAdapter, @NonNull String type,
            long id, @NonNull Map<String, List<String>> tags, boolean save) {
        // this needs to be done after the edit again in case the street name or whatever has changed
        if (lastAddresses == null) {
            lastAddresses = new LinkedList<>();
        }
        if (lastAddresses.size() >= MAX_SAVED_ADDRESSES) { // arbitrary limit for now
            lastAddresses.removeLast();
        }
        try {
            Address current = new Address(type, id, null);
            current.setTags(getAddressTags(context, current.lon, current.lat, multiValueToSingle(tags)));
            if (streetAdapter != null) {
                String streetName = current.getTags().get(Tags.KEY_ADDR_STREET);
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

    /**
     * Save the address list to persistent storage
     * 
     * @param context Android Context
     */
    public static synchronized void saveLastAddresses(@NonNull Context context) {
        if (lastAddresses != null) {
            savingHelperAddress.save(context, ADDRESS_TAGS_FILE, lastAddresses, false);
        }
    }

    /**
     * Read the address list from persistent storage
     * 
     * @param context Android Context
     */
    public static synchronized void loadLastAddresses(@NonNull Context context) {
        if (lastAddresses == null) {
            try {
                lastAddresses = savingHelperAddress.load(context, ADDRESS_TAGS_FILE, false);
                Log.d(DEBUG_TAG, "onResume read " + lastAddresses.size() + " addresses");
            } catch (Exception e) {
                // never crash
            }
        }
    }

    /**
     * Convert a multi-valued Map to one with single values
     * 
     * @param multi a Map&lt;String, List&lt;String&gt;&gt;
     * @return a Map&lt;String, String&gt;
     */
    public static Map<String, String> multiValueToSingle(@NonNull Map<String, List<String>> multi) {
        Map<String, String> simple = new TreeMap<>();
        for (Entry<String, List<String>> entry : multi.entrySet()) {
            simple.put(entry.getKey(), entry.getValue().get(0));
        }
        return simple;
    }

    /**
     * @return the tags
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
