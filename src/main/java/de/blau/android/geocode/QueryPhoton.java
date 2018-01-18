package de.blau.android.geocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Geometry;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.geocode.Query;
import de.blau.android.geocode.Search.SearchResult;
import de.blau.android.osm.ViewBox;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class QueryPhoton extends Query {

    private static final String DEBUG_TAG = "QueryPhoton";

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

    @Override
    protected List<SearchResult> doInBackground(String... params) {
        List<SearchResult> result = new ArrayList<>();
        String query = params[0];
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
        Log.d("Search", "urlString: " + urlString);
        InputStream inputStream = null;
        JsonReader reader = null;
        ResponseBody responseBody = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                Request request = new Request.Builder().url(urlString).build();
                Call searchCall = App.getHttpClient().newCall(request);
                Response searchCallResponse = searchCall.execute();
                if (searchCallResponse.isSuccessful()) {
                    responseBody = searchCallResponse.body();
                    inputStream = responseBody.byteStream();
                }
            } else { // FIXME 2.2/API 8 support
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", App.getUserAgent());
                inputStream = conn.getInputStream();
            }

            if (inputStream != null) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                StringBuilder sb = new StringBuilder();
                int cp;
                while ((cp = rd.read()) != -1) {
                    sb.append((char) cp);
                }
                FeatureCollection fc = FeatureCollection.fromJson(sb.toString());
                for (Feature f : fc.getFeatures()) {
                    SearchResult searchResult = readPhotonResult(f);
                    if (searchResult != null) {
                        result.add(searchResult);
                        Log.d("Search", "received: " + searchResult.toString());
                    }
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "QueryPhoton got " + e.getMessage());
            connectionError(e.getMessage());
        } finally {
            SavingHelper.close(inputStream);
            SavingHelper.close(responseBody);
            SavingHelper.close(reader);
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
            JsonObject properties = f.getProperties();
            Geometry<?> g = f.getGeometry();
            if (g instanceof Point) {
                Point p = (Point) g;
                Position pos = p.getCoordinates();
                result.setLat(pos.getLatitude());
                result.setLon(pos.getLongitude());
                StringBuilder sb = new StringBuilder();
                JsonElement name = properties.get("name");
                if (name != null) {
                    sb.append(name.getAsString());
                    sb.append("<small>");
                    JsonElement osmKey = properties.get("osm_key");
                    JsonElement osmValue = properties.get("osm_value");
                    if (osmKey != null && osmValue != null) {
                        String key = osmKey.getAsString();
                        String value = osmValue.getAsString();
                        Map<String, String> tag = new HashMap<>();
                        tag.put(key, value);
                        PresetItem preset = Preset.findBestMatch(App.getCurrentPresets(activity), tag, false);
                        if (preset != null) {
                            sb.append("<br>[" + preset.getTranslatedName() + "]<br>");
                        } else {
                            sb.append("<br>[" + key + "=" + value + "]<br>");
                        }
                    }
                    StringBuilder sb2 = new StringBuilder();
                    JsonElement street = properties.get("street");
                    if (street != null) {
                        sb2.append(street.getAsString());
                        JsonElement housenumber = properties.get("housenumber");
                        if (housenumber != null) {
                            sb2.append(" " + housenumber.getAsString());
                        }
                    }
                    JsonElement postcode = properties.get("postcode");
                    if (postcode != null) {
                        if (sb2.length() > 0) {
                            sb2.append(", ");
                        }
                        sb2.append(postcode.getAsString());
                    }
                    JsonElement city = properties.get("city");
                    if (city != null) {
                        if (sb2.length() > 0) {
                            sb2.append(", ");
                        }
                        sb2.append(city.getAsString());
                    }
                    JsonElement state = properties.get("state");
                    if (state != null) {
                        if (sb2.length() > 0) {
                            sb2.append(", ");
                        }
                        sb2.append(state.getAsString());
                    }
                    JsonElement country = properties.get("country");
                    if (country != null) {
                        if (sb2.length() > 0) {
                            sb2.append(", ");
                        }
                        sb2.append(country.getAsString());
                    }
                    if (sb2.length() > 0) {
                        sb.append("\n");
                        sb.append(sb2);
                    }
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
}
