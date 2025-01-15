package de.blau.android.layer.streetlevel.panoramax;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import de.blau.android.App;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.streetlevel.NetworkImageLoader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class PanoramaxLoader extends NetworkImageLoader {
    private static final long serialVersionUID = 2L;

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, PanoramaxLoader.class.getSimpleName().length());
    protected static final String DEBUG_TAG = PanoramaxLoader.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Construct a new loader
     * 
     * @param cacheDir the cacheDir that should be used as a destination for the images
     * @param cacheSize max size of the cache
     * @param imageUrl base url for retrieving the image
     * @param ids list of images ids
     */
    PanoramaxLoader(@NonNull File cacheDir, long cacheSize, @NonNull String imageUrl, List<String> ids) {
        super(cacheDir, cacheSize, imageUrl, ids);
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
        initThreadPool();
        try {
            mThreadPool.execute(() -> {
                Log.d(DEBUG_TAG, "querying server for " + key);
                try {
                    OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(20000, TimeUnit.MILLISECONDS)
                            .readTimeout(20000, TimeUnit.MILLISECONDS).build();
                    loadImage(imageFile, client, String.format(imageUrl, key));
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
     * Download the image
     * 
     * @param imageFile target file to save the image in
     * @param client OkHttp client
     * @param url image url
     * @throws IOException if download or writing has issues
     */
    private void loadImage(@NonNull File imageFile, @NonNull OkHttpClient client, @NonNull String url) throws IOException {
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
        }
    }

    @Override
    protected LayerType getLayerType() {
        return LayerType.PANORAMAX;
    }
}
