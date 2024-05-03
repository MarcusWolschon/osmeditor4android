package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.Logic;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.easyedit.EasyEditActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationUtils;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.propertyeditor.RelationMembersFragment.MemberEntry;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.collections.LinkedList;

public class RelationMemberSelectedActionModeCallback extends SelectedRowsActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RelationMemberSelectedActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = RelationMemberSelectedActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    // pm: protected static final int MENU_ITEM_DELETE = 1;
    // pm: private static final int MENU_ITEM_COPY = 2;
    // pm: private static final int MENU_ITEM_CUT = 3;
    // pm: protected static final int MENU_ITEM_HELP = 15;
    private static final int MENU_ITEM_MOVE_UP       = 4;
    private static final int MENU_ITEM_MOVE_DOWN     = 5;
    private static final int MENU_ITEM_SORT          = 6;
    private static final int MENU_ITEM_REVERSE_ORDER = 7;
    private static final int MENU_ITEM_DOWNLOAD      = 8;
    private static final int MENU_ITEM_TOP           = 9;
    private static final int MENU_ITEM_BOTTOM        = 10;
    private static final int MENU_ITEM_MOVE_TOP      = 11;
    private static final int MENU_ITEM_MOVE_BOTTOM   = 12;

    private final RelationMemberAdapter adapter;
    private final List<MemberEntry>     members;

    /**
     * Construct a new callback for selected RelationMembers
     * 
     * @param caller the calling Fragment
     * @param adapter a RelationMemberAdapter
     * @param members a List of MemberEntry
     */
    public RelationMemberSelectedActionModeCallback(@NonNull Fragment caller, @NonNull RelationMemberAdapter adapter, @NonNull List<MemberEntry> members) {
        super(caller);
        this.adapter = adapter;
        this.members = members;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        mode.setTitle(R.string.tag_action_members_title);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.clear();
        Context context = caller.getActivity();
        menu.add(Menu.NONE, SelectedRowsActionModeCallback.MENU_ITEM_DELETE, Menu.NONE, R.string.delete)
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_delete));
        menu.add(EasyEditActionModeCallback.GROUP_BASE, SelectedRowsActionModeCallback.MENU_ITEM_SELECT_ALL, Menu.CATEGORY_SYSTEM, R.string.menu_select_all)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(EasyEditActionModeCallback.GROUP_BASE, SelectedRowsActionModeCallback.MENU_ITEM_DESELECT_ALL, Menu.CATEGORY_SYSTEM, R.string.menu_deselect_all)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(EasyEditActionModeCallback.GROUP_BASE, SelectedRowsActionModeCallback.MENU_ITEM_HELP, Menu.CATEGORY_SYSTEM, R.string.menu_help)
                .setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_help)).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_help));

        menu.add(Menu.NONE, MENU_ITEM_MOVE_UP, Menu.NONE, R.string.tag_menu_move_up).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_up));
        menu.add(Menu.NONE, MENU_ITEM_MOVE_DOWN, Menu.NONE, R.string.tag_menu_move_down).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_down));
        menu.add(Menu.NONE, MENU_ITEM_SORT, Menu.NONE, R.string.tag_menu_sort).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_sort))
                .setEnabled(!members.isEmpty());
        menu.add(Menu.NONE, MENU_ITEM_REVERSE_ORDER, Menu.NONE, R.string.tag_menu_reverse_order)
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_reverse_order)).setEnabled(!members.isEmpty());

        // we only display the download button if at least one of the selected elements isn't downloaded
        boolean nonDownloadedSelected = false;

        for (MemberEntry member : members) {
            if (member.selected && !member.downloaded()) {
                nonDownloadedSelected = true;
                break;
            }
        }

        MenuItem downloadItem = menu.add(Menu.NONE, MENU_ITEM_DOWNLOAD, Menu.NONE, R.string.tag_menu_download)
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_download));
        // if we don't have network connectivity disable
        downloadItem.setEnabled(new NetworkStatus(caller.getContext()).isConnected() && nonDownloadedSelected);

        menu.add(Menu.NONE, MENU_ITEM_TOP, Menu.NONE, R.string.tag_menu_top);
        menu.add(Menu.NONE, MENU_ITEM_BOTTOM, Menu.NONE, R.string.tag_menu_bottom);
        menu.add(Menu.NONE, MENU_ITEM_MOVE_TOP, Menu.NONE, R.string.tag_menu_move_top);
        menu.add(Menu.NONE, MENU_ITEM_MOVE_BOTTOM, Menu.NONE, R.string.tag_menu_move_bottom);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        final int size = members.size();
        final List<MemberEntry> selected = new ArrayList<>();
        final List<Integer> selectedPos = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MemberEntry member = members.get(i);
            if (member.selected) {
                selected.add(member);
                selectedPos.add(i);
            }
        }
        final int selectedCount = selectedPos.size();
        if (selectedCount < 1) {
            Log.e(DEBUG_TAG, "onActionItemClicked called with nothing selected");
            return true;
        }
        int change = 1;
        int action = item.getItemId();
        switch (action) {
        case SelectedRowsActionModeCallback.MENU_ITEM_DELETE:
            for (MemberEntry member : selected) {
                members.remove(member);
            }
            if (currentAction != null) {
                currentAction.finish();
            }
            update();
            break;
        case SelectedRowsActionModeCallback.MENU_ITEM_SELECT_ALL:
            ((PropertyRows) caller).selectAllRows();
            return true;
        case SelectedRowsActionModeCallback.MENU_ITEM_DESELECT_ALL:
            ((PropertyRows) caller).deselectAllRows();
            return true;
        case SelectedRowsActionModeCallback.MENU_ITEM_HELP:
            HelpViewer.start(caller.getActivity(), R.string.help_propertyeditor);
            return true;
        case MENU_ITEM_MOVE_TOP:
            change = selectedPos.get(0);
        case MENU_ITEM_MOVE_UP:
            for (int i = 0; i < selectedCount; i++) {
                int p = selectedPos.get(i);
                int newPos = p - change;
                members.remove(p);
                if (newPos < 0) {
                    // one row removed at top. fix up positions
                    selectedPos.set(i, size - 1);
                    for (int j = i + 1; j < selectedCount; j++) {
                        selectedPos.set(j, selectedPos.get(j) - 1);
                    }
                    members.add(selected.get(i)); // add at end
                    break;
                } else {
                    selectedPos.set(i, newPos);
                    members.add(newPos, selected.get(i));
                }
            }
            update();
            ((RelationMembersFragment) caller).scrollToRow(selectedPos.get(0));
            return true;
        case MENU_ITEM_MOVE_BOTTOM:
            change = size - selectedPos.get(selectedCount - 1) - 1;
        case MENU_ITEM_MOVE_DOWN:
            for (int i = selectedCount - 1; i >= 0; i--) {
                int p = selectedPos.get(i);
                int newPos = p + change;
                members.remove(p);
                if (newPos > size - 1) {
                    // one row removed at bottom. fix up positions
                    selectedPos.set(i, 0);
                    for (int j = i - 1; j >= 0; j--) {
                        selectedPos.set(j, selectedPos.get(j) + 1);
                    }
                    members.add(0, selected.get(i)); // add at top
                    break;
                } else {
                    selectedPos.set(i, newPos);
                    members.add(newPos, selected.get(i));
                }
            }
            update();
            ((RelationMembersFragment) caller).scrollToRow(selectedPos.get(selectedPos.size() - 1));
            return true;
        case MENU_ITEM_SORT:
            List<Map<String, String>> tags = ((RelationMembersFragment) caller).propertyEditorListener.getUpdatedTags();
            boolean lineLike = false; // this needs a better name
            if (tags != null && tags.size() == 1) {
                String type = tags.get(0).get(Tags.KEY_TYPE);
                lineLike = Tags.VALUE_MULTIPOLYGON.equals(type) || Tags.VALUE_BOUNDARY.equals(type);
            }
            List<MemberEntry> temp = RelationUtils.sortRelationMembers(selected, new LinkedList<>(),
                    lineLike ? RelationUtils::haveEndConnection : RelationUtils::haveCommonNode);
            int top = members.indexOf(temp.get(0));
            for (MemberEntry entry : temp) {
                if (members.contains(entry)) {
                    members.remove(entry);
                }
            }
            members.addAll(Math.min(top, members.size()), temp);
            update();
            ((RelationMembersFragment) caller).scrollToRow(top);
            return true;
        case MENU_ITEM_REVERSE_ORDER:
            temp = new ArrayList<>(selected);
            Collections.reverse(temp);
            top = members.indexOf(temp.get(0));
            int i = 0;
            for (Integer p : selectedPos) {
                members.set(p, temp.get(i));
                i++;
            }
            update();
            ((RelationMembersFragment) caller).scrollToRow(top);
            return true;
        case MENU_ITEM_TOP:
        case MENU_ITEM_BOTTOM:
            ((RelationMembersFragment) caller).scrollToRow(action == MENU_ITEM_TOP ? 0 : size - 1);
            return true;
        case MENU_ITEM_DOWNLOAD:
            Progress.showDialog(caller.getActivity(), Progress.PROGRESS_DOWNLOAD);
            PostAsyncActionHandler handler = () -> {
                if (currentAction != null) {
                    for (int j = 0; j < selectedCount; j++) {
                        MemberEntry row = selected.get(j);
                        if (!row.downloaded()) {
                            updateRow(row);
                            selected.set(j, row);
                        }
                    }
                    currentAction.finish();
                    Progress.dismissDialog(caller.getActivity(), Progress.PROGRESS_DOWNLOAD);
                    update();
                }
            };
            final Logic logic = App.getLogic();
            if (selectedCount < size) {
                List<Long> nodes = new ArrayList<>();
                List<Long> ways = new ArrayList<>();
                List<Long> relations = new ArrayList<>();
                for (i = 0; i < selectedCount; i++) {
                    MemberEntry row = selected.get(i);
                    if (!row.downloaded()) {
                        switch (row.getType()) {
                        case Node.NAME:
                            nodes.add(row.getRef());
                            break;
                        case Way.NAME:
                            ways.add(row.getRef());
                            break;
                        case Relation.NAME:
                            relations.add(row.getRef());
                            break;
                        default:
                            Log.e(DEBUG_TAG, "Unknown member tyoe " + row.getType());
                        }
                    }
                }
                logic.downloadElements(caller.getActivity(), nodes, ways, relations, handler);
            } else {
                logic.downloadElement(caller.getActivity(), Relation.NAME, ((RelationMembersFragment) caller).getOsmId(), true, false, handler);
            }
            invalidate();
            return true;
        default:
            return false;

        }
        return true;
    }

    /**
     * Update the connections and notify the adapter
     */
    private void update() {
        for (int i = 0; i < members.size(); i++) {
            members.get(i).setPosition(i);
        }
        ((RelationMembersFragment) caller).setIcons();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // don't try to call super here
        for (MemberEntry member : members) {
            member.selected = false;
        }
        adapter.notifyDataSetChanged();
        onDestroyActionModeCommon();
    }

    /**
     * Check if all rows have been de-selected
     * 
     * @param skipHeaderRow if true skip the header row
     * @return true if no rows are selected
     */
    @Override
    public boolean rowsDeselected(boolean skipHeaderRow) {
        for (MemberEntry entry : members) {
            if (entry.selected) {
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
     * Update an entry from downloaded element
     * 
     * @param entry the entry to update
     */
    private void updateRow(@NonNull MemberEntry entry) {
        entry.setElement(entry.getElement());
        entry.update();
    }

    /**
     * Save the currently selected members
     * 
     * @param outState the Bundle to save the member numbers in to
     */
    @Override
    public void saveState(@NonNull Bundle outState) {
        final int size = members.size();
        ArrayList<Integer> selectedMembers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (members.get(i).selected) {
                selectedMembers.add(i);
            }
        }
        outState.putIntegerArrayList(SelectedRowsActionModeCallback.SELECTED_ROWS_KEY, selectedMembers);
    }

    /**
     * Restore the selected members
     * 
     * @param inState the Bundle to restore the row members from
     */
    @Override
    public void restoreState(@NonNull Bundle inState) {
        List<Integer> selectedMembers = inState.getIntegerArrayList(SelectedRowsActionModeCallback.SELECTED_ROWS_KEY);
        if (selectedMembers == null) {
            Log.e(DEBUG_TAG, "restoreState selectedMembers null");
            return;
        }
        final int size = members.size();
        for (int i : selectedMembers) {
            if (i <= size - 1) {
                members.get(i).selected = true;
            }
        }
    }
}
