package de.blau.android.propertyeditor;

import java.util.List;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import de.blau.android.R;
import de.blau.android.dialogs.ElementInfo;
import de.blau.android.osm.Relation;
import de.blau.android.propertyeditor.RelationMembershipFragment.RelationMembershipRow;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

class ParentSelectedActionModeCallback extends SelectedRowsActionModeCallback {

    /**
     * Create a new callback for selected rows
     * 
     * @param caller the calling Fragment
     * @param rows the Layout holding the selectable rows
     */
    public ParentSelectedActionModeCallback(@NonNull Fragment caller, @NonNull LinearLayout rows) {
        super(caller, rows);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        mode.setTitle(R.string.tag_action_parents_title);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        super.onPrepareActionMode(mode, menu);
        Context context = caller.getActivity();
        if (getSelectedRows().size() == 1) {
            menu.add(Menu.NONE, MENU_ITEM_INFO, Menu.NONE, R.string.menu_copy).setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_info))
                    .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_information));
        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        if (MENU_ITEM_INFO == item.getItemId()) {
            List<RelationMembershipRow> selectedRows = getSelectedRows();
            if (!selectedRows.isEmpty()) {
                Relation r = selectedRows.get(0).getRelation();
                if (r != null) {
                    ElementInfo.showDialog(caller.getActivity(), r, false, false);
                }
            }
        }
        return true;
    }
}
