package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.Way;
import de.blau.android.propertyeditor.RelationMembersFragment.Connected;
import de.blau.android.propertyeditor.RelationMembersFragment.RelationMemberRow;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class RelationMemberSelectedActionModeCallback extends SelectedRowsActionModeCallback {

    private static final String DEBUG_TAG = RelationMemberSelectedActionModeCallback.class.getSimpleName();

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

    public RelationMemberSelectedActionModeCallback(Fragment caller, LinearLayout rows) {
        super(caller, rows);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        mode.setTitle(R.string.tag_action_members_title);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        super.onPrepareActionMode(mode, menu);
        Context context = caller.getActivity();

        menu.add(Menu.NONE, MENU_ITEM_MOVE_UP, Menu.NONE, R.string.tag_menu_move_up).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_up));

        menu.add(Menu.NONE, MENU_ITEM_MOVE_DOWN, Menu.NONE, R.string.tag_menu_move_down).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_down));

        menu.add(Menu.NONE, MENU_ITEM_SORT, Menu.NONE, R.string.tag_menu_sort).setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_sort));

        menu.add(Menu.NONE, MENU_ITEM_REVERSE_ORDER, Menu.NONE, R.string.tag_menu_reverse_order)
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_reverse_order));

        // we only display the download button if at least one of the selected elements isn't downloaded
        boolean nonDownloadedSelected = false;
        final int size = rows.getChildCount();
        for (int i = 0; i < size; i++) {
            RelationMemberRow row = (RelationMemberRow) rows.getChildAt(i);
            if (row.isSelected() && !row.getRelationMemberDescription().downloaded()) {
                nonDownloadedSelected = true;
                break;
            }
        }
        if (nonDownloadedSelected) {
            menu.add(Menu.NONE, MENU_ITEM_DOWNLOAD, Menu.NONE, R.string.tag_menu_download)
                    .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_download));
        }

        menu.add(Menu.NONE, MENU_ITEM_TOP, Menu.NONE, R.string.tag_menu_top);

        menu.add(Menu.NONE, MENU_ITEM_BOTTOM, Menu.NONE, R.string.tag_menu_bottom);

        menu.add(Menu.NONE, MENU_ITEM_MOVE_TOP, Menu.NONE, R.string.tag_menu_move_top);

        menu.add(Menu.NONE, MENU_ITEM_MOVE_BOTTOM, Menu.NONE, R.string.tag_menu_move_bottom);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        return performAction(item.getItemId());
    }

    private boolean performAction(int action) {
        final int size = rows.getChildCount();
        final ArrayList<RelationMemberRow> selected = new ArrayList<>();
        final ArrayList<Integer> selectedPos = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            View view = rows.getChildAt(i);
            RelationMemberRow row = (RelationMemberRow) view;
            if (row.isSelected()) {
                selected.add(row);
                selectedPos.add(i);
            }
        }
        final int selectedCount = selectedPos.size();
        int change = 1;
        switch (action) {
        case MENU_ITEM_DELETE: // Note real work is done in super
            ((RelationMembersFragment) caller).setIcons();
            return true;
        case MENU_ITEM_MOVE_TOP:
            change = selectedPos.get(0).intValue();
        case MENU_ITEM_MOVE_UP:
            for (int i = 0; i < selectedCount; i++) {
                int p = selectedPos.get(i).intValue();
                int newPos = p - change;
                rows.removeViewAt(p);
                if (newPos < 0) {
                    // one row removed at top. fix up positions
                    selectedPos.set(i, size - 1);
                    for (int j = i + 1; j < selectedCount; j++) {
                        selectedPos.set(j, selectedPos.get(j) - 1);
                    }
                    rows.addView(selected.get(i)); // add at end
                    break;
                } else {
                    selectedPos.set(i, newPos);
                    rows.addView(selected.get(i), newPos);
                }
            }
            // this has some heuristics to avoid the selected row vanishing behind the top bars
            ((RelationMembersFragment) caller).scrollToRow(selected.get(0), true, action == MENU_ITEM_MOVE_TOP || forceScroll(selectedPos.get(0), size));
            ((RelationMembersFragment) caller).setIcons();
            return true;
        case MENU_ITEM_MOVE_BOTTOM:
            change = size - selectedPos.get(selectedCount - 1).intValue() - 1;
        case MENU_ITEM_MOVE_DOWN:
            for (int i = selectedCount - 1; i >= 0; i--) {
                int p = selectedPos.get(i).intValue();
                int newPos = p + change;
                rows.removeViewAt(p);
                if (newPos > size - 1) {
                    // one row removed at bottom. fix up positions
                    selectedPos.set(i, 0);
                    for (int j = i - 1; j >= 0; j--) {
                        selectedPos.set(j, selectedPos.get(j) + 1);
                    }
                    rows.addView(selected.get(i), 0); // add at end
                    break;
                } else {
                    selectedPos.set(i, newPos);
                    rows.addView(selected.get(i), newPos);
                }
            }
            // this has some heuristics to avoid the selected row vanishing behind the bottom actionbar
            ((RelationMembersFragment) caller).scrollToRow(selected.get(selected.size() - 1), false,
                    action == MENU_ITEM_MOVE_BOTTOM || forceScroll(selectedPos.get(selected.size() - 1), size));
            ((RelationMembersFragment) caller).setIcons();
            return true;
        case MENU_ITEM_SORT:
            List<RelationMemberDescription> rmds = new ArrayList<>();
            Map<RelationMemberDescription, RelationMemberRow> relationMemberRows = new HashMap<>();
            int top = selectedPos.get(0).intValue();
            for (int i = 0; i < selectedCount; i++) {
                RelationMemberRow row = selected.get(i);
                RelationMemberDescription rmd = row.getRelationMemberDescription();
                rmds.add(rmd);
                relationMemberRows.put(rmd, row);
                rows.removeView(row);
            }
            rmds = Util.sortRelationMembers(rmds);
            int pos = top;
            for (RelationMemberDescription rmd : rmds) {
                rows.addView(relationMemberRows.get(rmd), pos);
                pos++;
            }
            ((RelationMembersFragment) caller).scrollToRow(rows.getChildAt(top), false, false);
            ((RelationMembersFragment) caller).setIcons();
            return true;
        case MENU_ITEM_REVERSE_ORDER:
            top = selectedPos.get(0).intValue();
            List<RelationMemberRow> temp = new ArrayList<>(selected);
            Collections.reverse(temp);
            for (RelationMemberRow row : selected) {
                rows.removeView(row);
            }
            for (int i = 0; i < selectedPos.size(); i++) {
                pos = selectedPos.get(i);
                rows.addView(temp.get(i), pos);
            }
            ((RelationMembersFragment) caller).scrollToRow(rows.getChildAt(top), false, false);
            ((RelationMembersFragment) caller).setIcons();
            return true;
        case MENU_ITEM_TOP:
        case MENU_ITEM_BOTTOM:
            ((RelationMembersFragment) caller).scrollToRow(null, action == MENU_ITEM_TOP, false);
            return true;
        case MENU_ITEM_DOWNLOAD:
            Progress.showDialog(caller.getActivity(), Progress.PROGRESS_DOWNLOAD);
            PostAsyncActionHandler handler = new PostAsyncActionHandler() {
                @Override
                public void onSuccess() {
                    if (currentAction != null) {
                        for (int i = 0; i < selectedCount; i++) {
                            RelationMemberRow row = selected.get(i);
                            if (!row.getRelationMemberDescription().downloaded()) {
                                updateRow(row, selectedPos.get(i));
                                selected.set(i, row);
                            }
                        }
                        currentAction.finish();
                        Progress.dismissDialog(caller.getActivity(), Progress.PROGRESS_DOWNLOAD);
                        ((RelationMembersFragment) caller).setIcons();
                    }
                }

                @Override
                public void onError() {
                }
            };
            final Logic logic = App.getLogic();
            if (selectedCount < size) {
                List<Long> nodes = new ArrayList<>();
                List<Long> ways = new ArrayList<>();
                List<Long> relations = new ArrayList<>();
                for (int i = 0; i < selectedCount; i++) {
                    RelationMemberRow row = selected.get(i);
                    if (!row.getRelationMemberDescription().downloaded()) {
                        if (Node.NAME.equals(row.getType())) {
                            nodes.add(row.getOsmId());
                        } else if (Way.NAME.equals(row.getType())) {
                            ways.add(row.getOsmId());
                        } else if (Relation.NAME.equals(row.getType())) {
                            relations.add(row.getOsmId());
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
    }

    private void updateRow(RelationMemberRow row, int pos) {
        RelationMemberDescription rmd = row.getRelationMemberDescription();
        rmd.setElement(rmd.getElement());
        rmd.update();
        row.delete();
        ((RelationMembersFragment) caller).insertNewMember(rows, Integer.toString(pos), rmd, pos, Connected.NOT, true); // result
                                                                                                                        // not
                                                                                                                        // needed
    }

    private boolean forceScroll(Integer pos, int size) {
        return pos.intValue() < 3 || pos.intValue() > (size - 4);
    }
}
