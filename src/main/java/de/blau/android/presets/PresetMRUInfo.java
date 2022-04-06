package de.blau.android.presets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.Files;

/**
 * Serializable class for storing Most Recently Used information. Hash is used to check compatibility.
 */
public class PresetMRUInfo implements Serializable {
    private static final long serialVersionUID = 7708132207266548492L;

    private static final int MAX_MRU_SIZE = 50;

    private static final String DEBUG_TAG = null;

    /** hash of current preset (used to check validity of recentPresets indexes) */
    final String presetHash;

    /** indexes of recently used presets (for use with allItems) */
    private LinkedList<PresetElementPath> recentPresets = new LinkedList<>();

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
     * @param region country to filter on
     * 
     */
    public void putRecentlyUsed(@NonNull PresetItem item, @Nullable String region) {
        final PresetGroup rootGroup = item.getPreset().getRootGroup();
        PresetElementPath path = item.getPath(rootGroup);
        // prevent duplicates and preset is not in the list, add linked presets first
        if (!recentPresets.remove(path) && item.getLinkedPresetItems() != null) {
            LinkedList<PresetItemLink> linkedPresets = new LinkedList<>(item.getLinkedPresetItems());
            Collections.reverse(linkedPresets);
            Preset preset = item.getPreset();
            for (PresetItemLink pl : linkedPresets) {
                PresetItem linkedItem = preset.getItemByName(pl.getPresetName(), region);
                if (linkedItem != null) { // null if the link wasn't found
                    PresetElementPath linkedPath = linkedItem.getPath(rootGroup);
                    if (!recentPresets.contains(linkedPath)) { // only add if not already present
                        recentPresets.addFirst(linkedPath);
                        if (recentPresets.size() > MAX_MRU_SIZE) {
                            recentPresets.removeLast();
                        }
                    }
                } else {
                    Log.e(DEBUG_TAG, "linked preset not found for " + pl.getPresetName() + " in preset " + item.getName());
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
     * Add the contents to a PresetGroup
     * 
     * @param group the PresetGroup
     * @param p the parent Preset
     * @param region the current region if any
     */
    public void addToPresetGroup(@NonNull PresetGroup group, @NonNull Preset p, @Nullable String region) {
        for (PresetElementPath path : recentPresets) {
            final PresetItem item = (PresetItem) Preset.getElementByPath(p.getRootGroup(), path, region);
            if (item != null) {
                group.addElement(item, false);
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