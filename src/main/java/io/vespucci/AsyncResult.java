package io.vespucci;

import androidx.annotation.Nullable;

public class AsyncResult {
    private final int    code;
    private final String message;

    /**
     * Construct a new result instance
     * 
     * @param code the result code
     */
    public AsyncResult(int code) {
        this(code, null);
    }

    /**
     * Construct a new result instance
     * 
     * @param code the result code
     * @param message an optional message
     */
    public AsyncResult(int code, @Nullable String message) {
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
