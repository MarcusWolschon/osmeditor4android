package de.blau.android.propertyeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;

import android.util.Log;
import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.propertyeditor.PropertyEditor;

/**
 * Store coordinates and address information for use in address prediction
 * @author simon
 *
 */
public class Address implements Serializable {
	private static final long serialVersionUID = 3L;
	
	private static final String ADDRESS_TAGS_FILE = "addresstags.dat";
	private static final int MAX_SAVED_ADDRESSES = 100;
	
	private static SavingHelper<LinkedList<Address>> savingHelperAddress
	= new SavingHelper<LinkedList<Address>>();
	
	public enum Side {
		LEFT,
		RIGHT,
		UNKNOWN
	}
	Side side = Side.UNKNOWN;
	float lat;
	float lon;
	LinkedHashMap<String, String> tags;
	
	private static LinkedList<Address> lastAddresses = null;
	
	Address(Address a) {
		side = a.side;
		lat = a.lat;
		lon = a.lon;
		tags = new LinkedHashMap<String, String>(a.tags);
	}
	
	Address() {
		tags = new LinkedHashMap<String, String>();
	}
	
	Address(String type, long id, LinkedHashMap<String, String> tags) {
		OsmElement e = Main.getLogic().getDelegator().getOsmElement(type, id);
		switch (e.getType()) {
		case NODE: lat = ((Node)e).getLat()/1E7F; lon = ((Node)e).getLon()/1E7F; break;
		case WAY:
		case CLOSEDWAY:
			de.blau.android.Map map = Application.mainActivity.getMap();
			int[] center = Logic.centroid(map.getWidth(), map.getHeight(), map.getViewBox(), (Way)e);
			if (center != null) { 
				lat = center[0]/1E7F;
				lon = center[1]/1E7F;
			}
			break;
		case RELATION:
			// doing nothing is probably best for now
		default:
			break;
		}
		this.tags = new LinkedHashMap<String, String>(tags);
	}
	
	/**
	 * Set which side this of the road this address is on
	 * @param wayId
	 * @return
	 */
	void setSide(long wayId) {
		side = Side.UNKNOWN;
		Way w = (Way)Main.getLogic().getDelegator().getOsmElement(Way.NAME,wayId);
		if (w == null) {
			return;
		}
		double distance = Double.MAX_VALUE;
		
		// to avoid rounding errors we translate the bb to 0,0
		BoundingBox bb = Application.mainActivity.getMap().getViewBox();
		double latOffset = GeoMath.latE7ToMercatorE7(bb.getBottom());
		double lonOffset = bb.getLeft();
		double ny = GeoMath.latToMercator(lat)-latOffset/1E7D;
		double nx = lon - lonOffset/1E7D;
		
		ArrayList<Node> nodes = new ArrayList<Node>(w.getNodes());
		for (int i = 0;i <= nodes.size()-2;i++) {
			double bx = (nodes.get(i).getLon()-lonOffset)/1E7D;
			double by = (GeoMath.latE7ToMercatorE7(nodes.get(i).getLat())-latOffset )/1E7D;
			double ax = (nodes.get(i+1).getLon()-lonOffset)/1E7D;
			double ay = (GeoMath.latE7ToMercatorE7(nodes.get(i+1).getLat())-latOffset)/1E7D;
			float[] closest = GeoMath.closestPoint((float)nx, (float)ny, (float)bx, (float)by, (float)ax, (float)ay);
			double newDistance = GeoMath.haversineDistance(nx, ny, closest[0], closest[1]);
			if (newDistance < distance) {
				distance = newDistance;
				double determinant = (bx-ax)*(ny-ay) - (by-ay)*(nx-ax);
				if (determinant < 0) {
					side = Side.LEFT;
				} else if (determinant > 0) {
					side = Side.RIGHT;
				}
			}
		}
	}
	
	Side getSide() {
		return side;
	}
	
