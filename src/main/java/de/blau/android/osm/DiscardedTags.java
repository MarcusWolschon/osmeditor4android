package de.blau.android.osm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Tags that we want to remove before saving to server. List is in discarded.json from the iD repository
 * 
 * @author simon
 *
 */
public class DiscardedTags {

    private static final String DEBUG_TAG = DiscardedTags.class.getSimpleName().substring(0, Math.min(23, DiscardedTags.class.getSimpleName().length()));

    private static final String ASSET_FILE = "discarded.json";

    private Set<String> redundantTags = new HashSet<>();

    /**
     * Implicit assumption that the list will be short and that it is OK to read in synchronously
     * 
     * @param context Android Context
     */
    @SuppressLint("NewApi") // StandardCharsets is desugared for APIs < 19.
    public DiscardedTags(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Parsing configuration file");
        AssetManager assetManager = context.getAssets();
        try (InputStream is = assetManager.open(ASSET_FILE); JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                redundantTags.add(reader.nextName());
                reader.skipValue();
            }
            reader.endObject();
            Log.d(DEBUG_TAG, "Found " + redundantTags.size() + " tags.");
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "Opening/reading " + ASSET_FILE + " " + e.getMessage());
        }
    }

    /**
     * Remove the redundant tags from element.
     * 
     * Notes:
     * 
     * - element already has the modified flag set if not, something went wrong and we skip
     * 
     * - this does not create a checkpoint and assumes that we will never want to undo this
     * 
     * @param element the OsmElement we want to remove the tags from
     */
    void remove(@NonNull OsmElement element) {
        if (element.isUnchanged()) {
            Log.e(DEBUG_TAG, "Presented with unmodified element");
            return;
        }
        boolean modified = false;
        SortedMap<String, String> newTags = new TreeMap<>();
        for (String key : element.getTags().keySet()) {
            Log.d(DEBUG_TAG, "Checking " + key);
            if (!redundantTags.contains(key)) {
                newTags.put(key, element.getTags().get(key));
            } else {
                Log.d(DEBUG_TAG, " delete");
                modified = true;
            }
        }
        if (modified) {
            element.setTags(newTags);
        }
    }

    /**
     * Check if the element has only tags that would be discarded
     * 
     * @param element the OsmElement to check
     * @return true if only automatically removable tags are present
     */
    public boolean only(@NonNull OsmElement element) {
        for (String key : element.getTags().keySet()) {
            if (!redundantTags.contains(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the keys that we will discard
     * 
     * @return the redundant keys
     */
    @NonNull
    public Collection<String> getKeys() {
        return redundantTags;
    }
}
