package io.vespucci.util;

import java.io.Serializable;
import java.util.List;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public abstract class ReadFile implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Read a file
     * 
     * @param activity the current activity
     * @param fileUri Uri of the file to read
     * @return true if successful
     */
    public abstract boolean read(@NonNull FragmentActivity currentActivity, @NonNull Uri fileUri);

    /**
     * Read multiple files, empty default implementation
     * 
     * @param activity the current activity
     * @param uris List of Uri to read
     */
    public void read(@NonNull FragmentActivity currentActivity, @NonNull List<Uri> uris) {
        // empty
    }
}
