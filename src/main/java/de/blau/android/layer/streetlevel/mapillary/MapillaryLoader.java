package de.blau.android.layer.streetlevel.mapillary;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import de.blau.android.App;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.streetlevel.NetworkImageLoader;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class MapillaryLoader extends NetworkImageLoader {
    private static final long serialVersionUID = 2L;

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, MapillaryLoader.class.getSimpleName().length());
    protected static final String DEBUG_TAG = MapillaryLoader.class.getSimpleName().substring(0, TAG_LEN);

    private static final String COORDINATES_FIELD            = "coordinates";
    private static final String COMPUTED_GEOMETRY_FIELD      = "computed_geometry";
    private static final String COMPUTED_COMPASS_ANGLE_FIELD = "computed_compass_angle";
    private static final String CAPTURED_AT_FIELD            = "captured_at";
    private static final String THUMB_2048_URL_FIELD         = "thumb_2048_url";

    /**
     * Construct a new loader
     * 
     * @param cacheDir the cacheDir that should be used as a destination for the images
     * @param cacheSize max size of the cache
     * @param imageUrl base url for retrieving the image
     * @param ids list of images ids
     */
    MapillaryLoader(@NonNull File cacheDir, long cacheSize, @NonNull String imageUrl, List<String> ids) {
        super(cacheDir, cacheSize, imageUrl, ids);
    }

    @Override
    protected Runnable getDownloader(final String key, final SubsamplingScaleImageView view, final File imageFile) {
        return () -> {
            Log.d(DEBUG_TAG, "querying mapillary server for " + key);
            try {
                Request request = new Request.Builder().url(new URL(String.format(imageUrl, key))).build();
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(20000, TimeUnit.MILLISECONDS).readTimeout(20000, TimeUnit.MILLISECONDS)
                        .build();
                Call mapillaryCall = client.newCall(request);
                Response mapillaryCallResponse = mapillaryCall.execute();
                if (!mapillaryCallResponse.isSuccessful()) {
                    throw new IOException("Download of " + key + " failed with " + mapillaryCallResponse.code() + " " + mapillaryCallResponse.message());
                }
                try (ResponseBody responseBody = mapillaryCallResponse.body(); InputStream inputStream = responseBody.byteStream()) {
                    if (inputStream == null) {
                        throw new IOException("No InputStream");
                    }
                    JsonElement root = JsonParser.parseReader(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
                    if (!root.isJsonObject() || !((JsonObject) root).has(THUMB_2048_URL_FIELD)) {
                        throw new IOException("Unexpected / missing response");
                    }
                    loadImage(key, imageFile, client, (JsonObject) root, ((JsonObject) root).get(THUMB_2048_URL_FIELD).getAsString());
                }
                setImage(view, imageFile);
                pruneCache();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, e.getMessage());
            }
        };
    }

    /**
     * Download the image
     * 
     * @param key image key
     * @param imageFile target file to save the image in
     * @param client OkHttp client
     * @param point JsonElement holding coordinates for the image
     * @param url image url
     * @throws IOException if download or writing has issues
     */
    private void loadImage(@NonNull String key, @NonNull File imageFile, @NonNull OkHttpClient client, JsonObject meta, @NonNull String url)
            throws IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Download failed " + response.message());
        }
        try (ResponseBody responseBody = response.body(); InputStream inputStream = responseBody.byteStream()) {
            writeStreamToFile(inputStream, imageFile);
            JsonElement point = meta.get(COMPUTED_GEOMETRY_FIELD);
            if (!(point instanceof JsonObject) || imageFile.length() == 0) {
                throw new IOException("No geometry for image or image empty");
            }
            JsonElement coords = ((JsonObject) point).get(COORDINATES_FIELD);
            if (!(coords instanceof JsonArray) || ((JsonArray) coords).size() != 2) {
                throw new IOException("No geometry for image");
            }
            ExifInterface exif = new ExifInterface(imageFile);
            double lat = ((JsonArray) coords).get(1).getAsDouble();
            double lon = ((JsonArray) coords).get(0).getAsDouble();
            exif.setLatLong(lat, lon);
            JsonElement angleElement = meta.get(COMPUTED_COMPASS_ANGLE_FIELD);
            if (angleElement instanceof JsonPrimitive) {
                float angle = angleElement.getAsFloat();
                exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, Integer.toString((int) (angle * 100)) + "/100");
                exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, ExifInterface.GPS_DIRECTION_MAGNETIC);
            }
            JsonElement capturedAt = meta.get(CAPTURED_AT_FIELD);
            if (capturedAt instanceof JsonPrimitive) {
                exif.setDateTime(capturedAt.getAsLong());
            }
            exif.saveAttributes();
            coordinates.put(key, new double[] { lat, lon });
        }
    }

    @Override
    protected LayerType getLayerType() {
        return LayerType.MAPILLARY;
    }
}
