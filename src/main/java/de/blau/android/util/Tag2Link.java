package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.OsmXml;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Class to hold tag2link entries, see https://github.com/JOSM/tag2link
 * 
 * @author simon
 *
 */
public class Tag2Link {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Tag2Link.class.getSimpleName().length());
    private static final String DEBUG_TAG = Tag2Link.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG2LINK_JSON = "tag2link.json";

    private static final String URL_NAME = "url";
    private static final String KEY_NAME = "key";

    private static final String KEY_PREFIX    = "Key:";
    private static final String PLACEHOLDER_1 = "$1";
    private static final String ENCODED_SPACE = "%20";

    private final MultiHashMap<String, String> linkMap;

    /**
     * Implicit assumption that the data will be short and that it is OK to read in synchronously which may not be true
     * any longer
     * 
     * @param context Android Context
     */
    public Tag2Link(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Initalizing");
        linkMap = getTagMap(context.getAssets(), TAG2LINK_JSON);
    }

    /**
     * Read a Json file from assets conforming to https://github.com/simonpoole/osm-area-tags/schema.json
     * 
     * @param assetManager an AssetManager
     * @param fileName the name of the file
     * @return a Map
     */
    @NonNull
    private MultiHashMap<String, String> getTagMap(@NonNull AssetManager assetManager, @NonNull String fileName) {
        MultiHashMap<String, String> result = new MultiHashMap<>(false, true);
        try (InputStream is = assetManager.open(fileName); JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginArray();
            while (reader.hasNext()) {
                String tagKey = null;
                String url = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                    case KEY_NAME:
                        tagKey = reader.nextString().replace(KEY_PREFIX, "");
                        break;
                    case URL_NAME:
                        url = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                    }
                }
                if (tagKey != null && url != null) {
                    result.add(tagKey, url);
                }
                reader.endObject();
            }
            reader.endArray();
            Log.d(DEBUG_TAG, "Found " + result.size() + " entries.");
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "Reading " + fileName + " " + e.getMessage());
        }
        return result;
    }

    /**
     * Get the 1st url
     * 
     * @param tagKey the tags key
     * @param value the tags value
     * @return an appropriate url or the original value
     */
    @Nullable
    public String get(@NonNull String tagKey, @NonNull String value) {
        Set<String> urlTemplates = linkMap.get(tagKey);
        if (!urlTemplates.isEmpty()) {
            try {
                return urlTemplates.iterator().next().replace(PLACEHOLDER_1, URLEncoder.encode(value, OsmXml.UTF_8).replace("+", ENCODED_SPACE));
            } catch (UnsupportedEncodingException e) {
                Log.e(DEBUG_TAG, e.getMessage());
            }
        }
        return value;
    }

    /**
     * Check if we have an entry for a key
     * 
     * @param tagKey the key
     * @return true if we have an entry
     */
    public boolean isLink(@NonNull String tagKey) {
        return linkMap.containsKey(tagKey);
    }
}
