package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.exception.StorageException;
import de.blau.android.presets.Preset;
import de.blau.android.util.ACRAHelper;

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
 */
public class UndoStorage implements Serializable {
    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = "UndoStorage";

    // Original storages for "contains" checks and restoration
    private Storage currentStorage;
    private Storage apiStorage;

    private final LinkedList<Checkpoint> undoCheckpoints = new LinkedList<>();
    private final LinkedList<Checkpoint> redoCheckpoints = new LinkedList<>();

    static final Comparator<UndoElement> elementOrder = new Comparator<UndoElement>() {
        @Override
        public int compare(UndoElement ue1, UndoElement ue2) {
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
        }
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
    void remove(@NonNull OsmElement element) {
        Checkpoint checkpoint = undoCheckpoints.getLast();
        if (checkpoint != null) {
            checkpoint.remove(element);
        }
    }

    /**
     * Performs an undo operation, restoring the state at the last undo checkpoint. A redo checkpoint is automatically
     * created. If no checkpoint is available, an error is logged and the function does nothing.
     * 
     * @return the name of the undo checkpoint used, or null if no checkpoint was available
     */
    public String undo() {
        return undo(true);
    }
    
    /**
     * Performs an undo operation, restoring the state at the last undo checkpoint. A redo checkpoint is automatically
     * created. If no checkpoint is available, an error is logged and the function does nothing.
     * 
     * @param createRedo if true create a redo checkpoint
     * @return the name of the undo checkpoint used, or null if no checkpoint was available
     */
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
     * Performs an redo operation, restoring the state at the next redo checkpoint. A new undo checkpoint is
     * automatically created. If no checkpoint is available, an error is logged and the function does nothing.
     * 
     * @return the name of the redo checkpoint used, or null if no checkpoint was available
     */
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
    private class Checkpoint implements Serializable {
        private static final long serialVersionUID = 2L;

        private final Map<OsmElement, UndoElement> elements = new HashMap<>();
        private String                             name;

        /**
         * Construct a new checkpoint
         * 
         * @param name name of the checkpoint
         */
        public Checkpoint(String name) {
            this.name = name;
        }

        /**
         * Store the current state of the element, unless a state is already stored. Called before any changes to the
         * element occur via {@link UndoStorage#save(OsmElement)}.
         * 
         * @param element the element to save
         */
        public void add(@NonNull OsmElement element) {
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
        public void add(@NonNull OsmElement element, boolean inCurrentStorage, boolean inApiStorage) {
            if (elements.containsKey(element)) {
                return;
            }
            OsmElement key = currentStorage.getOsmElement(element.getName(), element.getOsmId());
            if (element instanceof Node) {
                elements.put(key, new UndoNode((Node) element, inCurrentStorage, inApiStorage));
            } else if (element instanceof Way) {
                elements.put(key, new UndoWay((Way) element, inCurrentStorage, inApiStorage));
            } else if (element instanceof Relation) {
                elements.put(key, new UndoRelation((Relation) element, inCurrentStorage, inApiStorage));
            } else {
                throw new IllegalArgumentException("Unsupported element type");
            }
        }

        /**
         * Remove the saved state for the element from this checkpoint
         * 
         * @param element the element for which remove the saved state
         */
        public void remove(@NonNull OsmElement element) {
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
        public boolean restore(@Nullable Checkpoint redoCheckpoint) {
            boolean ok = true;
            List<UndoElement> list = new ArrayList<>(elements.values());
            if (redoCheckpoint != null) {
                for (UndoElement ue : list) {
                    redoCheckpoint.add(ue.element); // save current state
                }
            }
            // we sort according to element type and relation membership so that
            // all member elements should be restored before their parents
            Collections.sort(list, elementOrder);
            for (UndoElement ue : list) {
                ok = ok && ue.restore();
            }
            return ok;
        }

        /**
         * @return true if no elements have yet been stored in this checkpoint
         */
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        /**
         * Get the name of this checkpoint
         * 
         * @return the name of the Checkpoint
         */
        public String getName() {
            return name;
        }

        /**
         * Set the name of the checkpoint
         * 
         * @param name the name to set
         */
        public void setName(@NonNull String name) {
            this.name = name;
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
    public abstract class UndoElement implements Serializable {
        private static final long serialVersionUID = 1L;

        final OsmElement element;

        private final long                    osmId;
        private final long                    osmVersion;
        private final byte                    state;
        private final TreeMap<String, String> tags;

        private final boolean inCurrentStorage;
        private final boolean inApiStorage;

        private final ArrayList<Relation> parentRelations;

        /**
         * Create a new undo object
         * 
         * @param originalElement the OsmElement we want to save
         * @param inCurrentStorage true if the element is in the current storage
         * @param inApiStorage true if the element is in the api storage
         */
        public UndoElement(@NonNull OsmElement originalElement, boolean inCurrentStorage, boolean inApiStorage) {
            this.inCurrentStorage = inCurrentStorage;
            this.inApiStorage = inApiStorage;

            element = originalElement;

            osmId = originalElement.osmId;
            osmVersion = originalElement.osmVersion;
            state = originalElement.state;
            tags = originalElement.tags == null ? new TreeMap<>() : new TreeMap<>(originalElement.tags);

            if (originalElement.parentRelations != null) {
                parentRelations = new ArrayList<>(originalElement.parentRelations);
            } else {
                parentRelations = null;
            }
        }

        /**
         * Restores the saved state of the element
         * 
         * @return true if the restore was successful
         */
        public boolean restore() {
            // Restore element existence
            Log.e(DEBUG_TAG, "restoring " + element.getDescription() + " current " + inCurrentStorage + " api " + inApiStorage);
            try {
                if (inCurrentStorage) {
                    currentStorage.insertElementSafe(element);
                } else {
                    currentStorage.removeElement(element);
                }
                if (inApiStorage) {
                    apiStorage.insertElementSafe(element);
                } else {
                    apiStorage.removeElement(element);
                }
            } catch (StorageException e) {
                // TODO handle OOM
                Log.e(DEBUG_TAG, "restore got " + e.getMessage());
                return false;
            }

            // restore saved values
            element.osmId = osmId;
            element.osmVersion = osmVersion;
            element.state = state;
            element.setTags(tags);

            // zap error state
            element.resetHasProblem();

            if (parentRelations != null) {
                element.parentRelations = new ArrayList<>();
                for (Relation r : parentRelations) {
                    if (currentStorage.contains(r)) {
                        element.parentRelations.add(r);
                    } else {
                        Log.e(DEBUG_TAG, element.getDescription() + " is a member of " + r.getDescription() + " which is missing");
                    }
                }
            } else {
                element.parentRelations = null;
            }
            return true;
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
                if (name != null && name.length() > 0) {
                    return name;
                }
                // Then the house number
                String housenb = tags.get(Tags.KEY_ADDR_HOUSENUMBER);
                if (housenb != null && housenb.length() > 0) {
                    return "house " + housenb;
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

        /**
         * Get the Map containing the tags
         * 
         * @return an unmodifiable Map containing the key-value pairs
         */
        public Map<String, String> getTags() {
            return Collections.unmodifiableMap(tags);
        }

        /**
         * Get the list of parent relations
         * 
         * @return an unmodifiable List of the relations or null
         */
        @Nullable
        public List<Relation> getParentRelations() {
            return parentRelations != null ? Collections.unmodifiableList(parentRelations) : null;
        }

        /**
         * Get the id for the underlying OsmElement
         * 
         * @return the id
         */
        public long getOsmId() {
            return osmId;
        }
    }

    /**
     * Stores a past state of a node
     * 
     * @see UndoElement
     */
    public class UndoNode extends UndoElement implements Serializable {
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
        public boolean restore() {
            boolean ok = super.restore();
            ((Node) element).lat = lat;
            ((Node) element).lon = getLon();
            return ok;
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
    }

    /**
     * Stores a past state of a way
     * 
     * @see UndoElement
     */
    public class UndoWay extends UndoElement implements Serializable {
        private static final long     serialVersionUID = 1L;
        private final ArrayList<Node> nodes;

        /**
         * Create a new undo object
         * 
         * @param originalWay the Way we want to save
         * @param inCurrentStorage if true the elements should be restored to the current storage
         * @param inApiStorage if true the elements should be restored to the api storage
         */
        public UndoWay(@NonNull Way originalWay, boolean inCurrentStorage, boolean inApiStorage) {
            super(originalWay, inCurrentStorage, inApiStorage);
            nodes = new ArrayList<>(originalWay.nodes);
        }

        @Override
        public boolean restore() {
            boolean ok = super.restore();
            ((Way) element).nodes.clear();
            for (Node n : nodes) {
                if (currentStorage.contains(n)) {
                    ((Way) element).nodes.add(n); // only add undeleted way nodes
                } else {
                    Log.e(DEBUG_TAG, n.getDescription() + " member of " + element.getDescription() + " is missing");
                    ok = false;
                    element.setState(OsmElement.STATE_MODIFIED);
                    try {
                        apiStorage.insertElementSafe(element);
                    } catch (StorageException e) {
                        // TODO Handle OOM
                    }
                }
            }
            // reset the style
            ((Way) element).setFeatureProfile(null);
            ((Way) element).invalidateBoundingBox();
            return ok;
        }

        /*
         * The following methods provide equivalent of the same ones in Way
         */
        /**
         * return true if first == last node, will not work for broken geometries
         * 
         * @return true if closed
         */
        public boolean isClosed() {
            return nodes.get(0).equals(nodes.get(nodes.size() - 1));
        }

        /**
         * Return the length in m
         * 
         * This uses the Haversine distance between nodes for calculation
         * 
         * @return the length in m
         */
        public double length() {
            return Way.length(nodes);
        }

        /**
         * Return the number of nodes in the is way
         * 
         * @return the number of nodes in this Way
         */
        public int nodeCount() {
            return nodes == null ? 0 : nodes.size();
        }
    }

    /**
     * Stores a past state of a relation
     * 
     * @see UndoElement
     */
    public class UndoRelation extends UndoElement implements Serializable {
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
            for (RelationMember member : originalRelation.members) {
                members.add(new RelationMember(member));
            }
        }

        @Override
        public boolean restore() {
            boolean ok = super.restore();
            ((Relation) element).members.clear();
            ((Relation) element).members.addAll(members);
            for (RelationMember rm : members) {
                OsmElement rmElement = rm.getElement();
                if (rmElement == null || currentStorage.contains(rmElement)) {
                    ((Relation) element).members.add(rm); // only add undeleted members or ones that haven't been
                                                          // downloaded
                } else {
                    Log.e(DEBUG_TAG, rmElement.getDescription() + " member of " + element.getDescription() + " is deleted");
                    ok = false;
                    element.setState(OsmElement.STATE_MODIFIED);
                    try {
                        apiStorage.insertElementSafe(element);
                    } catch (StorageException e) {
                        // TODO Handle OOM
                    }
                }
            }
            return ok;
        }

        /**
         * Get the list of RelationMembers for the UndoRelation
         * 
         * @return an unmodifiable copy of the List of RelationMembers
         */
        public List<RelationMember> getMembers() {
            return Collections.unmodifiableList(members);
        }

        /**
         * Get the list of all RelationMember objects for the specified OsmElement
         * 
         * @param e the OsmElement
         * @return a List of RelationMembers
         */
        public List<RelationMember> getAllMembers(OsmElement e) {
            List<RelationMember> result = new ArrayList<>();
            for (int i = 0; i < members.size(); i++) {
                RelationMember member = members.get(i);
                if (member.getElement() == e) {
                    result.add(member);
                }
            }
            return result;
        }
    }

    /**
     * Provides a list of names for the actions that can be undone
     * 
     * @param ctx Android context
     * @return a list of names, oldest action first (i.e. the last action will be the first to be undone)
     */
    public String[] getUndoActions(@Nullable Context ctx) {
        return getCheckpointActions(ctx, undoCheckpoints);
    }

    /**
     * Provides a list of names for the actions that can be redone
     * 
     * @param ctx Android context
     * @return a list of names, newest action first (i.e. the last action will be the first to be redone)
     */
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
    private String[] getCheckpointActions(@Nullable Context ctx, List<Checkpoint> checkpoints) {
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
        UndoElement result = null;
        int checkpointCount = undoCheckpoints.size();
        // loop over most recent to oldest checkpoint
        for (int i = checkpointCount - 1; i >= 0; i--) {
            UndoElement undoElement = undoCheckpoints.get(i).elements.get(element);
            if (undoElement != null) {
                result = undoElement;
            }
        }
        return result;
    }
}
