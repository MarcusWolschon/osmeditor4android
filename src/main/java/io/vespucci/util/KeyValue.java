package io.vespucci.util;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 
 * @author simon
 *
 */
public class KeyValue {
    private final String key;
    private List<String> values = null;

    /**
     * Construct a new instance
     * 
     * @param key the key
     * @param value the value
     */
    public KeyValue(@NonNull String key, @NonNull String value) {
        this.key = key;
        values = new ArrayList<>();
        values.add(value);
    }

    /**
     * Construct a new instance
     * 
     * @param key the key
     * @param values a List of values
     */
    public KeyValue(@NonNull String key, @Nullable List<String> values) {
        this.key = key;
        this.values = values;
    }

    /**
     * Get the key
     * 
     * @return the key
     */
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Get the value (if more than one are preset, the first)
     * 
     * @return a value or null
     */
    @Nullable
    public String getValue() {
        if (values != null) {
            return values.get(0);
        }
        return null;
    }

    /**
     * Get the List of values
     * 
     * @return the values or null
     */
    @Nullable
    public List<String> getValues() {
        return values;
    }

    /**
     * Set one value
     * 
     * @param value the value
     */
    public void setValue(@NonNull String value) {
        values = new ArrayList<>();
        values.add(value);
    }
}
