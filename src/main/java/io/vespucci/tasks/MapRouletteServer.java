package io.vespucci.tasks;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.App;
import io.vespucci.ErrorCodes;
import io.vespucci.UploadResult;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Server;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class MapRouletteServer {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapRouletteServer.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapRouletteServer.class.getSimpleName().substring(0, TAG_LEN);

    private static final String APIPATH = "api/v2/";
    private static final String API_KEY = "apiKey";

    /**
     * Timeout for connections in milliseconds.
     */
    private static final int TIMEOUT = 15 * 1000;

    /**
     * Private constructor
     */
    private MapRouletteServer() {
        // don't allow instantiating of this class
    }

    /**
     * Perform an HTTP request to download up to limit tasks inside the specified area. Blocks until the request is
     * complete.
     * 
     * @param server the Maproulette server
     * @param area Latitude/longitude *1E7 of area to download.
     * @param limit maximum number of tasks to return
     * @return All the bugs in the given area.
     */
    @Nullable
    public static Collection<MapRouletteTask> getTasksForBox(@NonNull String server, @NonNull BoundingBox area, long limit) {
        try {
            Log.d(DEBUG_TAG, "getTasksForBox");
            URL url = new URL(getServerURL(server) + "tasks/box/" + area.getLeft() / 1E7d + "/" + area.getBottom() / 1E7d + "/" + area.getRight() / 1E7d + "/"
                    + area.getTop() / 1E7d + "?includeGeometries=true&limit=" + Long.toString(limit));
            try (InputStream inputStream = getFromApi(url)) {
                if (inputStream != null) {
                    return MapRouletteTask.parseTasks(inputStream);
                }
                return new ArrayList<>();
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getTasksForBox got exception " + e.getMessage());
        }
        return null;
    }

    /**
     * Connect to the MapROulette API and return an InputStream
     * 
     * @param url the URL
     * @return null or the InputStream
     * @throws IOException if IO went wrong
     */
    @Nullable
    private static InputStream getFromApi(@NonNull URL url) throws IOException {
        Log.d(DEBUG_TAG, "query: " + url.toString());
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        Call mapRouletteCall = client.newCall(request);
        Response mapRouletteResponse = mapRouletteCall.execute();
        if (mapRouletteResponse.isSuccessful()) {
            ResponseBody responseBody = mapRouletteResponse.body();
            return responseBody.byteStream();
        } else {
            Log.e(DEBUG_TAG, "Download unsuccessful : " + mapRouletteResponse.code());
            return null;
        }
    }

    /**
     * Change the state of the MapRoulette task on the server
     * 
     * @param context an Android Context
     * @param server the maproulette server
     * @param apiKey the users apiKey
     * @param task the task with the state the server side task should be changed to
     * @return true if successful
     */
    @NonNull
    public static UploadResult changeState(@NonNull Context context, @NonNull String server, @NonNull String apiKey, @NonNull MapRouletteTask task) {
        try {
            URL url = new URL(getServerURL(server) + "task/" + task.getId() + "/" + MapRouletteFragment.state2pos(context.getResources(), task.getState()));
            Log.d(DEBUG_TAG, "changeState " + url.toString());
            Request request = new Request.Builder().url(url).put(RequestBody.create(null, "")).addHeader(API_KEY, apiKey).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call maprouletteCall = client.newCall(request);
            try (Response maprouletteCallResponse = maprouletteCall.execute()) {
                if (!maprouletteCallResponse.isSuccessful()) {
                    int responseCode = maprouletteCallResponse.code();
                    Log.d(DEBUG_TAG, "changeState respnse code " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        task.setChanged(false); // don't retry
                        App.getTaskStorage().setDirty();
                    }
                    UploadResult result = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
                    String message = Server.readStream(maprouletteCallResponse.body().byteStream());
                    result.setHttpError(responseCode);
                    result.setMessage(message);
                    return result;
                }
            }
            task.setChanged(false);
            App.getTaskStorage().setDirty();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "changeState got exception " + e.getMessage());
            UploadResult result = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
            result.setMessage(e.getMessage());
            return result;
        }
        Log.d(DEBUG_TAG, "changeState success");
        return new UploadResult(ErrorCodes.OK);
    }

    /**
     * Retrieve a MapRoulette Challenge
     * 
     * @param server the maproulette server
     * @param id the Challenge id
     * @return the Challenge or null if none could be retrieved
     */
    @Nullable
    public static MapRouletteChallenge getChallenge(@NonNull String server, long id) {
        try {
            Log.d(DEBUG_TAG, "getChallenge");
            URL url = new URL(getServerURL(server) + "challenge/" + Long.toString(id));
            Log.d(DEBUG_TAG, "query: " + url.toString());
            try (InputStream inputStream = getFromApi(url)) {
                if (inputStream != null) {
                    return MapRouletteChallenge.parseChallenge(inputStream);
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getChallenge got exception " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the Mapoulette server from preferences
     *
     * @param server the maproulette server
     * @return the server URL
     */
    @NonNull
    private static String getServerURL(@NonNull String server) {
        return server + APIPATH;
    }
}
