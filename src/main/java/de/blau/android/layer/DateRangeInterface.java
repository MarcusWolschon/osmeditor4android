package de.blau.android.layer;

public interface DateRangeInterface {
    /**
     * Set a date range to display
     * 
     * @param start the lower bound for the capture date in ms since the epoch
     * @param end the upper bound for the capture date in ms since the epoch
     */
    public void setDateRange(long start, long end);
}
