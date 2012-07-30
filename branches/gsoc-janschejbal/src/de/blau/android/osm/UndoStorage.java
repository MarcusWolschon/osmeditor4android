package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import de.blau.android.Logic;

import android.util.Log;

/**
 * This class provides undo support.
 * It is absolutely critical that {@link StorageDelegator} calls {@link #save(OsmElement)} each and every time
 * something changes, as otherwise undo will create inconsistencies.
 * 
 * Checkpoints should be created at appropriate intervals, e.g. before each user action is performed, using
 * {@link #createCheckpoint(String)}.
 * 
 * The undo function works by storing the original state of each {@link OsmElement} before changes are performed
 * in each check point. As soon as a state is saved, any further changes within the same checkpoint will be
 * ignored, as the state at the beginning of the checkpoint is already stored.
 * 
 * On undo, the state is restored. This includes not only the values of the element, but also to its presence
 * in the currentStorage and apiStorage. For this reason, the state includes whether the element was in each
 * of the storages, and on undo, it will be added or deleted if necessary.
 * 
 * @author Jan Schejbal
 */
public class UndoStorage implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String TAG = "UndoStorage";
	
	// Original storages for "contains" checks and restoration
	private final Storage currentStorage;
	private final Storage apiStorage;
	
	private final LinkedList<Checkpoint> undoCheckpoints = new LinkedList<Checkpoint>();
	private final LinkedList<Checkpoint> redoCheckpoints = new LinkedList<Checkpoint>();

	/**
	 * Creates a new UndoStorage.
	 * You need to pass the storage objects to which changes are applied.
	 * Please ensure that any time the {@link Logic} starts to use different objects,
	 * a new UndoStorage pointing to the correct objects is created.
	 * Otherwise, undo will mess up your data badly.
	 *  
	 * @param currentStorage the currentStorage in use
	 * @param apiStorage the apiStorage in use
	 */
	public UndoStorage(Storage currentStorage, Storage apiStorage) {
		this.currentStorage = currentStorage;
		this.apiStorage = apiStorage;
	}
	
	/**
	 * Call to create a new checkpoint. When the user performs an undo operation, 
	 * the state will be reverted to what it was at the last checkpoint.
	 * Checkpoints should NOT be created checkpoints for changes that are made as part of other operations.
	 * For this reason, checkpoints usually need to be triggered in {@link Logic}, not {@link StorageDelegator}.
	 * @param name the name of the checkpoint, used for debugging and display purposes
	 */
	public void createCheckpoint(String name) {
		if (undoCheckpoints.isEmpty() || !undoCheckpoints.getLast().isEmpty()) {
			undoCheckpoints.add(new Checkpoint(name));
			redoCheckpoints.clear();
		} else {
			// Empty checkpoint exists, just rename it
			undoCheckpoints.getLast().name = name;
		}
		
		while (undoCheckpoints.size() > 100) {
			undoCheckpoints.removeFirst();
		}
	}
	
	/**
	 * Saves the current state of the element in the checkpoint. Call before any changes to the element.
	 * A checkpoint needs to be created first using {@link #createCheckpoint(String)}, 
	 * otherwise an error is logged and the function does nothing.
	 * @param element the element to save
	 */
	protected void save(OsmElement element) {
		if (undoCheckpoints.isEmpty()) {
			Log.e(TAG, "Attempted to save without valid checkpoint - forgot to call createCheckpoint()");
			return;
		}
		undoCheckpoints.getLast().add(element);
		redoCheckpoints.clear();
	}
	
	/**
	 * Performs an undo operation, restoring the state at the last undo checkpoint.
	 * A redo checkpoint is automatically created.
	 * If no checkpoint is available, an error is logged and the function does nothing.
	 * @return the name of the undo checkpoint used, or null if no checkpoint was available
	 */
	public String undo() {
		if (!canUndo()) {
			Log.e(TAG, "Attempted to undo, but no undo checkpoints available");
			return null;
		}
		String name = undoCheckpoints.getLast().name;
		Checkpoint redoPoint = new Checkpoint(name);
		undoCheckpoints.removeLast().restore(redoPoint);
		redoCheckpoints.add(redoPoint);
		return name;
	}
	
	/**
	 * Performs an redo operation, restoring the state at the next redo checkpoint.
	 * A new undo checkpoint is automatically created.
	 * If no checkpoint is available, an error is logged and the function does nothing.
	 * @return the name of the redo checkpoint used, or null if no checkpoint was available
	 */
	public String redo() {
		if (!canRedo()) {
			Log.e(TAG, "Attempted to redo, but no redo checkpoints available");
			return null;
		}
		String name = redoCheckpoints.getLast().name;
		Checkpoint reundoPoint = new Checkpoint(name);
		redoCheckpoints.removeLast().restore(reundoPoint);
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
	 * Clears the storage, deleting all checkpoints
	 */
	protected void clear() {
		undoCheckpoints.clear();
		redoCheckpoints.clear();
	}


	/**
	 * Represents an undo checkpoint to which the user can revert.
	 * Any time an element is <b>first</b> changed since the checkpoint was created,
	 * the original element state is saved.
	 * (This is ensured by calling {@link #add(OsmElement)} on each change - repeated changes are ignored.)
	 * 
	 * The checkpoint can later be restored using {@link #restore(Checkpoint)}.
	 */
	private class Checkpoint implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private final HashMap<OsmElement, UndoElement> elements = new HashMap<OsmElement, UndoElement>();
		public String name;
		
		public Checkpoint(String name) {
			this.name = name;
		}
		
		/**
		 * Store the current state of the element, unless a state is already stored.
		 * Called before any changes to the element occur via {@link UndoStorage#save(OsmElement)}.
		 * @param element the element to save
		 */
		public void add(OsmElement element) {
			if (elements.containsKey(element)) return;
			
			if (element instanceof Node) elements.put(element, new UndoNode((Node)element));
			else if (element instanceof Way) elements.put(element, new UndoWay((Way)element));
			else throw new IllegalArgumentException("Unsupported element type");
		}
		
		/**
		 * Restores the storages to the state at the time of the creation of this checkpoint.
		 * @param redoCheckpoint optional - if given, the reverted elements are added to this checkpoint
		 *        to make a "redo" feature possible 
		 */
		public void restore(Checkpoint redoCheckpoint) {
			for (Entry<OsmElement, UndoElement> entry : elements.entrySet()) {
				if (redoCheckpoint != null) redoCheckpoint.add(entry.getKey()); // save current state
				entry.getValue().restore();
			}
		}
		
		/**
		 * @return true if no elements have yet been stored in this checkpoint
		 */
		public boolean isEmpty()  {
			return elements.isEmpty();
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
	 * This class contains a past state of a {@link OsmElement}.
	 * It is stored in {@link Checkpoint}s and used to restore the state of the element on undo.
	 * The constructor saves the state, {@link #restore()} restores it.
	 * @author Jan
	 */
	private abstract class UndoElement implements Serializable {
		private static final long serialVersionUID = 1L;

		protected final OsmElement element;
		
		private final long osmId;
		private final long osmVersion;
		private final byte state;
		private final SortedMap<String, String> tags;
		
		private final boolean inCurrentStorage;
		private final boolean inApiStorage;

		public UndoElement(OsmElement originalElement) {
			this.element    = originalElement;
			
			this.osmId      = originalElement.osmId;
			this.osmVersion = originalElement.osmVersion;
			this.state      = originalElement.state;
			this.tags       = new TreeMap<String, String>(originalElement.tags);
			
			this.inCurrentStorage = currentStorage.contains(originalElement);
			this.inApiStorage     = apiStorage.contains(originalElement);
		}
		
		/**
		 * Restores the saved state of the element
		 */
		public void restore() {
			// Restore element existence
			if (inCurrentStorage) currentStorage.insertElementSafe(element);
			else currentStorage.removeElement(element);

			if (inApiStorage) apiStorage.insertElementSafe(element);
			else apiStorage.removeElement(element);
			
			// restore saved values
			element.osmId      = osmId;
			element.osmVersion = osmVersion;
			element.state      = state;
			element.setTags(tags);
			element.cachedHasProblemValid = false;
		}
	}
	
	/**
	 * Stores a past state of a node
	 * @see UndoElement
	 */
	private class UndoNode extends UndoElement implements Serializable {
		private static final long serialVersionUID = 1L;
		private final int lat;
		private final int lon;

		public UndoNode(Node originalNode) {
			super(originalNode);
			this.lat = originalNode.lat;
			this.lon = originalNode.lon;
		}
		
		public void restore() {
			super.restore();
			((Node)element).lat = lat;
			((Node)element).lon = lon;
		}
	}

	/**
	 * Stores a past state of a way
	 * @see UndoElement
	 */
	private class UndoWay extends UndoElement implements Serializable {
		private static final long serialVersionUID = 1L;
		private ArrayList<Node> nodes;

		public UndoWay(Way originalWay) {
			super(originalWay);
			this.nodes = new ArrayList<Node>(originalWay.nodes);
		}
		
		public void restore() {
			super.restore();
			((Way)element).nodes.clear();
			((Way)element).nodes.addAll(nodes);
		}
	}
}
