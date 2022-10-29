package de.blau.android;

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.preference.PreferenceManager;
import de.blau.android.contract.Paths;
import de.blau.android.dialogs.Progress;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;

/**
 * Originally based https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 * 
 * We take the opportunity to do anything one time only too.
 * 
 * @author Simon Poole
 *
 */
public class Splash extends AppCompatActivity {
    private static final String DEBUG_TAG = Splash.class.getSimpleName();

    static final String SHORTCUT_EXTRAS_KEY = "shortcut_extras";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // don't use Preferences here as this will create the Vespucci directory which is bad for migration
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean lightTheme = prefs.getBoolean(getString(R.string.config_enableLightTheme_key), true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTheme(lightTheme ? R.style.SplashThemeLight : R.style.SplashTheme);
            SplashScreen.Companion.installSplashScreen(this);
        } else {
            setTheme(lightTheme ? R.style.SplashThemeLightPre21 : R.style.SplashThemePre21);
        }
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        new ExecutorTask<Void, Void, Void>() {

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
                        String packageName = Splash.this.getPackageName();
                        PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
                        lastUpdateTime = packageInfo.lastUpdateTime;
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
                App.getCurrentPresets(Splash.this);
                //
                Intent intent = new Intent(Splash.this, Main.class);
                intent.putExtra(SHORTCUT_EXTRAS_KEY, getIntent().getExtras());
                startActivity(intent);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.d(DEBUG_TAG, "onPostExecute");
                Progress.dismissDialog(Splash.this, Progress.PROGRESS_LOADING_PRESET);
                Splash.this.finish();
            }
        }.execute();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
}
