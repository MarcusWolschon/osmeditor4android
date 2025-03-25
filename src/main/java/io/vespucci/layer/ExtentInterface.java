package io.vespucci.layer;

import androidx.annotation.Nullable;
import io.vespucci.osm.BoundingBox;

public interface ExtentInterface {
    /**
     * Get the extent of the layer
     * 
     * @return a BoundingBox with the extent or null if it cannot be determined
     */
    @Nullable
    BoundingBox getExtent();
}
