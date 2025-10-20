package de.blau.android.util;

import java.util.Objects;

import androidx.annotation.NonNull;
import de.blau.android.osm.GeoPoint;

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

    /**
     * Construct a new Coordinate object
     * 
     * @param point a GeoPoint implementing object
     */
    public IntCoordinates(@NonNull GeoPoint point) {
        this.lon = point.getLon();
        this.lat = point.getLat();
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
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
        return lat == other.lat && lon == other.lon;
    }
}
