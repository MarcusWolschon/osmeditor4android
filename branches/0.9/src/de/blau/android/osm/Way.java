package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.xmlpull.v1.XmlSerializer;

import de.blau.android.R;
import de.blau.android.resources.Profile.FeatureProfile;

import android.nfc.FormatException;
import android.util.Log;
import android.widget.Toast;

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
		for (String key : tags.keySet()) {
			String value = tags.get(key);
			if (key.equals("oneway") || key.equals("incline") 
					|| key.equals("turn") || key.equals("turn:lanes")
					|| key.equals("direction") || key.endsWith(":left") 
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
		return result;
	}
	
	
	
	/**
	 * Reverse the direction dependent tags and save them to tags
	 * @param tags Map of all direction dependent tags
	 * @param reverseOneway if false don't change the value of the oneway tag if present
	 */
	public void reverseDirectionDependentTags(Map<String, String> dirTags, boolean reverseOneway) {
		for (String key : dirTags.keySet()) {
			if (!key.equals("oneway") || reverseOneway) {
				String value = tags.get(key).trim();
				tags.remove(key); //			
				if (key.equals("oneway")) {
					if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true") || value.equals("1")) {
						tags.put(key, "-1");
					} else if (value.equalsIgnoreCase("reverse") || value.equals("-1")) {
						tags.put(key, "yes");
					}
				} else if (key.equals("direction")) {
					if (value.equals("up")) {
						tags.put(key, "down");
					} else if (value.equals("down")) {
						tags.put(key, "up");
					} else {
						if (value.endsWith("°")) { //degrees
							try {
								String tmpVal = value.substring(0,value.length()-1);
								tags.put(key, floatToString(((Float.valueOf(tmpVal)+180.0f) % 360.0f)) + "°");
							} catch (NumberFormatException nex) {
								// oops put back original values 
								tags.put(key,value);
							}
						} else if (value.matches("-?\\d+(\\.\\d+)?")) { //degrees without degree symbol
							try {
								tags.put(key, floatToString(((Float.valueOf(value)+180.0f) % 360.0f)));
							} catch (NumberFormatException nex) {
								// oops put back original values 
								tags.put(key,value);
							}
						} else { // cardinal directions
							try {
								String tmpVal = "";
								for (int i=0;i<value.length();i++) {
									switch (value.toUpperCase().charAt(i)) {
										case 'N': tmpVal = tmpVal + 'S'; break;
										case 'W': tmpVal = tmpVal + 'E'; break;
										case 'S': tmpVal = tmpVal + 'N'; break;
										case 'E': tmpVal = tmpVal + 'W'; break;
										default: throw new FormatException(); 
									}
								}
								tags.put(key,tmpVal);
							} catch (FormatException fex) {
								tags.put(key,value);
							}
						}
					}
				} else if (key.equals("incline")) {
					if (value.equals("up")) {
						tags.put(key, "down");
					} else if (value.equals("down")) {
						tags.put(key, "up");
					} else {
						try {
							if (value.endsWith("°")) { //degrees
								String tmpVal = value.substring(0,value.length()-1);
								tags.put(key, floatToString((Float.valueOf(tmpVal)*-1)) + "°");
							} else if (value.endsWith("%")) { // percent{
								String tmpVal = value.substring(0,value.length()-1);
								tags.put(key, floatToString((Float.valueOf(tmpVal)*-1)) + "%");
							} else {
								tags.put(key, floatToString((Float.valueOf(value)*-1)));
							}
						} catch (NumberFormatException nex) {
							// oops put back original values 
							tags.put(key,value);
						}
					}
				} else if (key.equals("turn:lanes") || key.equals("turn")) { // turn:lane:forward/backward doesn't need to be handled special
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
					tags.put(key, tmpValue);
				} else if (key.endsWith(":left")) { // this would be more elegant in a loop
					String tmpKey = key.substring(0, key.length()-5);
					tags.put(tmpKey + ":right", value);
				} else if (key.endsWith(":right")) {
					String tmpKey = key.substring(0, key.length()-6);
					tags.put(tmpKey + ":left", value);
				} else if (key.endsWith(":backward")) {
					String tmpKey = key.substring(0, key.length()-9);
					tags.put(tmpKey + ":forward", value);
				} else if (key.endsWith(":forward")) {
					String tmpKey = key.substring(0, key.length()-8);
					tags.put(tmpKey + ":backward", value);
				} else if (key.indexOf(":forward:") >= 0) {
					String tmpKey = key.replace(":forward:", ":backward:");
					tags.put(tmpKey, value);
				} else if (key.indexOf(":backward:") >= 0) {
					String tmpKey = key.replace(":backward:", ":forward:");
					tags.put(tmpKey, value);
				} else if (key.indexOf(":right:") >= 0) {
					String tmpKey = key.replace(":right:", ":left:");
					tags.put(tmpKey, value);
				} else if (key.indexOf(":left:") >= 0) {
					String tmpKey = key.replace(":left:", ":right:");
					tags.put(tmpKey, value);
				} else if (value.equals("right")) {  // doing this for all values is probably dangerous
					tags.put(key, "left");
				} else if (value.equals("left")) {
					tags.put(key, "right");
				} else if (value.equals("forward")) {
					tags.put(key, "backward");
				} else if (value.equals("backward")) {
					tags.put(key, "forward");
				} else {
					// can't happen should throw an exception
					tags.put(key,value);
				}
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
		String waterway = getTagWithKey("waterway");
		if (waterway != null) {
			brainDead = true; // IHMO
		} else {
			String natural = getTagWithKey("natural");
			if ((natural != null) && (natural.equals("cliff") || natural.equals("coastline"))) {
				brainDead = true; // IHMO
			} else {
				String barrier = getTagWithKey("barrier");
				if ((barrier != null) && barrier.equals("retaining_wall")) {
					brainDead = true; // IHMO
				} else if ((barrier != null) && barrier.equals("kerb")) {
					brainDead = true; //
				} else if ((barrier != null) && barrier.equals("guard_rail")) {
					brainDead = true; //	
				} else if ((barrier != null) && barrier.equals("city_wall") && ((getTagWithKey("two_sided") == null) || !getTagWithKey("two_sided").equals("yes"))) {
					brainDead = true; // IMHO
				} else {
					String man_made = getTagWithKey("man_made");
					if ((man_made != null) && man_made.equals("embankment")) {
						brainDead = true; // IHMO
					}
				}
			}
		}
		return brainDead;
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
