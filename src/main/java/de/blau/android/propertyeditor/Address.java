package de.blau.android.propertyeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import de.blau.android.Application;
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
 * @author simon
 *
 */
public class Address implements Serializable {
	private static final long serialVersionUID = 5L;
	
	private static final String DEBUG_TAG = Address.class.getSimpleName();
	
	public static final int NO_HYSTERESIS = 0;
	public static final int DEFAULT_HYSTERESIS = 2;
	
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
	LinkedHashMap<String, ArrayList<String>> tags;
	
	private static LinkedList<Address> lastAddresses = null;
	
	/**
	 * Create a copy of Address a
	 * @param a
	 */
	Address(Address a) {
		side = a.side;
		lat = a.lat;
		lon = a.lon;
		tags = new LinkedHashMap<String, ArrayList<String>>(a.tags);
	}
	
	/**
	 * Create empty address object
	 */
	Address() {
		tags = new LinkedHashMap<String, ArrayList<String>>();
	}
	
	/**
	 * Create an address object from an OSM element
	 * @param type type of element	
	 * @param id its ID
	 * @param tags the relevant address tags
	 */
	Address(String type, long id, LinkedHashMap<String, ArrayList<String>> tags) {
		OsmElement e = Application.getDelegator().getOsmElement(type, id);
		if (e == null) {
			Log.e(DEBUG_TAG,type + " " + id + " doesn't exist in storage ");
			//FIXME is might make sense to create a crash dump here
			return;
		}
		init(e,tags);
	}
	
	/**
	 * Create an address object from an OSM element 
	 * @param e the OSM element
	 * @param tags the relevant address tags
	 */
	Address(OsmElement e, LinkedHashMap<String, ArrayList<String>> tags) {
		init(e,tags);
	}
	
