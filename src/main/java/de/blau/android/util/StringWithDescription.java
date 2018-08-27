package de.blau.android.util;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.presets.ValueWithCount;

@SuppressWarnings("NullableProblems")
public class StringWithDescription implements Comparable<StringWithDescription>, Serializable {
    private static final long serialVersionUID = 1L;

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
    public boolean equals(String s) {
        return this.value.equals(s);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && (o instanceof StringWithDescription && this == (StringWithDescription) o
                || (this.value.equals(((StringWithDescription) o).value) && ((this.description == null && ((StringWithDescription) o).description == null)
                        || (this.description != null && this.description.equals(((StringWithDescription) o).description)))));
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + (value == null ? 0 : value.hashCode());
        result = 37 * result + (description == null ? 0 : description.hashCode());
        return result;
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
