package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.exception.StorageException;
import de.blau.android.presets.Preset;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.Util;

/**
 * This class provides undo support. It is absolutely critical that {@link StorageDelegator} calls
 * {@link #save(OsmElement)} each and every time something changes, as otherwise undo will create inconsistencies.
 * 
 * Checkpoints should be created at appropriate intervals, e.g. before each user action is performed, using
 * {@link #createCheckpoint(String)}.
 * 
 * The undo function works by storing the original state of each {@link OsmElement} before changes are performed in each
 * check point. As soon as a state is saved, any further changes within the same checkpoint will be ignored, as the
 * state at the beginning of the checkpoint is already stored.
 * 
 * On undo, the state is restored. This includes not only the values of the element, but also to its presence in the
 * currentStorage and apiStorage. For this reason, the state includes whether the element was in each of the storages,
 * and on undo, it will be added or deleted if necessary.
 * 
 * Avoid calling functions that change the state from other threads except the main one. This may mess up your menu due
 * to calls to updateIcon. You have been warned.
 * 
 * @author Jan Schejbal
 * @author Simon Poole
 */
public class UndoStorage implements Serializable {
    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = UndoStorage.class.getSimpleName().substring(0, Math.min(23, UndoStorage.class.getSimpleName().length()));

    // Original storages for "contains" checks and restoration
    private Storage currentStorage;
    private Storage apiStorage;

    private final LinkedList<Checkpoint> undoCheckpoints = new LinkedList<>();
    private final LinkedList<Checkpoint> redoCheckpoints = new LinkedList<>();

    static final Comparator<UndoElement> elementOrder = (ue1, ue2) -> {
        OsmElement e1 = ue1.element;
        OsmElement e2 = ue2.element;
        if ((e1 instanceof Node && e2 instanceof Node) || ((e1 instanceof Way) && (e2 instanceof Way))) {
            return 0;
        }
        if (!(e1 instanceof Node) && e2 instanceof Node) {
            return 1;
        }
        if ((e1 instanceof Node) && !(e2 instanceof Node)) {
            return -1;
        }
        if (e1 instanceof Relation && e2 instanceof Way) {
            return 1;
        }
        if (e1 instanceof Way && e2 instanceof Relation) {
            return -1;
        }
        if (e1 instanceof Relation && e2 instanceof Relation) {
            if (e1.getOsmId() == e2.getOsmId()) {
                return 0;
            }
            Relation r1 = (Relation) e1;
            Relation r2 = (Relation) e2;
            if (r1.hasParentRelation(r2)) {
                return -1;
            }
            if (r2.hasParentRelation(r1)) {
                return 1;
            }
        }
        return 0;
    };

    /**
     * Creates a new UndoStorage. You need to pass the storage objects to which changes are applied. Please ensure that
     * any time the {@link Logic} starts to use different objects, a new UndoStorage pointing to the correct objects is
     * created. Otherwise, undo will mess up your data badly.
     * 
     * @param currentStorage the currentStorage in use
     * @param apiStorage the apiStorage in use
     */
    public UndoStorage(@NonNull Storage currentStorage, @NonNull Storage apiStorage) {
        this.currentStorage = currentStorage;
        this.apiStorage = apiStorage;
    }

    /**
     * Construct a copy of a UndoStorage
     * 
     * @param undoStorage the UndoStorage to copy
     * @param currentStorage the currentStorage to use
     * @param apiStorage the apiStorage to use
     */
    public UndoStorage(@NonNull UndoStorage undoStorage, @NonNull Storage currentStorage, @NonNull Storage apiStorage) {
        this.currentStorage = currentStorage;
        this.apiStorage = apiStorage;
        for (Checkpoint cp : undoStorage.undoCheckpoints) {
            undoCheckpoints.add(new Checkpoint(cp));
        }
        for (Checkpoint cp : undoStorage.redoCheckpoints) {
            redoCheckpoints.add(new Checkpoint(cp));
        }
    }

    /**
     * Set currentStorage without creating a new instance
     * 
     * @param currentStorage the current OsmElement storage
     */
    public void setCurrentStorage(@NonNull Storage currentStorage) {
        this.currentStorage = currentStorage;
    }

    /**
     * Set apiStorage without creating a new instance
     * 
     * @param apiStorage the api OsmElement storage
     */
    public void setApiStorage(@NonNull Storage apiStorage) {
        this.apiStorage = apiStorage;
    }

    /**
     * Call to create a new checkpoint. When the user performs an undo operation, the state will be reverted to what it
     * was at the last checkpoint. Checkpoints should NOT be created for changes that are made as part of other
     * operations. For this reason, checkpoints usually need to be triggered in {@link Logic}, not
     * {@link StorageDelegator}.
     * 
     * @param name the name of the checkpoint, used for debugging and display purposes
     */
    public void createCheckpoint(@NonNull String name) {
        if (undoCheckpoints.isEmpty() || !undoCheckpoints.getLast().isEmpty()) {
            undoCheckpoints.add(new Checkpoint(name));
        } else {
            // Empty checkpoint exists, just rename it
            undoCheckpoints.getLast().setName(name);
        }
    }

