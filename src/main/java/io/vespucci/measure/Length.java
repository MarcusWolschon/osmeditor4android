package io.vespucci.measure;

import androidx.annotation.NonNull;

public abstract class Length {
    private final String key;

    /**
     * Construct a new Length
     * 
     * @param key the tag key this applies to
     */
    protected Length(@NonNull String key) {
        this.key = key;
    }

    /**
     * Get the tag key this applies to
     * 
     * @return the key
     */
    public String getKey() {
        return key;
    }
}
