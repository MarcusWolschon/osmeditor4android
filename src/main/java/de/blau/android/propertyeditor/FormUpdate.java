package de.blau.android.propertyeditor;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
interface FormUpdate {
    /**
     * Fetch new tags etc
     */
    void tagsUpdated();
}
