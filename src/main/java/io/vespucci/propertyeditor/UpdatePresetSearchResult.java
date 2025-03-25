package io.vespucci.propertyeditor;

import java.util.List;

import androidx.annotation.NonNull;
import io.vespucci.presets.PresetElement;

public interface UpdatePresetSearchResult {

    /**
     * Update the displayed list of preset elements
     * 
     * @param term the search term that led to the results
     * @param presets the List of PresetElement
     */
    void update(@NonNull String term, @NonNull List<PresetElement> presets);
}
