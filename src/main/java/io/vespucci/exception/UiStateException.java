package io.vespucci.exception;

/**
 * Thrown when we encounter a fatal UI screwup
 * 
 * @author Simon Poole
 *
 */
public class UiStateException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public UiStateException(String message) {
        super(message);
    }
}
