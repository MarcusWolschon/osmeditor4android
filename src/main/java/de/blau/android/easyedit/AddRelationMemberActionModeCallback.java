package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.util.ThemeUtils;

/**
 * Callback for adding new members to a Relation
 * 
 * @author simon
 *
 */
public class AddRelationMemberActionModeCallback extends NonSimpleActionModeCallback {
    private static final int MENUITEM_REVERT = 1;

    private ArrayList<OsmElement> members;
    private Relation              relation    = null;
    private MenuItem              revertItem  = null;
    private boolean               backPressed = false;
    private boolean               existing    = false;

    /**
     * Construct a new AddRelationMemberActionModeCallback from a list of selected OsmElements
     * 
     * @param manager the current EasyEditManager instance
     * @param selection a List containing OsmElements
     */
    public AddRelationMemberActionModeCallback(@NonNull EasyEditManager manager, @NonNull List<OsmElement> selection) {
        super(manager);
        members = new ArrayList<>(selection);
    }

    /**
     * Construct a new AddRelationMemberActionModeCallback starting with a single OsmElement
     * 
     * @param manager the current EasyEditManager instance
     * @param element the OsmElement
     */
    public AddRelationMemberActionModeCallback(@NonNull EasyEditManager manager, @NonNull OsmElement element) {
        super(manager);
        members = new ArrayList<>();
        addElement(element);
    }

    /**
     * Construct a new AddRelationMemberActionModeCallback starting with an existing Relation any potentially a new
     * OsmElement to add
     * 
     * @param manager the current EasyEditManager instance
     * @param relation the existing Relation
     * @param element the OsmElement to add or null
     */
    public AddRelationMemberActionModeCallback(@NonNull EasyEditManager manager, @NonNull Relation relation, @Nullable OsmElement element) {
        super(manager);
        members = new ArrayList<>();
        if (element != null) {
            addElement(element);
        }
        this.relation = relation;
        existing = true;
    }

    /**
     * Add an OsmElement to an existing Relation
     * 
     * @param element the OsmElement to add
     */
    private void addElement(@NonNull OsmElement element) {
        members.add(element);
        if (element.getName().equals(Way.NAME)) {
            logic.addSelectedRelationWay((Way) element);
        } else if (element.getName().equals(Node.NAME)) {
            logic.addSelectedRelationNode((Node) element);
        } else if (element.getName().equals(Relation.NAME)) {
            logic.addSelectedRelationRelation((Relation) element);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addrelationmember;
        if (existing) {
            mode.setTitle(R.string.menu_edit_relation);
        } else {
            mode.setTitle(R.string.menu_relation);
        }
        mode.setSubtitle(R.string.menu_add_relation_member);
        super.onCreateActionMode(mode, menu);
        logic.setReturnRelations(true); // can add relations

        // menu setup
        menu = replaceMenu(menu, mode, this);
        revertItem = menu.add(Menu.NONE, MENUITEM_REVERT, Menu.NONE, R.string.tag_menu_revert)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo));
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help);
        arrangeMenu(menu); // needed at least once
        setClickableElements();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);

        boolean updated = ElementSelectionActionModeCallback.setItemVisibility(!members.isEmpty(), revertItem, false);
        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            switch (item.getItemId()) {
            case MENUITEM_REVERT: // remove last item in list
                if (!members.isEmpty()) {
                    OsmElement element = members.get(members.size() - 1);
                    if (element.getName().equals(Way.NAME)) {
                        logic.removeSelectedRelationWay((Way) element);
                    } else if (element.getName().equals(Node.NAME)) {
                        logic.removeSelectedRelationNode((Node) element);
                    } else if (element.getName().equals(Relation.NAME)) {
                        logic.removeSelectedRelationRelation((Relation) element);
                    }
                    members.remove(members.size() - 1);
                    setClickableElements();
                    main.invalidateMap();
                    if (members.isEmpty()) {
                        item.setVisible(false);
                    }
                }
                break;
            default:
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid elements can be
                                                            // clicked
        super.handleElementClick(element);
        addElement(element);
        setClickableElements();
        mode.invalidate();
        main.invalidateMap();
        return true;
    }

    /**
     * Calculate and set the elements the user can click
     */
    private void setClickableElements() {
        ArrayList<OsmElement> excludes = new ArrayList<>(members);
        if (relation != null) {
            logic.setSelectedRelationMembers(relation);
            excludes.addAll(relation.getMemberElements());
        }
        logic.setClickableElements(logic.findClickableElements(excludes));
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.deselectAll();
        if (!backPressed) {
            if (!members.isEmpty()) { // something was actually added
                if (relation == null) {
                    relation = logic.createRelation(main, null, members);
                    main.performTagEdit(relation, "type", false, false);
                } else {
                    logic.addMembers(main, relation, members);
                    main.performTagEdit(relation, null, false, false);
                }
                // starting action mode here doesn't seem to work ... main.startSupportActionMode(new
                // RelationSelectionActionModeCallback(relation));
            }
        }
    }

    /**
     * back button should abort relation creation
     */
    @Override
    public boolean onBackPressed() {
        backPressed = true;
        return super.onBackPressed(); // call the normal stuff
    }
}
