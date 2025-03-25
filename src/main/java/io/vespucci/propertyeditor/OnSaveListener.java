package io.vespucci.propertyeditor;

import androidx.annotation.NonNull;

/**
 * Generic interface to use when a tag needs to be saved by a callback
 * 
 * @author Simon Poole
 *
 */
interface OnSaveListener {

    /**
     * Save the tag
     * 
     * @param key the tag key
     * @param value the tag value
     */
    void save(@NonNull String key, @NonNull String value);
}
