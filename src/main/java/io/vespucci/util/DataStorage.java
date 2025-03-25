package io.vespucci.util;

import java.util.List;

import androidx.annotation.NonNull;
import io.vespucci.osm.BoundingBox;

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

    /**
     * Delete a BoundingBox from the List of BoundingBoxes
     * 
     * @param box the BoundingBox to delete
     */
    void deleteBoundingBox(@NonNull BoundingBox box);

    /**
     * Safely remove data that is not in/intersects with the provided BoundingBox
     * 
     * @param box the BoundingBox
     */
    void prune(@NonNull BoundingBox box);

    /**
     * Check if we need to prune
     * 
     * @param dataLimit number of data items that shouldn't be exceeded
     * @param boxLimit number of bounding boxes that shouldnn't be exceeded
     * @return
     */
    boolean reachedPruneLimits(int dataLimit, int boxLimit);
}
