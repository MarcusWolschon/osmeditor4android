package de.blau.android;

import java.io.InputStreamReader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.BugFixedAppCompatActivity;

/**
 * Show licence and author information for the app
 *
 */
public class LicenseViewer extends BugFixedAppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customLight);
        }

        super.onCreate(savedInstanceState);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setTitle(R.string.config_licensebutton_title);

        View container = View.inflate(this, R.layout.license_viewer, null);
        TextView textShort = (TextView) container.findViewById(R.id.licenseShortText);
        TextView textFull = (TextView) container.findViewById(R.id.licenseFullText);

        textShort.setText(load("LICENSE.txt"));

        StringBuilder builder = new StringBuilder();
        load("LICENSE-GPL3.txt", builder);
        load("LICENSE-Apache.txt", builder);
        load("josm-contributors.txt", builder);
        load("LICENSE-LGPL3.txt", builder);
        textFull.setText(builder.toString());
        setContentView(container);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            Log.e("LicenseViewer", "Unknown menu item " + item.getTitle());
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Load a text file in to a String
     * 
     * @param filename the name of the file
     * @return a String with the contents
     */
    private String load(@NonNull String filename) {
        StringBuilder builder = new StringBuilder();
        load(filename, builder);
        return builder.toString();
    }

    /**
     * Load a text file in to a StringBuilder
     * 
     * @param filename the name of the file
     * @param builder the StringBuilder
     */
    private void load(@NonNull String filename, @NonNull StringBuilder builder) {
        builder.append("== " + filename + " ==\n");
        try (InputStreamReader reader = new InputStreamReader(getAssets().open(filename), OsmXml.UTF_8)) {           
            int read = 0;
            do {
                char[] buf = new char[4096];
                read = reader.read(buf);
                if (read > 0) {
                    builder.append(buf, 0, read);
                }
            } while (read > 0);
        } catch (Exception e) {
            builder.append("Error while loading file: " + e.getMessage());
        }
        builder.append("\n\n\n\n");
    }
}
