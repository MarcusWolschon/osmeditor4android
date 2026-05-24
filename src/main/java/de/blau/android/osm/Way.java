package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.GeoMath;
import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.validation.Validator;

public class Way extends StyledOsmElement implements WayInterface, BoundedObject {

    private static final String DEBUG_TAG = Way.class.getSimpleName().substring(0, Math.min(23, Way.class.getSimpleName().length()));

    /**
     * 
     */
    private static final long serialVersionUID = 1104911642016294270L;

    private final List<Node> nodes;

    /**
     * Cache of bounding box
     */
    private int left = Integer.MIN_VALUE;
    private int bottom;
    private int right;
    private int top;

    /**
     * cached element type
     */
    private transient ElementType elementType;

    public static final String NAME = "way";

    /**
     * Abbreviation
     */
    public static final String ABBREV = "w";

    public static final String NODE = "nd";
    static final String        REF  = "ref";

    public static final int MINIMUM_NODES_IN_WAY        = 2;
    public static final int MINIMUM_NODES_IN_CLOSED_WAY = 3;

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
    void addNode(@NonNull final Node node) {
        int size = nodes.size();
        if ((size > 0) && (nodes.get(size - 1) == node)) {
            Log.i(DEBUG_TAG, "addNode attempt to add same node " + node.getOsmId() + " to " + getOsmId());
            return;
        }
        nodes.add(node);
    }

