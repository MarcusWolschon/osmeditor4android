package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.util.SavingHelper;

/**
 * Tags that we want to remove before saving to server. List is in discarded.json from the iD repository
 * 
 * @author simon
 *
 */
public class DiscardedTags {

    private static final String DEBUG_TAG = "DiscardedTags";

    private HashSet<String> redundantTags = new HashSet<>();

    /**
     * Implicit assumption that the list will be short and that it is OK to read in synchronously
     * 
     * @param context Android Context
     */
    public DiscardedTags(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Parsing configuration file");

        AssetManager assetManager = context.getAssets();

        InputStream is = null;
        JsonReader reader = null;
        try {
            is = assetManager.open("discarded.json");
            reader = new JsonReader(new InputStreamReader(is, OsmXml.UTF_8));
            try {
                reader.beginArray();
                while (reader.hasNext()) {
                    redundantTags.add(reader.nextString());
                }
                reader.endArray();
                Log.d(DEBUG_TAG, "Found " + redundantTags.size() + " tags.");
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "Reading discarded.json " + e.getMessage());
            }
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "Opening discarded.json " + e.getMessage());
        } finally {
            SavingHelper.close(reader);
            SavingHelper.close(is);
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
     * Check if the element has only tags that would be disarded
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
}
