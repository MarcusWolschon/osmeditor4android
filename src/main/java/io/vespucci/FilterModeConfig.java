package io.vespucci;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.filter.Filter;
import io.vespucci.filter.PresetFilter;
import io.vespucci.osm.OsmElement;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetElement;
import io.vespucci.presets.PresetElementPath;
import io.vespucci.presets.PresetGroup;
import io.vespucci.presets.PresetItem;

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
                PresetElementPath path = presetElement.getPath(preset.getRootGroup());
                if (path != null) {
                    result.add(path);
                    return result;
                }
            }
        }
        return null;
    }
}
