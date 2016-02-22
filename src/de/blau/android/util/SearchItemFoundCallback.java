package de.blau.android.util;

import java.io.Serializable;

import de.blau.android.util.Search.SearchResult;

public abstract class SearchItemFoundCallback implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	abstract public void onItemFound(SearchResult sr);
}

