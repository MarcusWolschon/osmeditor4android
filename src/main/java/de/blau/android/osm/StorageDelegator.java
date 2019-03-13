package de.blau.android.osm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.exception.StorageException;
import de.blau.android.filter.Filter;
import de.blau.android.osm.MergeResult.Issue;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SavingHelper.Exportable;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;
import de.blau.android.util.collections.LongOsmElementMap;

public class StorageDelegator implements Serializable, Exportable {

    private static final String DEBUG_TAG = "StorageDelegator";

    private static final long serialVersionUID = 9L;

    private Storage currentStorage;

    private Storage apiStorage;

    private UndoStorage undo;

    private ClipboardStorage clipboard;

    private ArrayList<String> imagery;

    /**
     * when reading state lockout writing/reading
     */
    private transient ReentrantLock readingLock = new ReentrantLock();

    /**
     * Indicates whether changes have been made since the last save to disk. Since a newly created storage is not saved,
     * the constructor sets it to true. After a successful save or load, it is set to false. If it is false, save does
     * nothing.
     */
    private transient boolean dirty;

    /**
     * if false we need to check if the current imagery has been recorded
     */
    private transient boolean imageryRecorded = false;

    public static final String FILENAME = "lastActivity.res";

    private transient SavingHelper<StorageDelegator> savingHelper = new SavingHelper<>();

    /**
     * A OsmElementFactory that is used to create new elements. Needs to be persisted together with
     * currentStorage/apiStorage to avoid duplicate IDs when the application is restarted after some elements have been
     * created.
     */
    private OsmElementFactory factory;

    /**
     * Construct a new empty instance
     */
    public StorageDelegator() {
        reset(false); // don't set dirty on instantiation
    }

    /**
     * Reset this instance to emply state
     * 
     * @param dirty if true mark the (empty) contents as dirty (this is useful because if true old state files will be
     *            overwritten)
     */
    public void reset(boolean dirty) {
        this.dirty = dirty;
        apiStorage = new Storage();
        currentStorage = new Storage();
        clipboard = new ClipboardStorage();
        undo = new UndoStorage(currentStorage, apiStorage);
        factory = new OsmElementFactory();
        imagery = new ArrayList<>();
    }

    /**
     * Replace the current Storage object with a new one clipboard and api storage will be reset
     * 
     * @param currentStorage the new Storage object to set
     */
    public void setCurrentStorage(@NonNull final Storage currentStorage) {
        dirty = true;
        apiStorage = new Storage();
        clipboard = new ClipboardStorage();
        this.currentStorage = currentStorage;
        undo = new UndoStorage(currentStorage, apiStorage);
    }

    /**
     * Check if the Storage is dirty and needs to be saved
     * 
     * @return true if dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * set dirty to true
     */
    public void dirty() {
        dirty = true;
        Log.d(DEBUG_TAG, "setting delegator to dirty");
    }

    /**
     * Get the current undo instance. For immediate use only - DO NOT CACHE THIS.
     * 
     * @return the UndoStorage, allowing operations like creation of checkpoints and undo/redo.
     */
    public UndoStorage getUndo() {
        return undo;
    }

    /**
     * Clears the undo storage.
     */
    public void clearUndo() {
        undo = new UndoStorage(currentStorage, apiStorage);
    }

    /**
     * Get the current OsmElementFactory instance used by this delegator. Use only the factory returned by this to
     * create new element IDs for insertion into this delegator! For immediate use only - DO NOT CACHE THIS.
     * 
     * @return the OsmElementFactory for creating nodes/ways with new IDs
     */
    public OsmElementFactory getFactory() {
        return factory;
    }

