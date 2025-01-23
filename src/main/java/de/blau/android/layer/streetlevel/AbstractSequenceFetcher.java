package de.blau.android.layer.streetlevel;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class AbstractSequenceFetcher implements Runnable {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AbstractSequenceFetcher.class.getSimpleName().length());
    private static final String DEBUG_TAG = AbstractSequenceFetcher.class.getSimpleName().substring(0, TAG_LEN);

    protected final FragmentActivity activity;
    final String                     urlTemplate;
    protected final String           sequenceId;
    final String                     apiKey;

    /**
     * Construct a new instance
     * 
     * @param activity the calling Activity
     * @param sequenceId the sequence id
     * @param id the image id
     */
    protected AbstractSequenceFetcher(@NonNull FragmentActivity activity, @NonNull String urlTemplate, @NonNull String sequenceId, @Nullable String apiKey) {
        this.activity = activity;
        this.urlTemplate = urlTemplate;
        this.sequenceId = sequenceId;
        this.apiKey = apiKey;
    }

    @Override
    public void run() {
        try {
            URL url = new URL(String.format(urlTemplate, sequenceId, apiKey));
            ArrayList<String> ids = new ArrayList<>();
            do {
                Log.d(DEBUG_TAG, "query sequence: " + url.toString());
                url = querySequence(url, ids);
            } while (url != null);
            saveIdsAndUpdate(ids);
        } catch (IOException ex) {
            Log.e(DEBUG_TAG, "query sequence failed with " + ex.getMessage());
        }
    }

    /**
     * query the api for a sequence
     * 
     * @param url the URL
     * @throws IOException if IO goes wrong
     */
    @Nullable
    protected URL querySequence(@NonNull URL url, @NonNull ArrayList<String> ids) throws IOException {
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(20000, TimeUnit.MILLISECONDS).readTimeout(20000, TimeUnit.MILLISECONDS).build();
        Call call = client.newCall(request);
        Response callResponse = call.execute();
        if (!callResponse.isSuccessful()) {
            return null;
        }
        ResponseBody responseBody = callResponse.body();
        try (InputStream inputStream = responseBody.byteStream()) {
            if (inputStream == null) {
                throw new IOException("null InputStream");
            }
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = inputStream.read()) != -1) {
                sb.append((char) cp);
            }
            JsonElement root = JsonParser.parseString(sb.toString());
            if (!root.isJsonObject()) {
                throw new IOException("root is not a JsonObject");
            }
            return getIds(root, ids);
        }
    }

    /**
     * Add ids list to state and update map
     * 
     * @param ids List of image ids
     */
    protected abstract void saveIdsAndUpdate(@NonNull ArrayList<String> ids);

    /**
     * Get list of ids from a sequence
     * 
     * @param root top level JsonElement
     * @param ids
     * @return a List of ids
     * @throws IOException if the ids can't be found
     */
    @Nullable
    protected abstract URL getIds(@NonNull JsonElement root, ArrayList<String> ids) throws IOException;
}
