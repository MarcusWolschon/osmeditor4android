package de.blau.android.layer.streetlevel;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface DateRangeInterface {
    /**
     * Set a date range to display
     * 
     * @param start the lower bound for the capture date in ms since the epoch
     * @param end the upper bound for the capture date in ms since the epoch
     */
    public void setDateRange(long start, long end);

    /**
     * Show a modal to select the date range
     * 
     * @param activity the current activity
     * @param layerIndex the index of the layer
     */
    public void selectDateRange(@NonNull FragmentActivity activity, int layerIndex);
    
    /**
     * Convert from days to ms
     * 
     * @param days the number of days
     * @return ms
     */
    static long fromDays(float days) {
        return (long) (days * 24 * 3600000L);
    }

    /**
     * Convert from ms to days
     * 
     * @param ms the number of ms
     * @return the number of days
     */
    static float toDays(long ms) {
        return ms / (24f * 3600000L);
    }
}
