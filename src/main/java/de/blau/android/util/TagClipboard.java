package de.blau.android.util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

public class TagClipboard implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = "TagClipboard";

    private static final String COPIED_TAGS_FILE = "copiedtags.dat";

    private static SavingHelper<TagClipboard> savingHelper = new SavingHelper<>();

    /**
     * copy of the tags
     */
    private LinkedHashMap<String, String> tags = null;

    /**
     * If true we need to be saved
     */
    private boolean dirty = false;

    /**
     * Save any copied tags to a file
     * 
     * @param context Android Context
     */
    public synchronized void save(Context context) {
        if (dirty) {
            savingHelper.save(context, COPIED_TAGS_FILE, this, false);
        }
    }

    /**
     * Restore any copied tags from a file
     * 
     * @param context Android Context
     */
    public synchronized void restore(Context context) {

        try {
            TagClipboard saved = savingHelper.load(context, COPIED_TAGS_FILE, false);
            if (saved != null) {
                tags = saved.tags;
                dirty = true;
            }
        } catch (ClassCastException cce) {
            // old format save file, ignore
            Log.e(DEBUG_TAG, "Got exception " + cce.getMessage());
        }
    }

    /**
     * Copy tags to the clipboard
     * 
     * @param tags a Map containing the tags
     */
    public synchronized void copy(@NonNull Map<String, String> tags) {
        this.tags = new LinkedHashMap<>(tags);
        dirty = true;
    }

    /**
     * Get the contexts of the clipboard
     * 
     * @return a Map containing the tags or null if the clipboard was empty
     */
    @Nullable
    public synchronized Map<String, String> paste() {
        if (tags == null) {
            return null;
        }
        return new LinkedHashMap<>(tags);
    }

    /**
     * Check if the clipboard contains something
     * 
     * @return true is empty
     */
    public boolean isEmpty() {
        return tags == null || tags.isEmpty();
    }
}
