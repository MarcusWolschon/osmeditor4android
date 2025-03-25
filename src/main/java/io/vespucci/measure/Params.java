package io.vespucci.measure;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.measure.streetmeasure.MeasureContract.LengthUnit;

public final class Params {
    private final String     key;
    private final LengthUnit lengthUnit;
    private final boolean    measureVertical;
    private final Integer    measuringTapeColor;
    private final Integer    precisionCm;
    private final Integer    precisionInch;

    /**
     * Create a new instance
     * 
     * @param key the tag key we are measuring
     * @param lengthUnit the units on measurement
     * @param precisionCm precision in centimeters
     * @param precisionInch precision in inches
     * @param measureVertical if true we are measuring vertically
     * @param measuringTapeColor color for the measuring tape (stretmeasue specific)
     */
    public Params(@NonNull String key, @Nullable LengthUnit lengthUnit, @Nullable Integer precisionCm, @Nullable Integer precisionInch, boolean measureVertical,
            @Nullable Integer measuringTapeColor) {
        this.key = key;
        this.lengthUnit = lengthUnit;
        this.precisionCm = precisionCm;
        this.precisionInch = precisionInch;
        this.measureVertical = measureVertical;
        this.measuringTapeColor = measuringTapeColor;
    }

    /**
     * Get the tag key we are measuring
     * 
     * @return the key
     */
    @NonNull
    public final String getKey() {
        return this.key;
    }

    /**
     * Get the LengthUnit we are using
     * 
     * @return the LengthUnit
     */
    @Nullable
    public final LengthUnit getLengthUnit() {
        return this.lengthUnit;
    }

    /**
     * Get the precision we want the result in, in centimeters
     * 
     * @return the precision in cm
     */
    @Nullable
    public final Integer getPrecisionCm() {
        return this.precisionCm;
    }

    /**
     * Get the precision we want the result in, in inches
     * 
     * @return the precision in inches
     */
    @Nullable
    public final Integer getPrecisionInch() {
        return this.precisionInch;
    }

    /**
     * Check if we want to measure vertically
     * 
     * @return true if we want to measure vertically
     */
    public final boolean measureVertical() {
        return this.measureVertical;
    }

    /**
     * Get the tape color (StreetMeasure specific)
     * 
     * @return the tape color
     */
    @Nullable
    public final Integer getMeasuringTapeColor() {
        return this.measuringTapeColor;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Params) {
            Params params = (Params) obj;
            return Objects.equals(this.key, params.key) && this.lengthUnit == params.lengthUnit && Objects.equals(this.precisionCm, params.precisionCm)
                    && Objects.equals(this.precisionInch, params.precisionInch) && this.measureVertical == params.measureVertical
                    && Objects.equals(this.measuringTapeColor, params.measuringTapeColor);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, lengthUnit, precisionCm, precisionInch, measureVertical, measuringTapeColor);
    }
}