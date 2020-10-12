package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;
import de.blau.android.util.ThemeUtils;

/**
 * Callback for adding new members to a Relation
 * 
 * @author Simon
 *
 */
public class AddRelationMemberActionModeCallback extends BuilderActionModeCallback {
    private static final int MENUITEM_REVERT = 1;

    private static final String RELATION_ID_KEY = "relation id";
    private static final String MEMBERS_KEY     = "members";

    private final List<RelationMember> members    = new ArrayList<>();
    private Relation                   relation   = null;
    private MenuItem                   revertItem = null;
    private boolean                    existing   = false;

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public AddRelationMemberActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        List<RelationMember> savedMembers = state.getList(MEMBERS_KEY);
        StorageDelegator delegator = App.getDelegator();
        for (RelationMember member : savedMembers) {
            OsmElement element = delegator.getOsmElement(member.getType(), member.getRef());
            if (element != null) {
                member.setElement(element);
                highlight(element);
            }
            members.add(member);
        }
        Long relationId = state.getLong(RELATION_ID_KEY);
        if (relationId != null) {
            relation = (Relation) delegator.getOsmElement(Relation.NAME, relationId);
        }
    }

    /**
     * Construct a new AddRelationMemberActionModeCallback from a list of selected OsmElements
     * 
     * @param manager the current EasyEditManager instance
     * @param selection a List containing OsmElements
     */
    public AddRelationMemberActionModeCallback(@NonNull EasyEditManager manager, @NonNull List<OsmElement> selection) {
        super(manager);
        for (OsmElement e : selection) {
            addElement(e);
        }
    }

    /**
     * Construct a new AddRelationMemberActionModeCallback starting with a single OsmElement
     * 
     * @param manager the current EasyEditManager instance
     * @param element the OsmElement
     */
    public AddRelationMemberActionModeCallback(@NonNull EasyEditManager manager, @NonNull OsmElement element) {
        super(manager);
        addElement(element);
    }

    /**
     * Construct a new AddRelationMemberActionModeCallback starting with an existing Relation and potentially a new
     * OsmElement to add
     * 
     * @param manager the current EasyEditManager instance
     * @param relation the existing Relation
     * @param element the OsmElement to add or null
     */
    public AddRelationMemberActionModeCallback(@NonNull EasyEditManager manager, @NonNull Relation relation, @Nullable OsmElement element) {
        super(manager);
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
        members.add(new RelationMember("", element));
        highlight(element);
    }

    /**
     * Pretend that the element is already a member and high light it
     * 
     * @param element the element
     */
    public void highlight(@NonNull OsmElement element) {
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
        boolean updated = super.onPrepareActionMode(mode, menu);
        updated |= ElementSelectionActionModeCallback.setItemVisibility(!members.isEmpty(), revertItem, false);
        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item) && item.getItemId() == MENUITEM_REVERT && !members.isEmpty()) {
            RelationMember member = members.get(members.size() - 1);
            OsmElement element = member.getElement();
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
            return true;
        }
        return false;
    }

    @Override
    public boolean handleElementClick(OsmElement element) {
        // due to clickableElements, only valid elements can be clicked
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
        ArrayList<OsmElement> excludes = new ArrayList<>();
        for (RelationMember member : members) {
            excludes.add(member.getElement());
        }
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
    }

    @Override
    public void finishBuilding() {
        if (!members.isEmpty()) { // something was actually added
            if (relation == null) {
                relation = logic.createRelationFromMembers(main, null, members);
                main.performTagEdit(relation, Tags.KEY_TYPE, false, false);
            } else {
                logic.addRelationMembers(main, relation, members);
                main.performTagEdit(relation, null, false, false);
            }
            main.startSupportActionMode(new RelationSelectionActionModeCallback(manager, relation));
        }
    }

    @Override
    protected boolean hasData() {
        System.out.println("members is empty " + members.isEmpty());
        return !members.isEmpty();
    }

    @Override
    public void saveState(SerializableState state) {
        List<RelationMember> savedMembers = new ArrayList<>();
        for (RelationMember member : members) {
            RelationMember toSave = new RelationMember(member);
            toSave.setElement(null);
            savedMembers.add(toSave);
        }
        state.putList(MEMBERS_KEY, savedMembers);
        if (relation != null) {
            state.putLong(RELATION_ID_KEY, relation.getOsmId());
        }
    }
}
