package de.blau.android.exception;

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

    public IllegalOperationException(String string) {
        super(string);
    }

    public IllegalOperationException(IllegalOperationException e) {
        super(e.getMessage());
    }

}
