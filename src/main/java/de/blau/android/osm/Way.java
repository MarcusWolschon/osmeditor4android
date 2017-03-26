package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.GeoMath;
import de.blau.android.util.rtree.BoundedObject;

public class Way extends OsmElement implements BoundedObject {

	private static final String DEBUG_TAG = "Way";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1104911642016294266L;
	
	private static final String[] importantHighways;

	final ArrayList<Node> nodes;

	public static final String NAME = "way";

	public static final String NODE = "nd";
	
	public static int maxWayNodes = 2000; // if API has a different value it will replace this
	
	private transient FeatureStyle featureProfile = null; // FeatureProfile is currently not serializable
	
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
			Log.i(DEBUG_TAG, "addNode attempt to add same node " + node.getOsmId() + " to " + getOsmId());
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
			Log.i(DEBUG_TAG, "Way without nodes");
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
			Log.i(DEBUG_TAG, "Way without nodes");
			throw new IllegalArgumentException("Way " + getOsmId() + " has no nodes");
		}

		tagsToXml(s);
		s.endTag("", "way");
	}
	
	/**
	 * Returns true if "node" is a way node of this way
	 * @param node
	 * @return
	 */
	public boolean hasNode(final Node node) {
		return nodes.contains(node);
	}

	/**
	 * Returns true if this way has a common node with "way"
	 * @param way
	 * @return
	 */
	public boolean hasCommonNode(final Way way) {
		for (Node n : this.nodes) {
			if (way.hasNode(n)) {
				return true;
			}
		}
 		return false;
	}
	
	/**
	 * Returns the first found common node with "way" or null if their are none
	 * @param way
	 * @return
	 */
	public Node getCommonNode(Way way) {
		for (Node n : this.nodes) {
			if (way.hasNode(n)) {
				return n;
			}
		}
		return null;
	}
	
	void removeNode(final Node node) {
		int index = nodes.lastIndexOf(node);
		if (index > 0 && index < (nodes.size()-1)) { // not the first or last node 
			if (nodes.get(index-1) == nodes.get(index+1)) {
				nodes.remove(index-1);
				Log.i(DEBUG_TAG, "removeNode removed duplicate node");
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
			Log.i(DEBUG_TAG, "appendNode attempt to add same node");
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
			Log.i(DEBUG_TAG, "addNodeAfter attempt to add same node");
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
				Log.i(DEBUG_TAG, "addNodes attempt to add same node");
				if (newNodes.size() > 1) {
					Log.i(DEBUG_TAG, "retrying addNodes");
					newNodes.remove(newNodes.size()-1);
					addNodes(newNodes, atBeginning);
				}
				return;
			}
			nodes.addAll(0, newNodes);
		} else {
			if ((nodes.size() > 0) && newNodes.get(0) == nodes.get(nodes.size()-1)) { // user error
				Log.i(DEBUG_TAG, "addNodes attempt to add same node");
				if (newNodes.size() > 1) {
					Log.i(DEBUG_TAG, "retrying addNodes");
					newNodes.remove(0);
					addNodes(newNodes, atBeginning);
				}
				return;
			}
			nodes.addAll(newNodes);
		}
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
			// check for duplicates
			if (idx > 0 && nodes.get(idx-1).equals(newNode)) {
				Log.i(DEBUG_TAG, "replaceNode node would duplicate preceeding node");
				nodes.remove(idx);
			}
			if (idx >= 0 &&  idx < nodes.size()-1 && nodes.get(idx+1).equals(newNode)) {
				Log.i(DEBUG_TAG, "replaceNode node would duplicate following node");
				nodes.remove(idx);
			}
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
		return tagValue != null && tagValue.equalsIgnoreCase(value);
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
			wayProblem = App.resources().getString(R.string.toast_unsurveyed_road);
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
				wayProblem = !wayProblem.equals("") ? wayProblem +", " :  App.resources().getString(R.string.toast_noname);
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
	public ElementType getType(Map<String,String> tags) {
		return getType();
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
	
	public FeatureStyle getFeatureProfile() {
		return featureProfile;
	}
	
	public void setFeatureProfile(FeatureStyle fp) {
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
	 * 
	 * @return the bounding box of the way
	 */
	public BoundingBox getBounds() {
		BoundingBox result = null;
		boolean first = true;
		for (Node n : getNodes()) {
			if (first) {
				result = new BoundingBox(n.lon,n.lat);
				first = false;
			} else {
				result.union(n.lon,n.lat);
			}
		}
		return result;
	}
	
	/**
	 * Returns a bounding box covering the way
	 * FIXME results should be cached in some intelligent way
	 * 
	 * @param result a bounding box to use for producing the result, avoids creating an object instance
	 * @return  the bounding box of the way
	 */
	public BoundingBox getBounds(BoundingBox result) {
		boolean first = true;
		for (Node n : getNodes()) {
			if (first) {
				result.resetTo(n.lon,n.lat);
				first = false;
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
