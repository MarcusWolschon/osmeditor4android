package de.blau.android.propertyeditor;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
interface FormUpdate {
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
}
