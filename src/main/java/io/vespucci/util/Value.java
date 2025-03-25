package io.vespucci.util;

import androidx.annotation.Nullable;

public interface Value {

    /**
     * Get the value for the object
     * 
     * @return the value or null
     */
    @Nullable
    public String getValue();
}
