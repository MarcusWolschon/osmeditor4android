package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.DisambiguationMenu;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.DisambiguationMenu.Type;
import de.blau.android.dialogs.ElementIssueDialog;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Result;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.BentleyOttmannForOsm;
import de.blau.android.util.Coordinates;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Sound;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class MultiSelectWithGeometryActionModeCallback extends MultiSelectActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MultiSelectWithGeometryActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = MultiSelectWithGeometryActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_MERGE                = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 1;
    private static final int MENUITEM_RELATION             = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 2;
    private static final int MENUITEM_ADD_RELATION_MEMBERS = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 3;
    private static final int MENUITEM_ORTHOGONALIZE        = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 4;
    private static final int MENUITEM_INTERSECT            = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 5;
    private static final int MENUITEM_CREATE_CIRCLE        = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 6;
    private static final int MENUITEM_ROTATE               = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 7;
    private static final int MENUITEM_EXTRACT_SEGMENT      = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 8;

    private MenuItem mergeItem;
    private MenuItem orthogonalizeItem;
    private MenuItem intersectItem;
    private MenuItem createCircleItem;
    private MenuItem extractSegmentItem;

    /**
     * Construct an Multi-Select actionmode from a List of OsmElements
     * 
     * @param manager the current EasEditManager instance
     * @param elements the List of OsmElements
     */
    public MultiSelectWithGeometryActionModeCallback(@NonNull EasyEditManager manager, @NonNull List<OsmElement> elements) {
        super(manager, elements);
    }

    /**
     * Construct an Multi-Select actionmode from a single OsmElement
     * 
     * @param manager the current EasEditManager instance
     * @param element the OsmElement
     */
    public MultiSelectWithGeometryActionModeCallback(@NonNull EasyEditManager manager, @Nullable OsmElement element) {
        super(manager, element);
    }

    @Override
    public boolean onCreateActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
        super.onCreateActionMode(mode, menu);
        menu = replaceMenu(menu, mode, this);
        menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_delete));
        if (!selectionContainsRelation()) {
            menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_copy));
            menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_cut));
        }

        mergeItem = menu.add(Menu.NONE, MENUITEM_MERGE, Menu.NONE, R.string.menu_merge).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));

        extractSegmentItem = menu.add(Menu.NONE, MENUITEM_EXTRACT_SEGMENT, Menu.NONE, R.string.menu_extract_segment);

        menu.add(Menu.NONE, MENUITEM_RELATION, Menu.CATEGORY_SYSTEM, R.string.menu_relation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation));

        menu.add(Menu.NONE, MENUITEM_ADD_RELATION_MEMBERS, Menu.CATEGORY_SYSTEM, R.string.tag_menu_addtorelation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation_add_member));

        orthogonalizeItem = menu.add(Menu.NONE, MENUITEM_ORTHOGONALIZE, Menu.NONE, R.string.menu_orthogonalize)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_ortho));

        intersectItem = menu.add(Menu.NONE, MENUITEM_INTERSECT, Menu.NONE, R.string.menu_add_node_at_intersection);

        createCircleItem = menu.add(Menu.NONE, MENUITEM_CREATE_CIRCLE, Menu.NONE, R.string.menu_create_circle);

        menu.add(Menu.NONE, MENUITEM_ROTATE, Menu.NONE, R.string.menu_rotate).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_rotate));

        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onPrepareActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
        boolean updated = super.onPrepareActionMode(mode, menu);

        final boolean canMergePolygons = canMergePolygons(selection);
        int count = selection.size();
        updated |= ElementSelectionActionModeCallback
                .setItemVisibility((count > 1 && sortedWays != null && !canMergePolygons) || (count == 2 && canMergePolygons), mergeItem, false);

        List<Way> selectedWays = logic.getSelectedWays();
        updated |= ElementSelectionActionModeCallback.setItemVisibility(selectedWays != null && !selectedWays.isEmpty(), orthogonalizeItem, false);

        updated |= ElementSelectionActionModeCallback.setItemVisibility(intersect(selectedWays), intersectItem, false);

        updated |= ElementSelectionActionModeCallback.setItemVisibility(countType(ElementType.NODE) >= StorageDelegator.MIN_NODES_CIRCLE, createCircleItem,
                false);

        if (selection.size() == 2 && selection.get(0) instanceof Node && selection.get(1) instanceof Node) {
            List<Way> commonWays = getWaysForNodes((Node) selection.get(0), (Node) selection.get(1));
            updated |= ElementSelectionActionModeCallback.setItemVisibility(!commonWays.isEmpty(), extractSegmentItem, true);
        }

        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
    }

    /**
     * Count elements of a certain type
     * 
     * @param type the ElementType we are looking for
     * @return the count
     */
    private int countType(@NonNull ElementType type) {
        int result = 0;
        for (OsmElement e : selection) {
            if (e != null && e.getType() == type) {
                result++;
            }
        }
        return result;
    }

    /**
     * Check if a Relation is in the selection
     * 
     * @return true if a Relation is selected
     */
    private boolean selectionContainsRelation() {
        for (OsmElement e : selection) {
            if (e instanceof Relation) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the current selection are closed ways
     * 
     * @param selection the current selection
     * @return true if they are all closed ways
     */
    private boolean canMergePolygons(@NonNull List<OsmElement> selection) {
        for (OsmElement e : selection) {
            if (!(Way.NAME.equals(e.getName()) && ((Way) e).isClosed())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (super.onActionItemClicked(mode, item)) {
            return true;
        }
        switch (item.getItemId()) {
        case ElementSelectionActionModeCallback.MENUITEM_DELETE:
            menuDelete(false);
            break;
        case ElementSelectionActionModeCallback.MENUITEM_COPY:
            logic.copyToClipboard(selection);
            mode.finish();
            break;
        case ElementSelectionActionModeCallback.MENUITEM_CUT:
            logic.cutToClipboard(main, selection);
            mode.finish();
            break;
        case MENUITEM_RELATION:
            ElementSelectionActionModeCallback.buildPresetSelectDialog(main,
                    p -> main.startSupportActionMode(new EditRelationMembersActionModeCallback(manager,
                            p != null ? p.getPath(App.getCurrentRootPreset(main).getRootGroup()) : null, selection)),
                    ElementType.RELATION, R.string.select_relation_type_title, Tags.KEY_TYPE, null).show();
            break;
        case MENUITEM_ADD_RELATION_MEMBERS:
            ElementSelectionActionModeCallback.buildRelationSelectDialog(main, r -> {
                Relation relation = (Relation) App.getDelegator().getOsmElement(Relation.NAME, r);
                if (relation != null) {
                    main.startSupportActionMode(new EditRelationMembersActionModeCallback(manager, relation, selection));
                }
            }, -1, R.string.select_relation_title, null, null, selection).show();
            break;
        case MENUITEM_ORTHOGONALIZE:
            orthogonalizeWays();
            break;
        case MENUITEM_MERGE:
            if (canMergePolygons(selection)) {
                mergePolygons();
            } else {
                mergeWays();
            }
            break;
        case MENUITEM_INTERSECT:
            intersectWays();
            break;
        case MENUITEM_CREATE_CIRCLE:
            createCircle();
            break;
        case MENUITEM_ROTATE:
            deselectOnExit = false;
            main.startSupportActionMode(new RotationActionModeCallback(manager));
            break;
        case MENUITEM_EXTRACT_SEGMENT:
            extractSegment();
            break;
        default:
            return false;
        }
        return true;
    }

    /**
     * Extract a segment from way(s) between two nodes
     */
    private void extractSegment() {
        if (selection.size() == 2 && selection.get(0) instanceof Node && selection.get(1) instanceof Node) {
            final Node node1 = (Node) selection.get(0);
            final Node node2 = (Node) selection.get(1);
            List<Way> commonWays = getWaysForNodes(node1, node2);
            if (!commonWays.isEmpty()) {
                if (commonWays.size() == 1) {
                    splitSafe(commonWays, extractSegment(commonWays, node1, node2));
                } else {
                    DisambiguationMenu menu = new DisambiguationMenu(main.getMap());
                    menu.setHeaderTitle(R.string.select_way_to_extract_from);
                    int id = 0;
                    menu.add(id, Type.WAY, main.getString(R.string.split_all_ways),
                            (int position) -> splitSafe(commonWays, extractSegment(commonWays, node1, node2)));
                    id++;
                    for (Way w : commonWays) {
                        menu.add(id, Type.WAY, w.getDescription(main),
                                (int position) -> splitSafe(Util.wrapInList(w), extractSegment(Util.wrapInList(w), node1, node2)));
                        id++;
                    }
                    menu.show();
                }
                return;
            }
        }
        Log.e(DEBUG_TAG, "extractSegment called but selection is invalid");

    }

    /**
     * Check if the current selection are ways that can be intersected
     * 
     * @param ways the current selected ways
     * @return true if the ways intersect
     */
    private boolean intersect(@Nullable List<Way> ways) {
        if (ways != null && !ways.isEmpty()) {
            List<Coordinates> intersections = BentleyOttmannForOsm.findIntersections(ways);
            return !intersections.isEmpty();
        }
        return false;
    }

    /**
     * Add a node at the 1st intersection we find
     */
    private void intersectWays() {
        List<Way> ways = logic.getSelectedWays();
        if (ways != null && !ways.isEmpty()) {
            List<Coordinates> intersections = BentleyOttmannForOsm.findIntersections(ways);
            if (!intersections.isEmpty()) {
                Map map = logic.getMap();
                int width = map.getWidth();
                float x = GeoMath.lonToX(width, logic.getViewBox(), intersections.get(0).x);
                float y = GeoMath.latMercatorToY(map.getHeight(), width, logic.getViewBox(), intersections.get(0).y);
                final Node node = logic.performAddOnWay(main, ways, x, y, true);
                if (node != null) {
                    logic.getHandler().post(() -> {
                        List<Way> waysWithNode = logic.getWaysForNode(node);
                        selection.removeAll(waysWithNode);
                        logic.performJoinNodeToWays(main, selection, node);
                        main.zoomTo(node);
                        manager.finish();
                        manager.editElement(node);
                    });
                } else {
                    ScreenMessage.toastTopError(main, R.string.toast_no_intersection_found);
                }
            }
        }
    }

    /**
     * Create a circle from selected nodes
     */
    private void createCircle() {
        try {
            logic.getHandler().post(() -> {
                Way circle = logic.createCircle(main, logic.getSelectedNodes());
                manager.finish();
                manager.editElement(circle);
                main.performTagEdit(circle, null, false, true);
            });
        } catch (OsmIllegalOperationException | IllegalStateException e) {
            ScreenMessage.barError(main, e.getLocalizedMessage());
        }
    }

    /**
     * Orthogonalize any selected Ways
     */
    private void orthogonalizeWays() {
        List<Way> selectedWays = logic.getSelectedWays();
        if (selectedWays != null && !selectedWays.isEmpty()) {
            logic.performOrthogonalize(main, selectedWays);
        }
    }

    /**
     * Actually merge the ways
     */
    void mergeWays() {
        try {
            List<Result> result = logic.performMerge(main, sortedWays);
            final Result r = result.get(0);
            main.startSupportActionMode(new WaySelectionActionModeCallback(manager, (Way) r.getElement()));
            if (result.size() > 1 || r.hasIssue()) {
                ElementIssueDialog.showTagConflictDialog(main, result);
            }
        } catch (OsmIllegalOperationException | IllegalStateException e) {
            ScreenMessage.barError(main, e.getLocalizedMessage());
        }
    }

    /**
     * Merge closed ways
     */
    private void mergePolygons() {
        try {
            List<Result> result = logic.performPolygonMerge(main, logic.getSelectedWays());
            final Result r = result.get(0);
            OsmElement e = r.getElement();
            if (e instanceof Way) {
                logic.setSelectedWay((Way) e);
                main.startSupportActionMode(new WaySelectionActionModeCallback(manager, (Way) e));
            } else if (e instanceof Relation) {
                logic.setSelectedRelation((Relation) e);
                main.startSupportActionMode(new RelationSelectionActionModeCallback(manager, (Relation) e));
            }
            if (result.size() > 1 || r.hasIssue()) {
                ElementIssueDialog.showTagConflictDialog(main, result);
            }
        } catch (OsmIllegalOperationException | IllegalStateException e) {
            ScreenMessage.barError(main, e.getLocalizedMessage());
        }
    }

    /**
     * Get a list of all the Ways common to the two given Nodes.
     * 
     * @param node1 the 1st Node
     * @param node2 the 2nd Node
     * @return A list of all Ways connected to both Nodes
     */
    @NonNull
    public List<Way> getWaysForNodes(@NonNull final Node node1, @NonNull final Node node2) {
        List<Way> result = new ArrayList<>();
        final Storage currentStorage = App.getDelegator().getCurrentStorage();
        List<Way> ways1 = currentStorage.getWays(node1);
        List<Way> ways2 = currentStorage.getWays(node2);
        if (ways1.size() < ways2.size()) {
            List<Way> temp = ways2;
            ways2 = ways1;
            ways1 = temp;
        }
        for (Way w : ways1) {
            if (ways2.contains(w)) {
                result.add(w);
            }
        }
        return result;
    }

    /**
     * Delete action
     * 
     * @param deleteFromRelations if true the elements will be deleted without regards for their Relation membership
     */
    private void menuDelete(boolean deleteFromRelations) {
        Log.d(DEBUG_TAG, "menuDelete " + deleteFromRelations + " " + selection);

        // check ways are actually downloaded this will abort so should be before the relation check
        for (OsmElement e : selection) {
            if (e instanceof Way && !logic.isInDownload((Way) e)) {
                new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deleteways_nodesnotdownloaded_description)
                        .setPositiveButton(R.string.okay, null).show();
                return;
            }
        }

        // check for relation membership
        if (!deleteFromRelations) {
            for (OsmElement e : selection) {
                if (e.hasParentRelations()) {
                    new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.delete_elements_from_relation_description)
                            .setPositiveButton(R.string.delete, (dialog, which) -> menuDelete(true)).show();
                    return;
                }
            }
        }

        List<Relation> origParents = new ArrayList<>();
        for (OsmElement e : selection) {
            List<Relation> temp = e.getParentRelations();
            if (temp != null) {
                origParents.addAll(temp);
            }
        }
        logic.performEraseMultipleObjects(main, selection);

        // check for new empty relations
        ElementSelectionActionModeCallback.checkEmptyRelations(main, origParents);
        manager.finish();
    }

    @Override
    public boolean processShortcut(Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_copy)) {
            logic.copyToClipboard(selection);
            manager.finish();
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_cut)) {
            logic.cutToClipboard(main, selection);
            manager.finish();
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_undo)) {
            undoListener.onClick(null);
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_merge)) {
            if (sortedWays != null) {
                mergeWays();
            } else {
                Sound.beep();
            }
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_square)) {
            orthogonalizeWays();
            return true;
        }
        return super.processShortcut(c);
    }
}
