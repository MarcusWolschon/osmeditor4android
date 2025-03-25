package io.vespucci.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Server;
import io.vespucci.util.DateFormatter;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Interface to the OAM API
 * 
 * The OAM API documentation essentially has nothing to do with the current version, have a look at the fixture oam.json
 * for actual output
 * 
 * @author Simon Poole
 *
 */
public class OAMCatalog {

    private static final String DEBUG_TAG = OAMCatalog.class.getSimpleName().substring(0, Math.min(23, OAMCatalog.class.getSimpleName().length()));

    /**
     * Includes milliseconds
     */
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Timeout for connections in milliseconds.
     */
    private static final int TIMEOUT = 45 * 1000;

    private int       limit        = 0;
    private String    license      = null;
    private int       found        = 0;
    private Pattern[] titleRegexps = null;

    /**
     * Query the OAM API for a list of imagery
     * 
     * @param context an Android Context
     * @param oamServer URL for the OAM API server
     * @param box if not null limit the query to this BoundingBox
     * 
     * @return a List of OAMCatalog.Entry
     * @throws IOException if reading the entries fails
     */
    @Nullable
    public List<LayerEntry> getEntries(@Nullable Context context, @NonNull String oamServer, @Nullable BoundingBox box) throws IOException {
        if (context != null) {
            String[] regexpStrings = context.getResources().getStringArray(R.array.bad_oam_title);
            int length = regexpStrings.length;
            titleRegexps = new Pattern[length];
            for (int i = 0; i < length; i++) {
                titleRegexps[i] = Pattern.compile(regexpStrings[i]);
            }
        }

        URL url = new URL(oamServer + "meta"
                + (box != null
                        ? "?" + "bbox=" + box.getLeft() / 1E7d + "," + box.getBottom() / 1E7d + "," + box.getRight() / 1E7d + "," + box.getTop() / 1E7d + "&"
                        : "?")
                + "has_tiled=true");
        Log.d(DEBUG_TAG, "query: " + url.toString());

        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        Call catalogCall = client.newCall(request);
        Response catalogCallResponse = catalogCall.execute();
        if (catalogCallResponse.isSuccessful()) {
            ResponseBody responseBody = catalogCallResponse.body();
            InputStream inputStream = responseBody.byteStream();
            return parseEntries(inputStream);
        } else {
            Server.throwOsmServerException(catalogCallResponse);
        }
        return null;
    }

    /**
     * Parse the output of OAM /meta API call
     * 
     * @param is InputStream connected to OAM
     * @return a List of OAMCatalog.Entry
     * @throws IOException if reading fails
     * @throws NumberFormatException if we can't parse a number
     */
    @NonNull
    private List<LayerEntry> parseEntries(@NonNull InputStream is) throws IOException, NumberFormatException {
        List<LayerEntry> result = new ArrayList<>();
        try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
            // key object
            String key = null;
            reader.beginObject();
            while (reader.hasNext()) {
                key = reader.nextName(); //
                if ("meta".equals(key)) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        key = reader.nextName();
                        if ("limit".equals(key)) {
                            limit = reader.nextInt();
                        } else if ("found".equals(key)) {
                            found = reader.nextInt();
                        } else if ("license".equals(key)) {
                            license = reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } else if ("results".equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        LayerEntry entry = new LayerEntry();
                        entry.license = license; // set to default for the site
                        reader.beginObject();
                        while (reader.hasNext()) {
                            key = reader.nextName();
                            switch (key) {
                            case "title":
                                entry.title = reader.nextString();
                                break;
                            case "bbox":
                                reader.beginArray();
                                double left = reader.nextDouble();
                                double bottom = reader.nextDouble();
                                double right = reader.nextDouble();
                                double top = reader.nextDouble();
                                reader.endArray();
                                entry.box = new BoundingBox(left, bottom, right, top);
                                break;
                            case "properties":
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    key = reader.nextName();
                                    if ("tms".equals(key)) {
                                        entry.tileUrl = reader.nextString();
                                    } else if ("thumbnail".equals(key)) {
                                        entry.thumbnailUrl = reader.nextString();
                                    } else if ("license".equals(key)) {
                                        entry.license = reader.nextString();
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                                break;
                            case "acquisition_start":
                                entry.startDate = parseAcquisitionDate(reader);
                                break;
                            case "acquisition_end":
                                entry.endDate = parseAcquisitionDate(reader);
                                break;
                            case "gsd":
                                entry.gsd = reader.nextDouble();
                                break;
                            case "provider":
                                entry.provider = reader.nextString();
                                break;
                            default:
                                reader.skipValue();
                                break;
                            }
                        }
                        reader.endObject();
                        Log.d(DEBUG_TAG, "got " + entry.title + " " + entry.tileUrl + " " + entry.box);
                        if (entry.title != null && entry.tileUrl != null && !matchTitleFilter(entry.title)) {
                            result.add(entry);
                        }
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException | IllegalStateException ex) {
            Log.d(DEBUG_TAG, "Ignoring " + ex);
        }
        return result;
    }

    /**
     * Check a title against the filter regexps
     * 
     * @param title the entry title
     * @return true if it matches
     */
    private boolean matchTitleFilter(@NonNull String title) {
        if (titleRegexps != null) {
            for (Pattern p : titleRegexps) {
                if (p.matcher(title).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Parse a acquisition date
     * 
     * @param reader the JsonReader
     * @return the time since the epoch in milliseconds
     * @throws IOException if reading fails
     */
    public long parseAcquisitionDate(JsonReader reader) throws IOException {
        try {
            return DateFormatter.getUtcFormat(TIMESTAMP_FORMAT).parse(reader.nextString()).getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return the found
     */
    public int getFound() {
        return found;
    }
}
