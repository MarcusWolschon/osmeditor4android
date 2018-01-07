package de.blau.android.propertyeditor;

import android.support.v4.app.Fragment;

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
}
