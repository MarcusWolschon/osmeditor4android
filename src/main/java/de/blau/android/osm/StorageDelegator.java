package de.blau.android.osm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.filter.Filter;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.Coordinates;
import de.blau.android.util.DataStorage;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Geometry;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SavingHelper.Exportable;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.LongOsmElementMap;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.validation.BaseValidator;

public class StorageDelegator implements Serializable, Exportable, DataStorage {

    private static final String DEBUG_TAG = "StorageDelegator";

    private static final long serialVersionUID = 10L;

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
        clipboard = new ClipboardStorage();
    }

    /**
     * Reset this instance to empty state
     * 
     * This maintains the clipboard as the user may want to keep it over data relaoads etc
     * 
     * @param dirty if true mark the (empty) contents as dirty (this is useful because if true old state files will be
     *            overwritten)
     */
    public void reset(boolean dirty) {
        this.dirty = dirty;
        apiStorage = new Storage();
        currentStorage = new Storage();
        undo = new UndoStorage(currentStorage, apiStorage);
        factory = new OsmElementFactory();
        imagery = new ArrayList<>();
    }

    /**
     * Replace the current Storage object with a new one and api storage will be reset
     * 
     * @param currentStorage the new Storage object to set
     */
    public synchronized void setCurrentStorage(@NonNull final Storage currentStorage) {
        dirty = true;
        apiStorage = new Storage();
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
    public synchronized void clearUndo() {
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
     * Uses methods that are nops if the element already is present
     * 
     * @param elem the element to insert
     */
    public synchronized void insertElementSafe(@NonNull final OsmElement elem) {
        dirty = true;
        undo.save(elem);
        try {
            currentStorage.insertElementSafe(elem);
            apiStorage.insertElementSafe(elem);
            onElementChanged((OsmElement) null, elem);
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
    private synchronized void insertElementUnsafe(@NonNull final OsmElement elem) {
        dirty = true;
        undo.save(elem);
        try {
            currentStorage.insertElementUnsafe(elem);
            apiStorage.insertElementUnsafe(elem);
            onElementChanged((OsmElement) null, elem);
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
    public synchronized void setTags(@NonNull final OsmElement elem, @Nullable final Map<String, String> tags) {
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
    void onElementChanged(@Nullable List<OsmElement> pre, @Nullable List<OsmElement> post) {
        if (post != null) {
            boolean nodeChanged = false;
            BoundingBox changed = null;
            for (OsmElement e : post) {
                e.stamp();
                e.resetHasProblem();
                if (Way.NAME.equals(e.getName())) {
                    ((Way) e).invalidateBoundingBox();
                } else if (Node.NAME.equals(e.getName())) {
                    nodeChanged = true;
                    if (changed == null) {
                        changed = e.getBounds();
                    } else {
                        changed.union(e.getBounds());
                    }
                }
            }
            if (nodeChanged) {
                for (Way w : currentStorage.getWays(changed)) {
                    w.invalidateBoundingBox();
                    w.resetHasProblem();
                }
            }
        }
        Logic logic = App.getLogic();
        if (logic != null) { // this might be null in testing
            Filter filter = logic.getFilter();
            if (filter != null) {
                filter.onElementChanged(pre, post);
            }
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
    void onElementChanged(@Nullable OsmElement pre, @Nullable OsmElement post) {
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
            Iterator<Node> it = nodes.iterator();
            Node n = it.next();
            ViewBox box = new ViewBox(n.lon, n.lat);
            while (it.hasNext()) {
                n = it.next();
                box.union(n.lon, n.lat);
            }
            List<Way> ways = currentStorage.getWays(box);
            for (Way w : ways) {
                box.union(w.getBounds());
            }
            box.expand(BaseValidator.MAX_CONNECTION_TOLERANCE);
            ways = currentStorage.getWays(box);
            if (ways.size() == 1) { // optimize the common case
                Way w = ways.get(0);
                invalidateWay(w);
            } else {
                for (Way w : new HashSet<>(ways)) {
                    invalidateWay(w);
                }
            }
        }
    }

    /**
     * Invalidate way bounding box, and if it is a highway its problem status
     * 
     * @param w the way to operate on
     */
    private void invalidateWay(@NonNull Way w) {
        w.invalidateBoundingBox();
        if (w.hasTagKey(Tags.KEY_HIGHWAY)) {
            // we only validate way connections for highways currently
            w.resetHasProblem();
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
                // IGNORE
            } catch (Error ignored) {
                // IGNORE
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
     * Create relation with a list of OsmElements as members
     * 
     * @param members members to add without role
     * @return the new relation
     */
    @NonNull
    public Relation createAndInsertRelation(@Nullable List<OsmElement> members) {
        // undo - nothing done here, relation gets saved/marked on insert
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
     * Create relation with a list of RelationMembers as members
     * 
     * @param members RelationMembers to add
     * @return the new relation
     */
    @NonNull
    public Relation createAndInsertRelationFromMembers(@NonNull List<RelationMember> members) {
        // undo - nothing done here, relation gets saved/marked on insert
        dirty = true;
        Relation relation = factory.createRelationWithNewId();
        insertElementUnsafe(relation);
        for (RelationMember member : members) {
            if (member.downloaded()) {
                OsmElement e = member.getElement();
                undo.save(e);
                relation.addMember(member);
                e.addParentRelation(relation);
                onParentRelationChanged(e);
            } else {
                relation.addMember(member);
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
    void validateWayNodeCount(final int newCount) {
        Logic logic = App.getLogic();
        if (logic != null) {
            Preferences prefs = logic.getPrefs();
            if (prefs != null && newCount > prefs.getServer().getCachedCapabilities().getMaxWayNodes()) {
                throw new OsmIllegalOperationException(App.resources().getString(R.string.exception_too_many_nodes));
            }
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
        if (newLatE7 > GeoMath.MAX_COMPAT_LAT_E7 || newLatE7 < -GeoMath.MAX_COMPAT_LAT_E7) {
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
            Set<Node> nodes = new HashSet<>(allNodes); // Guarantee uniqueness
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
     * FIXME use w,h,v parameters instead of map for testing
     * 
     * @param map current map view
     * @param way way to circulize
     */
    public void circulizeWay(@NonNull de.blau.android.Map map, @NonNull Way way) {
        final List<Node> wayNodes = way.getNodes();
        if (wayNodes.size() < 3) {
            Log.d(DEBUG_TAG, "circulize way " + way.getOsmId() + " has no nodes or less than 3!");
            return;
        }
        dirty = true;
        try {
            Set<Node> nodes = new LinkedHashSet<>(wayNodes); // Guarantee uniqueness
            invalidateWayBoundingBox(nodes);
            int width = map.getWidth();
            int height = map.getHeight();
            ViewBox box = map.getViewBox();

            Coordinates[] coords = Coordinates.nodeListToCoordinateArray(width, height, box, new ArrayList<>(nodes));

            // save nodes for undo
            for (Node nd : nodes) {
                undo.save(nd);
            }

            Coordinates center = Geometry.centroidXY(coords, true);

            // caclulate average radius
            double r = 0.0f;
            for (Coordinates p : coords) {
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
    private List<List<Way>> groupWays(@NonNull List<Way> ways) {
        List<List<Way>> groups = new ArrayList<>();
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
                for (List<Way> list : groups) {
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
     * This function converts to and then operates on screen coordinates.
     * 
     * FIXME use w,h,v parameters instead of map for testing
     * 
     * @param map current map view
     * @param ways List of Way to square
     * @param threshold maximum difference to 90°/180° to process
     */
    public void orthogonalizeWay(@NonNull de.blau.android.Map map, @NonNull List<Way> ways, final int threshold) {
        final double lowerThreshold = Math.cos((90 - threshold) * Math.PI / 180);
        final double upperThreshold = Math.cos(threshold * Math.PI / 180);
        final double epsilon = 1e-4;

        dirty = true;
        try {
            // save nodes for undo
            // adding to a Set first removes duplication
            Set<Node> save = new HashSet<>();
            for (Way way : ways) {
                if (way.getNodes() != null) {
                    save.addAll(way.getNodes());
                }
            }
            for (Node nd : save) {
                undo.save(nd);
            }
            invalidateWayBoundingBox(save);
            List<List<Way>> groups = groupWays(ways);

            int width = map.getWidth();
            int height = map.getHeight();
            ViewBox box = map.getViewBox();

            for (List<Way> wayList : groups) {
                List<Coordinates[]> coordsArray = new ArrayList<>();

                int totalNodes = 0;
                for (Way w : wayList) {
                    coordsArray.add(Coordinates.nodeListToCoordinateArray(width, height, box, w.getNodes()));
                    totalNodes += w.getNodes().size();
                }
                int coordsArraySize = coordsArray.size();
                Coordinates a;
                Coordinates b;
                Coordinates c;
                Coordinates p;
                Coordinates q;

                double loopEpsilon = epsilon * (totalNodes / 4D); // NOTE the original algorithm didn't take the number
                                                                  // of corners in to account
                // iterate until score is low enough
                for (int iteration = 0; iteration < 1000; iteration++) {
                    // calculate score
                    double score = 0.0;
                    for (int coordIndex = 0; coordIndex < coordsArraySize; coordIndex++) {
                        Coordinates[] coords = coordsArray.get(coordIndex);
                        int length = coords.length;
                        int start = 0;
                        int end = length;
                        if (!wayList.get(coordIndex).isClosed()) {
                            start = 1;
                            end = end - 1;
                        }
                        for (int i = start; i < end; i++) {
                            a = coords[(i - 1 + length) % length];
                            b = coords[i];
                            c = coords[(i + 1) % length];
                            p = a.subtract(b);
                            q = c.subtract(b);
                            p = Coordinates.normalize(p, 1.0);
                            q = Coordinates.normalize(q, 1.0);
                            double dotp = filter((p.x * q.x + p.y * q.y), lowerThreshold, upperThreshold);
                            score = score + 2.0 * Math.min(Math.abs(dotp - 1.0), Math.min(Math.abs(dotp), Math.abs(dotp + 1.0)));
                        }
                    }
                    if (score < loopEpsilon) {
                        break;
                    }
                    // calculate position changes
                    for (int coordIndex = 0; coordIndex < coordsArraySize; coordIndex++) {
                        Coordinates[] coords = coordsArray.get(coordIndex);
                        int length = coords.length;
                        int start = 0;
                        int end = length;
                        if (!wayList.get(coordIndex).isClosed()) {
                            start = 1;
                            end = end - 1;
                        }
                        Coordinates[] motions = new Coordinates[length];
                        for (int i = start; i < end; i++) {
                            a = coords[(i - 1 + length) % length];
                            b = coords[i];
                            c = coords[(i + 1) % length];
                            p = a.subtract(b);
                            q = c.subtract(b);
                            double scale = 2 * Math.min(Math.hypot(p.x, p.y), Math.hypot(q.x, q.y));
                            p = Coordinates.normalize(p, 1.0);
                            q = Coordinates.normalize(q, 1.0);
                            double dotp = filter((p.x * q.x + p.y * q.y), lowerThreshold, upperThreshold);
                            // nasty hack to deal with almost-straight segments (angle is closer to 180 than to 90/270).
                            if (dotp < -0.707106781186547) {
                                dotp += 1.0;
                            }
                            if (2 * Math.min(Math.abs(dotp - 1.0), Math.min(Math.abs(dotp), Math.abs(dotp + 1.0))) < epsilon) {
                                dotp = 0;
                            }
                            motions[i] = Coordinates.normalize(p.add(q), 0.1 * dotp * scale);
                        }
                        // apply position changes
                        for (int i = start; i < end; i++) {
                            coords[i] = coords[i].add(motions[i]);
                        }
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

    /**
     * Compare an input value to upper and lower bounds
     * 
     * @param in input value
     * @param lower lower bound
     * @param upper upper bound
     * @return the input value or 0 if out of bounds
     */
    private double filter(double in, double lower, double upper) {
        return (lower > Math.abs(in)) || (Math.abs(in) > upper) ? in : 0.0;
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
        if (Float.isNaN(angle)) {
            Log.e(DEBUG_TAG, "rotateWay angle is NaN");
            return;
        }
        dirty = true;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        try {
            Set<Node> nodes = new HashSet<>(way.getNodes()); // Guarantee uniqueness
            invalidateWayBoundingBox(nodes);
            for (Node nd : nodes) {
                undo.save(nd);
                double nodeX = GeoMath.lonE7ToX(w, v, nd.getLon());
                double nodeY = GeoMath.latE7ToY(h, w, v, nd.getLat());
                double newX = pivotX + (nodeX - pivotX) * cos - direction * (nodeY - pivotY) * sin;
                double newY = pivotY + direction * (nodeX - pivotX) * sin + (nodeY - pivotY) * cos;
                updateLatLon(nd, GeoMath.yToLatE7(h, w, v, (float) newY), GeoMath.xToLonE7(w, v, (float) newX));
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
     * @deprecated This is only used in testing
     */
    @Deprecated
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
    @Nullable
    public Way[] splitAtNodes(@NonNull Way way, @NonNull Node node1, @NonNull Node node2, boolean createPolygons) {
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
        List<OsmElement> changedElements = new ArrayList<>();
        try {
            if (createPolygons && way.nodeCount() > Way.MINIMUM_NODES_IN_WAY) { // close the original way now
                way.addNode(way.getFirstNode());
            }
            way.updateState(OsmElement.STATE_MODIFIED);
            apiStorage.insertElementSafe(way);
            changedElements.add(way);

            // create the new way
            Way newWay = factory.createWayWithNewId();
            newWay.addTags(way.getTags());
            newWay.addNodes(nodesForNewWay, false);
            if (createPolygons && newWay.nodeCount() > Way.MINIMUM_NODES_IN_WAY) { // close the new way now
                newWay.addNode(newWay.getFirstNode());
            }
            insertElementUnsafe(newWay);

            addSplitWayToRelations(way, true, newWay, changedElements);

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
     * @return the new Way in the first Result
     */
    @NonNull
    public List<Result> splitAtNode(@NonNull final Way way, @NonNull final Node node) {
        Log.d(DEBUG_TAG, "splitAtNode way " + way.getOsmId() + " node " + node.getOsmId());
        Result result = new Result();
        // undo - old way is saved here, new way is saved at insert
        dirty = true;
        undo.save(way);

        List<Node> nodes = way.getNodes();
        int occurances = Collections.frequency(way.getNodes(), node);
        // the following condition is fairly obscure and should likely be replaced by checking for position of the node
        // in the way
        if (nodes.size() < 3 || (way.isEndNode(node) && (way.isClosed() ? occurances == 2 : occurances == 1))) {
            // protect against producing single node ways
            String msg = "splitAtNode can't split " + nodes.size() + " node long way at this node";
            Log.d(DEBUG_TAG, msg);
            throw new OsmIllegalOperationException(msg);
        }

        // check tags for problematic keys
        List<String> metricKeys = new ArrayList<>();
        for (String key : way.getTags().keySet()) {
            if (Tags.isWayMetric(key)) {
                metricKeys.add(key);
            }
        }
        // determine the length before we remove nodes
        double originalLength = 1D;
        if (!metricKeys.isEmpty()) {
            originalLength = way.length();
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
            String msg = "splitAtNode can't split, new way would have " + nodesForNewWay.size() + " node(s)";
            Log.d(DEBUG_TAG, msg);
            throw new OsmIllegalOperationException(msg);
        }
        List<OsmElement> changedElements = new ArrayList<>();
        try {
            // update original way
            way.updateState(OsmElement.STATE_MODIFIED);
            apiStorage.insertElementSafe(way);
            changedElements.add(way);

            // create the new way
            Way newWay = factory.createWayWithNewId();
            newWay.addNodes(nodesForNewWay, false);
            newWay.addTags(way.getTags());
            insertElementUnsafe(newWay);

            if (!metricKeys.isEmpty() && originalLength != 0) {
                result.addIssue(SplitIssue.SPLIT_METRIC);
                for (String key : metricKeys) {
                    distributeMetric(key, originalLength, way);
                    distributeMetric(key, originalLength, newWay);
                }
            }

            List<Result> relationResults = addSplitWayToRelations(way, false, newWay, changedElements);
            onElementChanged(null, changedElements);
            result.setElement(newWay);
            List<Result> resultList = Util.wrapInList(result);
            resultList.addAll(relationResults);
            return resultList;
        } catch (StorageException e) {
            Log.e(DEBUG_TAG, "splitAtNode got " + e.getMessage());
            throw e;
        }
    }

    /**
     * Change the value of the tag with key proportionally to the length of the way relative to originalLength
     * 
     * @param key the tag key
     * @param originalLength the original length of the way
     * @param way the way
     */
    private void distributeMetric(@NonNull String key, double originalLength, @NonNull Way way) {
        String value = way.getTagWithKey(key);
        if (value != null && !"".equals(value)) {
            try {
                int metric = Tags.KEY_DURATION.equals(key) ? Duration.parse(value) : Integer.parseInt(value);
                double newLength = way.length();
                int newMetric = (int) Math.round(metric * newLength / originalLength);
                Map<String, String> tags = new TreeMap<>(way.getTags());
                tags.put(key, Tags.KEY_DURATION.equals(key) ? Duration.toString(newMetric) : Integer.toString(newMetric));
                way.setTags(tags);
            } catch (NumberFormatException nfex) {
                // ignore issue has been set in any case
            }
        } else {
            Log.e(DEBUG_TAG, "Unable to retrieve value for " + key);
        }
    }

    /**
     * Given the existing relation membership of a way, add a split off way to the same ones
     * 
     * @param way the existing Way
     * @param wasClosed indicate if the existing way was closed
     * @param newWay the new Way
     * @param changedElements add changed Relations to this
     * @return a Set containing any issues
     */
    @NonNull
    private List<Result> addSplitWayToRelations(@NonNull final Way way, boolean wasClosed, @NonNull Way newWay, @NonNull List<OsmElement> changedElements) {
        List<Result> result = new ArrayList<>();
        // check for relation membership
        if (way.getParentRelations() != null) {
            Set<Relation> relations = new HashSet<>(way.getParentRelations()); // copy and only unique relations!
            dirty = true;
            /*
             * iterate through relations, for all except restrictions add the new way to the relation, for now simply
             * after the old way
             */
            for (Relation r : relations) {
                Log.d(DEBUG_TAG, "addSplitWayToRelations processing relation (#" + r.getOsmId() + "/" + relations.size() + ")");
                List<RelationMember> members = r.getAllMembers(way);
                if (members.isEmpty()) {
                    Log.d(DEBUG_TAG, "Unconsistent state detected way " + way.getOsmId() + " should be relation member");
                    ACRAHelper.nocrashReport(null, "Unconsistent state detected way " + way.getOsmId() + " should be relation member");
                    continue;
                }
                undo.save(r);
                String type = r.getTagWithKey(Tags.KEY_TYPE);
                // determine if the relation is potentially like a restriction, as hasFromViaTo is fairly expensive
                // avoid calling it if we are sure that it can't be restriction like
                boolean isRoute = Tags.VALUE_ROUTE.equals(type);
                boolean isRestrictionLike = Tags.VALUE_RESTRICTION.equals(type)
                        || (!Tags.VALUE_MULTIPOLYGON.equals(type) && !Tags.VALUE_BOUNDARY.equals(type) && !isRoute && hasFromViaTo(r));
                for (RelationMember rm : members) {
                    Log.d(DEBUG_TAG, "addSplitWayToRelations member " + rm);
                    int memberPos = r.getPosition(rm);
                    // attempt to handle turn restrictions correctly, if element is the via way, copying relation
                    // membership to both is ok
                    String role = rm.getRole();
                    boolean isVia = Tags.isVia(type, role);
                    if (isRestrictionLike && !isVia) {
                        // check if the old way has a node in common with the via relation member, if no assume the
                        // new way has
                        List<RelationMember> rl = Tags.getVia(type, r);
                        boolean foundVia = false;
                        for (int j = 0; j < rl.size(); j++) {
                            RelationMember viaRm = rl.get(j);
                            OsmElement viaE = viaRm.getElement();
                            if (viaE instanceof Node) {
                                if (((Way) rm.getElement()).hasNode((Node) viaE)) {
                                    foundVia = true;
                                }
                            } else if (viaE instanceof Way && ((Way) rm.getElement()).hasCommonNode((Way) viaE)) {
                                foundVia = true;
                            }
                        }
                        Log.d(DEBUG_TAG, "addSplitWayToRelations foundVia " + foundVia);
                        if (!foundVia) {
                            replaceMemberWay(r, rm, way, newWay);
                        }
                    } else if (isRestrictionLike && isVia && wasClosed) {
                        // very rough check
                        List<RelationMember> fromMembers = r.getMembersWithRole(Tags.ROLE_FROM);
                        if (fromMembers != null && fromMembers.size() == 1) {
                            OsmElement fromElement = fromMembers.get(0).getElement();
                            if (fromElement instanceof Way) {
                                if (((Way) fromElement).hasNode(newWay.getFirstNode())) { // swap
                                    replaceMemberWay(r, rm, way, newWay);
                                }
                            }
                        }
                    } else { // default handling of relations membership
                        RelationMember newMember = new RelationMember(rm.getRole(), newWay); // use the same role
                        RelationMember prevMember = r.getMemberAt(memberPos - 1);
                        RelationMember nextMember = r.getMemberAt(memberPos + 1);
                        /*
                         * We need to determine if to insert the new way before or after the existing member If the new
                         * way has a common node with the previous member or if the existing way has a common node with
                         * the following member we insert before, otherwise we insert after the existing member.
                         * 
                         * FIXME To do this really properly we would have to download the previous and next elements for
                         * routes
                         */
                        if (hasCommonNode(prevMember, newWay)) {
                            r.addMemberBefore(rm, newMember);
                        } else if (hasCommonNode(nextMember, way)) {
                            r.addMemberBefore(rm, newMember);
                        } else {
                            r.addMemberAfter(rm, newMember);
                            boolean hasPrev = prevMember != null;
                            boolean hasNext = nextMember != null;
                            if (isRoute && (hasPrev || hasNext) && (!hasPrev || !prevMember.downloaded()) && (!hasNext || !nextMember.downloaded())) {
                                Log.w(DEBUG_TAG, "Incomplete route relation " + r.getOsmId() + " modified");
                                Result relationResult = new Result();
                                relationResult.setElement(r);
                                relationResult.addIssue(SplitIssue.SPLIT_ROUTE_ORDERING);
                                result.add(relationResult);
                            }
                        }
                        newWay.addParentRelation(r);
                    }
                }
                r.updateState(OsmElement.STATE_MODIFIED);
                apiStorage.insertElementSafe(r);
                changedElements.add(r);
            }
        }
        return result;
    }

    /**
     * Check if a way relation member has a common node with a way
     * 
     * @param member the RelationMember
     * @param way the Way
     * @return true if there is a common node
     */
    private boolean hasCommonNode(@Nullable RelationMember member, @NonNull Way way) {
        return member != null && member.getElement() instanceof Way && way.hasCommonNode((Way) member.getElement());
    }

    /**
     * Check if a relation has from, via and to members, that is, is similar to a restriction relation
     * 
     * Does one sequential scan of all members
     * 
     * @param r the Relation
     * @return true if all three roles are present
     */
    private boolean hasFromViaTo(@NonNull Relation r) {
        List<RelationMember> members = r.getMembers();
        boolean hasFrom = false;
        boolean hasVia = false;
        boolean hasTo = false;
        for (RelationMember rm : members) {
            String role = rm.getRole();
            if (role != null) {
                switch (role) {
                case Tags.ROLE_FROM:
                    hasFrom = true;
                    break;
                case Tags.ROLE_INTERSECTION:
                case Tags.ROLE_VIA:
                    hasVia = true;
                    break;
                case Tags.ROLE_TO:
                    hasTo = true;
                    break;
                default: // do nothing
                }
            }
        }
        return hasFrom && hasVia && hasTo;
    }

    /**
     * Replace a member way with a different one
     * 
     * @param r the Relation
     * @param rm the original ReleationMember
     * @param way original member way
     * @param newWay new member way
     */
    private void replaceMemberWay(@NonNull Relation r, @NonNull RelationMember rm, @NonNull final Way way, @NonNull Way newWay) {
        // remove way from relation, add newWay to it
        RelationMember newMember = new RelationMember(rm.getRole(), newWay);
        r.replaceMember(rm, newMember);
        way.removeParentRelation(r); // way is dirty and will be changed anyway
        newWay.addParentRelation(r);
    }

    /**
     * Remove node from specified way
     * 
     * If the node is untagged and not a member of any other way it will be deleted. If the way is closed and the end
     * node is being removed it will try to re-close.
     * 
     * @param way the Way
     * @param node the Node
     */
    public void removeNodeFromWay(@NonNull Way way, @NonNull Node node) {
        boolean closed = way.isClosed();
        int size = way.getNodes().size();
        int occurences = way.count(node);
        int targetSize = size - occurences;
        if (targetSize < Way.MINIMUM_NODES_IN_WAY || (closed && targetSize < Way.MINIMUM_NODES_IN_CLOSED_WAY)) {
            throw new OsmIllegalOperationException("No Nodes can be removed from this Way. This is a bug.");
        }
        dirty = true;
        undo.save(way);
        if (closed && way.isEndNode(node)) {
            way.removeNode(node);
            // re-close
            way.addNode(way.getFirstNode());
        } else {
            way.removeNode(node);
        }
        way.updateState(OsmElement.STATE_MODIFIED);
        apiStorage.insertElementSafe(way);
        onElementChanged(null, way);
        if (!node.hasTags() && getCurrentStorage().getWays(node).isEmpty()) {
            removeNode(node);
        }
    }

    /**
     * Remove last node from specified way
     * 
     * If the node is untagged and not a member of any other node it will be deleted. If the result Way has less than 2
     * Nodes it will be deleted.
     * 
     * @param fromEnd if true remove last node else first
     * @param way the Way
     * @param deleteNode delete the node after removing it from the way
     */
    public void removeEndNodeFromWay(boolean fromEnd, @NonNull Way way, boolean deleteNode) {
        dirty = true;
        undo.save(way);
        List<Node> nodes = way.getNodes();
        int size = nodes.size();
        final int endNodeIndex = fromEnd ? size - 1 : 0;
        Node node = nodes.get(endNodeIndex);
        if (size <= Way.MINIMUM_NODES_IN_WAY) {
            Log.w(DEBUG_TAG, "removeWayNode removing degenerate way " + way.getOsmId());
            removeWay(way);
        } else {
            nodes.remove(endNodeIndex);
            way.updateState(OsmElement.STATE_MODIFIED);
            apiStorage.insertElementSafe(way);
        }
        onElementChanged(null, way);
        if (deleteNode) {
            removeNode(node);
        }
    }

    /**
     * Unjoin all ways connected at the given node.
     * 
     * @param node The node connecting ways that are to be unjoined.
     */
    public void unjoinWays(@NonNull final Node node) {
        List<Way> ways = currentStorage.getWays(node);
        try {
            if (ways.size() > 1) {
                boolean first = true;
                for (Way way : ways) {
                    if (first) {
                        // first way doesn't need to be changed
                        first = false;
                    } else {
                        // subsequent ways
                        replaceWayNode(node, way);
                    }
                }
            }
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "unjoinWays got " + e.getMessage());
        }
    }

    /**
     * Unjoin a way by replacing shared nodes with new ones
     * 
     * @param ctx Android Context
     * @param way the Way to unjoin
     * @param ignoreSimilar don't unjoin from ways with the same primary key if true, but replace the node in them too
     */
    public void unjoinWay(@Nullable Context ctx, @NonNull final Way way, boolean ignoreSimilar) {
        Set<Node> wayNodes = new HashSet<>(way.getNodes()); // only do every node once
        Map<Long, Boolean> keyMap = new HashMap<>();
        String primaryTag = way.getPrimaryTag(ctx);
        String primaryKey = null;
        if (primaryTag != null) {
            String[] t = primaryTag.split("=");
            if (t.length == 2) {
                primaryKey = t[0];
            }
        }
        for (Node nd : wayNodes) {
            List<Way> otherWays = getCurrentStorage().getWays(nd);
            List<Way> similarWays = new ArrayList<>();
            if (otherWays.size() > 1 && ignoreSimilar && primaryKey != null) {
                for (Way other : otherWays) {
                    if (!way.equals(other)) {
                        Long otherId = Long.valueOf(other.getOsmId());
                        Boolean isSimilar = keyMap.get(otherId);
                        if (isSimilar == null) {
                            isSimilar = other.hasTagKey(primaryKey);
                            keyMap.put(otherId, isSimilar);
                        }
                        if (isSimilar) {
                            similarWays.add(other);
                        }
                    }
                }
            }
            if (similarWays.size() < otherWays.size() - 1) { // if all are the same no need to replace
                Node newNode = replaceWayNode(nd, way);
                for (Way similar : similarWays) {
                    replaceNodeInWay(nd, newNode, similar);
                }
            }
        }
    }

    /**
     * Replace a Node in a way with a new one
     * 
     * @param node the node to replace
     * @param way the Way
     * @return the new Node
     */
    @NonNull
    private Node replaceWayNode(@NonNull final Node node, @NonNull final Way way) {
        List<OsmElement> changedElements = new ArrayList<>();
        dirty = true;
        // create a new node that duplicates the given node
        Node newNode = factory.createNodeWithNewId(node.lat, node.lon);
        newNode.addTags(node.getTags());
        insertElementUnsafe(newNode);
        changedElements.add(newNode);
        // replace the given node in the way with the new node
        undo.save(way);
        List<Node> nodes = way.getNodes();
        if (way.isClosed() && way.isEndNode(node)) {
            // replace last occurrence too
            //
            // note:
            // this needs to be called before the 1st node is replaced
            // or else the way won't be closed anymore
            nodes.set(nodes.size() - 1, newNode);
        }
        nodes.set(nodes.indexOf(node), newNode);
        way.updateState(OsmElement.STATE_MODIFIED);
        apiStorage.insertElementSafe(way);
        changedElements.add(way);

        // check if node is in a relation, if yes, add to new node
        // should probably check for restrictions
        if (node.hasParentRelations()) {
            List<Relation> relations = node.getParentRelations();
            /*
             * iterate through relations, for all except restrictions add the new node to the relation, for now simply
             * after the old node
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
        onElementChanged(null, changedElements);
        return newNode;
    }

    /**
     * Replace the given node in any ways it is member of.
     * 
     * @param node The node to be replaced.
     * @return null if node was not member of a way, the replacement node if it was
     */
    @Nullable
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
     * @return a List of Results, if not empty something had to be reversed
     */
    @NonNull
    public List<Result> reverseWay(@NonNull final Way way) {
        List<Result> result = new ArrayList<>();
        dirty = true;
        undo.save(way);
        // check for direction dependent tags
        Map<String, String> dirTags = Reverse.getDirectionDependentTags(way);
        if (dirTags != null) {
            Result wayResult = new Result();
            wayResult.setElement(way);
            wayResult.addIssue(ReverseIssue.TAGSREVERSED);
            wayResult.addTags(dirTags);
            result.add(wayResult);
            Reverse.reverseDirectionDependentTags(way, dirTags, false);
        }
        result.addAll(reverseWayNodeTags(way.getNodes()));
        way.reverse();
        List<Relation> dirRelations = Reverse.getRelationsWithDirectionDependentRoles(way);
        if (!dirRelations.isEmpty()) {
            Reverse.reverseRoleDirection(way, dirRelations);
            for (Relation r : dirRelations) {
                Result relationResult = new Result();
                relationResult.setElement(r);
                relationResult.addIssue(ReverseIssue.ROLEREVERSED);
                result.add(relationResult);
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
        return result;
    }

    /**
     * Reverse any direction dependent tags on the way nodes
     * 
     * @param nodes List of nodes
     * @return a List of results from the operation, if empty nothing had to be done
     */
    @NonNull
    List<Result> reverseWayNodeTags(List<Node> nodes) {
        List<Result> result = new ArrayList<>();
        for (Node n : nodes) {
            Map<String, String> nodeDirTags = Reverse.getDirectionDependentTags(n);
            if (nodeDirTags != null) {
                undo.save(n);
                Result nodeResult = new Result();
                nodeResult.setElement(n);
                nodeResult.addIssue(ReverseIssue.TAGSREVERSED);
                nodeResult.addTags(nodeDirTags);
                if (getCurrentStorage().getWays(n).size() > 1) {
                    nodeResult.addIssue(ReverseIssue.SHAREDNODE);
                }
                result.add(nodeResult);
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
        return result;
    }

    /**
     * Replace an existing way Node with a new one
     * 
     * @param existingNode the existing Node
     * @param newNode the new Node
     * @param way the Way to exchange the Node in
     */
    void replaceNodeInWay(@NonNull final Node existingNode, @NonNull final Node newNode, @NonNull final Way way) {
        dirty = true;
        undo.save(way);
        way.replaceNode(existingNode, newNode);
        way.updateState(OsmElement.STATE_MODIFIED);
        try {
            int size = way.nodeCount();
            if (size < Way.MINIMUM_NODES_IN_WAY || (way.isClosed() && size < Way.MINIMUM_NODES_IN_CLOSED_WAY)) {
                Log.w(DEBUG_TAG, "replaceNodeInWay removing degenerate way " + way.getOsmId());
                removeWay(way);
            } else {
                apiStorage.insertElementSafe(way);
                onElementChanged(null, way);
            }
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
                if (size < Way.MINIMUM_NODES_IN_WAY || (way.isClosed() && size < Way.MINIMUM_NODES_IN_CLOSED_WAY)) {
                    Log.w(DEBUG_TAG, "removeWayNode removing degenerate way " + way.getOsmId());
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
    public void removeWay(@NonNull final Way way) {
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
                List<Relation> relations = new ArrayList<>(element.getParentRelations()); // need copy!
                List<OsmElement> changedElements = new ArrayList<>();
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
     * Remove members from a relation
     * 
     * Note the potentially present elements do not need to have their state changed or be stored in the API storage
     * since the parent relation back link is just internal.
     * 
     * @param members members to remove
     * @param r relation to remove the element from
     */
    public void removeRelationMembersFromRelation(@NonNull Relation r, @NonNull List<RelationMember> members) {
        dirty = true;
        undo.save(r);
        try {
            for (RelationMember member : members) {
                Log.i(DEBUG_TAG, "removing " + member.getType() + " #" + member.getRef() + " from relation #" + r.getOsmId());
                r.removeMember(member);
                if (member.downloaded()) {
                    OsmElement element = member.getElement();
                    undo.save(element);
                    element.removeParentRelation(r);
                    onParentRelationChanged(element);
                }
            }
            r.updateState(OsmElement.STATE_MODIFIED);
            apiStorage.insertElementSafe(r);
            onElementChanged(null, r);
            Log.i(DEBUG_TAG, "... done");
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeMemberFromRelation got " + e.getMessage());
        }
    }

    /**
     * Remove downloaded element from a relation
     * 
     * Note the element does not need to have its state changed or be stored in the API storage since the parent
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
            onElementChanged(null, r);
            onParentRelationChanged(element);
            Log.i(DEBUG_TAG, "... done");
        } catch (StorageException e) {
            // TODO handle OOM
            Log.e(DEBUG_TAG, "removeElementFromRelation got " + e.getMessage());
        }
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
     * Stuff to do if an OsmElement Relation membership has changed
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
     * compare current relations e is a member of to new state parents and make it so
     * 
     * @param e current OsmElement
     * @param parents new Map of parent Relations
     */
    public void updateParentRelations(@NonNull final OsmElement e, @NonNull final MultiHashMap<Long, RelationMemberPosition> parents) {
        Log.d(DEBUG_TAG, "updateParentRelations new parents size " + parents.size());
        List<Relation> origParents = e.getParentRelations() != null ? new ArrayList<>(e.getParentRelations()) : new ArrayList<>();

        for (Relation o : origParents) { // find changes to existing memberships
            if (!parents.containsKey(o.getOsmId())) {
                removeElementFromRelation(e, o); // saves undo state
                continue;
            }
            if (parents.containsKey(o.getOsmId())) {
                List<RelationMemberPosition> newMembers = new ArrayList<>(parents.get(o.getOsmId()));
                List<RelationMemberPosition> members = o.getAllMembersWithPosition(e);
                List<RelationMemberPosition> leftOvers = new ArrayList<>(members);
                for (RelationMemberPosition existing : members) {
                    if (newMembers.contains(existing)) {
                        newMembers.remove(existing);
                        leftOvers.remove(existing);
                    }
                }
                // leftOver contains any remaining existing members
                // newMembers members that we didn't find
                if (!newMembers.isEmpty() || !leftOvers.isEmpty()) {
                    dirty = true;
                    undo.save(o);
                    for (RelationMemberPosition newMember : newMembers) {
                        if (!leftOvers.isEmpty()) {
                            RelationMemberPosition member = leftOvers.get(0);
                            member.setRole(newMember.getRole());
                            leftOvers.remove(member);
                        } else {
                            addElementToRelation(e, -1, newMember.getRole(), o);
                        }
                    }
                    for (RelationMemberPosition rmp : leftOvers) { // these are no longer needed
                        o.removeMember(rmp.getRelationMember());
                    }
                }
            }
        }
        // add as new member to relation
        for (Long l : parents.getKeys()) {
            Log.d(DEBUG_TAG, "updateParentRelations new parent " + l);
            if (l != -1) { //
                Relation r = currentStorage.getRelation(l);
                if (!origParents.contains(r)) {
                    for (RelationMemberPosition rmp : parents.get(l)) {
                        Log.d(DEBUG_TAG, "updateParentRelations adding " + e.getDescription() + " to " + r.getDescription());
                        addElementToRelation(e, -1, rmp.getRole(), r); // append for now only
                    }
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
                OsmElement e = rmd.getElement();
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
     * Add further RelationMembers to an existing relation
     * 
     * @param relation existing relation
     * @param members list of new RelationMembers
     */
    public void addRelationMembersToRelation(@NonNull Relation relation, @NonNull List<RelationMember> members) {
        dirty = true;
        undo.save(relation);
        for (RelationMember member : members) {
            if (member.downloaded()) {
                OsmElement e = member.getElement();
                undo.save(e);
                relation.addMember(member);
                e.addParentRelation(relation);
                onParentRelationChanged(e);
            } else {
                relation.addMember(member);
            }
        }
        relation.updateState(OsmElement.STATE_MODIFIED);
        insertElementSafe(relation);
    }

    /**
     * Make a copy of the element and store it in the clipboard
     * 
     * @param elements elements to copy
     * @param lat latitude where it was located
     * @param lon longitude where it was located
     */
    public void copyToClipboard(@NonNull List<OsmElement> elements, int lat, int lon) {
        dirty = true; // otherwise clipboard will not get saved without other changes
        List<OsmElement> toCopy = new ArrayList<>();
        Map<Long, Node> processedNodes = new HashMap<>();
        for (OsmElement e : elements) {
            if (e instanceof Node) {
                Node newNode = factory.createNodeWithNewId(((Node) e).getLat(), ((Node) e).getLon());
                newNode.setTags(e.getTags());
                toCopy.add(newNode);
                processedNodes.put(e.getOsmId(), newNode);
            } else if (e instanceof Way) {
                Way newWay = factory.createWayWithNewId();
                newWay.setTags(e.getTags());
                for (Node nd : ((Way) e).getNodes()) {
                    Node newNode = processedNodes.get(nd.getOsmId());
                    if (newNode == null) {
                        newNode = factory.createNodeWithNewId(nd.getLat(), nd.getLon());
                        newNode.setTags(nd.getTags());
                        processedNodes.put(nd.getOsmId(), newNode);
                    }
                    newWay.addNode(newNode);
                }
                toCopy.add(newWay);
            }
        }
        if (!toCopy.isEmpty()) {
            clipboard.copyTo(toCopy, lat, lon);
        }
    }

    /**
     * Cut original element to clipboard, does -not- preserve relation memberships
     * 
     * @param elements elements to copy
     * @param lat latitude where it was located
     * @param lon longitude where it was located
     */
    public void cutToClipboard(@NonNull List<OsmElement> elements, int lat, int lon) {
        dirty = true; // otherwise clipboard will not get saved without other changes
        List<OsmElement> toCut = new ArrayList<>();
        Map<Long, Node> replacedNodes = new HashMap<>();
        for (OsmElement e : elements) {
            toCut.add(e);
            if (e instanceof Way) {
                undo.save(e);
                // clone all nodes that are members of other ways that are not being cut
                List<Node> nodes = new ArrayList<>(((Way) e).getNodes());
                for (Node nd : nodes) {
                    List<Way> ways = currentStorage.getWays(nd);
                    if (ways.size() > 1) { // 1 is expected (our way will be deleted later)
                        Node newNode = replacedNodes.get(nd.getOsmId());
                        if (newNode == null) {
                            // check if there is actually a Way we are not cutting
                            for (Way w : ways) {
                                if (!elements.contains(w)) {
                                    newNode = factory.createNodeWithNewId(nd.getLat(), nd.getLon());
                                    newNode.setTags(nd.getTags());
                                    insertElementSafe(newNode);
                                    replacedNodes.put(nd.getOsmId(), newNode);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (OsmElement removeElement : toCut) {
            if (removeElement instanceof Node) {
                removeNode((Node) removeElement);
            } else if (removeElement instanceof Way) {
                // we replace nodes here since we are iterating over the ways anyway
                // and we have to collect all replacements first above
                for (Node nd : ((Way) removeElement).getNodes()) {
                    Node replacement = replacedNodes.get(nd.getOsmId());
                    if (replacement != null) {
                        ((Way) removeElement).replaceNode(nd, replacement);
                    }
                }
                removeWay((Way) removeElement);
            }
        }
        // way nodes have to wait till we have removed all the ways
        for (OsmElement removeElement : toCut) {
            if (removeElement instanceof Way) {
                Set<Node> nodes = new HashSet<>(((Way) removeElement).getNodes());
                for (Node nd : nodes) {
                    removeNode(nd); //
                }
            }
        }
        clipboard.cutTo(toCut, lat, lon);
    }

    /**
     * Paste the contents of the clipboard to coordinates
     * 
     * If the content was copied to the clipboard new elements will be created.
     * 
     * @param lat latitude in WGS84*1E7 degrees
     * @param lon longitude in WGS84*1E7 degrees
     * @return the contents or null is the clipboard was empty
     */
    @Nullable
    public List<OsmElement> pasteFromClipboard(int lat, int lon) {
        List<OsmElement> elements = clipboard.pasteFrom();
        if (elements.isEmpty()) {
            return null;
        }
        Collections.sort(elements, new NwrComparator()); // enforce NWR order
        List<OsmElement> result = new ArrayList<>();
        boolean copy = !clipboard.isEmpty();
        int deltaLat = lat - clipboard.getSelectionLat();
        int deltaLon = lon - clipboard.getSelectionLon();
        Map<Node, Node> newNodes = new HashMap<>(); // every node needs to only be transformed once
        for (OsmElement e : elements) {
            // if the clipboard isn't empty now we need to clone the element
            if (copy) { // paste from copy
                if (e instanceof Node) {
                    Node newNode = factory.createNodeWithNewId(((Node) e).getLat() + deltaLat, ((Node) e).getLon() + deltaLon);
                    newNode.setTags(e.getTags());
                    insertElementSafe(newNode);
                    newNodes.put((Node) e, newNode);
                    e = newNode;
                } else if (e instanceof Way) {
                    Way newWay = factory.createWayWithNewId();
                    undo.save(newWay); // do this before we create and add nodes
                    newWay.setTags(e.getTags());
                    List<Node> nodeList = ((Way) e).getNodes();
                    // this is slightly complicated because we need to handle cases with potentially broken geometry
                    // allocate and set the position of the new nodes
                    Set<Node> nodes = new HashSet<>(nodeList);
                    for (Node nd : nodes) {
                        if (!newNodes.containsKey(nd)) {
                            Node newNode = factory.createNodeWithNewId(nd.getLat() + deltaLat, nd.getLon() + deltaLon);
                            newNode.setTags(nd.getTags());
                            insertElementSafe(newNode);
                            newNodes.put(nd, newNode);
                        }
                    }
                    // now add them to the new way
                    for (Node nd : nodeList) {
                        newWay.addNode(newNodes.get(nd));
                    }
                    insertElementSafe(newWay);
                    e = newWay;
                }
            } else { // paste from cut
                if (currentStorage.contains(e)) {
                    Log.e(DEBUG_TAG, "Attempt to paste from cut, but element is already present");
                    clipboard.reset();
                    return null;
                }
                undo.save(e);
                if (e instanceof Node) {
                    ((Node) e).setLat(((Node) e).getLat() + deltaLat);
                    ((Node) e).setLon(((Node) e).getLon() + deltaLon);
                    newNodes.put((Node) e, null);
                } else if (e instanceof Way) {
                    Set<Node> nodes = new HashSet<>(((Way) e).getNodes());
                    for (Node nd : nodes) {
                        if (!newNodes.containsKey(nd)) {
                            undo.save(nd);
                            nd.setLat(nd.getLat() + deltaLat);
                            nd.setLon(nd.getLon() + deltaLon);
                            nd.updateState(nd.getOsmId() < 0 ? OsmElement.STATE_CREATED : OsmElement.STATE_MODIFIED);
                            insertElementSafe(nd);
                            newNodes.put(nd, null);
                        }
                    }
                    ((Way) e).invalidateBoundingBox();
                }
                insertElementSafe(e);
                e.updateState(e.getOsmId() < 0 ? OsmElement.STATE_CREATED : OsmElement.STATE_MODIFIED);
            }
            result.add(e);
        }
        return result;
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
     * Check if the content of the clipboard was cut
     * 
     * @return true if the clipboards content was cut
     */
    public boolean clipboardContentWasCut() {
        return clipboard.contentsWasCut();
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
    public synchronized Storage getCurrentStorage() {
        return currentStorage;
    }

    @Override
    @NonNull
    public synchronized List<BoundingBox> getBoundingBoxes() {
        return currentStorage.getBoundingBoxes();
    }

    /**
     * Set the initial BoundingBox, this will truncate the list
     * 
     * @param box the initial BoundingBox
     */
    public synchronized void setOriginalBox(@NonNull final BoundingBox box) {
        dirty = true;
        currentStorage.setBoundingBox(box);
    }

    @Override
    public synchronized void addBoundingBox(@NonNull BoundingBox box) {
        dirty = true;
        currentStorage.addBoundingBox(box);
    }

    /**
     * Delete a BoundingBox from the List of BoundingBoxes in Storage
     * 
     * @param box the BoundingBox to delete
     */
    public synchronized void deleteBoundingBox(@NonNull BoundingBox box) {
        dirty = true;
        currentStorage.deleteBoundingBox(box);
    }

    /**
     * Merge a BoundingBox for a downloaded area in to the list
     * 
     * BoundingBoxes that the new box contains will be removed, if the new box on the other hand is contained in an
     * existing box it will not be added
     * 
     * @param box the additional BoundingBox
     */
    public synchronized void mergeBoundingBox(@NonNull BoundingBox box) {
        // if we are simply expanding the area no need keep the old bounding boxes
        dirty = true;
        List<BoundingBox> bbs = new ArrayList<>(currentStorage.getBoundingBoxes());
        for (BoundingBox bb : bbs) {
            if (bb != null) {
                if (box.contains(bb)) {
                    currentStorage.deleteBoundingBox(bb);
                } else if (bb.contains(box)) {
                    return; // existing area
                }
            } else {
                Log.e(DEBUG_TAG, "download null existing bounding box");
                currentStorage.removeNullBoundingboxes();
            }
        }
        currentStorage.addBoundingBox(box);
    }

    /**
     * Get the number of Nodes in API storage
     * 
     * @return the number of Nodes in API storage
     */
    public int getApiNodeCount() {
        return apiStorage.getNodeCount();
    }

    /**
     * Get the number of Ways in API storage
     * 
     * @return the number of Ways in API storage
     */
    public int getApiWayCount() {
        return apiStorage.getWayCount();
    }

    /**
     * Get the number of Relations in API storage
     * 
     * @return the number of Relations in API storage
     */
    public int getApiRelationCount() {
        return apiStorage.getRelationCount();
    }

    /**
     * Get the total number of elements in API storage
     * <p>
     * Returns the total number of elements to be created, modified or deleted
     * 
     * @return the element count
     */
    public int getApiElementCount() {
        return apiStorage.getRelationCount() + apiStorage.getWayCount() + apiStorage.getNodeCount();
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
     * Check if we have data in Storage
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
    public synchronized void writeToFile(@NonNull Context ctx) throws IOException {
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
     * @param closeOpenChangeset if true close any open Changeset first
     * @param closeChangeset if true close the Changeset
     * @param extraTags Additional tags to add
     * @param elements List of OsmElement to upload if null all changed elements will be uploaded
     * @throws IOException if the upload doesn't work
     */
    public synchronized void uploadToServer(@NonNull final Server server, @Nullable final String comment, @Nullable String source, boolean closeOpenChangeset,
            boolean closeChangeset, @Nullable Map<String, String> extraTags, @Nullable List<OsmElement> elements) throws IOException {

        dirty = true; // storages will get modified as data is uploaded, these changes need to be saved to file
        removeUnchanged();
        // upload methods set dirty flag too, in case the file is saved during an upload
        boolean fullUpload = elements == null;
        int uploadElementCount = fullUpload ? getApiElementCount() : elements.size();
        int notUploadedElementCount = getApiElementCount() - uploadElementCount; // will be zero for normal uploads
        boolean split = uploadElementCount > server.getCapabilities().getMaxElementsInChangeset();
        int part = 1;
        int elementCount = uploadElementCount;
        while (elementCount > 0) {
            String tmpSource = source;
            if (split) {
                tmpSource = source + " [" + part + "]";
            }
            server.openChangeset(closeOpenChangeset, comment, tmpSource, Util.listToOsmList(imagery), extraTags);
            try {
                lock();
                if (fullUpload) {
                    server.diffUpload(this, getApiStorage());
                } else {
                    Storage storage = new Storage();
                    // if we are uploading more than the limit elements
                    // this will work as uploaded elements will have
                    // unmodified status
                    storage.addChangedElements(elements);
                    server.diffUpload(this, storage);
                }
            } finally {
                unlock();
            }

            if (closeChangeset || split) { // always close when splitting
                server.closeChangeset();
            }
            part++;
            int currentElementCount = getApiElementCount();
            if (currentElementCount < notUploadedElementCount + elementCount) {
                elementCount = currentElementCount - notUploadedElementCount;
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
        if (fullUpload) {
            setImageryRecorded(false);
        }
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
        return FileExtensions.OSC;
    }

    /**
     * Merge additional data with existing, copy to a new storage because this may fail
     * 
     * This may throw an IllegalStateException if existing data was inconsistent
     * 
     * @param storage storage containing data to merge
     * @param postMerge handler to run after merging
     * @return true if the merge was successful
     */
    public boolean mergeData(@NonNull Storage storage, @Nullable PostMergeHandler postMerge) {
        Log.d(DEBUG_TAG, "mergeData called");

        if (storage.isEmpty()) { // no point in doing anything
            return true;
        }

        List<OsmElement> newElements = new ArrayList<>(); // elements that we need to run postMerg on

        synchronized (this) {

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
                        newElements.add(n);
                    } else {
                        if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
                            if (apiNode.getOsmVersion() >= n.getOsmVersion()) {
                                continue; // can use node we already have
                            } else {
                                return false; // can't resolve conflicts, upload first
                            }
                        }
                        Node existingNode = nodeIndex.get(n.getOsmId());
                        if (existingNode != null) {
                            if (existingNode.getOsmVersion() >= n.getOsmVersion()) { // larger just to be on the safe
                                                                                     // side
                                continue; // can use node we already have
                            } else {
                                if (existingNode.isUnchanged()) {
                                    temp.insertNodeUnsafe(n);
                                    newElements.add(n);
                                } else {
                                    return false; // can't resolve conflicts, upload first
                                }
                            }
                        } else {
                            // this shouldn't be able to happen
                            String debugString = "mergeData null existing node " + n.getOsmId() + " containsKey is " + nodeIndex.containsKey(n.getOsmId())
                                    + " apiNode is " + apiNode;
                            Log.e(DEBUG_TAG, debugString);
                            ACRAHelper.nocrashReport(null, debugString);
                            return false;
                        }
                    }
                }

                Log.d(DEBUG_TAG, "mergeData added nodes");

                // add ways
                for (Way w : storage.getWays()) {
                    Way apiWay = apiStorage.getWay(w.getOsmId()); // can contain deleted elements
                    if (!wayIndex.containsKey(w.getOsmId()) && apiWay == null) { // new way no problem
                        temp.insertWayUnsafe(w);
                        newElements.add(w);
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
                                    newElements.add(w);
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

                Log.d(DEBUG_TAG, "mergeData fixuped way nodes nodes");

                // add relations
                for (Relation r : storage.getRelations()) {
                    Relation apiRelation = apiStorage.getRelation(r.getOsmId()); // can contain deleted elements
                    if (!relationIndex.containsKey(r.getOsmId()) && apiRelation == null) { // new relation no problem
                        temp.insertRelationUnsafe(r);
                        newElements.add(r);
                    } else {
                        if (apiRelation != null && apiRelation.getState() == OsmElement.STATE_DELETED) {
                            if (apiRelation.getOsmVersion() >= r.getOsmVersion()) {
                                continue; // can use relation we already have
                            } else {
                                return false; // can't resolve conflicts, upload first
                            }
                        }
                        Relation existingRelation = relationIndex.get(r.getOsmId());
                        if (existingRelation != null) {
                            if (existingRelation.getOsmVersion() >= r.getOsmVersion()) { // larger just to be on the
                                                                                         // safe side
                                continue; // can use relation we already have
                            } else {
                                if (existingRelation.isUnchanged()) {
                                    temp.insertRelationUnsafe(r);
                                    newElements.add(r);
                                } else {
                                    return false; // can't resolve conflicts, upload first
                                }
                            }
                        } else {
                            // this shouldn't be able to happen
                            String debugString = "mergeData null existing relation " + r.getOsmId() + " containsKey is "
                                    + relationIndex.containsKey(r.getOsmId()) + " apiRelation is " + apiRelation;
                            Log.e(DEBUG_TAG, debugString);
                            ACRAHelper.nocrashReport(null, debugString);
                            return false;
                        }
                    }
                }

                Log.d(DEBUG_TAG, "mergeData added relations");

                // fixup relation back links and memberships

                if (!redoBacklinks(temp, nodeIndex, wayIndex, relationIndex)) {
                    Log.e(DEBUG_TAG, "mergeData redoBacklinks failed");
                    return false;
                }

                Log.d(DEBUG_TAG, "mergeData fixuped relations");

            } catch (StorageException sex) {
                // ran out of memory
                Log.e(DEBUG_TAG, "mergeData exception " + sex.getMessage());
                return false;
            }

            currentStorage = temp;
            undo.setCurrentStorage(temp);
        }
        // no need to do this in the synchronized block
        if (postMerge != null) {
            for (OsmElement e : newElements) {
                postMerge.handler(e);
            }
        }

        return true; // Success
    }

    /**
     * Redo all backlinks
     * 
     * This may throw an IllegalStateException if existing data was inconsistent
     * 
     * @param tempCurrent temp storage
     * @param nodeIndex index to the nodes in temp
     * @param wayIndex index to the ways in temp
     * @param relationIndex index to the relations in temp
     * @return true if successful
     */
    private boolean redoBacklinks(@NonNull Storage tempCurrent, @NonNull LongOsmElementMap<Node> nodeIndex, @NonNull LongOsmElementMap<Way> wayIndex,
            @NonNull LongOsmElementMap<Relation> relationIndex) {
        // zap all existing backlinks for our "old" relations
        for (Relation r : currentStorage.getRelations()) {
            final List<RelationMember> members = r.getMembers();
            if (members != null) {
                for (RelationMember rm : members) {
                    checkMember(r.getOsmId(), rm);
                    final long ref = rm.getRef();
                    final String type = rm.getType();
                    OsmElement e = elementFromIndex(r, type, ref, nodeIndex, wayIndex, relationIndex);
                    if (e != null) {
                        e.clearParentRelations();
                    }
                }
            } else {
                Log.e(DEBUG_TAG, "Relation has no members " + r.getOsmId());
            }
        }

        // add backlinks for all "new" relations
        for (Relation r : tempCurrent.getRelations()) {
            final List<RelationMember> members = r.getMembers();
            if (members != null) {
                for (RelationMember rm : members) {
                    checkMember(r.getOsmId(), rm);
                    final long ref = rm.getRef();
                    final String type = rm.getType();
                    OsmElement e = elementFromIndex(r, type, ref, nodeIndex, wayIndex, relationIndex);
                    if (e != null) {
                        rm.setElement(e);
                        e.addParentRelation(r);
                    } else if (memberIsDeleted(r, rm)) {
                        Log.e(DEBUG_TAG, "redoBacklinks relation " + r.getOsmId() + " member " + type + " " + ref + " missing");
                        return false;
                    } else if (rm.downloaded()) {
                        Log.w(DEBUG_TAG, "redoBacklinks relation " + r.getOsmId() + " member " + type + " " + ref + " not in target storage");
                        rm.setElement(null);
                    }
                }
            } else {
                Log.e(DEBUG_TAG, "Relation has no members " + r.getOsmId());
            }
        }
        return true; // successful
    }

    /**
     * Retrieve a relation member OsmElement from the appropriate index
     * 
     * @param r the Relation
     * @param type type of OsmElement
     * @param ref the OSM id of the element
     * @param nodeIndex node index
     * @param wayIndex way index
     * @param relationIndex relation index
     * @return the element or null
     */
    public OsmElement elementFromIndex(@NonNull Relation r, @NonNull final String type, final long ref, @NonNull LongOsmElementMap<Node> nodeIndex,
            @NonNull LongOsmElementMap<Way> wayIndex, @NonNull LongOsmElementMap<Relation> relationIndex) {
        OsmElement e = null;
        switch (type) {
        case Node.NAME:
            e = nodeIndex.get(ref);
            break;
        case Way.NAME:
            e = wayIndex.get(ref);
            break;
        case Relation.NAME:
            e = relationIndex.get(ref);
            break;
        default:
            logUnknownMemberType(r, type);
        }
        return e;
    }

    /**
     * Log an unknown member type
     * 
     * @param r the Relation
     * @param type the type
     */
    private void logUnknownMemberType(@NonNull Relation r, @NonNull final String type) {
        Log.e(DEBUG_TAG, "Unknown member type " + type + " for relation " + r.getOsmId());
    }

    /**
     * Generate a log message and throw an IllegalStateException if rm or the type is null
     * 
     * @param id the relation OSM id
     * @param rm the member
     */
    private void checkMember(long id, @Nullable RelationMember rm) {
        if (rm == null) {
            Log.e(DEBUG_TAG, "Null member of relation " + id);
            throw new IllegalStateException("Null member of relation " + id);
        } else if (rm.type == null) {
            Log.e(DEBUG_TAG, "Relation member with null type in " + id);
            throw new IllegalStateException("Relation member with null type in " + id);
        }
    }

    /**
     * Ensure that we have consistent backlinks
     */
    void fixupBacklinks() {
        // first zap all, really all, as referenced relations may have been deleted
        // a possible alternative would be to check undostorage for any relations
        for (OsmElement e : currentStorage.getElements()) {
            if (e != null) {
                e.clearParentRelations();
            }
        }
        // then add them back
        for (Relation r : currentStorage.getRelations()) {
            final List<RelationMember> members = r.getMembers();
            if (members != null) {
                for (RelationMember rm : r.getMembers()) {
                    OsmElement e = null;
                    final String type = rm.getType();
                    final long ref = rm.getRef();
                    switch (type) {
                    case Node.NAME:
                        e = currentStorage.getNode(ref);
                        break;
                    case Way.NAME:
                        e = currentStorage.getWay(ref);
                        break;
                    case Relation.NAME:
                        e = currentStorage.getRelation(ref);
                        break;
                    default:
                        logUnknownMemberType(r, type);
                    }
                    if (e != null) {
                        e.addParentRelation(r);
                    }
                }
            }
        }
    }

    /**
     * Safely remove data that is not in/intersects with the provided BoundingBox
     * 
     * Skips selected elements, removes BoundingBoxes
     * 
     * FIXME to determine if an element is selected this uses the current Logic instance
     * 
     * @param box the BoundingBox
     */
    @Override
    public synchronized void prune(@NonNull BoundingBox box) {
        prune(App.getLogic(), box);
    }

    /**
     * Safely remove data that is not in/intersects with the provided BoundingBox
     * 
     * Skips selected elements, removes BoundingBoxes
     * 
     * @param logic the current Logic instance if null element selection will not be tested
     * @param box the BoundingBox
     */
    protected void prune(@Nullable Logic logic, @NonNull BoundingBox box) {
        LongHashSet keepNodes = new LongHashSet();
        boolean noLogic = logic == null;

        for (Way w : currentStorage.getWays()) {
            if (apiStorage.getWay(w.getOsmId()) == null && !box.intersects(w.getBounds()) && (noLogic || !logic.isSelected(w))) {
                currentStorage.removeWay(w);
                removeReferenceFromParents(logic, w);
            } else { // keeping so we need to keep the nodes
                for (Node n : w.getNodes()) {
                    keepNodes.put(n.getOsmId());
                }
            }
        }
        for (Node n : currentStorage.getNodes()) {
            long nodeId = n.getOsmId();
            if (apiStorage.getNode(nodeId) == null && !box.contains(n.getLon(), n.getLat()) && !keepNodes.contains(nodeId)
                    && (noLogic || !logic.isSelected(n))) {
                currentStorage.removeNode(n);
                removeReferenceFromParents(logic, n);
            }
        }
        for (Relation r : currentStorage.getRelations()) {
            long relationId = r.getOsmId();
            if (apiStorage.getRelation(relationId) == null && (noLogic || !logic.isSelected(r)) && !r.hasDownloadedMembers()) {
                // Note: this will not remove already processed relations that had this as a member however further
                // prune passes will eventually delete them, which is good enough and so we don't rerun this explicitly
                // here
                currentStorage.removeRelation(r);
                removeReferenceFromParents(logic, r);
            }
        }
        BoundingBox.prune(this, box);
        dirty();
    }

    /**
     * Remove the references to downloaded elements from parent Relations
     * 
     * @param logic the current Logic instance or null, this is required because elements may be members of selected
     *            relations
     * @param e the OsmElement we want to remove references for
     */
    private void removeReferenceFromParents(@Nullable Logic logic, @NonNull OsmElement e) {
        List<Relation> parents = e.getParentRelations();
        if (parents != null) {
            for (Relation parent : parents) { // remove link from parent relations
                List<RelationMember> members = parent.getAllMembers(e);
                for (RelationMember member : members) {
                    member.setElement(null);
                }
            }
            if (logic != null) {
                logic.removeSelectedRelationElement(e);
            }
        }
    }

    /**
     * Remove all unchanged elements, retaining parent Relations for changed ones
     * 
     * Note this doesn't handle selected elements and should only be called when nothing is selected
     */
    public synchronized void pruneAll() {
        LongHashSet keepNodes = new LongHashSet();
        LongHashSet keepRelations = new LongHashSet();

        for (Way w : currentStorage.getWays()) {
            if (apiStorage.getWay(w.getOsmId()) == null) {
                currentStorage.removeWay(w);
            } else { // keeping so we need to keep the nodes
                for (Node n : w.getNodes()) {
                    keepNodes.put(n.getOsmId());
                }
                keepParents(keepRelations, w);
            }
        }
        for (Node n : currentStorage.getNodes()) {
            long nodeId = n.getOsmId();
            if (apiStorage.getNode(nodeId) == null && !keepNodes.contains(nodeId)) {
                currentStorage.removeNode(n);
            } else {
                keepNodes.put(nodeId);
                keepParents(keepRelations, n);
            }
        }
        for (Relation r : currentStorage.getRelations()) {
            long relationId = r.getOsmId();
            if (apiStorage.getRelation(relationId) != null) {
                keepRelations.put(relationId);
                keepParents(keepRelations, r);
            }
        }
        for (Relation r : currentStorage.getRelations()) {
            if (!keepRelations.contains(r.getOsmId())) {
                currentStorage.removeRelation(r);
            }
        }
        fixupBacklinks();
        dirty();
    }

    /**
     * Recursively add parent relations to keep
     * 
     * @param keepRelations the Set of relations to keep
     * @param e the OsmElement the parents of we want to keep
     */
    void keepParents(@NonNull LongHashSet keepRelations, @NonNull OsmElement e) {
        List<Relation> parents = e.getParentRelations();
        if (parents != null) {
            for (Relation r : parents) {
                long relationId = r.getOsmId();
                if (!keepRelations.contains(relationId)) {
                    keepRelations.put(relationId);
                    keepParents(keepRelations, r);
                }
            }
        }
    }

    /**
     * Merge additional data with existing, copy to a new storage because this may fail
     * 
     * If this is aborted the contents of the undo checkpoint need to be removed, this may throw an
     * IllegalStateException if existing data was inconsistent
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
        UndoStorage tempUndo = new UndoStorage(undo, tempCurrent, tempApi);

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
                    tempUndo.save(n, false, false);
                    if (postMerge != null) {
                        postMerge.handler(n);
                    }
                } else {
                    if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
                        if (apiNode.getOsmVersion() > n.getOsmVersion()) {
                            continue; // can use node we already have
                        } else if (state == OsmElement.STATE_DELETED || state == OsmElement.STATE_MODIFIED) {
                            tempUndo.save(apiNode);
                            tempApi.insertElementUnsafe(n);
                            if (state == OsmElement.STATE_MODIFIED) {
                                tempCurrent.insertElementUnsafe(n);
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
                            tempUndo.save(existingNode, true, false);
                            tempApi.insertNodeUnsafe(n);
                            tempCurrent.insertElementUnsafe(n);
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
                    tempUndo.save(w, false, false);
                    if (postMerge != null) {
                        postMerge.handler(w);
                    }
                } else {
                    if (apiWay != null && apiWay.getState() == OsmElement.STATE_DELETED) {
                        if (apiWay.getOsmVersion() > w.getOsmVersion()) {
                            continue; // can use node we already have
                        } else if (state == OsmElement.STATE_DELETED || state == OsmElement.STATE_MODIFIED) {
                            tempUndo.save(apiWay);
                            tempApi.insertElementUnsafe(w);
                            if (state == OsmElement.STATE_MODIFIED) {
                                tempCurrent.insertElementSafe(w);
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
                            tempUndo.save(existingWay, true, false);
                            tempApi.insertWayUnsafe(w);
                            tempCurrent.insertElementUnsafe(w);
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
                    tempUndo.save(r, false, false);
                    if (postMerge != null) {
                        postMerge.handler(r);
                    }
                } else {
                    if (apiRelation != null && apiRelation.getState() == OsmElement.STATE_DELETED) {
                        if (apiRelation.getOsmVersion() > r.getOsmVersion()) {
                            continue; // can use relation we already have
                        } else if (state == OsmElement.STATE_DELETED || state == OsmElement.STATE_MODIFIED) {
                            tempUndo.save(apiRelation);
                            tempApi.insertElementUnsafe(r);
                            if (state == OsmElement.STATE_MODIFIED) {
                                tempCurrent.insertElementUnsafe(r);
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
                            tempUndo.save(existingRelation, true, false);
                            tempApi.insertRelationUnsafe(r);
                            tempCurrent.insertElementUnsafe(r);
                            if (postMerge != null) {
                                postMerge.handler(r);
                            }
                        }
                    }
                }
            }

            Log.d(DEBUG_TAG, "applyOsc done relations");

            // fixup relation back links and memberships
            if (!redoBacklinks(tempCurrent, nodeIndex, wayIndex, relationIndex)) {
                Log.e(DEBUG_TAG, "applyOsc redoBacklinks failed");
                return false;
            }

            Log.d(DEBUG_TAG, "applyOsc fixuped relations");

        } catch (StorageException sex) {
            Log.d(DEBUG_TAG, "applyOsc aborting " + sex.getMessage());
            return false;
        }

        Log.d(DEBUG_TAG, "applyOsc finshed");
        undo = tempUndo;
        currentStorage = tempCurrent;
        apiStorage = tempApi;
        return true; // Success
    }

    /**
     * Check if a referenced relation member is deleted
     * 
     * @param r the Relation
     * @param rm the RelationMember
     * @return true if deleted
     */
    private boolean memberIsDeleted(@NonNull Relation r, @NonNull RelationMember rm) {
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
     * Set an OemElement to new state and remove it from the upload This is only used when trying to fix conflicts
     * 
     * @param element the OsmElement
     * @param state the new state
     */
    public void removeFromUpload(@NonNull OsmElement element, byte state) {
        apiStorage.removeElement(element);
        undo.save(element);
        element.setState(state);
    }

    /**
     * Set the version of an OsmElement This is only used when trying to fix conflicts
     * 
     * @param element the OsmElement
     * @param version the new version
     */
    public void setOsmVersion(@NonNull OsmElement element, long version) {
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
     * @return the last BoundingBox in the list or an empty one
     */
    @NonNull
    public BoundingBox getLastBox() {
        return currentStorage.getLastBox();
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
