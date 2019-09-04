package de.blau.android.propertyeditor;

import java.util.List;

import android.support.annotation.NonNull;
import de.blau.android.presets.Preset.PresetElement;

public interface UpdatePresetSearchResult {

    /**
     * Update the displayed list of preset elements
     * 
     * @param term the search term that led to the results
     * @param presets the List of PresetElement
     */
    void update(@NonNull String term, @NonNull List<PresetElement> presets);
}
