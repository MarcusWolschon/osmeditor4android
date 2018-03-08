package de.blau.android.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.stream.JsonReader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Server;
import de.blau.android.util.SavingHelper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Interface to the OAM API
 * 
 * THe OAM API documentation essentially has nothing to do with the current version, have a look at the fixture oam.json
 * for actual output
 * 
 * @author Simon Poole
 *
 */
public class OAMCatalog {

    private static final String DEBUG_TAG = "OAMCatalog";
    /**
     * Timeout for connections in milliseconds.
     */
    private static final int    TIMEOUT   = 45 * 1000;

    public class Entry {
        String      id;
        String      title;
        String      tileUrl;
        BoundingBox box;
        double      gsd;
        long        startDate = -1;
        long        endDate   = -1;

        @Override
        public String toString() {
            return title;
        }
    }

    private int limit = 0;
    private int found = 0;

    /**
     * Query the OAM API for a list of imagery
     * 
     * @param oamServer URL for the OAM API server
     * @param box if not null limit the query to this BoundingBox
     * @return a List of OAMCatalog.Entry
     * @throws IOException
     */
    public List<Entry> getEntries(@NonNull String oamServer, @Nullable BoundingBox box) throws IOException {
        URL url = new URL(oamServer + "meta" + (box != null
                ? "?" + "bbox=" + box.getLeft() / 1E7d + "," + box.getBottom() / 1E7d + "," + box.getRight() / 1E7d + "," + box.getTop() / 1E7d + "&" : "?")
                + "has_tiled=true");

        Log.d(DEBUG_TAG, "query: " + url.toString());
        ResponseBody responseBody = null;
        InputStream inputStream = null;

        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        Call catalogCall = client.newCall(request);
        Response catalogCallResponse = catalogCall.execute();
        if (catalogCallResponse.isSuccessful()) {
            responseBody = catalogCallResponse.body();
            inputStream = responseBody.byteStream();
        } else {
            Server.throwOsmServerException(catalogCallResponse);
        }

        return parseEntries(inputStream);
    }

    /**
     * Parse the output of OAM /meta API call
     * 
     * @param is InputStream connected to OAM
     * @return a List of OAMCatalog.Entry
     * @throws IOException
     * @throws NumberFormatException
     */
    private List<Entry> parseEntries(@NonNull InputStream is) throws IOException, NumberFormatException {
        List<Entry> result = new ArrayList<>();
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        try {
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
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } else if ("results".equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        Entry entry = new Entry();
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
                            default:
                                reader.skipValue();
                                break;
                            }
                        }
                        reader.endObject();
                        Log.d(DEBUG_TAG, "got " + entry.title + " " + entry.tileUrl + " " + entry.box);
                        if (entry.title != null && entry.tileUrl != null) {
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
        } finally {
            SavingHelper.close(reader);
        }
        return result;
    }

    /**
     * Parse a acquisition date
     * 
     * @param reader the Jsonreader
     * @return the time since the epoch in milliseconds
     * @throws IOException
     */
    public long parseAcquisitionDate(JsonReader reader) throws IOException {
        try {
            return new SimpleDateFormat(OsmParser.TIMESTAMP_FORMAT).parse(reader.nextString()).getTime();
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
