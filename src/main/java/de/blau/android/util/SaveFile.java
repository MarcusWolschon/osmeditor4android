package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public abstract class SaveFile implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, SaveFile.class.getSimpleName().length());
    private static final String DEBUG_TAG = SaveFile.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Add an extension to a file name if necessary
     * 
     * This will only work if the file has already been written
     * 
     * @param context an Android Context
     * @param fileUri the original Uri
     * @param extension the extension to add
     * @return a potentially new Uri
     */
    @NonNull
    public static Uri addExtensionIfNeeded(@NonNull Context context, @NonNull Uri fileUri, @NonNull String extension) {
        String displayName = ContentResolverUtil.getDisplaynameColumn(context, fileUri);
        if (displayName.indexOf(".") < 0) {
            String newName = displayName + "." + extension;
            Log.i(DEBUG_TAG, "Renaming to " + newName);
            try {
                Uri newUri = ContentResolverUtil.rename(context, fileUri, newName);
                if (newUri != null) {
                    return newUri;
                }
            } catch (Exception ex) {
                // we can't trust Android
                Log.e(DEBUG_TAG, "Rename to " + newName + " failed with " + ex.getMessage());
            }

        }
        return fileUri;
    }

    /**
     * Save a file
     * 
     * @param currentActivity current Activity
     * @param fileUri Uri file to save
     * @return true if successful
     */
    public abstract boolean save(@NonNull FragmentActivity currentActivity, @NonNull Uri fileUri);
}
