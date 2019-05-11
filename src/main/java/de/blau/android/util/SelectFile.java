
package de.blau.android.util;

import java.io.File;
import java.io.IOException;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 * Helper class that tries to provide some minimal file selector functionality for all supported Android versions
 * 
 * @author Simon Poole
 *
 */
public final class SelectFile {

    private static final String DEBUG_TAG = SelectFile.class.getName();

    public static final int SAVE_FILE     = 7113;
    public static final int READ_FILE     = 9340;
    public static final int READ_FILE_OLD = 9341;

    private static SaveFile     saveCallback;
    private static final Object saveCallbackLock = new Object();

    private static ReadFile         readCallback;
    private static final Object     readCallbackLock = new Object();
    private static FragmentActivity activity         = null;

    /**
     * Unused default constructor
     */
    private SelectFile() {
        // hide default constructor
    }

    /**
     * Save a file
     * 
     * @param activity activity that called us
     * @param directoryPrefKey string resources for shared preferences for preferred (last) directory
     * @param callback callback that does the actual saving, should call {@link #savePref(Preferences, int, Uri)}
     */
    public static void save(@NonNull FragmentActivity activity, int directoryPrefKey, @NonNull de.blau.android.util.SaveFile callback) {
        synchronized (saveCallbackLock) {
            saveCallback = callback;
            SelectFile.activity = activity;
        }
        Preferences prefs = new Preferences(activity);
        String path = prefs.getString(directoryPrefKey);
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("*/*");
            if (path != null) {
                i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(path));
            }
        } else {
            i = new Intent(activity, ThemedFilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_EXISTING_FILE, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_NEW_FILE);

            if (path != null) {
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, path);
            } else {
                try {
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, FileUtil.getPublicDirectory().getPath());
                } catch (IOException e) {
                    // if for whatever reason the above doesn't work we use the standard directory
                    Log.d(DEBUG_TAG, "falling back to standard dir instead");
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                }
            }
        }
        activity.startActivityForResult(i, SAVE_FILE);
    }

    /**
     * @param activity activity activity that called us
     * @param directoryPrefKey string resources for shared preferences for preferred (last) directory
     * @param readFile callback callback that does the actual saving, should call
     *            {@link #savePref(Preferences, int, Uri)}
     */
    public static void read(@NonNull FragmentActivity activity, int directoryPrefKey, @NonNull ReadFile readFile) {
        synchronized (readCallbackLock) {
            readCallback = readFile;
        }
        Preferences prefs = new Preferences(activity);
        String path = prefs.getString(directoryPrefKey);
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("*/*");
            if (path != null) {
                i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(path));
            }
        } else {
            i = new Intent(activity, ThemedFilePickerActivity.class);

            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

            if (path != null) {
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, path);
            } else {
                try {
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, FileUtil.getPublicDirectory().getPath());
                } catch (IOException e) {
                    // if for whatever reason the above doesn't work we use the standard directory
                    Log.d(DEBUG_TAG, "falling back to standard dir instead");
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                }
            }
        }
        activity.startActivityForResult(i, READ_FILE);
    }

    /**
     * Handle the file selector result
     * 
     * @param code returned request code
     * @param data the returned intent
     */
    public static void handleResult(int code, @NonNull Intent data) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uri = data.getData();
        } else {
            uri = Uri.fromFile(com.nononsenseapps.filepicker.Utils.getFileForUri(data.getData()));
        }
        if (code == SAVE_FILE) {
            File file = new File(uri.getPath());
            if (file.exists()) {
                Snack.barWarning(activity, activity.getResources().getString(R.string.toast_file_exists, file.getName()), R.string.overwrite,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                synchronized (saveCallbackLock) {
                                    if (saveCallback != null) {
                                        saveCallback.save(uri);
                                    }
                                }
                            }
                        });
            }
            synchronized (saveCallbackLock) {
                if (saveCallback != null) {
                    Log.d(DEBUG_TAG, "saving to " + uri);
                    saveCallback.save(uri);
                }
            }
        } else if (code == READ_FILE) {
            synchronized (readCallbackLock) {
                if (readCallback != null) {
                    Log.d(DEBUG_TAG, "reading " + uri);
                    readCallback.read(uri);
                }
            }
        } else if (code == READ_FILE_OLD) {
            synchronized (readCallbackLock) {
                if (readCallback != null) {
                    Log.d(DEBUG_TAG, "reading " + uri);
                    readCallback.read(uri);
                }
            }
        }
    }

    /**
     * Save the director path to shared preferences
     * 
     * @param prefs the Preferences instance
     * @param directoryPrefKey the key
     * @param fileUri the file uri
     */
    public static void savePref(Preferences prefs, int directoryPrefKey, Uri fileUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && FileUtil.FILE_SCHEME.equals(fileUri.getScheme())) {
            int slash = fileUri.getPath().lastIndexOf('/');
            if (slash >= 0) {
                String path = fileUri.getPath().substring(0, slash + 1);
                prefs.putString(directoryPrefKey, path);
            }
        } else {
            prefs.putString(directoryPrefKey, fileUri.toString());
        }
    }

    /**
     * See https://stackoverflow.com/questions/19985286/convert-content-uri-to-actual-path-in-android-4-4/27271131
     * 
     * Get a file path from a Uri. This will get the the path for Storage Access Framework Documents, as well as the
     * _data field for the MediaStore and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @return the path
     * @author paulburke
     */
    public static String getPath(@NonNull Context context, @NonNull Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
        } else if (FileUtil.FILE_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
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
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        final String column = "_data";
        final String[] projection = { column };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
}
