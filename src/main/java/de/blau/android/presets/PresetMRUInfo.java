package de.blau.android.presets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.Files;

/**
 * Serializable class for storing Most Recently Used information. Hash is used to check compatibility.
 */
public class PresetMRUInfo implements Serializable {
    private static final long serialVersionUID = 7708132207266548493L;

    private static final int MAX_MRU_SIZE = 50;

    private static final String DEBUG_TAG = PresetMRUInfo.class.getSimpleName().substring(0, Math.min(23, PresetMRUInfo.class.getSimpleName().length()));

    /** hash of current preset (used to check validity of recentPresets indexes) */
    final String presetHash;

    /** indexes of recently used presets (for use with allItems) */
    private LinkedList<TimestampedPresetElementPath> recentPresets = new LinkedList<>();

    private volatile boolean changed = false;

    /**
     * Construct a new instance
     * 
     * @param presetHash a hash for the Preset contents
     */
    PresetMRUInfo(String presetHash) {
        this.presetHash = presetHash;
    }

    /**
     * @return true if the MRU has been change
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * Mark the MRU as changed
     */
    public void setChanged() {
        this.changed = true;
    }

    /**
     * Add a preset to the front of the MRU list (removing old duplicates and limiting the list if needed)
     * 
     * @param item the item to add
     * @param regions list of regions to filter on
     * 
     */
    public void putRecentlyUsed(@NonNull PresetItem item, @Nullable List<String> regions) {
        final PresetGroup rootGroup = item.getPreset().getRootGroup();
        TimestampedPresetElementPath path = new TimestampedPresetElementPath(item.getPath(rootGroup));
        // prevent duplicates and preset is not in the list, add linked presets first
        if (!recentPresets.remove(path) && item.getLinkedPresetItems() != null) {
            LinkedList<PresetItemLink> linkedPresets = new LinkedList<>(item.getLinkedPresetItems());
            Collections.reverse(linkedPresets);
            Preset preset = item.getPreset();
            for (PresetItemLink pl : linkedPresets) {
                PresetItem linkedItem = preset.getItemByName(pl.getPresetName(), regions);
                if (linkedItem == null) { // null if the link wasn't found
                    Log.e(DEBUG_TAG, "linked preset not found for " + pl.getPresetName() + " in preset " + item.getName());
                    continue;
                }
                TimestampedPresetElementPath linkedPath = new TimestampedPresetElementPath(linkedItem.getPath(rootGroup));
                if (!recentPresets.contains(linkedPath)) { // only add if not already present
                    recentPresets.addFirst(linkedPath);
                    if (recentPresets.size() > MAX_MRU_SIZE) {
                        recentPresets.removeLast();
                    }
                }
            }
        }
        recentPresets.addFirst(path);
        if (recentPresets.size() > MAX_MRU_SIZE) {
            recentPresets.removeLast();
        }
        setChanged();
    }

    /**
     * Remove a preset
     * 
     * @param item the item to remove
     */
    public void removeRecentlyUsed(@NonNull PresetItem item) {
        // prevent duplicates
        recentPresets.remove(item.getPath(item.getPreset().getRootGroup()));
        setChanged();
    }

    /**
     * Reset the MRU list
     *
     * @param directory the directory the preset data is in
     */
    public void resetRecentlyUsed(@NonNull File directory) {
        recentPresets = new LinkedList<>();
        setChanged();
        saveMRU(directory);
    }

    /**
     * Check if this is empty
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return recentPresets == null || recentPresets.isEmpty();
    }

    /**
     * Add MRUs to a PresetGroup sorting the paths by the timestamp
     * 
     * This is slightly complex because PresetELementPath is relative to the presets root group
     * 
     * @param group the PresetGroup
     * @param presets the Presets with the MRUs
     * @param regions the list of current regions if any
     */
    public static void addToPresetGroup(@NonNull PresetGroup group, @NonNull Preset[] presets, @Nullable List<String> regions) {
        Map<TimestampedPresetElementPath, Preset> parent = new HashMap<>();
        List<TimestampedPresetElementPath> paths = new ArrayList<>();
        for (Preset p : presets) {
            if (p != null && p.hasMRU()) {
                for (TimestampedPresetElementPath path : p.getMru().recentPresets) {
                    paths.add(path);
                    parent.put(path, p);
                }
            }
        }
        Collections.sort(paths, (TimestampedPresetElementPath p1, TimestampedPresetElementPath p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));
        for (PresetElementPath path : paths) {
            final PresetElement element = Preset.getElementByPath(parent.get(path).getRootGroup(), path, regions, false);
            if (element instanceof PresetItem) {
                group.addElement(element, false);
            } else {
                Log.e(DEBUG_TAG, "Unexpected element for " + path);
            }
        }
    }

    /**
     * Saves the current MRU data to a file
     * 
     * @param directory the directory the preset data is in
     */
    public void saveMRU(@NonNull File directory) {
        if (isChanged()) {
            try (FileOutputStream fout = new FileOutputStream(new File(directory, Files.FILE_NAME_MRUFILE));
                    ObjectOutputStream out = new ObjectOutputStream(fout)) {
                out.writeObject(this);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "MRU saving failed", e);
            }
        }
    }

    /**
     * Initializes Most-recently-used data by either loading them or creating an empty list
     * 
     * @param directory data directory of the preset
     * @param hashValue XML hash value to check if stored data fits the XML
     * @return a MRU object valid for this Preset, never null
     */
    public static PresetMRUInfo getMRU(@NonNull File directory, String hashValue) {
        PresetMRUInfo tmpMRU;

        try (FileInputStream fout = new FileInputStream(new File(directory, Files.FILE_NAME_MRUFILE));
                ObjectInputStream mruReader = new ObjectInputStream(fout)) {
            tmpMRU = (PresetMRUInfo) mruReader.readObject();
            if (!tmpMRU.presetHash.equals(hashValue)) {
                throw new InvalidObjectException("hash mismatch");
            }
        } catch (Exception e) {
            tmpMRU = new PresetMRUInfo(hashValue);
            // Deserialization failed for whatever reason (missing file, wrong version, ...) - use empty list
            Log.i(DEBUG_TAG, "No usable old MRU list, creating new one (" + e.toString() + ")");
        }
        return tmpMRU;
    }
}