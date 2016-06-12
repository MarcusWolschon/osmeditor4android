package de.blau.android.util;

import de.blau.android.presets.Preset.PresetItem;

class IndexSearchResult implements Comparable<IndexSearchResult>{
	int count = 0;
	PresetItem item = null;

	@Override
	public int compareTo(IndexSearchResult arg0) {
		if (arg0.count > count) {
			return -1;
		} else if (arg0.count < count) {
			return +1;
		}
		return -1; // don't return 0 even if count is the same
				   // as 0 implies equal in SortedSet
	}
}
