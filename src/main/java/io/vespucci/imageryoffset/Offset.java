package io.vespucci.imageryoffset;

import java.io.Serializable;

import androidx.annotation.NonNull;

/**
 * Imagery offset for direct use when rendering
 * 
 * @author Simon Poole
 *
 */
public class Offset implements Serializable { // offsets in WGS84 needed to align imagery
    private static final long serialVersionUID = 1L;

    private double deltaLon = 0;
    private double deltaLat = 0;

    /**
     * Default constructor
     */
    public Offset() {
        // do nothing
    }

    /**
     * Copy constructor
     * 
     * @param toCopy Offset to copy
     */
    public Offset(@NonNull Offset toCopy) {
        deltaLon = toCopy.deltaLon;
        deltaLat = toCopy.deltaLat;
    }

    /**
     * @return deltaLon
     */
    public double getDeltaLon() {
        return deltaLon;
    }

    /**
     * @param deltaLon the deltaLon to set
     */
    public void setDeltaLon(double deltaLon) {
        this.deltaLon = deltaLon;
    }

    /**
     * @return deltaLat
     */
    public double getDeltaLat() {
        return deltaLat;
    }

    /**
     * @param deltaLat the deltaLat to set
     */
    public void setDeltaLat(double deltaLat) {
        this.deltaLat = deltaLat;
    }
}
