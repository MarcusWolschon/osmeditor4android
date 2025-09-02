package de.blau.android.imagestorage;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.net.URL;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.ErrorCodes;
import okhttp3.Response;

public final class Util {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Util.class.getSimpleName().length());
    private static final String DEBUG_TAG = Util.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Private constructor
     */
    private Util() {
        // empty
    }

    /**
     * Try to retrieve a JsonObject otherwise thow an exception
     * 
     * @param parent the parent JsonObject
     * @param name the name
     * @return a JsonObject
     * @throws IOException
     */
    static JsonObject getJsonObject(@NonNull JsonObject parent, @NonNull String name) throws IOException {
        JsonElement obj = parent.get(name);
        if (obj == null || !obj.isJsonObject()) {
            Log.e(DEBUG_TAG, "Unable to retrieve json for " + name);
            throw new IOException("unexpected JSON " + parent.toString());
        }
        return (JsonObject) obj;
    }

    /**
     * Generate an UploadResult from a Response
     * 
     * @param response the Response
     * @return an UploadResult
     */
    @NonNull
    static UploadResult uploadError(@NonNull Response response, @NonNull URL url) {
        UploadResult result = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
        result.setHttpError(response.code());
        String message = response.message();
        if (message == null) {
            try {
                message = new String(response.body().bytes());
            } catch (IOException e) {
                // nothing
            }
        }
        result.setMessage(message);
        result.setUrl(url.toString());
        return result;
    }
}
