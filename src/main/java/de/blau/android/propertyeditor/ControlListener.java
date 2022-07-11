package de.blau.android.propertyeditor;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Allow Fragments to tell their parent that they need to do something
 * 
 * @author Simon Poole
 *
 */
interface ControlListener {

    /**
     * Indicate that the Fragment is finished with whatever it was doing
     * 
     * @param finishedFragment the calling Fragment
     */
    void finished(@NonNull Fragment finishedFragment);
}
