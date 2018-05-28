package de.blau.android.exception;

/**
 * Used for handling out of memory situations and similar
 * 
 * @author simon
 *
 */
public class StorageException extends Exception {

    public static final int OOM = 0;
    private int             code;

    public StorageException(int code) {
        super();
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

}
