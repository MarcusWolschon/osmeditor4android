package de.blau.android.osm;

import java.io.Serializable;
import java.util.List;

import androidx.annotation.NonNull;

public interface BoundingBoxCoverage extends Serializable {
    
    /**
     * Returns a list of bounding boxes covering this way with a buffer around it.
     * 
     * 
     * @param buffer the buffer distance in meters
     * @param minDimension minimum dimension in meters for each bounding box
     * @param maxDimension maximum dimension in meters for each bounding box
     * @return a List of BoundingBox objects covering the way with the specified buffer and dimension constraints, or an
     *         empty list if the way has fewer than 2 nodes.
     */
    @NonNull
    List<BoundingBox> getCoverage(double buffer, double minDimension, double maxDimension);
}
