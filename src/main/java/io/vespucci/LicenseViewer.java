package io.vespucci;

import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import io.vespucci.R;
import io.vespucci.util.ConfigurationChangeAwareActivity;

import java.nio.charset.StandardCharsets;

/**
 * Show licence and author information for the app
 *
 */
public class LicenseViewer extends ConfigurationChangeAwareActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (App.getPreferences(this).lightThemeEnabled()) {
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
        load("LICENSE-OFL.txt", builder);
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
    @SuppressLint("NewApi") // StandardCharsets is desugared for APIs < 19.
    private void load(@NonNull String filename, @NonNull StringBuilder builder) {
        builder.append("== ").append(filename).append(" ==\n");
        try (InputStreamReader reader = new InputStreamReader(getAssets().open(filename), StandardCharsets.UTF_8)) {
            int read = 0;
            do {
                char[] buf = new char[4096];
                read = reader.read(buf);
                if (read > 0) {
                    builder.append(buf, 0, read);
                }
            } while (read > 0);
        } catch (Exception e) {
            builder.append("Error while loading file: ").append(e.getMessage());
        }
        builder.append("\n\n\n\n");
    }
}
