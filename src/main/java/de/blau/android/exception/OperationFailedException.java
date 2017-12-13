package de.blau.android.exception;

public class OperationFailedException extends RuntimeException {

    /**
     * Thrown when something failed that should really have worked
     */
    private static final long serialVersionUID = 1L;

    public OperationFailedException(String string) {
        super(string);
    }

    public OperationFailedException(OperationFailedException e) {
        super(e.getMessage());
    }

    public OperationFailedException(String string, Exception e) {
        super(string, e);
    }
}