	/**
	 * Initialize an address object from an OSM element 
	 * @param e the OSM element
	 * @param tags the relevant address tags
	 */
	private void init(OsmElement e, LinkedHashMap<String, ArrayList<String>> tags) {
		switch (e.getType()) {
		case NODE: lat = ((Node)e).getLat()/1E7F; lon = ((Node)e).getLon()/1E7F; 
		break;
		case WAY:
		case CLOSEDWAY:
		case AREA:
			if (Way.NAME.equals(e.getName())) {
				double[] center = Logic.centroidLonLat((Way)e);
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
		this.tags = new LinkedHashMap<String, ArrayList<String>>(tags);
	}

	/**
	 * Set which side of the road this address is on
	 * @param wayId
	 * @return
	 */
	void setSide(long wayId) {
		side = Side.UNKNOWN;
		Way w = (Way)Application.getDelegator().getOsmElement(Way.NAME,wayId);
		if (w == null) {
			return;
		}
		double distance = Double.MAX_VALUE;
		
		// to avoid rounding errors we translate the bb to 0,0
		BoundingBox bb = w.getBounds(); 
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
		// Log.d(DEBUG_TAG,"set side to " + side);
	}
	
	Side getSide() {
		return side;
	}
	
	/**
	 * Predict address tags
	 * This uses a file to cache/save the address information over invocations of the TagEditor, if the cache doesn't have entries for a specific street/place 
	 * an attempt to extract the information from the downloaded data is made
	 *
	 * @param elementType
	 * @param elementOsmId
	 * @param es
	 * @param current
	 * @param maxRank determines how far away from the nearest street the last address street can be, 0 will always use the nearest, higher numbers will provide some hysteresis
	 * @return
	 */
	public synchronized static LinkedHashMap<String,ArrayList<String>> predictAddressTags(Context context, final String elementType, final long elementOsmId, final ElementSearch es, final LinkedHashMap<String, ArrayList<String>> current, int maxRank) {
		Address newAddress = null;
			
		loadLastAddresses(context);
		
		if (lastAddresses != null && lastAddresses.size() > 0) {
			newAddress = new Address(elementType, elementOsmId,lastAddresses.get(0).tags); // last address we added
			if (newAddress.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
				newAddress.tags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(""));
			}
			Log.d("Address","seeding with last addresses");
		} 

		if (newAddress == null) { // make sure we have the address object
			newAddress = new Address(elementType, elementOsmId, new LinkedHashMap<String, ArrayList<String>>()); 
			Log.d("Address","nothing to seed with, creating new");
		}
		// merge in any existing tags
		for (String k: current.keySet()) {
			Log.d("Address","Adding in existing tag " + k);
			newAddress.tags.put(k, current.get(k));
		}
		boolean hasPlace = newAddress.tags.containsKey(Tags.KEY_ADDR_PLACE);
		boolean hasNumber = current.containsKey(Tags.KEY_ADDR_HOUSENUMBER); // if the object already had a number don't overwrite it
		StorageDelegator storageDelegator = Application.getDelegator();
		if (es != null /* || hasPlace */) {
			// the arrays should now be calculated, retrieve street names if any
			ArrayList<String> streetNames = new ArrayList<String>(Arrays.asList(es.getStreetNames()));
			// ArrayList<String> placeNames = new ArrayList<String>(Arrays.asList(placeAdapter.getNames()));		
			if ((streetNames != null && streetNames.size() > 0) || hasPlace) {
				LinkedHashMap<String, ArrayList<String>> tags = newAddress.tags;
				Log.d(DEBUG_TAG,"tags.get(Tags.KEY_ADDR_STREET)) " + tags.get(Tags.KEY_ADDR_STREET));
				// Log.d("TagEditor","Rank of " + tags.get(Tags.KEY_ADDR_STREET) + " " + streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
				String street;		
				if (!hasPlace) {
					ArrayList<String> addrStreetValues = tags.get(Tags.KEY_ADDR_STREET);
					int rank = -1;
					boolean hasAddrStreet =  addrStreetValues != null && addrStreetValues.size() > 0 && !addrStreetValues.get(0).equals("");
					if (hasAddrStreet) {
						rank = streetNames.indexOf(addrStreetValues.get(0)); // FIXME this and the following could consider other values in multi select
					}
					Log.d(DEBUG_TAG, (hasAddrStreet ? "rank " + rank + " for " +  addrStreetValues.get(0) : "no addrStreet tag"));
					if (!hasAddrStreet || rank > maxRank  || rank < 0)  { // check if has street and still in the top 3 nearest
						// Log.d("TagEditor","names.indexOf(tags.get(Tags.KEY_ADDR_STREET)) " + streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
						// nope -> zap
						tags.put(Tags.KEY_ADDR_STREET, Util.getArrayList(streetNames.get(0)));
					}
					addrStreetValues = tags.get(Tags.KEY_ADDR_STREET);
					if (addrStreetValues != null && addrStreetValues.size() > 0) {
						street = tags.get(Tags.KEY_ADDR_STREET).get(0); // should now have the final suggestion for a street
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
					if (addrPlaceValues != null && addrPlaceValues.size() > 0) {
						street = tags.get(Tags.KEY_ADDR_PLACE).get(0);
					} else {
						street = ""; // FIXME
					}
					newAddress.side = Side.UNKNOWN;
				}
				Log.d(DEBUG_TAG,"side " + newAddress.getSide());
				Side side = newAddress.getSide();
				// find the addresses corresponding to the current street
				if (!hasNumber && street != null && lastAddresses != null) {
					TreeMap<Integer,Address> list = getHouseNumbers(street, side, lastAddresses);
					if (list.size() == 0) { // try to seed lastAddresses from OSM data
						try {
							Log.d(DEBUG_TAG,"street " + street);
							long streetId = -1;
							if  (!hasPlace) {
								streetId = es.getStreetId(street);
							}
							// nodes
							for (Node n: storageDelegator.getCurrentStorage().getNodes()) {
								seedAddressList(street,streetId, n,lastAddresses);
							}
							// ways
							for (Way w: storageDelegator.getCurrentStorage().getWays()) {
								seedAddressList(street,streetId, w,lastAddresses);
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
					// TODO the above assumes that the road is not doubling back or similar, aka that the addresses are more or less in a straight line, 
					//     use the length along the way defined by the addresses instead
					//
					if (list.size() >= 2) {
						try {
							int firstNumber = list.firstKey();
							int lastNumber = list.lastKey();
							//
							// determine increment
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
							//
							// find the most appropriate next address
							//
							int nearest = -1; 
							int prev = -1;
							int post = -1;
							double distanceFirst = 0;
							double distanceLast = 0;
							double distance = Double.MAX_VALUE;
							for (int i=0;i<numbers.size();i++) {
								// determine the nearest existing address
								// FIXME there is an obvious better criteria
								int number = Integer.valueOf(numbers.get(i));
								Address a = list.get(number);
								double newDistance = GeoMath.haversineDistance(newAddress.lon, newAddress.lat, a.lon, a.lat);
								if (newDistance <= distance) { 
									// if distance is the same replace with values for the 
									// current number which will be larger
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
							int newNumber = Math.max(1, nearest+inc);
							if (numbers.contains(Integer.valueOf(newNumber))) {
								// try one inc more and one less, if they both fail use the original number
								if (!numbers.contains(Integer.valueOf(Math.max(1,newNumber+inc)))) {
									newNumber = Math.max(1,newNumber+inc);
								} else if (!numbers.contains(Integer.valueOf(Math.max(1,newNumber-inc)))) {
									newNumber = Math.max(1,newNumber-inc);
								}
							}
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList("" + newNumber));
						} catch (NumberFormatException nfe){
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(""));
						}
					} else if (list.size() == 1) {
						// can't do prediction with only one value 
						// apply tags from sole existing address if they don't already exist
						for (String key:list.get(list.firstKey()).tags.keySet()) {
							if (!tags.containsKey(key)) {
								tags.put(key,list.get(list.firstKey()).tags.get(key));
							}
						}
					} else if (list.size() == 0) {
						tags.put(Tags.KEY_ADDR_HOUSENUMBER, Util.getArrayList(""));
						// NOTE this could be the first address on this side of the road and we could
						// potentially use the house numbers from the opposite side for prediction
					}
				}
			} else { // last ditch attemot
				// fill with Karlsruher schema
				Preferences prefs = new Preferences(Application.getCurrentApplication());
				Set<String> addressTags = prefs.addressTags();
				for (String key:addressTags) {
					newAddress.tags.put(key, Util.getArrayList(""));
				}
			}
		}
		
		// is this a node on a building outline, if yes add entrance=yes if it doesn't already exist
		if (elementType.equals(Node.NAME)) {
			boolean isOnBuilding = false;
			// we can't call wayForNodes here because Logic may not be around
			for (Way w: storageDelegator.getCurrentStorage().getWays((Node) storageDelegator.getOsmElement(Node.NAME, elementOsmId))) {
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
				newAddress.tags.put(Tags.KEY_ENTRANCE, Util.getArrayList("yes"));
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
	private synchronized static TreeMap<Integer,Address> getHouseNumbers(String street, Address.Side side, LinkedList<Address> lastAddresses ) {
		TreeMap<Integer,Address> result = new TreeMap<Integer,Address>(); //list sorted by house numbers
		for (Address a:lastAddresses) {
			if (a != null && a.tags != null) {
				ArrayList<String> addrStreetValues = a.tags.get(Tags.KEY_ADDR_STREET);
				ArrayList<String> addrPlaceValues = a.tags.get(Tags.KEY_ADDR_PLACE);
				if ( ((addrStreetValues != null && addrStreetValues.size() > 0 && addrStreetValues.get(0).equals(street)) // FIXME 
						|| (addrPlaceValues != null && addrPlaceValues.size() > 0 && addrPlaceValues.get(0).equals(street)))
						&& a.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)
						&& a.getSide() == side) {
					Log.d(DEBUG_TAG,"Number " + a.tags.get(Tags.KEY_ADDR_HOUSENUMBER));
					ArrayList<String> addrHousenumberValues = a.tags.get(Tags.KEY_ADDR_HOUSENUMBER);
					if ( addrHousenumberValues != null && addrHousenumberValues.size()>0) {
						String[] numbers =  addrHousenumberValues.get(0).split("[\\,;\\-]");
						for (String n:numbers) {
							Log.d(DEBUG_TAG,"add number  " + n);
							//noinspection EmptyCatchBlock
							try {
								result.put(Integer.valueOf(getNumber(n)),a);
							} catch (NumberFormatException nfe){
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
	 * @param street
	 * @param streetId
	 * @param e
	 * @param addresses
	 */
	private static void seedAddressList(String street,long streetId, OsmElement e,LinkedList<Address> addresses) {
		if (e.hasTag(Tags.KEY_ADDR_STREET, street) && e.hasTagKey(Tags.KEY_ADDR_HOUSENUMBER)) {
			Address seed = new Address(e,getAddressTags(new LinkedHashMap<String,ArrayList<String>>(Util.getArrayListMap(e.getTags()))));
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
	
	protected static LinkedHashMap<String,ArrayList<String>> getAddressTags(LinkedHashMap<String,ArrayList<String>> sortedMap) {
		LinkedHashMap<String,ArrayList<String>> result = new LinkedHashMap<String,ArrayList<String>>();
		Preferences prefs = new Preferences(Application.getCurrentApplication());
		Set<String> addressTags = prefs.addressTags();
		for (String key:sortedMap.keySet()) {
			// include everything except interpolation related tags
			if (addressTags.contains(key)) {
				result.put(key, sortedMap.get(key));
			}
		}
		return result;
	}
	
	protected synchronized static void resetLastAddresses(Context context) {
		savingHelperAddress.save(context, ADDRESS_TAGS_FILE, new LinkedList<Address>(), false);
		lastAddresses = null;
	}
	
	protected synchronized static void updateLastAddresses(TagEditorFragment caller, LinkedHashMap<String,ArrayList<String>> tags) {
		// save any address tags for "last address tags"
		LinkedHashMap<String,ArrayList<String>> addressTags = getAddressTags(tags);
		// this needs to be done after the edit again in case the street name of what ever has changed 
		if (addressTags.size() > 0) {
			if (lastAddresses == null) {
				lastAddresses = new LinkedList<Address>();
			}
			if (lastAddresses.size() >= MAX_SAVED_ADDRESSES) { //arbitrary limit for now
				lastAddresses.removeLast();
			}
			Address current = new Address(caller.getType(), caller.getOsmId(), addressTags);
			StreetTagValueAdapter streetAdapter = (StreetTagValueAdapter)((NameAdapters)caller.getActivity()).getStreetNameAdapter(null);
			if (streetAdapter!= null) {
				ArrayList<String> values = tags.get(Tags.KEY_ADDR_STREET); 
				if (values != null && values.size() > 0) {
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
			savingHelperAddress.save(caller.getActivity(),ADDRESS_TAGS_FILE, lastAddresses, false);
		}
	}
	
	protected synchronized static void saveLastAddresses(Context context) {
		if (lastAddresses != null) {
			savingHelperAddress.save(context, ADDRESS_TAGS_FILE, lastAddresses, false);
		}
	}
	
	protected synchronized static void loadLastAddresses(Context context) {
		if (lastAddresses == null) {
			try {
				lastAddresses = savingHelperAddress.load(context, ADDRESS_TAGS_FILE, false);
				Log.d("TagEditor","onResume read " + lastAddresses.size() + " addresses");
			} catch (Exception e) {
				//TODO be more specific
			}
		}
	}
}
