package io.vespucci.propertyeditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface TagChanged {
    
    /**
     * Notify that a tag has changed
     * 
     * @param key the key
     * @param value the value, if null assume the tag has been deleted
     */
    void changed(@NonNull String key, @Nullable String value);
}
