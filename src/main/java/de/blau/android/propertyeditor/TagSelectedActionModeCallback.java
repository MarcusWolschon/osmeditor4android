package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.propertyeditor.TagEditorFragment.TagEditRow;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class TagSelectedActionModeCallback extends SelectedRowsActionModeCallback {

    // pm: protected static final int MENU_ITEM_DELETE = 1; NOSONAR
    // pm: protected static final int MENU_ITEM_HELP = 15; NOSONAR
    private static final int MENU_ITEM_COPY          = 2;
    private static final int MENU_ITEM_CUT           = 3;
    private static final int MENU_ITEM_COPY_SYSTEM   = 4;
    private static final int MENU_ITEM_CREATE_PRESET = 19;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param caller the calling Fragment
     * @param rows the Layout holding the rows
     */
    public TagSelectedActionModeCallback(@NonNull TagEditorFragment caller, @NonNull LinearLayout rows) {
        super(caller, rows);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        mode.setTitle(R.string.tag_action_tag_title);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        super.onPrepareActionMode(mode, menu);
        Context context = caller.getActivity();
        menu.add(Menu.NONE, MENU_ITEM_COPY, Menu.NONE, R.string.menu_copy).setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_copy))
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_copy));
        menu.add(Menu.NONE, MENU_ITEM_CUT, Menu.NONE, R.string.menu_cut).setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_cut))
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_cut));
        menu.add(Menu.NONE, MENU_ITEM_COPY_SYSTEM, Menu.NONE, R.string.tag_menu_copy_system);
        menu.add(Menu.NONE, MENU_ITEM_CREATE_PRESET, Menu.NONE, R.string.tag_menu_create_preset);
        return true;
    }

    /**
     * Copy tags to the internal tag clipboard
     * 
     * @param selectedRows List of selected rows
     * @param deleteEachRow if true the selected rows will be deleted
     */
    private void copyTags(@NonNull List<TagEditRow> selectedRows, boolean deleteEachRow) {
        Map<String, String> copiedTags = getTagsFromSelectedRows(selectedRows, deleteEachRow);
        App.getTagClipboard(caller.getActivity()).copy(copiedTags);
    }

    /**
     * Copy tags to the system clipboard
     * 
     * @param selectedRows List of selected rows
     */
    private void copyTagsToSystem(@NonNull List<TagEditRow> selectedRows) {
        Map<String, String> copiedTags = getTagsFromSelectedRows(selectedRows, false);
        ClipboardUtils.copyTags(caller.getActivity(), copiedTags);
    }

    /**
     * Get key-value tupels from the selected rows
     * 
     * @param selectedRows a list of the rows
     * @param deleteEachRow if true delete the rows
     * @return a Map of the key values
     */
    @NonNull
    private Map<String, String> getTagsFromSelectedRows(@NonNull List<TagEditRow> selectedRows, boolean deleteEachRow) {
        Map<String, String> copiedTags = new LinkedHashMap<>();
        for (TagEditRow row : selectedRows) {
            addKeyValue(copiedTags, row);
            if (deleteEachRow) {
                row.delete();
            }
        }
        return copiedTags;
    }

    /**
     * Build a map of the keys and values to add to the clipboards
     * 
     * @param tags Map containing the copied tags
     * @param row the current row
     */
    private void addKeyValue(@NonNull Map<String, String> tags, @NonNull final TagEditRow row) {
        String key = row.getKey().trim();
        if (!"".equals(key)) {
            tags.put(key, row.getValue().trim());
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return performAction(item.getItemId());
    }

    /**
     * Perform whatever was selected on the menu
     * 
     * @param action the action id
     * @return true if the action was consumed
     */
    private boolean performAction(int action) {
        TagEditorFragment tagEditorFragment = (TagEditorFragment) caller;
        List<TagEditRow> selected = getSelectedRows();
        switch (action) {
        case MENU_ITEM_DELETE:
            if (!selected.isEmpty()) {
                for (TagEditRow r : selected) {
                    r.delete();
                }
                tagEditorFragment.updateAutocompletePresetItem(null);
            }
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_COPY:
            copyTags(selected, false);
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_CUT:
            copyTags(selected, true);
            tagEditorFragment.updateAutocompletePresetItem(null);
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_COPY_SYSTEM:
            copyTagsToSystem(selected);
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_CREATE_PRESET:
            CustomPreset.create(tagEditorFragment, selected);
            tagEditorFragment.presetFilterUpdate.update(null);
            break;
        case MENU_ITEM_SELECT_ALL:
            ((PropertyRows) caller).selectAllRows();
            return true;
        case MENU_ITEM_DESELECT_ALL:
            ((PropertyRows) caller).deselectAllRows();
            break;
        case MENU_ITEM_HELP:
            HelpViewer.start(caller.getActivity(), R.string.help_propertyeditor);
            break;
        default:
            return false;
        }
        return true;
    }

    /**
     * Get the selected rows
     * 
     * @return a List of TagEditRow
     */
    @NonNull
    private List<TagEditRow> getSelectedRows() {
        final int size = rows.getChildCount();
        List<TagEditRow> selected = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            View view = rows.getChildAt(i);
            TagEditRow row = (TagEditRow) view;
            if (row.isSelected()) {
                selected.add(row);
            }
        }
        return selected;
    }
}
