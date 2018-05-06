package de.blau.android.presets;

import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;

public class AutoPresetItem extends PresetItem implements Comparable<AutoPresetItem> {

    private static final long serialVersionUID = 1L;

    private final int count; // times used in osm data

    public AutoPresetItem(Preset preset, PresetGroup parent, String name, String iconpath, String types, int count) {
        preset.super(parent, name, iconpath, types);
        this.count = count;
    }

    @Override
    public int compareTo(AutoPresetItem o) {
        if (count > o.count) {
            return -1;
        } else if (count < o.count) {
            return 1;
        }
        return 0;
    }
}
