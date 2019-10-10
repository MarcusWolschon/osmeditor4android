package de.blau.android.util;

import java.util.Comparator;

import android.support.annotation.NonNull;
import de.blau.android.presets.Preset.PresetItem;

/**
 * Container to allow sorting of preset search results
 * 
 * @author simon
 *
 */
public class IndexSearchResult {
    int              weight; // lower better
    final PresetItem item;

    /**
     * Construct a new instance
     * 
     * @param weight the initial weight
     * @param item the PresetItem
     */
    public IndexSearchResult(int weight, @NonNull PresetItem item) {
        this.weight = weight;
        this.item = item;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IndexSearchResult)) {
            return false;
        }
        return (item == null && ((IndexSearchResult) obj).item == null) || (item != null && item.equals(((IndexSearchResult) obj).item));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (item == null ? 0 : item.hashCode());
        return result;
    }

    public static final Comparator<IndexSearchResult> weightComparator = new Comparator<IndexSearchResult>() {

        @Override
        public int compare(IndexSearchResult isr1, IndexSearchResult isr2) {
            if (isr2.weight > isr1.weight) {
                return -1;
            } else if (isr2.weight < isr1.weight) {
                return +1;
            }
            return 0;
        }
    };
}
