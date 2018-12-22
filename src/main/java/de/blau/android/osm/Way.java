package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.GeoMath;
import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.validation.Validator;

public class Way extends OsmElement implements BoundedObject, StyleableFeature {

    private static final String DEBUG_TAG = "Way";

    /**
     * 
     */
    private static final long serialVersionUID = 1104911642016294267L;

    final ArrayList<Node> nodes;

    /**
     * Cache of bounding box
     */
    private int left = Integer.MIN_VALUE;
    private int bottom;
    private int right;
    private int top;

    public static final String NAME = "way";

    public static final String NODE = "nd";

    private transient FeatureStyle featureProfile = null; // FeatureProfile is currently not serializable

    /**
     * Construct a new Way
     * 
     * @param osmId the OSM id
     * @param osmVersion the version
     * @param timestamp a timestamp
     * @param status the current status
     */
    Way(final long osmId, final long osmVersion, final long timestamp, final byte status) {
        super(osmId, osmVersion, timestamp, status);
        nodes = new ArrayList<>();
    }

    /**
     * Add node at end of way
     * 
     * Will silently not add the same node twice
     * 
     * @param node Node to add
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
     * 
     * @return a List of Nodes
     */
    @NonNull
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
        StringBuilder res = new StringBuilder(super.toString());
        if (tags != null) {
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                res.append('\t');
                res.append(tag.getKey());
                res.append('=');
                res.append(tag.getValue());
            }
        }
        return res.toString();
    }

    @Override
    public void toXml(final XmlSerializer s, final Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
        toXml(s, changeSetId, false);
    }

    @Override
    public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        toXml(s, null, true);
    }

    /**
     * Generate XML format OSM files
     * 
     * @param s the XML serializer
     * @param changeSetId the current changeset id
     * @param josm if true use JOSM format
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void toXml(@NonNull final XmlSerializer s, @Nullable Long changeSetId, boolean josm)
            throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", NAME);
        attributesToXml(s, changeSetId, josm);
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
        s.endTag("", NAME);
    }

    /**
     * Returns true if "node" is a way node of this way
     * 
     * @param node the Node to check for
     * @return true if the Node is a member of the Way
     */
    public boolean hasNode(final Node node) {
        return nodes.contains(node);
    }

    /**
     * Returns true if this way has a common node with "way"
     * 
     * @param way the Way to check for a common Node
     * @return true if there is at least one common Node
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
     * Returns the first found common node with "way" or null if there are none
     * 
     * @param way the way we are inspecting
     * @return a common Node or null if none
     */
    @Nullable
    public Node getCommonNode(Way way) {
        for (Node n : this.nodes) {
            if (way.hasNode(n)) {
                return n;
            }
        }
        return null;
    }

    /**
     * Remove all occurrences of node from this way
     * 
     * If removing the node leads to two adjacent identical nodes delete one occurrence
     * 
     * @param node Node to remove
     */
    void removeNode(@NonNull final Node node) {
        int index = nodes.lastIndexOf(node);
        if (index > 0 && index < (nodes.size() - 1)) { // not the first or last node
            if (nodes.get(index - 1).equals(nodes.get(index + 1))) {
                nodes.remove(index - 1);
                Log.i(DEBUG_TAG, "removeNode removed duplicate node");
            }
        }
        int count = 0;
        while (nodes.remove(node)) {
            count++;
        }
        if (count > 1) {
            Log.i(DEBUG_TAG, "removeNode removed " + (count - 1) + " duplicate node(s)");
        }
    }

    /**
     * return true if first == last node, will not work for broken geometries
     * 
     * @return true if closed
     */
    public boolean isClosed() {
        if (nodes == null || nodes.isEmpty()) {
            Log.e(DEBUG_TAG, "way " + getOsmId() + " has no nodes");
            return false;
        }
        return nodes.get(0).equals(nodes.get(nodes.size() - 1));
    }

    /**
     * Append/pre-pend a Node to the Way using an exiting Node as reference
     * 
     * Note: assumes the Way isn't empty
     * 
     * @param refNode the existing Node
     * @param newNode the new Node
     */
    void appendNode(@NonNull final Node refNode, @NonNull final Node newNode) {
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

    /**
     * Inserts a Node after a specified one
     * 
     * Note: assumes the Way isn't empty and nodeBefore is actually a way Node
     * 
     * @param nodeBefore the reference Node
     * @param newNode the Node to insert
     */
    void addNodeAfter(@NonNull final Node nodeBefore, @NonNull final Node newNode) {
        if (nodeBefore == newNode) { // user error
            Log.i(DEBUG_TAG, "addNodeAfter attempt to add same node");
            return;
        }
        nodes.add(nodes.indexOf(nodeBefore) + 1, newNode);
    }

    /**
     * Adds multiple nodes to the way in the order in which they appear in the list. They can be either prepended or
     * appended to the existing nodes.
     * 
     * @param newNodes a list of new nodes
     * @param atBeginning if true, nodes are prepended, otherwise, they are appended
     */
    void addNodes(List<Node> newNodes, boolean atBeginning) {
        if (atBeginning) {
            if (!nodes.isEmpty() && nodes.get(0) == newNodes.get(newNodes.size() - 1)) { // user error
                Log.i(DEBUG_TAG, "addNodes attempt to add same node");
                if (newNodes.size() > 1) {
                    Log.i(DEBUG_TAG, "retrying addNodes");
                    newNodes.remove(newNodes.size() - 1);
                    addNodes(newNodes, atBeginning);
                }
                return;
            }
            nodes.addAll(0, newNodes);
        } else {
            if (!nodes.isEmpty() && newNodes.get(0) == nodes.get(nodes.size() - 1)) { // user error
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
     * 
     * @param existing The existing node to be replaced.
     * @param newNode The new node.
     */
    void replaceNode(Node existing, Node newNode) {
        int idx;
        while ((idx = nodes.indexOf(existing)) != -1) {
            nodes.set(idx, newNode);
            // check for duplicates
            if (idx > 0 && nodes.get(idx - 1).equals(newNode)) {
                Log.i(DEBUG_TAG, "replaceNode node would duplicate preceeding node");
                nodes.remove(idx);
            }
            if (idx >= 0 && idx < nodes.size() - 1 && nodes.get(idx + 1).equals(newNode)) {
                Log.i(DEBUG_TAG, "replaceNode node would duplicate following node");
                nodes.remove(idx);
            }
        }
    }

    /**
     * Checks if a node is an end node of the way (i.e. either the first or the last one)
     * 
     * @param node a node to check
     * @return in node is one of the end nodes
     */
    public boolean isEndNode(@Nullable final Node node) {
        if (nodes.size() > 0) {
            return getFirstNode() == node || getLastNode() == node;
        }
        return false;
    }

    /**
     * Get the first Node of this Way
     * 
     * @return the first Node
     */
    public Node getFirstNode() {
        return nodes.get(0);
    }

    /**
     * Get the last Node of this Way
     * 
     * @return the last Ndoe
     */
    public Node getLastNode() {
        return nodes.get(nodes.size() - 1);
    }

    /**
     * Checks if this way is tagged as oneway
     * 
     * @return 1 if this is a regular oneway-way (oneway:yes, oneway:true or oneway:1), -1 if this is a reverse
     *         oneway-way (oneway:-1 or oneway:reverse), 0 if this is not a oneway-way (no oneway tag or tag with none
     *         of the specified values)
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
     * There is a set of tags which lead to a way not being reversible, this method returns true if we match one of them
     * 
     * FIXME move the information to Tags natural=cliff natural=coastline barrier=retaining_wall barrier=kerb
     * barrier=guard_rail man_made=embankment barrier=city_wall if two_sided != yes waterway=*
     * 
     * @return true if tags are present
     */
    public boolean notReversable() {
        String waterway = getTagWithKey(Tags.KEY_WATERWAY);
        if (waterway != null) {
            return true;
        }

        String natural = getTagWithKey(Tags.KEY_NATURAL);
        if ((natural != null) && (natural.equals(Tags.VALUE_CLIFF) || natural.equals(Tags.VALUE_COASTLINE))) {
            return true;
        }

        String man_made = getTagWithKey(Tags.KEY_MAN_MADE);
        if ((man_made != null) && man_made.equals(Tags.VALUE_EMBANKMENT)) {
            return true; // IHMO
        }

        String highway = getTagWithKey(Tags.KEY_HIGHWAY);
        if (highway != null) {
            if (Tags.VALUE_MOTORWAY.equals(highway)) {
                return true;
            }
        }

        String barrier = getTagWithKey(Tags.KEY_BARRIER);
        if (barrier != null) {
            if (Tags.VALUE_RETAINING_WALL.equals(barrier)) {
                return true;
            }
            if (Tags.VALUE_KERB.equals(barrier)) {
                return true;
            }
            if (Tags.VALUE_GUARD_RAIL.equals(barrier)) {
                return true;
            }
            String twoSided = getTagWithKey(Tags.KEY_TWO_SIDED);
            if (Tags.VALUE_CITY_WALL.equals(barrier) && (twoSided == null || !Tags.VALUE_YES.equals(twoSided))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ElementType getType() {
        if (nodes.size() < 2) {
            return ElementType.WAY; // should not happen
        }
        if (getFirstNode().equals(getLastNode())) {
            return ElementType.CLOSEDWAY;
        } else {
            return ElementType.WAY;
        }
    }

    @Override
    public ElementType getType(Map<String, String> tags) {
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

    @Override
    public FeatureStyle getFeatureProfile() {
        return featureProfile;
    }

    @Override
    public void setFeatureProfile(@Nullable FeatureStyle fp) {
        featureProfile = fp;
    }

    /**
     * Return the number of nodes in the is way
     * 
     * @return the number of nodes in this Way
     */
    public int nodeCount() {
        return nodes == null ? 0 : nodes.size();
    }

    /**
     * Return the length in m
     * 
     * This uses the Haversine distance between nodes for calculation
     * 
     * @return the length in m
     */
    public double length() {
        return length(nodes);
    }

    /**
     * Return the length in m
     * 
     * This uses the Haversine distance between nodes for calculation
     * 
     * @param nodes List of Nodes
     * @return the length in m
     */
    public static double length(List<Node> nodes) {
        double result = 0d;
        if (nodes != null) {
            for (int i = 0; i < (nodes.size() - 1); i++) {
                result = result + GeoMath.haversineDistance(nodes.get(i).getLon() / 1E7D, nodes.get(i).getLat() / 1E7D, nodes.get(i + 1).getLon() / 1E7D,
                        nodes.get(i + 1).getLat() / 1E7D);
            }
        }
        return result;
    }

    /**
     * Get the distance from a coordinate to this way
     * 
     * Note this is only useful for sorting given that the result is returned in WGS84 °*1E7 or so
     * 
     * @param location the coordinate in WGS84*1E/
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
                    distance = Math.min(distance, GeoMath.getLineDistance(location[0], location[1], n1.getLat(), n1.getLon(), n2.getLat(), n2.getLon()));
                }
                n1 = n2;
            }
        }
        return distance;
    }

    /**
     * Returns a bounding box covering the wayy
     * 
     * @return the bounding box of the way
     */
    public BoundingBox getBounds() {
        BoundingBox result = null;
        if (left != Integer.MIN_VALUE) {
            return new BoundingBox(left, bottom, right, top);
        }
        boolean first = true;
        for (Node n : getNodes()) {
            if (first) {
                result = new BoundingBox(n.lon, n.lat);
                first = false;
            } else {
                result.union(n.lon, n.lat);
            }
        }
        setBoundingBoxCache(result);
        return result;
    }

    /**
     * Cache the calculated bounding box for this way
     * 
     * @param box the BoundingBox
     */
    private void setBoundingBoxCache(@Nullable BoundingBox box) {
        if (box == null) {
            return;
        }
        left = box.getLeft();
        bottom = box.getBottom();
        right = box.getRight();
        top = box.getTop();
    }

    /**
     * Called if geometry has changed and caced bbox is invalid
     */
    public void invalidateBoundingBox() {
        left = Integer.MIN_VALUE;
    }

    /**
     * Returns a bounding box covering the way
     * 
     * @param result a bounding box to use for producing the result, avoids creating an object instance
     * @return the bounding box of the way
     */
    public BoundingBox getBounds(BoundingBox result) {
        if (left != Integer.MIN_VALUE) {
            result.set(left, bottom, right, top);
            return result;
        }
        boolean first = true;
        for (Node n : getNodes()) {
            if (first) {
                result.resetTo(n.lon, n.lat);
                first = false;
            } else {
                result.union(n.lon, n.lat);
            }
        }
        setBoundingBoxCache(result);
        return result;
    }

    @Override
    protected int validate(Validator validator) {
        return validator.validate(this);
    }

    @Override
    <T extends OsmElement> void updateFrom(T e) {
        if (!(e instanceof Way)) {
            throw new IllegalArgumentException("e is not a Way");
        }
        if (e.getOsmId() != getOsmId()) {
            throw new IllegalArgumentException("Different ids " + e.getOsmId() + " != " + getOsmId());
        }
        setTags(e.getTags());
        setState(e.getState());
        nodes.clear();
        nodes.addAll(((Way) e).getNodes());
    }
}
