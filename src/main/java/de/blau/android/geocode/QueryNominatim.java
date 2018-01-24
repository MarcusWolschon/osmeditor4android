package de.blau.android.geocode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.stream.JsonReader;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.geocode.Search.SearchResult;
import de.blau.android.osm.ViewBox;
import de.blau.android.util.SavingHelper;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class QueryNominatim extends Query {

    private static final String DEBUG_TAG = null;

    /**
     * Query a Nominatim geocoder
     * 
     * @param activity the calling FragmentActivity, if null no progress spinner will be shown
     * @param url URL for the specific instance of the geocoder
     * @param bbox a ViewBox to restrict the query to, if null the whole world will be considered
     */
    public QueryNominatim(@Nullable FragmentActivity activity, @NonNull String url, @Nullable ViewBox bbox) {
        super(activity, url, bbox);
    }

    @Override
    protected List<SearchResult> doInBackground(String... params) {
        List<SearchResult> result = new ArrayList<>();
        String query = params[0];
        Uri.Builder builder = Uri.parse(url).buildUpon().appendPath("search").appendQueryParameter("q", query);
        if (bbox != null) {
            String viewBoxCoordinates = bbox.getLeft() / 1E7D + "," + bbox.getBottom() / 1E7D + "," + bbox.getRight() / 1E7D + "," + bbox.getTop() / 1E7D;
            builder.appendQueryParameter("viewboxlbrt", viewBoxCoordinates);
        }
        Uri uriBuilder = builder.appendQueryParameter("format", "jsonv2").build();

        String urlString = uriBuilder.toString();
        Log.d("Search", "urlString: " + urlString);
        InputStream inputStream = null;
        JsonReader reader = null;
        ResponseBody responseBody = null;
        try {
            Request request = new Request.Builder().url(urlString).build();
            Call searchCall = App.getHttpClient().newCall(request);
            Response searchCallResponse = searchCall.execute();
            if (searchCallResponse.isSuccessful()) {
                responseBody = searchCallResponse.body();
                inputStream = responseBody.byteStream();
            }

            if (inputStream != null) {
                reader = new JsonReader(new InputStreamReader(inputStream));
                reader.beginArray();
                while (reader.hasNext()) {
                    SearchResult searchResult = readNominatimResult(reader);
                    if (searchResult != null) { // TODO handle deprecated
                        result.add(searchResult);
                        Log.d("Search", "received: " + searchResult.toString());
                    }
                }
                reader.endArray();
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "QueryNominatim got " + e.getMessage());
            connectionError(e.getMessage());
        } finally {
            SavingHelper.close(responseBody);
            SavingHelper.close(reader);
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
                case "lat":
                    result.setLat(reader.nextDouble());
                    break;
                case "lon":
                    result.setLon(reader.nextDouble());
                    break;
                case "display_name":
                    result.displayName = reader.nextString();
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
}
