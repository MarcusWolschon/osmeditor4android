package de.blau.android.osm;

public interface GpxTimeFormater {
    /**
     * Return time in a format suitable for GPX format 
     *  
     * @param time  time in milliseconds since the epoch
     * @return the formated date and time
     */
    String format(long time);
}
