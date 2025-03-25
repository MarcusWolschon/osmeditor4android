package io.vespucci.exception;

import androidx.annotation.NonNull;

/**
 * Thrown when we have two keys that are identical
 * 
 * @author simon
 *
 */
public class DuplicateKeyException extends RuntimeException {

    private final String key;
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception
     * 
     * @param key the duplicate key
     */
    public DuplicateKeyException(@NonNull String key) {
        super();
        this.key = key;
    }

    /**
     * @return the key
     */
    @NonNull
    public String getKey() {
        return key;
    }
}
