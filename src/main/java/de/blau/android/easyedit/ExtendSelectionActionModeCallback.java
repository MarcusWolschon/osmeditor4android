package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Main.UndoListener;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.TagConflictDialog;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Result;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.search.Search;
import de.blau.android.util.BentleyOttmannForOsm;
import de.blau.android.util.Coordinates;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Snack;
import de.blau.android.util.Sound;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class ExtendSelectionActionModeCallback extends EasyEditActionModeCallback {
    private static final String DEBUG_TAG = "ExtendSelectionAct...";

    private static final int MENUITEM_MERGE                = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 1;
    private static final int MENUITEM_RELATION             = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 2;
    private static final int MENUITEM_ADD_RELATION_MEMBERS = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 3;
    private static final int MENUITEM_ORTHOGONALIZE        = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 4;
    private static final int MENUITEM_INTERSECT            = ElementSelectionActionModeCallback.LAST_REGULAR_MENUITEM + 5;
    private static final int MENUITEM_UPLOAD               = 31;
    private static final int MENUITEM_ZOOM_TO_SELECTION    = 34;
    private static final int MENUITEM_SEARCH_OBJECTS       = 35;

    private List<OsmElement> selection;
    private List<OsmElement> sortedWays;

    UndoListener undoListener;

    private boolean deselect = true;

    private MenuItem undoItem;
    private MenuItem mergeItem;
    private MenuItem orthogonalizeItem;
    private MenuItem uploadItem;
    private MenuItem intersectItem;

    /**
     * Construct an Multi-Select actionmode from a List of OsmElements
     * 
     * @param manager the current EasEditManager instance
     * @param elements the List of OsmElements
     */
    public ExtendSelectionActionModeCallback(@NonNull EasyEditManager manager, @NonNull List<OsmElement> elements) {
        super(manager);
        selection = new ArrayList<>();
        for (OsmElement e : elements) {
            if (e != null) {
                addOrRemoveElement(e);
            }
        }
        undoListener = main.new UndoListener();
    }

    /**
     * Construct an Multi-Select actionmode from a single OsmElement
     * 
     * @param manager the current EasEditManager instance
     * @param element the OsmElement
     */
    public ExtendSelectionActionModeCallback(@NonNull EasyEditManager manager, @Nullable OsmElement element) {
        super(manager);
        Log.d(DEBUG_TAG, "Multi-Select create mode with " + element);
        selection = new ArrayList<>();
        if (element != null) {
            addOrRemoveElement(element);
        }
        undoListener = main.new UndoListener();
    }

    /**
     * Add or remove objects from the selection
     * 
     * @param element object to add or remove
     */
    private void addOrRemoveElement(OsmElement element) {
        if (!selection.contains(element)) {
            selection.add(element);
            if (element.getName().equals(Way.NAME)) {
                logic.addSelectedWay((Way) element);
            } else if (element.getName().equals(Node.NAME)) {
                logic.addSelectedNode((Node) element);
            } else if (element.getName().equals(Relation.NAME)) {
                logic.addSelectedRelation((Relation) element);
            }
        } else {
            selection.remove(element);
            if (element.getName().equals(Way.NAME)) {
                logic.removeSelectedWay((Way) element);
            } else if (element.getName().equals(Node.NAME)) {
                logic.removeSelectedNode((Node) element);
            } else if (element.getName().equals(Relation.NAME)) {
                logic.removeSelectedRelation((Relation) element);
            }
        }
        if (selection.isEmpty()) {
            // nothing selected more .... stop
            manager.finish();
        } else {
            sortedWays = Util.sortWays(selection);
            manager.invalidate();
        }
        setSubTitle(mode);
        main.invalidateMap();
    }

    /**
     * Set aselected object count in the action mode subtitle
     * 
     * @param mode the ActionMode
     */
    private void setSubTitle(@Nullable ActionMode mode) {
        if (mode != null) {
            int count = selection.size();
            mode.setSubtitle(main.getResources().getQuantityString(R.plurals.actionmode_object_count, count, count));
        }
    }

    @Override
    public boolean onCreateActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
        helpTopic = R.string.help_multiselect;
        mode.setTitle(R.string.actionmode_multiselect);
        setSubTitle(mode);
        super.onCreateActionMode(mode, menu);
        logic.setReturnRelations(true); // can add relations

        // setup menus
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        main.getMenuInflater().inflate(R.menu.undo_action, menu);
        undoItem = menu.findItem(R.id.undo_action);

        View undoView = undoItem.getActionView();
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);

        menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_TAG, Menu.NONE, R.string.menu_tags)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_tags));
        menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_delete));
        if (!selectionContainsRelation()) {
            menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_copy));
            menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_cut));
        }

        mergeItem = menu.add(Menu.NONE, MENUITEM_MERGE, Menu.NONE, R.string.menu_merge).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));

        menu.add(Menu.NONE, MENUITEM_RELATION, Menu.CATEGORY_SYSTEM, R.string.menu_relation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation));

        menu.add(Menu.NONE, MENUITEM_ADD_RELATION_MEMBERS, Menu.CATEGORY_SYSTEM, R.string.tag_menu_addtorelation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation_add_member));

        orthogonalizeItem = menu.add(Menu.NONE, MENUITEM_ORTHOGONALIZE, Menu.NONE, R.string.menu_orthogonalize)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_ortho));

        intersectItem = menu.add(Menu.NONE, MENUITEM_INTERSECT, Menu.NONE, R.string.menu_add_node_at_intersection);

        menu.add(GROUP_BASE, MENUITEM_ZOOM_TO_SELECTION, Menu.CATEGORY_SYSTEM | 10, R.string.menu_zoom_to_selection);
        menu.add(GROUP_BASE, MENUITEM_SEARCH_OBJECTS, Menu.CATEGORY_SYSTEM | 10, R.string.search_objects_title);

        uploadItem = menu.add(GROUP_BASE, MENUITEM_UPLOAD, Menu.CATEGORY_SYSTEM | 10, R.string.menu_upload_elements);

        menu.add(GROUP_BASE, ElementSelectionActionModeCallback.MENUITEM_PREFERENCES, Menu.CATEGORY_SYSTEM | 10, R.string.menu_config)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_config));
        menu.add(GROUP_BASE, ElementSelectionActionModeCallback.MENUITEM_JS_CONSOLE, Menu.CATEGORY_SYSTEM | 10, R.string.tag_menu_js_console)
                .setEnabled(logic.getPrefs().isJsConsoleEnabled());
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onPrepareActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
        menu = replaceMenu(menu, mode, this);
        boolean updated = super.onPrepareActionMode(mode, menu);
        if (logic.getUndo().canUndo() || logic.getUndo().canRedo()) {
            if (!undoItem.isVisible()) {
                undoItem.setVisible(true);
                updated = true;
            }
        } else if (undoItem.isVisible()) {
            undoItem.setVisible(false);
            updated = true;
        }
        final boolean canMergePolygons = canMergePolygons(selection);
        updated |= ElementSelectionActionModeCallback
                .setItemVisibility((sortedWays != null && !canMergePolygons) || (selection.size() == 2 && canMergePolygons), mergeItem, false);

        List<Way> selectedWays = logic.getSelectedWays();
        updated |= ElementSelectionActionModeCallback.setItemVisibility(selectedWays != null && !selectedWays.isEmpty(), orthogonalizeItem, false);

        updated |= ElementSelectionActionModeCallback.setItemVisibility(intersect(selectedWays), intersectItem, false);

        boolean changedElementsSelected = false;
        for (OsmElement e : selection) {
            if (!e.isUnchanged()) {
                changedElementsSelected = true;
                break;
            }
        }
        updated |= ElementSelectionActionModeCallback.setItemVisibility(changedElementsSelected, uploadItem, true);

        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
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

    @Override
    public boolean elementsOnly() {
        return true;
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
        if (!super.onActionItemClicked(mode, item)) {
            switch (item.getItemId()) {

            case ElementSelectionActionModeCallback.MENUITEM_TAG:
                main.performTagEdit(selection, false, false);
                break;
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
                }, -1, R.string.select_relation_title, null, null).show();
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
            case MENUITEM_ZOOM_TO_SELECTION:
                main.zoomTo(selection);
                main.invalidateMap();
                break;
            case MENUITEM_SEARCH_OBJECTS:
                Search.search(main);
                break;
            case MENUITEM_UPLOAD:
                main.descheduleAutoLock();
                main.confirmUpload(ElementSelectionActionModeCallback.addRequiredElements(main, new ArrayList<>(selection)));
                break;
            case ElementSelectionActionModeCallback.MENUITEM_PREFERENCES:
                PrefEditor.start(main);
                break;
            case ElementSelectionActionModeCallback.MENUITEM_JS_CONSOLE:
                Main.showJsConsole(main);
                break;
            case R.id.undo_action:
                // should not happen
                Log.d(DEBUG_TAG, "menu undo clicked");
                undoListener.onClick(null);
                break;
            default:
                return false;
            }
        }
        return true;
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
                int height = map.getHeight();
                float y = GeoMath.latMercatorToY(height, width, logic.getViewBox(), intersections.get(0).y);
                Node node = logic.performAddOnWay(main, ways, x, y, false);
                if (node != null) {
                    List<Way> waysWithNode = logic.getWaysForNode(node);
                    selection.removeAll(waysWithNode);
                    logic.performJoinNodeToWays(main, selection, node);
                    main.zoomTo(node);
                    main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, node));
                } else {
                    Snack.toastTopError(main, R.string.toast_no_intersection_found);
                }
            }
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
                TagConflictDialog.showDialog(main, result);
            }
        } catch (OsmIllegalOperationException | IllegalStateException e) {
            Snack.barError(main, e.getLocalizedMessage());
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
                TagConflictDialog.showDialog(main, result);
            }
        } catch (OsmIllegalOperationException | IllegalStateException e) {
            Snack.barError(main, e.getLocalizedMessage());
        }
    }

    @Override
    public boolean handleElementClick(OsmElement element) {
        // due to clickableElements, only valid elements can be clicked
        Log.d(DEBUG_TAG, "Multi-Select add/remove " + element);
        addOrRemoveElement(element);
        main.invalidateMap();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Log.d(DEBUG_TAG, "onDestroyActionMode deselect " + deselect);
        super.onDestroyActionMode(mode);
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        if (deselect) {
            logic.deselectAll();
            main.invalidateMap();
        }
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
    public boolean onBackPressed() {
        Log.d(DEBUG_TAG, "onBackPressed");
        deselect = true;
        return super.onBackPressed(); // call the normal stuff
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
        } else if (c == Util.getShortCut(main, R.string.shortcut_tagedit)) {
            main.performTagEdit(selection, false, false);
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
