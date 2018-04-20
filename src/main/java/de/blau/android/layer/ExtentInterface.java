package de.blau.android.layer;

import android.support.annotation.Nullable;
import de.blau.android.osm.BoundingBox;

public interface ExtentInterface {
    /**
     * Get the extent of the layer
     * 
     * @return a BoundingBox with the extent or null if it cannot be determined
     */
    @Nullable
    BoundingBox getExtent();
}
