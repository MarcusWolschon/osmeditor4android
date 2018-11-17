package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

class MapRouletteServer {

    private static final String DEBUG_TAG = MapRouletteServer.class.getSimpleName();

    private static final String apiPath = "api/v2/";

    /**
     * Timeout for connections in milliseconds.
     */
    private static final int TIMEOUT = 45 * 1000;

    /**
     * Perform an HTTP request to download up to limit bugs inside the specified area. Blocks until the request is
     * complete.
     * 
     * @param context the Android context
     * @param area Latitude/longitude *1E7 of area to download.
     * @param limit unused
     * @return All the bugs in the given area.
     */
    public static Collection<MapRouletteTask> getTasksForBox(Context context, BoundingBox area, long limit) {
        Collection<MapRouletteTask> result = null;
        try {
            Log.d(DEBUG_TAG, "getTasksForBox");
            URL url;

            url = new URL(getServerURL(context) + "tasks/box/" + area.getLeft() / 1E7d + "/" + area.getBottom() / 1E7d + "/" + area.getRight() / 1E7d + "/"
                    + area.getTop() / 1E7d + "");
            Log.d(DEBUG_TAG, "query: " + url.toString());
            ResponseBody responseBody = null;
            InputStream inputStream = null;

            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call mapRouletteCall = client.newCall(request);
            Response mapRouletteResponse = mapRouletteCall.execute();
            if (mapRouletteResponse.isSuccessful()) {
                responseBody = mapRouletteResponse.body();
                inputStream = responseBody.byteStream();
            } else {
                Log.e(DEBUG_TAG, "Download unsuccessful : " + mapRouletteResponse.code());
                return new ArrayList<>();
            }

            result = MapRouletteTask.parseTasks(inputStream);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getTasksForBox got exception " + e.getMessage());
        }
        return result;
    }

    /**
     * Change the state of the MapRoulette task on the server
     * 
     * @param context the Android context
     * @param apiKey the users apiKey
     * @param task the task with the state the server side task should be changed to
     * @return true if successful
     */
    public static boolean changeState(@NonNull Context context, @NonNull String apiKey, @NonNull MapRouletteTask task) {
        try {
            URL url = new URL(getServerURL(context) + "task/" + task.getId() + "/" + task.getState().ordinal());
            Log.d(DEBUG_TAG, "changeState " + url.toString());
            Request request = new Request.Builder().url(url).put(RequestBody.create(null, "")).addHeader("apiKey", apiKey).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call maprouletteCall = client.newCall(request);
            Response maprouletteCallResponse = maprouletteCall.execute();
            if (!maprouletteCallResponse.isSuccessful()) {
                int responseCode = maprouletteCallResponse.code();
                Log.d(DEBUG_TAG, "changeState respnse code " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    task.setChanged(false); // don't retry
                    App.getTaskStorage().setDirty();
                }
                return false;
            }
            task.setChanged(false);
            App.getTaskStorage().setDirty();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "changeState got exception " + e.getMessage());
            return false;
        }
        Log.d(DEBUG_TAG, "changeState sucess");
        return true;
    }

    /**
     * Retrieve a MapROulette Challenge
     * 
     * @param context the Android Context
     * @param id the Challenge id
     * @return the Challenge or null if none could be retrieved
     */
    @Nullable
    public static MapRouletteChallenge getChallenge(@NonNull Context context, long id) {
        MapRouletteChallenge result = null;
        try {
            Log.d(DEBUG_TAG, "getChallenge");
            URL url;
            url = new URL(getServerURL(context) + "challenge/" + Long.toString(id));
            Log.d(DEBUG_TAG, "query: " + url.toString());
            ResponseBody responseBody = null;
            InputStream inputStream = null;

            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call mapRouletteCall = client.newCall(request);
            Response mapRouletteResponse = mapRouletteCall.execute();
            if (mapRouletteResponse.isSuccessful()) {
                responseBody = mapRouletteResponse.body();
                inputStream = responseBody.byteStream();
            } else {
                Log.e(DEBUG_TAG, "Challenge download unsuccessful : " + mapRouletteResponse.code());
                return null;
            }

            result = MapRouletteChallenge.parseChallenge(inputStream);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getChallenge got exception " + e.getMessage());
        }
        return result;
    }

    /**
     * Get the Mapoulette server from preferences
     *
     * @param context the Android context
     * @return the server URL
     */
    @NonNull
    private static String getServerURL(Context context) {
        Preferences prefs = new Preferences(context);
        return prefs.getMapRouletteServer() + apiPath;
    }
}
