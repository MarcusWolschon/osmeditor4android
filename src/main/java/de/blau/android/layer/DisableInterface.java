package de.blau.android.layer;

import android.content.Context;
import android.support.annotation.NonNull;

public interface DisableInterface {
    /**
     * Disable this layer
     * 
     * @param context Android context
     */
    void disable(@NonNull Context context);
}
