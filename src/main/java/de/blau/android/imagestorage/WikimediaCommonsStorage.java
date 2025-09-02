package de.blau.android.imagestorage;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.contract.MimeTypes;
import de.blau.android.net.OAuth2Interceptor;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.ImageStorageConfiguration;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.Util;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WikimediaCommonsStorage implements ImageStorage {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WikimediaCommonsStorage.class.getSimpleName().length());
    private static final String DEBUG_TAG = WikimediaCommonsStorage.class.getSimpleName().substring(0, TAG_LEN);

    private static final long TIMEOUT = 10000;

    private static final String ACTION_QUERY_META_TOKENS_FORMAT_JSON = "action=query&meta=tokens&format=json";
    private static final String W_API_PHP                            = "/w/api.php";
    private static final String QUERY                                = "query";
    private static final String TOKENS                               = "tokens";
    private static final String CSRFTOKEN                            = "csrftoken";
    private static final String NULL                                 = "null";
    private static final String ACTION                               = "action";
    private static final String FILENAME                             = "filename";
    private static final String TOKEN                                = "token";
    private static final String FILE                                 = "file";
    private static final String IGNOREWARNINGS                       = "ignorewarnings";
    private static final String JSON                                 = "json";
    private static final String FORMAT                               = "format";
    private static final String IMAGE_UPLOAD_WITH_VESPUCCI           = "Image upload with Vespucci";
    private static final String COMMENT                              = "comment";
    private static final String ERROR                                = "error";
    private static final String UPLOAD                               = "upload";
    private static final String IMAGEINFO                            = "imageinfo";
    private static final String CANONICALTITLE                       = "canonicaltitle";
    private static final String TEXT                                 = "text";

    private final ImageStorageConfiguration configuration;
    private final OkHttpClient              client;

    public WikimediaCommonsStorage(@NonNull ImageStorageConfiguration configuration) {
        this.configuration = configuration;
        client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).build();
    }

    @Override
    public boolean authorize(Context context) {
        // start the actual authorisation
        Intent intent = new Intent(context, WikimediaCommonsAuthorize.class);
        intent.putExtra(WikimediaCommonsAuthorize.CONFIGURATION_KEY, configuration);
        ((FragmentActivity) context).startActivity(intent);

        return false;
    }

    @Override
    public boolean checkAuthorized(Context context) {
        return getKey(context) != null;
    }

    /**
     * Get the key for the target service
     * 
     * @param context an Android Context
     * @return the key or null
     */
    @Nullable
    private String getKey(@NonNull Context context) {
        try (KeyDatabaseHelper kdb = new KeyDatabaseHelper(context); SQLiteDatabase db = kdb.getWritableDatabase()) {
            return KeyDatabaseHelper.getKey(db, configuration.id, EntryType.WIKIMEDIA_COMMONS_KEY);
        }
    }

    @Override
    public UploadResult upload(Context context, File imageFile) {
        try {
            // check if authorized
            if (!checkAuthorized(context)) {
                Log.e(DEBUG_TAG, "Not authorized");
                return new UploadResult(ErrorCodes.FORBIDDEN);
            }
            OkHttpClient authClient = client.newBuilder().addInterceptor(new OAuth2Interceptor(getKey(context))).build();

            // get session key
            URL url = new URL(configuration.url + W_API_PHP + "?" + ACTION_QUERY_META_TOKENS_FORMAT_JSON);

            RequestBody body = RequestBody.create(MediaType.parse(MimeTypes.JSON), "");

            Request sessionKeyRequest = new Request.Builder().url(url).post(body).build();
            try (Response sessionTokenCallResponse = authClient.newCall(sessionKeyRequest).execute()) {
                if (!sessionTokenCallResponse.isSuccessful()) {
                    Log.e(DEBUG_TAG, "Retrieving session token failed " + sessionTokenCallResponse.toString());
                    return de.blau.android.imagestorage.Util.uploadError(sessionTokenCallResponse, url);
                }

                JsonElement root = JsonParser.parseReader(sessionTokenCallResponse.body().charStream());
                if (root == null || !root.isJsonObject()) {
                    Log.e(DEBUG_TAG, "Unable to retrieve session token");
                    throw new IOException("unexpected JSON " + (root != null ? root.toString() : NULL));
                }
                JsonElement query = de.blau.android.imagestorage.Util.getJsonObject((JsonObject) root, QUERY);
                JsonElement tokens = de.blau.android.imagestorage.Util.getJsonObject((JsonObject) query, TOKENS);
                JsonElement csrfToken = ((JsonObject) tokens).get(CSRFTOKEN);
                if (csrfToken == null || !csrfToken.isJsonPrimitive()) {
                    Log.e(DEBUG_TAG, "Unable to retrieve token");
                    throw new IOException("unexpected JSON " + (csrfToken != null ? csrfToken.toString() : NULL));
                }

                Log.d(DEBUG_TAG, "csrftoken " + csrfToken.toString());

                body = RequestBody.create(MediaType.parse(MimeTypes.JPEG), imageFile);
                // @formatter:off
                MultipartBody multipartBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(ACTION, UPLOAD)
                        .addFormDataPart(FILENAME, imageFile.getName())
                        .addFormDataPart(COMMENT, IMAGE_UPLOAD_WITH_VESPUCCI)
                        .addFormDataPart(TEXT, App.getPreferences(context).getImageLicence())
                        .addFormDataPart(TOKEN, csrfToken.getAsString())
                        .addFormDataPart(FORMAT, JSON)
                        .addFormDataPart(IGNOREWARNINGS, "1")
                        .addFormDataPart(FILE, imageFile.getName(), body)
                        .build();
                // @formatter:on
                final URL uploadUrl = new URL(configuration.url + W_API_PHP);
                Log.d(DEBUG_TAG, "Upload url " + uploadUrl);
                Request uploadRequest = new Request.Builder().url(uploadUrl).post(multipartBody).build();
                Log.d(DEBUG_TAG, "Uploading image");

                try (Response uploadCallResponse = authClient.newCall(uploadRequest).execute()) {
                    if (!uploadCallResponse.isSuccessful()) {
                        Log.e(DEBUG_TAG, "Upload failed " + uploadCallResponse.toString());
                        return de.blau.android.imagestorage.Util.uploadError(uploadCallResponse, uploadUrl);
                    }
                    root = JsonParser.parseReader(uploadCallResponse.body().charStream());
                    if (!root.isJsonObject()) {
                        throw new IOException("unexpected JSON " + root.toString());
                    }
                    Log.d(DEBUG_TAG, root.toString());
                    if (((JsonObject) root).has(ERROR)) {
                        final String error = de.blau.android.imagestorage.Util.getJsonObject((JsonObject) root, ERROR).toString();
                        Log.e(DEBUG_TAG, "Upload returned error " + error);
                        UploadResult uploadResult = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
                        uploadResult.setMessage(error);
                        return uploadResult;
                    }
                    JsonElement upload = de.blau.android.imagestorage.Util.getJsonObject((JsonObject) root, UPLOAD);
                    JsonElement imageInfo = de.blau.android.imagestorage.Util.getJsonObject((JsonObject) upload, IMAGEINFO);
                    JsonElement canonicalTitle = ((JsonObject) imageInfo).get(CANONICALTITLE);
                    if (canonicalTitle != null) {
                        UploadResult uploadResult = new UploadResult(ErrorCodes.OK);
                        uploadResult.setUrl(canonicalTitle.getAsString());
                        return uploadResult;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "uploadImage " + e.getMessage());
            UploadResult result = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
            result.setMessage(e.getMessage());
            return result;
        }
        return new UploadResult(ErrorCodes.UNKNOWN_ERROR);
    }

    @Override
    public void addTag(String url, Map<String, String> tags) {
        // if the key already exists we add a numeric suffix
        Util.addTagWithNumericSuffix(Tags.KEY_WIKIMEDIA_COMMONS, url, tags);
    }

    @NonNull
    public static List<ImageStorageConfiguration> getInstances(@NonNull Context context) {
        // not used for WM
        return new ArrayList<>();
    }
}
