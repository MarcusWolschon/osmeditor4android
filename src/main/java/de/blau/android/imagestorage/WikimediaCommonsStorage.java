package de.blau.android.imagestorage;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.R;
import de.blau.android.contract.MimeTypes;
import de.blau.android.net.OAuth2Interceptor;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.ImageStorageConfiguration;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MRUList;
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
    private static final String EOL                                  = "\n";
    private static final String META_INFORMATION                     = "{{Information";
    private static final String META_END                             = "}}";
    private static final String META_DESCRIPTION                     = "|description=";
    private static final Object META_SOURCE                          = "|source=";
    private static final Object META_AUTHOR                          = "|author=";
    private static final String CATEGORY_START                       = "[[Category:";
    private static final String CATEGORY_END                         = "]]";

    private static final String WIKIMEDIA_CATEGORIES = "wikimedia-categories";
    private static final String WIKIMEDIA_AUTHORS    = "wikimedia-authors";
    private static final String WIKIMEDIA_SOURCES    = "wikimedia-sources";

    private static final int MRU_LIST_SIZE = 10;

    private final ImageStorageConfiguration configuration;
    private final OkHttpClient              client;

    /**
     * Meta data for the upload
     */
    private String metaFileName;
    private String metaDescription;
    private String metaSource;
    private String metaAuthor;
    private String metaCategory;

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

                JsonObject root = de.blau.android.imagestorage.Util.parseJsonResponse(sessionTokenCallResponse);
                JsonElement query = de.blau.android.imagestorage.Util.getJsonObject(root, QUERY);
                JsonElement tokens = de.blau.android.imagestorage.Util.getJsonObject((JsonObject) query, TOKENS);
                JsonElement csrfToken = ((JsonObject) tokens).get(CSRFTOKEN);
                if (csrfToken == null || !csrfToken.isJsonPrimitive()) {
                    Log.e(DEBUG_TAG, "Unable to retrieve token");
                    throw new IOException("unexpected JSON " + (csrfToken != null ? csrfToken.toString() : NULL));
                }

                Log.d(DEBUG_TAG, "csrftoken " + csrfToken.toString());

                body = RequestBody.create(MediaType.parse(MimeTypes.JPEG), imageFile);

                String metaText = buildMetaText(context);
                // @formatter:off
                MultipartBody multipartBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(ACTION, UPLOAD)
                        .addFormDataPart(FILENAME, metaFileName != null ? metaFileName : imageFile.getName())
                        .addFormDataPart(COMMENT, IMAGE_UPLOAD_WITH_VESPUCCI)
                        .addFormDataPart(TEXT, metaText)
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
                    root = de.blau.android.imagestorage.Util.parseJsonResponse(uploadCallResponse);
                    Log.d(DEBUG_TAG, root.toString());
                    if (root.has(ERROR)) {
                        final String error = de.blau.android.imagestorage.Util.getJsonObject(root, ERROR).toString();
                        Log.e(DEBUG_TAG, "Upload returned error " + error);
                        UploadResult uploadResult = new UploadResult(ErrorCodes.UPLOAD_PROBLEM);
                        uploadResult.setMessage(error);
                        return uploadResult;
                    }
                    JsonElement upload = de.blau.android.imagestorage.Util.getJsonObject(root, UPLOAD);
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

    /**
     * Build the text with meta information
     * 
     * @param context an Android Context
     * @return a String with the text
     */
    @NonNull
    private String buildMetaText(Context context) {
        StringBuilder metaText = new StringBuilder();
        if (metaDescription != null || metaSource != null || metaAuthor != null) {
            metaText.append(META_INFORMATION).append(EOL);
            if (metaDescription != null) {
                metaText.append(META_DESCRIPTION).append(metaDescription).append(EOL);
            }
            if (metaSource != null) {
                metaText.append(META_SOURCE).append(metaSource).append(EOL);
            }
            if (metaAuthor != null) {
                metaText.append(META_AUTHOR).append(metaAuthor).append(EOL);
            }
            metaText.append(META_END).append(EOL);
        }
        metaText.append(App.getPreferences(context).getImageLicence());
        if (metaCategory != null) {
            metaText.append(CATEGORY_START).append(metaCategory).append(CATEGORY_END);
        }
        return metaText.toString();
    }

    @Override
    public void addTag(String url, Map<String, String> tags) {
        // if the key already exists we add a numeric suffix
        Util.addTagWithNumericSuffix(Tags.KEY_WIKIMEDIA_COMMONS, url, tags);
    }

    @Override
    public boolean canSetMetaData() {
        return true;
    }

    private SavingHelper<MRUList<String>> listSaver = new SavingHelper<>();

    @Override
    public void setMetaData(Context context, @NonNull File imageFile, @NonNull Runnable upload) {
        final Preferences prefs = App.getPreferences(context);
        AlertDialog.Builder builder = ThemeUtils.getAlertDialogBuilder(context, prefs);
        builder.setTitle(R.string.wikimedia_meta_title);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(context);
        final View layout = inflater.inflate(R.layout.wikimedia_meta, null);
        builder.setView(layout);
        final EditText fileName = layout.findViewById(R.id.imageFileName);
        fileName.setText(imageFile.getName());
        final EditText description = layout.findViewById(R.id.imageDescription);
        final AutoCompleteTextView source = layout.findViewById(R.id.imageSource);
        final MRUList<String> sources = loadAndSetAdapter(context, WIKIMEDIA_SOURCES, source);
        final AutoCompleteTextView author = layout.findViewById(R.id.imageAuthor);
        final MRUList<String> authors = loadAndSetAdapter(context, WIKIMEDIA_AUTHORS, author);
        final Spinner licence = layout.findViewById(R.id.imageLicence);
        licence.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.setImageLicence(context.getResources().getStringArray(R.array.licence_values)[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing
            }
        });

        final AutoCompleteTextView category = layout.findViewById(R.id.imageCategory);
        final MRUList<String> categories = loadAndSetAdapter(context, WIKIMEDIA_CATEGORIES, category);

        builder.setNeutralButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.image_upload, (DialogInterface d, int pos) -> {
            metaFileName = fileName.getText().toString().trim();
            metaDescription = description.getText().toString().trim();
            metaSource = source.getText().toString().trim();
            sources.push(metaSource);
            listSaver.save(context, WIKIMEDIA_SOURCES, sources, false);
            metaAuthor = author.getText().toString().trim();
            authors.push(metaAuthor);
            listSaver.save(context, WIKIMEDIA_AUTHORS, authors, false);
            metaCategory = category.getText().toString().trim();
            categories.push(metaCategory);
            listSaver.save(context, WIKIMEDIA_CATEGORIES, categories, false);
            upload.run();
        });
        builder.show();
    }

    /**
     * Load saved strings, create and set an adapter on the text view
     * 
     * @param context an Android Context
     * @param saveFileName the name of the file to load
     * @param textView the target ActoCompleteTextView
     */
    private MRUList<String> loadAndSetAdapter(@NonNull Context context, @NonNull String saveFileName, @NonNull final AutoCompleteTextView textView) {
        MRUList<String> savedList = listSaver.load(context, saveFileName, false);
        if (savedList == null) {
            savedList = new MRUList<>(MRU_LIST_SIZE);
        }
        textView.setAdapter(new ArrayAdapter<String>(context, R.layout.autocomplete_row, savedList));
        // prefill with last value
        if (!savedList.isEmpty()) {
            textView.setText(savedList.get(0));
        }
        textView.setOnClickListener(v -> {
            if (v.hasFocus()) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
        return savedList;
    }
}
