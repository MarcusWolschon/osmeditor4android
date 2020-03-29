package de.blau.android.exception;

import androidx.annotation.NonNull;

public class OsmIllegalOperationException extends RuntimeException {

    /**
     * Thrown when we are trying to do something that is not allowed from an OSM data perspective
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public OsmIllegalOperationException(String message) {
        super(message);
    }

    /**
     * Construct a new exception from an existing one
     * 
     * @param original the original exception
     */
    public OsmIllegalOperationException(@NonNull OsmIllegalOperationException original) {
        super(original.getMessage());
    }
}
