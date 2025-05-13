package de.blau.android.exception;

import java.io.IOException;

public class NoOAuthConfigurationException extends IOException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public NoOAuthConfigurationException(final String message) {
        super(message);
    }

    /**
     * Construct a new exception
     * 
     * @param message the error message
     * @param cause the original Exception
     */
    NoOAuthConfigurationException(final String message, final Throwable cause) {
        super(message);
        initCause(cause);
    }
}
