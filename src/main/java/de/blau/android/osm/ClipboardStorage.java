package de.blau.android.osm;

import java.io.Serializable;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.exception.StorageException;

public class ClipboardStorage implements Serializable {
    static final String DEBUG_TAG = "ClipboardStorage";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public enum Mode {
        COPY, CUT
    }

    private Mode mode;

    private Storage storage;

    /*
     * save where object (really only necessary for ways) was selected
     */
    private int selectionLat;
    private int selectionLon;

    /**
     * Default constructor
     */
    ClipboardStorage() {
        storage = new Storage();
    }

    /**
     * Reset this instance completely removing any data
     */
    void reset() {
        storage = new Storage();
    }

    /**
     * Copy an OsmELement to the clipboard assumes the element has already been cloned note no need to store nodes
     * separate from ways
     * 
     * @param e the OsmElement
     * @param latE7 the latitude in WGS84*1E7 coordinates
     * @param lonE7 the longitude in WGS84*1E7 coordinates
     */
    public void copyTo(@NonNull OsmElement e, int latE7, int lonE7) {
        reset();
        selectionLat = latE7;
        selectionLon = lonE7;
        mode = Mode.COPY;

        try {
            storage.insertElementUnsafe(e);
        } catch (StorageException sex) {
            // TODO handle oom situation
            Log.d(DEBUG_TAG, "copyTo got exception " + sex.getMessage());
        }
    }

    /**
     * Cut an OsmELement to the clipboard assumes that element will be deleted and any necessary objects cloned
     * 
     * @param e the OsmElement
     * @param latE7 the latitude in WGS84*1E7 coordinates
     * @param lonE7 the longitude in WGS84*1E7 coordinates
     */
    public void cutTo(@NonNull OsmElement e, int latE7, int lonE7) {
        copyTo(e, latE7, lonE7);
        mode = Mode.CUT;
    }

    /**
     * Check if there is something in the clipboard
     * 
     * @return true if the clipboard is empty
     */
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    /**
     * Returns whatever is in the clipboard
     * 
     * @return the stored OsmElement or null if there is none
     */
    @Nullable
    public OsmElement pasteFrom() {
        List<Way> ways = storage.getWays();
        List<Node> nodes = storage.getNodes();
        if (mode == Mode.CUT) {
            reset(); // can only paste a cut way once
            if (ways != null && ways.size() == 1) {
                Way w = ways.get(0);
                return w;
            } else if (nodes != null && nodes.size() == 1) {
                Node n = nodes.get(0);
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

    /**
     * Get the original latitude of the element
     * 
     * @return the original latitude in WGS84*1E7 coordinates
     */
    public int getSelectionLat() {
        return selectionLat;
    }

    /**
     * Get the original longitude of the element
     * 
     * @return the original longitude in WGS84*1E7 coordinates
     */
    public int getSelectionLon() {
        return selectionLon;
    }

    /**
     * Check if the content was cut
     * 
     * @return true if the content was cut to the clipboard
     */
    public boolean contentsWasCut() {
        return mode == Mode.CUT;
    }
}
