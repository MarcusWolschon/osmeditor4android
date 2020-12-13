package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * This callback handles path creation.
 */
public class PathCreationActionModeCallback extends BuilderActionModeCallback {
    private static final String DEBUG_TAG = "PathCreationAction...";

    private static final int MENUITEM_UNDO          = 1;
    private static final int MENUITEM_NEWWAY_PRESET = 2;

    private static final String NODE_IDS_KEY = "way ids";
    private static final String WAY_ID_KEY   = "node id";
    private static final String TITLE_KEY    = "title";

    /** x coordinate of first node */
    private float   x;
    /** y coordinate of first node */
    private float   y;
    /** Node to append to */
    private Node    appendTargetNode;
    /** Way to append to */
    private Way     appendTargetWay;
    /** flag if we don't want to start the property editor in onDestroy **/
    private boolean dontTag = false;

    /** contains a pointer to the created way if one was created. used to fix selection after undo. */
    private Way        createdWay   = null;
    /** contains a list of created nodes. used to fix selection after undo. */
    private List<Node> createdNodes = new ArrayList<>();

    private String savedTitle = null;

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public PathCreationActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        List<Long> ids = state.getList(NODE_IDS_KEY);
        StorageDelegator delegator = App.getDelegator();
        for (Long id : ids) {
            Node node = (Node) delegator.getOsmElement(Node.NAME, id);
            if (node != null) {
                createdNodes.add(node);
            } else {
                throw new IllegalStateException("Failed to find node " + id);
            }
        }
        if (!createdNodes.isEmpty()) {
            appendTargetNode = createdNodes.get(createdNodes.size() - 1);
        }
        Long wayId = state.getLong(WAY_ID_KEY);
        if (wayId != null) {
            createdWay = (Way) delegator.getOsmElement(Way.NAME, wayId);
            appendTargetWay = createdWay;
        }
        savedTitle = state.getString(TITLE_KEY);
    }

    /**
     * Construct a new PathCreationActionModeCallback starting with screen coordinates
     * 
     * @param manager the current EasyEditManager instance
     * @param x screen x
     * @param y screen y
     */
    public PathCreationActionModeCallback(@NonNull EasyEditManager manager, float x, float y) {
        super(manager);
        this.x = x;
        this.y = y;
        appendTargetNode = null;
        appendTargetWay = null;
    }

    /**
     * Construct a new PathCreationActionModeCallback starting with an existing Node
     * 
     * @param manager the current EasyEditManager instance
     * @param node the existing Node
     */
    public PathCreationActionModeCallback(@NonNull EasyEditManager manager, @NonNull Node node) {
        super(manager);
        appendTargetNode = node;
        appendTargetWay = null;
    }

    /**
     * Construct a new PathCreationActionModeCallback starting with an existing Way and an existing Node to add
     * 
     * @param manager the current EasyEditManager instance
     * @param way the exiting Way
     * @param node the existing Node to add
     */
    public PathCreationActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Node node) {
        super(manager);
        appendTargetNode = node;
        appendTargetWay = way;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_pathcreation;
        super.onCreateActionMode(mode, menu);
        if (savedTitle != null) {
            mode.setTitle(savedTitle);
        }
        mode.setSubtitle(R.string.actionmode_createpath);
        logic.setSelectedWay(null);
        logic.setSelectedNode(appendTargetNode);
        if (appendTargetNode != null) {
            if (appendTargetWay != null) {
                logic.performAppendStart(appendTargetWay, appendTargetNode);
            } else {
                logic.performAppendStart(appendTargetNode);
            }
        } else {
            try {
                pathCreateNode(x, y);
            } catch (OsmIllegalOperationException e) {
                Snack.barError(main, e.getLocalizedMessage());
            }
        }
        logic.hideCrosshairs();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        menu.add(Menu.NONE, MENUITEM_UNDO, Menu.NONE, R.string.undo).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo));
        menu.add(Menu.NONE, MENUITEM_NEWWAY_PRESET, Menu.NONE, R.string.tag_menu_preset).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_preset));
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return super.onPrepareActionMode(mode, menu);
    }

    @Override
    public boolean handleClick(float x, float y) {
        super.handleClick(x, y);
        try {
            pathCreateNode(x, y);
        } catch (OsmIllegalOperationException e) {
            Snack.barError(main, e.getLocalizedMessage());
        }
        return true;
    }

    /**
     * Creates/adds a node into a path during path creation
     * 
     * @param x x screen coordinate
     * @param y y screen coordinate
     */
    private synchronized void pathCreateNode(float x, float y) {
        Node lastSelectedNode = logic.getSelectedNode();
        Way lastSelectedWay = logic.getSelectedWay();
        final boolean firstNode = createdNodes.isEmpty();
        if (appendTargetNode != null) {
            logic.performAppendAppend(main, x, y, firstNode);
            appendTargetNode = logic.getSelectedNode();
        } else {
            logic.performAdd(main, x, y, firstNode);
        }
        if (logic.getSelectedNode() == null) {
            // user clicked last node again -> finish adding
            delayedResetHasProblem(lastSelectedWay);
            manager.finish();
            removeCheckpoint();
            tagApplicable(lastSelectedNode, lastSelectedWay, true);
        } else { // update cache for undo
            createdWay = logic.getSelectedWay();
            if (createdWay == null) {
                createdNodes = new ArrayList<>();
            } else {
                createdWay.dontValidate();
            }
            createdNodes.add(logic.getSelectedNode());
        }
        main.invalidateMap();
    }

    /**
     * Remove spurious empty checkpoint created by touching again
     */
    private void removeCheckpoint() {
        App.getLogic().removeCheckpoint(main, createdWay != null ? R.string.undo_action_moveobjects : R.string.undo_action_movenode);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_UNDO:
            handleUndo();
            break;
        case MENUITEM_NEWWAY_PRESET:
            Way lastSelectedWay = logic.getSelectedWay();
            if (lastSelectedWay != null) {
                dontTag = true;
                main.startSupportActionMode(new WaySelectionActionModeCallback(manager, lastSelectedWay));
                // show preset screen
                main.performTagEdit(lastSelectedWay, null, false, item.getItemId() == MENUITEM_NEWWAY_PRESET);
            }
            return true;
        default:
            Log.e(DEBUG_TAG, "Unknown menu item");
            break;
        }
        return false;
    }

    /**
     * Handle presses on the undo button, this does not invoke the normal undo mechanism but simply removes the
     * non-saved nodes one by one
     */
    private synchronized void handleUndo() {
        Node removedNode = createdNodes.remove(createdNodes.size() - 1);
        if (createdWay != null) {
            logic.performRemoveEndNodeFromWay(main, createdWay.getLastNode().equals(logic.getSelectedNode()), createdWay, false);
            createdWay.dontValidate();
            if (OsmElement.STATE_DELETED == createdWay.getState()) {
                createdWay = null;
            }
        } else {
            logic.performEraseNode(main, removedNode, false);
        }
        // exit or select the previous node
        if (createdNodes.isEmpty()) {
            logic.setSelectedNode(null);
            // all nodes have been deleted, cancel action mode
            manager.finish();
        } else {
            // select last node
            logic.setSelectedNode(createdNodes.get(createdNodes.size() - 1));
        }

        createdWay = logic.getSelectedWay(); // will be null if way was deleted by undo
        main.invalidateMap();
    }

    /**
     * Path creation action mode is ending
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        super.onDestroyActionMode(mode);
    }

    /**
     * Hackish way of suppressing unnecessary validation failures
     * 
     * 1 sec seems to be enough to avoid invalidations and by that redraws of the map
     * 
     * @param way the way that we want to enable validation on
     */
    private void delayedResetHasProblem(@NonNull final Way way) {
        Map map = main.getMap();
        if (map != null) {
            map.postDelayed(() -> {
                if (way != null) {
                    way.resetHasProblem(); // remove Validator.OK
                }
            }, 1000);
        }
    }

    @Override
    public boolean processShortcut(Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_copy)) {
            handleUndo();
        }
        return super.processShortcut(c);
    }

    @Override
    protected void finishBuilding() {
        if (appendTargetNode == null && !dontTag) { // doesn't work as intended element selected modes get zapped,
            final Way lastSelectedWay = logic.getSelectedWay();
            final Node lastSelectedNode = logic.getSelectedNode();
            manager.finish();
            removeCheckpoint();
            tagApplicable(lastSelectedNode, lastSelectedWay, false);
            delayedResetHasProblem(lastSelectedWay);
        }
    }

    @Override
    public void saveState(SerializableState state) {
        List<Long> nodeIds = new ArrayList<>();
        for (Node n : createdNodes) {
            nodeIds.add(n.getOsmId());
        }
        state.putList(NODE_IDS_KEY, nodeIds);
        if (createdWay != null) {
            state.putLong(WAY_ID_KEY, createdWay.getOsmId());
        }
        state.putString(TITLE_KEY, mode.getTitle().toString());
    }

    @Override
    protected boolean hasData() {
        return true;
    }

    @Override
    public boolean onBackPressed() {
        Way lastSelectedWay = logic.getSelectedWay();
        if (lastSelectedWay != null) {
            lastSelectedWay.resetHasProblem();
        }
        return super.onBackPressed();
    }

    /**
     * Takes a parameter for a node and one for a way. If the way is not null, opens a tag editor for the way.
     * Otherwise, opens a tag editor for the node (unless the node is also null, then nothing happens).
     * 
     * @param possibleNode a node that was edited, or null
     * @param possibleWay a way that was edited, or null
     * @param select select the element before starting the PropertyEditor
     */
    private void tagApplicable(@Nullable final Node possibleNode, @Nullable final Way possibleWay, final boolean select) {
        if (possibleWay == null) {
            // Single node was added
            if (possibleNode != null) { // null-check to be sure
                if (select) {
                    main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, possibleNode));
                }
                main.performTagEdit(possibleNode, null, false, false);
            } else {
                Log.e(DEBUG_TAG, "tagApplicable called with null arguments");
            }
        } else { // way was added
            if (select) {
                main.startSupportActionMode(new WaySelectionActionModeCallback(manager, possibleWay));
            }
            main.performTagEdit(possibleWay, null, false, false);
        }
    }
}
