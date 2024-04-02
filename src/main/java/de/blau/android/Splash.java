package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.preference.PreferenceManager;
import de.blau.android.contract.Paths;
import de.blau.android.dialogs.Progress;
import de.blau.android.layer.LayerConfig;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Originally based https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 * 
 * We take the opportunity to do anything one time only too.
 * 
 * @author Simon Poole
 *
 */
public class Splash extends AppCompatActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Splash.class.getSimpleName().length());
    private static final String DEBUG_TAG = Splash.class.getSimpleName().substring(0, TAG_LEN);

    static final String SHORTCUT_EXTRAS_KEY = "shortcut_extras";
    static final String SAFE                = "safe";

    private Bundle  shortcutExtras;
    private Object  startedLock = new Object();
    private boolean started     = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // don't use Preferences here as this will create the Vespucci directory which is bad for migration
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean lightTheme = prefs.getBoolean(getString(R.string.config_enableLightTheme_key), true);
        setTheme(lightTheme ? R.style.SplashThemeLight : R.style.SplashTheme);
        SplashScreen.Companion.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    final ExecutorTask<Void, Void, Void> startup = new ExecutorTask<Void, Void, Void>() {

        boolean newInstall;
        boolean newConfig;
        boolean migratePublicDirectory;

        @Override
        protected Void doInBackground(Void param) {
            try (TileLayerDatabase db = new TileLayerDatabase(Splash.this)) {
                Log.d(DEBUG_TAG, "checking last tile source update");
                long lastDatabaseUpdate = 0;
                try {
                    lastDatabaseUpdate = Math.max(TileLayerDatabase.getSourceUpdate(db.getReadableDatabase(), TileLayerDatabase.SOURCE_JOSM_IMAGERY),
                            TileLayerDatabase.getSourceUpdate(db.getReadableDatabase(), TileLayerDatabase.SOURCE_ELI));
                } catch (SQLiteException sex) {
                    Log.e(DEBUG_TAG, "Exception accessing tile layer database " + sex.getMessage());
                    cancel();
                }
                Log.d(DEBUG_TAG, "checking last package update");
                long lastUpdateTime = 0L;
                try {
                    lastUpdateTime = Util.getPackageInfo(Splash.this.getPackageName(), getPackageManager()).lastUpdateTime;
                } catch (NameNotFoundException e1) {
                    // can't really happen
                }
                newInstall = lastDatabaseUpdate == 0;
                newConfig = lastUpdateTime > lastDatabaseUpdate;
                if (newInstall || newConfig) {
                    migratePublicDirectory = !FileUtil.publicDirectoryExists();
                    Progress.showDialog(Splash.this, migratePublicDirectory ? Progress.PROGRESS_MIGRATION : Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                }
                if (migratePublicDirectory) {
                    directoryMigration(Splash.this);
                    Splash.this.runOnUiThread(() -> {
                        Progress.dismissDialog(Splash.this, Progress.PROGRESS_MIGRATION);
                        Progress.showDialog(Splash.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                    });
                }
                if (newInstall || newConfig) {
                    KeyDatabaseHelper.readKeysFromAssets(Splash.this);
                }
                if (!isCancelled()) {
                    TileLayerSource.createOrUpdateCustomSource(Splash.this, db.getWritableDatabase(), true);
                    if (newInstall || newConfig) {
                        TileLayerSource.createOrUpdateFromAssetsSource(Splash.this, db.getWritableDatabase(), newConfig, true);
                    }
                    TileLayerSource.getListsLocked(Splash.this, db.getReadableDatabase(), true);
                }
            }
            if (newInstall || newConfig) {
                Progress.dismissDialog(Splash.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
            }
            // read Presets here to avoid reading them on UI thread on startup of Main
            Progress.showDialog(Splash.this, Progress.PROGRESS_LOADING_PRESET);
            Log.d(DEBUG_TAG, "Initial preset load");
            App.getCurrentPresets(Splash.this);
            Log.d(DEBUG_TAG, "Preset load finished");
            //
            Intent intent = new Intent(Splash.this, Main.class);
            intent.putExtra(SHORTCUT_EXTRAS_KEY, shortcutExtras);
            startActivity(intent);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(DEBUG_TAG, "onPostExecute");
            Progress.dismissDialog(Splash.this, Progress.PROGRESS_LOADING_PRESET);
            Splash.this.finish();
        }
    };

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        synchronized (startedLock) {
            if (!started) {
                started = true;
                shortcutExtras = getIntent().getExtras();
                if (shortcutExtras != null && shortcutExtras.getBoolean(SAFE)) {
                    showSafeModeDialog(startup);
                    return;
                }
                startup.execute();
            }
        }
    }

    /**
     * Show a dialog with options for safe mode
     * 
     * Note: dismissing the dialog before the task is run leads to the background vanishing
     * 
     * @param startupTask the ExecutorTask that is run to actually start the app
     */
    private void showSafeModeDialog(@NonNull ExecutorTask<Void, Void, Void> startupTask) {

        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(this);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.safe_mode, null);

        Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.safe_mode_dialog_title);
        builder.setView(layout);

        CheckBox style = (CheckBox) layout.findViewById(R.id.safe_style_check);
        CheckBox layers = (CheckBox) layout.findViewById(R.id.safe_layer_check);
        CheckBox state = (CheckBox) layout.findViewById(R.id.safe_state_check);

        builder.setPositiveButton(R.string.Continue, null);
        builder.setNegativeButton(R.string.cancel, (DialogInterface dialog, int which) -> finish());

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface d) -> {
            final Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener((View v) -> {
                Log.e(DEBUG_TAG, "Starting in safe mode");
                if (style.isChecked()) {
                    // use minimal data style
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putString(getString(R.string.config_mapProfile_key), DataStyle.getBuiltinStyleName()).commit();
                }
                if (layers.isChecked()) {
                    // hide all layers
                    try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(this)) {
                        final LayerConfig[] layerConfigs = db.getLayers();
                        for (LayerConfig config : layerConfigs) {
                            db.setLayerVisibility(config.getPosition(), false);
                        }
                    }
                }
                if (state.isChecked()) {
                    Builder reallyBuilder = new AlertDialog.Builder(this);
                    reallyBuilder.setTitle(R.string.safe_delete_state_title);
                    reallyBuilder.setPositiveButton(R.string.safe_delete_state_text, (DialogInterface dialog2, int which2) -> {
                        Log.e(DEBUG_TAG, "Removing state files");
                        this.deleteFile(StorageDelegator.FILENAME);
                        this.deleteFile(StorageDelegator.BACKUP_FILENAME);
                        dialog.dismiss();
                        startupTask.execute();
                    });
                    reallyBuilder.setNegativeButton(R.string.no, null);
                    reallyBuilder.show();
                    return;
                }
                dialog.dismiss();
                startupTask.execute();
            });
        });
        dialog.show();
    }

    /**
     * Migrate legacy public directories
     * 
     * @param ctx an Android Context
     */
    static void directoryMigration(@NonNull Context ctx) {
        Log.w(DEBUG_TAG, "Migrating public directory ...");
        try {
            FileUtil.copyDirectory(FileUtil.getLegacyPublicDirectory(), FileUtil.getPublicDirectory());
            Log.w(DEBUG_TAG, "... done.");
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Error migrating public directory " + e.getMessage());
        }
        try {
            Log.w(DEBUG_TAG, "Migrating style directory ...");
            File destStyleDir = new File(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_STYLES);
            for (File fileDir : ctx.getExternalFilesDirs(null)) {
                File inStyleDir = new File(fileDir, Paths.DIRECTORY_PATH_STYLES);
                if (inStyleDir.exists()) {
                    FileUtil.copyDirectory(inStyleDir, destStyleDir);
                }
            }
            Log.w(DEBUG_TAG, "... done.");
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "Unable to to migrate style directory " + ex.getMessage());
        }
    }
}
