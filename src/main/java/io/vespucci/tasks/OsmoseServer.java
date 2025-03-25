package io.vespucci.tasks;

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

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.App;
import io.vespucci.ErrorCodes;
import io.vespucci.UploadResult;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Server;
import io.vespucci.tasks.Task.State;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class OsmoseServer {

    private static final String DEBUG_TAG = OsmoseServer.class.getSimpleName().substring(0, Math.min(23, OsmoseServer.class.getSimpleName().length()));

    private static final String API03PATH = "/api/0.3/"; // NOSONAR

    /**
     * the list of supported languages was simply generated from the list of .po in the osmose repo and tested against
     * the API
     */
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList("ca", "cs", "en", "da", "de", "el", "es", "fr", "hu", "it", "ja", "lt", "nl", "pl",
            "pt", "ro", "ru", "sw", "uk");

    /**
     * Timeout for connections in milliseconds.
     */
    private static final int TIMEOUT = 45 * 1000;

    /**
     * Private constructor to stop instantiation
     */
    private OsmoseServer() {
        // private
    }

    /**
     * Perform an HTTP request to download up to limit bugs inside the specified area. Blocks until the request is
     * complete.
     * 
     * @param server the OSMOSE server
     * @param area Latitude/longitude *1E7 of area to download.
     * @param limit unused
     * @return All the bugs in the given area.
     */
    @NonNull
    public static Collection<OsmoseBug> getBugsForBox(@NonNull String server, @NonNull BoundingBox area, long limit) {
        // http://osmose.openstreetmap.fr/de/api/0.2/errors?bbox=8.32,47.33,8.42,47.28&full=true
        try {
            Log.d(DEBUG_TAG, "getBugsForBox");
            URL url = new URL(getServerURL(server) + "issues?" + "bbox=" + area.getLeft() / 1E7d + "," + area.getBottom() / 1E7d + "," + area.getRight() / 1E7d
                    + "," + area.getTop() / 1E7d + "&full=true&limit=" + Long.toString(limit));

            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call osmoseCall = client.newCall(request);
            try (Response osmoseCallResponse = osmoseCall.execute()) {
                if (osmoseCallResponse.isSuccessful()) {
                    return OsmoseBug.parseBugs(osmoseCallResponse.body().byteStream());
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getBugsForBox got exception " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Change the state of the bug on the server
     * 
     * @param server the OSMOSE server
     * @param bug bug with the state the server side bug should be changed to
     * @return true if successful
     */
    @NonNull
    public static UploadResult changeState(@NonNull String server, OsmoseBug bug) {
        // http://osmose.openstreetmap.fr/de/api/0.2/error/3313305479/done
        // http://osmose.openstreetmap.fr/de/api/0.2/error/3313313045/false
        if (bug.getState() == State.OPEN) {
            // open is the default state and we shouldn't actually get here
            return new UploadResult(ErrorCodes.BAD_REQUEST);
        }
        try {
            URL url = new URL(getServerURL(server) + "issue/" + bug.getId() + "/" + (bug.getState() == State.CLOSED ? "done" : "false"));
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
                UploadResult result = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
                String message = Server.readStream(osmoseCallResponse.body().byteStream());
                result.setHttpError(responseCode);
                result.setMessage(message);
                return result;
            }
            bug.setChanged(false);
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
     * Get meta information on a specific Osmose item / class
     * 
     * @param server the OSMOSE server
     * @param itemId the item id
     * @param classId the class id
     */
    public static void getMeta(@NonNull String server, String itemId, int classId) {
        try {
            // http://osmose.openstreetmap.fr/api/0.3/items/3130/class/31301?langs=en
            String lang = Locale.getDefault().getLanguage();
            if (!SUPPORTED_LANGUAGES.contains(lang)) {
                lang = "en";
            }
            URL url = new URL(getServerURL(server) + "items/" + itemId + "/class/" + Integer.toString(classId) + "?langs=" + lang);
            Log.d(DEBUG_TAG, "getMeta " + url.toString());
            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build();
            Call osmoseCall = client.newCall(request);
            Response osmoseCallResponse = osmoseCall.execute();
            if (osmoseCallResponse.isSuccessful()) {
                ResponseBody responseBody = osmoseCallResponse.body();
                InputStream inputStream = responseBody.byteStream();
                App.getTaskStorage().getOsmoseMeta().parse(inputStream);
            } else {
                Log.e(DEBUG_TAG, "getMeta failes");
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getMeta got exception " + e.getMessage());
            return;
        }
        Log.d(DEBUG_TAG, "getMeta sucess");
    }

    /**
     * Get the OSMOSE server url
     * 
     * @param server the base url
     * @return the server URL
     */
    private static String getServerURL(@NonNull String server) {
        return server + API03PATH;
    }
}
