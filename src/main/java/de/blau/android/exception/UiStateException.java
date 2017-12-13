package de.blau.android.exception;

/**
 * Thrown when we encounter a fatal UI screwup
 * 
 * @author simon
 *
 */
public class UiStateException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public UiStateException(String string) {
        super(string);
    }

    public UiStateException(UiStateException e) {
        super(e.getMessage());
    }
}
