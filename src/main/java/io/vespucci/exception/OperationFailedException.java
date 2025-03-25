package io.vespucci.exception;

public class OperationFailedException extends RuntimeException {

    /**
     * Thrown when something failed that should really have worked
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception
     * 
     * @param message the error message
     */
    public OperationFailedException(String message) {
        super(message);
    }

    /**
     * Construct a new exception
     * 
     * @param message the error message
     * @param cause the original Exception
     */
    public OperationFailedException(String message, Exception cause) {
        super(message, cause);
    }
}
