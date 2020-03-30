package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        bboxes = new ArrayList<>();
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
        bboxes = new ArrayList<>(s.bboxes);
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
    public OsmElement getOsmElement(@NonNull final String type, final long osmId) {
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
    @NonNull
    public List<Node> getNodes(@NonNull BoundingBox box) {
        List<Node> result = new ArrayList<>(nodes.size());
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
     * Get how many nodes there are in storage
     * 
     * @return the current Node count
     */
    public int getNodeCount() {
        return nodes.size();
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
    @NonNull
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
     * Get how many ways there are in storage
     * 
     * @return the current Way count
     */
    public int getWayCount() {
        return ways.size();
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
     * Get how many relations there are in storage
     * 
     * @return the current Relation count
     */
    public int getRelationCount() {
        return relations.size();
    }

    /**
     * Get a unmodifiable list of all elements
     * 
     * @return list containing all elements
     */
    @NonNull
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
    public boolean contains(@Nullable final OsmElement element) {
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
    void insertNodeUnsafe(@NonNull final Node node) {
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
    void insertWayUnsafe(@NonNull final Way way) {
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
    void insertRelationUnsafe(@NonNull final Relation relation) {
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
    void insertElementSafe(@Nullable final OsmElement element) {
        if (!contains(element)) {
            insertElementUnsafe(element);
        }
    }

    /**
     * Insert an element in to storage regardless of it is already present or not
     * 
     * @param element element to insert
     */
    void insertElementUnsafe(@Nullable final OsmElement element) {
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
    boolean removeNode(@NonNull final Node node) {
        return nodes.remove(node.getOsmId()) != null;
    }

    /**
     * Remove a way from storage
     * 
     * @param way way to remove
     * @return true if the way was in storage
     */
    boolean removeWay(@NonNull final Way way) {
        return ways.remove(way.getOsmId()) != null;
    }

    /**
     * Remove a relation from storage
     * 
     * @param relation relation to remove
     * @return true if the relation was in storage
     */
    boolean removeRelation(@NonNull final Relation relation) {
        return relations.remove(relation.getOsmId()) != null;
    }

    /**
     * Remove an element of any type from storage
     * 
     * @param element element to remove
     * @return true if the element was in storage
     */
    boolean removeElement(@Nullable final OsmElement element) {
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
     * Get an unmodifiable List of all bounding boxes of downloaded data
     * 
     * @return all bounding boxes
     */
    @NonNull
    public synchronized List<BoundingBox> getBoundingBoxes() {
        return Collections.unmodifiableList(bboxes);
    }

    /**
     * Resets bounding box list and adds this boundingbox
     * 
     * @param bbox bounding box to add
     */
    synchronized void setBoundingBox(@NonNull final BoundingBox bbox) {
        bboxes.clear();
        bboxes.add(bbox);
    }

    /**
     * Add this bounding box to list
     * 
     * @param bbox bounding box to add
     */
    synchronized void addBoundingBox(@NonNull final BoundingBox bbox) {
        if (!bboxes.contains(bbox)) {
            bboxes.add(bbox);
        }
    }

    /**
     * Remove bounding box from list
     * 
     * @param box bounding box to remove
     */
    public synchronized void deleteBoundingBox(@NonNull BoundingBox box) {
        bboxes.remove(box);
    }

    /**
     * Get the last added bounding box
     * 
     * @return the last BoundingBox or one covering the whole mercator extent
     */
    @NonNull
    synchronized BoundingBox getLastBox() {
        int s = bboxes.size();
        if (s > 0) {
            return bboxes.get(s - 1);
        }
        Log.e(DEBUG_TAG, "Bounding box list empty");
        return ViewBox.getMaxMercatorExtent(); // full extent
    }

    /**
     * Clear bounding box list
     */
    public synchronized void clearBoundingBoxList() {
        bboxes.clear();
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
    @NonNull
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
    public boolean isEndNode(@Nullable final Node node) {
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
    @NonNull
    public BoundingBox calcBoundingBoxFromData() throws OsmException {
        BoundingBox result = null;
        if (nodes != null) {
            for (Node n : nodes) {
                if (result == null) {
                    result = new BoundingBox(n.getLon(),n.getLat());
                } else {
                    result.union(n.getLon(),n.getLat());
                }
            }
        }
        if (result == null) {
            throw new OsmException("Coudn't construct a bounding box from data");
        }
        return result;
    }

    /**
     * Get the node map
     * 
     * @return the map indexing nodes
     */
    @NonNull
    public LongOsmElementMap<Node> getNodeIndex() {
        return nodes;
    }

    /**
     * Get the way map
     * 
     * @return the map indexing ways
     */
    @NonNull
    public LongOsmElementMap<Way> getWayIndex() {
        return ways;
    }

    /**
     * Get the relation map
     * 
     * @return the map indexing relations
     */
    @NonNull
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
