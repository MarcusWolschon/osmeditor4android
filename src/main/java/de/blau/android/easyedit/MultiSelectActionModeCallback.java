package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Main.UndoListener;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.search.Search;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class MultiSelectActionModeCallback extends EasyEditActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, EasyEditActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = MultiSelectActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    protected static final int MENUITEM_UPLOAD            = ElementSelectionActionModeCallback.MENUITEM_UPLOAD;
    protected static final int MENUITEM_ZOOM_TO_SELECTION = ElementSelectionActionModeCallback.MENUITEM_ZOOM_TO_SELECTION;
    protected static final int MENUITEM_SEARCH_OBJECTS    = ElementSelectionActionModeCallback.MENUITEM_SEARCH_OBJECTS;
    protected static final int MENUITEM_ADD_TO_TODO       = ElementSelectionActionModeCallback.MENUITEM_ADD_TO_TODO;
    protected static final int MENUITEM_PASTE_TAGS        = ElementSelectionActionModeCallback.MENUITEM_PASTE_TAGS;

    protected List<OsmElement> selection;
    protected List<OsmElement> sortedWays;

    protected UndoListener undoListener;

    protected boolean deselectOnExit = true;

    private MenuItem undoItem;
    private MenuItem uploadItem;
    private MenuItem pasteItem;

    /**
     * Construct an Multi-Select actionmode from a List of OsmElements
     * 
     * @param manager the current EasEditManager instance
     * @param elements the List of OsmElements
     */
    public MultiSelectActionModeCallback(@NonNull EasyEditManager manager, @NonNull List<OsmElement> elements) {
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
    public MultiSelectActionModeCallback(@NonNull EasyEditManager manager, @Nullable OsmElement element) {
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
        try {
            if (!selection.contains(element)) {
                selection.add(element);
                switch (element.getName()) {
                case Way.NAME:
                    if (((Way) element).nodeCount() == 0) {
                        ScreenMessage.toastTopError(main, main.getString(R.string.toast_degenerate_way_with_info, element.getDescription(main)), true);
                        selection.remove(element);
                        break;
                    }
                    logic.addSelectedWay((Way) element);
                    break;
                case Node.NAME:
                    logic.addSelectedNode((Node) element);
                    break;
                case Relation.NAME:
                    logic.addSelectedRelation((Relation) element);
                    break;
                default:
                    throw new OsmException(element.getName());
                }
            } else {
                selection.remove(element);
                switch (element.getName()) {
                case Way.NAME:
                    logic.removeSelectedWay((Way) element);
                    break;
                case Node.NAME:
                    logic.removeSelectedNode((Node) element);
                    break;
                case Relation.NAME:
                    logic.removeSelectedRelation((Relation) element);
                    break;
                default:
                    throw new OsmException(element.getName());
                }
            }
        } catch (OsmException osmex) {
            Log.e(DEBUG_TAG, "Unkown element type " + osmex.getMessage());
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
     * Set a selected object count in the action mode subtitle
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

        menu.add(GROUP_BASE, MENUITEM_ZOOM_TO_SELECTION, Menu.CATEGORY_SYSTEM | 10, R.string.menu_zoom_to_selection);
        menu.add(GROUP_BASE, MENUITEM_SEARCH_OBJECTS, Menu.CATEGORY_SYSTEM | 10, R.string.search_objects_title);
        menu.add(GROUP_BASE, MENUITEM_ADD_TO_TODO, Menu.CATEGORY_SYSTEM | 10, R.string.menu_add_to_todo);

        pasteItem = menu.add(Menu.NONE, MENUITEM_PASTE_TAGS, Menu.CATEGORY_SECONDARY, R.string.menu_paste_tags);

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

        updated |= ElementSelectionActionModeCallback.setItemVisibility(!App.getTagClipboard(main).isEmpty(), pasteItem, true);

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

    @Override
    public boolean elementsOnly() {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (super.onActionItemClicked(mode, item)) {
            return true;
        }
        switch (item.getItemId()) {
        case ElementSelectionActionModeCallback.MENUITEM_TAG:
            main.performTagEdit(selection, false, false);
            break;
        case MENUITEM_ZOOM_TO_SELECTION:
            main.zoomTo(selection);
            main.invalidateMap();
            break;
        case MENUITEM_SEARCH_OBJECTS:
            Search.search(main);
            break;
        case MENUITEM_ADD_TO_TODO:
            ElementSelectionActionModeCallback.addToTodoList(main, manager, selection);
            break;
        case MENUITEM_PASTE_TAGS:
            main.performTagEdit(selection, false, new HashMap<>(App.getTagClipboard(main).paste()), false);
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
        return true;
    }

    /**
     * Update the selection from logic, used after undo/redo
     */
    public void updateSelection() {
        synchronized (selection) {
            for (OsmElement e : new ArrayList<>(selection)) {
                if (!logic.isSelected(e)) {
                    selection.remove(e);
                }
            }
            sortedWays = Util.sortWays(selection);
            manager.invalidate();
            setSubTitle(mode);
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
        Log.d(DEBUG_TAG, "onDestroyActionMode deselect " + deselectOnExit);
        super.onDestroyActionMode(mode);
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        if (deselectOnExit) {
            logic.deselectAll();
            main.invalidateMap();
        }
    }

    @Override
    public boolean onBackPressed() {
        Log.d(DEBUG_TAG, "onBackPressed");
        deselectOnExit = true;
        return super.onBackPressed(); // call the normal stuff
    }

    @Override
    public boolean processShortcut(Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_tagedit)) {
            main.performTagEdit(selection, false, false);
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_undo)) {
            undoListener.onClick(null);
            return true;
        }
        return super.processShortcut(c);
    }
}
