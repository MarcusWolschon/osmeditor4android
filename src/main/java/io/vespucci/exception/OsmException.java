package io.vespucci.exception;

import java.io.IOException;

public class OsmException extends IOException {

    /**
     * 
     */
    private static final long serialVersionUID = 9160300298635675666L;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public OsmException(final String message) {
        super(message);
    }

    /**
     * Construct a new exception
     * 
     * @param message the error message
     * @param cause the original Exception
     */
    OsmException(final String message, final Throwable cause) {
        super(message);
        initCause(cause);
    }
}
