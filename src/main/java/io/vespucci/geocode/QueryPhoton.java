package io.vespucci.geocode;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.App;
import io.vespucci.geocode.Search.SearchResult;
import io.vespucci.osm.ViewBox;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.util.FileUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class QueryPhoton extends Query {
    private static final String DEBUG_TAG = QueryPhoton.class.getSimpleName().substring(0, Math.min(23, QueryPhoton.class.getSimpleName().length()));

    private static final String COUNTRY_FIELD      = "country";
    private static final String STATE_FIELD        = "state";
    private static final String CITY_FIELD         = "city";
    private static final String POSTCODE_FIELD     = "postcode";
    private static final String HOUSE_NUMBER_FIELD = "housenumber";
    private static final String STREET_FIELD       = "street";
    private static final String OSM_VALUE_FIELD    = "osm_value";
    private static final String OSM_KEY_FIELD      = "osm_key";
    private static final String NAME_FIELD         = "name";

    /**
     * Query a Photon geocoder
     * 
     * @param activity the calling FragmentActivity, if null no progress spinner will be shown
     * @param url URL for the specific instance of the geocoder
     * @param bbox a ViewBox to restrict the query to, if null the whole world will be considered
     */
    public QueryPhoton(@Nullable FragmentActivity activity, @NonNull String url, @Nullable ViewBox bbox) {
        super(activity, url, bbox);
    }

    @SuppressLint("NewApi") // StandardCharsets is desugared for APIs < 19.
    @Override
    protected List<SearchResult> doInBackground(String query) {
        List<SearchResult> result = new ArrayList<>();
        Uri.Builder builder = Uri.parse(url).buildUpon().appendPath("api").appendQueryParameter("q", query);
        if (bbox != null) {
            double lat = bbox.getCenterLat();
            double lon = (bbox.getLeft() + (bbox.getRight() - bbox.getLeft()) / 2D) / 1E7D;
            builder.appendQueryParameter("lat", Double.toString(lat));
            builder.appendQueryParameter("lon", Double.toString(lon));
        }
        builder.appendQueryParameter("limit", Integer.toString(10));
        Uri uriBuilder = builder.build();
        String urlString = uriBuilder.toString();
        Log.d(DEBUG_TAG, "urlString: " + urlString);
        try {
            Request request = new Request.Builder().url(urlString).build();
            Response searchCallResponse = App.getHttpClient().newCall(request).execute();
            if (searchCallResponse.isSuccessful()) {
                try (ResponseBody responseBody = searchCallResponse.body(); InputStream inputStream = responseBody.byteStream()) {
                    if (inputStream != null) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        FeatureCollection fc = FeatureCollection.fromJson(FileUtil.readToString(rd));
                        for (Feature f : fc.features()) {
                            SearchResult searchResult = readPhotonResult(f);
                            if (searchResult != null) {
                                result.add(searchResult);
                                Log.d(DEBUG_TAG, "received: " + searchResult.toString());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "QueryPhoton got " + e.getMessage());
            connectionError(e.getMessage());
        }
        return result;
    }

    /**
     * Read a single Photon result
     * 
     * @param f the GeoJson Feature to read
     * @return a SearchResult or null if the Feature couldn't be used
     */
    private SearchResult readPhotonResult(@NonNull Feature f) {
        SearchResult result = new SearchResult();
        try {
            JsonObject properties = f.properties();
            Geometry g = f.geometry();
            if (g instanceof Point) {
                Point p = (Point) g;
                result.setLat(p.latitude());
                result.setLon(p.longitude());
                StringBuilder sb = new StringBuilder();
                if (properties != null) {
                    appendString(properties, NAME_FIELD, sb);
                    sb.append("<small>");
                    JsonElement osmKey = properties.get(OSM_KEY_FIELD);
                    JsonElement osmValue = properties.get(OSM_VALUE_FIELD);
                    if (osmKey != null && osmValue != null) {
                        String key = osmKey.getAsString();
                        String value = osmValue.getAsString();
                        Map<String, String> tag = new HashMap<>();
                        tag.put(key, value);
                        PresetItem preset = Preset.findBestMatch(App.getCurrentPresets(activity), tag, null, null, false, null);
                        if (preset != null) {
                            sb.append("<br>[" + preset.getTranslatedName() + "]<br>");
                        } else {
                            sb.append("<br>[" + key + "=" + value + "]<br>");
                        }
                    }
                    appendAddress(properties, sb);
                    sb.append("</small>");
                }
                result.displayName = sb.toString();
                return result;
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "readPhotonResult got " + e.getMessage());
        }
        return null;
    }

    /**
     * Append the address from the query result to a StringBuilder
     * 
     * @param jsonObject the JsonObject holding the fields
     * @param sb the StringBuilder
     */
    private void appendAddress(@NonNull JsonObject jsonObject, @NonNull StringBuilder sb) {
        StringBuilder sb2 = new StringBuilder();
        JsonElement street = jsonObject.get(STREET_FIELD);
        if (street != null) {
            sb2.append(street.getAsString());
            JsonElement housenumber = jsonObject.get(HOUSE_NUMBER_FIELD);
            if (housenumber != null) {
                sb2.append(" " + housenumber.getAsString());
            }
        }
        appendString(jsonObject, POSTCODE_FIELD, sb2);
        appendString(jsonObject, CITY_FIELD, sb2);
        appendString(jsonObject, STATE_FIELD, sb2);
        appendString(jsonObject, COUNTRY_FIELD, sb2);
        if (sb2.length() > 0) {
            sb.append("\n");
            sb.append(sb2);
        }
    }

    /**
     * Append a JsonElement to a StringBuilder
     * 
     * @param jsonObject the JsonObject holding the fields
     * @param fieldName the name of the field
     * @param sb the StringBuilder
     */
    private void appendString(@NonNull JsonObject jsonObject, @NonNull String fieldName, @NonNull StringBuilder sb) {
        JsonElement stringField = jsonObject.get(fieldName);
        if (stringField != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(stringField.getAsString());
        }
    }
}
