package io.vespucci.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Container for OSMOSE API 0.3 meta information
 * 
 * Ignores categories and other stuff we don't need
 * 
 * @author simon
 *
 */
public class OsmoseMeta implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = OsmoseMeta.class.getSimpleName().substring(0, Math.min(23, OsmoseMeta.class.getSimpleName().length()));

    private static final String ITEM_KEY       = "item";
    private static final String RESOURCE_KEY   = "resource";
    private static final String DETAIL_KEY     = "detail";
    private static final String FIX_KEY        = "fix";
    private static final String TITLE_KEY      = "title";
    private static final String CLASS_KEY      = "class";
    private static final String ITEMS_KEY      = "items";
    private static final String CATEGORIES_KEY = "categories";
    private static final String AUTO_KEY       = "auto";

    class OsmoseClass implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        int                       id;
        String                    title;
        String                    detail;
        String                    fix;
        String                    resource;

        /**
         * Return a slightly formated version of detail + fix
         * 
         * @return a String containing detail and fix
         */
        @Nullable
        public String getHelpText() {
            String result = null;
            if (detail != null) {
                result = detail;
            }
            if (fix != null) {
                if (result != null) {
                    return result + "\n\n" + fix;
                } else {
                    return fix;
                }
            }
            return result;
        }

        /**
         * Check if we have any additional text to display
         * 
         * @return true if we can display something
         */
        public boolean hasHelpText() {
            return detail != null || fix != null;
        }
    }

    class Item implements Serializable {
        /**
         * 
         */
        private static final long         serialVersionUID = 1L;
        String                            id;
        private Map<Integer, OsmoseClass> classes          = new HashMap<>();
    }

    private Map<String, Item> items = new HashMap<>();

    /**
     * Parse an InputStream containing Osmose task data
     * 
     * @param is the InputString
     * @throws IOException for JSON reading issues
     * @throws NumberFormatException if a number conversion fails
     */
    public void parse(@NonNull InputStream is) throws IOException, NumberFormatException {
        Map<String, Item> tempItems = new HashMap<>();
        try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
            // key object
            String key = null;
            reader.beginObject();
            while (reader.hasNext()) {
                key = reader.nextName(); //
                if (CATEGORIES_KEY.equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            key = reader.nextName(); //
                            if (ITEMS_KEY.equals(key)) {
                                reader.beginArray();
                                while (reader.hasNext()) {
                                    reader.beginObject();
                                    Item item = new Item();
                                    while (reader.hasNext()) {
                                        key = reader.nextName(); //
                                        if (CLASS_KEY.equals(key)) {
                                            reader.beginArray();
                                            while (reader.hasNext()) {
                                                OsmoseClass oc = new OsmoseClass();
                                                while (reader.hasNext()) {
                                                    reader.beginObject();
                                                    while (reader.hasNext()) {
                                                        key = reader.nextName(); //
                                                        switch (key) {
                                                        case TITLE_KEY:
                                                            oc.title = getAutoString(reader);
                                                            break;
                                                        case CLASS_KEY:
                                                            oc.id = reader.nextInt();
                                                            break;
                                                        case FIX_KEY:
                                                            oc.fix = getAutoString(reader);
                                                            break;
                                                        case DETAIL_KEY:
                                                            oc.detail = getAutoString(reader);
                                                            break;
                                                        case RESOURCE_KEY:
                                                            oc.resource = getString(reader);
                                                            break;
                                                        default:
                                                            reader.skipValue();
                                                        }
                                                    }
                                                    reader.endObject();
                                                }
                                                item.classes.put(oc.id, oc);
                                            }
                                            reader.endArray();
                                        } else if (ITEM_KEY.equals(key)) {
                                            item.id = reader.nextString();
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    tempItems.put(item.id, item);
                                    reader.endObject();
                                }
                                reader.endArray();
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    }
                    reader.endArray();
                }
            }
            reader.endObject();
            for (Entry<String, Item> itemEntry : tempItems.entrySet()) {
                Item existingItem = items.get(itemEntry.getKey());
                if (existingItem != null) {
                    for (Entry<Integer, OsmoseClass> classEntry : itemEntry.getValue().classes.entrySet()) {
                        if (!existingItem.classes.containsKey(classEntry.getKey())) {
                            existingItem.classes.put(classEntry.getKey(), classEntry.getValue());
                        }
                    }
                } else {
                    items.put(itemEntry.getKey(), itemEntry.getValue());
                }
            }
        } catch (IOException | IllegalStateException | NumberFormatException ex) {
            Log.d(DEBUG_TAG, "Parse error, ignoring " + ex);
        }
    }

    /**
     * Get a string for language "auto", checking for JSON null values
     * 
     * @param reader the JsonReader
     * @return the value for auto
     * @throws IOException on parser errors etc
     */
    @Nullable
    static String getAutoString(@NonNull JsonReader reader) throws IOException {
        String result = null;
        JsonToken token = reader.peek();
        if (JsonToken.NULL.equals(token)) {
            reader.skipValue();
        } else {
            reader.beginObject();
            while (reader.hasNext()) {
                if (AUTO_KEY.equals(reader.nextName())) {
                    result = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        return result;
    }

    /**
     * Get a string checking for JSON null values
     * 
     * @param reader the JsonReader
     * @return get a string value from the reader
     * @throws IOException on parser errors etc
     */
    @Nullable
    private String getString(@NonNull JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        if (JsonToken.NULL.equals(token)) {
            reader.skipValue();
            return null;
        } else {
            return reader.nextString();
        }
    }

    /**
     * Get the data for a specific class
     * 
     * @param itemId the item id
     * @param classId the class id
     * @return an OsmoseClass or null
     */
    @Nullable
    public OsmoseClass getOsmoseClass(String itemId, int classId) {
        Item item = items.get(itemId);
        if (item != null) {
            return item.classes.get(classId);
        }
        return null;
    }
}
