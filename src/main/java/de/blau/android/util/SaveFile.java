package de.blau.android.util;

import java.io.Serializable;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public abstract class SaveFile implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Save a file
     * 
     * @param currentActivity current Activity
     * @param fileUri Uri file to save
     * @return true if successful
     */
    public abstract boolean save(@NonNull FragmentActivity currentActivity, @NonNull Uri fileUri);
}
