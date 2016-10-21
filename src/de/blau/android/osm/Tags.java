package de.blau.android.osm;

import java.util.Arrays;
import java.util.List;

/**
 * Key and value constants for tags that are used in the code
 * @author simon
 *
 */
public class Tags {
		
	// Karlsruher schema
	public static final String KEY_ADDR_BASE = "addr:";
	public static final String KEY_ADDR_HOUSENUMBER = "addr:housenumber";
	public static final String KEY_ADDR_STREET = "addr:street";
	public static final String KEY_ADDR_POSTCODE = "addr:postcode";
	public static final String KEY_ADDR_CITY = "addr:city";
	public static final String KEY_ADDR_COUNTRY = "addr:country";
	public static final String KEY_ADDR_FULL = "addr:full";
	// the following are less used but may be necessary
	public static final String KEY_ADDR_HOUSENAME = "addr:housename";
	public static final String KEY_ADDR_PLACE = "addr:place";
	public static final String KEY_ADDR_HAMLET = "addr:hamlet";
	public static final String KEY_ADDR_SUBURB = "addr:suburb";
	public static final String KEY_ADDR_SUBDISTRICT = "addr:subdistrict";
	public static final String KEY_ADDR_DISTRICT = "addr:district";
	public static final String KEY_ADDR_PROVINCE = "addr:province";
	public static final String KEY_ADDR_STATE = "addr:state";
	public static final String KEY_ADDR_FLATS = "addr:flats";
	public static final String KEY_ADDR_DOOR = "addr:door";
	public static final String KEY_ADDR_UNIT = "addr:unit";
	// address interpolation
	public static final String KEY_ADDR_INTERPOLATION = "addr:interpolation";
	public static final String VALUE_ODD = "odd";
	public static final String VALUE_EVEN = "even";
	public static final String KEY_ADDR_INCLUSION = "addr:inclusion";
	public static final String VALUE_ACTUAL = "actual";
	public static final String VALUE_ESTIMATE = "estimate";
	public static final String VALUE_POTENTIAL = "potential";
	// other address related stuff
	public static final String KEY_BUILDING = "building";
	public static final String KEY_ENTRANCE = "entrance";
	//
	public static final String KEY_NAME = "name";
	public static final String KEY_OFFICIAL_NAME = "official_name";
	public static final String KEY_ALT_NAME = "alt_name";
	public static final String KEY_LOC_NAME = "loc_name";
	public static final String KEY_OLD_NAME = "old_name";
	public static final String KEY_SHORT_NAME = "short_name";
	public static final String KEY_REG_NAME = "reg_name";
	public static final String KEY_NAT_NAME = "nat_name";
	public static final String KEY_INT_NAME = "int_name";
	public static final List<String> I18N_NAME_KEYS = Arrays.asList(KEY_NAME, KEY_OFFICIAL_NAME,
			KEY_ALT_NAME, KEY_LOC_NAME, KEY_SHORT_NAME, KEY_REG_NAME, KEY_NAT_NAME); 
    
	public static final String KEY_NAME_LEFT = "name:left";
	public static final String KEY_NAME_RIGHT= "name:right";
	public static final String KEY_REF = "ref";
	public static final String KEY_NONAME = "noname";
	public static final String KEY_VALIDATE_NO_NAME = "validate:no_name";
	public static final String KEY_HIGHWAY = "highway";
	public static final String VALUE_ROAD = "road";
	public static final String KEY_BARRIER = "barrier";
	public static final String VALUE_RETAINING_WALL = "retaining_wall";
	public static final String VALUE_KERB = "kerb";
	public static final String VALUE_GUARD_RAIL = "guard_rail";
	public static final String VALUE_CITY_WALL = "city_wall";
	public static final String KEY_TWO_SIDED = "two_sided";
	public static final String KEY_MAN_MADE ="man_made";
	public static final String VALUE_EMBANKMENT = "embankment";
	public static final String KEY_ONEWAY = "oneway";
	public static final String KEY_WATERWAY = "waterway"; 
	public static final String KEY_LANDUSE = "landuse";
	public static final String KEY_NATURAL = "natural";
	public static final String VALUE_CLIFF = "cliff";
	public static final String VALUE_COASTLINE = "coastline";
	public static final String KEY_PLACE = "place";
	
	
	public static final String KEY_RAILWAY = "railway";
	//
	public static final String KEY_SOURCE = "source";
	public static final String VALUE_SURVEY = "survey";
	public static final String VALUE_GPS = "GPS";
	// for relations
	public static final String KEY_TYPE = "type";
	public static final String VALUE_RESTRICTION = "restriction";
	public static final String VALUE_NO_U_TURN = "no_u_turn";
	public static final String VALUE_VIA = "via";
	public static final String VALUE_BUILDING = "building";
	public static final String VALUE_ROUTE = "route";
	public static final String VALUE_MULTIPOLYGON = "multipolygon";
	public static final String VALUE_BOUNDARY = "boundary";
	public static final String KEY_BOUNDARY = "boundary";
	
	//
	public static final String KEY_ELE = "ele";
	public static final String KEY_ELE_MSL = "ele:msl";
	public static final String KEY_SOURCE_ELE = "ele:source";
	//
	public static final String VALUE_YES = "yes";
	//
	public static final String KEY_WIKIPEDIA = "wikipedia";
	public static final String KEY_WIKIDATA = "wikidata";
	
	public static final String KEY_OPENING_HOURS = "opening_hours";
	
	public static final String KEY_CONDITIONAL_SUFFIX = ":conditional";
	
	// keys were the values are URLs
	public static final String KEY_WEBSITE = "website";
	public static final String KEY_CONTACT_WEBSITE = "contact:website";
	public static boolean isWebsiteKey(final String key) {
		return Tags.KEY_WEBSITE.equals(key)||Tags.KEY_CONTACT_WEBSITE.equals(key);
	}
	
	
}
