package de.blau.android.prefs.keyboard;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.prefs.keyboard.Shortcuts.Shortcut;
import de.blau.android.util.ConfigurationChangeAwareActivity;
import de.blau.android.util.ThemeUtils;

/**
 * Activity to manage and edit keyboard shortcuts
 */
public class ShortcutsActivity extends ConfigurationChangeAwareActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ShortcutsActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = ShortcutsActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_HELP = 1;

    private ShortcutsHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (App.getPreferences(this).lightThemeEnabled()) {
            setTheme(R.style.Theme_customActionBar_Light);
        }
        super.onCreate(savedInstanceState);
        LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.keyboard_shortcuts_activity, null);

        ViewGroupCompat.installCompatInsetsDispatch(l);
        ViewCompat.setOnApplyWindowInsetsListener(l, onApplyWindowInsetslistener);
        setContentView(l);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.config_keyboard_shortcuts_title);
        }

        RecyclerView shortcutsRecyclerView = findViewById(R.id.shortcuts_recycler_view);
        shortcutsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = new ShortcutsHelper(this);
        List<Shortcut> shortcuts = Arrays.asList(db.getShortcuts(db.getReadableDatabase()));
        ShortcutsAdapter adapter = new ShortcutsAdapter(this, shortcuts, this::saveShortcut);
        shortcutsRecyclerView.setAdapter(adapter);
    }

    /**
     * Save a modified shortcut to the database
     * 
     * @param item the shortcut item to save
     */
    private void saveShortcut(@NonNull Shortcut item) {
        try {
            Log.d(DEBUG_TAG, "Saving shortcut: " + item.getActionRef() + " -> " + item.getModifier() + " + " + item.getCharacter());
            db.updateShortcut(item);
            App.resetKeyboardShortcuts();
            Toast.makeText(this, R.string.keyboard_shortcut_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error saving shortcut", e);
            Toast.makeText(this, R.string.keyboard_shortcut_save_error, Toast.LENGTH_SHORT).show();
        }
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
            finish();
            return true;
        case MENUITEM_HELP:
            HelpViewer.start(this, R.string.help_keyboard);
            return true;
        default:
            Log.w(DEBUG_TAG, "Unknown menu item " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}
