package de.blau.android.presets;

import androidx.annotation.NonNull;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;

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
    boolean onItemLongClick(@NonNull PresetItem item);

    /**
     * Called for a normal click on a button showing a PresetGroup
     * 
     * @param group the PresetGroup
     */
    void onGroupClick(@NonNull PresetGroup group);

    /**
     * Called for a long click on a button showing a PresetGroup
     * 
     * @param group the PresetGroup
     * @return true if consumed
     */
    boolean onGroupLongClick(@NonNull PresetGroup group);
}
