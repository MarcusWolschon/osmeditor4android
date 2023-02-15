package de.blau.android.layer.mapillary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.ImageInfo;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ImageLoader;
import de.blau.android.util.ScreenMessage;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class MapillaryLoader extends ImageLoader {
    private static final long serialVersionUID = 2L;

    protected static final String DEBUG_TAG = MapillaryLoader.class.getSimpleName().substring(0, Math.min(23, MapillaryLoader.class.getSimpleName().length()));

    private static final int IMAGERY_LOAD_THREADS = 3;

    private static final String COORDINATES_FIELD            = "coordinates";
    private static final String COMPUTED_GEOMETRY_FIELD      = "computed_geometry";
    private static final String COMPUTED_COMPASS_ANGLE_FIELD = "computed_compass_angle";
    private static final String CAPTURED_AT_FIELD            = "captured_at";
    private static final String THUMB_2048_URL_FIELD         = "thumb_2048_url";

    private static final String JPG = "." + FileExtensions.JPG;

    final File                          cacheDir;
    final long                          cacheSize;
    final String                        imageUrl;
    private final Map<String, double[]> coordinates = new HashMap<>();
    private final List<String>          ids;

    private transient ThreadPoolExecutor mThreadPool;

    /**
     * Construct a new loader
     * 
     * @param cacheDir the cacheDir that should be used as a destination for the images
     * @param cacheSize max size of the cache
     * @param imageUrl base url for retrieving the image
     * @param ids list of images ids
     */
    MapillaryLoader(@NonNull File cacheDir, long cacheSize, @NonNull String imageUrl, List<String> ids) {
        this.cacheDir = cacheDir;
        this.cacheSize = cacheSize;
        this.imageUrl = imageUrl;
        this.ids = ids;
    }

    @SuppressLint("NewApi") // StandardCharsets is desugared for APIs < 19.
    @Override
    public void load(SubsamplingScaleImageView view, String key) {
        File imageFile = new File(cacheDir, key + JPG);
        if (imageFile.exists() && imageFile.length() > 0) {
            if (!coordinates.containsKey(key)) {
                try {
                    ExifInterface exif = new ExifInterface(imageFile);
                    coordinates.put(key, exif.getLatLong());
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, e.getMessage());
                }
            }
            setImage(view, imageFile);
            return;
        }
        // download
        if (mThreadPool == null) {
            mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(IMAGERY_LOAD_THREADS);
        }
        try {
            mThreadPool.execute(() -> {
                Log.d(DEBUG_TAG, "querying server for " + key);
                try {
                    String urlString = String.format(imageUrl, key);
                    URL url = new URL(urlString);
                    Request request = new Request.Builder().url(url).build();
                    OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(20000, TimeUnit.MILLISECONDS)
                            .readTimeout(20000, TimeUnit.MILLISECONDS).build();
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
            });
        } catch (RejectedExecutionException rjee) {
            Log.e(DEBUG_TAG, "Execution rejected " + rjee.getMessage());
        }
    }

    /**
     * Prune the image cache
     */
    private void pruneCache() {
        new ExecutorTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void arg) {
                FileUtil.pruneCache(cacheDir, cacheSize);
                return null;
            }
        }.execute();
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
            if (inputStream == null) {
                throw new IOException("Download failed no InputStream");
            }
            try (FileOutputStream fileOutput = new FileOutputStream(imageFile)) {
                byte[] buffer = new byte[1024];
                int bufferLength = 0;
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                }
            }
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

    /**
     * Set the image
     * 
     * @param view the ImageView to set it in
     * @param imageFile the file
     */
    void setImage(@NonNull SubsamplingScaleImageView view, @NonNull File imageFile) {
        view.post(() -> { // needs to run on the ui thread
            view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE);
            view.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            view.setImage(ImageSource.uri(Uri.parse(Schemes.FILE + ":" + imageFile.getAbsolutePath())));
        });
    }

    @Override
    public void showOnMap(Context context, int index) {
        if (!App.isPropertyEditorRunning()) {
            Intent intent = new Intent(context, Main.class);
            intent.setAction(Main.ACTION_MAPILLARY_SELECT);
            intent.putExtra(MapillaryOverlay.SET_POSITION_KEY, index);
            String key = ids.get(index);
            if (key != null && coordinates.containsKey(key)) {
                intent.putExtra(MapillaryOverlay.COORDINATES_KEY, coordinates.get(key));
            }
            context.startActivity(intent);
        }
    }

    @Override
    public void share(Context context, String key) {
        File imageFile = new File(cacheDir, key + JPG);
        if (imageFile.exists()) {
            Uri f = FileProvider.getUriForFile(context, context.getString(R.string.content_provider), imageFile);
            de.blau.android.layer.photos.Util.sharePhoto(context, key, f, MimeTypes.JPEG);
        } else {
            ScreenMessage.toastTopError(context, context.getString(R.string.toast_error_accessing_photo, key));
        }
    }

    @Override
    public boolean supportsInfo() {
        return true;
    }

    @Override
    public void info(@NonNull FragmentActivity activity, @NonNull String uri) {
        Uri f = FileProvider.getUriForFile(activity, activity.getString(R.string.content_provider), new File(cacheDir, uri + JPG));
        ImageInfo.showDialog(activity, f.toString());

    }
}