    /**
     * remove checkpoint from list. typically called when we otherwise would have an empty checkpoint at the top
     * 
     * @param name checkpoint name
     */
    public void removeCheckpoint(@NonNull String name) {
        removeCheckpoint(name, false);
    }

    /**
     * remove checkpoint from list. typically called when we otherwise would have an empty checkpoint at the top
     * 
     * @param name checkpoint name
     * @param force remove even if checkpoint is not empty
     */
    public void removeCheckpoint(@NonNull String name, boolean force) {
        if (!undoCheckpoints.isEmpty() && (undoCheckpoints.getLast().isEmpty() || force) && undoCheckpoints.getLast().getName().equals(name)) {
            undoCheckpoints.removeLast();
        }
    }

    /**
     * Saves the current state of the element in the checkpoint. Call before any changes to the element. A checkpoint
     * needs to be created first using {@link #createCheckpoint(String)}, otherwise an error is logged and the function
     * does nothing.
     * 
     * @param element the element to save
     */
    void save(@NonNull OsmElement element) {
        try {
            if (undoCheckpoints.isEmpty()) {
                Log.e(DEBUG_TAG, "Attempted to save without valid checkpoint - forgot to call createCheckpoint()");
                return;
            }
            undoCheckpoints.getLast().add(element);
        } catch (Exception ex) {
            ACRAHelper.nocrashReport(ex, ex.getMessage());
        }
    }

    /**
     * Saves the current state of the element in the checkpoint. Call before any changes to the element. A checkpoint
     * needs to be created first using {@link #createCheckpoint(String)}, otherwise an error is logged and the function
     * does nothing.
     * 
     * @param element the element to save¨
     * @param inCurrentStorage true if the element is in the current storage
     * @param inApiStorage true if the element is in the api storage
     */
    void save(@NonNull OsmElement element, boolean inCurrentStorage, boolean inApiStorage) {
        try {
            if (undoCheckpoints.isEmpty()) {
                Log.e(DEBUG_TAG, "Attempted to save without valid checkpoint - forgot to call createCheckpoint()");
                return;
            }
            undoCheckpoints.getLast().add(element, inCurrentStorage, inApiStorage);
        } catch (Exception ex) {
            ACRAHelper.nocrashReport(ex, ex.getMessage());
        }
    }

    /**
     * Remove the saved state of this element from the last checkpoint
     * 
     * @param element element for which the state should be removed
     */
    public void remove(@NonNull OsmElement element) {
        Checkpoint checkpoint = undoCheckpoints.getLast();
        if (checkpoint != null) {
            checkpoint.remove(element);
        }
    }

    /**
     * Remove the saved state of this element from the all checkpoints
     * 
     * @param element element for which the state should be removed
     */
    public void removeFromAll(@NonNull OsmElement element) {
        for (Checkpoint checkpoint : new ArrayList<>(undoCheckpoints)) {
            if (checkpoint != null) {
                checkpoint.remove(element);
                if (checkpoint.isEmpty()) {
                    undoCheckpoints.remove(checkpoint);
                }
            }
        }
        for (Checkpoint checkpoint : new ArrayList<>(redoCheckpoints)) {
            if (checkpoint != null) {
                checkpoint.remove(element);
                if (checkpoint.isEmpty()) {
                    undoCheckpoints.remove(checkpoint);
                }
            }
        }
    }

    /**
     * Get the current BoundingBox of the elements affected by the last Checkpoint
     * 
     * @return a BoundingBox or null
     */
    @Nullable
    public BoundingBox getCurrentBounds() {
        if (undoCheckpoints.isEmpty()) {
            return null;
        }
        Checkpoint checkpoint = undoCheckpoints.getLast();
        BoundingBox result = null;
        for (UndoElement ue : checkpoint.elements.values()) {
            BoundingBox box = ue.element.getBounds();
            if (box != null) {
                if (result == null) {
                    result = box;
                } else {
                    result.union(box);
                }
            }
        }
        return result;
    }

    /**
     * Get the BoundingBox of the last undo Checkpoint
     * 
     * @return a BoundingBox or null
     */
    @Nullable
    public BoundingBox getLastBounds() {
        return getLastBounds(undoCheckpoints);
    }

    /**
     * Get the BoundingBox of the last redo Checkpoint
     * 
     * @return a BoundingBox or null
     */
    @Nullable
    public BoundingBox getLastRedoBounds() {
        return getLastBounds(redoCheckpoints);
    }

    /**
     * Get the BoundingBox for the last checkpoint
     * 
     * @param checkpoints list of checkpoints
     * @return a BoundingBox or null
     */
    @Nullable
    private BoundingBox getLastBounds(@NonNull LinkedList<Checkpoint> checkpoints) {
        if (checkpoints.isEmpty()) {
            return null;
        }
        return getBounds(checkpoints.getLast());
    }

