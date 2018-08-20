package de.blau.android.presets;

import de.blau.android.presets.Preset.PresetElement;

public interface PresetElementHandler {
    /**
     * Do something with elements
     * 
     * @param element the PresetELenet to operate on
     */
    void handle(PresetElement element);
}
