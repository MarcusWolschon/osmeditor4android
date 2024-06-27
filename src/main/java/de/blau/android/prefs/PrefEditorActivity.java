package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.util.ConfigurationChangeAwareActivity;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;

/**
 * The handling of PreferenceScreen is partially based on
 * https://stackoverflow.com/questions/32494548/how-to-move-back-from-preferences-subscreen-to-main-screen-in-preferencefragment/32713331#32713331
 * 
 * @author simon
 *
 */
public abstract class PrefEditorActivity extends ConfigurationChangeAwareActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PrefEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = PrefEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

    protected static final int MENUITEM_HELP = 1;

    private TextView titleView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate");
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_AppCompatPrefsLight);
        }
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.actionbar_title_layout);
        titleView = findViewById(R.id.actionbar_title);
    }

    /**
     * Set the title in the actionbar
     * 
     * @param title the title to set
     */
    @Override
    public void setTitle(@NonNull CharSequence title) {
        if (titleView == null) {
            Log.e(DEBUG_TAG, "TitleView is null");
            return;
        }
        titleView.setText(title);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.add(0, MENUITEM_HELP, 0, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(this, R.attr.menu_help))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
        default:
            Log.w(DEBUG_TAG, "Unknown menu item " + item.getItemId());
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
        if ((requestCode == SelectFile.READ_FILE || requestCode == SelectFile.SAVE_FILE) && resultCode == RESULT_OK) {
            SelectFile.handleResult(this, requestCode, data);
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        Log.d(DEBUG_TAG, "callback called to attach the preference sub screen " + preferenceScreen.getKey());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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