    @Override
    @NonNull
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Be careful to leave at least 2 nodes!
     * 
     * @return list of nodes allowing {@link Iterator#remove()}.
     */
    @NonNull
    Iterator<Node> getNodeIterator() {
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
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    private void toXml(@NonNull final XmlSerializer s, @Nullable Long changeSetId, boolean josm)
            throws IllegalArgumentException, IllegalStateException, IOException {
        checkForNodes();
        s.startTag("", NAME);
        attributesToXml(s, changeSetId, josm);
        for (Node node : nodes) {
            s.startTag("", NODE);
            s.attribute("", REF, Long.toString(node.getOsmId()));
            s.endTag("", NODE);
        }
        tagsToXml(s);
        s.endTag("", NAME);
    }

    /**
     * Check if we have nodes, else throw an exception
     */
    private void checkForNodes() {
        if (nodes == null) {
            Log.i(DEBUG_TAG, "Way without nodes");
            throw new IllegalArgumentException("Way " + getOsmId() + " has no nodes");
        }
    }

    @Override
    public void toAugmentedXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        checkForNodes();
        s.startTag("", NAME);
        attributesToXml(s, null, false);
        if (!isDeleted()) {
            getBounds().toXml(s, null);
            wayNodesToAugmentedXml(s);
            tagsToXml(s);
        }
        s.endTag("", NAME);
    }

    /**
     * @param s
     * @throws IOException
     */
    public void wayNodesToAugmentedXml(XmlSerializer s) throws IOException {
        for (Node node : nodes) {
            s.startTag("", NODE);
            s.attribute("", REF, Long.toString(node.getOsmId()));
            Node.coordToXmlAttr(s, node.getLat(), node.getLon());
            s.endTag("", NODE);
        }
    }

    /**
     * Returns true if "node" is a way node of this way
     * 
     * Will check against the bounding box if it has been set
     * 
     * @param node the Node to check for
     * @return true if the Node is a member of the Way
     */
    public boolean hasNode(final Node node) {
        if (left != Integer.MIN_VALUE) {
            int lonE7 = node.getLon();
            int latE7 = node.getLat();
            return left <= lonE7 && lonE7 <= right && bottom <= latE7 && latE7 <= top && nodes.contains(node);
        }
        return nodes.contains(node);
    }

    /**
     * Returns true if this way has a common node with "way"
     * 
     * @param way the Way to check for a common Node
     * @return true if there is at least one common Node
     */
    public boolean hasCommonNode(@Nullable final Way way) {
        if (way != null) {
            for (Node n : this.nodes) {
                if (way.hasNode(n)) {
                    return true;
                }
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
    public Node getCommonNode(@Nullable final Way way) {
        if (way != null) {
            for (Node n : this.nodes) {
                if (way.hasNode(n)) {
                    return n;
                }
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
     * Remove all nodes from the Way
     * 
     * Only use if you are intending to add nodes immediately
     */
    public void removeAllNodes() {
        nodes.clear();
    }

    @Override
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
     * Note: assumes the Way isn't empty
     * 
     * @param beforeIndex the index of the Node before the one to insert
     * @param newNode the Node to insert
     */
    void addNodeAfter(@NonNull final int beforeIndex, @NonNull final Node newNode) {
        if (nodes.get(beforeIndex) == newNode) { // user error
            Log.i(DEBUG_TAG, "addNodeAfter attempt to add same node");
            return;
        }
        nodes.add(beforeIndex + 1, newNode);
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
    void replaceNode(@NonNull Node existing, @NonNull Node newNode) {
        int idx;
        while ((idx = nodes.indexOf(existing)) != -1) {
            nodes.set(idx, newNode);
            // check for duplicates
            if (idx > 0 && nodes.get(idx - 1).equals(newNode)) {
                Log.w(DEBUG_TAG, "replaceNode node would duplicate preceeding node");
                nodes.remove(idx);
                idx = idx - 1; // correct index for following check
            }
            if (idx >= 0 && idx < nodes.size() - 1 && nodes.get(idx + 1).equals(newNode)) {
                Log.w(DEBUG_TAG, "replaceNode node would duplicate following node");
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
        if (!nodes.isEmpty()) {
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
        String oneway = getTagWithKey(Tags.KEY_ONEWAY);
        if (oneway != null) {
            if (Tags.VALUE_YES.equalsIgnoreCase(oneway) || Tags.VALUE_TRUE.equalsIgnoreCase(oneway) || Tags.VALUE_ONE.equals(oneway)) {
                return 1;
            }
            if (Tags.VALUE_MINUS_ONE.equals(oneway) || Tags.VALUE_REVERSE.equalsIgnoreCase(oneway)) {
                return -1;
            }
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
            return !Tags.VALUE_RIVERBANK.equals(waterway);
        }

        String natural = getTagWithKey(Tags.KEY_NATURAL);
        if ((natural != null) && (natural.equals(Tags.VALUE_CLIFF) || natural.equals(Tags.VALUE_COASTLINE) || natural.equals(Tags.VALUE_EARTH_BANK))) {
            return true;
        }

        String manMade = getTagWithKey(Tags.KEY_MAN_MADE);
        if ((manMade != null) && manMade.equals(Tags.VALUE_EMBANKMENT)) {
            return true; // IHMO
        }

        String highway = getTagWithKey(Tags.KEY_HIGHWAY);
        if (highway != null && Tags.VALUE_MOTORWAY.equals(highway)) {
            return true;
        }

        String barrier = getTagWithKey(Tags.KEY_BARRIER);
        if (barrier == null) {
            return false;
        }
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
        return Tags.VALUE_CITY_WALL.equals(barrier) && (twoSided == null || !Tags.VALUE_YES.equals(twoSided));
    }

    @Override
    public ElementType getType() {
        if (elementType == null) {
            elementType = getType(tags);
        }
        return elementType;
    }

    @Override
    public ElementType getType(Map<String, String> tags) {
        if (nodes.size() >= MINIMUM_NODES_IN_WAY && isClosed()) {
            /*
             * From a systematic pov it would be better to get this from a preset, however the current matching preset
             * isn't available here and using the style is far cheaper.
             */
            if (tags != null && (Tags.VALUE_YES.equals(tags.get(Tags.KEY_AREA)) || (style != null && style.isArea()))) {
                return ElementType.AREA;
            }
            return ElementType.CLOSEDWAY;
        }
        return ElementType.WAY;
    }

    @Override
    public boolean setTags(Map<String, String> tags) {
        // changing tags might change type
        elementType = null;
        return super.setTags(tags);
    }

    @Override
    public int nodeCount() {
        return nodes == null ? 0 : nodes.size();
    }

    /**
     * Count the occurrences of node
     * 
     * @param node the Node
     * @return the number of times node is present
     */
    public int count(@NonNull Node node) {
        int result = 0;
        for (Node n : nodes) {
            if (node.equals(n)) {
                result++;
            }
        }
        return result;
    }

    @Override
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

    @Override
    public double getMinDistance(final int[] location) {
        double distance = Double.MAX_VALUE;
        int len = nodes.size();
        if (location != null && len > 0) {
            Node n1 = nodes.get(0);
            for (int i = 1; i < len; i++) {
                // distance to nodes of way
                Node n2 = nodes.get(i);
                // distance to lines of way
                distance = Math.min(distance, GeoMath.getLineDistance(location[0], location[1], n1.getLon(), n1.getLat(), n2.getLon(), n2.getLat()));
                n1 = n2;
            }
        }
        return distance;
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
     * Called if geometry has changed and cached bbox is invalid
     */
    public void invalidateBoundingBox() {
        left = Integer.MIN_VALUE;
        // changing geometry might chage the type
        elementType = null;
    }

    /**
     * Returns a bounding box covering the way
     * 
     * @return the bounding box of the way
     */
    @NonNull
    public BoundingBox getBounds() {
        return getBounds(new BoundingBox());
    }

    /**
     * Returns a bounding box covering the way
     * 
     * @param result a bounding box to use for producing the result, avoids creating an object instance
     * @return the bounding box of the way
     */
    @Override
    public BoundingBox getBounds(@NonNull BoundingBox result) {
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