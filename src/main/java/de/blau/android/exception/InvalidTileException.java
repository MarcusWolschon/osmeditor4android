package de.blau.android.exception;

import java.io.IOException;

public class InvalidTileException extends IOException {

    /**
     * Thrown when we have tried to retrieve an invalid tile
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new InvalidTileException
     * 
     * @param message a message indicating the specifics
     */
    public InvalidTileException(String message) {
        super(message);
    }
}
