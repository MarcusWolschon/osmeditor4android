package de.blau.android.util;

import java.util.List;

import android.support.annotation.NonNull;
import de.blau.android.osm.BoundingBox;

public interface DataStorage {

    /**
     * Retrieve all bounding boxes from storage
     * 
     * @return list of BoundingBoxes
     */
    @NonNull
    List<BoundingBox> getBoundingBoxes();

    /**
     * Add bounding box to storage
     * 
     * 
     * @param box BoundingBox to add
     */
    void addBoundingBox(@NonNull BoundingBox box);
}
