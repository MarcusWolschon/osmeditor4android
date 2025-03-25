package io.vespucci.util;

import java.util.Comparator;
import java.util.Objects;

import androidx.annotation.NonNull;
import io.vespucci.presets.PresetItem;

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
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IndexSearchResult)) {
            return false;
        }
        return item.equals(((IndexSearchResult) obj).item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item);
    }

    public static final Comparator<IndexSearchResult> WEIGHT_COMPARATOR = (isr1, isr2) -> {
        if (isr2.weight > isr1.weight) {
            return -1;
        } else if (isr2.weight < isr1.weight) {
            return +1;
        }
        return 0;
    };
}
