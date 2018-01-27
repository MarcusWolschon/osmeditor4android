package de.blau.android.util;

import java.io.Serializable;
import java.util.List;

import android.net.Uri;

public abstract class ReadFile implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Read a file
     * 
     * @param fileUri Uri of the file to read
     * @return true if sucessful
     */
    public abstract boolean read(Uri fileUri);

    /**
     * Read multiple files, empty defaul implementation
     * 
     * @param uris List of Uri to read
     */
    public void read(List<Uri> uris) {
        // empty 
    }
}
