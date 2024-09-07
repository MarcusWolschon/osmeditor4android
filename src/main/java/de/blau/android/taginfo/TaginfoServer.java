package de.blau.android.taginfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.osm.Server;
import de.blau.android.util.StringWithDescription;

/**
 * Interface to the taginfo API (version 4) search calls
 * 
 * @author Simon Poole
 * @see <a href="https://taginfo.openstreetmap.org/taginfo/apidoc">Taginfo API</A>
 *
 */
public final class TaginfoServer {

    protected static final String DEBUG_TAG = TaginfoServer.class.getSimpleName().substring(0, Math.min(23, TaginfoServer.class.getSimpleName().length()));

    private static final String DATA_NAME        = "data";
    private static final String KEY_NAME         = "key";
    private static final String VALUE_NAME       = "value";
    private static final String COUNT_ALL_NAME   = "count_all";
    private static final String COUNT_NAME       = "count";
    private static final String DESCRIPTION_NAME = "description";
    private static final String OTHER_KEY        = "other_key";
    private static final String OTHER_VALUE      = "other_value";
    public static final String  RELATIONS        = "relations";
    public static final String  WAYS             = "ways";
    public static final String  NODES            = "nodes";

    private static final String PAGE_1      = "&page=1";
    private static final String SORT_PARAMS = "&sortname=count_all&sortorder=desc";

    /**
     * Private constructor
     */
    private TaginfoServer() {
        // don't instantiate
    }

    /**
     * A search result as return by the search API calls
     * 
     * count may be 0 on calls that do not return a count
     * 
     */
    public static class SearchResult {

        private String key;
        private String value;
        private int    count = 0;

        /**
         * Construct a new SearchResult from JSON
         * 
         * @param reader the JsonReader
         * @param lang the preferred language
         * @throws IOException if reading JSON fails
         */
        SearchResult(@NonNull JsonReader reader, @Nullable String lang) throws IOException {
            reader.beginObject();
            while (reader.hasNext()) {
                String jsonName = reader.nextName();
                switch (jsonName) {
                case KEY_NAME:
                    key = reader.nextString();
                    break;
                case VALUE_NAME:
                    value = reader.nextString();
                    break;
                case COUNT_ALL_NAME:
                    count = reader.nextInt();
                    break;
                default:
                    reader.skipValue();
                    break;
                }
            }
            reader.endObject();
        }

