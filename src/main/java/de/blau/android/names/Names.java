package de.blau.android.names;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.osm.Tags;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Support for the name suggestion index see https://github.com/simonpoole/name-suggestion-index
 * 
 * @author simon
 *
 */
public class Names {
    static final String DEBUG_TAG = "Names";

    public class TagMap extends TreeMap<String, String> {

        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : this.entrySet()) {
                builder.append(entry.getKey().replace("|", " ") + "=" + entry.getValue() + "|");
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
    }

    public class NameAndTags implements Comparable<NameAndTags> {
        private final String name;
        private final int    count;
        private final String region;
        final TagMap         tags;

        public NameAndTags(String name, TagMap tags, int count, String region) {
            this.name = name;
            this.tags = tags;
            this.count = count;
            this.region = region;
        }

        @Override
        public String toString() {
            return getName() + " (" + tags.toString() + ")";
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the tags
         */
        public TagMap getTags() {
            return tags;
        }

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }

        @Override
        public int compareTo(@NonNull NameAndTags another) {
            if (another.name.equals(name)) {
                if (getCount() > ((NameAndTags) another).getCount()) {
                    return +1;
                } else if (getCount() < another.getCount()) {
                    return -1;
                }
                // more tags is better
                if (tags.size() > ((NameAndTags) another).tags.size()) {
                    return +1;
                } else if (tags.size() < another.tags.size()) {
                    return -1;
                }
                return 0;
            }
            return name.compareTo(another.name);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NameAndTags)) {
                return false;
            }
            return name.equals(((NameAndTags) obj).name) && tags.equals(((NameAndTags) obj).tags);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 37 * result + (name == null ? 0 : name.hashCode());
            result = 37 * result + (tags == null ? 0 : tags.hashCode());
            return result;
        }
    }

    private static MultiHashMap<String, NameAndTags> nameList       = new MultiHashMap<>(false); // names -> multiple
                                                                                                 // entries
    private static MultiHashMap<String, NameAndTags> tags2namesList = new MultiHashMap<>(false); // tagmap -A multiple
                                                                                                 // entries
    private static MultiHashMap<String, String>      categories     = new MultiHashMap<>(false);

    private static boolean ready = false;

    public Names(Context ctx) {
        synchronized (nameList) {

            if (!ready) {
                Log.d(DEBUG_TAG, "Parsing configuration files");

                AssetManager assetManager = ctx.getAssets();
                try {
                    InputStream is = assetManager.open("name-suggestions.min.json");
                    JsonReader reader = new JsonReader(new InputStreamReader(is));

                    try {
                        try {
                            // key object
                            String key = null;
                            reader.beginObject();
                            while (reader.hasNext()) {
                                key = reader.nextName(); // amenity, shop
                                // value object
                                String value = null;
                                reader.beginObject();
                                while (reader.hasNext()) { // restaurant, fast_food, ....
                                    value = reader.nextName();
                                    // name object
                                    String name = null;
                                    int count = 0;
                                    String region = null;
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        name = reader.nextName(); // name of estabishment
                                        reader.beginObject();
                                        TagMap secondaryTags = null; // any extra tags store here
                                        while (reader.hasNext()) {
                                            String jsonName = reader.nextName();
                                            switch (jsonName) {
                                            case "count":
                                                count = reader.nextInt();
                                                break;
                                            case "region":
                                                region = reader.nextString();
                                                break;
                                            case "tags":
                                                reader.beginObject();
                                                while (reader.hasNext()) {
                                                    secondaryTags = new TagMap();
                                                    secondaryTags.put(reader.nextName(), reader.nextString());
                                                }
                                                reader.endObject(); // tags
                                                break;
                                            default:
                                                reader.skipValue();
                                                break;
                                            }
                                        }
                                        reader.endObject(); // name

                                        // add to lists here

                                        TagMap tags = new TagMap();
                                        tags.put(key, value);
                                        if (secondaryTags != null) {
                                            tags.putAll(secondaryTags);
                                        }
                                        NameAndTags entry = new NameAndTags(name, tags, count, region);
                                        nameList.add(name, entry);
                                        tags2namesList.add(tags.toString(), entry);
                                    }
                                    reader.endObject(); // value
                                }
                                reader.endObject(); // key
                            }
                            reader.endObject();
                        } catch (IOException e) {
                            Log.e(DEBUG_TAG, "Got exception reading name-suggestions.min.json " + e.getMessage());
                        }
                    } finally {
                        SavingHelper.close(reader);
                        SavingHelper.close(is);
                    }
                    try {
                        is = assetManager.open("categories.json");
                        reader = new JsonReader(new InputStreamReader(is));
                        try {
                            String category = null;
                            reader.beginObject();
                            while (reader.hasNext()) {
                                category = reader.nextName();
                                String poiType = null;
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    poiType = reader.nextName();
                                    reader.beginArray();
                                    while (reader.hasNext()) {
                                        categories.add(category, poiType + "=" + reader.nextString());
                                    }
                                    reader.endArray();
                                }
                                reader.endObject();
                            }
                            reader.endObject();
                        } catch (IOException e) {
                            Log.d(DEBUG_TAG, "Got exception reading categories.json " + e.getMessage());
                        }
                    } finally {
                        SavingHelper.close(reader);
                        SavingHelper.close(is);
                    }
                } catch (IOException e) {
                    Log.d(DEBUG_TAG, "Got exception " + e.getMessage());
                }

                ready = true;
            }
        }
    }

    public Collection<NameAndTags> getNames(SortedMap<String, String> tags) {
        // remove irrelevant tags, TODO refine
        TagMap tm = new TagMap();
        String v = tags.get(Tags.KEY_AMENITY);
        if (v != null) {
            tm.put(Tags.KEY_AMENITY, v);
            // Log.d("Names","filtering for amenity="+v);
        } else {
            v = tags.get(Tags.KEY_SHOP);
            if (v != null) {
                tm.put(Tags.KEY_SHOP, v);
                // Log.d("Names","filtering for shop="+v);
            }
        }
        if (tm.isEmpty()) {
            return getNames();
        }

        Collection<NameAndTags> result = new ArrayList<>();

        String origTagKey = tm.toString();

        for (String key : tags2namesList.getKeys()) {
            if (key.contains(origTagKey)) {
                for (NameAndTags nt : tags2namesList.get(key)) {
                    result.add(nt);
                }
            }
        }

        TreeSet<String> seen = new TreeSet<>();
        // check categories for similar tags and add names from them too
        seen.add(origTagKey); // skip stuff we've already added
        for (String category : categories.getKeys()) { // loop over categories
            Set<String> set = categories.get(category);
            if (set.contains(origTagKey)) {
                for (String catTagKey : set) { // loop over categories content
                    if (!seen.contains(catTagKey)) { // suppress dups
                        for (String key : tags2namesList.getKeys()) {
                            if (key.contains(catTagKey)) {
                                for (NameAndTags nt : tags2namesList.get(key)) {
                                    result.add(nt);
                                }
                            }
                        }
                        seen.add(catTagKey);
                    }
                }
            }
        }
        // Log.d("Names","getNames result " + result.size());
        return result;
    }

    /**
     * Get all entries
     * 
     * @return a Collection containing all NameAndTags objects
     */
    @NonNull
    private Collection<NameAndTags> getNames() {
        Collection<NameAndTags> result = new ArrayList<>();
        for (String n : nameList.getKeys()) {
            for (NameAndTags nt : nameList.get(n)) {
                result.add(nt);
            }
        }
        return result;
    }

    /**
     * Get all entries valid in a specific region
     * 
     * @param region if an entry is region specific only return it if it is in use in region
     * @return a Collection of NameAndTags objects
     */
    @NonNull
    private Collection<NameAndTags> getNames(@Nullable String region) {
        Collection<NameAndTags> result = new ArrayList<>();
        for (String n : nameList.getKeys()) {
            for (NameAndTags nt : nameList.get(n)) {
                if (region != null && nt.region != null && !nt.region.contains(region)) {
                    continue;
                }
                result.add(nt);
            }
        }
        return result;
    }

    /**
     * Return a mapping from normalized name values to entries
     * 
     * @return a map from normalized names to NameAndTags objects
     */
    public MultiHashMap<String, NameAndTags> getSearchIndex() {
        MultiHashMap<String, NameAndTags> result = new MultiHashMap<>();
        Collection<NameAndTags> names = getNames();
        for (NameAndTags nat : names) {
            result.add(SearchIndexUtils.normalize(nat.getName()), nat);
        }
        return result;
    }
}
