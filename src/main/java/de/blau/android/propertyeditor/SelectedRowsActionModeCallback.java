package de.blau.android.propertyeditor;

import java.util.ArrayList;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.view.MenuCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditActionModeCallback;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

class SelectedRowsActionModeCallback implements Callback {

    public interface Row {

        /**
         * Delete the row
         */
        void delete();

        /**
         * De-select the row
         */
        void deselect();

        /**
         * Check if the row is selected
         * 
         * @return true if selected
         */
        boolean isSelected();
    }

    static final int MENU_ITEM_DELETE = 1;

    static final int MENU_ITEM_SELECT_ALL   = 13;
    static final int MENU_ITEM_DESELECT_ALL = 14;
    static final int MENU_ITEM_HELP         = 20;

    ActionMode currentAction;

    final LinearLayout rows;
    final Fragment     caller;

    /**
     * Careate a new callback for selected rows
     * 
     * @param caller the calling Fragment
     * @param rows the Layout holding the selectable rows
     */
    public SelectedRowsActionModeCallback(@NonNull Fragment caller, @NonNull LinearLayout rows) {
        this.rows = rows;
        this.caller = caller;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        currentAction = mode;
        ((PropertyEditor) caller.getActivity()).disablePaging();
        ((PropertyEditor) caller.getActivity()).disablePresets();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.clear();
        Context context = caller.getActivity();
        menu.add(Menu.NONE, MENU_ITEM_DELETE, Menu.NONE, R.string.delete).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_delete));
        MenuCompat.setShowAsAction(menu.add(EasyEditActionModeCallback.GROUP_BASE, MENU_ITEM_SELECT_ALL, Menu.CATEGORY_SYSTEM, R.string.menu_select_all),
                MenuItem.SHOW_AS_ACTION_NEVER);
        MenuCompat.setShowAsAction(menu.add(EasyEditActionModeCallback.GROUP_BASE, MENU_ITEM_DESELECT_ALL, Menu.CATEGORY_SYSTEM, R.string.menu_deselect_all),
                MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(EasyEditActionModeCallback.GROUP_BASE, MENU_ITEM_HELP, Menu.CATEGORY_SYSTEM, R.string.menu_help)
                .setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_help)).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_help));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_DELETE:
            final int size = rows.getChildCount();
            ArrayList<Row> toDelete = new ArrayList<>();
            for (int i = 0; i < size; ++i) {
                View view = rows.getChildAt(i);
                Row row = (Row) view;
                if (row.isSelected()) {
                    toDelete.add(row);
                }
            }
            if (!toDelete.isEmpty()) {
                for (Row r : toDelete) {
                    r.deselect();
                    r.delete();
                }
            }
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_SELECT_ALL:
            ((PropertyRows) caller).selectAllRows();
            return true;
        case MENU_ITEM_DESELECT_ALL:
            ((PropertyRows) caller).deselectAllRows();
            return true;
        case MENU_ITEM_HELP:
            HelpViewer.start(caller.getActivity(), R.string.help_propertyeditor);
            return true;
        default:
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        final int size = rows.getChildCount();
        for (int i = 0; i < size; ++i) {
            View view = rows.getChildAt(i);
            Row row = (Row) view;
            row.deselect();
        }
        ((PropertyEditor) caller.getActivity()).enablePaging();
        ((PropertyEditor) caller.getActivity()).enablePresets();
        PropertyRows rowContainer = (PropertyRows) caller;
        rowContainer.deselectHeaderCheckBox();
        rowContainer.deselectRow();
        currentAction = null;
        ((AppCompatActivity) caller.getActivity()).invalidateOptionsMenu();
    }

    /**
     * Check if all rows have been de-selected
     * 
     * @param skipHeaderRow if true skip the header row
     * @return true if no rows are selected
     */
    public boolean rowsDeselected(boolean skipHeaderRow) {
        final int size = rows.getChildCount();
        int initialRowIndex = skipHeaderRow ? 1 : 0;
        for (int i = initialRowIndex; i < size; ++i) {
            View view = rows.getChildAt(i);
            Row row = (Row) view;
            if (row.isSelected()) {
                // something is still selected
                return false;
            }
        }
        // nothing selected -> finish
        if (currentAction != null) {
            currentAction.finish();
        }
        return true;
    }

    /**
     * Invalidate the current ActionMode aka redisplay the menus
     */
    public void invalidate() {
        if (currentAction != null) {
            currentAction.invalidate();
        }
    }
}
