package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatCheckBox;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.AddressInterpolationDialog;
import de.blau.android.dialogs.Tip;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.UndoStorage.UndoElement;
import de.blau.android.osm.UndoStorage.UndoWay;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Snack;
import de.blau.android.util.Sound;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * This callback handles path creation.
 */
public class PathCreationActionModeCallback extends BuilderActionModeCallback {
    private static final String DEBUG_TAG = "PathCreationAction...";

    protected static final int MENUITEM_UNDO          = 1;
    private static final int   MENUITEM_SNAP          = 2;
    private static final int   MENUITEM_NEWWAY_PRESET = 3;
    private static final int   MENUITEM_ADDRESS       = 4;

    private static final String NODE_IDS_KEY          = "node ids";
    private static final String EXISTING_NODE_IDS_KEY = "existing node ids";
    private static final String WAY_ID_KEY            = "way id";
    private static final String TITLE_KEY             = "title";
    private static final String SUBTITLE_KEY          = "subtitle";
    private static final String CHECKPOINT_NAME_KEY   = "checkpoint name";

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
    /** snap to existing ways/nodes */
    private boolean snap    = true;

    /** contains a pointer to the created way if one was created. used to fix selection after undo. */
    private Way          createdWay    = null;
    /** contains a list of added nodes. used to fix selection after undo. */
    protected List<Node> addedNodes    = new ArrayList<>();
    /** nodes we added that already existed */
    private List<Node>   existingNodes = new ArrayList<>();

    private String savedTitle    = null;
    private String savedSubtitle = null;

