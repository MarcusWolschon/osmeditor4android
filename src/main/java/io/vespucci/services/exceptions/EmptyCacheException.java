// Created by plusminus on 14:29:37 - 12.10.2008
package io.vespucci.services.exceptions;

public class EmptyCacheException extends Exception {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final long serialVersionUID = -6096533745569312072L;

    // ===========================================================
    // Fields
    // ===========================================================

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Construct a new exception
     * 
     * @param detailMessage the error message
     */
    public EmptyCacheException(String detailMessage) {
        super(detailMessage);
    }
}
