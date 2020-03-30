package de.blau.android.presets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ValueWithCount implements Comparable<ValueWithCount> {
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
            return value + " (" + count + ")" + (description != null ? value + " - " + description : "");
        }
        return null; // NOSONAR this breaks the implicit contract for toString, however this class is specifically for
                     // use in ArrayAdapters
    }

    /**
     * 
     * @return the value
     */
    @NonNull
    public String getValue() {
        return value;
    }

    /**
     * 
     * @return the description or null if none
     */
    @Nullable
    public String getDescription() {
        return description;
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
        if (!(obj instanceof ValueWithCount)) {
            return false;
        }
        return value.equals(((ValueWithCount) obj).value) && count == ((ValueWithCount) obj).count;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (value == null ? 0 : value.hashCode());
        result = 37 * result + (description == null ? 0 : description.hashCode());
        result = 37 * result + count;
        result = 37 * result + (descriptionOnly ? 1231 : 1237);
        return result;
    }
}
