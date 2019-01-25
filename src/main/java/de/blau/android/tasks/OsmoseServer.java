package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Task.State;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class OsmoseServer {

    private static final String DEBUG_TAG = OsmoseServer.class.getSimpleName();

    private static final String       apiPath            = "/api/0.2/";
    /**
     * the list of supported languages was simply generated from the list of .po in the osmose repo and tested against
     * the API
     */
    private static final List<String> supportedLanguages = Arrays.asList("ca", "cs", "en", "da", "de", "el", "es", "fr", "hu", "it", "ja", "lt", "nl", "pl",
            "pt", "ro", "ru", "sw", "uk");

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
    public static Collection<OsmoseBug> getBugsForBox(Context context, BoundingBox area, long limit) {
        Collection<OsmoseBug> result = null;
        // http://osmose.openstreetmap.fr/de/api/0.2/errors?bbox=8.32,47.33,8.42,47.28&full=true
        try {
            Log.d(DEBUG_TAG, "getBugssForBox");
            URL url;

            url = new URL(getServerURL(context) + "errors?" + "bbox=" + area.getLeft() / 1E7d + "," + area.getBottom() / 1E7d + "," + area.getRight() / 1E7d
                    + "," + area.getTop() / 1E7d + "&full=true");
            Log.d(DEBUG_TAG, "query: " + url.toString());
            ResponseBody responseBody = null;
            InputStream inputStream = null;

            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call osmoseCall = client.newCall(request);
            Response osmoseCallResponse = osmoseCall.execute();
            if (osmoseCallResponse.isSuccessful()) {
                responseBody = osmoseCallResponse.body();
                inputStream = responseBody.byteStream();
            } else {
                return new ArrayList<>();
            }

            result = OsmoseBug.parseBugs(inputStream);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getBugsForBox got exception " + e.getMessage());
        }
        return result;
    }

    /**
     * Change the state of the bug on the server
     * 
     * @param context the Android context
     * @param bug bug with the state the server side bug should be changed to
     * @return true if successful
     */
    public static boolean changeState(@NonNull Context context, OsmoseBug bug) {
        // http://osmose.openstreetmap.fr/de/api/0.2/error/3313305479/done
        // http://osmose.openstreetmap.fr/de/api/0.2/error/3313313045/false
        if (bug.getState() == State.OPEN) {
            return false; // open is the default state and we shouldn't actually get here
        }
        try {
            URL url;
            url = new URL(getServerURL(context) + "error/" + bug.getId() + "/" + (bug.getState() == State.CLOSED ? "done" : "false"));
            Log.d(DEBUG_TAG, "changeState " + url.toString());
            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call osmoseCall = client.newCall(request);
            Response osmoseCallResponse = osmoseCall.execute();
            if (!osmoseCallResponse.isSuccessful()) {
                int responseCode = osmoseCallResponse.code();
                Log.d(DEBUG_TAG, "changeState respnse code " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_GONE) {
                    bug.setChanged(false); // don't retry
                    App.getTaskStorage().setDirty();
                }
                return false;
            }
            bug.setChanged(false);
            App.getTaskStorage().setDirty();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "changeState got exception " + e.getMessage());
            return false;
        }
        Log.d(DEBUG_TAG, "changeState sucess");
        return true;
    }

    /**
     * Get the OSMOSE server from preferences
     *
     * @param context the Android context
     * @return the server URL
     */
    private static String getServerURL(@NonNull Context context) {
        Preferences prefs = new Preferences(context);
        String lang = Locale.getDefault().getLanguage();
        if (!supportedLanguages.contains(lang)) {
            lang = "en";
        }
        return prefs.getOsmoseServer() + lang + apiPath;
    }
}
