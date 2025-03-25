package io.vespucci.util.rtree;

import androidx.annotation.NonNull;
import io.vespucci.osm.BoundingBox;

/**
 * An object bounded on an Axis-Aligned Bounding Box.
 * 
 * @author Colonel32
 * @author cnvandev
 * @author Simon Poole
 */
public interface BoundedObject {

    /**
     * Get the bounding box for this object
     *
     * @return the BoundingBox
     */
    @NonNull
    BoundingBox getBounds();

    /**
     * Get the bounding box for this object
     *
     * @param result pre-allocated BoundingBox
     * @return the BoundingBox
     */
    @NonNull
    default BoundingBox getBounds(@NonNull BoundingBox result) {
        result.set(getBounds());
        return result;
    }
}