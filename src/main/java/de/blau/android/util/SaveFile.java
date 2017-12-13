package de.blau.android.util;

import java.io.Serializable;

import android.net.Uri;

public abstract class SaveFile implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public abstract boolean save(Uri fileUri);
}
