package io.vespucci.propertyeditor.tagform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface KeyValueRow {

    /**
     * Return the OSM key value
     * 
     * @return the key as a String
     */
    @NonNull
    String getKey();
    
    /**
     * Get the current value
     * 
     * @return the current value as a String
     */
    @Nullable
    String getValue();
    
    default boolean hasKey(@Nullable String key) {
        return getKey().equals(key);
    }
}
