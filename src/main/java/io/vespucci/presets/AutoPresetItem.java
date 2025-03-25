package io.vespucci.presets;

import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AutoPresetItem extends PresetItem {

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
    public AutoPresetItem(@NonNull Preset preset, @Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath, @Nullable String types,
            int count) {
        super(preset, parent, name, iconpath, types);
        this.count = count;
    }

    public static final Comparator<AutoPresetItem> COMPARATOR = (AutoPresetItem o1, AutoPresetItem o2) -> {
        if (o1.count > o2.count) {
            return -1;
        } else if (o1.count < o2.count) {
            return 1;
        }
        return 0;
    };
}
