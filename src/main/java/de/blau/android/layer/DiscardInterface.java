package de.blau.android.layer;

import android.content.Context;
import android.support.annotation.NonNull;

public interface DiscardInterface {
    /**
     * Discard this layer
     * 
     * @param context Android context
     */
    void discard(@NonNull Context context);
}
