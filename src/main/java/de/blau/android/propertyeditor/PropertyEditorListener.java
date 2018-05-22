package de.blau.android.propertyeditor;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import de.blau.android.osm.OsmElement;

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
     * Get the current OsmELement
     * 
     * @return an OsmELement
     */
    @NonNull 
    OsmElement getElement();
}
