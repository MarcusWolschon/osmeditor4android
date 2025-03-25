package io.vespucci.measure;

import androidx.annotation.NonNull;

public class LengthInFeetAndInches extends Length {

    private final int feet;
    private final int inches;

    /**
     * Construct a new Length using imperial units
     * 
     * @param key the tag key this applies to
     * @param feet the feet part of the length
     * @param inches the inches part of the length
     */
    public LengthInFeetAndInches(@NonNull String key, int feet, int inches) {
        super(key);
        this.feet = feet;
        this.inches = inches;
    }

    /**
     * Get the number of feet
     * 
     * @return the number of feet
     */
    public int getFeet() {
        return feet;
    }

    /**
     * Get the number of inches
     * 
     * @return the number of inches
     */
    public int getInches() {
        return inches;
    }

    @Override
    public String toString() {
        return Integer.toString(feet) + "\'" + Integer.toString(inches) + "\"";
    }
}
