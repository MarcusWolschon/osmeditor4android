
package de.blau.android.util;

import java.io.File;
import java.io.IOException;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Schemes;
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
        String path = App.getPreferences(activity).getString(directoryPrefKey);
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("*/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && path != null) {
                i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(path));
            }
        } else {
            i = new Intent(activity, ThemedFilePickerActivity.class);
            i.putExtra(AbstractFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(AbstractFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(AbstractFilePickerActivity.EXTRA_ALLOW_EXISTING_FILE, true);
            i.putExtra(AbstractFilePickerActivity.EXTRA_MODE, AbstractFilePickerActivity.MODE_NEW_FILE);

            if (path != null) {
                i.putExtra(AbstractFilePickerActivity.EXTRA_START_PATH, path);
            } else {
                try {
                    i.putExtra(AbstractFilePickerActivity.EXTRA_START_PATH, FileUtil.getPublicDirectory().getPath());
                } catch (IOException e) {
                    // if for whatever reason the above doesn't work we use the standard directory
                    Log.d(DEBUG_TAG, "falling back to standard dir instead");
                    i.putExtra(AbstractFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
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
            SelectFile.activity = activity;
        }
        String path = App.getPreferences(activity).getString(directoryPrefKey);
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("*/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && path != null) {
                i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(path));
            }
        } else {
            i = new Intent(activity, ThemedFilePickerActivity.class);

            i.putExtra(AbstractFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(AbstractFilePickerActivity.EXTRA_SINGLE_CLICK, true);
            i.putExtra(AbstractFilePickerActivity.EXTRA_MODE, AbstractFilePickerActivity.MODE_FILE);

            if (path != null) {
                i.putExtra(AbstractFilePickerActivity.EXTRA_START_PATH, path);
            } else {
                try {
                    i.putExtra(AbstractFilePickerActivity.EXTRA_START_PATH, FileUtil.getPublicDirectory().getPath());
                } catch (IOException e) {
                    // if for whatever reason the above doesn't work we use the standard directory
                    Log.d(DEBUG_TAG, "falling back to standard dir instead");
                    i.putExtra(AbstractFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
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
        ContentResolverUtil.persistPermissions(activity, data.getFlags(), uri);
        if (code == SAVE_FILE) {
            File file = new File(uri.getPath());
            if (file.exists()) {
                Snack.barWarning(activity, activity.getResources().getString(R.string.toast_file_exists, file.getName()), R.string.overwrite, v -> {
                    synchronized (saveCallbackLock) {
                        if (saveCallback != null) {
                            saveCallback.save(uri);
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
        } else if (code == READ_FILE || code == READ_FILE_OLD) {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && Schemes.FILE.equals(fileUri.getScheme())) {
            int slash = fileUri.getPath().lastIndexOf('/');
            if (slash >= 0) {
                String path = fileUri.getPath().substring(0, slash + 1);
                prefs.putString(directoryPrefKey, path);
            }
        } else {
            prefs.putString(directoryPrefKey, fileUri.toString());
        }
    }
}
