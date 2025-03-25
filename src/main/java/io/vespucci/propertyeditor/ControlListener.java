package io.vespucci.propertyeditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.vespucci.osm.OsmElement;

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
    void finished(@Nullable Fragment finishedFragment);

    /**
     * Add a PropertyEditor for the element
     * 
     * @param element the OsmElement
     */
    void addPropertyEditor(@NonNull OsmElement element);
}
