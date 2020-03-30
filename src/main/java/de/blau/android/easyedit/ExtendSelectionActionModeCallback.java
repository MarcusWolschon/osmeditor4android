package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import de.blau.android.Main;
import de.blau.android.Main.UndoListener;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.MergeResult;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.search.Search;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class ExtendSelectionActionModeCallback extends EasyEditActionModeCallback {
    private static final String DEBUG_TAG = "ExtendSelectionAct...";

    private static final int MENUITEM_MERGE             = 6;
    private static final int MENUITEM_RELATION          = 7;
    private static final int MENUITEM_ORTHOGONALIZE     = 8;
    private static final int MENUITEM_MERGE_POLYGONS    = 9;
    private static final int MENUITEM_ZOOM_TO_SELECTION = 33;
    private static final int MENUITEM_SEARCH_OBJECTS    = 34;

    private List<OsmElement> selection;
    private List<OsmElement> sortedWays;

    UndoListener undoListener;

    private boolean deselect = true;

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
    public ExtendSelectionActionModeCallback(@NonNull EasyEditManager manager, @NonNull OsmElement element) {
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
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onPrepareActionMode(@NonNull ActionMode mode, @NonNull Menu menu) {
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        main.getMenuInflater().inflate(R.menu.undo_action, menu);
        MenuItem undo = menu.findItem(R.id.undo_action);
        if (logic.getUndo().canUndo() || logic.getUndo().canRedo()) {
            undo.setVisible(true);
            undo.setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_undo));
        }
        View undoView = MenuItemCompat.getActionView(undo);
        if (undoView == null) { // FIXME this is a temp workaround for pre-11 Android
            Context context = ThemeUtils.getThemedContext(main, R.style.Theme_customMain_Light, R.style.Theme_customMain);
            undoView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.undo_action_view, null);
        }
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);

        menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_TAG, Menu.NONE, R.string.menu_tags)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_tags));
        menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_delete));
        if (!selectionContainsRelation()) {
            menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy)
                    .setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_copy)).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_copy));
            menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut)
                    .setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_cut)).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_cut));
        }
        if (sortedWays != null) {
            menu.add(Menu.NONE, MENUITEM_MERGE, Menu.NONE, R.string.menu_merge).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));
        }
        menu.add(Menu.NONE, MENUITEM_RELATION, Menu.CATEGORY_SYSTEM, R.string.menu_relation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation));

        List<Way> selectedWays = logic.getSelectedWays();
        if (selectedWays != null && !selectedWays.isEmpty()) {
            menu.add(Menu.NONE, MENUITEM_ORTHOGONALIZE, Menu.NONE, R.string.menu_orthogonalize)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_ortho));
        }

        // // for now just two
        // if (selection.size() == 2 && canMerge(selection)) {
        // menu.add(Menu.NONE,MENUITEM_MERGE_POLYGONS, Menu.NONE, "Merge polygons");
        // }
        menu.add(GROUP_BASE, MENUITEM_ZOOM_TO_SELECTION, Menu.CATEGORY_SYSTEM | 10, R.string.menu_zoom_to_selection);
        menu.add(GROUP_BASE, MENUITEM_SEARCH_OBJECTS, Menu.CATEGORY_SYSTEM | 10, R.string.search_objects_title);
        menu.add(GROUP_BASE, ElementSelectionActionModeCallback.MENUITEM_PREFERENCES, Menu.CATEGORY_SYSTEM | 10, R.string.menu_config)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_config));
        Preferences prefs = new Preferences(main);
        menu.add(GROUP_BASE, ElementSelectionActionModeCallback.MENUITEM_JS_CONSOLE, Menu.CATEGORY_SYSTEM | 10, R.string.tag_menu_js_console)
                .setEnabled(prefs.isJsConsoleEnabled());
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help))
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return true;
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

    // private boolean canMerge(ArrayList<OsmElement> selection) {
    // for (OsmElement e:selection) {
    // if (!(e.getName().equals(Way.NAME) && ((Way)e).isClosed())) {
    // return false;
    // }
    // }
    //
    // return true;
    // }
    //
    // private ArrayList<OsmElement> merge(ArrayList<OsmElement> selection) {
    // if (selection.size() > 1) {
    // Way first = (Way) selection.get(0);
    // ArrayList<OsmElement> rest = (ArrayList<OsmElement>) selection.subList(1,selection.size());
    // ArrayList<OsmElement> newSelection = new ArrayList<OsmElement>();
    // for (OsmElement w:rest) {
    // Way n = logic.mergeSimplePolygons(first, (Way)w);
    // if (n!=null) {
    // first = n;
    // } else {
    // newSelection.add(first);
    // first = (Way)w;
    // }
    // }
    // newSelection.add(first);
    // return
    // }
    // }

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
                main.startSupportActionMode(new AddRelationMemberActionModeCallback(manager, selection));
                break;
            case MENUITEM_ORTHOGONALIZE:
                List<Way> selectedWays = logic.getSelectedWays();
                if (selectedWays != null && !selectedWays.isEmpty()) {
                    logic.performOrthogonalize(main, selectedWays);
                }
                break;
            case MENUITEM_MERGE:
                // check if the tags are the same for all ways first ... ignores direction dependent stuff
                Map<String, String> firstTags = selection.get(0).getTags();
                boolean ok = true;
                for (int i = 1; i < selection.size(); i++) {
                    if ((firstTags.isEmpty() && !selection.get(i).getTags().isEmpty()) || !firstTags.entrySet().equals(selection.get(i).getTags().entrySet())) {
                        ok = false;
                    }
                }
                if (!ok) {
                    Snack.barWarning(main, R.string.toast_potential_merge_tag_conflict);
                    main.performTagEdit(selection, false, false);
                } else {
                    try {
                        MergeResult result = logic.performMerge(main, sortedWays);
                        main.startSupportActionMode(new WaySelectionActionModeCallback(manager, (Way) result.getElement()));
                        if (result.hasIssue()) {
                            showConflictAlert(result);
                        }
                    } catch (OsmIllegalOperationException e) {
                        Snack.barError(main, e.getLocalizedMessage());
                    }
                }
                break;
            case MENUITEM_ZOOM_TO_SELECTION:
                main.zoomTo(selection);
                main.invalidateMap();
                break;
            case MENUITEM_SEARCH_OBJECTS:
                Search.search(main);
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
     * @param deleteFromRelations if true the elements will be deleted without regards for their Relation memebrship
     */
    private void menuDelete(boolean deleteFromRelations) {
        Log.d(DEBUG_TAG, "menuDelete " + deleteFromRelations + " " + selection);

        // check for relation membership
        if (!deleteFromRelations) {
            for (OsmElement e : selection) {
                if (e.hasParentRelations()) {
                    new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.delete_from_relation_description)
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    menuDelete(true);
                                }
                            }).show();
                    return;
                }
            }
        }

        logic.performEraseMultipleObjects(main, selection);

        manager.finish();
    }

    @Override
    public boolean onBackPressed() {
        Log.d(DEBUG_TAG, "onBackPressed");
        deselect = true;
        return super.onBackPressed(); // call the normal stuff
    }
}
