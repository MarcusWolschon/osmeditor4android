package de.blau.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import de.blau.android.dialogs.Progress;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.FileUtil;

/**
 * Taken from https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 * 
 * We take the opportunity to do anything one time only too.
 * 
 * @author Simon Poole
 *
 */
public class Splash extends AppCompatActivity {
    private static final String DEBUG_TAG = "Splash";

    static final String SHORTCUT_EXTRAS_KEY = "shortcut_extras";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // don't use Preferences here as this will create the Vespucci directory which is bad for migration
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(getString(R.string.config_enableLightTheme_key), true)) {
            setTheme(R.style.SplashThemeLight);
        } else {
            setTheme(R.style.SplashTheme);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        new AsyncTask<Void, Void, Void>() {

            TileLayerDatabase db = new TileLayerDatabase(Splash.this);
            boolean           newInstall;
            boolean           newConfig;
            boolean           migratePublicDirectory;

            @Override
            protected void onPreExecute() {
                Log.d(DEBUG_TAG, "onPreExecute");
                Log.d(DEBUG_TAG, "checking last tile source update");
                long lastDatabaseUpdate = 0;
                try {
                    lastDatabaseUpdate = Math.max(TileLayerDatabase.getSourceUpdate(db.getReadableDatabase(), TileLayerDatabase.SOURCE_JOSM_IMAGERY),
                            TileLayerDatabase.getSourceUpdate(db.getReadableDatabase(), TileLayerDatabase.SOURCE_ELI));
                } catch (SQLiteException sex) {
                    if (sex instanceof SQLiteDatabaseLockedException) {
                        Log.e(DEBUG_TAG, "tile layer database is locked");
                        cancel(true);
                    }
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
            }

            @Override
            protected Void doInBackground(Void... params) {
                Log.d(DEBUG_TAG, "doInBackGround");
                if (migratePublicDirectory) {
                    Log.w(DEBUG_TAG, "Migrating public directory ...");
                    try {
                        FileUtil.copyDirectory(FileUtil.getLegacyPublicDirectory(), FileUtil.getPublicDirectory(Splash.this));
                        Log.w(DEBUG_TAG, "... done.");
                    } catch (IOException e) {
                        Log.e(DEBUG_TAG, "Error migrating public directory " + e.getMessage());
                    }
                    Splash.this.runOnUiThread(() -> {
                        Progress.dismissDialog(Splash.this, Progress.PROGRESS_MIGRATION);
                        Progress.showDialog(Splash.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                    });
                }
                if (newInstall || newConfig) {
                    KeyDatabaseHelper.readKeysFromAssets(Splash.this);
                }
                try {
                    if (!isCancelled()) {
                        TileLayerSource.createOrUpdateCustomSource(Splash.this, db.getWritableDatabase(), true);
                        if (newInstall || newConfig) {
                            TileLayerSource.createOrUpdateFromAssetsSource(Splash.this, db.getWritableDatabase(), newConfig, true);
                        }
                    }
                } finally {
                    db.close();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.d(DEBUG_TAG, "onPostExecute");
                if (newInstall || newConfig) {
                    Progress.dismissDialog(Splash.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                }
                Intent intent = new Intent(Splash.this, Main.class);
                intent.putExtra(SHORTCUT_EXTRAS_KEY, getIntent().getExtras());
                startActivity(intent);
                Splash.this.finish();
            }

        }.execute();
    }
}
