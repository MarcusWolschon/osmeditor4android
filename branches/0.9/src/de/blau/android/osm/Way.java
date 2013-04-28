package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import de.blau.android.resources.Profile.FeatureProfile;

import android.util.Log;

public class Way extends OsmElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1104911642016294266L;
	
	private static final String[] importantHighways;

	protected final ArrayList<Node> nodes;

	public static final String NAME = "way";

	public static final String NODE = "nd";
	
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

	void addNode(final Node node) {
		if ((nodes.size() > 0) && (nodes.get(nodes.size() - 1) == node)) {
			Log.i("Way", "addNode attempt to add same node");
			return;
		}
		nodes.add(node);
	}

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
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			res += "\t" + tag.getKey() + "=" + tag.getValue();
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

		for (Node node : nodes) {
			s.startTag("", "nd");
			s.attribute("", "ref", Long.toString(node.getOsmId()));
			s.endTag("", "nd");
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
			;
		}
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
	 * Test if the way has a problem.
	 * @return true if the way has a problem, false if it doesn't.
	 */
	protected boolean calcProblem() {
		String highway = getTagWithKey("highway"); // cache frequently accessed key
		if ("road".equalsIgnoreCase(highway)) {
			// unsurveyed road
			return true;
		}
		if (getTagWithKey("name") == null) {
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
	
}
