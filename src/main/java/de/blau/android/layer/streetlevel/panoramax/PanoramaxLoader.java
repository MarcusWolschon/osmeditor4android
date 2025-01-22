package de.blau.android.layer.streetlevel.panoramax;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.util.Log;
import androidx.annotation.NonNull;
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

    private final Map<String, String> urls;

    /**
     * Construct a new loader
     * 
     * @param cacheDir the cacheDir that should be used as a destination for the images
     * @param cacheSize max size of the cache
     * @param ids list of images ids
     * @param urls map of id to url
     */
    PanoramaxLoader(@NonNull File cacheDir, long cacheSize, final List<String> ids, final Map<String, String> urls) {
        super(cacheDir, cacheSize, "", ids);
        this.urls = urls;
    }

    @Override
    protected Runnable getDownloader(final String key, final SubsamplingScaleImageView view, final File imageFile) {
        return () -> {
            Log.d(DEBUG_TAG, "querying paranomax server for " + key);
            try {
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(20000, TimeUnit.MILLISECONDS).readTimeout(20000, TimeUnit.MILLISECONDS)
                        .build();
                final String urlFromCache = urls.get(key);
                if (urlFromCache == null) {
                    throw new IOException("No cached URL found for " + key);
                }
                Request request = new Request.Builder().url(urlFromCache).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Download failed " + response.message());
                }
                try (ResponseBody responseBody = response.body(); InputStream inputStream = responseBody.byteStream()) {
                    writeStreamToFile(inputStream, imageFile);
                }
                setImage(view, imageFile);
                pruneCache();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, e.getMessage());
            }
        };
    }

    @Override
    protected LayerType getLayerType() {
        return LayerType.PANORAMAX;
    }
}
