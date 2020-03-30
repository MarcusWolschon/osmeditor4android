package de.blau.android.layer;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface LayerInfoInterface {
    /**
     * Display information on the layer
     * 
     * @param activity calling FragmentActivity
     */
    void showInfo(@NonNull final FragmentActivity activity);
}
