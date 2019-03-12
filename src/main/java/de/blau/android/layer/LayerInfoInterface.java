package de.blau.android.layer;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

public interface LayerInfoInterface {
    /**
     * Display information on the layer
     * 
     * @param activity calling FragmentActivity
     */
    void showInfo(@NonNull final FragmentActivity activity);
}
