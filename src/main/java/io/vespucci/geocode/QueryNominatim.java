package io.vespucci.geocode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.App;
import io.vespucci.geocode.Search.SearchResult;
import io.vespucci.osm.ViewBox;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.AdvancedPrefDatabase.Geocoder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class QueryNominatim extends Query {

    private static final String DEBUG_TAG = QueryNominatim.class.getSimpleName().substring(0, Math.min(23, QueryNominatim.class.getSimpleName().length()));

    private static final String DISPLAY_NAME_FIELD = "display_name";
    private static final String LON_FIELD          = "lon";
    private static final String LAT_FIELD          = "lat";
    private static final String OSM_ID_FIELD       = "osm_id";

    final boolean limitToBoundingBox;

    /**
     * Query a Nominatim geocoder
     * 
     * @param activity the calling FragmentActivity, if null no progress spinner will be shown
     * @param url URL for the specific instance of the geocoder
     * @param bbox a ViewBox to restrict the query to, if null the whole world will be considered
     * @param limitSearch if true limit search to bbox
     */
    public QueryNominatim(@Nullable FragmentActivity activity, @NonNull String url, @Nullable ViewBox bbox, boolean limitSearch) {
        super(activity, url, bbox);
        limitToBoundingBox = limitSearch;
    }

    @Override
    protected List<SearchResult> doInBackground(String query) {
        List<SearchResult> result = new ArrayList<>();
        Uri.Builder builder = Uri.parse(url).buildUpon().appendPath("search").appendQueryParameter("q", query);
        if (bbox != null) {
            String viewBoxCoordinates = bbox.getLeft() / 1E7D + "," + bbox.getBottom() / 1E7D + "," + bbox.getRight() / 1E7D + "," + bbox.getTop() / 1E7D;
            builder.appendQueryParameter("viewboxlbrt", viewBoxCoordinates);
            if (limitToBoundingBox) {
                builder.appendQueryParameter("bounded", "1");
            }
        }
        Uri uriBuilder = builder.appendQueryParameter("format", "jsonv2").build();

        String urlString = uriBuilder.toString();
        Log.d(DEBUG_TAG, "urlString: " + urlString);
        try {
            Request request = new Request.Builder().url(urlString).build();
            Response searchCallResponse = App.getHttpClient().newCall(request).execute();
            if (searchCallResponse.isSuccessful()) {
                try (ResponseBody responseBody = searchCallResponse.body(); InputStream inputStream = responseBody.byteStream()) {
                    if (inputStream != null) {
                        try (JsonReader reader = new JsonReader(new InputStreamReader(inputStream))) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                SearchResult searchResult = readNominatimResult(reader);
                                if (searchResult != null) {
                                    result.add(searchResult);
                                    Log.d(DEBUG_TAG, "received: " + searchResult.toString());
                                }
                            }
                            reader.endArray();
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "QueryNominatim got " + e.getMessage());
            connectionError(e.getMessage());
        }
        return result;
    }

    /**
     * Read a single Nominatim result
     * 
     * @param reader the JsonReader
     * @return a SearchResult object or null if reading failed
     */
    @Nullable
    private SearchResult readNominatimResult(@NonNull JsonReader reader) {
        SearchResult result = new SearchResult();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String jsonName = reader.nextName();
                switch (jsonName) {
                case LAT_FIELD:
                    result.setLat(reader.nextDouble());
                    break;
                case LON_FIELD:
                    result.setLon(reader.nextDouble());
                    break;
                case DISPLAY_NAME_FIELD:
                    result.displayName = reader.nextString();
                    break;
                case OSM_ID_FIELD:
                    result.setOsmId(reader.nextLong());
                    break;
                default:
                    reader.skipValue();
                    break;
                }
            }
            reader.endObject();
            return result;
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "readNominatimResult got " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get a URL for a Nominatim server
     * 
     * @param context an Android Context
     * @return the url or null
     */
    @Nullable
    public static String getNominatimUrl(@NonNull final Context context) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            final Geocoder[] geocoders = db.getActiveGeocoders();
            String url = null;
            for (Geocoder g : geocoders) {
                if (g.type == AdvancedPrefDatabase.GeocoderType.NOMINATIM) {
                    url = g.url;
                    break;
                }
            }
            return url;
        }
    }
}
