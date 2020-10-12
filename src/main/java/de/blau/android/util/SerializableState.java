package de.blau.android.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provide a Bundle like class that supports Serialisation
 * 
 * @author simon
 *
 */
public class SerializableState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Serializable> state = new HashMap<>();

    /**
     * Store an object that implements Serializable
     * 
     * @param key the key
     * @param o the serializable object
     */
    public void putSerializable(@NonNull String key, @Nullable Serializable o) {
        state.put(key, o);
    }

    /**
     * Store a List of T
     * 
     * @param <T> a Serializable object
     * @param key the key
     * @param l a List of T
     */
    public <T extends Serializable> void putList(@NonNull String key, @Nullable List<T> l) {
        state.put(key, (Serializable) l);
    }

    /**
     * Store a Long
     * 
     * @param key the key
     * @param l the Long object
     */
    public void putLong(@NonNull String key, @Nullable Long l) {
        state.put(key, l);
    }

    /**
     * Get a serializable object
     * 
     * @param key the key
     * @return a serializable object or null if not found
     */
    @Nullable
    public Serializable getSerializable(@NonNull String key) {
        return state.get(key);
    }

    /**
     * Get a Long
     * 
     * @param key the key
     * @return a Long or null if not found or not a Long
     */
    @Nullable
    public Long getLong(@NonNull String key) {
        try {
            return (Long) state.get(key);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Get a list of T
     * 
     * @param <T> a Serializable object
     * @param key the key
     * @return a List of T or null if not found or not a List of T
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Serializable> List<T> getList(@NonNull String key) {
        try {
            return (List<T>) state.get(key);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
