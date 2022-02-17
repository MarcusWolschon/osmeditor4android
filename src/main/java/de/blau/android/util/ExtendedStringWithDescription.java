package de.blau.android.util;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ExtendedStringWithDescription extends StringWithDescription implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean deprecated;

    /**
     * Construct a new instance
     * 
     * @param value the value
     */
    public ExtendedStringWithDescription(@NonNull final String value) {
        super(value);
    }

    /**
     * Construct a new instance
     * 
     * @param value the value
     * @param description the description of the value
     */
    public ExtendedStringWithDescription(@NonNull final String value, @Nullable final String description) {
        super(value, description);
    }

    /**
     * Construct a new instance from object of a known type
     * 
     * @param o one of StringWithDescription, ValueWIihCOunt or String
     */
    public ExtendedStringWithDescription(@NonNull Object o) {
        super(o);
        if (o instanceof ExtendedStringWithDescription) {
            this.deprecated = ((ExtendedStringWithDescription) o).deprecated;
        }
    }

    /**
     * Check if this value is deprecated
     * 
     * @return true if the value is deprecated
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * Set this values deprecated status
     * 
     * @param deprecated if true the value is considered deprecated
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public String toString() {
        return super.toString() + (deprecated ? " (deprecated)" : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ExtendedStringWithDescription)) {
            return false;
        }
        ExtendedStringWithDescription other = (ExtendedStringWithDescription) obj;
        if (deprecated != other.deprecated) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 37 * super.hashCode() + (deprecated ? 1 : 0);
    }
}
