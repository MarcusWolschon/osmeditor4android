package io.vespucci.propertyeditor;

import androidx.annotation.NonNull;
import io.vespucci.presets.PresetItem;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
public interface FormUpdate {
    /**
     * Fetch new tags etc
     */
    void tagsUpdated();

    /**
     * Update TagEditor from text fields that may have not been saved yet
     * 
     * This is used internally by the TagFormFragment
     * 
     * @return true if there was something to save
     */
    boolean updateEditorFromText();

    /**
     * Store if we want to display the PresetItem in question with optional element
     * 
     * @param presetItem the PresetITem we are displaying
     * @param optional if true display optional fields too
     */
    void displayOptional(@NonNull PresetItem presetItem, boolean optional);
}
