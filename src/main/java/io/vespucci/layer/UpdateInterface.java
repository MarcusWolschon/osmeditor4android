package io.vespucci.layer;

import java.util.Collection;

import androidx.annotation.Nullable;

public interface UpdateInterface<V> {

    interface OnUpdateListener<W> {
        /**
         * Call this with the current displayed objects
         * 
         * @param <C>
         * 
         * @param objects Collections of W
         */
        @SuppressWarnings("unchecked")
        <C extends Collection<W>> void onUpdate(C... objects);
    }

    /**
     * Set a listener to be called on updates
     * 
     * @param listener the OnUpdateListener
     */
    void setOnUpdateListener(@Nullable OnUpdateListener<V> listener);
}
