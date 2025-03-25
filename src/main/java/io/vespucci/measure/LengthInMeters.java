package io.vespucci.measure;

import androidx.annotation.NonNull;

public class LengthInMeters extends Length {

    private final double meters;

    /**
     * Construct a new Length from a value in meters
     * 
     * @param key the tag key this applies to
     * @param meters the length in meters
     */
    public LengthInMeters(@NonNull String key, double meters) {
        super(key);
        this.meters = meters;
    }

    /**
     * Get the length in meters
     * 
     * @return the length in meters
     */
    public double getMeters() {
        return meters;
    }

    @Override
    public String toString() {
        return Double.toString(meters);
    }
}