    /**
     * Get the BoundingBox of a Checkpoint
     * 
     * @param checkpoint the Checkpoint
     * @return a BoundingBox of null if it coudn't be determined
     */
    @Nullable
    public BoundingBox getBounds(@NonNull Checkpoint checkpoint) {
        BoundingBox result = null;
        for (UndoElement ue : checkpoint.elements.values()) {
            BoundingBox box = ue.getBounds(checkpoint);
            if (box != null) {
                if (result == null) {
                    result = box;
                } else {
                    result.union(box);
                }
            }
        }
        return result;
    }

    /**
     * Performs an undo operation, restoring the state at the last undo checkpoint. A redo checkpoint is automatically
     * created. If no checkpoint is available, an error is logged and the function does nothing.
     * 
     * @return the name of the undo checkpoint used, or null if no checkpoint was available
     */
    @Nullable
    public String undo() {
        return undo(true);
    }

    /**
     * Performs an undo operation, restoring the state at the last undo checkpoint. If no checkpoint is available, an
     * error is logged and the function does nothing.
     * 
     * @param createRedo if true create a redo checkpoint
     * @return the name of the undo checkpoint used, or null if no checkpoint was available
     */
    @Nullable
    public String undo(boolean createRedo) {
        if (!canUndo()) {
            Log.w(DEBUG_TAG, "Attempted to undo, but no undo checkpoints available");
            return null;
        }
        String name = undoCheckpoints.getLast().getName();
        if (createRedo) {
            Checkpoint redoPoint = new Checkpoint(name);
            undoCheckpoints.removeLast().restore(redoPoint);
            redoCheckpoints.add(redoPoint);
        } else {
            undoCheckpoints.removeLast().restore(null);
        }
        return name;
    }

    /**
     * Performs an undo operation, restoring a specific undo checkpoint. A redo checkpoint is automatically created. If
     * no checkpoint is available, an error is logged and the function does nothing.
     * 
     * @param checkpoint index of the checkpoint to undo
     * @return the name of the undo checkpoint used, or null if no checkpoint was available
     */
    @Nullable
    public String undo(int checkpoint) {
        if (!canUndo()) {
            Log.w(DEBUG_TAG, "Attempted to undo, but no undo checkpoints available");
            return null;
        }
        String name = undoCheckpoints.get(checkpoint).getName();
        Checkpoint redoPoint = new Checkpoint(name);
        if (undoCheckpoints.get(checkpoint).restore(redoPoint)) {
            undoCheckpoints.remove(checkpoint);
        }
        redoCheckpoints.add(redoPoint);
        return name;
    }

    /**
     * Performs an undo operation, restoring a specific undo checkpoint. No redo checkpoint is created.
     * 
     * @param checkpoint the checkpoint to undo
     */
    public void undo(@NonNull Checkpoint checkpoint) {
        checkpoint.restore(null);
    }

    /**
     * Performs an redo operation, restoring the state at the next redo checkpoint. A new undo checkpoint is
     * automatically created. If no checkpoint is available, an error is logged and the function does nothing.
     * 
     * @return the name of the redo checkpoint used, or null if no checkpoint was available
     */
    @Nullable
    public String redo() {
        if (!canRedo()) {
            Log.e(DEBUG_TAG, "Attempted to redo, but no redo checkpoints available");
            return null;
        }
        String name = redoCheckpoints.getLast().getName();
        Checkpoint reundoPoint = new Checkpoint(name);
        redoCheckpoints.removeLast().restore(reundoPoint);
        undoCheckpoints.add(reundoPoint);
        return name;
    }

    /**
     * Performs an redo operation, restoring the state at the next redo checkpoint. A new undo checkpoint is
     * automatically created. If no checkpoint is available, an error is logged and the function does nothing.
     * 
     * @param checkpoint index of the checkpoint to redo
     * @return the name of the redo checkpoint used, or null if no checkpoint was available
     */
    @Nullable
    public String redo(int checkpoint) {
        if (!canRedo()) {
            Log.e(DEBUG_TAG, "Attempted to redo, but no redo checkpoints available");
            return null;
        }
        String name = redoCheckpoints.get(checkpoint).getName();
        Checkpoint reundoPoint = new Checkpoint(name);
        redoCheckpoints.remove(checkpoint).restore(reundoPoint);
        undoCheckpoints.add(reundoPoint);
        return name;
    }

    /**
     * @return true if at least one undo checkpoint is available. The checkpoint itself is not checked for emptyness.
     */
    public boolean canUndo() {
        return !undoCheckpoints.isEmpty();
    }

    /**
     * @return true if at least one redo checkpoint is available.
     */
    public boolean canRedo() {
        return !redoCheckpoints.isEmpty();
    }

    /**
     * Represents an undo checkpoint to which the user can revert. Any time an element is <b>first</b> changed since the
     * checkpoint was created, the original element state is saved. (This is ensured by calling {@link #add(OsmElement)}
     * on each change - repeated changes are ignored.)
     * 
     * The checkpoint can later be restored using {@link #restore(Checkpoint)}.
     */
    public class Checkpoint implements Serializable {
        private static final long serialVersionUID = 2L;

