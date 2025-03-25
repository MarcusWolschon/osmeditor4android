package io.vespucci;

import androidx.annotation.Nullable;

public interface PostAsyncActionHandler {

    /**
     * call this on success
     */
    public void onSuccess();

    /**
     * Error handling with some details
     * 
     * @param result the result we got
     */
    default void onError(@Nullable AsyncResult result) {
        // do nothing
    }
}
