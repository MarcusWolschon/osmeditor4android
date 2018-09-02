package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;

public class AutoPresetItem extends PresetItem implements Comparable<AutoPresetItem> {

    private static final long serialVersionUID = 1L;

    private final int count; // times used in osm data

    /**
     * Construct a new AutoPresetItem
     * 
     * @param preset the preset this is created in
     * @param parent parent group (or null if this is the root group)
     * @param name name of the element or null
     * @param iconpath the icon path (either "http://" URL or "presets/" local image reference) or null
     * @param types comma separated list of types of OSM elements this applies to or null for all
     * @param count the count returned by a taginfo query or similar
     */
    public AutoPresetItem(@NonNull Preset preset, @Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath, @Nullable String types, int count) {
        preset.super(parent, name, iconpath, types);
        this.count = count;
    }

    @Override
    public int compareTo(@NonNull AutoPresetItem o) {
        if (count > o.count) {
            return -1;
        } else if (count < o.count) {
            return 1;
        }
        return 0;
    }
}
