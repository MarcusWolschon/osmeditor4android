package de.blau.android.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Key and value constants for tags that are used in the code
 * 
 * Some of this duplicates information that in principal is available in the standard preset too, however having it here
 * makes some key thins possible without having to rely on a preset being present and matched correctly
 * 
 * @author Simon
 *
 */
public final class Tags {
    public static final String OSM_VALUE_SEPARATOR = ";";

    // Karlsruher schema
    private static final String KEY_ADDR             = "addr";
    public static final String  KEY_ADDR_BASE        = KEY_ADDR + ":";
    public static final String  KEY_ADDR_HOUSENUMBER = "addr:housenumber";
    public static final String  KEY_ADDR_STREET      = "addr:street";
    public static final String  KEY_ADDR_POSTCODE    = "addr:postcode";
    public static final String  KEY_ADDR_CITY        = "addr:city";
    public static final String  KEY_ADDR_COUNTRY     = "addr:country";
    public static final String  KEY_ADDR_FULL        = "addr:full";
    // the following are less used but may be necessary
    public static final String KEY_ADDR_HOUSENAME          = "addr:housename";
    public static final String KEY_ADDR_CONSCRIPTIONNUMBER = "addr:conscriptionnumber";
    public static final String KEY_ADDR_PLACE              = "addr:place";
    public static final String KEY_ADDR_BLOCK              = "addr:block";
    public static final String KEY_ADDR_BLOCK_NUMBER       = "addr:block_number";
    public static final String KEY_ADDR_PARENTSTREET       = "addr:parentstreet";
    public static final String KEY_ADDR_HAMLET             = "addr:hamlet";
    public static final String KEY_ADDR_SUBURB             = "addr:suburb";
    public static final String KEY_ADDR_SUBDISTRICT        = "addr:subdistrict";
    public static final String KEY_ADDR_DISTRICT           = "addr:district";
    public static final String KEY_ADDR_PROVINCE           = "addr:province";
    public static final String KEY_ADDR_STATE              = "addr:state";
    public static final String KEY_ADDR_FLATS              = "addr:flats";
    public static final String KEY_ADDR_DOOR               = "addr:door";
    public static final String KEY_ADDR_UNIT               = "addr:unit";

