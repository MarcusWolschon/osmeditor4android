package de.blau.android.services.util;

import android.location.Location;
import androidx.annotation.NonNull;

public class ExtendedLocation extends Location {

    private static final int HAS_HDOP_MASK              = 1;
    private static final int HAS_GEOID_HEIGHT_MASK      = 2;
    private static final int HAS_GEOID_CORRECTION_MASK  = 4;
    private static final int HAS_BAROMETRIC_HEIGHT_MASK = 8;
    private static final int USE_BAROMETRIC_HEIGHT_MASK = 16;

    private int flags = 0;

    private double hdop             = Double.NaN;
    private double geoidHeight      = Double.NaN;
    private double geoidCorrection  = Double.NaN;
    private double barometricHeight = Double.NaN;

    /**
     * Construct a new instance for a specific provider
     * 
     * @param provider the provider
     */
    public ExtendedLocation(@NonNull String provider) {
        super(provider);
    }

    /**
     * Copy constructor
     * 
     * @param location the Location to copy
     */
    public ExtendedLocation(@NonNull Location location) {
        super(location);
    }

    /**
     * Check if we have a hdop value
     * 
     * @return true if present
     */
    public boolean hasHdop() {
        return (flags & HAS_HDOP_MASK) != 0;
    }

    /**
     * @return the hdop
     */
    public double getHdop() {
        return hdop;
    }

    /**
     * @param hdop the hdop to set
     */
    public void setHdop(double hdop) {
        this.hdop = hdop;
        flags |= HAS_HDOP_MASK;
    }

    /**
     * Check if we have a height over the geoid value
     * 
     * @return true if present
     */
    public boolean hasGeoidHeight() {
        return (flags & HAS_GEOID_HEIGHT_MASK) != 0;
    }

    /**
     * @return the mslHeight
     */
    public double getGeoidHeight() {
        return geoidHeight;
    }

    /**
     * @param geoidHeight the height over the geoid to set
     */
    public void setGeoidHeight(double geoidHeight) {
        this.geoidHeight = geoidHeight;
        flags |= HAS_GEOID_HEIGHT_MASK;
    }

    /**
     * Check if we have a geoid correction value
     * 
     * @return true if present
     */
    public boolean hasGeoidCorrection() {
        return (flags & HAS_GEOID_CORRECTION_MASK) != 0;
    }

    /**
     * @return the geoidCorrection
     */
    public double getGeoidCorrection() {
        return geoidCorrection;
    }

    /**
     * @param geoidCorrection the geoidCorrection to set
     */
    public void setGeoidCorrection(double geoidCorrection) {
        this.geoidCorrection = geoidCorrection;
        flags |= HAS_GEOID_CORRECTION_MASK;
    }

    /**
     * Check if we have a barometric height value
     * 
     * @return true if present
     */
    public boolean hasBarometricHeight() {
        return (flags & HAS_BAROMETRIC_HEIGHT_MASK) != 0;
    }

    /**
     * @return the barometricHeight
     */
    public double getBarometricHeight() {
        return barometricHeight;
    }

    /**
     * @param barometricHeight the barometricHeight to set
     */
    public void setBarometricHeight(double barometricHeight) {
        this.barometricHeight = barometricHeight;
        flags |= HAS_BAROMETRIC_HEIGHT_MASK;
    }

    /**
     * Check if we should use barometric height values
     * 
     * @return true if present
     */
    public boolean useBarometricHeight() {
        return (flags & USE_BAROMETRIC_HEIGHT_MASK) != 0;
    }

    /**
     * Indicate that we should use barometric height
     */
    public void setUseBarometricHeight() {
        flags |= USE_BAROMETRIC_HEIGHT_MASK;
    }
}
