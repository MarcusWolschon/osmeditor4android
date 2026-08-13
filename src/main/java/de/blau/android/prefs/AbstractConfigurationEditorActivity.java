package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**  */
abstract class AbstractConfigurationEditorActivity extends URLListEditActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AbstractConfigurationEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = AbstractConfigurationEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_HELP = 1;

    protected AdvancedPrefDatabase db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customLight);
        }
        db = new AdvancedPrefDatabase(this);
        super.onCreate(savedInstanceState);
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
        if (item.getItemId() == MENUITEM_HELP) {
            HelpViewer.start(this, getHelpResourceId());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Get the resource for the help page
     * 
     * @return the resource id for the help page
     */
    abstract int getHelpResourceId();
}