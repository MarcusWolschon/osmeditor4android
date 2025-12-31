package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.ElementIssueDialog;
import de.blau.android.easyedit.route.RouteSegmentActionModeCallback;
import de.blau.android.easyedit.turnrestriction.FromElementActionModeCallback;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Result;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.keyboard.Shortcuts;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.Geometry;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class WaySelectionActionModeCallback extends ElementSelectionActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WaySelectionActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = WaySelectionActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_SPLIT             = LAST_REGULAR_MENUITEM + 1;
    private static final int MENUITEM_MERGE             = LAST_REGULAR_MENUITEM + 2;
    private static final int MENUITEM_REVERSE           = LAST_REGULAR_MENUITEM + 3;
    private static final int MENUITEM_APPEND            = LAST_REGULAR_MENUITEM + 4;
    private static final int MENUITEM_RESTRICTION       = LAST_REGULAR_MENUITEM + 5;
    private static final int MENUITEM_ROUTE             = LAST_REGULAR_MENUITEM + 6;
    private static final int MENUITEM_ADD_TO_ROUTE      = LAST_REGULAR_MENUITEM + 7;
    private static final int MENUITEM_ROTATE            = LAST_REGULAR_MENUITEM + 8;
    private static final int MENUITEM_ORTHOGONALIZE     = LAST_REGULAR_MENUITEM + 9;
    private static final int MENUITEM_CIRCULIZE         = LAST_REGULAR_MENUITEM + 10;
    private static final int MENUITEM_SPLIT_POLYGON     = LAST_REGULAR_MENUITEM + 11;
    private static final int MENUITEM_ADDRESS           = LAST_REGULAR_MENUITEM + 12;
    private static final int MENUITEM_UNJOIN            = LAST_REGULAR_MENUITEM + 13;
    private static final int MENUITEM_UNJOIN_DISSIMILAR = LAST_REGULAR_MENUITEM + 14;
    private static final int MENUITEM_REMOVE_NODE       = LAST_REGULAR_MENUITEM + 15;
    private static final int MENUITEM_EXTRACT_SEGMENT   = LAST_REGULAR_MENUITEM + 16;
    private static final int MENUITEM_SELECT_WAY_NODES  = LAST_REGULAR_MENUITEM + 17;
    private static final int MENUITEM_START_END_OF_WAY  = LAST_REGULAR_MENUITEM + 18;

    private Set<OsmElement> cachedMergeableWays;
    private Set<OsmElement> cachedAppendableNodes;
    private Set<OsmElement> cachedViaElements;
    private MenuItem        addressItem;
    private MenuItem        splitItem;
    private MenuItem        mergeItem;
    private MenuItem        appendItem;
    private MenuItem        restrictionItem;
    private MenuItem        orthogonalizeItem;
    private MenuItem        circulizeItem;
    private MenuItem        splitPolygonItem;
    private MenuItem        removeNodeItem;
    private MenuItem        unjoinItem;
    private MenuItem        unjoinDissimilarItem;
    private MenuItem        extractSegmentItem;
    private String          orthogonalizeTitle;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param way the selected Way
     */
    WaySelectionActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way) {
        super(manager, way);
        Log.d(DEBUG_TAG, "constructor");
        findConnectedWays(way);

        actionMap.put(main.getString(R.string.ACTION_SQUARE), new Shortcuts.Action(R.string.action_square, () -> {
            logic.performOrthogonalize(main, (Way) element);
            manager.invalidate();
        }));
    }

    /**
     * Find the Ways in different categories that are connected to this Way
     * 
     * @param way the selected Way
     */
    private void findConnectedWays(@NonNull Way way) {
        if (way.nodeCount() == 0) {
            final String errorMsg = main.getString(R.string.toast_degenerate_way_with_info, way.getDescription(main));
            ScreenMessage.toastTopError(main, errorMsg, true);
            manager.finish();
            // on prepare action mode might run any way, so protect against crash
            cachedMergeableWays = new HashSet<>();
            cachedAppendableNodes = new HashSet<>();
            cachedViaElements = new HashSet<>();
            ACRAHelper.nocrashReport(null, errorMsg);
            return;
        }
        cachedMergeableWays = findMergeableWays(way);
        cachedAppendableNodes = findAppendableNodes(way);
        cachedViaElements = findViaElements(way, true);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_wayselection;
        super.onCreateActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onCreateActionMode");
        logic.setSelectedWay((Way) element);
        main.invalidateMap();
        mode.setTitle(R.string.actionmode_wayselect);
        mode.setSubtitle(null);

        menu = replaceMenu(menu, mode, this);

        addressItem = menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_address));

        menu.add(Menu.NONE, MENUITEM_REVERSE, Menu.NONE, R.string.menu_reverse).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_reverse));

        splitItem = menu.add(Menu.NONE, MENUITEM_SPLIT, Menu.NONE, R.string.menu_split).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_split));

        mergeItem = menu.add(Menu.NONE, MENUITEM_MERGE, Menu.NONE, R.string.menu_merge).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));

        appendItem = menu.add(Menu.NONE, MENUITEM_APPEND, Menu.NONE, R.string.menu_append).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_append));

        restrictionItem = menu.add(Menu.NONE, MENUITEM_RESTRICTION, Menu.NONE, R.string.actionmode_restriction)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_add_restriction));

        menu.add(Menu.NONE, MENUITEM_ROUTE, Menu.NONE, R.string.menu_create_route);
        menu.add(Menu.NONE, MENUITEM_ADD_TO_ROUTE, Menu.NONE, R.string.menu_add_to_route);

        orthogonalizeTitle = main.getString(R.string.menu_orthogonalize);
        orthogonalizeItem = menu.add(Menu.NONE, MENUITEM_ORTHOGONALIZE, Menu.NONE, orthogonalizeTitle)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_ortho));

        menu.add(Menu.NONE, MENUITEM_ROTATE, Menu.NONE, R.string.menu_rotate).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_rotate));

        circulizeItem = menu.add(Menu.NONE, MENUITEM_CIRCULIZE, Menu.NONE, R.string.menu_circulize);

        splitPolygonItem = menu.add(Menu.NONE, MENUITEM_SPLIT_POLYGON, Menu.NONE, R.string.menu_split_polygon);

        removeNodeItem = menu.add(Menu.NONE, MENUITEM_REMOVE_NODE, Menu.NONE, R.string.menu_remove_node_from_way);

        unjoinItem = menu.add(Menu.NONE, MENUITEM_UNJOIN, Menu.NONE, R.string.menu_unjoin);
        unjoinDissimilarItem = menu.add(Menu.NONE, MENUITEM_UNJOIN_DISSIMILAR, Menu.NONE, R.string.menu_unjoin_dissimilar);

        extractSegmentItem = menu.add(Menu.NONE, MENUITEM_EXTRACT_SEGMENT, Menu.NONE, R.string.menu_extract_segment);

        menu.add(Menu.NONE, MENUITEM_SELECT_WAY_NODES, Menu.NONE, R.string.menu_select_way_nodes);

        menu.add(Menu.NONE, MENUITEM_START_END_OF_WAY, Menu.NONE, R.string.menu_start_end_way);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Log.d(DEBUG_TAG, "onPrepareActionMode");
        menu = replaceMenu(menu, mode, this);
        boolean updated = super.onPrepareActionMode(mode, menu);

        Way way = (Way) element;
        int size = way.getNodes().size();
        boolean closed = way.isClosed();

        updated |= setItemVisibility(way.hasTagKey(Tags.KEY_BUILDING) && !way.hasTagKey(Tags.KEY_ADDR_HOUSENUMBER), addressItem, false);

        updated |= setItemVisibility(size > 2, splitItem, false);

        updated |= setItemVisibility(!cachedMergeableWays.isEmpty(), mergeItem, false);

        updated |= setItemVisibility(!(cachedAppendableNodes.isEmpty() || way.nodeCount() >= maxWayNodes), appendItem, false);

        updated |= setItemVisibility(way.getTagWithKey(Tags.KEY_HIGHWAY) != null && !cachedViaElements.isEmpty(), restrictionItem, false);

        updated |= setItemVisibility(size > 2, orthogonalizeItem, false);
        if (orthogonalizeItem.isVisible()) {
            if (closed) {
                if (!orthogonalizeItem.getTitle().equals(orthogonalizeTitle)) {
                    orthogonalizeItem.setTitle(orthogonalizeTitle);
                    updated = true;
                }
            } else {
                if (orthogonalizeItem.getTitle().equals(orthogonalizeTitle)) {
                    orthogonalizeItem.setTitle(R.string.menu_straighten);
                    updated = true;
                }
            }
        }

        updated |= setItemVisibility(size > StorageDelegator.MIN_NODES_CIRCLE && closed, circulizeItem, false);

        // 5 nodes is the minimum required to be able to split in to two polygons
        updated |= setItemVisibility(size > 4 && closed, splitPolygonItem, false);

        updated |= setItemVisibility(size >= 4 || (!closed && size >= 3), removeNodeItem, false);

        boolean joined = isJoined(way);
        updated |= setItemVisibility(joined, unjoinItem, false);
        updated |= setItemVisibility(joined, unjoinDissimilarItem, false);

        updated |= setItemVisibility(size >= 3, extractSegmentItem, false);

        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
    }

    /**
     * Reverse a way displaying any serious issues
     * 
     * @param way the Way
     */
    private void reverseWay(@NonNull final Way way) {
        try {
            List<Result> result = logic.performReverse(main, way);
            if (!result.isEmpty()) {
                ElementIssueDialog.showTagConflictDialog(main, result);
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            // toast has already been displayed
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            try {
                Way way = (Way) element;
                switch (item.getItemId()) {
                case MENUITEM_SPLIT:
                    deselect = false;
                    main.startSupportActionMode(new WaySplittingActionModeCallback(manager, way, false));
                    break;
                case MENUITEM_MERGE:
                    deselect = false;
                    main.startSupportActionMode(new WayMergingActionModeCallback(manager, way, cachedMergeableWays));
                    break;
                case MENUITEM_REVERSE:
                    if (way.notReversable()) {
                        ThemeUtils.getAlertDialogBuilder(main).setTitle(R.string.menu_reverse).setMessage(R.string.notreversable_description)
                                .setPositiveButton(R.string.reverse_anyway, (dialog, which) -> reverseWay(way)).show();
                    } else {
                        reverseWay(way);
                    }
                    manager.invalidate();
                    findConnectedWays(way);
                    break;
                case MENUITEM_APPEND:
                    deselect = false;
                    main.startSupportActionMode(new WayAppendingActionModeCallback(manager, way, cachedAppendableNodes));
                    break;
                case MENUITEM_RESTRICTION:
                    deselect = false;
                    main.startSupportActionMode(new FromElementActionModeCallback(manager, way, cachedViaElements));
                    break;
                case MENUITEM_ROUTE:
                    main.startSupportActionMode(new RouteSegmentActionModeCallback(manager, way, findViaElements(way, false)));
                    break;
                case MENUITEM_ADD_TO_ROUTE:
                    buildRelationSelectDialog(main, r -> {
                        Relation route = (Relation) App.getDelegator().getOsmElement(Relation.NAME, r);
                        if (route != null) {
                            main.startSupportActionMode(new RouteSegmentActionModeCallback(manager, way, route, findViaElements(way, false), null));
                        }
                    }, -1, R.string.select_route_title, Tags.KEY_TYPE, Tags.VALUE_ROUTE, Util.wrapInList(element)).show();
                    break;
                case MENUITEM_ROTATE:
                    deselect = false;
                    main.startSupportActionMode(new RotationActionModeCallback(manager));
                    break;
                case MENUITEM_ORTHOGONALIZE:
                    logic.performOrthogonalize(main, way);
                    manager.invalidate();
                    break;
                case MENUITEM_CIRCULIZE:
                    logic.performCirculize(main, way);
                    manager.invalidate();
                    break;
                case MENUITEM_SPLIT_POLYGON:
                    deselect = false;
                    main.startSupportActionMode(new WaySplittingActionModeCallback(manager, way, true));
                    break;
                case MENUITEM_ADDRESS:
                    main.performTagEdit(element, null, true, false);
                    break;
                case MENUITEM_REMOVE_NODE:
                    deselect = false;
                    main.startSupportActionMode(new RemoveNodeFromWayActionModeCallback(manager, way));
                    break;
                case MENUITEM_UNJOIN:
                    logic.performUnjoinWay(main, way, null);
                    break;
                case MENUITEM_UNJOIN_DISSIMILAR:
                    unjonDissimilar(main, way);
                    break;
                case MENUITEM_SHARE_POSITION:
                    Util.sharePosition(main, Geometry.centroidLonLat(way), main.getMap().getZoomLevel());
                    break;
                case MENUITEM_EXTRACT_SEGMENT:
                    deselect = false;
                    main.startSupportActionMode(new WaySegmentActionModeCallback(manager, way));
                    break;
                case MENUITEM_SELECT_WAY_NODES:
                    logic.deselectAll();
                    deselect = false;
                    main.startSupportActionMode(new MultiSelectWithGeometryActionModeCallback(manager, new ArrayList<>(new HashSet<>(way.getNodes()))));
                    break;
                case MENUITEM_START_END_OF_WAY:
                    ThemeUtils.getAlertDialogBuilder(main).setMessage(R.string.start_end_way_description).setPositiveButton(R.string.end, (dialog, which) -> {
                        main.zoomTo(way.getLastNode());
                        main.invalidateMap();
                    }).setNegativeButton(R.string.start, (dialog, which) -> {
                        main.zoomTo(way.getFirstNode());
                        main.invalidateMap();
                    }).show();
                    break;
                default:
                    return false;
                }
            } catch (OsmIllegalOperationException | StorageException ex) {
                // logic will have already toasted
            }
        }
        return true;
    }

    /**
     * Unjoin way from "dissimilar" other ways
     * 
     * @param activity the current Activity
     * @param way the Way
     */
    private void unjonDissimilar(@NonNull FragmentActivity activity, @NonNull Way way) {
        List<String> primaryKeys = way.getObjectKeys(activity);
        final int keyCount = primaryKeys.size();
        if (keyCount <= 1) {
            logic.performUnjoinWay(activity, way, keyCount == 1 ? primaryKeys.get(0) : null);
            return;
        }
        ThemeUtils.getAlertDialogBuilder(activity).setTitle(R.string.select_primary_key)
                .setItems(primaryKeys.toArray(new String[0]), (DialogInterface dialog, int which) -> {
                    try {
                        logic.performUnjoinWay(activity, way, primaryKeys.get(which));
                    } catch (OsmIllegalOperationException | StorageException ex) {
                        // already toasted
                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }

    @Override
    protected void menuDelete(final ActionMode mode) {
        boolean isRelationMember = element.hasParentRelations();
        boolean allNodesDownloaded = logic.isInDownload((Way) element);
        if (allNodesDownloaded) {
            if (isRelationMember) {
                ThemeUtils.getAlertDialogBuilder(main).setTitle(R.string.delete).setMessage(R.string.deleteway_relation_description)
                        .setPositiveButton(R.string.deleteway_wayandnodes, (dialog, which) -> deleteWay(mode)).setNeutralButton(R.string.cancel, null).show();
            } else {
                deleteWay(mode);
            }
        } else {
            ThemeUtils.getAlertDialogBuilder(main).setTitle(R.string.delete).setMessage(R.string.deleteway_nodesnotdownloaded_description)
                    .setPositiveButton(R.string.okay, null).show();
        }
    }

    /**
     * Delete way including orphan nodes and finish this mode
     * 
     * @param mode the current ActionMode
     */
    private void deleteWay(@Nullable final ActionMode mode) {
        List<Relation> origParents = element.hasParentRelations() ? new ArrayList<>(element.getParentRelations()) : null;
        logic.performEraseWay(main, (Way) element, true, true);
        if (mode != null) {
            mode.finish();
        }
        checkEmptyRelations(main, origParents);
    }

    /**
     * Check for at least one Node that is shared with another Way
     * 
     * @param way the way
     * @return true if there is at least one shared node
     */
    private boolean isJoined(@NonNull Way way) {
        for (Node node : way.getNodes()) {
            List<Way> ways = logic.getWaysForNode(node);
            if (ways.size() > 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void update() {
        findConnectedWays((Way) element);
    }
}
