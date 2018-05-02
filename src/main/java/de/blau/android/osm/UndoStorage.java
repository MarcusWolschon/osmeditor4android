package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = "UndoStorage";

    // Original storages for "contains" checks and restoration
    private Storage       currentStorage;
    private final Storage apiStorage;

    private final LinkedList<Checkpoint> undoCheckpoints = new LinkedList<>();
    private final LinkedList<Checkpoint> redoCheckpoints = new LinkedList<>();

    /**
     * Creates a new UndoStorage. You need to pass the storage objects to which changes are applied. Please ensure that
     * any time the {@link Logic} starts to use different objects, a new UndoStorage pointing to the correct objects is
     * created. Otherwise, undo will mess up your data badly.
     * 
     * @param currentStorage the currentStorage in use
     * @param apiStorage the apiStorage in use
     */
    public UndoStorage(Storage currentStorage, Storage apiStorage) {
        this.currentStorage = currentStorage;
        this.apiStorage = apiStorage;
    }

    /**
     * Set currentStorage without creating a new instance
     * 
     * @param currentStorage the current OsmElement storage
     */
    public void setCurrentStorage(Storage currentStorage) {
        this.currentStorage = currentStorage;
    }

    /**
     * Call to create a new checkpoint. When the user performs an undo operation, the state will be reverted to what it
     * was at the last checkpoint. Checkpoints should NOT be created for changes that are made as part of other
     * operations. For this reason, checkpoints usually need to be triggered in {@link Logic}, not
     * {@link StorageDelegator}.
     * 
     * @param name the name of the checkpoint, used for debugging and display purposes
     */
    public void createCheckpoint(String name) {
        Log.d("UndoStorage", "creating checkpoint " + name);
        if (undoCheckpoints.isEmpty() || !undoCheckpoints.getLast().isEmpty()) {
            undoCheckpoints.add(new Checkpoint(name));
            redoCheckpoints.clear();
        } else {
            // Empty checkpoint exists, just rename it
            Log.d("UndoStorage", "renaming checkpoint " + name);
            undoCheckpoints.getLast().setName(name);
        }

        while (undoCheckpoints.size() > 100) {
            undoCheckpoints.removeFirst();
        }
    }

    /**
     * remove checkpoint from list. typically called when we otherwise would have an empty checkpoint at the top
     * 
     * @param name checkpoint name
     */
    public void removeCheckpoint(String name) {
        removeCheckpoint(name, false);
    }

    /**
     * remove checkpoint from list. typically called when we otherwise would have an empty checkpoint at the top
     * 
     * @param name checkpoint name
     * @param force remove even if checkpoint is not empty
     */
    public void removeCheckpoint(String name, boolean force) {
        if (!undoCheckpoints.isEmpty() && (undoCheckpoints.getLast().isEmpty() || force) && undoCheckpoints.getLast().getName().equals(name))
            undoCheckpoints.removeLast();
    }

    /**
     * Saves the current state of the element in the checkpoint. Call before any changes to the element. A checkpoint
     * needs to be created first using {@link #createCheckpoint(String)}, otherwise an error is logged and the function
     * does nothing.
     * 
     * @param element the element to save
     */
    void save(OsmElement element) {
        try {
            if (undoCheckpoints.isEmpty()) {
                Log.e(DEBUG_TAG, "Attempted to save without valid checkpoint - forgot to call createCheckpoint()");
                return;
            }
            undoCheckpoints.getLast().add(element);
            redoCheckpoints.clear();
        } catch (Exception ex) {
            ACRAHelper.nocrashReport(ex, ex.getMessage());
        }
    }

    /**
     * Remove the saved state of this element from the last checkpoint
     * 
     * @param element eleent for which the state should be removed
     */
    void remove(OsmElement element) {
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
        if (!canUndo()) {
            Log.w(DEBUG_TAG, "Attempted to undo, but no undo checkpoints available");
            return null;
        }
        String name = undoCheckpoints.getLast().getName();
        Checkpoint redoPoint = new Checkpoint(name);
        undoCheckpoints.removeLast().restore(redoPoint);
        redoCheckpoints.add(redoPoint);
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
        undoCheckpoints.remove(checkpoint).restore(redoPoint);
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

        public Checkpoint(String name) {
            this.name = name;
        }

        /**
         * Store the current state of the element, unless a state is already stored. Called before any changes to the
         * element occur via {@link UndoStorage#save(OsmElement)}.
         * 
         * @param element the element to save
         */
        public void add(OsmElement element) throws IllegalArgumentException {
            if (elements.containsKey(element)) {
                return;
            }
            if (element instanceof Node) {
                elements.put(element, new UndoNode((Node) element));
            } else if (element instanceof Way) {
                elements.put(element, new UndoWay((Way) element));
            } else if (element instanceof Relation) {
                elements.put(element, new UndoRelation((Relation) element));
            } else {
                throw new IllegalArgumentException("Unsupported element type");
            }
        }

        /**
         * Remove the saved state for the element from this checkpoint
         * 
         * @param element the element for which remove the saved state
         */
        public void remove(OsmElement element) throws IllegalArgumentException {
            if (!elements.containsKey(element)) {
                return;
            }
            elements.remove(element);
        }

        /**
         * Restores the storages to the state at the time of the creation of this checkpoint.
         * 
         * @param redoCheckpoint optional - if given, the reverted elements are added to this checkpoint to make a
         *            "redo" feature possible
         */
        public void restore(Checkpoint redoCheckpoint) {
            for (Entry<OsmElement, UndoElement> entry : elements.entrySet()) {
                if (redoCheckpoint != null) {
                    redoCheckpoint.add(entry.getKey()); // save current state
                }
                entry.getValue().restore();
            }
        }

        /**
         * @return true if no elements have yet been stored in this checkpoint
         */
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
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

        public UndoElement(OsmElement originalElement) {
            element = originalElement;

            osmId = originalElement.osmId;
            osmVersion = originalElement.osmVersion;
            state = originalElement.state;
            tags = originalElement.tags == null ? new TreeMap<String, String>() : new TreeMap<>(originalElement.tags);

            inCurrentStorage = currentStorage.contains(originalElement);
            inApiStorage = apiStorage.contains(originalElement);

            if (originalElement.parentRelations != null) {
                parentRelations = new ArrayList<>(originalElement.parentRelations);
            } else {
                parentRelations = null;
            }
        }

        /**
         * Restores the saved state of the element
         */
        public void restore() {
            // Restore element existence
            try {
                if (inCurrentStorage)
                    currentStorage.insertElementSafe(element);
                else
                    currentStorage.removeElement(element);

                if (inApiStorage)
                    apiStorage.insertElementSafe(element);
                else
                    apiStorage.removeElement(element);
            } catch (StorageException e) {
                // TODO handle OOM
                Log.e(DEBUG_TAG, "restore got " + e.getMessage());
            }

            // restore saved values
            element.osmId = osmId;
            element.osmVersion = osmVersion;
            element.state = state;
            element.setTags(tags);

            if (parentRelations != null) {
                element.parentRelations = new ArrayList<>();
                for (Relation r: parentRelations) {
                    if (currentStorage.contains(r)) {
                        element.parentRelations.add(r);
                    } else {
                        Log.e(DEBUG_TAG, element.getDescription() + " is a member of " + r.getDescription() + " which is deleted");
                    }
                }
            } else {
                element.parentRelations = null;
            }
        }

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
                        for (String key : p.getObjectKeys()) {
                            result = getTagValueString(key);
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                }
            }
            // Failing the above, the OSM ID
            return element.getName() + " #" + Long.toString(element.getOsmId());
        }

        private String getTagValueString(String tag) {
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

        public UndoNode(Node originalNode) {
            super(originalNode);
            lat = originalNode.lat;
            lon = originalNode.lon;
        }

        @Override
        public void restore() {
            super.restore();
            ((Node) element).lat = lat;
            ((Node) element).lon = getLon();
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
            return lon;
        }
    }

    /**
     * Stores a past state of a way
     * 
     * @see UndoElement
     */
    public class UndoWay extends UndoElement implements Serializable {
        private static final long serialVersionUID = 1L;
        private ArrayList<Node>   nodes;

        public UndoWay(Way originalWay) {
            super(originalWay);
            nodes = new ArrayList<>(originalWay.nodes);
        }

        @Override
        public void restore() {
            super.restore();
            ((Way) element).nodes.clear();
            ((Way) element).nodes.addAll(nodes);
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
        private static final long    serialVersionUID = 1L;
        private List<RelationMember> members;

        public UndoRelation(Relation originalRelation) {
            super(originalRelation);
            members = new ArrayList<>(originalRelation.members);
        }

        @Override
        public void restore() {
            Log.d("Undo", "Restoring relation " + element.getDescription());
            super.restore();
            ((Relation) element).members.clear();
            ((Relation) element).members.addAll(members);
        }
        
        /**
         * Get the list of RelationMembers for the UndoRelation
         * 
         * @return an unmodifiable copy of the List of RelationMembers
         */
        public List<RelationMember> getMembers() {
           return Collections.unmodifiableList(members); 
        }
    }

    /**
     * Provides a list of names for the actions that can be undone
     * 
     * @param ctx Android context
     * @return a list of names, oldest action first (i.e. the last action will be the first to be undone)
     */
    public String[] getUndoActions(@Nullable Context ctx) {
        String[] result = new String[undoCheckpoints.size()];
        int i = 0;
        for (Checkpoint checkpoint : undoCheckpoints) {
            String message = checkpoint.getName() + "<br>";
            for (UndoElement u : checkpoint.elements.values()) {
                message = message + "<small>" + u.getDescription(ctx) + "</small><br>";
            }
            result[i++] = message;
        }
        return result;
    }

    /**
     * Provides a list of names for the actions that can be redone
     * 
     * @param ctx Android context
     * @return a list of names, newest action first (i.e. the last action will be the first to be redone)
     */
    public String[] getRedoActions(@Nullable Context ctx) {
        String[] result = new String[redoCheckpoints.size()];
        int i = 0;
        for (Checkpoint checkpoint : redoCheckpoints) {
            String message = checkpoint.getName() + "<br>";
            for (UndoElement u : checkpoint.elements.values()) {
                message = message + "<small>" + u.getDescription(ctx) + "</small><br>";
            }
            result[i++] = message;
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
