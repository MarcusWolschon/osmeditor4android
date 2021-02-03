package de.blau.android.util;

/**
 * Wrapper for a WGS84*1E7 coordinate tupel
 * 
 * @author simon
 *
 */
public class IntCoordinates {

    public int lon; // NOSONAR
    public int lat; // NOSONAR

    /**
     * Construct a new Coordinate object
     * 
     * @param lon WGS84*1E7 longitude
     * @param lat WGS84*1E7 latitude
     */
    public IntCoordinates(int lon, int lat) {
        this.lon = lon;
        this.lat = lat;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + lat;
        return prime * result + lon;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IntCoordinates)) {
            return false;
        }
        IntCoordinates other = (IntCoordinates) obj;
        if (lat != other.lat) {
            return false;
        }
        return lon == other.lon;
    }
}
