package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends AppCompatActivity {

    private static final String DEBUG_TAG = PrefEditor.class.getSimpleName();

    final static String CURRENT_VIEWBOX = "VIEWBOX";
    private BoundingBox viewBox         = null;

    private static final int MENUITEM_HELP = 1;

    /**
     * Start the PrefEditor activity
     * 
     * @param context Android Context
     * @param viewBox the current ViewBox
     */
    public static void start(@NonNull Context context, BoundingBox viewBox) {
        Intent intent = new Intent(context, PrefEditor.class);
        intent.putExtra(CURRENT_VIEWBOX, viewBox);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        Log.d(DEBUG_TAG, "onCreate");
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_AppCompatPrefsLight);
        }

        super.onCreate(savedInstanceState);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Log.d(DEBUG_TAG, "initializing from intent");
            // No previous state to restore - get the state from the intent
            viewBox = (BoundingBox) getIntent().getSerializableExtra(CURRENT_VIEWBOX);
        } else {
            Log.d(DEBUG_TAG, "initializing from saved state");
            // Restore activity from saved state
            viewBox = (BoundingBox) savedInstanceState.getSerializable(CURRENT_VIEWBOX);
        }

        PrefEditorFragment f = new PrefEditorFragment();

        Bundle args = new Bundle();
        args.putSerializable(CURRENT_VIEWBOX, viewBox);
        f.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuCompat.setShowAsAction(menu.add(0, MENUITEM_HELP, 0, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(this, R.attr.menu_help)),
                MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(DEBUG_TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case MENUITEM_HELP:
            HelpViewer.start(this, R.string.help_preferences);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstaceState");
        super.onSaveInstanceState(outState);
        outState.putSerializable(CURRENT_VIEWBOX, viewBox);
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
