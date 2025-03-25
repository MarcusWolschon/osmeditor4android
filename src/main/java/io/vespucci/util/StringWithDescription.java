package io.vespucci.util;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.presets.Regionalizable;
import io.vespucci.presets.ValueWithCount;

@SuppressWarnings("NullableProblems")
public class StringWithDescription extends Regionalizable implements Comparable<StringWithDescription>, Serializable, Value {
    private static final long serialVersionUID = 2L;

    private final String value;
    private String       description;

    /**
     * Construct a new instance
     * 
     * @param value the value
     */
    public StringWithDescription(@NonNull final String value) {
        this.value = value;
    }

    /**
     * Construct a new instance
     * 
     * @param value the value
     * @param description the description of the value
     */
    public StringWithDescription(@NonNull final String value, @Nullable final String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Construct a new instance from object of a known type
     * 
     * @param o one of StringWithDescription, ValueWIihCOunt or String
     */
    public StringWithDescription(@NonNull Object o) {
        if (o instanceof ValueWithCount) {
            value = ((ValueWithCount) o).getValue();
            description = ((ValueWithCount) o).getDescription();
        } else if (o instanceof StringWithDescription) {
            value = ((StringWithDescription) o).getValue();
            description = ((StringWithDescription) o).getDescription();
        } else if (o instanceof String) {
            value = (String) o;
            description = value;
        } else {
            value = "";
            description = value;
        }
    }

    /**
     * @return the value
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * @return the description can be null
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Set the description field
     * 
     * @param description the description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return value + (description != null ? (value == null || "".equals(value) ? "" : " - ") + description : "");
    }

    @Override
    public int compareTo(@NonNull StringWithDescription s) {
        return value.compareTo(s.getValue());
    }

    /**
     * Check for equality with a String value
     * 
     * This is likely bad style
     * 
     * @param s String to compare with
     * @return true if the value of this object is the same as s
     */
    public boolean equals(@Nullable String s) {
        return this.value.equals(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StringWithDescription)) {
            return false;
        }
        final StringWithDescription other = (StringWithDescription) o;
        return value.equals(other.value) && Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, description);
    }

    /**
     * 
     * @author simon
     *
     */
    public static class LocaleComparator implements Comparator<StringWithDescription> {

        Collator defaultLocaleCollator = Collator.getInstance();

        @Override
        public int compare(StringWithDescription lhs, StringWithDescription rhs) {
            String lhsDescription = lhs.getDescription();
            if (lhsDescription == null || "".equals(lhsDescription)) {
                lhsDescription = lhs.getValue();
            }
            String rhsDescription = rhs.getDescription();
            if (rhsDescription == null || "".equals(rhsDescription)) {
                rhsDescription = rhs.getValue();
            }
            return defaultLocaleCollator.compare(lhsDescription, rhsDescription);
        }
    }
}
