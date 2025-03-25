package io.vespucci.easyedit;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.ThemeUtils;

/**
 * Extend this class instead of EasyEditActionModeCallback if the action mode is building an object and you need a way
 * of indicating that you are finished and not via using the home button.
 * 
 * @author simon
 *
 */
public abstract class BuilderActionModeCallback extends EasyEditActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {

    protected final Preferences prefs;
    protected Drawable          savedButton;

    /**
     * Construct a callback that shows a FAB with a check instead of the SimpleActionsButton
     * 
     * @param manager the EasyEditManager instance
     */
    protected BuilderActionModeCallback(@NonNull EasyEditManager manager) {
        super(manager);
        prefs = App.getPreferences(main);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        // setup menu
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        FloatingActionButton button = main.getSimpleActionsButton();
        button.setOnClickListener(v -> finishBuilding());
        savedButton = button.getDrawable();
        button.setImageResource(R.drawable.ic_done_white_36dp);
        if (!prefs.areSimpleActionsEnabled()) {
            main.showSimpleActionsButton();
        }
        main.descheduleAutoLock();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        super.onPrepareActionMode(mode, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        FloatingActionButton button = main.getSimpleActionsButton();
        button.setImageDrawable(savedButton);
        main.setSimpleActionsButtonListener();
        if (!prefs.areSimpleActionsEnabled()) {
            main.hideSimpleActionsButton();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem arg0) {
        return false;
    }

    /**
     * This will be called when the "done" FAB is clicked
     */
    protected abstract void finishBuilding();

    @Override
    public boolean onBackPressed() {
        if (hasData()) {
            new AlertDialog.Builder(main).setTitle(R.string.abort_action_title).setPositiveButton(R.string.yes, (dialog, which) -> super.onBackPressed())
                    .setNeutralButton(R.string.cancel, null).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onCloseClicked() {
        if (onBackPressed()) {
            super.onCloseClicked();
        }
    }

    /**
     * Check if the callback has data that could be lost
     * 
     * @return true if data could be lost
     */
    protected abstract boolean hasData();
}