    /**
     * Insert a new element in to storage
     * 
     * Uses methods that are nops if theelement already is present
     * 
     * @param elem the element to insert
     */
    public void insertElementSafe(@NonNull final OsmElement elem) {
        dirty = true;
        undo.save(elem);
        try {
            currentStorage.insertElementSafe(elem);
            apiStorage.insertElementSafe(elem);
            onElementChanged((List<OsmElement>) null, (List<OsmElement>) null);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "insertElementSafe got " + e.getMessage());
        }
    }

    /**
     * Insert a new element in to storage
     * 
     * @param elem the element to insert
     */
    private void insertElementUnsafe(@NonNull final OsmElement elem) {
        dirty = true;
        undo.save(elem);
        try {
            currentStorage.insertElementUnsafe(elem);
            apiStorage.insertElementUnsafe(elem);
            onElementChanged((List<OsmElement>) null, (List<OsmElement>) null);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "insertElementUnsafe got " + e.getMessage());
        }
    }

    /**
     * Sets the tags of the element, replacing all existing ones
     * 
     * @param elem the element to tag
     * @param tags the new tags
     */
    public void setTags(@NonNull final OsmElement elem, @Nullable final Map<String, String> tags) {
        dirty = true;
        undo.save(elem);

        if (elem.setTags(tags)) {
            // OsmElement tags have changed
            elem.updateState(OsmElement.STATE_MODIFIED);
            elem.stamp();
            elem.resetHasProblem();
            try {
                apiStorage.insertElementSafe(elem);
                onElementChanged(null, elem);
            } catch (StorageException e) {
                // TODO handle OOM
                Log.e(DEBUG_TAG, "setTags got " + e.getMessage());
            }
        }
    }

    /**
     * Called after an element has been changed
     * 
     * As it may be fairly expensive to determine all changes pre and/or post may be null Don't call this if just the
     * node positions have changed
     * 
     * @param pre list of changed elements before the operation or null
     * @param post list of changed elements after the operation or null
     */
    private void onElementChanged(@Nullable List<OsmElement> pre, @Nullable List<OsmElement> post) {
        if (post != null) {
            boolean nodeMoved = false;
            BoundingBox changed = null;
            for (OsmElement e : post) {
                e.stamp();
                e.resetHasProblem();
                if (Way.NAME.equals(e.getName())) {
                    ((Way) e).invalidateBoundingBox();
                } else if (Node.NAME.equals(e.getName())) {
                    nodeMoved = true;
                    if (changed == null) {
                        changed = e.getBounds();
                    } else {
                        changed.union(e.getBounds());
                    }
                }
            }
            if (nodeMoved) {
                for (Way w : currentStorage.getWays(changed)) {
                    for (OsmElement e : post) {
                        if (e instanceof Node && w.hasNode((Node) e)) {
                            w.invalidateBoundingBox();
                            w.resetHasProblem();
                            break; // only do this once per way
                        }
                    }
                }
            }
        }
        Filter filter = App.getLogic().getFilter();
        if (filter != null) {
            filter.onElementChanged(pre, post);
        }
    }

    /**
     * Called after an element has been changed
     * 
     * As it may be fairly expensive to determine all changes pre and/or post may be null Don't call this if just the
     * node positions have changed
     * 
     * @param pre changed element before the operation or null
     * @param post changed element after the operation or null
     */
    private void onElementChanged(@Nullable OsmElement pre, @Nullable OsmElement post) {
        List<OsmElement> preList = null;
        List<OsmElement> postList = null;

        if (pre != null) {
            preList = new ArrayList<>();
            preList.add(pre);
        }
        if (post != null) {
            postList = new ArrayList<>();
            postList.add(post);
        }
        onElementChanged(preList, postList);
    }

    /**
     * Way geometry has to be invalidated -before- nodes are moved
     * 
     * @param nodes List of nodes that are going to change
     */
    private void invalidateWayBoundingBox(@NonNull Collection<Node> nodes) {
        // this would seem to be very complicated, however
        // a trivial implementation would be very expensive
        // even just for a single long way.
        // This way of doing it collects all candidate ways
        // first and then invalidates each of them max. once.
        if (!nodes.isEmpty()) {
            BoundingBox box = null;
            boolean first = true;
            for (Node n : nodes) {
                if (first) {
                    box = new BoundingBox(n.lon, n.lat);
                    first = false;
                } else {
                    box.union(n.lon, n.lat);
                }
            }
            List<Way> ways = currentStorage.getWays(box);
            if (ways.size() == 1) { // optimize the common case
                Way w = ways.get(0);
                w.invalidateBoundingBox();
                if (w.hasTagKey(Tags.KEY_HIGHWAY)) {
                    // we only validate way connections for highways currently
                    w.resetHasProblem();
                }
            } else {
                for (Way w : new HashSet<>(ways)) {
                    for (Node n : nodes) {
                        if (w.getNodes().contains(n)) {
                            w.invalidateBoundingBox();
                            if (w.hasTagKey(Tags.KEY_HIGHWAY)) {
                                w.resetHasProblem();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Way geometry has to be invalidated -before- nodes are moved
     * 
     * @param node node that is going to change
     */
    private void invalidateWayBoundingBox(@NonNull Node node) {
        List<Node> nodeList = new ArrayList<>();
        nodeList.add(node);
        invalidateWayBoundingBox(nodeList);
    }

    /**
     * Store the currently used imagery
     * 
     * @param map the current Map instance
     */
    public void recordImagery(@Nullable de.blau.android.Map map) {
        if (!imageryRecorded) { // flag is reset when we change imagery
            try {
                if (map != null) { // currently we only modify data when the map exists
                    List<String> currentImagery = map.getImageryNames();
                    for (String i : currentImagery) {
                        if (!imagery.contains(i) && !"None".equalsIgnoreCase(i)) {
                            imagery.add(i);
                        }
                    }
                    imageryRecorded = true;
                }
            } catch (Exception ignored) { // never fail on anything here
            } catch (Error ignored) {
            }
        }
    }

    /**
     * Set the imageryRecorded flag
     * 
     * @param recorded the new state of the flag
     */
    public void setImageryRecorded(boolean recorded) {
        imageryRecorded = recorded;
    }

    /**
     * Reset the cached "problems" for all OsmElements
     */
    public void resetProblems() {
        for (OsmElement e : currentStorage.getElements()) {
            e.resetHasProblem();
        }
        for (OsmElement e : apiStorage.getElements()) {
            e.resetHasProblem();
        }
    }

    /**
     * Create apiStorage (aka the changes to the original data) based on state field of the elements. Assumes that
     * apiStorage is empty. As a side effect it updates the id sequences for the creation of new elements.
     */
    public synchronized void fixupApiStorage() {
        try {
            long minNodeId = 0;
            long minWayId = 0;
            long minRelationId = 0;
            List<Node> nl = new ArrayList<>(currentStorage.getNodes());
            for (Node n : nl) {
                if (n.getState() != OsmElement.STATE_UNCHANGED) {
                    apiStorage.insertElementUnsafe(n);
                    if (n.getOsmId() < minNodeId) {
                        minNodeId = n.getOsmId();
                    }
                }
                if (n.getState() == OsmElement.STATE_DELETED) {
                    currentStorage.removeElement(n);
                }
            }
            List<Way> wl = new ArrayList<>(currentStorage.getWays());
            for (Way w : wl) {
                if (w.getState() != OsmElement.STATE_UNCHANGED) {
                    apiStorage.insertElementUnsafe(w);
                    if (w.getOsmId() < minWayId) {
                        minWayId = w.getOsmId();
                    }
                }
                if (w.getState() == OsmElement.STATE_DELETED) {
                    currentStorage.removeElement(w);
                }
            }
            List<Relation> rl = new ArrayList<>(currentStorage.getRelations());
            for (Relation r : rl) {
                if (r.getState() != OsmElement.STATE_UNCHANGED) {
                    apiStorage.insertElementUnsafe(r);
                    if (r.getOsmId() < minRelationId) {
                        minRelationId = r.getOsmId();
                    }
                }
                if (r.getState() == OsmElement.STATE_DELETED) {
                    currentStorage.removeElement(r);
                }
            }
            getFactory().setIdSequences(minNodeId, minWayId, minRelationId);
        } catch (StorageException e) {
            // FIXME do something reasonable
            Log.e(DEBUG_TAG, "fixupApiStorage got " + e.getMessage());
        }
    }

    /**
     * Create empty relation
     * 
     * @param members members to add without role
     * @return the new relation
     */
    @NonNull
    public Relation createAndInsertRelation(@Nullable List<OsmElement> members) {
        // undo - nothing done here, way gets saved/marked on insert
        dirty = true;

        Relation relation = factory.createRelationWithNewId();
        insertElementUnsafe(relation);
        if (members != null) {
            for (OsmElement e : members) {
                undo.save(e);
                RelationMember rm = new RelationMember("", e);
                relation.addMember(rm);
                e.addParentRelation(relation);
                onParentRelationChanged(e);
            }
        }
        return relation;
    }

    /**
     * Create a new way with one node
     * 
     * @param firstWayNode the first node
     * @return the new way
     */
    public Way createAndInsertWay(final Node firstWayNode) {
        // undo - nothing done here, way gets saved/marked on insert
        dirty = true;

        Way way = factory.createWayWithNewId();
        way.addNode(firstWayNode);
        insertElementUnsafe(way);
        return way;
    }

    /**
     * Add a node at the end of a way
     * 
     * @param node the node to add
     * @param way the way to add the node to
     * @throws OsmIllegalOperationException if the operation would result in an object violating an OSM specific
     *             constraint
     */
    public void addNodeToWay(final Node node, final Way way) throws OsmIllegalOperationException {
        dirty = true;
        undo.save(way);

        try {
            validateWayNodeCount(way.nodeCount() + 1);
            apiStorage.insertElementSafe(way);
            way.addNode(node);
            way.updateState(OsmElement.STATE_MODIFIED);
            onElementChanged(null, way);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "addNodeToWay got " + e.getMessage());
        }
    }

    /**
     * Check the future node count against the maximum supported by the current API
     * 
     * @param newCount the node count we would like to have
     * @throws OsmIllegalOperationException if the count is larger than the maximum supported
     */
    private void validateWayNodeCount(final int newCount) {
        Preferences prefs = new Preferences(App.getCurrentInstance());
        if (newCount > prefs.getServer().getCachedCapabilities().getMaxWayNodes()) {
            throw new OsmIllegalOperationException(App.resources().getString(R.string.exception_too_many_nodes));
        }
    }

    /**
     * Add a node to a way after a specified node
     * 
     * @param nodeBefore existing way node the new node is to be added after
     * @param newNode the new way node
     * @param way the way to perform the operation on
     * @throws OsmIllegalOperationException if the operation would result in an object violating an OSM specific
     *             constraint
     */
    public void addNodeToWayAfter(final Node nodeBefore, final Node newNode, final Way way) throws OsmIllegalOperationException {
        dirty = true;
        undo.save(way);
        try {
            validateWayNodeCount(way.nodeCount() + 1);
            apiStorage.insertElementSafe(way);
            way.addNodeAfter(nodeBefore, newNode);
            way.updateState(OsmElement.STATE_MODIFIED);
            onElementChanged(null, way);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "addNodeToWayAfter got " + e.getMessage());
        }
    }

    /**
     * Append or prepend a node to a way depending of if refNode is the last or first element of the way
     * 
     * @param refNode last or first way node
     * @param nextNode the new node to add
     * @param way the way to perform the operation on
     * @throws OsmIllegalOperationException if the operation would result in an object violating an OSM specific
     *             constraint
     */
    public void appendNodeToWay(final Node refNode, final Node nextNode, final Way way) throws OsmIllegalOperationException {
        dirty = true;
        undo.save(way);
        try {
            validateWayNodeCount(way.nodeCount() + 1);
            apiStorage.insertElementSafe(way);
            way.appendNode(refNode, nextNode);
            way.updateState(OsmElement.STATE_MODIFIED);
            onElementChanged(null, way);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "appendNodeToWay got " + e.getMessage());
        }
    }

    /**
     * Move a node to a new position
     * 
     * @param node the node to move
     * @param latE7 the new latitude (E7)
     * @param lonE7 the new longitude (E7)
     */
    public void moveNode(@NonNull final Node node, final int latE7, final int lonE7) {
        validateCoordinates(latE7, lonE7);
        dirty = true;
        undo.save(node);
        try {
            invalidateWayBoundingBox(node);
            updateLatLon(node, latE7, lonE7);
            onElementChanged(null, node);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "updateLatLon got " + e.getMessage());
        }
    }

    /**
     * Update the position of a Node
     * 
     * @param node the Node to update
     * @param latE7 new WGS84*1E7 latitude
     * @param lonE7 new WGS84*1E7 longitude
     */
    private void updateLatLon(@NonNull final Node node, final int latE7, final int lonE7) {
        apiStorage.insertElementSafe(node);
        node.setLat(latE7);
        node.setLon(lonE7);
        node.updateState(OsmElement.STATE_MODIFIED);
    }

    /**
     * Move all nodes in a way, since the nodes keep their ids, the way itself doesn't change and doesn't need to be
     * saved apply translation only once to every node
     * 
     * @param way way containing the nodes
     * @param deltaLatE7 the delta to move the latitude (E7)
     * @param deltaLonE7 the delta to move the longitude (E7)
     */
    public void moveWay(@NonNull final Way way, final int deltaLatE7, final int deltaLonE7) {
        moveNodes(way.getNodes(), deltaLatE7, deltaLonE7);
    }

    /**
     * Check that the new position would still be valid This should be done before the operation in question
     *
     * @param newLatE7 the new latitude (WGS84*1E7)
     * @param newLonE7 the new longitude (WGS84*1E7)
     * @throws OsmIllegalOperationException if the new position would be off world
     */
    void validateCoordinates(final int newLatE7, final int newLonE7) throws OsmIllegalOperationException {
        if (newLatE7 > GeoMath.MAX_LAT_E7 || newLatE7 < -GeoMath.MAX_LAT_E7) {
            Log.e(DEBUG_TAG, "lat of " + newLatE7 + " is invalid");
            throw new OsmIllegalOperationException("lat of " + newLatE7 + " is invalid");
        }
        if (newLonE7 > GeoMath.MAX_LON_E7 || newLonE7 < -GeoMath.MAX_LON_E7) {
            Log.e(DEBUG_TAG, "lon of " + newLonE7 + " is invalid");
            throw new OsmIllegalOperationException("lon of " + newLonE7 + " is invalid");
        }
    }

    /**
     * Move a list of nodes, apply translation only once
     * 
     * @param allNodes the list of nodes
     * @param deltaLatE7 the delta to move the latitude (WGS84*1E7)
     * @param deltaLonE7 the delta to move the longitude (WGS84*1E7)
     */
    public void moveNodes(@Nullable final List<Node> allNodes, final int deltaLatE7, final int deltaLonE7) {
        if (allNodes == null) {
            Log.e(DEBUG_TAG, "moveNodes  no nodes!");
            return;
        }
        dirty = true;
        try {
            HashSet<Node> nodes = new HashSet<>(allNodes); // Guarantee uniqueness
            // check that all coordinates are valid before moving
            for (Node nd : nodes) {
                validateCoordinates(nd.getLat() + deltaLatE7, nd.getLon() + deltaLonE7);
            }
            invalidateWayBoundingBox(nodes);
            for (Node nd : nodes) {
                undo.save(nd);
                updateLatLon(nd, nd.getLat() + deltaLatE7, nd.getLon() + deltaLonE7);
            }
            // Don't call onElementChanged
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "moveNodes got " + e.getMessage());
        }
    }

    /**
     * Arrange way nodes in a circle
     * 
     * @param map current map view
     * @param c center of the circle
     * @param way way to circulize
     */
    public void circulizeWay(@NonNull de.blau.android.Map map, @NonNull int[] c, @NonNull Way way) {
        if ((way.getNodes() == null) || (way.getNodes().size() < 3)) {
            Log.d(DEBUG_TAG, "circulize way " + way.getOsmId() + " has no nodes or less than 3!");
            return;
        }
        dirty = true;
        try {
            HashSet<Node> nodes = new HashSet<>(way.getNodes()); // Guarantee uniqueness
            invalidateWayBoundingBox(nodes);
            int width = map.getWidth();
            int height = map.getHeight();
            ViewBox box = map.getViewBox();

            Coordinates coords[] = nodeListToCooardinateArray(width, height, box, new ArrayList<>(nodes));

            // save nodes for undo
            for (Node nd : nodes) {
                undo.save(nd);
            }

            Coordinates center = new Coordinates(GeoMath.lonE7ToX(width, box, c[1]), GeoMath.latE7ToY(height, width, box, c[0]));

            // caclulate average radius
            double r = 0.0f;
            for (Coordinates p : coords) {
                Log.d(DEBUG_TAG, "r=" + Math.sqrt((p.x - center.x) * (p.x - center.x) + (p.y - center.y) * (p.y - center.y)));
                r = r + Math.sqrt((p.x - center.x) * (p.x - center.x) + (p.y - center.y) * (p.y - center.y));
            }
            r = r / coords.length;
            for (Coordinates p : coords) {
                double ratio = r / Math.sqrt((p.x - center.x) * (p.x - center.x) + (p.y - center.y) * (p.y - center.y));
                p.x = ((p.x - center.x) * ratio) + center.x;
                p.y = ((p.y - center.y) * ratio) + center.y;
            }
            int i = 0;
            for (Node nd : nodes) {
                updateLatLon(nd, GeoMath.yToLatE7(height, width, box, (float) coords[i].y), GeoMath.xToLonE7(width, box, (float) coords[i].x));
                i++;
            }
            // Don't call onElementChanged
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "circulizeWay got " + e.getMessage());
        }
    }

    /**
     * Build groups of ways that have common nodes
     * 
     * There must be a better way to do this, but they likely all fall afoul of our current data model
     * 
     * @param ways the ways to group
     * @return a list of list of ways with common nodes
     */
    @NonNull
    private ArrayList<ArrayList<Way>> groupWays(@NonNull List<Way> ways) {
        ArrayList<ArrayList<Way>> groups = new ArrayList<>();
        int group = 0;
        int index = 0;
        int groupIndex = 1;
        groups.add(new ArrayList<>());
        Way startWay = ways.get(index);
        groups.get(group).add(startWay);
        do {
            do {
                for (Node nd : startWay.getNodes()) {
                    for (Way w : ways) {
                        if (w.getNodes().contains(nd) && !groups.get(group).contains(w)) {
                            groups.get(group).add(w);
                        }
                    }
                }
                if (groupIndex < groups.get(group).size()) {
                    startWay = groups.get(group).get(groupIndex);
                    groupIndex++;
                }
            } while (groupIndex < groups.get(group).size());
            // repeat until no new ways are added in the loop

            // find the next way that is not in a group and start a new one
            for (; index < ways.size(); index++) {
                Way w = ways.get(index);
                boolean found = false;
                for (ArrayList<Way> list : groups) {
                    found = found || list.contains(w);
                }
                if (!found) {
                    group++;
                    groups.add(new ArrayList<>());
                    startWay = w;
                    groupIndex = 1;
                    break;
                }
            }
        } while (index < ways.size());

        Log.d(DEBUG_TAG, "number of groups found " + groups.size());
        return groups;
    }

    /**
     * "square" a way/polygon, based on the algorithm used by iD and before that by P2, originally written by Matt Amos
     * If multiple ways are selected the ways are grouped in groups that share nodes and the groups individually
     * squared.
     * 
     * @param map current map view
     * @param ways List of Way to square
     */
    public void orthogonalizeWay(@NonNull de.blau.android.Map map, @NonNull List<Way> ways) {
        final int threshold = 10; // degrees within right or straight to alter
        final double lowerThreshold = Math.cos((90 - threshold) * Math.PI / 180);
        final double upperThreshold = Math.cos(threshold * Math.PI / 180);
        final double epsilon = 1e-4;

        dirty = true;
        try {
            // save nodes for undo
            // adding to a Set first removes duplication
            HashSet<Node> save = new HashSet<>();
            for (Way way : ways) {
                if (way.getNodes() != null) {
                    save.addAll(way.getNodes());
                }
            }
            for (Node nd : save) {
                undo.save(nd);
            }
            invalidateWayBoundingBox(save);
            List<ArrayList<Way>> groups = groupWays(ways);

            int width = map.getWidth();
            int height = map.getHeight();
            ViewBox box = map.getViewBox();

            for (ArrayList<Way> wayList : groups) {
                // Coordinates coords[] = nodeListToCooardinateArray(nodes);
                ArrayList<Coordinates[]> coordsArray = new ArrayList<>();
                int totalNodes = 0;
                for (Way w : wayList) {
                    coordsArray.add(nodeListToCooardinateArray(width, height, box, w.getNodes()));
                    totalNodes += w.getNodes().size();
                }
                Coordinates a, b, c, p, q;

                double loopEpsilon = epsilon * (totalNodes / 4D); // NOTE the original algorithm didn't take the number
                                                                  // of corners in to account

                // iterate until score is low enough
                for (int iteration = 0; iteration < 1000; iteration++) {
                    for (int coordIndex = 0; coordIndex < coordsArray.size(); coordIndex++) {
                        Coordinates[] coords = coordsArray.get(coordIndex);
                        int start = 0;
                        int end = coords.length;
                        if (!wayList.get(coordIndex).isClosed()) {
                            start = 1;
                            end = end - 1;
                        }
                        Coordinates motions[] = new Coordinates[coords.length];
                        for (int i = start; i < end; i++) {
                            a = coords[(i - 1 + coords.length) % coords.length];
                            b = coords[i];
                            c = coords[(i + 1) % coords.length];
                            p = a.subtract(b);
                            q = c.subtract(b);
                            double scale = 2 * Math.min(Math.hypot(p.x, p.y), Math.hypot(q.x, q.y));
                            p = normalize(p, 1.0);
                            q = normalize(q, 1.0);
                            double dotp = filter((p.x * q.x + p.y * q.y), lowerThreshold, upperThreshold);

                            // nasty hack to deal with almost-straight segments (angle is closer to 180 than to 90/270).
                            if (dotp < -0.707106781186547) {
                                dotp += 1.0;
                            }
                            motions[i] = normalize(p.add(q), 0.1 * dotp * scale);
                        }
                        // apply position changes
                        for (int i = start; i < end; i++) {
                            coords[i] = coords[i].add(motions[i]);
                        }
                    }
                    // calculate score
                    double score = 0.0;
                    for (int coordIndex = 0; coordIndex < coordsArray.size(); coordIndex++) {
                        Coordinates[] coords = coordsArray.get(coordIndex);
                        int start = 0;
                        int end = coords.length;
                        if (!wayList.get(coordIndex).isClosed()) {
                            start = 1;
                            end = end - 1;
                        }
                        for (int i = start; i < end; i++) {
                            // yes I know that this -nearly- duplicates the code above
                            a = coords[(i - 1 + coords.length) % coords.length];
                            b = coords[i];
                            c = coords[(i + 1) % coords.length];
                            p = a.subtract(b);
                            q = c.subtract(b);
                            p = normalize(p, 1.0);
                            q = normalize(q, 1.0);
                            double dotp = filter((p.x * q.x + p.y * q.y), lowerThreshold, upperThreshold);

                            score = score + 2.0 * Math.min(Math.abs(dotp - 1.0), Math.min(Math.abs(dotp), Math.abs(dotp + 1.0)));
                        }
                    }
                    // Log.d("StorageDelegator", "orthogonalize way iteration/score " + iteration + "/" + score);
                    if (score < loopEpsilon) {
                        break;
                    }
                }

                // prepare updated nodes for upload
                for (int wayIndex = 0; wayIndex < wayList.size(); wayIndex++) {
                    List<Node> nodes = wayList.get(wayIndex).getNodes();
                    Coordinates[] coords = coordsArray.get(wayIndex);
                    for (int i = 0; i < nodes.size(); i++) {
                        Node nd = nodes.get(i);
                        updateLatLon(nd, GeoMath.yToLatE7(height, width, box, (float) coords[i].y), GeoMath.xToLonE7(width, box, (float) coords[i].x));
                    }
                }
            }
            // Don't call onElementChanged
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "orthogonalizeWay got " + e.getMessage());
        }
    }

    // TODO move this and following code somewhere else and generalize
    private Coordinates normalize(Coordinates p, double scale) {
        Coordinates result = p;
        double length = p.length();
        if (length != 0) {
            result = p.divide(length);
        }
        return result.multiply(scale);
    }

    private double filter(double v, double lower, double upper) {
        return (lower > Math.abs(v)) || (Math.abs(v) > upper) ? v : 0.0;
    }

    /**
     * Wrapper for a screen coordinate tupel
     * 
     * @author simon
     *
     */
    private class Coordinates {
        double x;
        double y;

        /**
         * Construct a new Coordinate object
         * 
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        Coordinates(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Subtract Coordinates from this object
         * 
         * @param s the Coordinates to subtract
         * @return the result of the operation
         */
        @NonNull
        Coordinates subtract(@NonNull Coordinates s) {
            return new Coordinates(this.x - s.x, this.y - s.y);
        }

        /**
         * Add Coordinates to this object
         * 
         * @param p the Coordinates to add
         * @return the result of the operation
         */
        @NonNull
        Coordinates add(@NonNull Coordinates p) {
            return new Coordinates(this.x + p.x, this.y + p.y);
        }

        /**
         * Multiple this object with a scalar value
         * 
         * @param m the scalar value to multiply with
         * @return the result of the operation
         */
        @NonNull
        Coordinates multiply(double m) {
            return new Coordinates((float) (this.x * m), (float) (this.y * m));
        }

        /**
         * Divide this object by a scalar value
         * 
         * @param d the scalar value to divide by
         * @return the result of the operation
         */
        Coordinates divide(double d) {
            return new Coordinates((float) (this.x / d), (float) (this.y / d));
        }

        /**
         * The scalar length
         * 
         * @return the length of this assuming it is a vector from 0,0
         */
        double length() {
            return (float) Math.hypot(x, y);
        }
    }

    /**
     * Convert the coordinates from a list of Nodes to an array of screen coordinates
     * 
     * @param width screen width
     * @param height screen height
     * @param box current ViewBox
     * @param nodes the List of Nodes
     * @return an array of Coordinates
     */
    @NonNull
    private Coordinates[] nodeListToCooardinateArray(int width, int height, @NonNull ViewBox box, @NonNull List<Node> nodes) {
        Coordinates points[] = new Coordinates[nodes.size()];
        // loop over all nodes
        for (int i = 0; i < nodes.size(); i++) {
            points[i] = new Coordinates(0.0f, 0.0f);
            points[i].x = GeoMath.lonE7ToX(width, box, nodes.get(i).getLon());
            points[i].y = GeoMath.latE7ToY(height, width, box, nodes.get(i).getLat());
        }
        return points;
    }

    /**
     * Rotate all nodes in a way, since the nodes keep their ids, the way itself doesn't change and doesn't need to be
     * saved apply translation only once to each node. Rotation is done in screen coords
     * 
     * @param way way to rotate
     * @param angle angle to rotate the way by
     * @param direction rotation direction
     * @param pivotX screen X coordinate of the pivot point
     * @param pivotY screen Y coordinate of the pivot point
     * @param w screen width
     * @param h screen height
     * @param v screen viewbox
     */
    public void rotateWay(@NonNull final Way way, final float angle, final int direction, final float pivotX, final float pivotY, int w, int h,
            @NonNull ViewBox v) {
        if (way.getNodes().isEmpty()) {
            Log.d(DEBUG_TAG, "rotateWay way " + way.getOsmId() + " has no nodes!");
            return;
        }
        // Log.d("StorageDelegator","Rotating " + angle + " around " + pivotY + " " + pivotX );
        dirty = true;
        try {
            HashSet<Node> nodes = new HashSet<>(way.getNodes()); // Guarantee uniqness
            invalidateWayBoundingBox(nodes);
            for (Node nd : nodes) {
                undo.save(nd);
                double nodeX = GeoMath.lonE7ToX(w, v, nd.getLon());
                double nodeY = GeoMath.latE7ToY(h, w, v, nd.getLat());
                double newX = pivotX + (nodeX - pivotX) * Math.cos(angle) - direction * (nodeY - pivotY) * Math.sin(angle);
                double newY = pivotY + direction * (nodeX - pivotX) * Math.sin(angle) + (nodeY - pivotY) * Math.cos(angle);
                int lat = GeoMath.yToLatE7(h, w, v, (float) newY);
                int lon = GeoMath.xToLonE7(w, v, (float) newX);
                updateLatLon(nd, lat, lon);
            }
            // Don't call onElementChanged(null, new ArrayList<>(nodes));
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "rotateWay got " + e.getMessage());
        }
    }

    /**
     * Delete a node
     * 
     * The operation will remove it from any ways and relations relations it is a member of, ways that contain just 1 or
     * less nodes after the deletion will be deleted too
     * 
     * @param node the node to remove
     */
    public void removeNode(@NonNull final Node node) {
        // undo - node saved here, affected ways saved in removeWayNodes
        dirty = true;
        if (node.state == OsmElement.STATE_DELETED) {
            Log.e(DEBUG_TAG, "removeNode: node already deleted " + node.getOsmId());
            return; // node was already deleted
        }
        undo.save(node);
        try {
            if (node.state == OsmElement.STATE_CREATED) {
                apiStorage.removeElement(node);
            } else {
                apiStorage.insertElementSafe(node);
            }
            removeWayNode(node);
            removeElementFromRelations(node);
            currentStorage.removeNode(node);
            node.updateState(OsmElement.STATE_DELETED);
            onElementChanged((List<OsmElement>) null, (List<OsmElement>) null);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeNode got " + e.getMessage());
        }
    }

    /**
     * Split all Ways that contain the Node
     * 
     * @param node Node to split at
     */
    public void splitAtNode(@NonNull final Node node) {
        Log.d(DEBUG_TAG, "splitAtNode for all ways");
        // undo - nothing done here, everything done in splitAtNode
        dirty = true;
        List<Way> ways = currentStorage.getWays(node);
        for (Way way : ways) {
            splitAtNode(way, node);
        }
    }

    /**
     * Split a (closed) way at two points
     * 
     * @param way way to split
     * @param node1 first node to split at
     * @param node2 second node to split at
     * @param createPolygons split in to two polygons
     * @return null if split failed or wasn't possible, the two resulting ways otherwise
     */
    public Way[] splitAtNodes(Way way, Node node1, Node node2, boolean createPolygons) {
        Log.d(DEBUG_TAG, "splitAtNodes way " + way.getOsmId() + " node1 " + node1.getOsmId() + " node2 " + node2.getOsmId());
        // undo - old way is saved here, new way is saved at insert
        dirty = true;
        undo.save(way);

        List<Node> nodes = way.getNodes();
        if (nodes.size() < 3) {
            return null;
        }

        /*
         * convention iterate over list, copy everything between first split node found and 2nd split node found if 2nd
         * split node found first the same
         */
        List<Node> nodesForNewWay = new LinkedList<>();
        List<Node> nodesForOldWay1 = new LinkedList<>();
        List<Node> nodesForOldWay2 = new LinkedList<>();
        boolean found1 = false;
        boolean found2 = false;
        for (Iterator<Node> it = way.getRemovableNodes(); it.hasNext();) {
            Node wayNode = it.next();
            if (!found1 && wayNode.getOsmId() == node1.getOsmId()) {
                found1 = true;
                nodesForNewWay.add(wayNode);
                if (!found2) {
                    nodesForOldWay1.add(wayNode);
                } else {
                    nodesForOldWay2.add(wayNode);
                }
            } else if (!found2 && wayNode.getOsmId() == node2.getOsmId()) {
                found2 = true;
                nodesForNewWay.add(wayNode);
                if (!found1) {
                    nodesForOldWay1.add(wayNode);
                } else {
                    nodesForOldWay2.add(wayNode);
                }
            } else if ((found1 && !found2) || (!found1 && found2)) {
                nodesForNewWay.add(wayNode);
            } else if (!found1 && !found2) {
                nodesForOldWay1.add(wayNode);
            } else if (found1 && found2) {
                nodesForOldWay2.add(wayNode);
            }
        }

        // shuffle the nodes around for the original way so that they are in sequence and the way isn't closed
        Log.d(DEBUG_TAG, "nodesForNewWay " + nodesForNewWay.size() + " oldNodes1 " + nodesForOldWay1.size() + " oldNodes2 " + nodesForOldWay2.size());
        List<Node> oldNodes = way.getNodes();
        oldNodes.clear();
        if (nodesForOldWay1.isEmpty()) {
            oldNodes.addAll(nodesForOldWay2);
        } else if (nodesForOldWay2.isEmpty()) {
            oldNodes.addAll(nodesForOldWay1);
        } else if (nodesForOldWay1.get(0) == nodesForOldWay2.get(nodesForOldWay2.size() - 1)) {
            oldNodes.addAll(nodesForOldWay2);
            nodesForOldWay1.remove(0);
            oldNodes.addAll(nodesForOldWay1);
        } else {
            oldNodes.addAll(nodesForOldWay1);
            nodesForOldWay2.remove(0);
            oldNodes.addAll(nodesForOldWay2);
        }
        ArrayList<OsmElement> changedElements = new ArrayList<>();
        try {
            if (createPolygons && way.length() > 2) { // close the original way now
                way.addNode(way.getFirstNode());
            }
            way.updateState(OsmElement.STATE_MODIFIED);
            apiStorage.insertElementSafe(way);
            changedElements.add(way);

            // create the new way
            Way newWay = factory.createWayWithNewId();
            newWay.addTags(way.getTags());
            newWay.addNodes(nodesForNewWay, false);
            if (createPolygons && newWay.length() > 2) { // close the new way now
                newWay.addNode(newWay.getFirstNode());
            }
            insertElementUnsafe(newWay);

            // check for relation membership
            if (way.hasParentRelations()) {
                ArrayList<Relation> relations = new ArrayList<>(way.getParentRelations()); // copy !
                dirty = true;
                /* iterate through relations, add the new way to the relation, for now simply after the old way */
                for (Relation r : relations) {
                    Log.d(DEBUG_TAG, "splitAtNode processing relation (#" + r.getOsmId() + "/" + relations.size() + ") " + r.getDescription());
                    RelationMember rm = r.getMember(way);
                    undo.save(r);
                    // no role specific code for now
                    RelationMember newMember = new RelationMember(rm.getRole(), newWay);
                    r.addMemberAfter(rm, newMember);
                    newWay.addParentRelation(r);
                    r.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(r);
                    changedElements.add(r);
                }
            }

            onElementChanged(null, changedElements);
            Way[] result = new Way[2];
            result[0] = way;
            result[1] = newWay;
            return result;
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "splitAtNodes got " + e.getMessage());
        }
        return null;
    }

    /**
     * Split way at node with relation support
     * 
     * @param way way to split
     * @param node node to split at
     * @return the new Way or null if the split wasn't successful
     */
    public Way splitAtNode(@NonNull final Way way, @NonNull final Node node) {
        Log.d(DEBUG_TAG, "splitAtNode way " + way.getOsmId() + " node " + node.getOsmId());
        // undo - old way is saved here, new way is saved at insert
        dirty = true;
        undo.save(way);

        List<Node> nodes = way.getNodes();
        int occurances = Collections.frequency(way.getNodes(), node);
        // the following condition is fairly obscure and should likely be replaced by checking for position of the node
        // in the way
        if (nodes.size() < 3 || (way.isEndNode(node) && (way.isClosed() ? occurances == 2 : occurances == 1))) {
            // protect against producing single node ways FIXME give feedback that this is not good
            Log.d(DEBUG_TAG, "splitAtNode can't split " + nodes.size() + " node long way at this node");
            return null;
        }
        // we assume this node is only contained in the way once.
        // else the user needs to split the remaining way again.
        List<Node> nodesForNewWay = new LinkedList<>();
        boolean found = false;
        boolean first = true; // node to split at can't be the first one
        for (Iterator<Node> it = way.getRemovableNodes(); it.hasNext();) {
            Node wayNode = it.next();
            if (!found && wayNode.getOsmId() == node.getOsmId() && !first) {
                found = true;
                nodesForNewWay.add(wayNode);
            } else if (found) {
                nodesForNewWay.add(wayNode);
                it.remove();
            }
            first = false;
        }
        if (nodesForNewWay.size() <= 1) {
            Log.d(DEBUG_TAG, "splitAtNode can't split, new way would have " + nodesForNewWay.size() + " node(s)");
            return null; // do not create 1-node way
        }
        ArrayList<OsmElement> changedElements = new ArrayList<>();
        try {
            way.updateState(OsmElement.STATE_MODIFIED);
            apiStorage.insertElementSafe(way);
            changedElements.add(way);

            // create the new way
            Way newWay = factory.createWayWithNewId();
            newWay.addTags(way.getTags());
            newWay.addNodes(nodesForNewWay, false);
            insertElementUnsafe(newWay);

            // check for relation membership
            if (way.getParentRelations() != null) {
                Set<Relation> relations = new HashSet<>(way.getParentRelations()); // copy and only unique relations!
                dirty = true;
                /*
                 * iterate through relations, for all except restrictions add the new way to the relation, for now
                 * simply after the old way
                 */
                for (Relation r : relations) {
                    Log.d(DEBUG_TAG, "splitAtNode processing relation (#" + r.getOsmId() + "/" + relations.size() + ") " + r.getDescription());
                    List<RelationMember> members = r.getAllMembers(way);
                    if (members.isEmpty()) {
                        Log.d(DEBUG_TAG, "Unconsistent state detected way " + way.getOsmId() + " should be relation member");
                        ACRAHelper.nocrashReport(null, "Unconsistent state detected way " + way.getOsmId() + " should be relation member");
                        continue;
                    }
                    undo.save(r);
                    for (RelationMember rm : members) {
                        Log.d(DEBUG_TAG, "splitAtNode member " + rm);
                        int memberPos = r.getPosition(rm);
                        String type = r.getTagWithKey(Tags.KEY_TYPE);

                        // attempt to handle turn restrictions correctly, if element is the via way, copying relation
                        // membership to both is ok
                        if (Tags.VALUE_RESTRICTION.equals(type) && !rm.getRole().equals(Tags.ROLE_VIA)) {
                            // check if the old way has a node in common with the via relation member, if no assume the
                            // new way has
                            List<RelationMember> rl = r.getMembersWithRole(Tags.ROLE_VIA);
                            boolean foundVia = false;
                            for (int j = 0; j < rl.size(); j++) {
                                RelationMember viaRm = rl.get(j);
                                OsmElement viaE = viaRm.getElement();
                                if (viaE instanceof Node) {
                                    if (((Way) rm.getElement()).hasNode((Node) viaE)) {
                                        foundVia = true;
                                    }
                                } else if (viaE instanceof Way) {
                                    if (((Way) rm.getElement()).hasCommonNode((Way) viaE)) {
                                        foundVia = true;
                                    }
                                }
                            }
                            Log.d(DEBUG_TAG, "splitAtNode foundVia " + foundVia);
                            if (!foundVia) {
                                // remove way from relation, add newWay to it
                                RelationMember newMember = new RelationMember(rm.getRole(), newWay);
                                r.replaceMember(rm, newMember);
                                way.removeParentRelation(r); // way is dirty and will be changes anyway
                                newWay.addParentRelation(r);
                            }
                        } else {
                            RelationMember newMember = new RelationMember(rm.getRole(), newWay);
                            RelationMember prevMember = r.getMemberAt(memberPos - 1);
                            RelationMember nextMember = r.getMemberAt(memberPos + 1);
                            /*
                             * We need to determine if to insert the new way before or after the existing member If the
                             * new way has a common node with the previous member or if the existing way has a common
                             * node with the following member we insert before, otherwise we insert after the existing
                             * member.
                             * 
                             * FIXME To do this really properly we would have to download the previous and next elements
                             */
                            if (prevMember != null && prevMember.getElement() instanceof Way && newWay.hasCommonNode((Way) prevMember.getElement())) {
                                r.addMemberBefore(rm, newMember);
                            } else if (nextMember != null && nextMember.getElement() instanceof Way && way.hasCommonNode((Way) nextMember.getElement())) {
                                r.addMemberBefore(rm, newMember);
                            } else {
                                r.addMemberAfter(rm, newMember);
                            }
                            newWay.addParentRelation(r);
                        }
                    }
                    r.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(r);
                    changedElements.add(r);
                }
            }
            onElementChanged(null, changedElements);
            return newWay;
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "splitAtNode got " + e.getMessage());
            return null;
        }
    }

    /**
     * Merge two nodes into one.
     * 
     * Updates ways and relations the node is a member of.
     * 
     * @param mergeInto The node to merge into. Tags are combined.
     * @param mergeFrom The node to merge from. Is deleted.
     * @return a MergeResult object with a reference to the resulting object and any issues
     */
    @NonNull
    public MergeResult mergeNodes(@NonNull Node mergeInto, @NonNull Node mergeFrom) {
        MergeResult result = new MergeResult();
        if (mergeInto.equals(mergeFrom)) {
            result.addIssue(Issue.SAMEOBJECT);
            result.setElement(mergeFrom);
            return result;
        }
        dirty = true;
        // first determine if one of the nodes already has a valid id, if it is not and other node has valid id swap
        // else check version numbers this helps preserve history
        if (((mergeInto.getOsmId() < 0) && (mergeFrom.getOsmId() > 0)) || mergeInto.getOsmVersion() < mergeFrom.getOsmVersion()) {
            // swap
            Log.d(DEBUG_TAG, "swap into #" + mergeInto.getOsmId() + " with from #" + mergeFrom.getOsmId());
            Node tmpNode = mergeInto;
            mergeInto = mergeFrom;
            mergeFrom = tmpNode;
            Log.d(DEBUG_TAG, "now into #" + mergeInto.getOsmId() + " from #" + mergeFrom.getOsmId());
        }
        if (roleConflict(mergeInto, mergeFrom)) { // need to do this before we remove objects from relations.
            result.addIssue(Issue.ROLECONFLICT);
        }
        // merge tags
        setTags(mergeInto, OsmElement.mergedTags(mergeInto, mergeFrom)); // this calls onElementChange for the node
        // if merging the tags creates multiple-value tags, mergeOK should be set to false
        for (String v : mergeInto.getTags().values()) {
            if (v.indexOf(';') >= 0) {
                result.addIssue(Issue.MERGEDTAGS);
                break;
            }
        }
        // replace references to mergeFrom node in ways with mergeInto
        for (Way way : currentStorage.getWays(mergeFrom)) {
            replaceNodeInWay(mergeFrom, mergeInto, way);
        }
        // belt and suspenders not really necessary
        for (Way way : apiStorage.getWays(mergeFrom)) {
            replaceNodeInWay(mergeFrom, mergeInto, way);
        }
        mergeElementsRelations(mergeInto, mergeFrom);
        // delete mergeFrom node
        removeNode(mergeFrom);
        result.setElement(mergeInto);
        return result;
    }

    /**
     * Merges two ways by prepending/appending all nodes from the second way to the first one, then deleting the second
     * one.
     * 
     * Updated for relation support if roles are not the same the merge will fail.
     * 
     * @param mergeInto Way to merge the other way into. This way will be kept if it has a valid id.
     * @param mergeFrom Way to merge into the other.
     * @return a MergeResult object with a reference to the resulting object and any issues
     * @throws OsmIllegalOperationException if the ways cannot be merged
     */
    @NonNull
    public MergeResult mergeWays(@NonNull Way mergeInto, @NonNull Way mergeFrom) throws OsmIllegalOperationException {
        MergeResult result = new MergeResult();

        validateWayNodeCount(mergeInto.nodeCount() + mergeFrom.nodeCount());
        // first determine if one of the nodes already has a valid id, if it is not and other node has valid id swap
        // else check version numbers this helps preserve history
        if (((mergeInto.getOsmId() < 0) && (mergeFrom.getOsmId() > 0)) || mergeInto.getOsmVersion() < mergeFrom.getOsmVersion()) {
            // swap
            Log.d(DEBUG_TAG, "swap into #" + mergeInto.getOsmId() + " with from #" + mergeFrom.getOsmId());
            Way tmpWay = mergeInto;
            mergeInto = mergeFrom;
            mergeFrom = tmpWay;
            Log.d(DEBUG_TAG, "now into #" + mergeInto.getOsmId() + " from #" + mergeFrom.getOsmId());
        }

        if (roleConflict(mergeInto, mergeFrom)) { // need to do this before we remove objects from relations.
            result.addIssue(Issue.ROLECONFLICT);
        }

        // undo - mergeInto way saved here, mergeFrom way will not be changed directly and will be saved in removeWay
        dirty = true;
        undo.save(mergeInto);
        removeWay(mergeFrom); // have to do this here because otherwise the way will be saved with potentially reversed
                              // tags

        List<Node> newNodes = new ArrayList<>(mergeFrom.getNodes());
        boolean atBeginning;

        if (mergeInto.getFirstNode().equals(mergeFrom.getFirstNode())) {
            // Result: f3 f2 f1 (f0=)i0 i1 i2 i3 (f0 = 0th node of mergeFrom, i1 = 1st node of mergeInto)
            atBeginning = true;
            // check for direction dependent tags
            Map<String, String> dirTags = Reverse.getDirectionDependentTags(mergeFrom);
            if (dirTags != null) {
                Reverse.reverseDirectionDependentTags(mergeFrom, dirTags, true);
            }
            if (mergeFrom.notReversable()) {
                result.addIssue(Issue.NOTREVERSABLE);
            }
            Collections.reverse(newNodes);
            newNodes.remove(newNodes.size() - 1); // remove "last" (originally first) node after reversing
            reverseWayNodeTags(newNodes);
        } else if (mergeInto.getLastNode().equals(mergeFrom.getFirstNode())) {
            // Result: i0 i1 i2 i3(=f0) f1 f2 f3
            atBeginning = false;
            newNodes.remove(0);
        } else if (mergeInto.getFirstNode().equals(mergeFrom.getLastNode())) {
            // Result: f0 f1 f2 (f3=)i0 i1 i2 i3
            atBeginning = true;
            newNodes.remove(newNodes.size() - 1);
        } else if (mergeInto.getLastNode().equals(mergeFrom.getLastNode())) {
            // Result: i0 i1 i2 i3(=f3) f2 f1 f0
            atBeginning = false;
            // check for direction dependent tags
            Map<String, String> dirTags = Reverse.getDirectionDependentTags(mergeFrom);
            if (dirTags != null) {
                Reverse.reverseDirectionDependentTags(mergeFrom, dirTags, true);
            }
            if (mergeFrom.notReversable()) {
                result.addIssue(Issue.NOTREVERSABLE);
            }
            newNodes.remove(newNodes.size() - 1); // remove last node before reversing
            reverseWayNodeTags(newNodes);
            Collections.reverse(newNodes);
        } else {
            throw new OsmIllegalOperationException("attempted to merge non-mergeable nodes. this is a bug.");
        }

        // merge tags (after any reversal has been done)
        setTags(mergeInto, OsmElement.mergedTags(mergeInto, mergeFrom));
        // if merging the tags creates multiple-value tags, mergeOK should be set to false
        for (String v : mergeInto.getTags().values()) {
            if (v.indexOf(';') >= 0) {
                result.addIssue(Issue.MERGEDTAGS);
                break;
            }
        }

        mergeInto.addNodes(newNodes, atBeginning);
        mergeInto.updateState(OsmElement.STATE_MODIFIED);
        insertElementSafe(mergeInto);
        mergeElementsRelations(mergeInto, mergeFrom);

        result.setElement(mergeInto);
        return result;
    }

    /**
     * Reverse any direction dependent tags on the way nodes
     * 
     * @param nodes List of nodes
     * @return true if tags on a node had to be reversed
     */
    private boolean reverseWayNodeTags(List<Node> nodes) {
        boolean hadToReverse = false;
        for (Node n : nodes) {
            Map<String, String> nodeDirTags = Reverse.getDirectionDependentTags(n);
            if (nodeDirTags != null) {
                undo.save(n);
                hadToReverse = true;
                Reverse.reverseDirectionDependentTags(n, nodeDirTags, true);
                n.updateState(OsmElement.STATE_MODIFIED);
                try {
                    apiStorage.insertElementSafe(n);
                } catch (StorageException e) {
                    // TODO handle OOM
                    Log.e(DEBUG_TAG, "reverseWayNodeTags got " + e.getMessage());
                }
            }
        }
        return hadToReverse;
    }

    /**
     * Check if two elements have different roles in the same relation
     * 
     * @param o1 the first OsmElement
     * @param o2 the second OsmElement
     * @return true if elements have different roles in the same relation
     */
    private boolean roleConflict(OsmElement o1, OsmElement o2) {
        List<Relation> r1 = o1.getParentRelations() != null ? o1.getParentRelations() : new ArrayList<>();
        List<Relation> r2 = o2.getParentRelations() != null ? o2.getParentRelations() : new ArrayList<>();
        for (Relation r : r1) {
            if (r2.contains(r)) {
                RelationMember rm1 = r.getMember(o1);
                RelationMember rm2 = r.getMember(o2);
                if (rm1 != null && rm2 != null) { // if either of these are null something is broken
                    String role1 = rm1.getRole();
                    String role2 = rm2.getRole();
                    // noinspection StringEquality
                    if ((role1 != null && role2 == null) || (role1 == null && role2 != null) || (role1 != role2 && !role1.equals(role2))) { // NOSONAR
                        Log.d(DEBUG_TAG, "role conflict between " + o1.getDescription() + " role " + role1 + " and " + o2.getDescription() + " role " + role2);
                        return true;
                    }
                } else {
                    Log.e(DEBUG_TAG, "inconsistent relation membership in " + r.getOsmId() + " for " + o1.getOsmId() + " and " + o2.getOsmId());
                    ACRAHelper.nocrashReport(null, "inconsistent relation membership in " + r.getOsmId() + " for " + o1.getOsmId() + " and " + o2.getOsmId());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Unjoins ways connected at the given node. Updated for relation support
     * 
     * @param node The node connecting ways that are to be unjoined.
     */
    public void unjoinWays(@NonNull final Node node) {
        List<Way> ways = currentStorage.getWays(node);
        try {
            if (ways.size() > 1) {
                ArrayList<OsmElement> changedElements = new ArrayList<>();
                boolean first = true;
                for (Way way : ways) {
                    if (first) {
                        // first way doesn't need to be changed
                        first = false;
                    } else {
                        // subsequent ways
                        dirty = true;
                        // create a new node that duplicates the given node
                        Node newNode = factory.createNodeWithNewId(node.lat, node.lon);
                        newNode.addTags(node.getTags());
                        insertElementUnsafe(newNode);
                        // replace the given node in the way with the new node
                        undo.save(way);
                        List<Node> nodes = way.getNodes();
                        nodes.set(nodes.indexOf(node), newNode);
                        way.updateState(OsmElement.STATE_MODIFIED);
                        apiStorage.insertElementSafe(way);
                        changedElements.add(way);

                        // check if node is in a relation, if yes, add to new node
                        // should probably check for restrictions
                        if (node.hasParentRelations()) {
                            List<Relation> relations = node.getParentRelations();
                            /*
                             * iterate through relations, for all except restrictions add the new node to the relation,
                             * for now simply after the old node
                             */
                            for (Relation r : relations) {
                                RelationMember rm = r.getMember(node);
                                undo.save(r);
                                String type = r.getTagWithKey(Tags.KEY_TYPE);
                                if (type != null) {
                                    if (type.equals(Tags.VALUE_RESTRICTION)) {
                                        // doing nothing for now at least gives a chance of being right :-)
                                    } else {
                                        RelationMember newMember = new RelationMember(rm.getRole(), newNode);
                                        r.addMemberAfter(rm, newMember);
                                        newNode.addParentRelation(r);
                                    }

                                } else {
                                    RelationMember newMember = new RelationMember(rm.getRole(), newNode);
                                    r.addMemberAfter(rm, newMember);
                                    newNode.addParentRelation(r);
                                }
                                r.updateState(OsmElement.STATE_MODIFIED);
                                apiStorage.insertElementSafe(r);
                                changedElements.add(r);
                            }
                        }
                    }
                }
                onElementChanged(null, changedElements);
            }
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "unjoinWays got " + e.getMessage());
        }
    }

    /**
     * Replace the given node in any ways it is member of.
     * 
     * @param node The node to be replaced.
     * @return null if node was not member of a way, the replacement node if it was
     */
    public Node replaceNode(@NonNull final Node node) {
        List<Way> ways = currentStorage.getWays(node);
        if (!ways.isEmpty()) {
            Node newNode = factory.createNodeWithNewId(node.lat, node.lon);
            insertElementUnsafe(newNode);
            dirty = true;
            for (Way way : ways) {
                replaceNodeInWay(node, newNode, way);
            }
            return newNode;
        }
        return null;
    }

    /**
     * Reverses a way (reverses the order of its nodes)
     * 
     * @param way to reverse
     * @return true is way had tags that needed to be reversed
     */
    public boolean reverseWay(@NonNull final Way way) {
        dirty = true;
        undo.save(way);
        // check for direction dependent tags
        Map<String, String> dirTags = Reverse.getDirectionDependentTags(way);
        // TODO inform user about the tags
        if (dirTags != null) {
            Reverse.reverseDirectionDependentTags(way, dirTags, false); // assume we only wants to change the oneway
                                                                        // direction for now
        }
        boolean dirNodeTags = reverseWayNodeTags(way.getNodes());
        way.reverse();
        List<Relation> dirRelations = Reverse.getRelationsWithDirectionDependentRoles(way);
        if (dirRelations != null) {
            Reverse.reverseRoleDirection(way, dirRelations);
            for (Relation r : dirRelations) {
                r.updateState(OsmElement.STATE_MODIFIED);
                try {
                    apiStorage.insertElementSafe(r);
                } catch (StorageException e) {
                    // TODO handle OOM
                    Log.e(DEBUG_TAG, "reverseWay got " + e.getMessage());
                }
            }
        }
        way.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(way);
            onElementChanged(null, way);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "reverseWay got " + e.getMessage());
        }
        return (dirTags != null || dirRelations != null || dirNodeTags);
    }

    /**
     * Replace an existing way Node with a new one
     * 
     * @param existingNode the existing Node
     * @param newNode the new Node
     * @param way the Way to exchange the Node in
     */
    private void replaceNodeInWay(final Node existingNode, final Node newNode, final Way way) {
        dirty = true;
        undo.save(way);
        way.replaceNode(existingNode, newNode);
        way.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(way);
            onElementChanged(null, way);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "replaceNodeInWay got " + e.getMessage());
        }
    }

    /**
     * Remove a node from all ways in storage, deleting ways with just one node in the process
     * 
     * If the first/last node of a closed way is deleted the way is re-closed
     * 
     * @param node Node to delete
     * @return count of how many ways node was deleted from
     */
    private int removeWayNode(@NonNull final Node node) {
        // undo - node is not changed, affected way(s) are stored below
        dirty = true;
        int deleted = 0;
        try {
            List<Way> ways = currentStorage.getWays(node);
            ArrayList<OsmElement> changedElements = new ArrayList<>();
            for (Way way : ways) {
                undo.save(way);
                if (way.isClosed() && way.isEndNode(node) && way.getNodes().size() > 1) { // note protection against
                                                                                          // degenerate closed ways
                    way.removeNode(node);
                    if (way.getNodes().size() > 1 && !way.isClosed()) {
                        way.addNode(way.getFirstNode()); // re-close the way, except if it is already closed, which
                                                         // means it is degenerate
                    } else {
                        Log.e(DEBUG_TAG, "Way " + way.getOsmId() + " way already closed!");
                    }
                } else {
                    way.removeNode(node);
                }
                // remove way when less than two waynodes exist
                // or only the same node twice
                // NOTE this will not remove ways with three and more times the same node
                int size = way.getNodes().size();
                if (size < 2 || (way.isClosed() && size == 2)) {
                    removeWay(way);
                } else {
                    way.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(way);
                    changedElements.add(way);
                }
                deleted++;
            }
            onElementChanged(null, changedElements);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeWayNode got " + e.getMessage());
        }
        return deleted;
    }

    /**
     * Deletes a way
     * 
     * Removes it from any relations it is a member of
     * 
     * @param way way to delete
     */
    public void removeWay(final Way way) {
        dirty = true;
        undo.save(way);
        try {
            currentStorage.removeWay(way);
            if (apiStorage.contains(way)) {
                if (way.getState() == OsmElement.STATE_CREATED) {
                    apiStorage.removeElement(way);
                }
            } else {
                apiStorage.insertElementSafe(way);
            }
            removeElementFromRelations(way);
            way.updateState(OsmElement.STATE_DELETED);
            onElementChanged((List<OsmElement>) null, (List<OsmElement>) null);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeWay got " + e.getMessage());
        }
    }

    /**
     * Delete a relation
     * 
     * Note this will only remove backlinks from elements in storage
     * 
     * @param relation relation to remove
     */
    public void removeRelation(@NonNull final Relation relation) {
        // undo - relation saved here, affected ways saved in removeRelationFromMembers
        dirty = true;
        undo.save(relation);
        try {
            if (relation.state == OsmElement.STATE_CREATED) {
                apiStorage.removeElement(relation);
            } else {
                apiStorage.insertElementSafe(relation);
            }
            removeElementFromRelations(relation);
            removeRelationFromMembers(relation);
            currentStorage.removeRelation(relation);
            relation.updateState(OsmElement.STATE_DELETED);
            onElementChanged((List<OsmElement>) null, (List<OsmElement>) null);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeRelation got " + e.getMessage());
        }
    }

    /**
     * Remove backlinks in elements
     * 
     * @param relation to remove from members
     */
    private void removeRelationFromMembers(@NonNull final Relation relation) {
        for (RelationMember rm : relation.getMembers()) {
            OsmElement e = rm.getElement();
            if (e != null) { // if null the element wasn't downloaded
                undo.save(e);
                e.removeParentRelation(relation);
                onParentRelationChanged(e);
            }
        }
    }

    /**
     * Remove downloaded element from any relations it is a member of
     * 
     * Note the element does not need to have its state changed or be stored in the API storage since the parent
     * relation back link is just internal.
     * 
     * @param element to remove from any relations it is a member of
     */
    private void removeElementFromRelations(@NonNull final OsmElement element) {
        try {
            if (element.hasParentRelations()) {
                ArrayList<Relation> relations = new ArrayList<>(element.getParentRelations()); // need copy!
                ArrayList<OsmElement> changedElements = new ArrayList<>();
                for (Relation r : relations) {
                    Log.i(DEBUG_TAG, "removing " + element.getName() + " #" + element.getOsmId() + " from relation #" + r.getOsmId());
                    dirty = true;
                    undo.save(r);
                    r.removeMember(r.getMember(element));
                    r.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(r);
                    changedElements.add(r);
                    undo.save(element);
                    element.removeParentRelation(r);
                    Log.i(DEBUG_TAG, "... done");
                }
                onElementChanged(null, changedElements);
                onParentRelationChanged(element);
            }
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeElementFromRelations got " + e.getMessage());
        }
    }

    /**
     * Remove downloaded element from a relation
     * 
     * Note the element does not need to have its state changed of be stored in the API storage since the parent
     * relation back link is just internal.
     * 
     * @param element element to remove
     * @param r relation to remove the element from
     */
    private void removeElementFromRelation(@NonNull final OsmElement element, @NonNull final Relation r) {
        Log.i(DEBUG_TAG, "removing " + element.getName() + " #" + element.getOsmId() + " from relation #" + r.getOsmId());
        dirty = true;
        undo.save(r);
        try {
            r.removeMember(r.getMember(element));
            r.updateState(OsmElement.STATE_MODIFIED);
            apiStorage.insertElementSafe(r);
            undo.save(element);
            element.removeParentRelation(r);
            Log.i(DEBUG_TAG, "... done");
            onElementChanged(null, r);
            onParentRelationChanged(element);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeElementFromRelation got " + e.getMessage());
        }
    }

    /**
     * Remove non-downloaded element from relation
     * 
     * @param type type (node, way, relation) of element
     * @param elementId id of the element
     * @param r relation to remove the element from
     */
    public void removeElementFromRelation(@NonNull String type, final Long elementId, @NonNull final Relation r) {
        Log.i(DEBUG_TAG, "removing  #" + elementId + " from relation #" + r.getOsmId());
        dirty = true;
        undo.save(r);
        r.removeMember(r.getMember(type, elementId));
        r.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(r);
            onElementChanged(null, r);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeElementFromRelation got " + e.getMessage());
        }
        //
        Log.i(DEBUG_TAG, "... done");
    }

    /**
     * Add element to relation at a specific position
     * 
     * @param e OsmElement to add
     * @param pos position to insert the element
     * @param role role of the element
     * @param rel relation to add the element to
     */
    private void addElementToRelation(@NonNull final OsmElement e, final int pos, final String role, @NonNull final Relation rel) {
        dirty = true;
        undo.save(rel);
        undo.save(e);

        RelationMember newMember = new RelationMember(role, e);
        rel.addMember(pos, newMember);
        e.addParentRelation(rel);

        rel.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(rel);
            onElementChanged(null, rel);
            onParentRelationChanged(e);
        } catch (StorageException sex) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "addElementToRelation got " + sex.getMessage());
        }
    }

    /**
     * Add a new member to relation at end
     * 
     * @param e the OsmElement
     * @param role the role of the element
     * @param rel target relation
     */
    public void addMemberToRelation(@NonNull final OsmElement e, final String role, @NonNull final Relation rel) {

        dirty = true;
        undo.save(rel);
        undo.save(e);

        RelationMember newMember = new RelationMember(role, e);
        rel.addMember(newMember);
        e.addParentRelation(rel);

        rel.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(rel);
            onElementChanged(null, rel);
            onParentRelationChanged(e);
        } catch (StorageException sex) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "addMemberToRelation got " + sex.getMessage());
        }
    }

    /**
     * Add new member to relation at end
     * 
     * @param newMember member to add
     * @param rel target relation
     */
    public void addMemberToRelation(@NonNull final RelationMember newMember, @NonNull final Relation rel) {
        OsmElement e = newMember.getElement();
        if (e == null) {
            Log.e(DEBUG_TAG, "addElementToRelation element not found");
            return;
        }

        dirty = true;
        undo.save(rel);

        undo.save(e);

        rel.addMember(newMember);
        e.addParentRelation(rel);

        rel.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(rel);
            onElementChanged(null, rel);
            onParentRelationChanged(e);
        } catch (StorageException sex) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "addMemberToRelation got " + sex.getMessage());
        }
    }

    /**
     * Stuff to do if an OsmELement Relation membership has changed
     * 
     * @param e the OsmElement
     */
    private void onParentRelationChanged(@NonNull OsmElement e) {
        e.resetHasProblem();
        if (e instanceof StyleableFeature) {
            ((StyleableFeature) e).setStyle(null);
        }
    }

    /**
     * Set role for a relation member to new value
     * 
     * Note: does not deal with the same element being a element multiple times
     * 
     * @param e member OemELement
     * @param role the new role to set
     * @param rel the relation in which e is a member
     */
    private void setRole(final OsmElement e, final String role, final Relation rel) {
        dirty = true;
        undo.save(rel);

        RelationMember oldRm = rel.getMember(e);
        RelationMember rm = new RelationMember(oldRm); // necessary or else we will overwrite the role string in undo
                                                       // storage

        rm.setRole(role);
        rel.replaceMember(oldRm, rm);

        rel.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(rel);
            onElementChanged(null, rel);
        } catch (StorageException sex) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "setRole got " + sex.getMessage());
        }
        Log.w(DEBUG_TAG, "set role for #" + e.getOsmId() + " to " + role + " in relation #" + rel.getOsmId());
    }

    /**
     * Set role for a relation member to new value
     * 
     * Note: does not deal with the same element being a element multiple times
     * 
     * @param type member OsmElment type
     * @param elementId member OsmElment id
     * @param role the new role to set
     * @param rel the relation in which e is a member
     */
    public void setRole(final String type, final long elementId, final String role, final Relation rel) {
        dirty = true;
        undo.save(rel);

        RelationMember oldRm = rel.getMember(type, elementId);
        RelationMember rm = new RelationMember(oldRm); // necessary or else we will overwrite the role string in undo
                                                       // storage

        rm.setRole(role);
        rel.replaceMember(oldRm, rm);

        rel.updateState(OsmElement.STATE_MODIFIED);
        try {
            apiStorage.insertElementSafe(rel);
            onElementChanged(null, rel);
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "setRole got " + e.getMessage());
        }
        Log.w(DEBUG_TAG, "set role for #" + elementId + " to " + role + " in relation #" + rel.getOsmId());
    }

    /**
     * compare current relations e is a member of to new state parents and make it so
     * 
     * @param e current OsmElement
     * @param parents new Map of parent Relations
     */
    public void updateParentRelations(@NonNull final OsmElement e, @NonNull final Map<Long, String> parents) {
        Log.d(DEBUG_TAG, "updateParentRelations new parents size " + parents.size());
        List<Relation> origParents = e.getParentRelations() != null ? new ArrayList<>(e.getParentRelations()) : new ArrayList<>();

        for (Relation o : origParents) { // find changes to existing memberships
            if (!parents.containsKey(o.getOsmId())) {
                removeElementFromRelation(e, o); // saves undo state
                continue;
            }
            if (parents.containsKey(o.getOsmId())) {
                String newRole = parents.get(o.getOsmId());
                if (!o.getMember(e).getRole().equals(newRole)) {
                    setRole(e, newRole, o);
                }
            }
        }
        // add as new member to relation
        for (Entry<Long, String> entry : parents.entrySet()) {
            long l = entry.getKey();
            Log.d(DEBUG_TAG, "updateParentRelations new parent " + l);
            if (l != -1) { //
                Relation r = (Relation) currentStorage.getOsmElement(Relation.NAME, l);
                if (!origParents.contains(r)) {
                    Log.d(DEBUG_TAG, "updateParentRelations adding " + e.getDescription() + " to " + r.getDescription());
                    addElementToRelation(e, -1, entry.getValue(), r); // append for now only
                }
            }
        }
    }

    /**
     * Compare current list of relations members to new list and apply the necessary changes
     * 
     * @param r the relation
     * @param members new list of members
     */
    public void updateRelation(@NonNull Relation r, @NonNull List<RelationMemberDescription> members) {
        dirty = true;
        undo.save(r);
        boolean changed = false;
        List<RelationMember> origMembers = new ArrayList<>(r.getMembers());
        LinkedHashMap<String, RelationMember> membersHash = new LinkedHashMap<>();
        for (RelationMember rm : r.getMembers()) {
            membersHash.put(rm.getType() + "-" + rm.getRef(), rm);
        }
        ArrayList<RelationMember> newMembers = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            RelationMemberDescription rmd = members.get(i);
            String key = rmd.getType() + "-" + rmd.getRef();
            OsmElement e = rmd.getElement();
            RelationMember rm = membersHash.get(key);
            if (rm != null) {
                int origPos = origMembers.indexOf(rm);
                String newRole = rmd.getRole();
                if (!rm.getRole().equals(newRole)) {
                    changed = true;
                    rm = new RelationMember(rm); // allocate new element
                    rm.setRole(newRole);
                }
                newMembers.add(rm); // existing member simply add to list
                if (origPos != i) {
                    changed = true;
                }
                membersHash.remove(key);
            } else { // new member
                changed = true;
                RelationMember newMember = null;
                if (e != null) { // downloaded
                    newMember = new RelationMember(rmd.getRole(), e);
                } else {
                    newMember = new RelationMember(rmd.getType(), rmd.getRef(), rmd.getRole());
                }
                newMembers.add(newMember);
            }
        }
        for (RelationMember rm : membersHash.values()) {
            changed = true;
            OsmElement e = rm.getElement();
            if (e != null) {
                undo.save(e);
                e.removeParentRelation(r);
            }
        }

        if (changed) {
            r.replaceMembers(newMembers);
            r.updateState(OsmElement.STATE_MODIFIED);
            try {
                apiStorage.insertElementSafe(r);
                onElementChanged(null, r);
            } catch (StorageException e) {
                // TODO Handle OOM
                Log.e(DEBUG_TAG, "updateRelation got " + e.getMessage());
            }
        } else {
            undo.remove(r); // nothing changed
        }
    }

    /**
     * Add further members without role to an existing relation
     * 
     * @param relation existing relation
     * @param members list of new members
     */
    public void addMembersToRelation(@NonNull Relation relation, @NonNull List<OsmElement> members) {
        dirty = true;
        undo.save(relation);
        for (OsmElement e : members) {
            undo.save(e);
            RelationMember rm = new RelationMember("", e);
            relation.addMember(rm);
            e.addParentRelation(relation);
            onParentRelationChanged(e);
        }
        relation.updateState(OsmElement.STATE_MODIFIED);
        insertElementSafe(relation);
    }

    /**
     * Assumes mergeFrom will deleted by caller and doesn't update back refs
     * 
     * @param mergeInto OsmElement to merge the parent Relations into
     * @param mergeFrom OsmElement with potentially new parent Relations
     */
    private void mergeElementsRelations(@NonNull final OsmElement mergeInto, @NonNull final OsmElement mergeFrom) {
        // copy just to be safe, use Set to ensure uniqueness
        Set<Relation> fromRelations = mergeFrom.getParentRelations() != null ? new HashSet<>(mergeFrom.getParentRelations()) : new HashSet<>();
        List<Relation> toRelations = mergeInto.getParentRelations() != null ? mergeInto.getParentRelations() : new ArrayList<>();
        try {
            Set<OsmElement> changedElements = new HashSet<>();
            for (Relation r : fromRelations) {
                if (!toRelations.contains(r)) {
                    dirty = true;
                    undo.save(r);
                    List<RelationMember> members = r.getAllMembers(mergeFrom);
                    for (RelationMember rm : members) {
                        // create new member with same role
                        RelationMember newRm = new RelationMember(rm.getRole(), mergeInto);
                        // insert at same place
                        r.replaceMember(rm, newRm);
                        mergeInto.addParentRelation(r);
                    }
                    r.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(r);
                    changedElements.add(r);
                    mergeInto.updateState(OsmElement.STATE_MODIFIED);
                    apiStorage.insertElementSafe(mergeInto);
                    changedElements.add(mergeInto);
                }
            }
            onElementChanged(null, new ArrayList<>(changedElements));
        } catch (StorageException sex) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "mergeElementsRelations got " + sex.getMessage());
        }
    }

    /**
     * Make a copy of the element and store it in the clipboard
     * 
     * @param e element to copy
     * @param lat latitude where it was located
     * @param lon longitude where it was located
     */
    public void copyToClipboard(@Nullable OsmElement e, int lat, int lon) {
        dirty = true; // otherwise clipboard will not get saved without other changes
        if (e instanceof Node) {
            Node newNode = factory.createNodeWithNewId(((Node) e).getLat(), ((Node) e).getLon());
            newNode.setTags(e.getTags());
            clipboard.copyTo(newNode, lat, lon);
        } else if (e instanceof Way) {
            Way newWay = factory.createWayWithNewId();
            newWay.setTags(e.getTags());
            for (Node nd : ((Way) e).getNodes()) {
                Node newNode = factory.createNodeWithNewId(nd.getLat(), nd.getLon());
                newNode.setTags(nd.getTags());
                newWay.addNode(nd);
            }
            clipboard.copyTo(newWay, lat, lon);
        }
    }

    /**
     * Cut original element to clipboard, does -not- preserve relation memberships
     * 
     * @param e element to copy
     * @param lat latitude where it was located
     * @param lon longitude where it was located
     */
    public void cutToClipboard(@Nullable OsmElement e, int lat, int lon) {
        dirty = true; // otherwise clipboard will not get saved without other changes
        if (e instanceof Node) {
            clipboard.cutTo(e, lat, lon);
            removeNode((Node) e);
        } else if (e instanceof Way) {
            // clone all nodes that are members of other ways
            ArrayList<Node> nodes = new ArrayList<>(((Way) e).getNodes());
            for (Node nd : nodes) {
                if (currentStorage.getWays(nd).size() > 1) { // 1 is expected (our way will be deleted later)
                    Log.d(DEBUG_TAG, "Duplicating node");
                    Node newNode = factory.createNodeWithNewId(nd.getLat(), nd.getLon());
                    newNode.setTags(nd.getTags());
                    ((Way) e).replaceNode(nd, newNode);
                }
            }
            clipboard.cutTo(e, lat, lon);
            removeWay((Way) e);
            nodes = new ArrayList<>(((Way) e).getNodes());
            for (Node nd : nodes) {
                removeNode(nd); //
            }
        }
    }

    /**
     * Paste the contents of the clipboard to coordinates If the content was copied to the clipboard a new element will
     * be created.
     * 
     * @param lat latitude in WGS84*1E7 degrees
     * @param lon longitude in WGS84*1E7 degrees
     * @return the contents or null is the clipboard was empty
     */
    @Nullable
    public OsmElement pasteFromClipboard(int lat, int lon) {
        OsmElement e = clipboard.pasteFrom();
        // if the clipboard isn't empty now we need to clone the element
        if (!clipboard.isEmpty()) { // paste from copy
            if (e instanceof Node) {
                Node newNode = factory.createNodeWithNewId(lat, lon);
                newNode.setTags(e.getTags());
                insertElementSafe(newNode);
                e = newNode;
            } else if (e instanceof Way) {
                Way newWay = factory.createWayWithNewId();
                newWay.setTags(e.getTags());
                int deltaLat = lat - clipboard.getSelectionLat();
                int deltaLon = lon - clipboard.getSelectionLon();

                List<Node> nodeList = ((Way) e).getNodes();
                // this is slightly complicated because we need to handle cases with potentially broken geometry
                // allocate and set the position of the new nodes
                Set<Node> nodes = new HashSet<>(nodeList);
                HashMap<Node, Node> newNodes = new HashMap<>();
                for (Node nd : nodes) {
                    Node newNode = factory.createNodeWithNewId(nd.getLat() + deltaLat, nd.getLon() + deltaLon);
                    newNode.setTags(nd.getTags());
                    newNode.setLat(nd.getLat() + deltaLat);
                    newNode.setLon(nd.getLon() + deltaLon);
                    newNode.updateState(OsmElement.STATE_MODIFIED);
                    insertElementSafe(newNode);
                    newNodes.put(nd, newNode);
                }
                // now add them to the new way
                for (Node nd : nodeList) {
                    newWay.addNode(newNodes.get(nd));
                }
                insertElementSafe(newWay);
                e = newWay;
            }
        } else { // paste from cut
            if (e instanceof Node) {
                ((Node) e).setLat(lat);
                ((Node) e).setLon(lon);
            } else if (e instanceof Way) {
                int deltaLat = lat - clipboard.getSelectionLat();
                int deltaLon = lon - clipboard.getSelectionLon();
                Set<Node> nodes = new HashSet<>(((Way) e).getNodes());
                for (Node nd : nodes) {
                    nd.setLat(nd.getLat() + deltaLat);
                    nd.setLon(nd.getLon() + deltaLon);
                    nd.updateState(OsmElement.STATE_MODIFIED);
                    insertElementSafe(nd);
                }
                ((Way) e).invalidateBoundingBox();
            }
            e.updateState(OsmElement.STATE_MODIFIED);
            insertElementSafe(e);
        }
        return e;
    }

    /**
     * Check if there is something in the clipboard
     * 
     * @return true if the clipboard is empty
     */
    public boolean clipboardIsEmpty() {
        return clipboard.isEmpty();
    }

    /**
     * Clear the clipboard and set the dirty flag
     */
    public void clearClipboard() {
        clipboard.reset();
        dirty();
    }

    /**
     * Get the current API Storage object
     * 
     * @return the Storage for changes that should be uploaded
     */
    @NonNull
    public Storage getApiStorage() {
        return apiStorage;
    }

    /**
     * Get the current Storage object
     * 
     * @return the current Storage object
     */
    @NonNull
    public Storage getCurrentStorage() {
        return currentStorage;
    }

    /**
     * Return the list of BoundingBoxes from the current Storage object
     * 
     * @return the current List of BoundingBoxes
     */
    @NonNull
    public List<BoundingBox> getBoundingBoxes() {
        // TODO make a copy?
        return currentStorage.getBoundingBoxes();
    }

    /**
     * Set the initial BoundingBox, this will truncate the list
     * 
     * @param box the initial BoundingBox
     */
    public void setOriginalBox(@NonNull final BoundingBox box) {
        dirty = true;
        currentStorage.setBoundingBox(box);
    }

    /**
     * Add a Boundingbox to the List of BoundingBoxes in Storage
     * 
     * @param box the BoundingBox to add
     */
    public void addBoundingBox(@NonNull BoundingBox box) {
        dirty = true;
        currentStorage.addBoundingBox(box);
    }

    /**
     * Delete a BoundingBox from the List of BoundingBoxes in Storage
     * 
     * @param box the BoundingBox to delete
     */
    public void deleteBoundingBox(@NonNull BoundingBox box) {
        dirty = true;
        currentStorage.deleteBoundingBox(box);
    }

    /**
     * Get the number of Nodes in API storage
     * 
     * @return the number of Nodes in API storage
     */
    public int getApiNodeCount() {
        return apiStorage.getNodes().size();
    }

    /**
     * Get the number of Ways in API storage
     * 
     * @return the number of Ways in API storage
     */
    public int getApiWayCount() {
        return apiStorage.getWays().size();
    }

    /**
     * Get the number of Relations in API storage
     * 
     * @return the number of Relations in API storage
     */
    public int getApiRelationCount() {
        return apiStorage.getRelations().size();
    }

    /**
     * Get the total number of elements in API storage
     * <p>
     * Returns the total number of elements to be created, modified or deleted
     * 
     * @return the element count
     */
    public int getApiElementCount() {
        return apiStorage.getRelations().size() + apiStorage.getWays().size() + apiStorage.getNodes().size();
    }

    /**
     * Retrieve an OsmElement from Storage This will check the API Storage first (because of deleted objects) and then
     * the regular version
     * 
     * @param type the type of object as a String (NODE, WAY, RELATION)
     * @param osmId the id
     * @return the object or null if not in storage
     */
    @Nullable
    public OsmElement getOsmElement(@NonNull final String type, final long osmId) {
        OsmElement elem = apiStorage.getOsmElement(type, osmId);
        if (elem == null) {
            elem = currentStorage.getOsmElement(type, osmId);
        }
        return elem;
    }

    /**
     * Check if the data in Storage has been changed
     * 
     * @return true if changes have been made
     */
    public boolean hasChanges() {
        return !apiStorage.isEmpty();
    }

    /**
     * Check is we have data in Storage
     * 
     * @return true if there is no data
     */
    public boolean isEmpty() {
        return currentStorage.isEmpty() && apiStorage.isEmpty();
    }

    /**
     * Stores the current storage data to the default storage file
     * 
     * @param ctx Android Context
     * @throws IOException if saving failed
     */
    public void writeToFile(@NonNull Context ctx) throws IOException {
        if (apiStorage == null || currentStorage == null) {
            // don't write empty state files
            Log.i(DEBUG_TAG, "storage delegator empty, skipping save");
            return;
        }
        if (!dirty) { // dirty flag should only be set if we have actually read/loaded/changed something
            Log.i(DEBUG_TAG, "storage delegator not dirty, skipping save");
            return;
        }

        if (readingLock.tryLock()) {
            // TODO this doesn't really help with error conditions need to throw exception
            if (savingHelper.save(ctx, FILENAME, this, true)) {
                dirty = false;
            } else {
                // this is essentially catastrophic and can only happen if something went really wrong
                // running out of memory or disk, or HW failure
                if (ctx instanceof Activity) {
                    try {
                        Snack.barError((Activity) ctx, R.string.toast_statesave_failed);
                    } catch (Exception ignored) {
                        Log.e(DEBUG_TAG, "Emergency toast failed with " + ignored.getMessage());
                    } catch (Error ignored) { // NOSONAR crashing is not an option
                        Log.e(DEBUG_TAG, "Emergency toast failed with " + ignored.getMessage());
                    }
                }
                SavingHelper.asyncExport(ctx, this); // ctx == null is checked in method
                Log.d(DEBUG_TAG, "save of state file failed, written emergency change file");
            }
            readingLock.unlock();
        } else {
            Log.i(DEBUG_TAG, "storage delegator state being read, skipping save");
        }
    }

    /**
     * Read save data from standard file
     * 
     * Loads the storage data from the default storage file NOTE: lock is acquired in logic before this is called
     * 
     * @param context Android context
     * @return true if the state was read successfully
     */
    public boolean readFromFile(Context context) {
        return readFromFile(context, FILENAME);
    }

    /**
     * Read save data from file
     * 
     * @param context Android context
     * @param filename the file to read
     * @return true if the state was read successfully
     */
    public boolean readFromFile(Context context, String filename) {
        try {
            lock();
            StorageDelegator newDelegator = savingHelper.load(context, filename, true);

            if (newDelegator != null) {
                Log.d(DEBUG_TAG, "read saved state");
                currentStorage = newDelegator.currentStorage;
                if (currentStorage.getBoundingBoxes().isEmpty()) { // can happen if data was added before load
                    try {
                        currentStorage.setBoundingBox(currentStorage.calcBoundingBoxFromData());
                    } catch (OsmException e) {
                        Log.e(DEBUG_TAG, "readFromFile got " + e.getMessage());
                    }
                }
                apiStorage = newDelegator.apiStorage;
                undo = newDelegator.undo;
                clipboard = newDelegator.clipboard;
                factory = newDelegator.factory;
                dirty = false; // data was just read, i.e. memory and file are in sync
                return true;
            } else {
                Log.d(DEBUG_TAG, "saved state null");
                return false;
            }
        } finally {
            unlock();
        }
    }

    /**
     * Return a localized list of strings describing the changes we would upload on {@link #uploadToServer(Server)}.
     * 
     * @param aResources the translations
     * @return the changes
     */
    public List<String> listChanges(final Resources aResources) {
        List<String> retval = new ArrayList<>();

        for (Node node : new ArrayList<>(apiStorage.getNodes())) {
            retval.add(node.getStateDescription(aResources));
        }

        for (Way way : new ArrayList<>(apiStorage.getWays())) {
            retval.add(way.getStateDescription(aResources));
        }

        for (Relation relation : new ArrayList<>(apiStorage.getRelations())) {
            retval.add(relation.getStateDescription(aResources));
        }
        return retval;
    }

    /**
     * Return a list of the elements we would upload on {@link #uploadToServer(Server)}.
     * 
     * @return the changed OsmElements
     */
    public List<OsmElement> listChangedElements() {
        List<OsmElement> retval = new ArrayList<>();
        retval.addAll(new ArrayList<>(apiStorage.getNodes()));
        retval.addAll(new ArrayList<>(apiStorage.getWays()));
        retval.addAll(new ArrayList<>(apiStorage.getRelations()));
        return retval;
    }

    /**
     * Remove any elements in API storage that haven't been changed
     * 
     * This shouldn't be necessary and indicates that there is something which doesn't correctly remove elements.
     */
    private void removeUnchanged() {
        for (Node node : new ArrayList<>(apiStorage.getNodes())) {
            if (node.getState() == OsmElement.STATE_UNCHANGED) {
                apiStorage.removeNode(node);
                Log.e(DEBUG_TAG, "Node " + node.getOsmId() + " was unchanged in API");
            }
        }

        for (Way way : new ArrayList<>(apiStorage.getWays())) {
            if (way.getState() == OsmElement.STATE_UNCHANGED) {
                apiStorage.removeWay(way);
                Log.e(DEBUG_TAG, "Way " + way.getOsmId() + " was unchanged in API");
            }
        }

        for (Relation relation : new ArrayList<>(apiStorage.getRelations())) {
            if (relation.getState() == OsmElement.STATE_UNCHANGED) {
                apiStorage.removeRelation(relation);
                Log.e(DEBUG_TAG, "Relation " + relation.getOsmId() + " was unchanged in API");
            }
        }
    }

    /**
     * Upload created, modified and deleted data in diff format
     * 
     * @param server Server to upload changes to.
     * @param comment Changeset comment tag
     * @param source Changeset source tag
     * @param closeChangeset if true close the Changeset
     * @param extraTags Additional tags to add
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws OsmServerException
     * @throws IOException
     */
    public synchronized void uploadToServer(final Server server, final String comment, String source, boolean closeChangeset,
            @Nullable Map<String, String> extraTags) throws MalformedURLException, ProtocolException, OsmServerException, IOException {

        dirty = true; // storages will get modified as data is uploaded, these changes need to be saved to file
        removeUnchanged();
        // upload methods set dirty flag too, in case the file is saved during an upload
        boolean split = getApiElementCount() > server.getCapabilities().getMaxElementsInChangeset();
        int part = 1;
        int elementCount = getApiElementCount();
        while (elementCount > 0) {
            String tmpSource = source;
            if (split) {
                tmpSource = source + " [" + part + "]";
            }
            server.openChangeset(comment, tmpSource, Util.listToOsmList(imagery), extraTags);
            try {
                lock();
                server.diffUpload(this);
            } finally {
                unlock();
            }

            if (closeChangeset || split) { // always close when splitting
                server.closeChangeset();
            }
            part++;
            int currentElementCount = getApiElementCount();
            if (currentElementCount < elementCount) {
                elementCount = currentElementCount;
            } else {
                // element count didn't do anything, that should cause an exception to be
                // thrown in diffUpload, but it is conceivable that that doesn't happen
                Log.e(DEBUG_TAG, "Upload had no effect, API element count " + elementCount);
                throw new ProtocolException("Upload had no effect");
            }
        }
        // yes, again, just to be sure
        dirty = true;

        // reset imagery recording for next upload
        imagery = new ArrayList<>();
        setImageryRecorded(false);
    }

    /**
     * Exports changes as a OsmChange file.
     */
    @Override
    public void export(OutputStream outputStream) throws Exception {
        OsmXml.writeOsmChange(getApiStorage(), outputStream, null, Integer.MAX_VALUE, App.getUserAgent());
    }

    @Override
    public String exportExtension() {
        return "osc";
    }

    /**
     * Merge additional data with existing, copy to a new storage because this may fail
     * 
     * @param storage storage containing data to merge
     * @param postMerge handler to run after merging
     * @return true if the merge was successful
     */
    public synchronized boolean mergeData(@NonNull Storage storage, @Nullable PostMergeHandler postMerge) {
        Log.d(DEBUG_TAG, "mergeData called");
        // make temp copy of current storage (we may have to abort
        Storage temp = new Storage(currentStorage);

        // retrieve the maps
        LongOsmElementMap<Node> nodeIndex = temp.getNodeIndex();
        LongOsmElementMap<Way> wayIndex = temp.getWayIndex();
        LongOsmElementMap<Relation> relationIndex = temp.getRelationIndex();

        Log.d(DEBUG_TAG, "mergeData finished init");

        try {
            // add nodes
            for (Node n : storage.getNodes()) {
                Node apiNode = apiStorage.getNode(n.getOsmId()); // can contain deleted elements
                if (!nodeIndex.containsKey(n.getOsmId()) && apiNode == null) { // new node no problem
                    temp.insertNodeUnsafe(n);
                    if (postMerge != null) {
                        postMerge.handler(n);
                    }
                } else {
                    if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
                        if (apiNode.getOsmVersion() >= n.getOsmVersion()) {
                            continue; // can use node we already have
                        } else {
                            return false; // can't resolve conflicts, upload first
                        }
                    }
                    Node existingNode = nodeIndex.get(n.getOsmId());
                    if (existingNode.getOsmVersion() >= n.getOsmVersion()) { // larger just to be on the safe side
                        continue; // can use node we already have
                    } else {
                        if (existingNode.isUnchanged()) {
                            temp.insertNodeUnsafe(n);
                            if (postMerge != null) {
                                postMerge.handler(n);
                            }
                        } else {
                            return false; // can't resolve conflicts, upload first
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "mergeData added nodes");

            // add ways
            for (Way w : storage.getWays()) {
                Way apiWay = apiStorage.getWay(w.getOsmId()); // can contain deleted elements
                if (!wayIndex.containsKey(w.getOsmId()) && apiWay == null) { // new way no problem
                    temp.insertWayUnsafe(w);
                    if (postMerge != null) {
                        postMerge.handler(w);
                    }
                } else {
                    if (apiWay != null && apiWay.getState() == OsmElement.STATE_DELETED) {
                        if (apiWay.getOsmVersion() >= w.getOsmVersion()) {
                            continue; // can use way we already have
                        } else {
                            return false; // can't resolve conflicts, upload first
                        }
                    }
                    Way existingWay = wayIndex.get(w.getOsmId());
                    if (existingWay != null) {
                        if (existingWay.getOsmVersion() >= w.getOsmVersion()) {// larger just to be on the safe side
                            continue; // can use way we already have
                        } else {
                            if (existingWay.isUnchanged()) {
                                temp.insertWayUnsafe(w);
                                if (postMerge != null) {
                                    postMerge.handler(w);
                                }
                            } else {
                                return false; // can't resolve conflicts, upload first
                            }
                        }
                    } else {
                        // this shouldn't be able to happen
                        String debugString = "mergeData null existing way " + w.getOsmId() + " containsKey is " + wayIndex.containsKey(w.getOsmId())
                                + " apiWay is " + apiWay;
                        Log.e(DEBUG_TAG, debugString);
                        ACRAHelper.nocrashReport(null, debugString);
                        return false;
                    }
                }
            }

            Log.d(DEBUG_TAG, "mergeData added ways");

            // fix up way nodes
            // all nodes should be in storage now, however new ways will have references to copies not in storage
            for (Way w : wayIndex) {
                List<Node> nodes = w.getNodes();
                for (int i = 0; i < nodes.size(); i++) {
                    Node wayNode = nodes.get(i);
                    long wayNodeId = wayNode.getOsmId();
                    Node n = nodeIndex.get(wayNodeId);
                    if (n != null) {
                        nodes.set(i, n);
                    } else {
                        // node might have been deleted, aka somebody deleted nodes outside of the down loaded data
                        // bounding box
                        // that belonged to a not downloaded way
                        Node apiNode = apiStorage.getNode(wayNodeId);
                        if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
                            // attempt to fix this up, reinstate the original node so that any existing references
                            // remain
                            // FIXME undoing the original delete will likely cause havoc
                            Log.e(DEBUG_TAG, "mergeData null undeleting node " + wayNodeId);
                            if (apiNode.getOsmVersion() == wayNode.getOsmVersion() && (apiNode.isTagged() && apiNode.getTags().equals(wayNode.getTags()))
                                    && apiNode.getLat() == wayNode.getLat() && apiNode.getLon() == wayNode.getLon()) {
                                apiNode.setState(OsmElement.STATE_UNCHANGED);
                                apiStorage.removeNode(apiNode);
                            } else {
                                apiNode.setState(OsmElement.STATE_MODIFIED);
                            }
                            temp.insertNodeUnsafe(apiNode);
                            nodes.set(i, apiNode);
                        } else {
                            String debugString = "mergeData null way node for way " + w.getOsmId() + " v" + w.getOsmVersion() + " node " + wayNodeId
                                    + (apiNode != null ? " state in api " + apiNode.getState() : "");
                            Log.e(DEBUG_TAG, debugString);
                            ACRAHelper.nocrashReport(null, debugString);
                            return false;
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "mergeData fixup way nodes nodes");

            // add relations
            for (Relation r : storage.getRelations()) {
                Relation apiRelation = apiStorage.getRelation(r.getOsmId()); // can contain deleted elements
                if (!relationIndex.containsKey(r.getOsmId()) && apiRelation == null) { // new relation no problem
                    temp.insertRelationUnsafe(r);
                    if (postMerge != null) {
                        postMerge.handler(r);
                    }
                } else {
                    if (apiRelation != null && apiRelation.getState() == OsmElement.STATE_DELETED) {
                        if (apiRelation.getOsmVersion() >= r.getOsmVersion()) {
                            continue; // can use relation we already have
                        } else {
                            return false; // can't resolve conflicts, upload first
                        }
                    }
                    Relation existingRelation = relationIndex.get(r.getOsmId());
                    if (existingRelation.getOsmVersion() >= r.getOsmVersion()) { // larger just to be on the safe side
                        continue; // can use relation we already have
                    } else {
                        if (existingRelation.isUnchanged()) {
                            temp.insertRelationUnsafe(r);
                            if (postMerge != null) {
                                postMerge.handler(r);
                            }
                        } else {
                            return false; // can't resolve conflicts, upload first
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "mergeData added relations");

            // fixup relation back links and memberships

            // zap all existing backlinks for our "old" relations
            for (Relation r : currentStorage.getRelations()) {
                for (RelationMember rm : r.getMembers()) {
                    if (rm.getType().equals(Node.NAME)) {
                        Node n = nodeIndex.get(rm.getRef());
                        if (n != null) {
                            n.clearParentRelations();
                        }
                    } else if (rm.getType().equals(Way.NAME)) {
                        Way w = wayIndex.get(rm.getRef());
                        if (w != null) {
                            w.clearParentRelations();
                        }
                    } else if (rm.getType().equals(Relation.NAME)) {
                        Relation r2 = relationIndex.get(rm.getRef());
                        if (r2 != null) {
                            r2.clearParentRelations();
                        }
                    }
                }
            }

            // add backlinks for all "new" relations
            for (Relation r : temp.getRelations()) {
                for (RelationMember rm : r.getMembers()) {
                    if (rm.getType().equals(Node.NAME)) {
                        Node n = nodeIndex.get(rm.getRef());
                        if (n != null) { // if node is downloaded always re-set it
                            rm.setElement(n);
                            n.addParentRelation(r);
                        } else { // check if deleted
                            memberIsDeleted(r, rm);
                        }
                    } else if (rm.getType().equals(Way.NAME)) { // same logic as for nodes
                        Way w = wayIndex.get(rm.getRef());
                        if (w != null) {
                            rm.setElement(w);
                            w.addParentRelation(r);
                        } else if (memberIsDeleted(r, rm)) {
                            return false;
                        }
                    } else if (rm.getType().equals(Relation.NAME)) { // same logic as for nodes
                        Relation r2 = relationIndex.get(rm.getRef());
                        if (r2 != null) {
                            rm.setElement(r2);
                            r2.addParentRelation(r);
                        } else if (memberIsDeleted(r, rm)) {
                            return false;
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "mergeData fixuped relations");

        } catch (StorageException sex) {
            // ran of memory
            return false;
        }

        currentStorage = temp;
        undo.setCurrentStorage(temp);
        return true; // Success
    }

    /**
     * Ensure that we have consistent backlinks
     */
    void fixupBacklinks() {
        // first zap all
        for (Relation r : currentStorage.getRelations()) {
            for (RelationMember rm : r.getMembers()) {
                if (rm.getType().equals(Node.NAME)) {
                    Node n = currentStorage.getNode(rm.getRef());
                    if (n != null) {
                        n.clearParentRelations();
                    }
                } else if (rm.getType().equals(Way.NAME)) {
                    Way w = currentStorage.getWay(rm.getRef());
                    if (w != null) {
                        w.clearParentRelations();
                    }
                } else if (rm.getType().equals(Relation.NAME)) {
                    Relation r2 = currentStorage.getRelation(rm.getRef());
                    if (r2 != null) {
                        r2.clearParentRelations();
                    }
                }
            }
        }
        // then add them back
        for (Relation r : currentStorage.getRelations()) {
            for (RelationMember rm : r.getMembers()) {
                if (rm.getType().equals(Node.NAME)) {
                    Node n = currentStorage.getNode(rm.getRef());
                    if (n != null) {
                        n.addParentRelation(r);
                    }
                } else if (rm.getType().equals(Way.NAME)) {
                    Way w = currentStorage.getWay(rm.getRef());
                    if (w != null) {
                        w.addParentRelation(r);
                    }
                } else if (rm.getType().equals(Relation.NAME)) {
                    Relation r2 = currentStorage.getRelation(rm.getRef());
                    if (r2 != null) {
                        r2.addParentRelation(r);
                    }
                }
            }
        }
    }

    /**
     * Merge additional data with existing, copy to a new storage because this may fail
     * 
     * If this is aborted the contents of the undo checkpoint need to be removed
     * 
     * @param osc storage containing data to merge
     * @param postMerge handler to run after merging
     * @return true if the operation was successful
     */
    public synchronized boolean applyOsc(@NonNull Storage osc, @Nullable PostMergeHandler postMerge) {
        Log.d(DEBUG_TAG, "applyOsc called");
        final String ABORTMESSAGE = "applyOsc aborting %s is unchanged/created";

        // make temp copy of current storage (we may have to abort
        Storage tempCurrent = new Storage(currentStorage);
        Storage tempApi = new Storage(apiStorage);

        // retrieve the maps
        LongOsmElementMap<Node> nodeIndex = tempCurrent.getNodeIndex();
        LongOsmElementMap<Way> wayIndex = tempCurrent.getWayIndex();
        LongOsmElementMap<Relation> relationIndex = tempCurrent.getRelationIndex();

        Log.d(DEBUG_TAG, "applyOsc finished init");

        try {
            // add nodes
            for (Node n : osc.getNodes()) {
                byte state = n.getState();
                if (n.getOsmId() < 0) {
                    // place holder, need to get a valid placeholder and renumber
                    Node tempNode = getFactory().createNodeWithNewId(-1, -1);
                    n.setOsmId(tempNode.getOsmId());
                }
                Node apiNode = tempApi.getNode(n.getOsmId()); // can contain deleted elements
                if (!nodeIndex.containsKey(n.getOsmId()) && apiNode == null) { // new node no problem
                    tempCurrent.insertNodeUnsafe(n);
                    tempApi.insertNodeUnsafe(n);
                    undo.save(n, false, false);
                    if (postMerge != null) {
                        postMerge.handler(n);
                    }
                } else {
                    if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
                        if (apiNode.getOsmVersion() > n.getOsmVersion()) {
                            continue; // can use node we already have
                        } else if (state == OsmElement.STATE_DELETED || state == OsmElement.STATE_MODIFIED) {
                            undo.save(apiNode);
                            apiNode.updateFrom(n);
                            if (state == OsmElement.STATE_MODIFIED) {
                                tempCurrent.insertElementSafe(apiNode);
                            }
                            continue;
                        } else {
                            Log.d(DEBUG_TAG, String.format(ABORTMESSAGE, n.getDescription()));
                            return false;
                        }
                    }
                    Node existingNode = nodeIndex.get(n.getOsmId());
                    if (existingNode != null) {
                        if (existingNode.getOsmVersion() <= n.getOsmVersion()) {
                            // so that we can abort cleanly, we actually need to replace the current element
                            undo.save(existingNode, true, true);
                            existingNode.updateFrom(n);
                            tempApi.insertNodeUnsafe(existingNode);
                            if (postMerge != null) {
                                postMerge.handler(n);
                            }
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "applyOsc done nodes");

            // add ways
            for (Way w : osc.getWays()) {
                byte state = w.getState();
                if (w.getOsmId() < 0) {
                    // place holder, need to get a valid placeholder and renumber
                    Way tempWay = getFactory().createWayWithNewId();
                    w.setOsmId(tempWay.getOsmId());
                }
                Way apiWay = tempApi.getWay(w.getOsmId()); // can contain deleted elements
                if (!wayIndex.containsKey(w.getOsmId()) && apiWay == null) { // new way no problem
                    tempCurrent.insertWayUnsafe(w);
                    tempApi.insertWayUnsafe(w);
                    undo.save(w, false, false);
                    if (postMerge != null) {
                        postMerge.handler(w);
                    }
                } else {
                    if (apiWay != null && apiWay.getState() == OsmElement.STATE_DELETED) {
                        if (apiWay.getOsmVersion() > w.getOsmVersion()) {
                            continue; // can use node we already have
                        } else if (state == OsmElement.STATE_DELETED || state == OsmElement.STATE_MODIFIED) {
                            undo.save(apiWay);
                            apiWay.updateFrom(w);
                            if (state == OsmElement.STATE_MODIFIED) {
                                tempCurrent.insertElementSafe(apiWay);
                            }
                            continue;
                        } else {
                            Log.d(DEBUG_TAG, String.format(ABORTMESSAGE, w.getDescription()));
                            return false;
                        }
                    }
                    Way existingWay = wayIndex.get(w.getOsmId());
                    if (existingWay != null) {
                        if (existingWay.getOsmVersion() <= w.getOsmVersion()) {
                            undo.save(existingWay, true, true);
                            existingWay.updateFrom(w);
                            tempApi.insertWayUnsafe(existingWay);
                            if (postMerge != null) {
                                postMerge.handler(w);
                            }
                        }
                    } else {
                        // this shouldn't be able to happen
                        String debugString = "applyOsc null existing way " + w.getOsmId() + " containsKey is " + wayIndex.containsKey(w.getOsmId())
                                + " apiWay is " + apiWay;
                        Log.e(DEBUG_TAG, debugString);
                        ACRAHelper.nocrashReport(null, debugString);
                        return false;
                    }
                }
            }

            Log.d(DEBUG_TAG, "applyOsc done ways");

            // fix up way nodes
            // all nodes should be in storage now, however new ways will have references to copies not in storage
            // this conveniently deals with references that are not in the osmChange file but should be in storage
            for (Way w : wayIndex) {
                List<Node> nodes = w.getNodes();
                for (int i = 0; i < nodes.size(); i++) {
                    Node wayNode = nodes.get(i);
                    Node n = nodeIndex.get(wayNode.getOsmId());
                    if (n != null) {
                        nodes.set(i, n);
                    } else {
                        Log.d(DEBUG_TAG, "applyOsc aborting missing node " + wayNode.getOsmId());
                        return false; // way nodes have to exist, potentially download them here
                    }
                }
            }

            Log.d(DEBUG_TAG, "applyOsc done fixup way nodes nodes");

            // add relations
            for (Relation r : osc.getRelations()) {
                byte state = r.getState();
                if (r.getOsmId() < 0) {
                    // place holder, need to get a valid placeholder and renumber
                    Relation tempRelation = getFactory().createRelationWithNewId();
                    r.setOsmId(tempRelation.getOsmId());
                }
                Relation apiRelation = tempApi.getRelation(r.getOsmId()); // can contain deleted elements
                if (!relationIndex.containsKey(r.getOsmId()) && apiRelation == null) { // new relation no problem
                    tempCurrent.insertRelationUnsafe(r);
                    tempApi.insertRelationUnsafe(r);
                    undo.save(r, false, false);
                    if (postMerge != null) {
                        postMerge.handler(r);
                    }
                } else {
                    if (apiRelation != null && apiRelation.getState() == OsmElement.STATE_DELETED) {
                        if (apiRelation.getOsmVersion() > r.getOsmVersion()) {
                            continue; // can use relation we already have
                        } else if (state == OsmElement.STATE_DELETED || state == OsmElement.STATE_MODIFIED) {
                            undo.save(apiRelation);
                            apiRelation.updateFrom(r);
                            if (state == OsmElement.STATE_MODIFIED) {
                                tempCurrent.insertElementSafe(apiRelation);
                            }
                            continue;
                        } else {
                            Log.d(DEBUG_TAG, String.format(ABORTMESSAGE, r.getDescription()));
                            return false;
                        }
                    }
                    Relation existingRelation = relationIndex.get(r.getOsmId());
                    if (existingRelation != null) {
                        if (existingRelation.getOsmVersion() <= r.getOsmVersion()) {
                            undo.save(existingRelation, true, true);
                            existingRelation.updateFrom(r);
                            tempApi.insertRelationUnsafe(existingRelation);
                            if (postMerge != null) {
                                postMerge.handler(r);
                            }
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "applyOsc done relations");

            // fixup relation back links and memberships

            // zap all existing backlinks for our "old" relations
            for (Relation r : currentStorage.getRelations()) {
                for (RelationMember rm : r.getMembers()) {
                    if (rm.getType().equals(Node.NAME)) {
                        Node n = nodeIndex.get(rm.getRef());
                        if (n != null) {
                            n.clearParentRelations();
                        }
                    } else if (rm.getType().equals(Way.NAME)) {
                        Way w = wayIndex.get(rm.getRef());
                        if (w != null) {
                            w.clearParentRelations();
                        }
                    } else if (rm.getType().equals(Relation.NAME)) {
                        Relation r2 = relationIndex.get(rm.getRef());
                        if (r2 != null) {
                            r2.clearParentRelations();
                        }
                    }
                }
            }

            // add backlinks for all "new" relations
            for (Relation r : tempCurrent.getRelations()) {
                for (RelationMember rm : r.getMembers()) {
                    if (rm.getType().equals(Node.NAME)) {
                        Node n = nodeIndex.get(rm.getRef());
                        if (n != null) { // if node is downloaded always re-set it
                            rm.setElement(n);
                            n.addParentRelation(r);
                        } else if (memberIsDeleted(r, rm)) {
                            return false;
                        }
                    } else if (rm.getType().equals(Way.NAME)) { // same logic as for nodes
                        Way w = wayIndex.get(rm.getRef());
                        if (w != null) {
                            rm.setElement(w);
                            w.addParentRelation(r);
                        } else if (memberIsDeleted(r, rm)) {
                            return false;
                        }
                    } else if (rm.getType().equals(Relation.NAME)) { // same logic as for nodes
                        Relation r2 = relationIndex.get(rm.getRef());
                        if (r2 != null) {
                            rm.setElement(r2);
                            r2.addParentRelation(r);
                        } else if (memberIsDeleted(r, rm)) {
                            return false;
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "applyOsc fixuped relations");

        } catch (StorageException sex) {
            Log.d(DEBUG_TAG, "applyOsc aborting " + sex.getMessage());
            return false;
        }

        currentStorage = tempCurrent;
        undo.setCurrentStorage(tempCurrent);
        apiStorage = tempApi;
        undo.setApiStorage(tempApi);
        return true; // Success
    }

    /**
     * Check if a referenced relation member is deleted
     * 
     * @param r the Relation
     * @param rm the RelationMember
     * @return true if deleted
     */
    private boolean memberIsDeleted(Relation r, RelationMember rm) {
        OsmElement apiElement = apiStorage.getOsmElement(rm.getType(), rm.getRef());
        if (apiElement != null && apiElement.getState() == OsmElement.STATE_DELETED) {
            String debugString = "mergeData/applyOsc deleted " + rm.getType() + " in downloaded relation " + r.getOsmId();
            Log.e(DEBUG_TAG, debugString);
            ACRAHelper.nocrashReport(null, debugString);
            fixupBacklinks(); // nexessary as we've removed the original ones from the elements
            return true; // can't resolve conflicts, upload first
        }
        return false;
    }

    /**
     * Set an OemElement to unchanged and remove it from the upload This is only used when trying to fix conflicts
     * 
     * @param element the OsmElement
     */
    public void removeFromUpload(OsmElement element) {
        apiStorage.removeElement(element);
        element.setState(OsmElement.STATE_UNCHANGED);
    }

    /**
     * Set the version of an OsmElement This is only used when trying to fix conflicts
     * 
     * @param element the OsmElement
     * @param version the new version
     */
    public void setOsmVersion(OsmElement element, long version) {
        element.setOsmVersion(version);
        element.setState(OsmElement.STATE_MODIFIED);
        insertElementSafe(element);
    }

    /**
     * Check if coordinates are in the original bboxes from downloads, needs a more efficient implementation
     * 
     * @param lonE7 WGS84 longitude*1E7
     * @param latE7 WGS84 latitude*1E7
     * @return true if the coordinates are in one of the bounding boxes
     */
    public boolean isInDownload(int lonE7, int latE7) {
        for (BoundingBox bb : new ArrayList<>(currentStorage.getBoundingBoxes())) { // make shallow copy
            if (bb.isIn(lonE7, latE7)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the last added BoundingBox
     * 
     * @return the last BoundingBox in the list or an emply one
     */
    public BoundingBox getLastBox() {
        int s = getBoundingBoxes().size();
        if (s > 0) {
            return currentStorage.getBoundingBoxes().get(getBoundingBoxes().size() - 1);
        }
        Log.e(DEBUG_TAG, "Bounding box list empty");
        return new BoundingBox(); // empty box
    }

    /**
     * for debugging only
     */
    public void logStorage() {
        Log.d(DEBUG_TAG, "storage dirty? " + isDirty());
        Log.d(DEBUG_TAG, "currentStorage");
        currentStorage.logStorage();
        Log.d(DEBUG_TAG, "apiStorage");
        apiStorage.logStorage();
    }

    /**
     * Set the reading lock
     */
    public void lock() {
        readingLock.lock();
    }

    /**
     * Free the reading lock
     */
    public void unlock() {
        readingLock.unlock();
    }
}
