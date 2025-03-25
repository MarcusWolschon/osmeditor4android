package io.vespucci.nsi;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.osm.Tags;
import io.vespucci.util.SearchIndexUtils;
import io.vespucci.util.collections.MultiHashMap;

/**
 * Support for the name suggestion index see https://github.com/simonpoole/name-suggestion-index
 * 
 * Current supports v6 format
 * 
 * @author simon
 *
 */
public class Names {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Names.class.getSimpleName().length());
    private static final String DEBUG_TAG = Names.class.getSimpleName().substring(0, TAG_LEN);

    // besides country codes, the NSI uses UN M49 numeric values, see https://en.wikipedia.org/wiki/UN_M49 however only
    // 001 seems to be actually used
    private static final String UN_M49_WHOLE_WORLD = "001";

    private static final String TAGS_FIELD         = "tags";
    private static final String EXCLUDE_FIELD      = "exclude";
    private static final String INCLUDE_FIELD      = "include";
    private static final String LOCATION_SET_FIELD = "locationSet";
    private static final String DISPLAY_NAME_FIELD = "displayName";
    private static final String ITEMS_FIELD        = "items";
    private static final String PROPERTIES_FIELD   = "properties";
    private static final String NSI_FIELD          = "nsi";

    private static final String CATEGORIES_FILE = "categories.json";
    private static final String NSI_FILE        = "name-suggestions.min.json";

    private static final List<String> AMENITY_VALUES_TO_REMOVE = Collections
            .unmodifiableList(Arrays.asList(Tags.VALUE_ATM, Tags.VALUE_VENDING_MACHINE, Tags.VALUE_PAYMENT_TERMINAL));

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

    /**
     * Container class for a name and the associated tags
     * 
     * @author simon
     *
     */
    public class NameAndTags implements Comparable<NameAndTags> {
        private final String       name;
        private final int          count;
        private final List<String> includeRegions;
        private final List<String> excludeRegions;
        final TagMap               tags;

        /**
         * Construct a new instance
         * 
         * @param name the value for the name tag
         * @param tags associated tags
         * @param count the times this establishment was found, works as a proxy for importance, NSI V6 doesn't support
         *            this anymore
         * @param includeRegions regions this is applicable to
         * @param excludeRegions regions this is not applicable to
         */
        public NameAndTags(@NonNull String name, @NonNull TagMap tags, @NonNull int count, @Nullable List<String> includeRegions,
                @Nullable List<String> excludeRegions) {
            this.name = name;
            this.tags = tags;
            this.count = count;
            this.includeRegions = includeRegions;
            this.excludeRegions = excludeRegions;
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

        /**
         * Check if this entry is in use or not in use in a specific region
         * 
         * @param currentRegions the list of regions to check for, null == any region
         * @return true if the entry is appropriate for the region
         */
        public boolean inUseIn(@Nullable List<String> currentRegions) {
            if (currentRegions == null) {
                return true;
            }
            boolean inUse = false;
            if (includeRegions != null) {
                for (String current : currentRegions) {
                    if (includeRegions.contains(current)) {
                        inUse = true;
                        break;
                    }
                }
            } else {
                inUse = true;
            }
            if (excludeRegions != null) {
                for (String current : currentRegions) {
                    if (excludeRegions.contains(current)) {
                        inUse = false;
                        break;
                    }
                }
            }
            return inUse;
        }

        @Override
        public int compareTo(@NonNull NameAndTags another) {
            if (another.name.equals(name)) {
                if (getCount() > another.getCount()) {
                    return +1;
                } else if (getCount() < another.getCount()) {
                    return -1;
                }
                // more tags is better
                if (tags.size() > another.tags.size()) {
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
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NameAndTags)) {
                return false;
            }
            final NameAndTags other = (NameAndTags) obj;
            return name.equals(other.name) && tags.equals(other.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, tags);
        }
    }

    private static MultiHashMap<String, NameAndTags> nameList       = new MultiHashMap<>(false); // names -> multiple
                                                                                                 // entries
    private static MultiHashMap<String, NameAndTags> tags2namesList = new MultiHashMap<>(false); // tagmap -A multiple
                                                                                                 // entries
    private static MultiHashMap<String, String>      categories     = new MultiHashMap<>(false);

    private static boolean ready = false;

    /**
     * Construct a new instance of the data structure holding names and tags
     * 
     * The contents are currently read from a hardwired file as the
     * 
     * @param ctx an Android Context
     */
    public Names(@NonNull Context ctx) {
        synchronized (nameList) {
            if (!ready) {
                Log.d(DEBUG_TAG, "Parsing configuration files");
                AssetManager assetManager = ctx.getAssets();
                readNSI(assetManager);
                readCategories(assetManager);
                ready = true;
            }
        }
    }

    /**
     * Read the NSI configuration from assets
     * 
     * @param assetManager an AssetManager instance
     */
    private void readNSI(@NonNull AssetManager assetManager) {
        try (InputStream is = assetManager.open(NSI_FILE); JsonReader reader = new JsonReader(new InputStreamReader(is))) {
            reader.beginObject(); // top level
            while (reader.hasNext()) {
                if (NSI_FIELD.equals(reader.nextName())) {
                    reader.beginObject(); // entries
                    while (reader.hasNext()) {
                        reader.nextName();
                        reader.beginObject(); // entry
                        while (reader.hasNext()) {
                            String jsonName = reader.nextName();
                            switch (jsonName) {
                            case PROPERTIES_FIELD:
                                reader.skipValue();
                                break;
                            case ITEMS_FIELD:
                                reader.beginArray(); // item
                                while (reader.hasNext()) {
                                    reader.beginObject();
                                    String name = null;
                                    List<String> includeRegions = null;
                                    List<String> excludeRegions = null;
                                    TagMap tags = new TagMap();
                                    while (reader.hasNext()) {
                                        String field = reader.nextName();
                                        switch (field) {
                                        case DISPLAY_NAME_FIELD:
                                            name = reader.nextString();
                                            break;
                                        case LOCATION_SET_FIELD:
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                switch (reader.nextName()) {
                                                case INCLUDE_FIELD:
                                                    includeRegions = readLocationStringArray(reader);
                                                    break;
                                                case EXCLUDE_FIELD:
                                                    excludeRegions = readLocationStringArray(reader);
                                                    break;
                                                default:
                                                    reader.skipValue();
                                                }
                                            }
                                            reader.endObject();
                                            break;
                                        case TAGS_FIELD:
                                            readTags(reader, tags);
                                            break;
                                        default:
                                            reader.skipValue();
                                            break;
                                        }
                                    } // item
                                    reader.endObject();
                                    if (name != null) {
                                        NameAndTags entry = new NameAndTags(name, tags, 1, includeRegions, excludeRegions);
                                        nameList.add(name, entry);
                                        tags2namesList.add(tags.toString(), entry);
                                    }
                                } // items
                                reader.endArray();
                                break;
                            default:
                                reader.skipValue();
                                break;
                            }
                        }
                        reader.endObject(); // entry
                    }
                    reader.endObject(); // entries
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject(); // top level
        } catch (IOException | IllegalStateException e) {
            Log.e(DEBUG_TAG, "Got exception reading " + NSI_FILE + " " + e.getMessage());
        }
    }

    /**
     * Read and filter tags
     * 
     * @param reader the JsonReader
     * @param tags the map to save the tags in
     * @throws IOException if reading or parsing fails
     */
    private void readTags(@NonNull JsonReader reader, @NonNull TagMap tags) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String k = reader.nextName();
            if (!Tags.KEY_BRAND_WIKIPEDIA.equals(k) && !Tags.KEY_BRAND_WIKIDATA.equals(k)) {
                tags.put(k, reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject(); // tags
        // remove bogus name tags
        String name = tags.get(Tags.KEY_NAME);
        String brand = tags.get(Tags.KEY_BRAND);
        if (name != null && name.equals(brand)) {
            String amenity = tags.get(Tags.KEY_AMENITY);
            if (amenity != null && AMENITY_VALUES_TO_REMOVE.contains(amenity)) {
                for (String nameTag : Tags.I18N_KEYS) {
                    tags.remove(nameTag);
                }
            }
        }
    }

    /**
     * Read the category configuration from assets
     * 
     * @param assetManager an AssetManager instance
     */
    private void readCategories(AssetManager assetManager) {
        try (InputStream is = assetManager.open(CATEGORIES_FILE); JsonReader reader = new JsonReader(new InputStreamReader(is));) {
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
        } catch (IOException | IllegalStateException e) {
            Log.d(DEBUG_TAG, "Got exception reading " + CATEGORIES_FILE + " " + e.getMessage());
        }
    }

    /**
     * Read a JsonArray of string in to a String[]
     * 
     * @param reader the JsonReader
     * @return a String[] with the JSON strings
     * @throws IOException on IO and parse errors
     */
    @Nullable
    private List<String> readLocationStringArray(@NonNull JsonReader reader) throws IOException {
        boolean valid = true;
        List<String> result = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            if (reader.peek() == JsonToken.STRING) {
                String code = reader.nextString().toUpperCase(Locale.US);
                if (UN_M49_WHOLE_WORLD.equals(code)) {
                    valid = false;
                } else {
                    result.add(code);
                }
            } else {
                // we currently don't support coordinates with radius
                reader.skipValue();
                valid = false;
            }
        }
        reader.endArray();
        return valid ? result : null;
    }

    /**
     * Given a set of tags determine the names and tags that could be appropriate
     * 
     * @param tags a SortedMap with the existing tags
     * @param regions country or country subdivision code
     * @return a List of possibly appropriate entries
     */
    @NonNull
    public List<NameAndTags> getNames(@NonNull SortedMap<String, String> tags, @Nullable List<String> regions) {
        // remove irrelevant tags, TODO refine
        TagMap tm = new TagMap();
        String v = tags.get(Tags.KEY_AMENITY);
        if (v != null) {
            tm.put(Tags.KEY_AMENITY, v);
        } else {
            v = tags.get(Tags.KEY_SHOP);
            if (v != null) {
                tm.put(Tags.KEY_SHOP, v);
            } else {
                v = tags.get(Tags.KEY_TOURISM);
                if (Tags.VALUE_HOTEL.equals(v) || Tags.VALUE_MOTEL.equals(v)) {
                    tm.put(Tags.KEY_TOURISM, v);
                } else {
                    // return the whole list
                    return getNames(regions);
                }
            }
        }

        // filter on the tags
        List<NameAndTags> result = new ArrayList<>();

        String origTagKey = tm.toString();

        for (String key : tags2namesList.getKeys()) {
            if (key.contains(origTagKey)) {
                for (NameAndTags nt : tags2namesList.get(key)) {
                    if (nt.inUseIn(regions)) {
                        result.add(nt);
                    }
                }
            }
        }

        Set<String> seen = new TreeSet<>();
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
                                    if (nt.inUseIn(regions)) {
                                        result.add(nt);
                                    }
                                }
                            }
                        }
                        seen.add(catTagKey);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get all entries
     * 
     * @return a List containing all NameAndTags objects
     */
    @NonNull
    private List<NameAndTags> getNames() {
        List<NameAndTags> result = new ArrayList<>();
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
     * @param regions if an entry is region specific only return it if it is in use in region
     * @return a List of NameAndTags objects
     */
    @NonNull
    private List<NameAndTags> getNames(@Nullable List<String> regions) {
        List<NameAndTags> result = new ArrayList<>();
        for (String n : nameList.getKeys()) {
            for (NameAndTags nt : nameList.get(n)) {
                if (nt.inUseIn(regions)) {
                    result.add(nt);
                }
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
        List<NameAndTags> names = getNames();
        for (NameAndTags nat : names) {
            result.add(SearchIndexUtils.normalize(nat.getName()), nat);
        }
        return result;
    }
}
