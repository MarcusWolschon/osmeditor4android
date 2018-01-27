package de.blau.android.util;

import java.io.Serializable;

import android.net.Uri;

public abstract class SaveFile implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Save a file
     * 
     * @param fileUri Uri file to save
     * @return true if successful
     */
    public abstract boolean save(Uri fileUri);
}
