package de.blau.android.util;

import java.io.Serializable;
import java.util.List;

import android.net.Uri;
import androidx.annotation.NonNull;

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
    public abstract boolean read(@NonNull Uri fileUri);

    /**
     * Read multiple files, empty default implementation
     * 
     * @param uris List of Uri to read
     */
    public void read(@NonNull List<Uri> uris) {
        // empty
    }
}
