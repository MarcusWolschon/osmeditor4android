package de.blau.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
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
    public HashMap<String, String> getExtraTags(Logic logic, OsmElement e) {
        return null;
    }  

    @Nullable
    public ArrayList<PresetElementPath> getPresetItems(Context ctx, OsmElement e) {
        // if we have a PresetFilter set for a single PresetItem, apply that automatically
        Logic logic = App.getLogic();
        Filter filter = logic.getFilter();
        if (filter instanceof PresetFilter) {
            PresetElement presetElement = ((PresetFilter)filter).getPresetElement();
            if (presetElement instanceof PresetItem || presetElement instanceof PresetGroup) {
                Preset[] presets = App.getCurrentPresets(ctx);
                ArrayList<PresetElementPath>result = new ArrayList<PresetElementPath>();
                result.add(presetElement.getPath(presets[0].getRootGroup()));
                return result;
            }
        }
        return null;
    }
}
