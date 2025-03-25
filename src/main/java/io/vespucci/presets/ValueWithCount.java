package io.vespucci.presets;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.util.Value;

public class ValueWithCount implements Comparable<ValueWithCount>, Value {
    private final String value;
    private String       description     = null;
    private int          count           = -1;
    private boolean      descriptionOnly = false;

    /**
     * Construct a new ValueWithCount instance
     * 
     * @param value the value
     */
    public ValueWithCount(@NonNull final String value) {
        this.value = value;
    }

    /**
     * Construct a new ValueWithCount instance
     * 
     * @param value the value
     * @param description the description
     */
    public ValueWithCount(@NonNull final String value, @Nullable final String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Construct a new ValueWithCount instance
     * 
     * @param value the value
     * @param description the description
     * @param descriptionOnly if true there is only the description present
     */
    public ValueWithCount(@NonNull final String value, @Nullable final String description, final boolean descriptionOnly) {
        this.value = value;
        this.description = description;
        this.descriptionOnly = descriptionOnly;
    }

    /**
     * Construct a new ValueWithCount instance
     * 
     * @param value the value
     * @param count the count
     */
    public ValueWithCount(@NonNull final String value, final int count) {
        this.value = value;
        this.count = count;
    }

    @Override
    public String toString() {
        if (count == -1) {
            return descriptionOnly ? (description != null ? description : value) : (description != null ? value + " - " + description : value);
        } else if (count >= 1) {
            return value + " (" + count + ")" + (description != null ? " " + description : "");
        }
        return null; // NOSONAR this breaks the implicit contract for toString, however this class is specifically for
                     // use in ArrayAdapters
    }

    /**
     * Get the actual value
     * 
     * @return the value
     */
    @Override
    @NonNull
    public String getValue() {
        return value;
    }

    /**
     * Get the description for this value
     * 
     * @return the description or null if none
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Set the description for this value
     * 
     * @param description the description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Override
    public int compareTo(@NonNull ValueWithCount other) {
        int result = this.value.compareTo(other.value);
        if (result == 0) {
            if (other.count < count) {
                return -1;
            } else if (other.count > count) {
                return +1;
            }
            return 0;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ValueWithCount)) {
            return false;
        }
        final ValueWithCount other = (ValueWithCount) obj;
        return value.equals(other.value) && count == other.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, description, count, descriptionOnly);
    }
}
