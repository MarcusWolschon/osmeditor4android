package de.blau.android.util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class TagClipboard implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String COPIED_TAGS_FILE = "copiedtags.dat";

    private static SavingHelper<TagClipboard> savingHelper = new SavingHelper<>();

    /**
     * copy of the tags
     */
    private LinkedHashMap<String, String> tags = null;

    /**
     * Flag if this was a cut or a copy
     */
    private boolean cut = false;

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
        TagClipboard saved = savingHelper.load(context, COPIED_TAGS_FILE, false);
        if (saved != null) {
            tags = saved.tags;
            cut = saved.cut;
            dirty = true;
        }
    }

    /**
     * Copy tags to the clipboard
     * 
     * @param tags a Map containing the tags
     */
    public synchronized void copy(@NonNull Map<String, String> tags) {
        cut = false;
        this.tags = new LinkedHashMap<>(tags);
        dirty = true;
    }

    /**
     * Copy tags to the clipboard
     * 
     * @param tags a Map containing the tags
     */
    public synchronized void cut(@NonNull Map<String, String> tags) {
        cut = true;
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
        Map<String, String> result = new LinkedHashMap<>(tags);
        if (cut) {
            tags = null;
            dirty = true;
        }
        return result;
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
