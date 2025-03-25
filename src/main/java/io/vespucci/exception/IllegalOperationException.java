package io.vespucci.exception;

/**
 * Thrown when we are trying to do something that is not allowed
 * 
 * @author simon
 *
 */
public class IllegalOperationException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public IllegalOperationException(String message) {
        super(message);
    }
}