        @Override
        public String toString() {
            return key + "=" + value + " (" + count + ")";
        }

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }
    }

    public static class WikiPageResult {
        private static final String TAGS_COMBINATON_FIELD = "tags_combination";
        private static final String ON_RELATION_FIELD     = "on_relation";
        private static final String ON_AREA_FIELD         = "on_area";
        private static final String ON_WAY_FIELD          = "on_way";
        private static final String ON_NODE_FIELD         = "on_node";
        private static final String TITLE_FIELD           = "title";
        private static final String LANG_FIELD            = "lang";
        private static final String EN                    = "en";

        private boolean      onNode        = false;
        private boolean      onWay         = false;
        private boolean      onArea        = false;
        private boolean      onRelation    = false;
        private String       description   = null;
        private String       descriptionEN = null;
        private String       title         = null;
        private String       titleEN       = null;
        private String       titleOther    = null;
        private List<String> combinations  = new ArrayList<>();

        /**
         * Construct a new WikiPageResult from JSON
         * 
         * @param reader the JsonReader
         * @param lang the preferred language
         * @throws IOException if reading JSON fails
         */
        WikiPageResult(@NonNull JsonReader reader, String lang) throws IOException {

            String tempLang = null;
            boolean tempOnNode = false;
            boolean tempOnWay = false;
            boolean tempOnArea = false;
            boolean tempOnRelation = false;
            String tempTitle = null;
            String tempDescription = null;
            List<String> tempCombinations = new ArrayList<>();

            reader.beginObject();
            while (reader.hasNext()) {
                String jsonName = reader.nextName();
                if (DATA_NAME.equals(jsonName)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        tempCombinations.clear();
                        while (reader.hasNext()) {
                            jsonName = reader.nextName();
                            switch (jsonName) {
                            case LANG_FIELD:
                                tempLang = reader.nextString();
                                break;
                            case TITLE_FIELD:
                                tempTitle = reader.nextString();
                                break;
                            case DESCRIPTION_NAME:
                                tempDescription = reader.nextString();
                                break;
                            case ON_NODE_FIELD:
                                tempOnNode = reader.nextBoolean();
                                break;
                            case ON_WAY_FIELD:
                                tempOnWay = reader.nextBoolean();
                                break;
                            case ON_AREA_FIELD:
                                tempOnArea = reader.nextBoolean();
                                break;
                            case ON_RELATION_FIELD:
                                tempOnRelation = reader.nextBoolean();
                                break;
                            case TAGS_COMBINATON_FIELD:
                                reader.beginArray();
                                while (reader.hasNext()) {
                                    tempCombinations.add(reader.nextString());
                                }
                                reader.endArray();
                                break;
                            default:
                                reader.skipValue();
                                break;
                            }
                        }
                        reader.endObject();
                        if (EN.equals(tempLang)) {
                            onNode = tempOnNode;
                            onWay = tempOnWay;
                            onArea = tempOnArea;
                            onRelation = tempOnRelation;
                            combinations.addAll(tempCombinations);
                            descriptionEN = tempDescription;
                            titleEN = tempTitle;
                        } else if (lang.equals(tempLang)) {
                            description = tempDescription;
                            title = tempTitle;
                        }
                        titleOther = tempTitle;
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }

        /**
         * @return the onNode
         */
        public boolean isOnNode() {
            return onNode;
        }

        /**
         * @return the onWay
         */
        public boolean isOnWay() {
            return onWay;
        }

        /**
         * @return the onArea
         */
        public boolean isOnArea() {
            return onArea;
        }

        /**
         * @return the onRelation
         */
        public boolean isOnRelation() {
            return onRelation;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return the descriptionEN
         */
        public String getDescriptionEN() {
            return descriptionEN;
        }

        /**
         * @return the title
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return the titleEN
         */
        public String getTitleEN() {
            return titleEN;
        }

        /**
         * @return the titleOther
         */
        public String getTitleOther() {
            return titleOther;
        }

        /**
         * @return the combinations
         */
        public List<String> getCombinations() {
            return combinations;
        }

        @Override
        public String toString() {
            return titleEN + " / " + title + " / " + titleOther;
        }
    }

    public static class ValueResult extends StringWithDescription {
        private static final long serialVersionUID = 1L;

        private int count;

        /**
         * Construct a new ValueResult
         * 
         * @param value the value
         * @param description optional description
         * @param count how many times this value occurs
         */
        public ValueResult(@NonNull String value, @Nullable String description, int count) {
            super(value, description);
            this.count = count;
        }

        /**
         * Get a new ValueResult from JSON
         * 
         * @param reader the JsonReader
         * @param lang the preferred language
         * @return a new ValueReader instance
         * @throws IOException if reading JSON fails
         */
        @NonNull
        public static ValueResult newValueResult(@NonNull JsonReader reader, @Nullable String lang) throws IOException {

            String tempValue = null;
            String tempDescription = null;
            int tempCount = 0;
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                    case VALUE_NAME:
                        tempValue = reader.nextString();
                        break;
                    case COUNT_NAME:
                        tempCount = reader.nextInt();
                        break;
                    case DESCRIPTION_NAME:
                        switch (reader.peek()) {
                        case STRING:
                            tempDescription = reader.nextString();
                            break;
                        default:
                            reader.skipValue();
                        }
                        break;
                    default:
                        reader.skipValue();
                        break;
                    }
                }
                reader.endObject();
            } catch (IllegalStateException isex) {
                throw new IOException(isex.getMessage());
            }
            if (tempValue == null) {
                throw new IOException("Input missing value");
            }
            return new ValueResult(tempValue, tempDescription, tempCount);
        }

        /**
         * @return the count
         */
        int getCount() {
            return count;
        }

        @Override
        public String toString() {
            String description = getDescription();
            return getValue() + (description != null && !"".equals(description) ? " / " + description : "") + " (" + count + ")";
        }
    }

    /**
     * Search for by key and value
     * 
     * @param context Android Context
     * @param server server url
     * @param key the key
     * @param value the value
     * @param maxResults maximum number of results
     * @return a List of SearchResults, or null is something seriously went wrong
     */
    @Nullable
    public static List<SearchResult> searchByKeyAndValue(@Nullable final Context context, @NonNull String server, @NonNull String key, @NonNull String value,
            int maxResults) {
        // https://taginfo.openstreetmap.org/api/4/search/by_key_and_value?query=%3Dresidential&page=1&rp=10&sortname=count_all&sortorder=desc
        String url = server + "api/4/search/by_key_and_value?query=" + key + "%3D" + value + maxResultParameters(maxResults) + SORT_PARAMS;
        return search(context, url);
    }

    /**
     * Get a String with the correct number of pages
     * 
     * @param maxResults the max number of results we want
     * @return a String with the parameters
     */
    private static String maxResultParameters(int maxResults) {
        return PAGE_1 + (maxResults != -1 ? "&rp=" + maxResults : "");
    }

    /**
     * Search for by value
     * 
     * @param context Android Context
     * @param server server url
     * @param value the value
     * @param maxResults maximum number of results
     * @return a List of SearchResults, or null is something seriously went wrong
     */
    @Nullable
    public static List<SearchResult> searchByValue(@Nullable final Context context, @NonNull String server, @NonNull String value, int maxResults) {
        // https://taginfo.openstreetmap.org/api/4/search/by_value?query=residential&page=1&rp=10&sortname=count_all&sortorder=desc
        String url = server + "api/4/search/by_value?query=" + value + maxResultParameters(maxResults) + SORT_PARAMS;
        return search(context, url);
    }

    /**
     * Search for a keyword
     * 
     * @param context Android Context
     * @param server server url
     * @param keyword the keyword
     * @param maxResults maximum number of results
     * @return a List of SearchResults, or null is something seriously went wrong
     */
    @Nullable
    public static List<SearchResult> searchByKeyword(@Nullable final Context context, @NonNull String server, @NonNull String keyword, int maxResults) {
        String url = server + "api/4/search/by_keyword?query=" + keyword + maxResultParameters(maxResults);
        return search(context, url);
    }

    /**
     * Search taginfo
     * 
     * @param context AndroidContext
     * @param url the url
     * @return a List of SearchResults, or null is something seriously went wrong
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static List<SearchResult> search(@Nullable final Context context, @NonNull String url) {
        ResultReader resultReader = new ResultReader() {

            @Override
            public Object read(JsonReader reader) throws IOException {

                List<SearchResult> result = new ArrayList<>();
                reader.beginObject();
                while (reader.hasNext()) {
                    if (DATA_NAME.equals(reader.nextName())) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            try {
                                SearchResult searchResult = new SearchResult(reader, null);
                                result.add(searchResult);
                            } catch (IOException e) {
                                Log.e(DEBUG_TAG, e.getMessage());
                            }
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                return result;
            }
        };
        return (List<SearchResult>) querySync(context, url, resultReader, null);
    }

    /**
     * Query a taginfo server for wikipage info on a specific tag
     * 
     * @param context Android Context
     * @param server server url
     * @param key the tag key
     * @param value the tag value
     * @param lang the preferred language
     * @param handler a handler to call after download
     * @return a WikiPageResult instance, or null is something seriously went wrong
     */
    @Nullable
    public static WikiPageResult wikiPage(@Nullable final Context context, @NonNull String server, @NonNull String key, @NonNull String value,
            final String lang, @Nullable final PostAsyncActionHandler handler) {
        final String url = server + "api/4/tag/wiki_pages?key=" + key + "&value=" + value;
        return (WikiPageResult) querySync(context, url, new ResultReader() {

            @Override
            public Object read(JsonReader reader) throws IOException {
                return new WikiPageResult(reader, lang);
            }
        }, null);
    }

    /**
     * Get a list of values for a key
     * 
     * @param context Android Context
     * @param server server url
     * @param key the key
     * @param maxResults maximum number of results
     * @return a List of ValueResults, or null is something seriously went wrong
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static List<ValueResult> keyValues(@Nullable final Context context, @NonNull String server, @NonNull String key, int maxResults) {
        String url = server + "api/4/key/values?key=" + key + maxResultParameters(maxResults) + SORT_PARAMS;
        return (List<ValueResult>) querySync(context, url, new ResultReader() {

            @Override
            public Object read(JsonReader reader) throws IOException {

                List<ValueResult> result = new ArrayList<>();
                reader.beginObject();
                while (reader.hasNext()) {
                    if (DATA_NAME.equals(reader.nextName())) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            try {
                                ValueResult valueResult = ValueResult.newValueResult(reader, null);
                                if (!"*".equals(valueResult.getValue())) { // we definitely never want to return *
                                    result.add(valueResult);
                                }
                            } catch (IOException e) {
                                Log.e(DEBUG_TAG, e.getMessage());
                            }
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                return result;
            }
        }, null);
    }

    /**
     * Get the count of the tag in the DB
     * 
     * @param context Android Context
     * @param server server url
     * @param key the tag key
     * @param value the tag value
     * @return a SearchResult instance containing the count, or null is something seriously went wrong
     */
    @Nullable
    public static SearchResult tagStats(@Nullable final Context context, @NonNull String server, @NonNull String key, @NonNull String value) {
        String url = server + "api/4/tags/list?tags=" + key + "%3D" + value;
        return (SearchResult) querySync(context, url, new ResultReader() {

            @Override
            public Object read(JsonReader reader) throws IOException {
                SearchResult result = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    if (DATA_NAME.equals(reader.nextName())) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            if (result == null) {
                                try {
                                    result = new SearchResult(reader, null);
                                } catch (IOException e) {
                                    Log.e(DEBUG_TAG, e.getMessage());
                                }
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                return result;
            }
        }, null);
    }

    /**
     * Retrieve tags that are used together with tag or key
     * 
     * @param context Android Context
     * @param server server url
     * @param key the tag key
     * @param value the tag value, if null combinations with the key will be returned
     * @param filter filter for element type
     * @param maxResults the maximum number of results to return
     * @return a List of tag values in the form key=value or just key, or null is something seriously went wrong
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static List<String> tagCombinations(@Nullable final Context context, @NonNull String server, @NonNull String key, @Nullable String value,
            @Nullable String filter, int maxResults) {
        String url = server + "api/4/tag/combinations?key=" + key + (value != null ? "&value=" + value : "") + (filter != null ? "&filter=" + filter : "")
                + maxResultParameters(maxResults) + "&sortname=together_count&sortorder=desc";
        return (List<String>) querySync(context, url, keyValueReader, null);
    }

    /**
     * Retrieve tags that are used together with tag or key
     * 
     * @param context Android Context
     * @param server server url
     * @param key the tag key
     * @param filter filter for element type
     * @param maxResults the maximum number of results to return
     * @return a List of tag values in the form key=value or just key, or null is something seriously went wrong
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static List<String> keyCombinations(@Nullable final Context context, @NonNull String server, @NonNull String key, @Nullable String filter,
            int maxResults) {
        String url = server + "api/4/key/combinations?key=" + key + (filter != null ? "&filter=" + filter : "") + maxResultParameters(maxResults)
                + "&sortname=together_count&sortorder=desc";
        return (List<String>) querySync(context, url, keyValueReader, null);
    }

    private static final ResultReader keyValueReader = new ResultReader() {

        @Override
        public Object read(JsonReader reader) throws IOException {
            List<String> result = new ArrayList<>();
            reader.beginObject();
            while (reader.hasNext()) {
                if (DATA_NAME.equals(reader.nextName())) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        try {
                            String otherKey = null;
                            String otherValue = null;
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String jsonName = reader.nextName();
                                switch (jsonName) {
                                case OTHER_KEY:
                                    otherKey = reader.nextString();
                                    break;
                                case OTHER_VALUE:
                                    otherValue = reader.nextString();
                                    break;
                                default:
                                    reader.skipValue();
                                    break;
                                }
                            }
                            reader.endObject();
                            if (otherValue == null) {
                                result.add(otherKey);
                            } else {
                                result.add(otherKey + "=" + otherValue);
                            }
                        } catch (IOException e) {
                            Log.e(DEBUG_TAG, e.getMessage());
                        }
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return result;
        }
    };

    /**
     * Query a taginfo server
     * 
     * @param context Android Context
     * @param url the url to query
     * @param resultReader the ResultReader instance to use
     * @param handler a handler to call after the download or null
     * @return an Object that has to be cast to the correct type, or null is something seriously went wrong
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static Object querySync(@Nullable final Context context, @NonNull final String url, @NonNull final ResultReader resultReader,
            @Nullable final PostAsyncActionHandler handler) {
        Log.d(DEBUG_TAG, "querying server for " + url);
        Object result = null;
        try (InputStream is = Server.openConnection(context, new URL(url), 1000, 1000); JsonReader reader = new JsonReader(new InputStreamReader(is));) {
            result = resultReader.read(reader);
            if (result != null && handler != null) {
                handler.onSuccess();
            }
            Log.d(DEBUG_TAG, "returning " + (result instanceof List ? ((List) result).size() : "1") + " results");
            return result;
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "querySync got exception " + e.getMessage());
        }
        return null;
    }
}
