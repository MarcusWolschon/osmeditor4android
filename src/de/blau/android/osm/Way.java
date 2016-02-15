package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.resources.Profile.FeatureProfile;
import de.blau.android.util.GeoMath;
import de.blau.android.util.rtree.BoundedObject;

public class Way extends OsmElement implements BoundedObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1104911642016294266L;
	
	private static final String[] importantHighways;

	protected final ArrayList<Node> nodes;

	public static final String NAME = "way";

	public static final String NODE = "nd";
	
	public static int maxWayNodes = 2000; // if API has a different value it will replace this
	
	transient FeatureProfile featureProfile = null; // FeatureProfile is currently not serializable
	
	static {
		importantHighways = (
				"motorway,motorway_link,trunk,trunk_link,primary,primary_link,"+
				"secondary,secondary_link,tertiary,residential,unclassified,living_street"
		).split(",");
	}

	Way(final long osmId, final long osmVersion, final byte status) {
		super(osmId, osmVersion, status);
		nodes = new ArrayList<Node>();
	}

	/**
	 * Add node at end of way
	 * @param node
	 */
	void addNode(final Node node) {
		int size = nodes.size();
		if ((size > 0) && (nodes.get(size - 1) == node)) {
			Log.i("Way", "addNode attempt to add same node " + node.getOsmId() + " to " + getOsmId());
			return;
		}
		nodes.add(node);
	}

	/**
	 * Return list of all nodes in a way
	 * @return
	 */
	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * Be careful to leave at least 2 nodes!
	 * 
	 * @return list of nodes allowing {@link Iterator#remove()}.
	 */
	Iterator<Node> getRemovableNodes() {
		return nodes.iterator();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String toString() {
		String res = super.toString();
		if (tags != null) {
			for (Map.Entry<String, String> tag : tags.entrySet()) {
				res += "\t" + tag.getKey() + "=" + tag.getValue();
			}
		}
		return res;
	}

	@Override
	public void toXml(final XmlSerializer s, final Long changeSetId) throws IllegalArgumentException,
			IllegalStateException, IOException {
		s.startTag("", "way");
		s.attribute("", "id", Long.toString(osmId));
		if (changeSetId != null) s.attribute("", "changeset", Long.toString(changeSetId));
		s.attribute("", "version", Long.toString(osmVersion));

		if (nodes != null) {
			for (Node node : nodes) {
				s.startTag("", "nd");
				s.attribute("", "ref", Long.toString(node.getOsmId()));
				s.endTag("", "nd");
			}
		} else {
			Log.i("Way", "Way without nodes");
			throw new IllegalArgumentException("Way " + getOsmId() + " has no nodes");
		}

		tagsToXml(s);
		s.endTag("", "way");
	}
	
	@Override
	public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException,
			IllegalStateException, IOException {
		s.startTag("", "way");
		s.attribute("", "id", Long.toString(osmId));
		if (state == OsmElement.STATE_DELETED) {
			s.attribute("", "action", "delete");
		} else if (state == OsmElement.STATE_CREATED || state == OsmElement.STATE_MODIFIED) {
			s.attribute("", "action", "modify");
		}
		s.attribute("", "version", Long.toString(osmVersion));
		s.attribute("", "visible", "true");

		if (nodes != null) {
			for (Node node : nodes) {
				s.startTag("", "nd");
				s.attribute("", "ref", Long.toString(node.getOsmId()));
				s.endTag("", "nd");
			}
		} else {
			Log.i("Way", "Way without nodes");
			throw new IllegalArgumentException("Way " + getOsmId() + " has no nodes");
		}

		tagsToXml(s);
		s.endTag("", "way");
	}
	

	public boolean hasNode(final Node node) {
		return nodes.contains(node);
	}

	public boolean hasCommonNode(final Way way) {
		for (Node n : this.nodes) {
			if (way.hasNode(n)) {
				return true;
			}
		}
 		return false;
	}
	
	void removeNode(final Node node) {
		int index = nodes.lastIndexOf(node);
		if (index > 0 && index < (nodes.size()-1)) { // not the first or last node 
			if (nodes.get(index-1) == nodes.get(index+1)) {
				nodes.remove(index-1);
				Log.i("Way", "removeNode removed duplicate node");
			}
		}
		while (nodes.remove(node)) {
		}
	}
	
	/**
	 * return true if first == last node, will not work for broken geometries
	 * @return
	 */
	public boolean isClosed() {
		return nodes.get(0).equals(nodes.get(nodes.size() - 1));
	}

	void appendNode(final Node refNode, final Node newNode) {
		if (refNode == newNode) { // user error
			Log.i("Way", "appendNode attempt to add same node");
			return;
		}
		if (nodes.get(0) == refNode) {
			nodes.add(0, newNode);
		} else if (nodes.get(nodes.size() - 1) == refNode) {
			nodes.add(newNode);
		}
	}

	void addNodeAfter(final Node nodeBefore, final Node newNode) {
		if (nodeBefore == newNode) { // user error
			Log.i("Way", "addNodeAfter attempt to add same node");
			return;
		}
		nodes.add(nodes.indexOf(nodeBefore) + 1, newNode);
	}
	
	/**
	 * Adds multiple nodes to the way in the order in which they appear in the list.
	 * They can be either prepended or appended to the existing nodes.
	 * @param newNodes a list of new nodes
	 * @param atBeginning if true, nodes are prepended, otherwise, they are appended
	 */
	void addNodes(List<Node> newNodes, boolean atBeginning) {
		if (atBeginning) {
			if ((nodes.size() > 0) && nodes.get(0) == newNodes.get(newNodes.size()-1)) { // user error
				Log.i("Way", "addNodes attempt to add same node");
				if (newNodes.size() > 1) {
					Log.i("Way", "retrying addNodes");
					newNodes.remove(newNodes.size()-1);
					addNodes(newNodes, atBeginning);
				}
				return;
			}
			nodes.addAll(0, newNodes);
		} else {
			if ((nodes.size() > 0) && newNodes.get(0) == nodes.get(nodes.size()-1)) { // user error
				Log.i("Way", "addNodes attempt to add same node");
				if (newNodes.size() > 1) {
					Log.i("Way", "retrying addNodes");
					newNodes.remove(0);
					addNodes(newNodes, atBeginning);
				}
				return;
			}
			nodes.addAll(newNodes);
		}
	}
	
	/**
	 * Return the direction dependent tags and associated values 
	 * oneway, *:left, *:right, *:backward, *:forward
	 * Probably we should check for issues with relation membership too 
	 * @return
	 */
	public Map<String, String> getDirectionDependentTags() {
		Map<String, String> result = null;
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
	public List<Relation> getRelationsWithDirectionDependentRoles() {
		ArrayList<Relation> result = null;
		if (getParentRelations() != null) {
			for (Relation r:getParentRelations()) {
				String t = r.getTagWithKey(Tags.KEY_TYPE);
				if (t != null && Tags.VALUE_ROUTE.equals(t)) {
					RelationMember rm = r.getMember(Way.NAME, getOsmId());
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
	 * Reverse the role of this way in any relations it is in (currently only relevant for routes)
	 * @param relations
	 */
	public void reverseRoleDirection(List<Relation> relations) {
		if (relations != null) {
			for (Relation r:relations) {
				for (RelationMember rm:r.getMembers()) {
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
	
	private String reverseCardinalDirection(final String value) throws NumberFormatException
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
	
	private String reverseDirection(final String value) {
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
	
	private String reverseTurnLanes(final String value) {
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
	
	private String reverseIncline(final String value) {
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
	
	private String reverseOneway(final String value) {
		if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true") || value.equals("1")) {
			return "-1";
		} else if (value.equalsIgnoreCase("reverse") || value.equals("-1")) {
			return "yes";
		}
		return value;
	}
	
	/**
	 * Reverse the direction dependent tags and save them to tags
	 * Note this code in its original version ran in to complexity limits on Android 2.2 (and probably older). Eliminating if .. else if constructs seems to have
	 * resolved this
	 * @param tags Map of all direction dependent tags
	 * @param reverseOneway if false don't change the value of the oneway tag if present
	 */
	public void reverseDirectionDependentTags(Map<String, String> dirTags, boolean reverseOneway) {
		if (tags == null) {
			return;
		}
		for (String key : dirTags.keySet()) {
			if (!key.equals("oneway") || reverseOneway) {
				String value = tags.get(key).trim();
				tags.remove(key); //			
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
				// can't happen should throw an exception
				tags.put(key,value);
			}
		}
	}
	
	String floatToString(float f)
	{
		if(f == (int) f)
	        return String.format(Locale.US, "%d",(int)f);
	    else
	        return String.format(Locale.US,"%s",f);
	}
	
	/**
	 * Reverses the direction of the way
	 */
	void reverse() {
		Collections.reverse(nodes);
	}
	
	/**
	 * Replace an existing node in a way with a different node.
	 * @param existing The existing node to be replaced.
	 * @param newNode The new node.
	 */
	void replaceNode(Node existing, Node newNode) {
		int idx;
		while ((idx = nodes.indexOf(existing)) != -1) {
			nodes.set(idx, newNode);
		}
	}

	/**
	 * Checks if a node is an end node of the way (i.e. either the first or the last one)
	 * @param node a node to check
	 * @return 
	 */
	public boolean isEndNode(final Node node) {
		return getFirstNode() == node || getLastNode() == node;
	}
	
	public Node getFirstNode() {
		return nodes.get(0);
	}

	public Node getLastNode() {
		return nodes.get(nodes.size() - 1);
	}

	/**
	 * Checks if this way is tagged as oneway
	 * @return 1 if this is a regular oneway-way (oneway:yes, oneway:true or oneway:1),
	 *         -1 if this is a reverse oneway-way (oneway:-1 or oneway:reverse),
	 *         0 if this is not a oneway-way (no oneway tag or tag with none of the specified values)
	 */
	public int getOneway() {
		String oneway = getTagWithKey("oneway");
		if ("yes".equalsIgnoreCase(oneway) || "true".equalsIgnoreCase(oneway) || "1".equals(oneway)) {
			return 1;
		} else if ("-1".equals(oneway) || "reverse".equalsIgnoreCase(oneway)) {
			return -1;
		}
		return 0;
	}
	
	/**
	 * There is a set of tags which lead to a way not being reversible, this is EXTREMLY stupid and should be depreciated immediately.
	 * 
	 * natural=cliff
	 * natural=coastline
	 * barrier=retaining_wall
	 * barrier=kerb
	 * barrier=guard_rail
	 * man_made=embankment
	 * barrier=city_wall if two_sided != yes
	 * waterway=*
	 * 
	 * @return true if somebody added the brain dead tags
	 */
	public boolean notReversable()
	{
		boolean brainDead = false;
		String waterway = getTagWithKey(Tags.KEY_WATERWAY);
		if (waterway != null) {
			brainDead = true; // IHMO
		} else {
			String natural = getTagWithKey(Tags.KEY_NATURAL);
			if ((natural != null) && (natural.equals(Tags.VALUE_CLIFF) || natural.equals(Tags.VALUE_COASTLINE))) {
				brainDead = true; // IHMO
			} else {
				String barrier = getTagWithKey(Tags.KEY_BARRIER);
				if ((barrier != null) && barrier.equals(Tags.VALUE_RETAINING_WALL)) {
					brainDead = true; // IHMO
				} else if ((barrier != null) && barrier.equals(Tags.VALUE_KERB)) {
					brainDead = true; //
				} else if ((barrier != null) && barrier.equals(Tags.VALUE_GUARD_RAIL)) {
					brainDead = true; //	
				} else if ((barrier != null) && barrier.equals(Tags.VALUE_CITY_WALL) && ((getTagWithKey(Tags.KEY_TWO_SIDED) == null) || !getTagWithKey(Tags.KEY_TWO_SIDED).equals(Tags.VALUE_YES))) {
					brainDead = true; // IMHO
				} else {
					String man_made = getTagWithKey(Tags.KEY_MAN_MADE);
					if ((man_made != null) && man_made.equals(Tags.VALUE_EMBANKMENT)) {
						brainDead = true; // IHMO
					}
				}
			}
		}
		return brainDead;
	}
	
	private boolean hasTagWithValue(String tag, String value) {
		String tagValue = getTagWithKey(tag);
		return tagValue != null ? tagValue.equalsIgnoreCase(value) : false;
	}
	
	/**
	 * Test if the way has a problem.
	 * @return true if the way has a problem, false if it doesn't.
	 */
	@Override
	protected boolean calcProblem() {
		String highway = getTagWithKey(Tags.KEY_HIGHWAY); // cache frequently accessed key
		if (Tags.VALUE_ROAD.equalsIgnoreCase(highway)) {
			// unsurveyed road
			return true;
		}
		if ((getTagWithKey(Tags.KEY_NAME) == null) && (getTagWithKey(Tags.KEY_REF) == null) 
				&& !(hasTagWithValue(Tags.KEY_NONAME,Tags.VALUE_YES) || hasTagWithValue(Tags.KEY_VALIDATE_NO_NAME,Tags.VALUE_YES))) {
			// unnamed way - only the important ones need names
			for (String h : importantHighways) {
				if (h.equalsIgnoreCase(highway)) {
					return true;
				}
			}
		}
		return super.calcProblem();
	}
	
	@Override
	public String describeProblem() {
		String superProblem = super.describeProblem();
		String wayProblem = "";
		String highway = getTagWithKey(Tags.KEY_HIGHWAY);
		if (Tags.VALUE_ROAD.equalsIgnoreCase(highway)) {
			wayProblem = Application.mainActivity.getString(R.string.toast_unsurveyed_road);
		}
		if ((getTagWithKey(Tags.KEY_NAME) == null) && (getTagWithKey(Tags.KEY_REF) == null)
				&& !(hasTagWithValue(Tags.KEY_NONAME,Tags.VALUE_YES) || hasTagWithValue(Tags.KEY_VALIDATE_NO_NAME,Tags.VALUE_YES))) {
			boolean isImportant = false;
			for (String h : importantHighways) {
				if (h.equalsIgnoreCase(highway)) {
					isImportant = true;
					break;
				}
			}
			if (isImportant) {
				wayProblem = !wayProblem.equals("") ? wayProblem +", " :  Application.mainActivity.getString(R.string.toast_noname);
			}
		}
		if (!superProblem.equals("")) 
			return superProblem + (!wayProblem.equals("") ? "\n" + wayProblem : "");
		else
			return wayProblem;
	}
	
	@Override
	public ElementType getType() {
		if (nodes.size() < 2) return ElementType.WAY; // should not happen
		
		if (getFirstNode().equals(getLastNode())) {
			return ElementType.CLOSEDWAY;
		} else {
			return ElementType.WAY;
		}
	}
	
	@Override
	void updateState(final byte newState) {
		featureProfile = null; // force recalc of style
		super.updateState(newState);
	}
	
	@Override
	void setState(final byte newState) {
		featureProfile = null; // force recalc of style
		super.setState(newState);
	}
	
	public FeatureProfile getFeatureProfile() {
		return featureProfile;
	}
	
	public void setFeatureProfile(FeatureProfile fp) {
		featureProfile = fp;
	}
	
	/** 
	 * return the number of nodes in the is way
	 * @return
	 */
	public int nodeCount() {
		return nodes == null ? 0 : nodes.size();
	}
	
	/** 
	 * return the length in m
	 * @return
	 */
	public double length() {
		double result = 0d;
		if (nodes != null) {
			for (int i = 0; i < (nodes.size() - 1); i++) {
				result = result + GeoMath.haversineDistance(nodes.get(i).getLon()/1E7D, nodes.get(i).getLat()/1E7D, nodes.get(i+1).getLon()/1E7D, nodes.get(i+1).getLat()/1E7D);
			}
		}
		return result;
	}
	
	/**
	 * Note this is only useful for sorting given that the result is returned in WGS84 °*1E7 or so
     * @param location
     * @return the minimum distance of this way to the given location
     */
	public double getDistance(final int[] location) {
		double distance = Double.MAX_VALUE;
		if (location != null) {
			Node n1 = null;
			for (Node n2 : getNodes()) {
				// distance to nodes of way
				if (n1 != null) {
					// distance to lines of way
					distance = Math.min(distance,
							GeoMath.getLineDistance(
									location[0], location[1],
									n1.getLat(), n1.getLon(),
									n2.getLat(), n2.getLon()));
				}
				n1 = n2;
			}
		}
		return distance;
	}
	
	/**
	 * Returns a bounding box covering the way
	 * FIXME results should be cached in some intelligent way
	 * @return
	 */
	public BoundingBox getBounds() {
		BoundingBox result = null;
		boolean first = true;
		for (Node n : getNodes()) {
			if (first) {
				result = new BoundingBox(n.lon,n.lat);
			} else {
				result.union(n.lon,n.lat);
			}
		}
		return result;
	}

	/**
	 * Set the maximum number of nodes allowed in one way
	 * @param max
	 */
	public static void setMaxWayNodes(int max) {
		maxWayNodes = max;
	}
}
