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
}