    /** what the checkpoint is called **/
    private Integer checkpointName;

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
                addedNodes.add(node);
            } else {
                throw new IllegalStateException("Failed to find node " + id);
            }
        }
        List<Long> existingIds = state.getList(EXISTING_NODE_IDS_KEY);
        for (Long id : existingIds) {
            Node node = (Node) delegator.getOsmElement(Node.NAME, id);
            if (node != null) {
                existingNodes.add(node);
            } else {
                throw new IllegalStateException("Failed to find node " + id);
            }
        }
        if (!addedNodes.isEmpty()) {
            appendTargetNode = addedNodes.get(addedNodes.size() - 1);
        }
        Long wayId = state.getLong(WAY_ID_KEY);
        if (wayId != null) {
            createdWay = (Way) delegator.getOsmElement(Way.NAME, wayId);
            appendTargetWay = createdWay;
        }
        savedTitle = state.getString(TITLE_KEY);
        savedSubtitle = state.getString(SUBTITLE_KEY);
        checkpointName = state.getInteger(CHECKPOINT_NAME_KEY);
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
        if (savedSubtitle != null) {
            mode.setSubtitle(savedSubtitle);
        } else {
            mode.setSubtitle(R.string.actionmode_createpath);
        }
        snap = logic.getPrefs().isWaySnapEnabled();
        logic.setSelectedWay(null);
        logic.setSelectedNode(appendTargetNode);
        if (appendTargetNode != null) {
            logic.performAppendStart(appendTargetWay, appendTargetNode);
            existingNodes.add(appendTargetNode);
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
        MenuItem undo = menu.add(Menu.NONE, MENUITEM_UNDO, Menu.NONE, R.string.undo).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo))
                .setVisible(!addedNodes.isEmpty());
        View undoView = main.getLayoutInflater().inflate(R.layout.undo_action_view, null);
        undoView.setOnClickListener((View v) -> handleUndo());
        undoView.setOnLongClickListener((View v) -> {
            Sound.beep();
            Tip.showDialog(main, R.string.tip_no_redo_key, R.string.tip_no_redo);
            return true;
        });
        undo.setActionView(undoView);
        addSnapCheckBox(main, menu, snap, (CompoundButton buttonView, boolean isChecked) -> {
            snap = isChecked;
            logic.getPrefs().enableWaySnap(isChecked);
        });
        //
        menu.add(Menu.NONE, MENUITEM_NEWWAY_PRESET, Menu.NONE, R.string.tag_menu_preset).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_preset));
        menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_address));
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return super.onPrepareActionMode(mode, menu);
    }

    /**
     * Change the action mode title, needs to be called before the action mode is started
     * 
     * @param titleRes the resource id of the title
     */
    public void setTitle(int titleRes) {
        savedTitle = manager.getMain().getString(titleRes);
    }

    /**
     * Change the action mode subtitle, needs to be called before the action mode is started
     * 
     * @param subtitleRes the resource id of the subtitle
     */
    public void setSubTitle(int subtitleRes) {
        savedSubtitle = manager.getMain().getString(subtitleRes);
    }

    /**
     * Add a checkbox to the menu to turn snapping on/off
     * 
     * @param ctx an Android Context
     * @param menu the Menu
     * @param snap initial state
     * @param listener an OnCheckedChangeListener
     */
    static void addSnapCheckBox(@NonNull Context ctx, @NonNull Menu menu, boolean snap, @NonNull OnCheckedChangeListener listener) {
        // setting an icon will make sure this gets shown
        MenuItem snapItem = menu.add(Menu.NONE, MENUITEM_SNAP, Menu.NONE, R.string.menu_snap).setIcon(ThemeUtils.getResIdFromAttribute(ctx, R.attr.menu_merge));
        AppCompatCheckBox check = (AppCompatCheckBox) LayoutInflater.from(ctx).inflate(R.layout.snap_action_view, null);
        check.setChecked(snap);
        check.setOnCheckedChangeListener(listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            check.setTooltipText(ctx.getString(R.string.menu_snap));
        }
        snapItem.setActionView(check);
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
        final boolean firstNode = addedNodes.isEmpty();
        Node clicked = logic.getClickedNode(x, y);
        if (appendTargetNode != null) {
            logic.performAppendAppend(main, x, y, firstNode, snap);
            appendTargetNode = logic.getSelectedNode();
            if (firstNode) {
                checkpointName = R.string.undo_action_append;
            }
        } else {
            logic.performAdd(main, x, y, firstNode, snap);
            if (firstNode) {
                checkpointName = R.string.undo_action_add;
            }
        }
        if (logic.getSelectedNode() == null) {
            // user clicked last node again -> finish adding
            finishPath(lastSelectedWay, lastSelectedNode);
        } else { // update cache for undo
            createdWay = logic.getSelectedWay();
            if (createdWay == null) {
                addedNodes = new ArrayList<>();
            } else {
                createdWay.dontValidate();
            }
            addedNodes.add(logic.getSelectedNode());
            if (firstNode) {
                mode.invalidate(); // activate undo
            }
            if (clicked != null) {
                // node already existed
                existingNodes.add(clicked);
            }
        }
        mode.setSubtitle(R.string.add_way_node_instruction);
        main.invalidateMap();
    }

    /**
     * Remove spurious empty checkpoint created by touching again
     */
    protected void removeCheckpoint() {
        App.getLogic().removeCheckpoint(main, createdWay != null ? R.string.undo_action_moveobjects : R.string.undo_action_movenode);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        final int itemId = item.getItemId();
        switch (itemId) {
        case MENUITEM_UNDO:
            handleUndo();
            break;
        case MENUITEM_NEWWAY_PRESET:
        case MENUITEM_ADDRESS:
            Way lastSelectedWay = logic.getSelectedWay();
            if (lastSelectedWay != null) {
                dontTag = true;
                main.startSupportActionMode(new WaySelectionActionModeCallback(manager, lastSelectedWay));
                if (itemId == MENUITEM_ADDRESS && !lastSelectedWay.isClosed()) {
                    AddressInterpolationDialog.showDialog(main, lastSelectedWay);
                } else {
                    // show preset screen
                    main.performTagEdit(lastSelectedWay, null, itemId == MENUITEM_ADDRESS, itemId == MENUITEM_NEWWAY_PRESET);
                }
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
        if (addedNodes.isEmpty()) {
            Log.e(DEBUG_TAG, "Undo called but nothing to undo");
        }
        Node removedNode = addedNodes.remove(addedNodes.size() - 1);
        final boolean deleteNode = !existingNodes.contains(removedNode);
        final List<Way> modifiedWays = logic.getWaysForNode(removedNode);
        if (createdWay != null) {
            logic.performRemoveEndNodeFromWay(main, createdWay.getLastNode().equals(logic.getSelectedNode()), createdWay, deleteNode, false);
            createdWay.dontValidate();
            if (OsmElement.STATE_DELETED == createdWay.getState()) {
                createdWay = null;
                logic.setSelectedWay(null);
            }
        } else if (deleteNode) {
            logic.performEraseNode(main, removedNode, false);
        }
        // undo any changes from creating and then removing nodes on ways
        if (deleteNode) {
            for (Way w : modifiedWays) {
                if (!w.equals(createdWay)) {
                    UndoStorage undo = logic.getUndo();
                    List<UndoElement> undoWays = undo.getUndoElements(w);
                    UndoElement undoWay = undoWays.get(undoWays.size() - 1);
                    if (undoWay instanceof UndoWay) {
                        if (((UndoWay) undoWay).getState() == OsmElement.STATE_UNCHANGED && w.getNodes().equals(((UndoWay) undoWay).getNodes())) {
                            undoWay.restore(); // this should just update the state
                            undo.remove(w);
                        } else {
                            Log.w(DEBUG_TAG, "Not fixing up " + w);
                        }
                    } else {
                        Log.e(DEBUG_TAG, "UndoElement should be an UndoWay " + undoWay.toString());
                    }
                }
            }
        }
        // exit or select the previous node
        if (addedNodes.isEmpty()) {
            logic.setSelectedNode(null);
            // delete undo checkpoint
            if (checkpointName != null) {
                logic.rollback();
            } else {
                Log.e(DEBUG_TAG, "checkpointName is null");
            }
            // all nodes have been deleted, cancel action mode
            manager.finish();
        } else {
            // select last node
            logic.setSelectedNode(addedNodes.get(addedNodes.size() - 1));
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
    protected void delayedResetHasProblem(@Nullable final Way way) {
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
        final Way lastSelectedWay = logic.getSelectedWay();
        final Node lastSelectedNode = logic.getSelectedNode();
        finishPath(lastSelectedWay, lastSelectedNode);
    }

    /**
     * Common code for finishing a path
     * 
     * @param lastSelectedWay the way
     * @param lastSelectedNode the node
     */
    protected void finishPath(@Nullable final Way lastSelectedWay, @Nullable final Node lastSelectedNode) {
        manager.finish();
        removeCheckpoint();
        if (!addedNodes.isEmpty() && !dontTag) {
            tagApplicable(lastSelectedNode, lastSelectedWay, false);
            delayedResetHasProblem(lastSelectedWay);
        }
    }

    @Override
    public void saveState(SerializableState state) {
        List<Long> nodeIds = new ArrayList<>();
        for (Node n : addedNodes) {
            nodeIds.add(n.getOsmId());
        }
        state.putList(NODE_IDS_KEY, nodeIds);
        List<Long> existingNodeIds = new ArrayList<>();
        for (Node n : existingNodes) {
            existingNodeIds.add(n.getOsmId());
        }
        state.putList(EXISTING_NODE_IDS_KEY, existingNodeIds);
        if (createdWay != null) {
            state.putLong(WAY_ID_KEY, createdWay.getOsmId());
        }
        state.putString(TITLE_KEY, mode.getTitle().toString());
        state.putString(SUBTITLE_KEY, mode.getSubtitle().toString());
        state.putInteger(CHECKPOINT_NAME_KEY, checkpointName);
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
