package de.blau.android.easyedit;

import java.util.Set;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.easyedit.turnrestriction.FromElementActionModeCallback;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class WaySelectionActionModeCallback extends ElementSelectionActionModeCallback {
    private static final String DEBUG_TAG              = "WaySelectionAction...";
    private static final int    MENUITEM_SPLIT         = 10;
    private static final int    MENUITEM_MERGE         = 11;
    private static final int    MENUITEM_REVERSE       = 12;
    private static final int    MENUITEM_APPEND        = 13;
    private static final int    MENUITEM_RESTRICTION   = 14;
    private static final int    MENUITEM_ROTATE        = 15;
    private static final int    MENUITEM_ORTHOGONALIZE = 16;
    private static final int    MENUITEM_CIRCULIZE     = 17;
    private static final int    MENUITEM_SPLIT_POLYGON = 18;
    private static final int    MENUITEM_ADDRESS       = 19;

    private Set<OsmElement> cachedMergeableWays;
    private Set<OsmElement> cachedAppendableNodes;
    private Set<OsmElement> cachedViaElements;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param way the selected Way
     */
    WaySelectionActionModeCallback(EasyEditManager manager, Way way) {
        super(manager, way);
        Log.d(DEBUG_TAG, "constructor");
        findConnectedWays(way);
    }

    /**
     * Find the Ways in different categories that are connected to this Way
     * 
     * @param way the selected Way
     */
    private void findConnectedWays(@NonNull Way way) {
        cachedMergeableWays = findMergeableWays(way);
        cachedAppendableNodes = findAppendableNodes(way);
        cachedViaElements = findViaElements(way);
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
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onPrepareActionMode");
        if (((Way) element).getTags().containsKey(Tags.KEY_BUILDING) && !((Way) element).getTags().containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
            menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_address));
        }
        menu.add(Menu.NONE, MENUITEM_REVERSE, Menu.NONE, R.string.menu_reverse).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_reverse));
        if (((Way) element).getNodes().size() > 2) {
            menu.add(Menu.NONE, MENUITEM_SPLIT, Menu.NONE, R.string.menu_split).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_split));
        }
        if (!cachedMergeableWays.isEmpty()) {
            menu.add(Menu.NONE, MENUITEM_MERGE, Menu.NONE, R.string.menu_merge).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));
        }
        if (!cachedAppendableNodes.isEmpty()) {
            menu.add(Menu.NONE, MENUITEM_APPEND, Menu.NONE, R.string.menu_append).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_append));
        }
        if (((Way) element).getTagWithKey(Tags.KEY_HIGHWAY) != null && !cachedViaElements.isEmpty()) {
            menu.add(Menu.NONE, MENUITEM_RESTRICTION, Menu.NONE, R.string.actionmode_restriction)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_add_restriction));
        }
        if (((Way) element).getNodes().size() > 2) {
            menu.add(Menu.NONE, MENUITEM_ORTHOGONALIZE, Menu.NONE, R.string.menu_orthogonalize)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_ortho));
        }
        menu.add(Menu.NONE, MENUITEM_ROTATE, Menu.NONE, R.string.menu_rotate).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_rotate));
        if (((Way) element).getNodes().size() > 3 && ((Way) element).isClosed()) {
            menu.add(Menu.NONE, MENUITEM_CIRCULIZE, Menu.NONE, R.string.menu_circulize);
            if (((Way) element).getNodes().size() > 4) { // 5 nodes is the minimum required to be able to split in
                                                         // to two polygons
                menu.add(Menu.NONE, MENUITEM_SPLIT_POLYGON, Menu.NONE, R.string.menu_split_polygon);
            }
        }
        arrangeMenu(menu);
        return true;
    }

    /**
     * Reverse a way showing a confirmation dialog if it is not reversible without changing semantics
     */
    private void reverseWay() {
        final Way way = (Way) element;
        if (way.notReversable()) {
            new AlertDialog.Builder(main).setTitle(R.string.menu_reverse).setMessage(R.string.notreversable_description)
                    .setPositiveButton(R.string.reverse_anyway, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (logic.performReverse(main, way)) { // true if it had oneway tag
                                Snack.barWarning(main, R.string.toast_oneway_reversed);
                                main.performTagEdit(way, null, false, false, false);
                            }
                        }
                    }).show();
        } else if (logic.performReverse(main, way)) { // true if it had oneway tag
            Snack.barWarning(main, R.string.toast_oneway_reversed);
            main.performTagEdit(way, null, false, false, false);
        } else {
            manager.invalidate(); // sucessful reversal update menubar
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            switch (item.getItemId()) {
            case MENUITEM_SPLIT:
                main.startSupportActionMode(new WaySplittingActionModeCallback(manager, (Way) element, false));
                break;
            case MENUITEM_MERGE:
                main.startSupportActionMode(new WayMergingActionModeCallback(manager, (Way) element, cachedMergeableWays));
                break;
            case MENUITEM_REVERSE:
                reverseWay();
                findConnectedWays((Way) element);
                break;
            case MENUITEM_APPEND:
                main.startSupportActionMode(new WayAppendingActionModeCallback(manager, (Way) element, cachedAppendableNodes));
                break;
            case MENUITEM_RESTRICTION:
                main.startSupportActionMode(new FromElementActionModeCallback(manager, (Way) element, cachedViaElements));
                break;
            case MENUITEM_ROTATE:
                deselect = false;
                main.startSupportActionMode(new WayRotationActionModeCallback(manager, (Way) element));
                break;
            case MENUITEM_ORTHOGONALIZE:
                logic.performOrthogonalize(main, (Way) element);
                manager.invalidate();
                break; // FIXME move to asynctask
            case MENUITEM_CIRCULIZE:
                logic.performCirculize(main, (Way) element);
                manager.invalidate();
                break;
            case MENUITEM_SPLIT_POLYGON:
                main.startSupportActionMode(new WaySplittingActionModeCallback(manager, (Way) element, true));
                break;
            case MENUITEM_ADDRESS:
                main.performTagEdit(element, null, true, false, false);
                break;
            case MENUITEM_SHARE_POSITION:
                Util.sharePosition(main, Logic.centroidLonLat((Way) element), main.getMap().getZoomLevel());
                break;
            default:
                return false;
            }
        }
        return true;
    }

    @Override
    protected void menuDelete(final ActionMode mode) {
        boolean isRelationMember = element.hasParentRelations();
        boolean allNodesDownloaded = logic.isInDownload((Way) element);
        if (allNodesDownloaded) {
            if (isRelationMember) {
                new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deleteway_relation_description)
                        .setPositiveButton(R.string.deleteway_wayandnodes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteWay(mode);
                            }
                        }).setNeutralButton(R.string.cancel, null).show();
            } else {
                deleteWay(mode);
            }
        } else {
            new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deleteway_nodesnotdownloaded_description)
                    .setPositiveButton(R.string.okay, null).show();
        }
    }

    /**
     * Delete way including orphan nodes and finish this mode
     * 
     * @param mode the current ActionMode
     */
    private void deleteWay(final ActionMode mode) {
        logic.performEraseWay(main, (Way) element, true, true);
        if (mode != null) {
            mode.finish();
        }
    }

    @Override
    protected void update() {
        findConnectedWays((Way) element);
    }
}
