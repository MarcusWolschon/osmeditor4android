package io.vespucci.prefs;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.App;
import io.vespucci.contract.FileExtensions;
import io.vespucci.contract.MimeTypes;
import io.vespucci.contract.Paths;
import io.vespucci.osm.Server;
import io.vespucci.services.util.StreamUtils;
import io.vespucci.util.FileUtil;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class PresetLoader {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PresetLoader.class.getSimpleName().length());
    private static final String DEBUG_TAG = PresetLoader.class.getSimpleName().substring(0, TAG_LEN);

    private static final String FILE_NAME_TEMPORARY_ARCHIVE = "temp.zip";

    public static final int DOWNLOADED_PRESET_ERROR = -1;
    public static final int DOWNLOADED_PRESET_XML   = 0;
    public static final int DOWNLOADED_PRESET_ZIP   = 1;

    /**
     * Private constructor to stop instantiation
     */
    private PresetLoader() {
        // empty
    }

    /**
     * Download a Preset
     * 
     * @param url the url to download from
     * @param presetDir the directory to save the preset to
     * @param filename A filename where to save the file.
     * @return code indicating result
     */
    public static int download(@NonNull String url, @NonNull File presetDir, @NonNull String filename) {
        Log.d(DEBUG_TAG, "Downloading " + url + " to " + presetDir + "/" + filename);
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(Server.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).build();
        Call presetCall = client.newCall(request);
        try (Response presetCallResponse = presetCall.execute()) {
            if (presetCallResponse.isSuccessful()) {
                ResponseBody responseBody = presetCallResponse.body();
                InputStream downloadStream = responseBody.byteStream();
                String contentType = responseBody.contentType().toString();
                boolean zip = (contentType != null && MimeTypes.ZIP.equalsIgnoreCase(contentType))
                        || url.toLowerCase(Locale.US).endsWith("." + FileExtensions.ZIP);
                if (zip) {
                    Log.d(DEBUG_TAG, "detected zip file");
                    filename = System.currentTimeMillis() + FILE_NAME_TEMPORARY_ARCHIVE;
                }
                final File destinationFile = new File(presetDir, filename);
                try (OutputStream fileStream = new FileOutputStream(destinationFile)) {
                    StreamUtils.copy(downloadStream, fileStream);
                    if (zip && FileUtil.unpackZip(presetDir.getPath() + Paths.DELIMITER, filename)) {
                        if (!destinationFile.delete()) { // NOSONAR requires API 26
                            Log.e(DEBUG_TAG, "Could not delete " + filename);
                        }
                        return DOWNLOADED_PRESET_ZIP;
                    }
                    return DOWNLOADED_PRESET_XML;
                }
            } else {
                Log.w(DEBUG_TAG, "Could not download file " + url + " respose code " + presetCallResponse.code());
                return DOWNLOADED_PRESET_ERROR;
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Could not download file " + url + " " + e.getMessage());
            return DOWNLOADED_PRESET_ERROR;
        }
    }

    /**
     * Load a Preset from a local file
     * 
     * @param context an Android Context
     * @param uri the uri to load from
     * @param presetDir the directory to save the preset to
     * @param filename A filename where to save the file.
     * 
     * @return code indicating result
     */
    public static int load(@NonNull Context context, @NonNull Uri uri, @NonNull File presetDir, @NonNull String filename) {
        final ContentResolver contentResolver = context.getContentResolver();
        boolean zip = uri.getPath().toLowerCase(Locale.US).endsWith("." + FileExtensions.ZIP) || MimeTypes.ZIP.equals(contentResolver.getType(uri));
        if (zip) {
            Log.d(DEBUG_TAG, "detected zip file");
            filename = System.currentTimeMillis() + FILE_NAME_TEMPORARY_ARCHIVE;
        }
        final File destinationFile = new File(presetDir, filename);
        try (InputStream loadStream = contentResolver.openInputStream(uri); OutputStream fileStream = new FileOutputStream(destinationFile);) {
            Log.d(DEBUG_TAG, "Loading " + uri + " to " + presetDir + Paths.DELIMITER + filename);
            StreamUtils.copy(loadStream, fileStream);
            if (zip && FileUtil.unpackZip(presetDir.getPath() + Paths.DELIMITER, filename)) {
                if (!destinationFile.delete()) { // NOSONAR requires API 26
                    Log.e(DEBUG_TAG, "Could not delete " + filename);
                }
                return DOWNLOADED_PRESET_ZIP;
            }
            return DOWNLOADED_PRESET_XML;
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Could not load file " + uri + " " + e.getMessage());
            return DOWNLOADED_PRESET_ERROR;
        }
    }

}
