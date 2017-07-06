package de.blau.android.osm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/**
 * Logic for reversing direction dependent tags, this is one of the more arcane things about the OSM data model
 * @author simon
 *
 */
public class Reverse {
	
	/**
	 * Return the direction dependent tags and associated values 
	 * oneway, *:left, *:right, *:backward, *:forward
	 * Probably we should check for issues with relation membership too 
	 * @return
	 */
	public static Map<String, String> getDirectionDependentTags(OsmElement e) {
		Map<String, String> result = null;
		Map<String, String> tags = e.getTags();
		if (tags != null) {
			for (String key : tags.keySet()) {
				String value = tags.get(key);
				if ("oneway".equals(key) || "incline".equals(key) 
						|| "turn".equals(key) || "turn:lanes".equals(key)
						|| "direction".equals(key) || key.endsWith(":left") 
						|| key.endsWith(":right") || key.endsWith(":backward") 
						|| key.endsWith(":forward") 
						|| key.contains(":forward:") || key.contains(":backward:")
						|| key.contains(":right:") || key.contains(":left:")
						|| value.equals("right") || value.equals("left") 
						|| value.equals("forward") || value.equals("backward")) {
					if (result == null) {
						result = new TreeMap<String, String>();
					}
					result.put(key, value);
				}
			}
		}
		return result;
	}
	
