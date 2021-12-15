package de.blau.android;

import androidx.annotation.Nullable;

public interface PostAsyncActionHandler {

    /**
     * call this on success
     */
    public void onSuccess();

    /**
     * Error handling with some details
     * 
     * @param result
     */
    default void onError(@Nullable AsyncResult result) {
        // do nothing
    }
}
