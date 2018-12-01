package de.blau.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.filter.Filter;
import de.blau.android.filter.PresetFilter;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;

/**
 * ModeConfig for modes that allow filters to be set
 */
public class FilterModeConfig implements ModeConfig {

    @Override
    public void setup(Main main, Logic logic) {
        // empty stub
    }

    @Override
    public void teardown(Main main, Logic logic) {
        // empty stub
    }

    @Override
    public HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
        return null;
    }

    @Override
    @Nullable
    public ArrayList<PresetElementPath> getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
        // if we have a PresetFilter set for a single PresetItem, apply that automatically
        Logic logic = App.getLogic();
        Filter filter = logic.getFilter();
        if (filter instanceof PresetFilter) {
            PresetElement presetElement = ((PresetFilter) filter).getPresetElement();
            if (presetElement instanceof PresetItem || presetElement instanceof PresetGroup) {
                Preset preset = App.getCurrentRootPreset(ctx);
                ArrayList<PresetElementPath> result = new ArrayList<>();
                result.add(presetElement.getPath(preset.getRootGroup()));
                return result;
            }
        }
        return null;
    }
}
