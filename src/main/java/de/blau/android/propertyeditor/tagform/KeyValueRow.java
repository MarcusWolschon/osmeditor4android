package de.blau.android.propertyeditor.tagform;

public interface KeyValueRow {

    /**
     * Return the OSM key value
     * 
     * @return the key as a String
     */
    String getKey();
    
    /**
     * Get the current value
     * 
     * @return the current value as a String
     */
    String getValue();
}
