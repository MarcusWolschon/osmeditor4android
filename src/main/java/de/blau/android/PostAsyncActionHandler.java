package de.blau.android;

import androidx.annotation.Nullable;

public interface PostAsyncActionHandler {

    /**
     * call this on success
     */
    public void onSuccess();

    /**
     * method for error handling
     * 
     * @deprecated use onError(AsyncResult result)
     */
    @Deprecated
    default void onError() {
        onError(null);
    }

    /**
     * Error handling with some details
     * 
     * @param result
     */
    default void onError(@Nullable ReadAsyncResult result) {
        // do nothing
    }
}
