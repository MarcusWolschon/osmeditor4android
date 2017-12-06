package de.blau.android.util;

import android.support.annotation.NonNull;
import de.blau.android.presets.Preset.PresetItem;

/**
 * Container to allow sorting of preset search results
 * @author simon
 *
 */
public class IndexSearchResult implements Comparable<IndexSearchResult>{
	final int weight; // lower better
	final PresetItem item;
	
	public IndexSearchResult(int weight, @NonNull PresetItem item) {
	    this.weight = weight;
	    this.item = item;
	}

	@Override
	public int compareTo(@NonNull IndexSearchResult arg0) {
		if (arg0.weight > weight) {
			return -1;
		} else if (arg0.weight < weight) {
			return +1;
		}
		return 0; 
	}
	
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IndexSearchResult)) {
            return false;
        }
        return weight == ((IndexSearchResult)obj).weight && ((item == null && ((IndexSearchResult)obj).item == null) || (item != null && item.equals(((IndexSearchResult)obj).item)));
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (int)(weight ^ (weight >>> 32));
        result = 37 * result + (item == null ? 0 : item.hashCode());
        return result;
    }
}
