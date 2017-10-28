package de.blau.android.propertyeditor;

import android.support.v4.app.Fragment;

/**
 * Interface for communicating with the ProprtyLevel activity
 */
interface PropertyEditorListener {
	
    /**
     * Check if we are actually visible to the user
     * 
     * @param me    the calling Fragment
     * @return true if shown
     */
	boolean onTop(Fragment me);
}

