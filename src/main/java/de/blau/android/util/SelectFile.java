
package de.blau.android.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import de.blau.android.R;
import de.blau.android.dialogs.GetFileName;
import de.blau.android.prefs.Preferences;

/**
 * Helper class that tries to provide some minimal file selector functionality for all supported Android versions
 * 
 * @author simon
 *
 */
public class SelectFile {

    private static final String DEBUG_TAG = SelectFile.class.getName();

    public static final int SAVE_FILE     = 7113;
    public static final int READ_FILE     = 9340;
    public static final int READ_FILE_OLD = 9341;

    private static SaveFile     saveCallback;
    private final static Object saveCallbackLock = new Object();

    private static ReadFile         readCallback;
    private final static Object     readCallbackLock = new Object();
    private static FragmentActivity activity         = null;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Intent i = new Intent(activity, ThemedFilePickerActivity.class);

            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_EXISTING_FILE, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_NEW_FILE);

            Preferences prefs = new Preferences(activity);
            String path = prefs.getString(directoryPrefKey);

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

            activity.startActivityForResult(i, SAVE_FILE);
        } else {
            GetFileName.showDialog(activity, callback);
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Intent i = new Intent(activity, ThemedFilePickerActivity.class);

            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

            Preferences prefs = new Preferences(activity);
            String path = prefs.getString(directoryPrefKey);

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

            activity.startActivityForResult(i, READ_FILE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                activity.startActivityForResult(intent, READ_FILE_OLD);
            } catch (android.content.ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
                Snack.barError(activity, R.string.toast_missing_filemanager);
            }
        }
    }

    /**
     * Handle the file selector result
     * 
     * @param code returned request code
     * @param data the returned intent
     */
    public static void handleResult(int code, Intent data) {
        // for now this doesn't do anything when multiple files are selected
        if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
            // For JellyBean and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ClipData clip = data.getClipData();

                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                        // Do something with the URI
                    }
                }
                // For Ice Cream Sandwich
            } else {
                ArrayList<String> paths = data.getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);

                if (paths != null) {
                    for (String path : paths) {
                        Uri uri = Uri.parse(path);
                        // Do something with the URI
                    }
                }
            }
        } else {
            final Uri uri = data.getData();
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
    }

    /**
     * Save the director path to shared preferences
     * 
     * @param prefs the Preferences instance
     * @param directoryPrefKey the key
     * @param fileUri the file uri
     */
    public static void savePref(Preferences prefs, int directoryPrefKey, Uri fileUri) {
        if (fileUri.getScheme().equals("file")) {
            int slash = fileUri.getPath().lastIndexOf('/');
            if (slash >= 0) {
                String path = fileUri.getPath().substring(0, slash + 1);
                prefs.putString(directoryPrefKey, path);
            }
        }
    }
}
