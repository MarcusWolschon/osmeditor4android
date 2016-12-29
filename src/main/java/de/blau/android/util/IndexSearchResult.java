package de.blau.android.util;

import android.support.annotation.NonNull;

import de.blau.android.presets.Preset.PresetItem;

/**
 * Container to allow sorting of preset search results
 * @author simon
 *
 */
class IndexSearchResult implements Comparable<IndexSearchResult>{
	int count = 0;
	PresetItem item = null;

	@Override
	public int compareTo(@NonNull IndexSearchResult arg0) {
		if (arg0.count > count) {
			return -1;
		} else if (arg0.count < count) {
			return +1;
		}
		return 0; 
	}
}
