package de.blau.android;

import java.io.InputStreamReader;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.BugFixedAppCompatActivity;
import de.blau.android.util.SavingHelper;

public class LicenseViewer extends BugFixedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        textFull.setText(builder.toString());
        setContentView(container);
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String load(String filename) {
        StringBuilder builder = new StringBuilder();
        load(filename, builder);
        return builder.toString();
    }

    private void load(String filename, StringBuilder builder) {
        builder.append("== " + filename + " ==\n");

        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(getAssets().open(filename), "UTF-8");
            int read = 0;
            do {
                char[] buf = new char[4096];
                read = reader.read(buf);
                if (read > 0)
                    builder.append(buf, 0, read);
            } while (read > 0);
        } catch (Exception e) {
            builder.append("Error while loading file: " + e.getMessage());
        } finally {
            SavingHelper.close(reader);
        }
        builder.append("\n\n\n\n");
    }
}