        private final Map<OsmElement, UndoElement> elements = new HashMap<>();
        private String                             name;

        /**
         * Construct a new checkpoint
         * 
         * @param name name of the checkpoint
         */
        private Checkpoint(@NonNull String name) {
            this.name = name;
        }

        /**
         * Construct a copy of a checkpoint
         * 
         * @param cp the original Checkpoint
         */
        private Checkpoint(@NonNull Checkpoint cp) {
            name = cp.name;
            elements.putAll(cp.elements);
        }

        /**
         * Store the current state of the element, unless a state is already stored. Called before any changes to the
         * element occur via {@link UndoStorage#save(OsmElement)}.
         * 
         * @param element the element to save
         */
        private void add(@NonNull OsmElement element) {
            add(element, currentStorage.contains(element), apiStorage.contains(element));
        }

        /**
         * Store the current state of the element, unless a state is already stored. Called before any changes to the
         * element occur via {@link UndoStorage#save(OsmElement)}.
         * 
         * @param element the element to save
         * @param inCurrentStorage if true the elements should be restored to the current storage
         * @param inApiStorage if true the elements should be restored to the api storage
         */
        private void add(@NonNull OsmElement element, boolean inCurrentStorage, boolean inApiStorage) {
            if (elements.containsKey(element)) {
                return;
            }
            if (element instanceof Node) {
                elements.put(element, new UndoNode((Node) element, inCurrentStorage, inApiStorage));
            } else if (element instanceof Way) {
                elements.put(element, new UndoWay((Way) element, inCurrentStorage, inApiStorage));
            } else if (element instanceof Relation) {
                elements.put(element, new UndoRelation((Relation) element, inCurrentStorage, inApiStorage));
            } else {
                throw new IllegalArgumentException("Unsupported element type");
            }
        }

        /**
         * Remove the saved state for the element from this checkpoint
         * 
         * @param element the element for which remove the saved state
         */
        private void remove(@NonNull OsmElement element) {
            if (!elements.containsKey(element)) {
                return;
            }
            elements.remove(element);
        }

        /**
         * Tries to restore the storages to the state at the time of the creation of this checkpoint.
         * 
         * @param redoCheckpoint optional - if given, the reverted elements are added to this checkpoint to make a
         *            "redo" feature possible
         * @return true if the restore was successful
         */
        private boolean restore(@Nullable Checkpoint redoCheckpoint) {
            boolean ok = true;
            List<UndoElement> list = new ArrayList<>(elements.values());
            final StorageDelegator delegator = App.getDelegator();
            try {
                delegator.lock();
                if (redoCheckpoint != null) {
                    for (UndoElement ue : list) {
                        redoCheckpoint.add(getUptodateElement(ue.element)); // save current state
                    }
                }
                // we sort according to element type and relation membership so that
                // all member elements should be restored before their parents
                Collections.sort(list, elementOrder);
                boolean restoredNode = false;
                for (UndoElement ue : list) {
                    if (ue instanceof UndoNode) {
                        restoredNode = true;
                    }
                    ok = (ue.restore() != null) && ok;
                }
                if (restoredNode) {
                    // zap the bounding box of all ways as their geometry may have changed
                    //
                    // this looks expensive but is actually the cheapest option
                    for (Way way : currentStorage.getWays()) {
                        way.invalidateBoundingBox();
                    }
                }

                delegator.fixupBacklinks();
            } finally {
                delegator.unlock();
            }
            return ok;
        }

        /**
         * @return true if no elements have yet been stored in this checkpoint
         */
        private boolean isEmpty() {
            return elements.isEmpty();
        }

        /**
         * Get the name of this checkpoint
         * 
         * @return the name of the Checkpoint
         */
        private String getName() {
            return name;
        }

        /**
         * Set the name of the checkpoint
         * 
         * @param name the name to set
         */
        private void setName(@NonNull String name) {
            this.name = name;
        }

        /**
         * Get a Set of all the elements we have undo information for
         * 
         * @return a Set of OsmElement
         */
        @NonNull
        public Set<OsmElement> getSavedElements() {
            return elements.keySet();
        }

