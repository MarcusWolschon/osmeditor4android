package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Class to determine if an OSM tag implies area semantics, see https://github.com/simonpoole/osm-area-tags
 * 
 * @author simon
 *
 */
public class AreaTags {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AreaTags.class.getSimpleName().length());
    private static final String DEBUG_TAG = AreaTags.class.getSimpleName().substring(0, TAG_LEN);

    private static final String DEFAULT   = "default";
    private static final String AREA_KEYS = "areaKeys";
    private static final String VALUES    = "values";

    private static final String AREA_TAGS_JSON = "area-tags.json";

    private final Map<String, Boolean> tagMap;

    /**
     * Implicit assumption that the data will be short and that it is OK to read in synchronously which may not be true
     * any longer
     * 
     * @param context Android Context
     */
    public AreaTags(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Initalizing");
        tagMap = getTagMap(context.getAssets(), AREA_TAGS_JSON);
    }

    /**
     * Read a Json file from assets conforming to https://github.com/simonpoole/osm-area-tags/schema.json
     * 
     * @param assetManager an AssetManager
     * @param fileName the name of the file
     * @return a Map
     */
    @NonNull
    private Map<String, Boolean> getTagMap(@NonNull AssetManager assetManager, @NonNull String fileName) {
        Map<String, Boolean> result = new HashMap<>();
        try (InputStream is = assetManager.open(fileName); JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!AREA_KEYS.equals(reader.nextName())) {
                    reader.skipValue();
                }
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String values = reader.nextName();
                        if (DEFAULT.equals(values)) {
                            result.put(key, reader.nextBoolean());
                        } else if (VALUES.equals(values)) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String value = reader.nextName();
                                result.put(key + " " + value, reader.nextBoolean());
                            }
                            reader.endObject();
                        }
                    }
                    reader.endObject();
                }
                reader.endObject();
            }
            reader.endObject();
            Log.d(DEBUG_TAG, "Found " + result.size() + " entries.");
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "Reading " + fileName + " " + e.getMessage());
        }
        return result;
    }

    /**
     * Check if a tag implies area semantics
     * 
     * @param key the key
     * @param value the value
     * @return true if the tag implies area semantics
     */
    public boolean isImpliedArea(@NonNull String key, @NonNull String value) {
        Boolean result = tagMap.get(key + " " + value);
        if (result != null) {
            return result;
        }
        result = tagMap.get(key);
        return result != null && result;
    }

    /**
     * Check if a set of tags implies area semantics
     * 
     * @param tags a Map containing the tags
     * @return true if the tags implies area semantics
     */
    public boolean isImpliedArea(@NonNull Map<String, String> tags) {
        for (Entry<String, String> tag : tags.entrySet()) {
            if (isImpliedArea(tag.getKey(), tag.getValue())) {
                return true;
            }
        }
        return false;
    }
}
