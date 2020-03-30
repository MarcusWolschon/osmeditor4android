package de.blau.android.util;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provide a reasonably generic way of handling results from Activities
 * 
 * @author Simon Poole
 *
 */
public interface ActivityResultHandler {

    /**
     * Set a listener for the result from an Activity
     * 
     * @param code tHe code returned from the Intent
     * @param listener the ActivityResult.Listener to set
     */
    void setResultListener(int code, @NonNull Listener listener);

    interface Listener {

        /**
         * Process the result from an Activity
         * 
         * @param resultCode the response code from the activity
         * @param result the returned Intent
         */
        void processResult(int resultCode, @Nullable Intent result);
    }
}
