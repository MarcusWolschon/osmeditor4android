package de.blau.android;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import de.blau.android.dialogs.Progress;
import de.blau.android.prefs.Preferences;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.SplashThemeLight);
        } else {
            setTheme(R.style.SplashTheme);
        }
    }

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
                long lastDatabaseUpdate = TileLayerDatabase.getSourceUpdate(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
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
                TileLayerServer.createOrUpdateCustomSource(Splash.this, db.getWritableDatabase(), true);
                try {
                    if (newInstall || newConfig) {
                        TileLayerServer.createOrUpdateFromAssetsSource(Splash.this, db.getWritableDatabase(), newConfig, true);
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
