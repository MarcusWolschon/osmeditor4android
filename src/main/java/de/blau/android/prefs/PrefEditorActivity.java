package de.blau.android.prefs;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;

/**
 * The handling of PreferenceScreen is partially based on
 * https://stackoverflow.com/questions/32494548/how-to-move-back-from-preferences-subscreen-to-main-screen-in-preferencefragment/32713331#32713331
 * 
 * @author simon
 *
 */
public abstract class PrefEditorActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private static final String DEBUG_TAG = "PrefEditorActivity";

    protected static final int MENUITEM_HELP = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("PrefEditorActivity", "onCreate");
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_AppCompatPrefsLight);
        }
        super.onCreate(savedInstanceState);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

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
            if (!getSupportFragmentManager().popBackStackImmediate()) {
                finish();
            }
            return true;
        case MENUITEM_HELP:
            HelpViewer.start(this, getHelpTopic());
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

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        Log.d(DEBUG_TAG, "callback called to attach the preference sub screen " + preferenceScreen.getKey());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // SubScreenFragment fragment = SubScreenFragment.newInstance();
        ExtendedPreferenceFragment fragment = newEditorFragment();
        Bundle args = new Bundle();
        // Defining the sub screen as new root for the subscreen
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.replace(android.R.id.content, fragment, preferenceScreen.getKey());
        ft.addToBackStack(null);
        ft.commit();
        return true;
    }

    /**
     * Get and instance of the Fragment we actually want to show
     * 
     * @return an ExtendedPreferenceFragment instance
     */
    abstract ExtendedPreferenceFragment newEditorFragment();

    /**
     * Get the help topic resource
     * 
     * @return a resource id for the help topic
     */
    abstract int getHelpTopic();

}
