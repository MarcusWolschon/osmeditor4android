package de.blau.android.imagestorage;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.xmlpull.v1.XmlPullParserException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import de.blau.android.contract.Urls;
import de.blau.android.net.OAuth2Interceptor;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.AdvancedPrefDatabase.ImageStorageType;
import de.blau.android.prefs.ImageStorageConfiguration;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.Util;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PanoramaxStorage implements ImageStorage {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PanoramaxStorage.class.getSimpleName().length());
    private static final String DEBUG_TAG = PanoramaxStorage.class.getSimpleName().substring(0, TAG_LEN);

    private static final String UPLOAD_SET_BODY          = "{" + "\"title\": \"" + "Vespucci upload" + "\"," + "\"estimated_nb_files\": \"1\"}";
    private static final String PICTURE_ID               = "picture_id";
    private static final String EXISTING_ITEM_ID         = "existing_item_id";
    private static final String UPLOAD_SETS              = "upload_sets";
    private static final String FILE                     = "file";
    private static final String ID                       = "id";
    private static final String FILES                    = "/files";
    private static final String API_UPLOAD_SETS          = "/upload_sets";
    private static final String HREF                     = "href";
    private static final String LINKS                    = "links";
    private static final String JWT_TOKEN                = "jwt_token";
    private static final String LABEL                    = "label";
    private static final String NAME                     = "name";
    private static final String REGISTRATION_IS_OPEN     = "registration_is_open";
    private static final String ENABLED                  = "enabled";
    private static final String CONFIGURATION_JSON       = "configuration";
    private static final String INSTANCES_JSON           = "instances";
    private static final String AUTH                     = "auth";
    private static final String URL                      = "url";
    private static final String API_AUTH_TOKENS_GENERATE = "/auth/tokens/generate";
    private static final String API_USERS_ME             = "/users/me";

    private static final long TIMEOUT = 20000;

    private final ImageStorageConfiguration configuration;
    private final OkHttpClient              client;

    public PanoramaxStorage(ImageStorageConfiguration configuration) {
        this.configuration = configuration;
        client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).build();
    }

    @Override
    public boolean authorize(Context context) {
        try {
            return new ExecutorTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void nothing) throws XmlPullParserException, IOException {
                    URL url = new URL(configuration.url + API_AUTH_TOKENS_GENERATE);
                    Request generateKeyRequest = new Request.Builder().url(url).post(RequestBody.create(null, "")).build();
                    try (Response generateKeyCallResponse = client.newCall(generateKeyRequest).execute()) {
                        if (!generateKeyCallResponse.isSuccessful()) {
                            Log.e(DEBUG_TAG, "Creating keys failed " + generateKeyCallResponse.toString());
                            return false;
                        }
                        JsonObject root = de.blau.android.imagestorage.Util.parseJsonResponse(generateKeyCallResponse);

                        String key = root.get(JWT_TOKEN).getAsString();
                        // this should only be set if auth was successful
                        try (KeyDatabaseHelper kdb = new KeyDatabaseHelper(context); SQLiteDatabase db = kdb.getWritableDatabase()) {
                            KeyDatabaseHelper.replaceOrDeleteKey(db, configuration.id, KeyDatabaseHelper.EntryType.PANORAMAX_KEY, key, false, true, null, null);
                        }
                        JsonElement links = root.get(LINKS);
                        if (!links.isJsonArray() && ((JsonArray) links).size() < 1) {
                            Log.e(DEBUG_TAG, "No links array found");
                            return false;
                        }
                        JsonElement link = ((JsonArray) links).get(0);
                        if (!link.isJsonObject()) {
                            Log.e(DEBUG_TAG, "No link found");
                            return false;
                        }
                        JsonElement claimUrl = ((JsonObject) link).get(HREF);
                        if (!claimUrl.isJsonPrimitive()) {
                            Log.e(DEBUG_TAG, "No url found");
                            return false;
                        }
                        // start the actual authorisation
                        Intent intent = new Intent(context, PanoramaxAuthorize.class);
                        intent.putExtra(PanoramaxAuthorize.URL_KEY, claimUrl.getAsString());
                        ((FragmentActivity) context).startActivity(intent);
                        Log.d(DEBUG_TAG, "Claim url " + claimUrl.getAsString());
                    }

                    return false;
                }
            }.execute().get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.d(DEBUG_TAG, "Unable to authorize " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean checkAuthorized(Context context) {
        String key = getKey(context);
        if (key == null) {
            return false;
        }
        try {
            return new ExecutorTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void nothing) throws XmlPullParserException, IOException {
                    OkHttpClient authClient = client.newBuilder().addInterceptor(new OAuth2Interceptor(key)).build();
                    URL url = new URL(configuration.url + API_USERS_ME);
                    Request meRequest = new Request.Builder().url(url).get().build();
                    try (Response meCallResponse = authClient.newCall(meRequest).execute()) {
                        Log.d(DEBUG_TAG, "Authorized " + meCallResponse.toString());
                        return meCallResponse.isSuccessful();
                    }
                }
            }.execute().get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.d(DEBUG_TAG, "Unable to check authorization status " + e.getMessage());
        }
        return false;
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
            return KeyDatabaseHelper.getKey(db, configuration.id, EntryType.PANORAMAX_KEY);
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

            URL url = new URL(configuration.url + API_UPLOAD_SETS);
            Log.d(DEBUG_TAG, "uploadImageToPanoramax " + url.toString());

            RequestBody body = RequestBody.create(MediaType.parse(MimeTypes.JSON), UPLOAD_SET_BODY);

            Request uploadSetRequest = new Request.Builder().url(url).post(body).build();
            try (Response uploadSetsCallResponse = authClient.newCall(uploadSetRequest).execute()) {
                if (!uploadSetsCallResponse.isSuccessful()) {
                    Log.e(DEBUG_TAG, "Creating upload_set failed " + uploadSetsCallResponse.toString());
                    return de.blau.android.imagestorage.Util.uploadError(uploadSetsCallResponse, url);
                }

                JsonObject root = de.blau.android.imagestorage.Util.parseJsonResponse(uploadSetsCallResponse);
                JsonElement uploadSetId = root.get(ID);
                if (uploadSetId == null || !uploadSetId.isJsonPrimitive()) {
                    Log.e(DEBUG_TAG, "Unable to retrieve upload set id");
                    throw new IOException("unexpected JSON " + root.toString());
                }
                Log.d(DEBUG_TAG, "Upload set id " + uploadSetId);
                body = RequestBody.create(MediaType.parse(MimeTypes.JPEG), imageFile);

                MultipartBody multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(FILE, imageFile.getName(), body).build();
                final URL uploadSetUrl = new URL(url.toString() + "/" + uploadSetId.getAsString() + FILES);
                Log.d(DEBUG_TAG, "Upload url " + uploadSetUrl);
                Request uploadRequest = new Request.Builder().url(uploadSetUrl).post(multipartBody).build();
                Log.d(DEBUG_TAG, "Uploading image");

                try (Response uploadCallResponse = authClient.newCall(uploadRequest).execute()) {
                    final boolean duplicate = uploadCallResponse.code() == HttpURLConnection.HTTP_CONFLICT;
                    if (!uploadCallResponse.isSuccessful()) {
                        Log.e(DEBUG_TAG, "Upload failed " + uploadCallResponse.toString() + " duplicate " + duplicate);
                        if (!duplicate) {
                            return de.blau.android.imagestorage.Util.uploadError(uploadCallResponse, uploadSetUrl);
                        }
                    }
                    root = de.blau.android.imagestorage.Util.parseJsonResponse(uploadCallResponse);
                    JsonElement pictureId = !duplicate ? root.get(PICTURE_ID) : extractExistingItemId(root);
                    if (pictureId != null) {
                        UploadResult result = new UploadResult(ErrorCodes.OK);
                        result.setUrl(pictureId.getAsString());
                        return result;
                    }
                    Log.e(DEBUG_TAG, "upload id not found in response");
                }
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "upload " + e.getClass().getCanonicalName() + " " + e.getMessage());
            UploadResult result = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
            result.setMessage(e.getMessage());
            return result;
        }
        return new UploadResult(ErrorCodes.UNKNOWN_ERROR);
    }

    /**
     * Get an "existing_item_id" from the error response
     * 
     * @param root the JsonObject returned as the response
     * @return the id or null
     */
    @Nullable
    private JsonElement extractExistingItemId(@NonNull JsonObject root) {
        JsonElement uploadSets = root.get(UPLOAD_SETS);
        if ((uploadSets instanceof JsonArray) && (((JsonArray) uploadSets).size() > 0)) {
            return ((JsonObject) ((JsonArray) uploadSets).get(0)).get(EXISTING_ITEM_ID);
        }
        return null;
    }

    @Override
    public void addTag(String url, Map<String, String> tags) {
        // if the key already exists we add a numeric suffix
        Util.addTagWithNumericSuffix(Tags.KEY_PANORAMAX, url, tags);
    }

    @NonNull
    public static List<ImageStorageConfiguration> getInstances(@NonNull Context context) {
        final ExecutorTask<Void, Void, List<ImageStorageConfiguration>> getInstancesTask = new ExecutorTask<Void, Void, List<ImageStorageConfiguration>>() {
            @Override
            protected List<ImageStorageConfiguration> doInBackground(Void nothing) throws IOException {

                List<ImageStorageConfiguration> result = new ArrayList<>();

                URL url = new URL(Urls.PANORAMAX_EXPLORE);
                Request instancesRequest = new Request.Builder().url(url).get().build();
                try (Response instancesResponse = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).build().newCall(instancesRequest).execute()) {
                    if (!instancesResponse.isSuccessful()) {
                        Log.e(DEBUG_TAG, "Retrieving instances failed " + instancesResponse.toString());
                        return result;
                    }

                    JsonObject root = de.blau.android.imagestorage.Util.parseJsonResponse(instancesResponse);

                    JsonElement instances = root.get(INSTANCES_JSON);
                    if (instances == null || !instances.isJsonArray()) {
                        throw new IOException("unexpected JSON for instance array " + root.toString());
                    }
                    for (JsonElement e : ((JsonArray) instances)) {
                        if (!e.isJsonObject()) {
                            throw new IOException("unexpected JSON for instance " + e.toString());
                        }
                        JsonElement instanceUrl = ((JsonObject) e).get(URL);
                        JsonObject instanceConfiguration = de.blau.android.imagestorage.Util.getJsonObject((JsonObject) e, CONFIGURATION_JSON);
                        JsonObject auth = de.blau.android.imagestorage.Util.getJsonObject(instanceConfiguration, AUTH);
                        if (!auth.get(ENABLED).getAsBoolean() || !auth.get(REGISTRATION_IS_OPEN).getAsBoolean()) {
                            continue;
                        }
                        JsonObject name = de.blau.android.imagestorage.Util.getJsonObject(instanceConfiguration, NAME);
                        JsonElement label = name.get(LABEL);
                        if (instanceUrl.isJsonPrimitive() && label.isJsonPrimitive()) {
                            ImageStorageConfiguration imageStoreConf = new ImageStorageConfiguration(null, label.getAsString(), ImageStorageType.PANORAMAX,
                                    instanceUrl.getAsString(), false);
                            result.add(imageStoreConf);
                        }
                    }
                }
                return result;
            }

            @Override
            protected void onBackgroundError(Exception e) {
                Log.e(DEBUG_TAG, "getInstances " + e.getMessage());
            }
        };
        try {
            return getInstancesTask.execute().get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.d(DEBUG_TAG, "Unable to return instances " + e.getMessage());
        }
        return new ArrayList<>();
    }
}
