package de.blau.android.geocode;

import de.blau.android.geocode.Search.SearchResult;

public interface SearchItemSelectedCallback {
    /**
     * Provide interface for SearchResult selection
     * 
     * @param sr the SearchReult
     */
    void onItemSelected(SearchResult sr);
}
