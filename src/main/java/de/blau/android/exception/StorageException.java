package de.blau.android.exception;

/**
 * Used for handling out of memory situations and similar
 * 
 * @author simon
 *
 */
public class StorageException extends RuntimeException {

    /**
     * Out of memory
     */
    public static final int OOM = 0;
    private int             code;

    /**
     * Construct a new exception
     * 
     * @param code the error code
     */
    public StorageException(int code) {
        super();
        this.code = code;
    }

    /**
     * Get the error code
     * 
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

}
