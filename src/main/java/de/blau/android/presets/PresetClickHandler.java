package de.blau.android.presets;

import androidx.annotation.NonNull;

/** Interface for handlers handling clicks on item or group icons */
public interface PresetClickHandler {

    /**
     * Called for a normal click on a button showing a PresetItem
     * 
     * @param item the PresetItem
     */
    void onItemClick(@NonNull PresetItem item);

    /**
     * Called for a long click on a button showing a PresetItem
     * 
     * @param item the PresetItem
     * @return true if consumed
     */
    default boolean onItemLongClick(@NonNull PresetItem item) {
        return false;
    }

    /**
     * Called for a normal click on a button showing a PresetGroup
     * 
     * @param group the PresetGroup
     */
    default void onGroupClick(@NonNull PresetGroup group) {
        // do nothing
    }

    /**
     * Called for a long click on a button showing a PresetGroup
     * 
     * @param group the PresetGroup
     * @return true if consumed
     */
    default boolean onGroupLongClick(@NonNull PresetGroup group) {
        return false;
    }
}
