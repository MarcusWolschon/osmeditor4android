package io.vespucci.osm;

import androidx.annotation.NonNull;

/**
 * Callback for operations after an element has been merged into our in memory storage
 * 
 * @author Simon Poole
 *
 */
public interface PostMergeHandler {

    /**
     * Call after data has been merged
     * 
     * @param e an OsmElement
     */
    public void handler(@NonNull OsmElement e);
}
