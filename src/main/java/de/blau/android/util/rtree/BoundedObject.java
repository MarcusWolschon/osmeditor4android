package de.blau.android.util.rtree;

import androidx.annotation.NonNull;
import de.blau.android.osm.BoundingBox;

/**
 * An object bounded on an Axis-Aligned Bounding Box.
 * 
 * @author Colonel32
 * @author cnvandev
 */
public interface BoundedObject {

    /**
     * Get the bounding box for this object
     *
     * @return the BoundingBox
     */
    @NonNull
    BoundingBox getBounds();
}