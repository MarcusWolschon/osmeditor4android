package de.blau.android.osm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import android.util.Log;
import de.blau.android.exception.StorageException;

public class ClipboardStorage implements Serializable  {
    final static String DEBUG_TAG = "ClipboardStorage";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum Mode { COPY, CUT }

	private Mode mode;
	
	private Storage storage;
	
	/*
	 * save where object (really only necessary for ways) was selected
	 */
	private int selectionLat;
	private	int selectionLon;
	
	/**
	 * cut elements are deleted and have their state set appropriately, the original pre-cut state has to be restored on paste
	 */
	private byte savedState;
	private HashMap<Node,Byte> savedNdState;
	
	ClipboardStorage() {
		storage = new Storage();
	}
	
	private void reset() {
		storage = new Storage();
	}
	
	/**
	 * assumes the element has already been cloned
	 * note no need to store nodes separate from ways
	 * @param e
	 * @param lat
	 * @param lon
	 */
	public void copyTo(OsmElement e, int lat, int lon) {
		reset();
		selectionLat = lat;
		selectionLon = lon;
		mode = Mode.COPY;
		
		try {
			storage.insertElementUnsafe(e);
		} catch (StorageException sex) {
			// TODO handle oom situation
		    Log.d(DEBUG_TAG,sex.getMessage());
		}
	}
	
	/**
	 * assumes that element will be deleted and any necessary objects cloned
	 * @param e
	 * @param lat
	 * @param lon
	 */
	public void cutTo(OsmElement e, int lat, int lon) {
		copyTo(e, lat, lon);
		savedState = e.getState();
		if (e instanceof Way) {
			savedNdState = new HashMap<>();
			for (Node nd:((Way)e).getNodes()) {
				Log.d("CutTo","Saving state for " + nd.getOsmId());
				savedNdState.put(nd, nd.getState());
			}
		}
		mode = Mode.CUT;
	}
	
	public boolean isEmpty() {
		return storage.isEmpty();
	}
	
	/**
	 * returns whatever is in the clipboard
	 * @return
	 */
	public OsmElement pasteFrom() {
		List<Way> ways = storage.getWays();
		List<Node> nodes = storage.getNodes();
		if (mode == Mode.CUT) {
			reset(); // can only paste a cut way once
			if (ways != null && ways.size() == 1) { // restore original state
				Way w = ways.get(0);
				w.setState(savedState);
				for (Node nd:w.getNodes()) {
					Log.d("PasteFrom","Restoring state for " + nd.getOsmId());
					nd.setState(savedNdState.get(nd));
				}
				return w;
			} else if (nodes != null && nodes.size() == 1) {
				Node n = nodes.get(0);
				n.setState(savedState);
				return n;
			}
		} else {
			if (ways != null && ways.size() == 1) {
				return ways.get(0);
			} else if (nodes != null && nodes.size() == 1) {
				return nodes.get(0);
			}
		}
		return null;
	}
	
	public int getSelectionLat() {
		return selectionLat;
	}
	
	public int getSelectionLon() {
		return selectionLon;
	}
}
