package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.prefs.PresetLoader;
import de.blau.android.presets.Preset;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Display a dialog asking if missing downloadable presets (and eventually styles) should be downloaded
 *
 */
public class DownloadMissing extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DownloadMissing.class.getSimpleName().length());
    private static final String DEBUG_TAG = DownloadMissing.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG            = "fragment_download_missing";
    private static final String PRESET_IDS_KEY = "presets";
    private static final String STYLE_IDS_KEY  = "styles";

    private ArrayList<String> presetIds;
    private ArrayList<String> styleIds;

    /**
     * Display a dialog asking if missing downloadable presets (and eventually styles) should be downloaded
     * 
     * @param activity the calling Activity
     * @param presetIds a list of preset ids
     * @param styleIds a list of style ids
     */
    public static void showDialog(@NonNull FragmentActivity activity, @Nullable final List<String> presetIds, @Nullable final List<String> styleIds) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            DownloadMissing downloadMissingFragment = newInstance(presetIds, styleIds);
            downloadMissingFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new EmptyRelation dialog instance
     * 
     * @param relationIds ids of the empty Relations
     * @return a new EmptyRelation dialog instance
     */
    @NonNull
    private static DownloadMissing newInstance(@Nullable final List<String> presetIds, @Nullable final List<String> styleIds) {
        DownloadMissing f = new DownloadMissing();

        Bundle args = new Bundle();
        if (presetIds != null) {
            args.putStringArrayList(PRESET_IDS_KEY, new ArrayList<>(presetIds));
        }
        if (styleIds != null) {
            args.putStringArrayList(STYLE_IDS_KEY, new ArrayList<>(styleIds));
        }
        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            getArgsFromBundle(savedInstanceState);
        } else {
            getArgsFromBundle(getArguments());
        }
    }

    /**
     * Get the ids from a bundle
     * 
     * @param bundle a Bundle
     */
    private void getArgsFromBundle(Bundle bundle) {
        presetIds = bundle.getStringArrayList(PRESET_IDS_KEY);
        styleIds = bundle.getStringArrayList(STYLE_IDS_KEY);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = requireActivity();
        Builder builder = ThemeUtils.getAlertDialogBuilder(activity);
        builder.setTitle(R.string.download_missing_title);
        int presetCount = presetIds != null ? presetIds.size() : 0;
        int styleCount = styleIds != null ? styleIds.size() : 0;
        builder.setMessage(
                getString(R.string.download_missing_message, getResources().getQuantityString(R.plurals.download_missing_presets, presetCount, presetCount),
                        getResources().getQuantityString(R.plurals.download_missing_styles, styleCount, styleCount)));

        builder.setPositiveButton(R.string.download, (dialog, which) -> {
            if (Util.isEmpty(presetIds)) {
                return;
            }
            new MissingDownloader(activity).execute();
        });

        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(PRESET_IDS_KEY, presetIds);
        outState.putStringArrayList(STYLE_IDS_KEY, styleIds);
    }

    private class MissingDownloader extends ExecutorTask<Void, Void, Void> {

        private final FragmentActivity activity;

        /**
         * Create a new downloader for missing config files
         * 
         * @param activity the calling FragmentActivity
         */
        public MissingDownloader(@NonNull final FragmentActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            Progress.showDialog(activity, Progress.PROGRESS_PRESET);
        }

        @Override
        protected Void doInBackground(Void param) throws IOException {
            List<String> tempIds = new ArrayList<>(presetIds);
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                for (String id : tempIds) {
                    final File presetDir = db.getPresetDirectory(id);
                    presetDir.mkdir();
                    if (!presetDir.exists()) {
                        throw new IOException("Unable to create preset directory " + presetDir.getAbsolutePath());
                    }
                    final PresetInfo preset = db.getPreset(id);
                    int code = PresetLoader.download(preset.url, presetDir, Preset.PRESETXML);
                    if (code != PresetLoader.DOWNLOADED_PRESET_ERROR) {
                        presetIds.remove(id); // saved state needs to remove downloaded ids
                        // doesn't support icon download for now, but do that here if necessary
                        continue;
                    }
                    presetDir.delete(); // NOSONAR
                    throw new IOException("Unable to download " + preset.name);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            App.resetPresets();
            Progress.dismissDialog(activity, Progress.PROGRESS_PRESET);
        }

        @Override
        protected void onBackgroundError(Exception ex) {
            Log.e(DEBUG_TAG, ex.getMessage());
            if (activity.isFinishing()) {
                return;
            }
            Progress.dismissDialog(activity, Progress.PROGRESS_PRESET);
            ScreenMessage.toastTopError(activity, activity.getString(R.string.download_missing_error, ex.getMessage()));
        }
    }
}