    private static final Map<String, Integer> ADDRESS_SORT_ORDER_TEMP = new HashMap<>();
    static {
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_HOUSENUMBER, 0);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_HOUSENAME, 1);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_CONSCRIPTIONNUMBER, 2);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_FLATS, 3);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_UNIT, 4);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_DOOR, 5);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_PLACE, 6);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_STREET, 6);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_BLOCK, 6);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_BLOCK_NUMBER, 6);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_POSTCODE, 7);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_HAMLET, 8);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_SUBURB, 9);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_CITY, 10);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_SUBDISTRICT, 11);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_DISTRICT, 12);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_PROVINCE, 13);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_STATE, 14);
        ADDRESS_SORT_ORDER_TEMP.put(KEY_ADDR_COUNTRY, 15);
    }
    public static final Map<String, Integer> ADDRESS_SORT_ORDER = Collections.unmodifiableMap(ADDRESS_SORT_ORDER_TEMP);

    // address interpolation
    public static final String KEY_ADDR_INTERPOLATION = "addr:interpolation";
    public static final String VALUE_ODD              = "odd";
    public static final String VALUE_EVEN             = "even";
    public static final String KEY_ADDR_INCLUSION     = "addr:inclusion";
    public static final String VALUE_ACTUAL           = "actual";
    public static final String VALUE_ESTIMATE         = "estimate";
    public static final String VALUE_POTENTIAL        = "potential";

    // "large scale" address keys
    public static final List<String> ADDRESS_LARGE = Collections.unmodifiableList(
            Arrays.asList(KEY_ADDR_STREET, KEY_ADDR_PLACE, KEY_ADDR_BLOCK, KEY_ADDR_BLOCK_NUMBER, KEY_ADDR_PARENTSTREET, KEY_ADDR_POSTCODE, KEY_ADDR_HAMLET,
                    KEY_ADDR_SUBURB, KEY_ADDR_CITY, KEY_ADDR_SUBDISTRICT, KEY_ADDR_DISTRICT, KEY_ADDR_PROVINCE, KEY_ADDR_STATE, KEY_ADDR_COUNTRY));

    // other address related stuff
    public static final String KEY_BUILDING = "building";
    public static final String KEY_ENTRANCE = "entrance";
    //
    public static final String       KEY_NAME          = "name";
    public static final String       KEY_OFFICIAL_NAME = "official_name";
    public static final String       KEY_ALT_NAME      = "alt_name";
    private static final String      KEY_LOC_NAME      = "loc_name";
    public static final String       KEY_OLD_NAME      = "old_name";
    private static final String      KEY_SHORT_NAME    = "short_name";
    private static final String      KEY_REG_NAME      = "reg_name";
    private static final String      KEY_NAT_NAME      = "nat_name";
    public static final String       KEY_INT_NAME      = "int_name";
    public static final List<String> I18N_NAME_KEYS    = Collections
            .unmodifiableList(Arrays.asList(KEY_NAME, KEY_OFFICIAL_NAME, KEY_ALT_NAME, KEY_LOC_NAME, KEY_SHORT_NAME, KEY_REG_NAME, KEY_NAT_NAME));

    /**
     * Check if a key in general can be assumed to have a different value for each occurrence
     * 
     * We also assume that key: are variants that follow the same rule
     * 
     * @param key the key to check
     * @return true if the key has name-like semantics
     */
    public static boolean isLikeAName(@NonNull String key) {
        List<String> nameLikeKeys = new ArrayList<>(Tags.I18N_NAME_KEYS);
        nameLikeKeys.add(Tags.KEY_ADDR);
        nameLikeKeys.add(Tags.KEY_ADDR_HOUSENAME);
        nameLikeKeys.add(Tags.KEY_ADDR_UNIT);
        nameLikeKeys.add(Tags.KEY_REF);
        for (String k : nameLikeKeys) {
            if (k.equals(key) || key.startsWith(k + ":")) {
                return true;
            }
        }
        return false;
    }

    public static final String KEY_NAME_LEFT             = "name:left";
    public static final String KEY_NAME_RIGHT            = "name:right";
    public static final String KEY_REF                   = "ref";
    public static final String KEY_NONAME                = "noname";
    public static final String KEY_VALIDATE_NO_NAME      = "validate:no_name";
    public static final String KEY_NOREF                 = "noref";
    public static final String KEY_HIGHWAY               = "highway";
    public static final String VALUE_ROAD                = "road";
    public static final String VALUE_MOTORWAY            = "motorway";
    public static final String VALUE_MOTORWAY_LINK       = "motorway_link";
    public static final String VALUE_FOOTWAY             = "footway";
    public static final String VALUE_CYCLEWAY            = "cycleway";
    public static final String VALUE_PATH                = "path";
    public static final String VALUE_STEPS               = "steps";
    public static final String VALUE_TRACK               = "track";
    public static final String KEY_TRACKTYPE             = "tracktype";
    public static final String KEY_SIDEWALK              = "sidewalk";
    public static final String KEY_BRIDGE                = "bridge";
    public static final String KEY_TUNNEL                = "tunnel";
    public static final String VALUE_CULVERT             = "culvert";
    public static final String KEY_BARRIER               = "barrier";
    public static final String VALUE_RETAINING_WALL      = "retaining_wall";
    public static final String VALUE_KERB                = "kerb";
    public static final String VALUE_GUARD_RAIL          = "guard_rail";
    public static final String VALUE_CITY_WALL           = "city_wall";
    public static final String KEY_TWO_SIDED             = "two_sided";
    public static final String KEY_MAN_MADE              = "man_made";
    public static final String VALUE_EMBANKMENT          = "embankment";
    public static final String KEY_ONEWAY                = "oneway";
    public static final String VALUE_REVERSE             = "reverse";
    public static final String VALUE_MINUS_ONE           = "-1";
    public static final String VALUE_ONE                 = "1";
    public static final String VALUE_CYCLIST_WAITING_AID = "cyclist_waiting_aid";
    public static final String KEY_SIDE                  = "side";

    public static final String KEY_WATERWAY    = "waterway";
    public static final String VALUE_RIVERBANK = "riverbank";

    public static final String KEY_LANDUSE        = "landuse";
    public static final String VALUE_CONSTRUCTION = "construction";
    public static final String KEY_NATURAL        = "natural";
    public static final String VALUE_CLIFF        = "cliff";
    public static final String VALUE_COASTLINE    = "coastline";
    public static final String VALUE_EARTH_BANK   = "earth_bank";
    public static final String KEY_PLACE          = "place";

    public static final String KEY_OPERATOR        = "operator";
    public static final String KEY_BRAND           = "brand";
    public static final String KEY_BRAND_WIKIPEDIA = "brand:wikipedia";
    public static final String KEY_BRAND_WIKIDATA  = "brand:wikidata";

    public static final String KEY_RAILWAY = "railway";
    //
    public static final String KEY_SOURCE   = "source";
    public static final String VALUE_SURVEY = "survey";
    public static final String VALUE_GPS    = "GPS";
    // for relations
    public static final String KEY_TYPE               = "type";
    public static final String VALUE_RESTRICTION      = "restriction";
    public static final String VALUE_NO_U_TURN        = "no_u_turn";
    public static final String VALUE_NO_RIGHT_TURN    = "no_right_turn";
    public static final String VALUE_ONLY_RIGHT_TURN  = "only_right_turn";
    public static final String VALUE_NO_LEFT_TURN     = "no_left_turn";
    public static final String VALUE_ONLY_LEFT_TURN   = "only_left_turn";
    public static final String VALUE_NO_STRAIGHT_ON   = "no_straight_on";
    public static final String VALUE_ONLY_STRAIGHT_ON = "only_straight_on";
    public static final String ROLE_VIA               = "via";
    public static final String ROLE_FROM              = "from";
    public static final String ROLE_TO                = "to";
    public static final String VALUE_DESTINATION_SIGN = "destination_sign";
    public static final String ROLE_INTERSECTION      = "intersection";
    public static final String VALUE_BUILDING         = "building";
    public static final String VALUE_ROUTE            = "route";
    public static final String ROLE_FORWARD           = "forward";
    public static final String ROLE_BACKWARD          = "backward";
    public static final String ROLE_NORTH             = "north";
    public static final String ROLE_SOUTH             = "south";
    public static final String ROLE_EAST              = "east";
    public static final String ROLE_WEST              = "west";
    public static final String VALUE_MULTIPOLYGON     = "multipolygon";
    public static final String VALUE_BOUNDARY         = "boundary";
    public static final String KEY_BOUNDARY           = "boundary";
    public static final String ROLE_OUTER             = "outer";
    public static final String ROLE_INNER             = "inner";

    /**
     * Check if a relation member element should be treated as a via element
     * 
     * @param type relation type
     * @param role role of element
     * @return true if we should treat the elements as if it had a via role
     */
    public static boolean isVia(@NonNull String type, @NonNull String role) {
        return ROLE_VIA.equals(role) || (VALUE_DESTINATION_SIGN.equals(type) && ROLE_INTERSECTION.equals(role));
    }

    /**
     * Get the via element of a Relation
     * 
     * @param type the Relation type
     * @param r the Relation
     * @return a List of RelationMembers
     */
    @NonNull
    public static List<RelationMember> getVia(@NonNull String type, @NonNull Relation r) {
        return VALUE_DESTINATION_SIGN.equals(type) ? r.getMembersWithRole(Tags.ROLE_INTERSECTION) : r.getMembersWithRole(Tags.ROLE_VIA);
    }

    /**
     * Check if an element is a multipolygon
     * 
     * @param e the emelent
     * @return true is a multipolygon
     */
    public static boolean isMultiPolygon(@Nullable OsmElement e) {
        return e instanceof Relation && e.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON);
    }

    public static final String KEY_MAXSPEED = "maxspeed";
    public static final String KEY_MINSPEED = "minspeed";
    public static final String MPH          = " mph";

    /**
     * Check if the key has something to do with a vehicle speed
     * 
     * @param key the key to check
     * @return true is a speed related key
     */
    public static boolean isSpeedKey(@Nullable final String key) {
        return key != null && (key.startsWith(KEY_MAXSPEED) || key.startsWith(KEY_MINSPEED)) && !(key.contains(KEY_SOURCE) || key.contains(KEY_TYPE));
    }

    public static final String KEY_ACCESS        = "access";
    public static final String KEY_VEHICLE       = "vehicle";
    public static final String KEY_BICYCLE       = "bicycle";
    public static final String KEY_MOTORCAR      = "motorcar";
    public static final String KEY_MOTORCYCLE    = "motorcycle";
    public static final String KEY_MOTOR_VEHICLE = "motor_vehicle";

    //
    public static final String KEY_DIRECTION = "direction";
    public static final String KEY_INCLINE   = "incline";
    public static final String VALUE_UP      = "up";
    public static final String VALUE_DOWN    = "down";
    public static final char   VALUE_EAST    = 'E';
    public static final char   VALUE_WEST    = 'W';
    public static final char   VALUE_SOUTH   = 'S';
    public static final char   VALUE_NORTH   = 'N';

    public static final String KEY_AREA = "area";

    public static final String KEY_CONVEYING = "conveying";
    public static final String KEY_PRIORITY  = "priority";

    //
    public static final String KEY_TURN       = "turn";
    public static final String KEY_TURN_LANES = "turn:lanes";
    public static final String VALUE_RIGHT    = "right";
    public static final String VALUE_LEFT     = "left";
    public static final String VALUE_THROUGH  = "through";

    public static final String VALUE_FORWARD  = "forward";
    public static final String VALUE_BACKWARD = "backward";

    //
    public static final String KEY_ELE            = "ele";
    public static final String KEY_ELE_ELLIPSOID  = "ele:ellipsoid";
    public static final String KEY_ELE_GEOID      = "ele:geoid";
    public static final String KEY_ELE_BAROMETRIC = "ele:barometric";
    public static final String KEY_SOURCE_ELE     = "ele:source";
    public static final String VALUE_BAROMETER    = "barometer";
    public static final String VALUE_GNSS         = "gnss";
    public static final String KEY_GNSS_HDOP      = "gnss:hdop";

    //
    public static final String VALUE_YES   = "yes";
    public static final String VALUE_TRUE  = "true";
    public static final String VALUE_NO    = "no";
    public static final String VALUE_FALSE = "false";
    //
    public static final String KEY_WIKIPEDIA = "wikipedia";
    public static final String KEY_WIKIDATA  = "wikidata";

    public static final String KEY_OPENING_HOURS    = "opening_hours";
    public static final String KEY_SERVICE_TIMES    = "service_times";
    public static final String KEY_COLLECTION_TIMES = "collection_times";

    public static final Set<String> OPENING_HOURS_SYNTAX = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(KEY_OPENING_HOURS, KEY_SERVICE_TIMES, KEY_COLLECTION_TIMES)));

    public static final String KEY_CONDITIONAL_SUFFIX = ":conditional";

    /**
     * Check if this is a key is a conditional restriction
     * 
     * @param key the key to check
     * @return true if the key is a conditional restriction
     */
    public static boolean isConditional(@NonNull String key) {
        return key.endsWith(Tags.KEY_CONDITIONAL_SUFFIX);
    }

    /**
     * Check if this is a key can potential have nested semi-colon lists
     * 
     * @param key the key to check
     * @return true if the key is a conditional restriction
     */
    public static boolean hasNestedLists(@NonNull String key) {
        return Tags.isConditional(key) || Tags.KEY_TURN_LANES.equals(key);
    }

    // keys where the values are URLs
    private static final String KEY_WEBSITE         = "website";
    private static final String KEY_CONTACT_WEBSITE = "contact:website";
    public static final String  HTTP_PREFIX         = "http://";
    public static final String  HTTPS_PREFIX        = "https://";

    /**
     * Check if this is a key that expects an URL
     * 
     * @param key the key to check
     * @return true if it expects an URL
     */
    public static boolean isWebsiteKey(@Nullable final String key) {
        return Tags.KEY_WEBSITE.equals(key) || Tags.KEY_CONTACT_WEBSITE.equals(key);
    }

    // keys where the values are phone numbers
    private static final String KEY_PHONE         = "phone";
    private static final String KEY_CONTACT_PHONE = "contact:phone";
    private static final String KEY_FAX           = "fax";
    private static final String KEY_CONTACT_FAX   = "contact:fax";

    /**
     * Check if this is a key that expects a phone number
     * 
     * @param key the key to check
     * @return true if it expects a phone number
     */
    public static boolean isPhoneKey(@Nullable final String key) {
        return Tags.KEY_PHONE.equals(key) || Tags.KEY_CONTACT_PHONE.equals(key) || Tags.KEY_FAX.equals(key) || Tags.KEY_CONTACT_FAX.equals(key);
    }

    // keys where the values are e-mail addresses
    private static final String KEY_EMAIL         = "email";
    private static final String KEY_CONTACT_EMAIL = "contact:email";

    /**
     * Check if this is a key that expects an e-mail address
     * 
     * @param key the key to check
     * @return true if it expects an e-mail address
     */
    public static boolean isEmailKey(@Nullable final String key) {
        return Tags.KEY_EMAIL.equals(key) || Tags.KEY_CONTACT_EMAIL.equals(key);
    }

    // keys that are a way metric and need special handling when splitting/merging
    public static final String KEY_STEP_COUNT                           = "step_count";
    public static final String KEY_DURATION                             = "duration";
    public static final String KEY_PARKING_LANE_BOTH_CAPACITY           = "parking:lane:both:capacity";
    public static final String KEY_PARKING_LANE_LEFT_CAPACITY           = "parking:lane:left:capacity";
    public static final String KEY_PARKING_LANE_RIGHT_CAPACITY          = "parking:lane:right:capacity";
    public static final String KEY_PARKING_LANE_BOTH_CAPACITY_DISABLED  = "parking:lane:both:capacity:disabled";
    public static final String KEY_PARKING_LANE_LEFT_CAPACITY_DISABLED  = "parking:lane:left:capacity:disabled";
    public static final String KEY_PARKING_LANE_RIGHT_CAPACITY_DISABLED = "parking:lane:right:capacitydisabled";

    private static final List<String> WAY_METRIC_KEYS = Collections.unmodifiableList(
            Arrays.asList(KEY_STEP_COUNT, KEY_DURATION, KEY_PARKING_LANE_BOTH_CAPACITY, KEY_PARKING_LANE_LEFT_CAPACITY, KEY_PARKING_LANE_RIGHT_CAPACITY,
                    KEY_PARKING_LANE_BOTH_CAPACITY_DISABLED, KEY_PARKING_LANE_LEFT_CAPACITY_DISABLED, KEY_PARKING_LANE_RIGHT_CAPACITY_DISABLED));

    /**
     * Check if the key is a way metric
     * 
     * That is: depends on the length of the way and needs to be handled specially on splitting and merging
     * 
     * @param key the key
     * @return true if a way metric
     */
    public static boolean isWayMetric(@Nullable final String key) {
        return WAY_METRIC_KEYS.contains(key);
    }

    // Indoor keys
    public static final String KEY_LEVEL      = "level";
    public static final String KEY_MIN_LEVEL  = "min_level";
    public static final String KEY_MAX_LEVEL  = "max_level";
    public static final String KEY_REPEAT_ON  = "repeat_on";
    public static final String KEY_INDOOR     = "indoor";
    public static final String VALUE_ROOM     = "room";
    public static final String VALUE_CORRIDOR = "corridor";
    public static final String VALUE_AREA     = "area";
    public static final String VALUE_LEVEL    = "level";

    // S3DB
    public static final String KEY_BUILDING_PART = "building:part";

    private static final Map<String, String> IGNORE_FOR_MAP_ICONS_TEMP = new HashMap<>();
    static {
        IGNORE_FOR_MAP_ICONS_TEMP.put(KEY_BUILDING, "");
        IGNORE_FOR_MAP_ICONS_TEMP.put(KEY_BUILDING_PART, "");
        IGNORE_FOR_MAP_ICONS_TEMP.put(KEY_INDOOR, VALUE_ROOM);
    }
    public static final Map<String, String> IGNORE_FOR_MAP_ICONS = Collections.unmodifiableMap(IGNORE_FOR_MAP_ICONS_TEMP);

    //
    public static final String KEY_LAYER = "layer";

    // more primary keys
    public static final String KEY_AEROWAY            = "aeroway";
    public static final String KEY_AERIALWAY          = "aerialway";
    public static final String KEY_POWER              = "power";
    public static final String KEY_LEISURE            = "leisure";
    public static final String KEY_AMENITY            = "amenity";
    public static final String VALUE_RESTAURANT       = "restaurant";
    public static final String VALUE_FAST_FOOD        = "fast_food";
    public static final String VALUE_CAFE             = "cafe";
    public static final String VALUE_PUB              = "pub";
    public static final String VALUE_BAR              = "bar";
    public static final String VALUE_TOILETS          = "toilets";
    public static final String VALUE_ATM              = "atm";
    public static final String VALUE_VENDING_MACHINE  = "vending_machine";
    public static final String VALUE_PAYMENT_TERMINAL = "payment_terminal";

    public static final String KEY_OFFICE     = "office";
    public static final String KEY_SHOP       = "shop";
    public static final String KEY_CRAFT      = "craft";
    public static final String KEY_EMERGENCY  = "emergency";
    public static final String KEY_TOURISM    = "tourism";
    public static final String VALUE_HOTEL    = "hotel";
    public static final String VALUE_MOTEL    = "motel";
    public static final String KEY_HISTORIC   = "historic";
    public static final String KEY_MILITARY   = "military";
    public static final String KEY_PIPELINE   = "pipeline";
    public static final String KEY_HEALTHCARE = "healthcare";
    public static final String KEY_GEOLOGICAL = "geological";

    // annotations
    public static final String KEY_NOTE = "note";

    // disabled access
    public static final String KEY_WHEELCHAIR = "wheelchair";

    // more QA keys
    public static final String KEY_CHECK_DATE    = "check_date";
    public static final String CHECK_DATE_FORMAT = "yyyy-MM-dd";

    // Changeset keys
    public static final String KEY_CREATED_BY       = "created_by";
    public static final String KEY_COMMENT          = "comment";
    public static final String KEY_IMAGERY_USED     = "imagery_used";
    public static final String KEY_LOCALE           = "locale";
    public static final String KEY_REVIEW_REQUESTED = "review_requested";

    // deprecated
    public static final String KEY_IS_IN = "is_in";

    /**
     * An set of tags considered 'important'. These are typically tags that define real-world objects and not properties
     * of such.
     */
    public static final Set<String> IMPORTANT_TAGS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(KEY_HIGHWAY, KEY_BARRIER, KEY_WATERWAY, KEY_RAILWAY, KEY_AEROWAY, KEY_AERIALWAY, KEY_POWER, KEY_MAN_MADE, KEY_BUILDING,
                    KEY_LEISURE, KEY_AMENITY, KEY_OFFICE, KEY_SHOP, KEY_CRAFT, KEY_EMERGENCY, KEY_TOURISM, KEY_HISTORIC, KEY_LANDUSE, KEY_MILITARY, KEY_NATURAL,
                    KEY_BOUNDARY, KEY_PLACE, KEY_TYPE, KEY_ENTRANCE, KEY_PIPELINE, KEY_HEALTHCARE, KEY_GEOLOGICAL, KEY_ADDR_HOUSENUMBER, KEY_ADDR_HOUSENAME)));
    /** ways that we might want to render differently */
    public static final Set<String> WAY_TAGS       = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(KEY_BUILDING, KEY_RAILWAY, KEY_LEISURE, KEY_LANDUSE, KEY_WATERWAY, KEY_NATURAL, KEY_ADDR_INTERPOLATION,
                    KEY_BOUNDARY, KEY_MAN_MADE, KEY_AMENITY, KEY_SHOP, KEY_POWER, KEY_AERIALWAY, KEY_MILITARY, KEY_HISTORIC, KEY_INDOOR, KEY_BUILDING_PART)));
    /** relations that we might want to render differently */
    public static final Set<String> RELATION_TAGS  = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(KEY_BOUNDARY, KEY_LEISURE, KEY_LANDUSE, KEY_NATURAL, KEY_WATERWAY, KEY_BUILDING, KEY_MAN_MADE)));

    /**
     * Private constructor to avoid getting a public one
     */
    private Tags() {
    }
}
