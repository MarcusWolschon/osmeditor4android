package de.blau.android.util;

import java.io.IOException;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Schemes;

public final class ContentProviderUtil {

    private static final String DEBUG_TAG = ContentProviderUtil.class.getSimpleName();

    private static final String PRIMARY                   = "primary";
    private static final String MY_DOWNLOADS              = "content://downloads/my_downloads";
    private static final String PUBLIC_DOWNLOADS          = "content://downloads/public_downloads";
    private static final String RAW_PREFIX                = Schemes.RAW + ":";
    private static final String DOWNLOADS_DOCUMENTS       = "com.android.providers.downloads.documents";
    private static final String EXTERNALSTORAGE_DOCUMENTS = "com.android.externalstorage.documents";

    /**
     * Private constructor
     */
    private ContentProviderUtil() {
        // do nothing
    }

    /**
     * See https://stackoverflow.com/questions/19985286/convert-content-uri-to-actual-path-in-android-4-4/27271131
     * 
     * Get a file path from a Uri. This will get the path for Storage Access Framework Documents, as well as the _data
     * field for the MediaStore and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @return the path
     * @author paulburke
     * @author Simon Poole
     */
    @Nullable
    public static String getPath(@NonNull Context context, @NonNull Uri uri) {
        Log.d(DEBUG_TAG, "getPath uri: " + uri.toString());
        final String scheme = uri.getScheme();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                Log.i(DEBUG_TAG, "isExternalStorageDocument");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if (PRIMARY.equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + Paths.DELIMITER + split[1];
                } else {
                    Log.e(DEBUG_TAG, "unknown doc type " + type);
                }
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                Log.i(DEBUG_TAG, "isDownloadsDocument");
                final String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith(RAW_PREFIX)) {
                    return id.substring(RAW_PREFIX.length());
                }
                try {
                    long longId = Long.parseLong(id);
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(PUBLIC_DOWNLOADS), longId);
                    String path = getDataColumn(context, contentUri, null, null);
                    if (path == null) { // maybe Oreo, maybe some specific devices
                        contentUri = ContentUris.withAppendedId(Uri.parse(MY_DOWNLOADS), longId);
                        return getDataColumn(context, contentUri, null, null);
                    }
                    return path;
                } catch (NumberFormatException nfex) {
                    Log.e(DEBUG_TAG, "getPath " + id + " id not a long");
                }
            } else if (Schemes.CONTENT.equalsIgnoreCase(scheme)) {
                Log.i(DEBUG_TAG, "content scheme");
                return getDataColumn(context, uri, null, null);
            }
        } else if (Schemes.CONTENT.equalsIgnoreCase(scheme) && context.getString(R.string.content_provider).equals(uri.getAuthority())) {
            Log.i(DEBUG_TAG, "Vespucci file provider");
            try {
                return FileUtil.getPublicDirectory() + uri.getPath();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "getPath " + e.getMessage());
                return null;
            }
        } else if (Schemes.FILE.equalsIgnoreCase(scheme)) {
            return uri.getPath();
        }
        Log.e(DEBUG_TAG, "Unable to determine how to handle Uri " + uri);
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other file-based
     * ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @Nullable
    public static String getDataColumn(@NonNull Context context, @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final String[] projection = { MediaStore.MediaColumns.DATA };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, ex.getMessage());
        }
        return null;
    }

    /**
     * Get the value of the display name column for this Uri. This is useful for MediaStore Uris, and other file-based
     * ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @return The value of the _display_name column, which is typically a file name.
     */
    @Nullable
    public static String getDisplaynameColumn(@NonNull Context context, @NonNull Uri uri) {
        final String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(column_index);
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(@NonNull Uri uri) {
        return EXTERNALSTORAGE_DOCUMENTS.equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(@NonNull Uri uri) {
        return DOWNLOADS_DOCUMENTS.equals(uri.getAuthority());
    }
}