	/**
	 * Predict address tags
	 * This uses a file to cache/save the address information over invocations of the TagEditor, if the cache doesn't have entries for a specific street/place 
	 * an attempt tp extract the information from the downloaded data is made
	 * @param caller TODO
	 */
	protected static LinkedHashMap<String,String> predictAddressTags(TagEditorFragment caller, final LinkedHashMap<String, String> current) {
		Address newAddress = null;
			
		try {
			lastAddresses = savingHelperAddress.load(ADDRESS_TAGS_FILE, false);
			Log.d("TagEditor","doAddressTags read " + lastAddresses.size() + " addresses");
		} catch (Exception e) {
			//TODO be more specific
		}
		StreetTagValueAutocompletionAdapter streetAdapter = (StreetTagValueAutocompletionAdapter) caller.getStreetNameAutocompleteAdapter(null);
		// PlaceTagValueAutocompletionAdapter placeAdapter = (PlaceTagValueAutocompletionAdapter) getPlaceNameAutocompleteAdapter();
		if (lastAddresses != null && lastAddresses.size() > 0) {
			newAddress = new Address(caller.getType(), caller.getOsmId(),lastAddresses.get(0).tags); // last address we added
		} 

		if (newAddress == null) { // make sure we have the address object
			newAddress = new Address(caller.getType(), caller.getOsmId(), new LinkedHashMap<String, String>()); 
		}
		// merge in any existing tags
		for (String k: current.keySet()) {
			newAddress.tags.put(k, current.get(k));
		}
		boolean hasPlace = newAddress.tags.containsKey(Tags.KEY_ADDR_PLACE);
		if (streetAdapter != null || hasPlace) {
			// the auto completion arrays should now be calculated, retrieve street names if any
			ArrayList<String> streetNames = new ArrayList<String>(Arrays.asList(streetAdapter.getNames()));
			// ArrayList<String> placeNames = new ArrayList<String>(Arrays.asList(placeAdapter.getNames()));		
			if ((streetNames != null && streetNames.size() > 0) || hasPlace) {
				LinkedHashMap<String, String> tags = newAddress.tags;
				Log.d("TagEditor","tags.get(Tags.KEY_ADDR_STREET)) " + tags.get(Tags.KEY_ADDR_STREET));
				Log.d("TagEditor","Rank of " + tags.get(Tags.KEY_ADDR_STREET) + " " + streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
				String street;		
				if (!hasPlace) {
					if (!newAddress.tags.containsKey(Tags.KEY_ADDR_STREET) 
							|| newAddress.tags.get(Tags.KEY_ADDR_STREET).equals("") 
							|| streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)) > 2 
							|| streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)) < 0)  { // check if has street and still in the top 3
						Log.d("TagEditor","names.indexOf(tags.get(Tags.KEY_ADDR_STREET)) " + streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
						// nope -> zap
						tags.put(Tags.KEY_ADDR_STREET, streetNames.get(0));
						if (tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
						}
					}
					street = tags.get(Tags.KEY_ADDR_STREET); // should now have the final suggestion for a street
					try {
						newAddress.setSide(streetAdapter.getId(street));
					} catch (OsmException e) { // street not in adapter
						newAddress.side = Side.UNKNOWN;
					}
				} else { // ADDR_PLACE minimal support, don't overwrite with street
					street = tags.get(Tags.KEY_ADDR_PLACE);
					newAddress.side = Side.UNKNOWN;
				}
				Log.d("TagEditor","side " + newAddress.getSide());
				Side side = newAddress.getSide();
				// find the addresses corresponding to the current street
				if (street != null && lastAddresses != null) {
					TreeMap<Integer,Address> list = getHouseNumbers(street, side, lastAddresses);
					if (list.size() == 0) { // try to seed lastAddresses from OSM data
						try {
							Log.d("TagEditor","street " + street);
							long streetId = -1;
							if  (!hasPlace) {
								streetId = streetAdapter.getId(street);
							}
							// nodes
							for (Node n:Main.getLogic().getDelegator().getCurrentStorage().getNodes()) {
								seedAddressList(street,streetId,(OsmElement)n,lastAddresses);
							}
							// ways
							for (Way w:Main.getLogic().getDelegator().getCurrentStorage().getWays()) {
								seedAddressList(street,streetId,(OsmElement)w,lastAddresses);
							}
							// and try again
							list = getHouseNumbers(street, side, lastAddresses);

						} catch (OsmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// try to predict the next number
					//
					// - get all existing numbers for the side of the street we are on
					// - determine if the increment per number is 1 or 2 (for now everything else is ignored)
					// - determine the nearest address node
					// - if it is the last or first node and we are at one side use that and add or subtract the increment
					// - if the nearest node is somewhere in the middle determine on which side of it we are, 
					// - inc/dec in that direction
					// If everything works out correctly even if a prediction is wrong, entering the correct number should improve the next prediction
					//TODO the following assumes that the road is not doubling back or similar, aka that the addresses are more or less in a straight line, 
					//     use the length along the way defined by the addresses instead
					//
					if (list.size() >= 2) {
						try {
							int firstNumber = list.firstKey();
							int lastNumber = list.lastKey();
							// 
							int inc = 1;
							float incTotal = 0;
							float incCount = 0;
							ArrayList<Integer> numbers = new ArrayList<Integer>(list.keySet());
							for (int i=0;i<numbers.size()-1;i++) {
								int diff = numbers.get(i+1).intValue()-numbers.get(i).intValue();
								if (diff > 0 && diff <= 2) {
									incTotal = incTotal + diff;
									incCount++;
								}
							}
							inc = Math.round(incTotal/incCount);
							int nearest = -1; 
							int prev = -1;
							int post = -1;
							double distanceFirst = 0;
							double distanceLast = 0;
							double distance = Double.MAX_VALUE;
							for (int i=0;i<numbers.size();i++) {
								// determine the nearest existing address
								int number = Integer.valueOf(numbers.get(i));
								Address a = list.get(number);
								double newDistance = GeoMath.haversineDistance(newAddress.lon, newAddress.lat, a.lon, a.lat);
								if (newDistance < distance) {
									distance = newDistance;
									nearest = number;
									prev = numbers.get(Math.max(0, i-1));
									post = numbers.get(Math.min(numbers.size()-1, i+1));
								}
								if (i==0) {
									distanceFirst = newDistance;
								} else if (i==numbers.size()-1) {
									distanceLast = newDistance;
								}
							}
							//
							double distanceTotal = GeoMath.haversineDistance(list.get(firstNumber).lon, list.get(firstNumber).lat, list.get(lastNumber).lon, list.get(lastNumber).lat);
							if (nearest == firstNumber) { 
								if (distanceLast > distanceTotal) {
									inc = -inc;
								}
							} else if (nearest == lastNumber) { 
								if (distanceFirst < distanceTotal) {
									inc = -inc;
								}
							} else {
								double distanceNearestFirst = GeoMath.haversineDistance(list.get(firstNumber).lon, list.get(firstNumber).lat, list.get(nearest).lon, list.get(nearest).lat);
								if (distanceFirst < distanceNearestFirst) {
									inc = -inc;
								} // else already correct
							} 
							// Toast.makeText(this, "First " + firstNumber + " last " + lastNumber + " nearest " + nearest + "inc " + inc + " prev " + prev + " post " + post + " side " + side, Toast.LENGTH_LONG).show();
							Log.d("TagEditor","First " + firstNumber + " last " + lastNumber + " nearest " + nearest + " inc " + inc + " prev " + prev + " post " + post + " side " + side);
							// first apply tags from nearest address if they don't already exist
							for (String key:list.get(nearest).tags.keySet()) {
								if (!tags.containsKey(key)) {
									tags.put(key,list.get(nearest).tags.get(key));
								}
							}
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "" + Math.max(1, nearest+inc));
						} catch (NumberFormatException nfe){
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
						}
					} else if (list.size() == 1) {
						// can't do prediction with only one value 
						// apply tags from sole existing address if they don't already exist
						for (String key:list.get(list.firstKey()).tags.keySet()) {
							if (!tags.containsKey(key)) {
								tags.put(key,list.get(list.firstKey()).tags.get(key));
							}
						}
					}
				}
			} else { // last ditch attemot
				LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>();
				// fill with Karlsruher schema
				tags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
				tags.put(Tags.KEY_ADDR_STREET, "");
				tags.put(Tags.KEY_ADDR_POSTCODE, "");
				tags.put(Tags.KEY_ADDR_CITY, "");
				tags.put(Tags.KEY_ADDR_COUNTRY, "");
				tags.put(Tags.KEY_ADDR_FULL, "");
				// the following are less used but may be necessary
				tags.put(Tags.KEY_ADDR_HOUSENAME, "");
				tags.put(Tags.KEY_ADDR_PLACE, "");
				tags.put(Tags.KEY_ADDR_HAMLET, "");
				tags.put(Tags.KEY_ADDR_SUBURB, "");
				tags.put(Tags.KEY_ADDR_SUBDISTRICT, "");
				tags.put(Tags.KEY_ADDR_DISTRICT, "");
				tags.put(Tags.KEY_ADDR_PROVINCE, "");
				tags.put(Tags.KEY_ADDR_STATE, "");
				tags.put(Tags.KEY_ADDR_FLATS, "");
				tags.put(Tags.KEY_ADDR_DOOR, "");
				tags.put(Tags.KEY_ADDR_UNIT, "");
				newAddress.tags.putAll(tags); 
			}
		}
		
