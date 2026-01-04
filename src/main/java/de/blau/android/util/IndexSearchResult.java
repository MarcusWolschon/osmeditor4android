package de.blau.android.util;

import java.util.Comparator;
import java.util.Objects;

import androidx.annotation.NonNull;
import de.blau.android.presets.PresetItem;

/**
 * Container to allow sorting of preset search results
 * 
 * @author simon
 *
 */
public class IndexSearchResult {
    private int              weight; // lower better
    private final PresetItem item;

    /**
     * Construct a new instance
     * 
     * @param weight the initial weight
     * @param item the PresetItem
     */
    public IndexSearchResult(int weight, @NonNull PresetItem item) {
        this.setWeight(weight);
        this.item = item;
    }

    /**
     * @return the item
     */
    public PresetItem getItem() {
        return item;
    }

    /**
     * @return the weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(int weight) {
        this.weight = weight;
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

    @Override
    public String toString() {
        return item.getName() + " " + weight;
    }
}
