package de.blau.android;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import de.blau.android.contract.Files;
import de.blau.android.dialogs.Progress;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.SplashThemeLight);
        } else {
            setTheme(R.style.SplashTheme);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        new AsyncTask<Void, Void, Void>() {

            TileLayerDatabase db = new TileLayerDatabase(Splash.this);
            boolean           newInstall;
            boolean           newConfig;

            @Override
            protected void onPreExecute() {
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
                long lastUpdateTime = 0L;
                try {
                    PackageInfo packageInfo = Splash.this.getPackageManager().getPackageInfo(Splash.this.getPackageName(), 0);
                    lastUpdateTime = packageInfo.lastUpdateTime;
                } catch (NameNotFoundException e1) {
                    // can't really happen
                }
                newInstall = lastDatabaseUpdate == 0;
                newConfig = lastUpdateTime > lastDatabaseUpdate;
                if (newInstall || newConfig) {
                    Progress.showDialog(Splash.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (newInstall || newConfig) {
                    AssetManager assetManager = getAssets();
                    try (KeyDatabaseHelper keys = new KeyDatabaseHelper(Splash.this)) {
                        keys.keysFromStream(assetManager.open(Files.FILE_NAME_KEYS));
                    } catch (IOException e) {
                        Log.e(DEBUG_TAG, "Error reading keys file " + e.getMessage());
                    }
                }
                try {
                    if (!isCancelled()) {
                        TileLayerServer.createOrUpdateCustomSource(Splash.this, db.getWritableDatabase(), true);
                        if (newInstall || newConfig) {
                            TileLayerServer.createOrUpdateFromAssetsSource(Splash.this, db.getWritableDatabase(), newConfig, true);
                        }
                    }
                } finally {
                    db.close();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (newInstall || newConfig) {
                    Progress.dismissDialog(Splash.this, Progress.PROGRESS_BUILDING_IMAGERY_DATABASE);
                }
                Intent intent = new Intent(Splash.this, Main.class);
                startActivity(intent);
                Splash.this.finish();
            }

        }.execute();
    }
}
