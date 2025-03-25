package io.vespucci.layer;

import android.content.Context;
import androidx.annotation.NonNull;

public interface DiscardInterface {
    /**
     * Discard this layer
     * 
     * @param context Android context
     */
    void discard(@NonNull Context context);
}
