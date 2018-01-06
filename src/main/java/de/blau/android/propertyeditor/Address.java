package de.blau.android.propertyeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Logic;
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
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StreetTagValueAdapter;
import de.blau.android.util.Util;

/**
 * Store coordinates and address information for use in address prediction
 * 
 * @author simon
 *
 */
public class Address implements Serializable {
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

    private Side                                     side = Side.UNKNOWN;
    private float                                    lat;
    private float                                    lon;
    private LinkedHashMap<String, ArrayList<String>> tags;

    private static LinkedList<Address> lastAddresses = null;

    /**
     * Create a copy of Address a
     * 
     * @param a Address object
     */
    private Address(Address a) {
        side = a.side;
        lat = a.lat;
        lon = a.lon;
        tags = new LinkedHashMap<>(a.tags);
    }

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
    private Address(String type, long id, LinkedHashMap<String, ArrayList<String>> tags) {
        OsmElement e = App.getDelegator().getOsmElement(type, id);
        if (e == null) {
            Log.e(DEBUG_TAG, type + " " + id + " doesn't exist in storage ");
            // FIXME is might make sense to create a crash dump here
            return;
        }
        init(e, tags);
    }

    /**
     * Create an address object from an OSM element
     * 
     * @param e the OSM element
     * @param tags the relevant address tags
     */
    private Address(OsmElement e, LinkedHashMap<String, ArrayList<String>> tags) {
        init(e, tags);
    }

