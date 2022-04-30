package de.blau.android.util;

import java.io.IOException;
import java.util.Locale;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
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

public final class ContentResolverUtil {

    private static final String DEBUG_TAG = ContentResolverUtil.class.getSimpleName();

    private static final String PRIMARY                   = "primary";
    private static final String MY_DOWNLOADS              = "content://downloads/my_downloads";
    private static final String PUBLIC_DOWNLOADS          = "content://downloads/public_downloads";
    private static final String RAW_PREFIX                = Schemes.RAW + ":";
    private static final String DOWNLOADS_DOCUMENTS       = "com.android.providers.downloads.documents";
    private static final String EXTERNALSTORAGE_DOCUMENTS = "com.android.externalstorage.documents";

    /**
     * Private constructor
     */
    private ContentResolverUtil() {
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
        final String scheme = uri.getScheme().toLowerCase(Locale.US);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            return getPathFromDocumentUri(context, scheme, uri);
        } else if (Schemes.CONTENT.equals(scheme) && context.getString(R.string.content_provider).equals(uri.getAuthority())) {
            Log.i(DEBUG_TAG, "Vespucci file provider");
            try {
                return FileUtil.getPublicDirectory() + uri.getPath();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "getPath " + e.getMessage());
                return null;
            }
        } else if (Schemes.FILE.equals(scheme)) {
            return uri.getPath();
        }
        Log.e(DEBUG_TAG, "Unable to determine how to handle Uri " + uri);
        return null;
    }

    /**
     * Try to determine the actual file path from a Document Uri
     * 
     * @param context an Android Context
     * @param scheme the scheme of the Uri
     * @param uri the Uri
     * @return a path or null
     */
    @Nullable
    private static String getPathFromDocumentUri(@NonNull Context context, @Nullable String scheme, @NonNull Uri uri) {
        if (isExternalStorageDocument(uri)) {
            Log.i(DEBUG_TAG, "isExternalStorageDocument");
            final String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            final String type = docId.split(":")[0];

            if (PRIMARY.equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + Paths.DELIMITER + split[1]; // NOSONAR
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
        } else if (Schemes.CONTENT.equals(scheme)) {
            Log.i(DEBUG_TAG, "content scheme");
            return getDataColumn(context, uri, null, null);
        }
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
        final String[] projection = { MediaStore.MediaColumns.DATA }; // NOSONAR
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA); // NOSONAR
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

    /**
     * Persist file access permissions if possible
     * 
     * @param context an Android Context
     * @param intentFlags the flags from the Intent
     * @param uri the Uri
     * @return true if things seemed to work
     */
    public static boolean persistPermissions(@NonNull Context context, int intentFlags, @NonNull Uri uri) {
        if ((intentFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
            Log.d(DEBUG_TAG, "Persisting permissions for " + uri);
            try {
                context.getContentResolver().takePersistableUriPermission(uri,
                        intentFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                return true;
            } catch (Exception ex) {
                Log.e(DEBUG_TAG, "Unable to persist read permission for " + uri);
                Snack.toastTopWarning(context, R.string.toast_unable_to_persist_permissions);
            }
        }
        return false;
    }
}
