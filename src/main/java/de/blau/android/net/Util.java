package de.blau.android.net;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.osm.Server;
import de.blau.android.services.util.StreamUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class Util {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Util.class.getSimpleName().length());
    private static final String DEBUG_TAG = Util.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Private constructor
     */
    private Util() {
        // don't allow instantiating of this class
    }

    /**
     * Download a file
     * 
     * @param url the url to download from
     * @param dir the directory to save the preset to
     * @param filename A filename where to save the file.
     * @throws IOException
     */
    public static void download(@NonNull String url, @NonNull File dir, @NonNull String filename) throws IOException {
        Log.d(DEBUG_TAG, "Downloading " + url + " to " + dir + "/" + filename);
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).build();
        Call call = client.newCall(request);
        try (Response callResponse = call.execute()) {
            if (!callResponse.isSuccessful()) {
                Log.w(DEBUG_TAG, "Could not download file " + url + " respose code " + callResponse.code());
                throw new IOException("Could not download file " + url + " respose code " + callResponse.code());
            }
            ResponseBody responseBody = callResponse.body();
            InputStream downloadStream = responseBody.byteStream();
            final File destinationFile = new File(dir, filename);
            try (OutputStream fileStream = new FileOutputStream(destinationFile)) {
                StreamUtils.copy(downloadStream, fileStream);
            }
        }
    }
}
