package de.blau.android;

public interface PostAsyncActionHandler {

    /**
     * call this on success
     */
    public void onSuccess();

    /**
     * method for error handling
     */
    default void onError() {
        // do nothing
    }
}
