package de.blau.android;

import android.support.annotation.Nullable;

public class ReadAsyncResult {
    private final int    code;
    private final String message;

    /**
     * Construct a new result instance
     * 
     * @param code the result code
     */
    ReadAsyncResult(int code) {
        this(code, null);
    }

    /**
     * Construct a new result instance
     * 
     * @param code the result code
     * @param message an optional message
     */
    ReadAsyncResult(int code, @Nullable String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 
     * @return the result code
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the message or null
     */
    @Nullable
    public String getMessage() {
        return message;
    }
}
