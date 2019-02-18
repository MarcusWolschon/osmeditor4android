package de.blau.android.exception;

import android.support.annotation.NonNull;


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
