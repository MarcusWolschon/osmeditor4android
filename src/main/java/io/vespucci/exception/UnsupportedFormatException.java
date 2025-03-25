package io.vespucci.exception;

import androidx.annotation.NonNull;

/**
 * Used file format issues
 * 
 * @author simon
 *
 */
public class UnsupportedFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception
     * 
     * @param message the exception message
     */
    public UnsupportedFormatException(@NonNull String message) {
        super(message);
    }
}
