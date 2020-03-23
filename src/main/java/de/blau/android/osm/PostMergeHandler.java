package de.blau.android.osm;

import androidx.annotation.NonNull;

/**
 * Callback for operations after an element has been merged into our in memory storage
 * 
 * @author Simon Poole
 *
 */
public abstract class PostMergeHandler {

    /**
     * Call after data has been merged
     * 
     * @param e an OsmElement
     */
    public abstract void handler(@NonNull OsmElement e);
}
