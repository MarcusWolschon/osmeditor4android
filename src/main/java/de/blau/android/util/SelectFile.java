
package de.blau.android.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.NetworkOnMainThreadException;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.R;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.PresetElement;

/**
 * Helper class that tries to provide some minimal file selector functionality for all supported Android versions
 * 
 * @author Simon Poole
 *
 */
public final class SelectFile {

    private static final String DEBUG_TAG = SelectFile.class.getName();

    public static final int SAVE_FILE = 7113;
    public static final int READ_FILE = 9340;

    private static SaveFile     saveCallback;
    private static final Object saveCallbackLock = new Object();

    private static ReadFile     readCallback;
    private static final Object readCallbackLock = new Object();

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
        }
        String path = App.getPreferences(activity).getString(directoryPrefKey);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            startFileSelector(activity, Intent.ACTION_CREATE_DOCUMENT, SAVE_FILE, path, false);
        } else {
            startFilePickerActivity(activity, SAVE_FILE, path);
        }
    }

    /**
     * @param activity activity activity that called us
     * @param directoryPrefKey string resources for shared preferences for preferred (last) directory
     * @param readFile callback callback that does the actual saving, should call
     *            {@link #savePref(Preferences, int, Uri)}
     */
    public static void read(@NonNull FragmentActivity activity, int directoryPrefKey, @NonNull ReadFile readFile) {
        read(activity, directoryPrefKey, readFile, false);
    }

    /**
     * @param activity activity activity that called us
     * @param directoryPrefKey string resources for shared preferences for preferred (last) directory
     * @param readFile callback callback that does the actual saving, should call
     *            {@link #savePref(Preferences, int, Uri)}
     * @param allowMultiple if true support selecting multiple files
     */
    public static void read(@NonNull FragmentActivity activity, int directoryPrefKey, @NonNull ReadFile readFile, boolean allowMultiple) {
        synchronized (readCallbackLock) {
            readCallback = readFile;
        }
        String path = App.getPreferences(activity).getString(directoryPrefKey);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            startFileSelector(activity, Intent.ACTION_OPEN_DOCUMENT, READ_FILE, path, allowMultiple);
        } else {
            startFilePickerActivity(activity, READ_FILE, path);
        }
    }

    /**
     * Start a file picker for pre-KITKAT Androids
     * 
     * @param activity the calling Activity
     * @param requestCode the request code
     * @param path a directory path to try to start with
     */
    private static void startFilePickerActivity(@NonNull Activity activity, int requestCode, @Nullable String path) {
        Intent i = new Intent(activity, ThemedFilePickerActivity.class);
        i.putExtra(AbstractFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        if (requestCode == READ_FILE) {
            i.putExtra(AbstractFilePickerActivity.EXTRA_SINGLE_CLICK, true);
        }
        if (requestCode == SAVE_FILE) {
            i.putExtra(AbstractFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(AbstractFilePickerActivity.EXTRA_ALLOW_EXISTING_FILE, true);
        }
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
        activity.startActivityForResult(i, requestCode);
    }

    /**
     * Start the system file selector or other installed app with the same functionality
     * 
     * @param activity the calling Activity
     * @param intentAction the intent action we want to use
     * @param intentRequestCode the request code
     * @param path a directory path to try to start with
     */
    private static void startFileSelector(@NonNull FragmentActivity activity, @NonNull String intentAction, int intentRequestCode, @Nullable String path,
            boolean allowMultiple) {
        Intent i = new Intent(intentAction);
        i.setType("*/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && path != null) {
            i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(path));
        }
        if (intentRequestCode == READ_FILE && allowMultiple) {
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        final PackageManager pm = activity.getPackageManager();

        @SuppressWarnings("deprecation")
        List<ResolveInfo> activities = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? pm.queryIntentActivities(i, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL))
                : pm.queryIntentActivities(i, PackageManager.MATCH_ALL);
        if (activities.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ErrorAlert.showDialog(activity, ErrorCodes.REQUIRED_FEATURE_MISSING, "file selector");
            return;
        }
        if (activities.size() > 1) { // multiple activities support the required action
            selectFileSelectorActivity(activity, pm, activities, i, intentRequestCode);
            return;
        }
        activity.startActivityForResult(i, intentRequestCode);
    }

    /**
     * Select which Activity to use for file selection
     * 
     * @param activity current Activity
     * @param pm a PackageManager
     * @param resolvedActivities a list of ResolvInfo
     * @param intent the Intent
     * @param code the request code to use
     */
    private static void selectFileSelectorActivity(@NonNull final Activity activity, @NonNull PackageManager pm,
            @NonNull final List<ResolveInfo> resolvedActivities, @NonNull final Intent intent, int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.select_file_picker_title);
        builder.setAdapter(new ResolveInfoAdapter(activity, pm, resolvedActivities), (DialogInterface dialog, int which) -> {
            intent.setPackage(resolvedActivities.get(which).activityInfo.packageName);
            activity.startActivityForResult(intent, code);
        });
        builder.setNeutralButton(R.string.cancel, null);
        builder.show();
    }

    private static class ResolveInfoAdapter extends ArrayAdapter<ResolveInfo> {
        private final PackageManager pm;
        private final int            side;

        /**
         * Get an adapter
         * 
         * @param context an Android Context
         * @param pm a PackageManager
         * @param items a List of ReolveInfo
         */
        public ResolveInfoAdapter(@NonNull Context context, @NonNull PackageManager pm, @NonNull List<ResolveInfo> items) {
            super(context, R.layout.resolveinfo_list_item, items);
            this.pm = pm;
            side = Density.dpToPx(context, PresetElement.ICON_SIZE_DP);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LinearLayout ll = (LinearLayout) (!(convertView instanceof LinearLayout) ? View.inflate(getContext(), R.layout.resolveinfo_list_item, null)
                    : convertView);
            TextView name = ll.findViewById(R.id.app_name);
            final ResolveInfo item = getItem(position);
            name.setText(item.loadLabel(pm));
            Drawable icon = item.loadIcon(pm);
            icon.setBounds(0, 0, side, side);
            ImageView appIcon = ll.findViewById(R.id.app_icon);
            appIcon.setImageDrawable(icon);
            return ll;
        }
    }

    /**
     * Handle the file selector result
     * 
     * @param activity the current Activity
     * @param code returned request code
     * @param data the returned intent
     */
    public static void handleResult(@NonNull FragmentActivity activity, int code, @NonNull Intent data) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uri = data.getData();
        } else {
            uri = Uri.fromFile(com.nononsenseapps.filepicker.Utils.getFileForUri(data.getData()));
        }
        ContentResolverUtil.persistPermissions(activity, data.getFlags(), uri);
        try {
            if (code == SAVE_FILE) {
                callSaveCallback(activity, uri);
            } else if (code == READ_FILE) {
                callReadCallback(activity, data, uri);
            }
        } catch (NetworkOnMainThreadException nex) {
            Log.e(DEBUG_TAG, "Got exception for " + " uri " + nex.getMessage());
            ScreenMessage.toastTopError(activity, activity.getString(R.string.toast_network_file_not_supported, nex.getMessage()));
        }
    }

    /**
     * Call the callback for saving to a file
     * 
     * @param activity the current Activity
     * @param uri the file Uri
     */
    private static void callSaveCallback(@NonNull FragmentActivity activity, @Nullable Uri uri) {
        if (uri == null) {
            Log.e(DEBUG_TAG, "callSaveCallback called with null uri");
            return;
        }
        File file = new File(uri.getPath());
        if (file.exists()) {
            ScreenMessage.barWarning(activity, activity.getResources().getString(R.string.toast_file_exists, file.getName()), R.string.overwrite, v -> {
                synchronized (saveCallbackLock) {
                    if (saveCallback != null) {
                        saveCallback.save(activity, uri);
                    }
                }
            });
            return;
        }
        synchronized (saveCallbackLock) {
            if (saveCallback != null) {
                Log.d(DEBUG_TAG, "saving to " + uri);
                saveCallback.save(activity, uri);
            }
        }
    }

    /**
     * Call the callback for reading an or multiple files
     * 
     * @param the current Activity
     * @param data the Intent
     * @param uri the file Uri
     */
    private static void callReadCallback(@NonNull FragmentActivity activity, @NonNull Intent data, @Nullable Uri uri) {
        synchronized (readCallbackLock) {
            if (readCallback != null) {
                Log.d(DEBUG_TAG, "reading " + uri);
                if (uri == null) {
                    ClipData clipData = data.getClipData();
                    List<Uri> uris = new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri u = item.getUri();
                        if (u != null) {
                            uris.add(u);
                        }
                    }
                    readCallback.read(activity, uris);
                    return;
                }
                readCallback.read(activity, uri);
            }
        }
    }

    /**
     * Save the directory path to shared preferences
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