		// is this a node on a building outline, if yes add entrance=yes if it doesn't already exist
		if (caller.getType().equals(Node.NAME)) {
			boolean isOnBuilding = false;
			for (Way w:Main.getLogic().getWaysForNode((Node)Main.getLogic().getDelegator().getOsmElement(Node.NAME, caller.getOsmId()))) {
				if (w.hasTagKey(Tags.KEY_BUILDING)) {
					isOnBuilding = true;
				} else if (w.getParentRelations() != null) { // need to check relations too
					for (Relation r:w.getParentRelations()) {
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
				newAddress.tags.put(Tags.KEY_ENTRANCE, "yes");
			}
		}
		return newAddress.tags;
	}
	
	private static int getNumber(String hn) throws NumberFormatException {
		StringBuffer sb = new StringBuffer();
		for (Character c:hn.toCharArray()) {
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
	 * Return a sorted list of house numbers and the associated address objects
	 * @param street
	 * @param side
	 * @param lastAddresses
	 * @return
	 */
	private static TreeMap<Integer,Address> getHouseNumbers(String street, Address.Side side, LinkedList<Address> lastAddresses ) {
		TreeMap<Integer,Address> result = new TreeMap<Integer,Address>(); //list sorted by house numbers
		for (Address a:lastAddresses) {
			if (a != null && a.tags != null 
					&& ((a.tags.get(Tags.KEY_ADDR_STREET) != null && a.tags.get(Tags.KEY_ADDR_STREET).equals(street)) 
							|| (a.tags.get(Tags.KEY_ADDR_PLACE) != null && a.tags.get(Tags.KEY_ADDR_PLACE).equals(street)))
					&& a.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)
					&& a.getSide() == side) {
				Log.d("TagEditor","Number " + a.tags.get(Tags.KEY_ADDR_HOUSENUMBER));
				String[] numbers = a.tags.get(Tags.KEY_ADDR_HOUSENUMBER).split("\\,");
				for (String n:numbers) {
					Log.d("TagEditor","add number  " + n);
					try {
						result.put(Integer.valueOf(getNumber(n)),a);
					} catch (NumberFormatException nfe){
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Add an address from OSM data to the address cache
	 * @param street
	 * @param streetId
	 * @param e
	 * @param addresses
	 */
	private static void seedAddressList(String street,long streetId, OsmElement e,LinkedList<Address> addresses) {
		if (e.hasTag(Tags.KEY_ADDR_STREET, street) && e.hasTagKey(Tags.KEY_ADDR_HOUSENUMBER)) {
			Address seed = new Address(e.getName(), e.getOsmId(),getAddressTags(new LinkedHashMap<String,String>(e.getTags())));
			if (streetId > 0) {
				seed.setSide(streetId);
			}
			if (addresses.size() >= MAX_SAVED_ADDRESSES) { //arbitrary limit for now
				addresses.removeLast();
			}
			addresses.addFirst(seed);
			Log.d("TagEditor","seedAddressList added " + seed.tags.toString());
		}
	}
	
	protected static LinkedHashMap<String,String> getAddressTags(LinkedHashMap<String,String> sortedMap) {
		LinkedHashMap<String,String> result = new LinkedHashMap<String,String>();
		for (String key:sortedMap.keySet()) {
			// include everything except interpolation related tags
			if (key.startsWith(Tags.KEY_ADDR_BASE) && !key.startsWith(Tags.KEY_ADDR_INTERPOLATION) && !key.startsWith(Tags.KEY_ADDR_HOUSENAME) && !key.startsWith(Tags.KEY_ADDR_INCLUSION)) {
				result.put(key, sortedMap.get(key));
			}
		}
		return result;
	}
	
	protected static void resetLastAddresses() {
		savingHelperAddress.save(ADDRESS_TAGS_FILE, new LinkedList<Address>(), false);
		lastAddresses = null;
	}
	
	protected static void updateLastAddresses(TagEditorFragment caller, LinkedHashMap<String,String> tags) {
		// save any address tags for "last address tags"
		LinkedHashMap<String,String> addressTags = getAddressTags(tags);
		// this needs to be done after the edit again in case the street name of what ever has changes 
		if (addressTags.size() > 0) {
			if (lastAddresses == null) {
				lastAddresses = new LinkedList<Address>();
			}
			if (lastAddresses.size() >= MAX_SAVED_ADDRESSES) { //arbitrary limit for now
				lastAddresses.removeLast();
			}
			Address current = new Address(caller.getType(), caller.getOsmId(), addressTags);
			StreetTagValueAutocompletionAdapter streetAdapter = (StreetTagValueAutocompletionAdapter)caller.getStreetNameAutocompleteAdapter(null);
			if (streetAdapter!= null) {
				String streetName = tags.get(Tags.KEY_ADDR_STREET);
				if (streetName != null) {
					try {
						current.setSide(streetAdapter.getId(streetName));
					} catch (OsmException e) {
						current.side = Side.UNKNOWN;
					}
				}
			}
			lastAddresses.addFirst(current);
			savingHelperAddress.save(ADDRESS_TAGS_FILE, lastAddresses, false);
		}
	}
	
	protected static void saveLastAddresses() {
		if (lastAddresses != null) {
			savingHelperAddress.save(ADDRESS_TAGS_FILE, lastAddresses, false);
		}
	}
	
	protected static void loadLastAddresses() {
		if (lastAddresses == null) {
			try {
				lastAddresses = savingHelperAddress.load(ADDRESS_TAGS_FILE, false);
				Log.d("TagEditor","onResume read " + lastAddresses.size() + " addresses");
			} catch (Exception e) {
				//TODO be more specific
			}
		}
	}
}
