package io.vespucci.osm;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.exception.StorageException;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.util.Density;

public class ClipboardStorage implements Serializable {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ClipboardStorage.class.getSimpleName().length());
    private static final String DEBUG_TAG = ClipboardStorage.class.getSimpleName().substring(0, TAG_LEN);

    private static final long serialVersionUID = 1L;

    public static final BitmapDrawable NO_ICON = new BitmapDrawable(); // NOSONAR
    private transient BitmapDrawable   icon    = null;

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
     * Cut a List of OsmElement to the clipboard assumes that element will be deleted and any necessary objects cloned
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
            mode = Mode.COPY; // can only paste the original element once
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

    /**
     * Get a suitable icon for the current clipboard
     * 
     * @param ctx an Android Context
     * @return a BitmapDrawable
     */
    @NonNull
    public BitmapDrawable getIcon(@NonNull Context ctx) {
        if (icon != null) {
            return icon;
        }
        int iconSize = Math.round(ctx.getResources().getDimension(R.dimen.button_preset_size));
        for (OsmElement e : storage.getElements()) {
            PresetItem item = Preset.findBestMatch(App.getCurrentPresets(ctx), e.getTags(), null, null);
            if (item != null) {
                Drawable tempIcon = item.getIcon(ctx, Density.dpToPx(ctx, iconSize));
                if (tempIcon instanceof BitmapDrawable) {
                    icon = (BitmapDrawable) tempIcon;
                    break;
                }
            }
        }
        if (icon == null) {
            icon = NO_ICON;
        }
        return icon;
    }

    /**
     * Set the selection coordinates, used after pasting a cut clipboard
     * 
     * @param lon the selection longitude in WGS84*1E7
     * @param lat the selection latitude in WGS84*1E7
     */
    public void setSelectionCoords(int lon, int lat) {
        selectionLon = lon;
        selectionLat = lat;
    }
}
