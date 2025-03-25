package io.vespucci.easyedit;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.dialogs.EmptyRelation;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.RelationMember;
import io.vespucci.osm.Tags;
import io.vespucci.osm.ViewBox;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.ThemeUtils;
import io.vespucci.util.Util;

public class RelationSelectionActionModeCallback extends ElementSelectionActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RelationSelectionActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = RelationSelectionActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_SELECT_RELATION_MEMBERS = LAST_REGULAR_MENUITEM + 1;
    private static final int MENUITEM_ROTATE                  = LAST_REGULAR_MENUITEM + 2;

    private MenuItem selectMembersItem;
    private MenuItem rotateItem;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param relation the selected Relation
     */
    public RelationSelectionActionModeCallback(@NonNull EasyEditManager manager, @NonNull Relation relation) {
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

        // menu setup
        menu = replaceMenu(menu, mode, this);
        selectMembersItem = menu.add(Menu.NONE, MENUITEM_SELECT_RELATION_MEMBERS, Menu.NONE, R.string.menu_select_relation_members)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation_members));

        rotateItem = menu.add(Menu.NONE, MENUITEM_ROTATE, Menu.NONE, R.string.menu_rotate).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_rotate));

        return true;
    }

    /**
     * If the relation is empty, terminate the action mode and show a dialog
     * 
     * @param mode current ActionMode
     * @return true if Relation is empty
     */
    private boolean checkForEmptyRelation(@NonNull ActionMode mode) {
        if (element != null && (((Relation) element).getMembers() == null || ((Relation) element).getMembers().isEmpty())) {
            EmptyRelation.showDialog(main, element.getOsmId());
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
        boolean updated = super.onPrepareActionMode(mode, menu);

        updated |= setItemVisibility(((Relation) element).getMembers() != null, selectMembersItem, false);

        updated |= setItemVisibility(element.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON) && ((Relation) element).allDownloaded(), rotateItem,
                false);

        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            switch (item.getItemId()) {
            case MENUITEM_SELECT_RELATION_MEMBERS:
                selectMembers();
                break;
            case MENUITEM_ROTATE:
                deselect = false;
                main.startSupportActionMode(new RotationActionModeCallback(manager));
                break;
            case MENUITEM_SHARE_POSITION:
                ViewBox box = new ViewBox(element.getBounds());
                Util.sharePosition(main, box.getCenter(), main.getMap().getZoomLevel()); // the center of the box is
                                                                                         // only a rough value
                break;
            default:
                return false;
            }
        }
        return true;
    }

    /**
     * Select the relation members and start the multi-select action mode
     */
    private void selectMembers() {
        List<OsmElement> selection = new ArrayList<>();
        List<RelationMember> members = ((Relation) element).getMembers();
        if (members != null) {
            for (RelationMember rm : members) {
                OsmElement e = rm.getElement();
                if (e != null) {
                    selection.add(e);
                }
            }
        }
        if (!selection.isEmpty()) {
            deselect = false;
            main.startSupportActionMode(new MultiSelectWithGeometryActionModeCallback(manager, selection));
            if (members != null && members.size() != selection.size()) {
                ScreenMessage.toastTopWarning(main, R.string.toast_members_not_downloaded);
            }
        }
    }

    @Override
    protected void menuDelete(final ActionMode mode) {
        final Relation r = (Relation) element;
        if (r.hasParentRelations()) {
            new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deleterelation_relation_description)
                    .setPositiveButton(R.string.deleterelation, (dialog, which) -> deleteRelation(mode, r)).show();
        } else {
            deleteRelation(mode, r);
        }
    }

    /**
     * Actually delete the Relation
     * 
     * @param mode the ActionMode
     * @param r the Relation
     */
    private void deleteRelation(@Nullable final ActionMode mode, @NonNull final Relation r) {
        List<Relation> origParents = r.hasParentRelations() ? new ArrayList<>(r.getParentRelations()) : null;
        logic.performEraseRelation(main, r, true);
        if (mode != null) {
            mode.finish();
        }
        checkEmptyRelations(main, origParents);
    }
}
