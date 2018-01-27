package de.blau.android.prefs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import de.blau.android.R;
import de.blau.android.util.SelectFile;

public class AdvancedPrefEditor extends AppCompatActivity {

    private static final String DEBUG_TAG = "AdvancedPrefEditor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("AdvancedPrefEditor", "onCreate");
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_AppCompatPrefsLight);
        }

        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new AdvancedPrefEditorFragment()).commit();

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d("AdvancedPrefEditor", "onOptionsItemSelected");
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(DEBUG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == SelectFile.READ_FILE || requestCode == SelectFile.READ_FILE_OLD || requestCode == SelectFile.SAVE_FILE)
                && resultCode == RESULT_OK) {
            SelectFile.handleResult(requestCode, data);
        }
    }
}
