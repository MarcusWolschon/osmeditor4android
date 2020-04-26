package de.blau.android.easyedit;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.prefs.Preferences;

/**
 * Extend this class instead of EasyEditActionModeCallback if you need to disable the SimpleActionButton
 * 
 * @author simon
 *
 */
public class NonSimpleActionModeCallback extends EasyEditActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {
    private static final String DEBUG_TAG = "NonSimpleActionMode...";

    final Preferences prefs;

    /**
     * Construct a callback for that disables/enables the SimpleActionsButton
     * 
     * @param manager the EasyEditManager instance
     */
    public NonSimpleActionModeCallback(@NonNull EasyEditManager manager) {
        super(manager);
        prefs = new Preferences(main);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        if (prefs.areSimpleActionsEnabled()) {
            main.disableSimpleActionsButton();
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.clear();
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help))
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        if (prefs.areSimpleActionsEnabled()) {
            main.enableSimpleActionsButton();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem arg0) {
        return false;
    }
}
