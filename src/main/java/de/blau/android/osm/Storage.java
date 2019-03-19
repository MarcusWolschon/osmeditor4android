package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.StorageException;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.LongOsmElementMap;

/**
 * Container for OSM data
 * 
 *
 */
public class Storage implements Serializable {

    private static final String DEBUG_TAG = "Storage";

    private static final long serialVersionUID = 3838107046050083566L;

    private final LongOsmElementMap<Node> nodes;

    private final LongOsmElementMap<Way> ways;

    private final LongOsmElementMap<Relation> relations;

    private final List<BoundingBox> bboxes;

    private transient LongHashSet nodeIsRef;

    /**
     * Default constructor
     * <p>
     * Initializes the storage and adds a maximum valid mercator size bounding box
     */
    public Storage() {
        nodes = new LongOsmElementMap<>(1000);
        ways = new LongOsmElementMap<>();
        relations = new LongOsmElementMap<>();
        bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>());
        // a default entry may not make sense
        bboxes.add(new BoundingBox(-BoundingBox.MAX_LON_E7, -BoundingBox.MAX_LAT_E7, BoundingBox.MAX_LON_E7, BoundingBox.MAX_LAT_E7));
    }

    /**
     * Construct a new storage object with the contents of an existing one
     * 
     * @param s storage object to duplicate
     */
    Storage(Storage s) {
        nodes = new LongOsmElementMap<>(s.nodes);
        ways = new LongOsmElementMap<>(s.ways);
        relations = new LongOsmElementMap<>(s.relations);
        bboxes = Collections.synchronizedList(new ArrayList<>(s.bboxes));
    }

    /**
     * Get a specific node by id
     * 
     * @param nodeOsmId id of the node
     * @return the node or null if not found
     */
    @Nullable
    public Node getNode(final long nodeOsmId) {
        return nodes.get(nodeOsmId);
    }

    /**
     * Get a specific way by id
     * 
     * @param wayOsmId id of the way
     * @return the way or null if not found
     */
    @Nullable
    public Way getWay(final long wayOsmId) {
        return ways.get(wayOsmId);
    }

    /**
     * Get a specific relation by id
     * 
     * @param relationOsmId id of the relation
     * @return the relation or null if not found
     */
    @Nullable
    public Relation getRelation(final long relationOsmId) {
        return relations.get(relationOsmId);
    }

    /**
     * Get a OsmElement
     * 
     * @param type the element type as a String (NODE, WAY, RELATION)
     * @param osmId the id
     * @return the OsmElement or null if not found
     */
    @Nullable
    public OsmElement getOsmElement(final String type, final long osmId) {
        if (type.equalsIgnoreCase(Node.NAME)) {
            return getNode(osmId);
        } else if (type.equalsIgnoreCase(Way.NAME)) {
            return getWay(osmId);
        } else if (type.equalsIgnoreCase(Relation.NAME)) {
            return getRelation(osmId);
        }
        return null;
    }

    /**
     * Get a unmodifiable list of all nodes
     * 
     * @return list containing all nodes
     */
    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes.values());
    }

    /**
     * Return all nodes in a bounding box
     * 
     * Notes: - currently this does a sequential scan of all nodes - zsing a for loop instead of for each is twice as
     * fast
     * 
     * @param box bounding box to search in
     * @return a list of all nodes in box
     */
    public List<Node> getNodes(BoundingBox box) {
        ArrayList<Node> result = new ArrayList<>(nodes.size());
        List<Node> list = nodes.values();
        int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            Node n = list.get(i);
            if (box.isIn(n.getLon(), n.getLat())) {
                result.add(n);
            }
        }
        return result;
    }

    /**
     * Get a unmodifiable list of all ways
     * 
     * @return list containing all ways
     */
    public List<Way> getWays() {
        return Collections.unmodifiableList(ways.values());
    }

    /**
     * Return all ways covered or possibly intersecting a bounding box
     * <p>
     * Note: currently this does a sequential scan of all ways
     * 
     * @param box bounding box to search in
     * @return a list of all ways in box
     */
    public List<Way> getWays(@NonNull BoundingBox box) {
        List<Way> result = new ArrayList<>(ways.size());
        BoundingBox newBox = new BoundingBox(); // avoid creating new instances
        List<Way> list = ways.values();
        int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            Way w = list.get(i);
            BoundingBox wayBox = w.getBounds(newBox);
            if (wayBox.intersects(box)) {
                result.add(w);
            }
        }
        return result;
    }

    /**
     * Get a unmodifiable list of all relations
     * 
     * @return list containing all relations
     */
    public List<Relation> getRelations() {
        return Collections.unmodifiableList(relations.values());
    }

    /**
     * Get a unmodifiable list of all elements
     * 
     * @return list containing all elements
     */
    public List<OsmElement> getElements() {
        List<OsmElement> l = new ArrayList<>();
        l.addAll(nodes.values());
        l.addAll(ways.values());
        l.addAll(relations.values());
        return Collections.unmodifiableList(l);
    }

    /**
     * Test if an element is present in storage
     * 
     * @param element element to check for
     * @return true if element is in storage
     */
    public boolean contains(final OsmElement element) {
        if (element instanceof Way) {
            return ways.containsKey(element.getOsmId());
        } else if (element instanceof Node) {
            return nodes.containsKey(element.getOsmId());
        } else if (element instanceof Relation) {
            return relations.containsKey(element.getOsmId());
        }
        return false;
    }

    /**
     * Insert a node in to storage regardless of it is already present or not
     * 
     * @param node node to insert
     */
    void insertNodeUnsafe(final Node node) {
        try {
            nodes.put(node.getOsmId(), node);
        } catch (OutOfMemoryError err) {
            throw new StorageException(StorageException.OOM);
        }

    }

    /**
     * Insert a way in to storage regardless of it is already present or not
     * 
     * @param way way to insert
     */
    void insertWayUnsafe(final Way way) {
        try {
            ways.put(way.getOsmId(), way);
        } catch (OutOfMemoryError err) {
            throw new StorageException(StorageException.OOM);
        }
    }

    /**
     * Insert a relation in to storage regardless of it is already present or not
     * 
     * @param relation relation to insert
     */
    void insertRelationUnsafe(final Relation relation) {
        try {
            relations.put(relation.getOsmId(), relation);
        } catch (OutOfMemoryError err) {
            throw new StorageException(StorageException.OOM);
        }
    }

    /**
     * Insert an element if it is not already present in storage
     * <p>
     * Note: the current data structures do not allow multiple entries for the same object in any case
     * 
     * @param element element to insert
     */
    void insertElementSafe(final OsmElement element) {
        if (!contains(element)) {
            insertElementUnsafe(element);
        }
    }

    /**
     * Insert an element in to storage regardless of it is already present or not
     * 
     * @param element element to insert
     */
    void insertElementUnsafe(final OsmElement element) {
        if (element instanceof Way) {
            insertWayUnsafe((Way) element);
        } else if (element instanceof Node) {
            insertNodeUnsafe((Node) element);
        } else if (element instanceof Relation) {
            insertRelationUnsafe((Relation) element);
        }
    }

    /**
     * Remove a node from storage
     * 
     * @param node node to remove
     * @return true if the node was in storage
     */
    boolean removeNode(final Node node) {
        return nodes.remove(node.getOsmId()) != null;
    }

    /**
     * Remove a way from storage
     * 
     * @param way way to remove
     * @return true if the way was in storage
     */
    boolean removeWay(final Way way) {
        return ways.remove(way.getOsmId()) != null;
    }

    /**
     * Remove a relation from storage
     * 
     * @param relation relation to remove
     * @return true if the relation was in storage
     */
    boolean removeRelation(final Relation relation) {
        return relations.remove(relation.getOsmId()) != null;
    }

    /**
     * Remove an element of any type from storage
     * 
     * @param element element to remove
     * @return true if the element was in storage
     */
    boolean removeElement(final OsmElement element) {
        if (element instanceof Way) {
            return ways.remove(element.getOsmId()) != null;
        } else if (element instanceof Node) {
            return nodes.remove(element.getOsmId()) != null;
        } else if (element instanceof Relation) {
            return relations.remove(element.getOsmId()) != null;
        }
        return false;
    }

    /**
     * Get all bounding boxes of downloaded data
     * 
     * @return all bounding boxes
     */
    @NonNull
    public List<BoundingBox> getBoundingBoxes() {
        return bboxes;
    }

    /**
     * Resets bounding box list and adds this boundingbox
     * 
     * @param bbox bounding box to add
     */
    void setBoundingBox(@NonNull final BoundingBox bbox) {
        bboxes.clear();
        bboxes.add(bbox);
    }

    /**
     * Add this bounding box to list
     * 
     * @param bbox bounding box to add
     */
    void addBoundingBox(final BoundingBox bbox) {
        bboxes.add(bbox);
    }

    /**
     * Remove bounding box from list
     * 
     * @param box bounding box to remove
     */
    public void deleteBoundingBox(BoundingBox box) {
        bboxes.remove(box);
    }

    /**
     * Return true if this storage is empty
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return nodes.isEmpty() && ways.isEmpty() && relations.isEmpty();
    }

    /**
     * Get all ways that node is a vertex of
     * 
     * This method currently does a sequential scan of all ways in storage and should be avoided
     * 
     * @param node node to search for
     * @return list containing all ways containing node
     */
    @NonNull
    public List<Way> getWays(@NonNull final Node node) {
        List<Way> mWays = new ArrayList<>();
        // BoundingBox box = new BoundingBox();
        for (Way way : ways) {
            // box = way.getBounds(box);
            if (/* box.contains(node.getLon(), node.getLat()) && */way.hasNode(node)) {
                mWays.add(way);
            }
        }
        return mWays;
    }

    /**
     * Get all nodes that are vertexes in a way
     * <p>
     * This method currently does a sequential scan of all ways in storage and should be avoided
     * 
     * @return all way nodes
     */
    public List<Node> getWayNodes() {
        List<Node> waynodes = new ArrayList<>();
        for (Way way : ways) {
            waynodes.addAll(way.getNodes());
        }
        return waynodes;
    }

    /**
     * Tests if node is first or last node of any way in storage
     * <p>
     * This method currently does a sequential scan of all ways in storage and should be avoided
     * 
     * @param node node to check
     * @return true if node is the first or last node of at least one way
     */
    public boolean isEndNode(final Node node) {
        for (Way way : ways) {
            if (way.isEndNode(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate a bounding box just covering the data
     * 
     * @return a bounding box
     * @throws OsmException if no valid BoundingBox could be created
     */
    public BoundingBox calcBoundingBoxFromData() throws OsmException {
        int top = 0;
        int bottom = 0;
        int left = 0;
        int right = 0;

        if (nodes != null) {
            for (Node n : nodes) {
                if (n.getLat() > top) {
                    top = n.getLat();
                } else if (n.getLat() < bottom) {
                    bottom = n.getLat();
                }
                if (n.getLon() > right) {
                    right = n.getLon();
                } else if (n.getLon() < left) {
                    left = n.getLon();
                }
            }
        }
        return new BoundingBox(left, bottom, right, top);
    }

    /**
     * Get the node map
     * 
     * @return the map indexing nodes
     */
    public LongOsmElementMap<Node> getNodeIndex() {
        return nodes;
    }

    /**
     * Get the way map
     * 
     * @return the map indexing ways
     */
    public LongOsmElementMap<Way> getWayIndex() {
        return ways;
    }

    /**
     * Get the relation map
     * 
     * @return the map indexing relations
     */
    public LongOsmElementMap<Relation> getRelationIndex() {
        return relations;
    }

    /**
     * Rehash the maps used for storing elements.
     * <p>
     * This is required since elements will change their id when being saved to the OSM database the first time.
     */
    public void rehash() {
        nodes.rehash();
        ways.rehash();
        relations.rehash();
    }

    /**
     * Log the contents
     */
    public void logStorage() {
        //
        for (Node n : nodes) {
            Log.d(DEBUG_TAG, "Node " + n.getOsmId());
            for (String k : n.getTags().keySet()) {
                Log.d(DEBUG_TAG, k + "=" + n.getTags().get(k));
            }
        }
        for (Way w : ways) {
            Log.d(DEBUG_TAG, "Way " + w.getOsmId());
            for (String k : w.getTags().keySet()) {
                Log.d(DEBUG_TAG, k + "=" + w.getTags().get(k));
            }
            for (Node nd : w.getNodes()) {
                Log.d(DEBUG_TAG, "\t" + nd.getOsmId());
            }
        }
        for (Relation r : relations) {
            Log.d(DEBUG_TAG, "Relation " + r.getOsmId());
            for (String k : r.getTags().keySet()) {
                Log.d(DEBUG_TAG, k + "=" + r.getTags().get(k));
            }
            for (RelationMember rm : r.getMembers()) {
                Log.d(DEBUG_TAG, "\t" + rm.getRef() + " " + rm.getRole());
            }
        }
    }

    /**
     * Indicate that the Node is referenced by a Way
     * 
     * @param id the Nodes id
     */
    public synchronized void addNodeRef(long id) {
        if (nodeIsRef == null) {
            nodeIsRef = new LongHashSet();
        }
        nodeIsRef.put(id);
    }

    /**
     * Remove all unreferenced nodes that are not in the bounding box
     * 
     * Providing this here allows us to directly merge objects in to the same Storage instance and complete the trimming
     * once after all data has been loaded.
     * 
     * @param box the BoundingBox
     */
    public synchronized void removeUnreferencedNodes(@NonNull BoundingBox box) {
        if (nodeIsRef != null) {
            for (Node nd : getNodes()) {
                if (!nodeIsRef.contains(nd.getOsmId()) && !box.contains(nd.getLon(), nd.getLat())) {
                    removeNode(nd);
                }
            }
        }
        nodeIsRef = null;
    }
}
