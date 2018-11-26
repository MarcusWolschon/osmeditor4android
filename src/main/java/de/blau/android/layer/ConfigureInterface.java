package de.blau.android.layer;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

public interface ConfigureInterface {

    /**
     * Check if the configuration can be shown
     * 
     * @return true if the configuration dialog can be shown
     */
    boolean enableConfiguration();

    /**
     * Configure this layer
     * 
     * @param activity calling FragmentActivity
     */
    void configure(@NonNull FragmentActivity activity);
}
