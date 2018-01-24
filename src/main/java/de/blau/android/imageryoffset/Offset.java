package de.blau.android.imageryoffset;

import java.io.Serializable;

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