	/**
	 * Return a list of (route) relations that this way is a member of with a direction dependent role
	 * @return
	 */
	public static List<Relation> getRelationsWithDirectionDependentRoles(OsmElement e) {
		ArrayList<Relation> result = null;
		ArrayList<Relation> parents = e.getParentRelations();
		if (parents != null) {
			for (Relation r:parents) {
				String t = r.getTagWithKey(Tags.KEY_TYPE);
				if (t != null && Tags.VALUE_ROUTE.equals(t)) {
					RelationMember rm = r.getMember(Way.NAME, e.getOsmId());
					if (rm != null && ("forward".equals(rm.getRole()) || "backward".equals(rm.getRole()))) {
						if (result == null) {
							result = new ArrayList<Relation>();
						}
						result.add(r);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Reverse the role of this element in any relations it is in (currently only relevant for routes)
	 * @param relations
	 */
	public  static void reverseRoleDirection(OsmElement e, List<Relation> relations) {
		if (relations != null) {
			for (Relation r:relations) {
				for (RelationMember rm:r.getAllMembers(e)) {
					if (rm.role != null && "forward".equals(rm.role)) {
						rm.setRole("backward");
						continue;
					} 
					if (rm.role != null && "backward".equals(rm.role)) {
						rm.setRole("forward");
						continue;
					} 
				}
			}
		}
	}
	
	private static String reverseCardinalDirection(final String value) throws NumberFormatException
	{
		String tmpVal = "";
		for (int i=0;i<value.length();i++) {
			switch (value.toUpperCase(Locale.US).charAt(i)) {
				case 'N': tmpVal = tmpVal + 'S'; break;
				case 'W': tmpVal = tmpVal + 'E'; break;
				case 'S': tmpVal = tmpVal + 'N'; break;
				case 'E': tmpVal = tmpVal + 'W'; break;
				default: throw new NumberFormatException(); 
			}
		}
		return tmpVal;
	}
	
	private static String reverseDirection(final String value) {
		if (value.equals("up")) {
			return "down";
		} else if (value.equals("down")) {
			return "up";
		} else {
			if (value.endsWith("°")) { //degrees
				try {
					String tmpVal = value.substring(0,value.length()-1);
					return floatToString(((Float.valueOf(tmpVal)+180.0f) % 360.0f)) + "°";
				} catch (NumberFormatException nex) {
					// oops put back original values 
					return value;
				}
			} else if (value.matches("-?\\d+(\\.\\d+)?")) { //degrees without degree symbol
				try {
					return floatToString(((Float.valueOf(value)+180.0f) % 360.0f));
				} catch (NumberFormatException nex) {
					// oops put back original values 
					return value;
				}
			} else { // cardinal directions
				try {
					return reverseCardinalDirection(value);
				} catch (NumberFormatException fex) {
					return value;
				}
			}
		}
	}
	
	private static String reverseTurnLanes(final String value) {
		String tmpValue = "";
		for (String s:value.split("\\|")) {
			String tmpValue2 = "";
			for (String s2:s.split(";")) {
				if (s2.indexOf("right") >= 0) {
					s2 = s2.replace("right", "left");
				} else if (s.indexOf("left") >= 0) {
					s2 = s2.replace("left", "right");	
				}
				if (tmpValue2.equals("")) {	
					tmpValue2 = s2;
				} else {
					tmpValue2 = s2 + ";" + tmpValue2; // reverse order 
				}
			}
			if (tmpValue.equals("")) {	
				tmpValue = tmpValue2;
			} else {
				tmpValue = tmpValue2 + "|" + tmpValue; // reverse order 
			}
		}
		return tmpValue;
	}
	
	private static String reverseIncline(final String value) {
		String tmpVal;
		if (value.equals("up")) {
			return "down";
		} else if (value.equals("down")) {
			return "up";
		} else {
			try {
				if (value.endsWith("°")) { //degrees
					tmpVal = value.substring(0,value.length()-1);
					return floatToString((Float.valueOf(tmpVal)*-1)) + "°";
				} else if (value.endsWith("%")) { // percent{
					tmpVal = value.substring(0,value.length()-1);
					return floatToString((Float.valueOf(tmpVal)*-1)) + "%";
				} else {
					return floatToString((Float.valueOf(value)*-1));
				}
			} catch (NumberFormatException nex) {
				// oops put back original values 
				return value;
			}
		}
	}
	
	private static String reverseOneway(final String value) {
		if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true") || value.equals("1")) {
			return "-1";
		} else if (value.equalsIgnoreCase("reverse") || value.equals("-1")) {
			return "yes";
		}
		return value;
	}
	
	/**
	 * Reverse the direction dependent tags and save them to tags
	 * 
	 * Note this code in its original version ran in to complexity limits on Android 2.2 (and probably older). Eliminating if .. else if constructs seems to have
	 * resolved this
	 * @param tags Map of all direction dependent tags
	 * @param reverseOneway if false don't change the value of the oneway tag if present
	 */
	public  static void reverseDirectionDependentTags(OsmElement e, Map<String, String> dirTags, boolean reverseOneway) {
		if (e.getTags() == null) {
			return;
		}
		Map<String, String> tags = new TreeMap<String,String>(e.getTags());

		// remove all dir dependent key first
		for (String key : dirTags.keySet()) {
			tags.remove(key);
		}
		
		for (String key : dirTags.keySet()) {
			String value = dirTags.get(key).trim();	
			if (!key.equals("oneway") || reverseOneway) {
				if (key.equals("oneway")) {
					tags.put(key, reverseOneway(value));
					continue;
				} 
				if (key.equals("direction")) {
					tags.put(key, reverseDirection(value));
					continue;
				} 
				if (key.equals("incline")) {
					tags.put(key, reverseIncline(value));
					continue;
				} 
				if (key.equals("turn:lanes") || key.equals("turn")) { // turn:lane:forward/backward doesn't need to be handled special
					tags.put(key,reverseTurnLanes(value));
					continue;
				} 
				if (key.endsWith(":left")) { // this would be more elegant in a loop
					tags.put(key.substring(0, key.length()-5) + ":right", value);
					continue;
				} 
				if (key.endsWith(":right")) {
					tags.put(key.substring(0, key.length()-6) + ":left", value);
					continue;
				} 
				if (key.endsWith(":backward")) {
					tags.put(key.substring(0, key.length()-9) + ":forward", value);
					continue;
				} 
				if (key.endsWith(":forward")) {
					tags.put(key.substring(0, key.length()-8) + ":backward", value);
					continue;
				} 
				if (key.indexOf(":forward:") >= 0) {
					tags.put(key.replace(":forward:", ":backward:"), value);
					continue;
				} 
				if (key.indexOf(":backward:") >= 0) {
					tags.put(key.replace(":backward:", ":forward:"), value);
					continue;
				} 
				if (key.indexOf(":right:") >= 0) {
					tags.put(key.replace(":right:", ":left:"), value);
					continue;
				} 
				if (key.indexOf(":left:") >= 0) {
					tags.put(key.replace(":left:", ":right:"), value);
					continue;
				} 
				if (value.equals("right")) {  // doing this for all values is probably dangerous
					tags.put(key, "left");
					continue;
				} 
				if (value.equals("left")) {
					tags.put(key, "right");
					continue;
				} 
				if (value.equals("forward")) {
					tags.put(key, "backward");
					continue;
				} 
				if (value.equals("backward")) {
					tags.put(key, "forward");
					continue;
				} 
				// shouldn't happen should throw an exception
				tags.put(key,value);
			} else {
				tags.put(key,value);
			}
		}
		e.setTags(tags);
	}
	
	private static String floatToString(float f)
	{
		if(f == (int) f)
	        return String.format(Locale.US, "%d",(int)f);
	    else
	        return String.format(Locale.US,"%s",f);
	}
}
