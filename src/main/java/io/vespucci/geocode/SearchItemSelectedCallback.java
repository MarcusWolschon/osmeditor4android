package io.vespucci.geocode;

import androidx.annotation.NonNull;
import io.vespucci.geocode.Search.SearchResult;

/**
 * Provide interface for SearchResult selection
 * 
 * @author Simon Poole
 *
 */
public interface SearchItemSelectedCallback {

    /**
     * Called when a result has been selected
     * 
     * @param sr the SearchResult
     */
    void onItemSelected(@NonNull SearchResult sr);
}
