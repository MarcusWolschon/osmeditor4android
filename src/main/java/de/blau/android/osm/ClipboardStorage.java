package de.blau.android.osm;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.exception.StorageException;

public class ClipboardStorage implements Serializable {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ClipboardStorage.class.getSimpleName().length());
    private static final String DEBUG_TAG = ClipboardStorage.class.getSimpleName().substring(0, TAG_LEN);

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
     * Copy a List of OsmELement to the clipboard assumes the elements have already been cloned
     * 
     * Note no need to store way nodes separate from ways
     * 
     * @param elements a List of OsmElement
     * @param latE7 the latitude in WGS84*1E7 coordinates
     * @param lonE7 the longitude in WGS84*1E7 coordinates
     */
    public void copyTo(@NonNull List<OsmElement> elements, int latE7, int lonE7) {
        reset();
        selectionLat = latE7;
        selectionLon = lonE7;
        mode = Mode.COPY;
        try {
            for (OsmElement e : elements) {
                storage.insertElementUnsafe(e);
            }
        } catch (StorageException sex) {
            // TODO handle oom situation
            Log.d(DEBUG_TAG, "copyTo got exception " + sex.getMessage());
        }
    }

    /**
     * Cut a List of OsmELement to the clipboard assumes that element will be deleted and any necessary objects cloned
     * 
     * @param elements a List of OsmElement
     * @param latE7 the latitude in WGS84*1E7 coordinates
     * @param lonE7 the longitude in WGS84*1E7 coordinates
     */
    public void cutTo(@NonNull List<OsmElement> elements, int latE7, int lonE7) {
        copyTo(elements, latE7, lonE7);
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
     * @return the stored OsmElements or null if there is none
     */
    @NonNull
    public List<OsmElement> pasteFrom() {
        List<Way> ways = storage.getWays();
        List<Node> nodes = storage.getNodes();
        List<Relation> relations = storage.getRelations();
        List<OsmElement> result = new ArrayList<>();
        if (mode == Mode.CUT) {
            reset(); // can only paste a cut way once
        }
        if (nodes != null) {
            for (Node n : nodes) {
                result.add(n);
            }
        }
        if (ways != null) {
            for (Way w : ways) {
                result.add(w);
            }
        }
        if (relations != null) {
            for (Relation r : relations) {
                result.add(r);
            }
        }
        return result;
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

    /**
     * Check if we have a specific element
     * 
     * @param e the OsmElement we are interested in
     * @return true if it is in the clipboard
     */
    boolean contains(@NonNull OsmElement e) {
        return storage.contains(e);
    }

    /**
     * Check that after an undo or similar event the contents can still be pasted
     * 
     * @param delegator the current StorageDelegator
     * @return true if everything is consistent
     */
    public boolean check(@NonNull StorageDelegator delegator) {
        if (contentsWasCut()) {
            final Storage currentStorage = delegator.getCurrentStorage();
            final Storage apiStorage = delegator.getApiStorage();
            for (OsmElement e : storage.getElements()) {
                if (currentStorage.contains(e) || !apiStorage.contains(e)) {
                    return false;
                }
            }
        }
        return true;
    }
}
