package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends AppCompatActivity {

    final static String CURRENT_VIEWBOX = "VIEWBOX";
    private BoundingBox viewBox         = null;

    public static void start(@NonNull Context context, BoundingBox viewBox) {
        Intent intent = new Intent(context, PrefEditor.class);
        intent.putExtra(CURRENT_VIEWBOX, viewBox);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        Log.d("PrefEditor", "onCreate");
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_AppCompatPrefsLight);
        }

        super.onCreate(savedInstanceState);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Log.d("PrefEditor", "initializing from intent");
            // No previous state to restore - get the state from the intent
            viewBox = (BoundingBox) getIntent().getSerializableExtra(CURRENT_VIEWBOX);
        } else {
            Log.d("PrefEditor", "initializing from saved state");
            // Restore activity from saved state
            viewBox = (BoundingBox) savedInstanceState.getSerializable(CURRENT_VIEWBOX);
        }

        PrefEditorFragment f = new PrefEditorFragment();

        Bundle args = new Bundle();
        args.putSerializable(CURRENT_VIEWBOX, viewBox);
        f.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d("PrefEditor", "onOptionsItemSelected");
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        Log.d("PrefEditor", "onSaveInstaceState");
        super.onSaveInstanceState(outState);
        outState.putSerializable(CURRENT_VIEWBOX, viewBox);
    }
}
