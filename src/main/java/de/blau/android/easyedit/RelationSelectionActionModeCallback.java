package de.blau.android.easyedit;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.R;
import de.blau.android.dialogs.EmptyRelation;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.ViewBox;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class RelationSelectionActionModeCallback extends ElementSelectionActionModeCallback {
    private static final String DEBUG_TAG = "RelationSelectionAct...";

    private static final int MENUITEM_ADD_RELATION_MEMBERS    = 10;
    private static final int MENUITEM_SELECT_RELATION_MEMBERS = 11;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param relation the selected Relation
     */
    public RelationSelectionActionModeCallback(EasyEditManager manager, Relation relation) {
        super(manager, relation);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_relationselection;
        super.onCreateActionMode(mode, menu);
        if (checkForEmptyRelation(mode)) {
            return false;
        }
        logic.setSelectedRelation((Relation) element);
        Log.d(DEBUG_TAG, "selected relations " + logic.selectedRelationsCount());
        mode.setTitle(R.string.actionmode_relationselect);
        mode.setSubtitle(null);
        main.invalidateMap();
        return true;
    }

    /**
     * If the relation is empty, terminate the action mode and show a dialog
     * 
     * @param mode current ActionMode
     * @return true if Relation is empty
     */
    private boolean checkForEmptyRelation(ActionMode mode) {
        if (element != null && (((Relation) element).getMembers() == null || ((Relation) element).getMembers().isEmpty())) {
            EmptyRelation.showDialog(main, ((Relation) element).getOsmId());
            mode.finish();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (checkForEmptyRelation(mode)) {
            return true;
        }
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.add(Menu.NONE, MENUITEM_ADD_RELATION_MEMBERS, Menu.NONE, R.string.menu_add_relation_member)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation_add_member));
        if (((Relation) element).getMembers() != null) {
            menu.add(Menu.NONE, MENUITEM_SELECT_RELATION_MEMBERS, Menu.NONE, R.string.menu_select_relation_members)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation_members));
        }
        arrangeMenu(menu);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            switch (item.getItemId()) {
            case MENUITEM_ADD_RELATION_MEMBERS:
                main.startSupportActionMode(new AddRelationMemberActionModeCallback(manager, (Relation) element, null));
                break;
            case MENUITEM_SELECT_RELATION_MEMBERS:
                ArrayList<OsmElement> selection = new ArrayList<>();
                if (((Relation) element).getMembers() != null) {
                    for (RelationMember rm : ((Relation) element).getMembers()) {
                        selection.add(rm.getElement());
                    }
                }
                if (!selection.isEmpty()) {
                    deselect = false;
                    main.startSupportActionMode(new ExtendSelectionActionModeCallback(manager, selection));
                }
                break;
            case MENUITEM_SHARE_POSITION:
                ViewBox box = new ViewBox(element.getBounds());
                Util.sharePosition(main, box.getCenter()); // the center of the box is only a rough value
                break;
            default:
                return false;
            }
        }
        return true;
    }

    @Override
    protected void menuDelete(final ActionMode mode) {
        final Relation r = (Relation) element;
        if (r.hasParentRelations()) {
            new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deleterelation_relation_description)
                    .setPositiveButton(R.string.deleterelation, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            logic.performEraseRelation(main, r, true);
                            if (mode != null) {
                                mode.finish();
                            }
                        }
                    }).show();
        } else {
            logic.performEraseRelation(main, r, true);
            mode.finish();
        }
    }
}