        /**
         * @return a string representation of the Checkpoint (its name)
         */
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * This class contains a past state of a {@link OsmElement}. It is stored in {@link Checkpoint}s and used to restore
     * the state of the element on undo. The constructor saves the state, {@link #restore()} restores it.
     * 
     * @author Jan
     */
    public abstract class UndoElement implements OsmElementInterface, Serializable {
        private static final long serialVersionUID = 2L;

        final OsmElement element;

        private final long                    osmId;
        private final long                    osmVersion;
        private final byte                    state;
        private final TreeMap<String, String> tags;

        private final boolean inCurrentStorage;
        private final boolean inApiStorage;

        private final List<Relation> parentRelations;

        /**
         * Create a new undo object
         * 
         * @param originalElement the OsmElement we want to save
         * @param inCurrentStorage true if the element is in the current storage
         * @param inApiStorage true if the element is in the api storage
         */
        protected UndoElement(@NonNull OsmElement originalElement, boolean inCurrentStorage, boolean inApiStorage) {
            this.inCurrentStorage = inCurrentStorage;
            this.inApiStorage = inApiStorage;

            element = originalElement;

            osmId = originalElement.getOsmId();
            osmVersion = originalElement.getOsmVersion();
            state = originalElement.getState();
            tags = originalElement.getTags().isEmpty() ? new TreeMap<>() : new TreeMap<>(originalElement.getTags());

            parentRelations = element.getParentRelations() != null ? new ArrayList<>(element.getParentRelations()) : null;
        }

        /**
         * Restores the saved state of the element
         * 
         * @return the element the restore was successful
         */
        @Nullable
        public OsmElement restore() {
            // Restore element existence
            Log.e(DEBUG_TAG, "restoring " + element.getOsmId() + " state " + state + " current " + inCurrentStorage + " api " + inApiStorage);
            OsmElement restored = getUptodateElement(element);
            try {
                if (inCurrentStorage) {
                    currentStorage.insertElementSafe(restored);
                } else {
                    Log.e(DEBUG_TAG, "removing from current");
                    currentStorage.removeElement(restored);
                }
                if (inApiStorage) {
                    apiStorage.insertElementSafe(restored);
                } else {
                    Log.e(DEBUG_TAG, "removing from api");
                    apiStorage.removeElement(restored);
                }
            } catch (StorageException e) {
                Log.e(DEBUG_TAG, "restore got " + e.getMessage());
                return null;
            }

            // restore saved values
            restored.setOsmId(osmId);
            restored.setOsmVersion(osmVersion);
            restored.setState(state);
            restored.setTags(tags);

            // zap error state
            restored.resetHasProblem();

            return restored;
        }

        /**
         * Get a short description of the UndoElement
         * 
         * @param ctx Android Context
         * @return a descriptive String
         */
        @NonNull
        public String getDescription(@Nullable Context ctx) {
            // Use the name if it exists
            if (tags != null) {
                String name = tags.get(Tags.KEY_NAME);
                if (Util.notEmpty(name)) {
                    return name;
                }

                String address = OsmElement.getAddressString(ctx, tags);
                if (Util.notEmpty(address)) {
                    return address;
                }
                // Then the value of the most 'important' tag the element has
                String result = null;
                for (String tag : Tags.IMPORTANT_TAGS) {
                    result = getTagValueString(tag);
                    if (result != null) {
                        return result;
                    }
                }
                if (ctx != null) {
                    Preset[] presets = App.getCurrentPresets(ctx);
                    for (Preset p : presets) {
                        if (p != null) {
                            for (String key : p.getObjectKeys()) {
                                result = getTagValueString(key);
                                if (result != null) {
                                    return result;
                                }
                            }
                        }
                    }
                }
            }
            // Failing the above, the OSM ID
            return element.getName() + " #" + Long.toString(element.getOsmId());
        }

        /**
         * Get a formated string version of the tag
         * 
         * @param tag the tag to format
         * @return a the tag as a string of the form osm element type key:value {@link #osmId}
         */
        @Nullable
        private String getTagValueString(@NonNull String tag) {
            String value = tags.get(tag);
            if (value != null && value.length() > 0) {
                return element.getName() + " " + tag + ":" + value + " #" + Long.toString(element.getOsmId());
            }
            return null;
        }

        @Override
        @NonNull
        public SortedMap<String, String> getTags() {
            return Collections.unmodifiableSortedMap(tags);
        }

        @Override
        @Nullable
        public List<Relation> getParentRelations() {
            return parentRelations != null ? Collections.unmodifiableList(parentRelations) : null;
        }

        @Override
        public long getOsmId() {
            return osmId;
        }

        @Override
        public long getOsmVersion() {
            return osmVersion;
        }

        @Override
        public byte getState() {
            return state;
        }

        @Override
        public long getTimestamp() {
            return -1;
        }

        /**
         * Get a BoundingBox for the element
         * 
         * @param checkpoint the Checkpoint this element is located in
         * @return a BoundingBox or null
         */
        @Nullable
        public abstract BoundingBox getBounds(@NonNull Checkpoint checkpoint);

        /**
         * Check if this element was in storage when it was saved
         * 
         * @return true if it was in the current storage
         */
        public boolean wasInCurrentStorage() {
            return inCurrentStorage;
        }
    }

    /**
     * Stores a past state of a node
     * 
     * @see UndoElement
     */
    public class UndoNode extends UndoElement implements GeoPoint, Serializable {
        private static final long serialVersionUID = 1L;
        private final int         lat;
        private final int         lon;

        /**
         * Create a new undo object
         * 
         * @param originalNode the Node we want to save
         * @param inCurrentStorage if true the elements should be restored to the current storage
         * @param inApiStorage if true the elements should be restored to the api storage
         */
        public UndoNode(@NonNull Node originalNode, boolean inCurrentStorage, boolean inApiStorage) {
            super(originalNode, inCurrentStorage, inApiStorage);
            lat = originalNode.lat;
            lon = originalNode.lon;
        }

        @Override
        public OsmElement restore() {
            OsmElement restored = super.restore();
            if (restored == null) {
                return null;
            }
            ((Node) restored).lat = lat;
            ((Node) restored).lon = lon;
            return restored;
        }

        /**
         * @return the longitude in WGS84*1E7
         */
        public int getLon() {
            return lon;
        }

        /**
         * @return the latitude in WGS84*1E7
         */
        public int getLat() {
            return lat;
        }

        @Override
        public BoundingBox getBounds(Checkpoint checkpoint) {
            return new BoundingBox(getLon(), getLat());
        }
    }

    /**
     * Stores a past state of a way
     * 
     * @see UndoElement
     */
    public class UndoWay extends UndoElement implements WayInterface, Serializable {
        private static final long serialVersionUID = 3L;
        private final List<Node>  nodes;

        /**
         * Create a new undo object
         * 
         * @param originalWay the Way we want to save
         * @param inCurrentStorage if true the elements should be restored to the current storage
         * @param inApiStorage if true the elements should be restored to the api storage
         */
        public UndoWay(@NonNull Way originalWay, boolean inCurrentStorage, boolean inApiStorage) {
            super(originalWay, inCurrentStorage, inApiStorage);
            nodes = inCurrentStorage || inApiStorage ? new ArrayList<>(originalWay.getNodes()) : new ArrayList<>();
        }

        @Override
        public OsmElement restore() {
            OsmElement restored = super.restore();
            // check that at least one node is available
            int inStorage = 0;
            for (Node n : nodes) {
                if (currentStorage.contains(n)) {
                    inStorage++;
                }
            }
            boolean deleted = super.state == OsmElement.STATE_DELETED;
            if (inStorage == 0 && !nodes.isEmpty() && !deleted) {
                // if no nodes we are restoring to pre-creation state without nodes which is ok
                Log.e(DEBUG_TAG, "#" + element.getOsmId() + " " + element.getDescription() + " is missing all nodes");
                // note this still allows ways with 1 node to be created which might be necessary
                return null;
            }
            // now we can restore with confidence
            if (restored == null) {
                return null;
            }
            ((Way) restored).removeAllNodes();
            for (Node n : nodes) {
                Node wayNode = currentStorage.getNode(n.getOsmId());
                boolean nodeNotNull = wayNode != null;
                if (nodeNotNull || deleted) {
                    // only add undeleted way nodes except if we are deleted
                    ((Way) restored).addNode(nodeNotNull ? wayNode : n);
                    if (nodeNotNull) {
                        wayNode.resetHasProblem();
                    }
                } else {
                    Log.w(DEBUG_TAG, "#" + element.getOsmId() + " " + element.getDescription() + " missing node " + n.getOsmId());
                    restored.updateState(OsmElement.STATE_MODIFIED);
                    try {
                        apiStorage.insertElementSafe(restored);
                    } catch (StorageException e) {
                        // TODO Handle OOM
                    }
                }
            }
            // reset the style
            ((Way) restored).setStyle(null);
            ((Way) restored).invalidateBoundingBox();
            return restored;
        }

        @Override
        public boolean isClosed() {
            return !nodes.isEmpty() && nodes.get(0).equals(nodes.get(nodes.size() - 1));
        }

        @Override
        public double length() {
            return Way.length(nodes);
        }

        @Override
        public int nodeCount() {
            return nodes == null ? 0 : nodes.size();
        }

        @Override
        public List<Node> getNodes() {
            return Collections.unmodifiableList(nodes);
        }

        @Override
        public BoundingBox getBounds(Checkpoint checkpoint) {
            return UndoStorage.getBounds(checkpoint, nodes);
        }
    }

    /**
     * Stores a past state of a relation
     * 
     * @see UndoElement
     */
    public class UndoRelation extends UndoElement implements RelationInterface, Serializable {

        private static final String        DEBUG_TAG        = "UndoRelation";
        private static final long          serialVersionUID = 1L;
        private final List<RelationMember> members;

        /**
         * Create a new undo object
         * 
         * @param originalRelation the Relation we want to save
         * @param inCurrentStorage if true the elements should be restored to the current storage
         * @param inApiStorage if true the elements should be restored to the api storage
         */
        public UndoRelation(@NonNull Relation originalRelation, boolean inCurrentStorage, boolean inApiStorage) {
            super(originalRelation, inCurrentStorage, inApiStorage);
            // deep copy
            members = new ArrayList<>();
            if (inCurrentStorage || inApiStorage) {
                for (RelationMember member : originalRelation.getMembers()) {
                    members.add(new RelationMember(member));
                }
            }
        }

        @Override
        public OsmElement restore() {
            OsmElement restored = super.restore();
            if (restored == null) {
                return null;
            }
            ((Relation) restored).getMembers().clear();
            for (RelationMember rm : members) {
                OsmElement rmElement = rm.getElement();
                if (rmElement instanceof StyleableFeature) {
                    ((StyleableFeature) rmElement).setStyle(null); // style could have been generated from Relation
                }
                OsmElement rmStorage = currentStorage.getOsmElement(rm.getType(), rm.getRef());
                Log.d(DEBUG_TAG, "rmElement " + rmElement + " rmStorage " + rmStorage);
                if (rmElement == null || rmStorage != null) {
                    rm.setElement(rmStorage);
                    ((Relation) restored).getMembers().add(rm); // only add undeleted members or ones that haven't
                                                                // been
                    // downloaded
                } else {
                    Log.e(DEBUG_TAG, rm.getType() + " #" + rmElement.getOsmId() + " member of relation #" + restored.getOsmId() + " is deleted");
                    restored.updateState(OsmElement.STATE_MODIFIED);
                    try {
                        apiStorage.insertElementSafe(restored);
                    } catch (StorageException e) {
                        // TODO Handle OOM
                    }
                }
            }
            return restored;
        }

        @Override
        @NonNull
        public List<RelationMember> getMembers() {
            return Collections.unmodifiableList(members);
        }

        @Override
        @NonNull
        public List<RelationMember> getAllMembers(@Nullable OsmElement e) {
            List<RelationMember> result = new ArrayList<>();
            for (int i = 0; i < members.size(); i++) {
                RelationMember member = members.get(i);
                if (member.getElement() == e) {
                    result.add(member);
                }
            }
            return result;
        }

        @Override
        public BoundingBox getBounds(Checkpoint checkpoint) {
            return UndoStorage.getBounds(checkpoint, getMembers(), 1);
        }
    }

    /**
     * Provides a list of names for the actions that can be undone
     * 
     * @param ctx Android context
     * @return a list of names, oldest action first (i.e. the last action will be the first to be undone)
     */
    @NonNull
    public String[] getUndoActions(@Nullable Context ctx) {
        return getCheckpointActions(ctx, undoCheckpoints);
    }

    /**
     * Provides a list of names for the actions that can be redone
     * 
     * @param ctx Android context
     * @return a list of names, newest action first (i.e. the last action will be the first to be redone)
     */
    @NonNull
    public String[] getRedoActions(@Nullable Context ctx) {
        return getCheckpointActions(ctx, redoCheckpoints);
    }

    /**
     * Provides a list of names for the checkpoints
     * 
     * @param ctx Android context
     * @param checkpoints List of Checkpoints
     * @return a list of names of the Checkpoints plus description
     */
    @NonNull
    private String[] getCheckpointActions(@Nullable Context ctx, @NonNull List<Checkpoint> checkpoints) {
        String[] result = new String[checkpoints.size()];
        int i = 0;
        for (Checkpoint checkpoint : checkpoints) {
            StringBuilder message = new StringBuilder(checkpoint.getName() + "<br>");
            for (UndoElement u : checkpoint.elements.values()) {
                message.append("<small>");
                message.append(u.getDescription(ctx));
                message.append("</small><br>");
            }
            result[i++] = message.toString();
        }
        return result;
    }

    /**
     * Get the original unchanged information of an element
     * 
     * @param element the element we are looking for
     * @return an UndoELement or null if no checkpoint containing element was found
     */
    @Nullable
    public UndoElement getOriginal(@NonNull OsmElement element) {
        List<UndoElement> undoElements = getUndoElements(element);
        if (!undoElements.isEmpty()) {
            return undoElements.get(0);
        }
        return null;
    }

    /**
     * Get a list of all UndoElements for a specific element
     * 
     * @param element the OsmElement
     * @return a List of UndoElement empty if nothing found
     */
    @NonNull
    public List<UndoElement> getUndoElements(@NonNull OsmElement element) {
        return getElements(undoCheckpoints, element);
    }

    /**
     * Get a list of all UndoElements for a specific element for Redo
     * 
     * @param element the OsmElement
     * @return a List of UndoElement empty if nothing found
     */
    @NonNull
    public List<UndoElement> getRedoElements(@NonNull OsmElement element) {
        return getElements(redoCheckpoints, element);
    }

    /**
     * Get a list of all UndoElements for a specific element
     * 
     * @param checkpoints list of checkpoints
     * @param element the element
     * @return a list of UndoElements empty if nothing found
     */
    @NonNull
    private List<UndoElement> getElements(@NonNull LinkedList<Checkpoint> checkpoints, @NonNull OsmElement element) {
        List<UndoElement> result = new ArrayList<>();
        String name = element.getName();
        long osmId = element.getOsmId();
        for (Checkpoint checkpoint : checkpoints) {
            for (UndoElement undoElement : checkpoint.elements.values()) {
                if (undoElement.element.getName().equals(name) && undoElement.osmId == osmId) {
                    result.add(undoElement);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Get a list of all Checkpoint for a specific element
     * 
     * @param element the OsmElement
     * @return a List of UndoElement empty if nothing found
     */
    @NonNull
    public List<Checkpoint> getUndoCheckpoints(@NonNull OsmElement element) {
        return getCheckpoints(undoCheckpoints, element);
    }

    /**
     * Get a list of all Checkpoint for a specific element for Redo
     * 
     * @param element the OsmElement
     * @return a List of Checkpoint empty if nothing found
     */
    @NonNull
    public List<Checkpoint> getRedoCheckpoints(@NonNull OsmElement element) {
        return getCheckpoints(redoCheckpoints, element);
    }

    /**
     * Get a list of all UndoElements for a specific element, more recent first, oldest last
     * 
     * @param checkpoints list of checkpoints
     * @param element the element
     * @return a list of UndoElements empty if nothing found
     */
    @NonNull
    private List<Checkpoint> getCheckpoints(@NonNull LinkedList<Checkpoint> checkpoints, @NonNull OsmElement element) {
        List<Checkpoint> result = new ArrayList<>();
        String name = element.getName();
        long osmId = element.getOsmId();
        for (Checkpoint checkpoint : checkpoints) {
            for (UndoElement undoElement : checkpoint.elements.values()) {
                if (undoElement.element.getName().equals(name) && undoElement.osmId == osmId) {
                    result.add(checkpoint);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Return a bounding box covering the relation with loop protection
     * 
     * This will stop when depth > MAX_DEPTH
     * 
     * The method tries to use any element that is contained in the Checkpoint, if this is not the top checkpoint and
     * the relation contains elements that were changed in later checkpoints the result will be incorrect.
     * 
     * @param checkpoint the current Checkpoint holding the element
     * @param members the relation members
     * @param depth current depth in the tree we are at
     * @return the BoundingBox or null if it cannot be determined
     */
    @Nullable
    private static BoundingBox getBounds(@NonNull Checkpoint checkpoint, @NonNull List<RelationMember> members, int depth) {
        // NOTE this will only return a bb covering the downloaded elements
        BoundingBox result = null;
        if (depth <= Relation.MAX_DEPTH) {
            for (RelationMember rm : members) {
                OsmElement e = rm.getElement();
                UndoElement ue = checkpoint.elements.get(e);
                BoundingBox box = null;
                if (ue != null) {
                    if (ue instanceof UndoRelation) {
                        box = getBounds(checkpoint, ((UndoRelation) ue).getMembers(), depth + 1);
                    } else if (ue instanceof UndoWay) {
                        box = getBounds(checkpoint, ((UndoWay) ue).nodes);
                    } else {
                        box = ue.getBounds(checkpoint);
                    }
                } else if (e != null) {
                    if (e instanceof Relation) {
                        box = getBounds(checkpoint, ((Relation) e).getMembers(), depth + 1);
                    } else if (e instanceof Way) {
                        box = getBounds(checkpoint, ((Way) e).getNodes());
                    } else {
                        box = e.getBounds();
                    }
                }
                if (box != null) {
                    if (result == null) {
                        result = box;
                    } else {
                        if (box != null) {
                            result.union(box);
                        }
                    }
                }
            }
        } else {
            Log.e(DEBUG_TAG, "getBounds relation nested too deep");
        }
        return result;
    }

    /**
     * Return a bounding box covering a way
     * 
     * The method tries to use any node that is contained in the Checkpoint, if this is not the top checkpoint and the
     * way contains nodes that were changed in later checkpoints the result will be incorrect.
     * 
     * @param checkpoint the current Checkpoint holding the way
     * @param nodes the list of way nodes
     * @return the BoundingBox or null (for a degenerate Way with no nodes)
     */
    @Nullable
    private static BoundingBox getBounds(@NonNull Checkpoint checkpoint, @NonNull List<Node> nodes) {
        BoundingBox result = null;
        for (Node n : nodes) {
            UndoNode un = (UndoNode) checkpoint.elements.get(n);
            if (un == null) {
                if (result == null) {
                    result = new BoundingBox(n.getLon(), n.getLat());
                } else {
                    result.union(n.getLon(), n.getLat());
                }
            } else {
                if (result == null) {
                    result = new BoundingBox(un.getLon(), un.getLat());
                } else {
                    result.union(un.getLon(), un.getLat());
                }
            }
        }
        return result;
    }

    /**
     * See if an element with same id and type is in storage, if yes use that
     * 
     * @param element the original element
     * @return the current object or the original one
     */
    @NonNull
    OsmElement getUptodateElement(@NonNull OsmElement element) {
        // see if an element with same id and type is in storage, if yes use that
        OsmElement currentElement = currentStorage.getOsmElement(element.getName(), element.getOsmId());
        if (currentElement == null) {
            OsmElement apiElement = apiStorage.getOsmElement(element.getName(), element.getOsmId());
            if (apiElement != null) {
                element = apiElement;
            }
        } else {
            element = currentElement;
        }
        return element;
    }
}
