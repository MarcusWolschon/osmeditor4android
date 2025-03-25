package io.vespucci.osm;

import io.vespucci.gpx.TrackPoint;

/**
 * Something that has a latitude and longitude and can return it in 1E7 format (e.g. {@link Node} and
 * {@link TrackPoint}).
 * 
 * @author Jan
 *
 */
public interface GeoPoint {

    /** @return the latitude of this point in 1E7 format */
    int getLat();

    /** @return the longitude of this point in 1E7 format */
    int getLon();

    interface InterruptibleGeoPoint extends GeoPoint {
        /** @return true if no line should be drawn from the last point to this one */
        boolean isInterrupted();
    }
}