    /**
     * Initialize an address object from an OSM element
     * 
     * @param e the OSM element
     * @param tags the relevant address tags
     */
    private void init(OsmElement e, LinkedHashMap<String, ArrayList<String>> tags) {
        switch (e.getType()) {
        case NODE:
            lat = ((Node) e).getLat() / 1E7F;
            lon = ((Node) e).getLon() / 1E7F;
            break;
        case WAY:
        case CLOSEDWAY:
        case AREA:
            if (Way.NAME.equals(e.getName())) {
                double[] center = Logic.centroidLonLat((Way) e);
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
        // Log.d(DEBUG_TAG,"set side to " + side);
    }

    private Side getSide() {
        return side;
    }

    /**
     * Predict address tags
     * 
     * This uses a file to cache/save the address information over invocations of the TagEditor, if the cache doesn't
     * have entries for a specific street/place an attempt to extract the information from the downloaded data is made
     *
     * @param context Android context
     * @param elementType element type (node, way, relation)
     * @param elementOsmId osm object id
     * @param es
     * @param current current tags
     * @param maxRank determines how far away from the nearest street the last address street can be, 0 will always use
     *            the nearest, higher numbers will provide some hysteresis
     * @return map containing the predicted address tags
     */
    public synchronized static Map<String, ArrayList<String>> predictAddressTags(@NonNull Context context, @NonNull final String elementType,
            final long elementOsmId, @Nullable final ElementSearch es, @NonNull final Map<String, ArrayList<String>> current, int maxRank) {
        Address newAddress = null;

        loadLastAddresses(context);

        if (lastAddresses != null && !lastAddresses.isEmpty()) {
            Log.d(DEBUG_TAG, "initializing with last addresses");
            Address lastAddress = lastAddresses.get(0);
            newAddress = new Address(elementType, elementOsmId, lastAddress.tags); // last address we added
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
            if (newAddress.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
                newAddress.tags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(""));
            }
        }

        if (newAddress == null) { // make sure we have the address object
            newAddress = new Address(elementType, elementOsmId, new LinkedHashMap<String, ArrayList<String>>());
            Log.d("Address", "nothing to seed with, creating new");
        }
        // merge in any existing tags
        for (Entry<String, ArrayList<String>> entry : current.entrySet()) {
            Log.d("Address", "Adding in existing tag " + entry.getKey());
            newAddress.tags.put(entry.getKey(), entry.getValue());
        }

        boolean hasPlace = newAddress.tags.containsKey(Tags.KEY_ADDR_PLACE);
        boolean hasNumber = current.containsKey(Tags.KEY_ADDR_HOUSENUMBER); // if the object already had a number don't
                                                                            // overwrite it
        StorageDelegator storageDelegator = App.getDelegator();
        if (es != null) {
            // the arrays should now be calculated, retrieve street names if any
            ArrayList<String> streetNames = new ArrayList<>(Arrays.asList(es.getStreetNames()));
            if ((streetNames != null && !streetNames.isEmpty()) || hasPlace) {
                LinkedHashMap<String, ArrayList<String>> tags = newAddress.tags;
                Log.d(DEBUG_TAG, "tags.get(Tags.KEY_ADDR_STREET)) " + tags.get(Tags.KEY_ADDR_STREET));
                // Log.d("TagEditor","Rank of " + tags.get(Tags.KEY_ADDR_STREET) + " " +
                // streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
                String street;
                if (!hasPlace) {
                    ArrayList<String> addrStreetValues = tags.get(Tags.KEY_ADDR_STREET);
                    int rank = -1;
                    boolean hasAddrStreet = addrStreetValues != null && !addrStreetValues.isEmpty() && !addrStreetValues.get(0).equals("");
                    if (hasAddrStreet) {
                        rank = streetNames.indexOf(addrStreetValues.get(0)); // FIXME this and the following could
                                                                             // consider other values in multi select
                    }
                    Log.d(DEBUG_TAG, (hasAddrStreet ? "rank " + rank + " for " + addrStreetValues.get(0) : "no addrStreet tag"));
                    if (!hasAddrStreet || rank > maxRank || rank < 0) { // check if has street and still in the top 3
                                                                        // nearest
                        // Log.d("TagEditor","names.indexOf(tags.get(Tags.KEY_ADDR_STREET)) " +
                        // streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
                        // nope -> zap
                        tags.put(Tags.KEY_ADDR_STREET, Util.getArrayList(streetNames.get(0)));
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
                    ArrayList<String> addrPlaceValues = tags.get(Tags.KEY_ADDR_PLACE);
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
                    TreeMap<Integer, Address> list = getHouseNumbers(street, side, lastAddresses);
                    if (list.size() == 0) { // try to seed lastAddresses from OSM data
                        try {
                            Log.d(DEBUG_TAG, "street " + street);
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
                    // tags = predictNumber(newAddress, tags, street, side, list, false);
                    newAddress.tags = predictNumber(newAddress, tags, street, side, list, false, null);
                }
            } else { // last ditch attemot
                // fill with Karlsruher schema
                Preferences prefs = new Preferences(context);
                Set<String> addressTags = prefs.addressTags();
                for (String key : addressTags) {
                    newAddress.tags.put(key, Util.getArrayList(""));
                }
            }
        }

        // is this a node on a building outline, if yes add entrance=yes if it doesn't already exist
        if (elementType.equals(Node.NAME)) {
            boolean isOnBuilding = false;
            // we can't call wayForNodes here because Logic may not be around
            for (Way w : storageDelegator.getCurrentStorage().getWays((Node) storageDelegator.getOsmElement(Node.NAME, elementOsmId))) {
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
                newAddress.tags.put(Tags.KEY_ENTRANCE, Util.getArrayList("yes"));
            }
        }
        return newAddress.tags;
    }

    /**
     * Try to predict the next number - get all existing numbers for the side of the street we are on - determine if the
     * increment per number is 1 or 2 (for now everything else is ignored) - determine the nearest address node - if it
     * is the last or first node and we are at one side use that and add or subtract the increment - if the nearest node
     * is somewhere in the middle determine on which side of it we are, - inc/dec in that direction If everything works
     * out correctly even if a prediction is wrong, entering the correct number should improve the next prediction TODO
     * the above assumes that the road is not doubling back or similar, aka that the addresses are more or less in a
     * straight line, use the length along the way defined by the addresses instead
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
    private static LinkedHashMap<String, ArrayList<String>> predictNumber(@NonNull Address newAddress,
            @NonNull LinkedHashMap<String, ArrayList<String>> originalTags, @NonNull String street, @NonNull Side side, @NonNull TreeMap<Integer, Address> list,
            boolean oppositeSide, @Nullable TreeMap<Integer, Address> otherSideList) {
        LinkedHashMap<String, ArrayList<String>> newTags = new LinkedHashMap<>(originalTags);
        if (list.size() >= 2) {
            try {
                //
                // determine increment
                //
                int inc = 1;
                float incTotal = 0;
                float incCount = 0;
                ArrayList<Integer> numbers = new ArrayList<>(list.keySet());
                for (int i = 0; i < numbers.size() - 1; i++) {
                    int diff = numbers.get(i + 1).intValue() - numbers.get(i).intValue();
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
                    // FIXME there is an obvious better criteria
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
                        newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(Integer.toString(newNumber)));
                    } else { // no sense to guess pattern
                        Log.d(DEBUG_TAG, "giving up");
                        newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(""));
                    }
                } else { // predict on this side
                    int newNumber = Math.max(1, nearest + inc);
                    // Toast.makeText(this, "First " + firstNumber + " last " + lastNumber + " nearest " + nearest +
                    // "inc " + inc + " prev " + prev + " post " + post + " side " + side, Toast.LENGTH_LONG).show();
                    Log.d(DEBUG_TAG, "Predicted " + newNumber + " first " + firstNumber + " last " + lastNumber + " nearest " + nearest + " inc " + inc
                            + " prev " + prev + " post " + post + " side " + side);
                    if (numbers.contains(Integer.valueOf(newNumber))) {
                        // try one inc more and one less, if they both fail use the original number
                        if (!numbers.contains(Integer.valueOf(Math.max(1, newNumber + inc)))) {
                            newNumber = Math.max(1, newNumber + inc);
                        } else if (!numbers.contains(Integer.valueOf(Math.max(1, newNumber - inc)))) {
                            newNumber = Math.max(1, newNumber - inc);
                        }
                    }
                    Log.d(DEBUG_TAG, "final predicted result " + newNumber);
                    newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(Integer.toString(newNumber)));
                }
            } catch (NumberFormatException nfe) {
                Log.d(DEBUG_TAG, "exception " + nfe);
                newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(""));
            }
        } else if (list.size() == 1) {
            Log.d(DEBUG_TAG, "only one number on this side");
            // can't do prediction with only one value
            // apply tags from sole existing address if they don't already exist
            TreeMap<Integer, Address> otherList = getHouseNumbers(street, Side.opposite(side), lastAddresses);
            if (otherList.size() >= 2) {
                newTags = predictNumber(newAddress, originalTags, street, side, otherList, true, list);
            } else {
                copyTags(list.get(list.firstKey()), newTags);
            }
        } else if (list.size() == 0) {
            Log.d(DEBUG_TAG, "no numbers on this side");
            TreeMap<Integer, Address> otherList = getHouseNumbers(street, Side.opposite(side), lastAddresses);
            if (otherList.size() >= 2) {
                newTags = predictNumber(newAddress, originalTags, street, side, otherList, true, list);
            } else if (otherList.size() == 1) {
                copyTags(otherList.get(otherList.firstKey()), newTags);
            } else {
                newTags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(""));
            }
        }
        return newTags;
    }

    private static void copyTags(@Nullable Address address, @NonNull LinkedHashMap<String, ArrayList<String>> tags) {
        if (address != null) {
            for (Entry<String, ArrayList<String>> entry : address.tags.entrySet()) {
                String key = entry.getKey();
                if (!tags.containsKey(key)) {
                    tags.put(key, entry.getValue());
                }
            }
        } else {
            Log.e(DEBUG_TAG, "address shoudn't be null");
            // maybe a crash dump would be a good ide
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
    private static int getNumber(String hn) throws NumberFormatException {
        StringBuilder sb = new StringBuilder();
        for (Character c : hn.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        if (sb.toString().equals("")) {
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
    private synchronized static TreeMap<Integer, Address> getHouseNumbers(String street, Address.Side side, LinkedList<Address> addresses) {
        TreeMap<Integer, Address> result = new TreeMap<>(); // list sorted by house numbers
        for (Address a : addresses) {
            if (a != null && a.tags != null) {
                ArrayList<String> addrStreetValues = a.tags.get(Tags.KEY_ADDR_STREET);
                ArrayList<String> addrPlaceValues = a.tags.get(Tags.KEY_ADDR_PLACE);
                if (((addrStreetValues != null && !addrStreetValues.isEmpty() && addrStreetValues.get(0).equals(street)) // FIXME
                        || (addrPlaceValues != null && !addrPlaceValues.isEmpty() && addrPlaceValues.get(0).equals(street)))
                        && a.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER) && a.getSide() == side) {
                    Log.d(DEBUG_TAG, "Number " + a.tags.get(Tags.KEY_ADDR_HOUSENUMBER));
                    ArrayList<String> addrHousenumberValues = a.tags.get(Tags.KEY_ADDR_HOUSENUMBER);
                    if (addrHousenumberValues != null && !addrHousenumberValues.isEmpty()) {
                        String[] numbers = addrHousenumberValues.get(0).split("[\\,;\\-]");
                        for (String n : numbers) {
                            Log.d(DEBUG_TAG, "add number  " + n);
                            // noinspection EmptyCatchBlock
                            try {
                                result.put(Integer.valueOf(getNumber(n)), a);
                            } catch (NumberFormatException nfe) {
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
     * @param street
     * @param streetId
     * @param e
     * @param addresses
     */
    private static void seedAddressList(Context context, String street, long streetId, OsmElement e, LinkedList<Address> addresses) {
        if (e.hasTag(Tags.KEY_ADDR_STREET, street) && e.hasTagKey(Tags.KEY_ADDR_HOUSENUMBER)) {
            Address seed = new Address(e, getAddressTags(context, new LinkedHashMap<>(Util.getArrayListMap(e.getTags()))));
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

    private static LinkedHashMap<String, ArrayList<String>> getAddressTags(Context context, LinkedHashMap<String, ArrayList<String>> sortedMap) {
        LinkedHashMap<String, ArrayList<String>> result = new LinkedHashMap<>();
        Preferences prefs = new Preferences(context);
        Set<String> addressTags = prefs.addressTags();
        for (Entry<String, ArrayList<String>> entry : sortedMap.entrySet()) {
            // include everything except interpolation related tags
            if (addressTags.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Flush the cache both in memory and on disk
     * 
     * @param context Android context
     */
    public synchronized static void resetLastAddresses(Context context) {
        savingHelperAddress.save(context, ADDRESS_TAGS_FILE, new LinkedList<Address>(), false);
        lastAddresses = null;
    }

    synchronized static void updateLastAddresses(TagEditorFragment caller, LinkedHashMap<String, ArrayList<String>> tags) {
        // save any address tags for "last address tags"
        LinkedHashMap<String, ArrayList<String>> addressTags = getAddressTags(caller.getContext(), tags);
        // this needs to be done after the edit again in case the street name of what ever has changed
        if (addressTags.size() > 0) {
            if (lastAddresses == null) {
                lastAddresses = new LinkedList<>();
            }
            if (lastAddresses.size() >= MAX_SAVED_ADDRESSES) { // arbitrary limit for now
                lastAddresses.removeLast();
            }
            Address current = new Address(caller.getType(), caller.getOsmId(), addressTags);
            StreetTagValueAdapter streetAdapter = (StreetTagValueAdapter) ((NameAdapters) caller.getActivity()).getStreetNameAdapter(null);
            if (streetAdapter != null) {
                ArrayList<String> values = tags.get(Tags.KEY_ADDR_STREET);
                if (values != null && !values.isEmpty()) {
                    String streetName = values.get(0); // FIXME can't remember what this is supposed to do....
                    if (streetName != null) {
                        try {
                            current.setSide(streetAdapter.getId(streetName));
                        } catch (OsmException e) {
                            current.side = Side.UNKNOWN;
                        }
                    }
                }
            }
            lastAddresses.addFirst(current);
            savingHelperAddress.save(caller.getActivity(), ADDRESS_TAGS_FILE, lastAddresses, false);
        }
    }

    synchronized static void saveLastAddresses(Context context) {
        if (lastAddresses != null) {
            savingHelperAddress.save(context, ADDRESS_TAGS_FILE, lastAddresses, false);
        }
    }

    synchronized static void loadLastAddresses(Context context) {
        if (lastAddresses == null) {
            try {
                lastAddresses = savingHelperAddress.load(context, ADDRESS_TAGS_FILE, false);
                Log.d("TagEditor", "onResume read " + lastAddresses.size() + " addresses");
            } catch (Exception e) {
                // TODO be more specific
            }
        }
    }
}
