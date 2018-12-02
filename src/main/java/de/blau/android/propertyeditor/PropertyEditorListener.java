package de.blau.android.propertyeditor;

import java.util.LinkedHashMap;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset;

/**
 * Interface for communicating with the PropertyEditor activity
 */
interface PropertyEditorListener {

    /**
     * Check if we are actually visible to the user
     * 
     * @param me the calling Fragment
     * @return true if shown
     */
    boolean onTop(Fragment me);

    /**
     * Are we connected to a network?
     * 
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Are we connected or connecting to a network?
     * 
     * @return true if connected or in the process of connecting
     */
    boolean isConnectedOrConnecting();

    /**
     * Get a List of ISO country codes for the current object
     * 
     * @return a List of ISO country code Strings or null if none found
     */
    @Nullable
    List<String> getIsoCodes();

    /**
     * Get the current OsmElement
     * 
     * @return an OsmELement
     */
    @NonNull
    OsmElement getElement();

    /**
     * Get the current set of Presets
     * 
     * @return an array of Preset or null
     */
    @Nullable
    Preset[] getPresets();

    /**
     * Get current contents of editor for saving
     * 
     * @return list containing the tag maps or null if something went wrong
     */
    @Nullable
    public List<LinkedHashMap<String, String>> getUpdatedTags();
}
