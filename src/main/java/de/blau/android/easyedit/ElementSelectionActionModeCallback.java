package de.blau.android.easyedit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Main.UndoListener;
import de.blau.android.R;
import de.blau.android.dialogs.ElementInfo;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.UndoStorage.UndoElement;
import de.blau.android.osm.Way;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * This action mode handles element selection. When a node or way should be selected, just start this mode. The element
 * will be automatically selected, and a second click on the same element will open the tag editor.
 * 
 * @author Jan
 *
 */
public abstract class ElementSelectionActionModeCallback extends EasyEditActionModeCallback {

    private static final String DEBUG_TAG                 = "ElementSelectionActi...";
    private static final int    MENUITEM_UNDO             = 0;
    private static final int    MENUITEM_TAG              = 1;
    private static final int    MENUITEM_DELETE           = 2;
    private static final int    MENUITEM_HISTORY          = 3;
    private static final int    MENUITEM_COPY             = 4;
    private static final int    MENUITEM_CUT              = 5;
    private static final int    MENUITEM_PASTE_TAGS       = 6;
    private static final int    MENUITEM_RELATION         = 7;
    private static final int    MENUITEM_EXTEND_SELECTION = 8;
    private static final int    MENUITEM_ELEMENT_INFO     = 9;

    protected static final int MENUITEM_SHARE_POSITION    = 21;
    private static final int   MENUITEM_TAG_LAST          = 22;
    private static final int   MENUITEM_ZOOM_TO_SELECTION = 23;
    private static final int   MENUITEM_PREFERENCES       = 24;
    private static final int   MENUITEM_JS_CONSOLE        = 25;

    OsmElement element = null;

    boolean deselect = true;

    UndoListener undoListener;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param element the selected OsmElement
     */
    public ElementSelectionActionModeCallback(EasyEditManager manager, OsmElement element) {
        super(manager);
        this.element = element;
        undoListener = main.new UndoListener();
    }

    /**
     * Internal helper to avoid duplicate code in {@link #handleElementClick(OsmElement)}}.
     * 
     * @param element clicked element
     * @return true if handled, false if default handling should apply
     */
    @Override
    public boolean handleElementClick(OsmElement element) {
        super.handleElementClick(element);
        if (element == this.element) {
            // remove any empty undo checkpoint from potentially starting a move
            switch (element.getName()) {
            case Node.NAME:
                App.getLogic().removeCheckpoint(main, R.string.undo_action_movenode);
                break;
            case Way.NAME:
                App.getLogic().removeCheckpoint(main, R.string.undo_action_moveway);
                break;
            default:
            }
            main.performTagEdit(element, null, false, false, false);
            return true;
        }
        return false;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.clear();
        menuUtil.reset();

        main.getMenuInflater().inflate(R.menu.undo_action, menu);
        MenuItem undo = menu.findItem(R.id.undo_action);
        if (logic.getUndo().canUndo() || logic.getUndo().canRedo()) {
            undo.setVisible(true);
            undo.setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_undo));
        } else {
            undo.setVisible(false);
        }
        View undoView = MenuItemCompat.getActionView(undo);
        if (undoView == null) { // FIXME this is a temp workaround for pre-11 Android, we could probably simply
                                // always do the following
            Log.d(DEBUG_TAG, "undoView null");
            Context context = ThemeUtils.getThemedContext(main, R.style.Theme_customMain_Light, R.style.Theme_customMain);
            undoView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.undo_action_view, null);
        }
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);

        menu.add(Menu.NONE, MENUITEM_TAG, Menu.NONE, R.string.menu_tags).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_tagedit))
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_tags));
        menu.add(Menu.NONE, MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_delete));
        // disabled for now menu.add(Menu.NONE, MENUITEM_TAG_LAST, Menu.NONE,
        // R.string.tag_menu_repeat).setIcon(R.drawable.tag_menu_repeat);
        if (!(element instanceof Relation)) {
            menu.add(Menu.NONE, MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy)
                    .setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_copy)).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_copy));
            menu.add(Menu.NONE, MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_cut))
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_cut));
        }
        menu.add(Menu.NONE, MENUITEM_PASTE_TAGS, Menu.CATEGORY_SECONDARY, R.string.menu_paste_tags)
                .setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_paste_tags)).setEnabled(!App.getTagClipboard(main).isEmpty());

        menu.add(GROUP_BASE, MENUITEM_EXTEND_SELECTION, Menu.CATEGORY_SYSTEM, R.string.menu_extend_selection)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_multi_select));
        menu.add(Menu.NONE, MENUITEM_RELATION, Menu.CATEGORY_SYSTEM, R.string.menu_relation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation));
        if (element.getOsmId() > 0) {
            menu.add(GROUP_BASE, MENUITEM_HISTORY, Menu.CATEGORY_SYSTEM, R.string.menu_history)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_history)).setEnabled(main.isConnectedOrConnecting());
        }
        menu.add(GROUP_BASE, MENUITEM_ELEMENT_INFO, Menu.CATEGORY_SYSTEM, R.string.menu_information)
                .setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_info)).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_information));
        menu.add(GROUP_BASE, MENUITEM_ZOOM_TO_SELECTION, Menu.CATEGORY_SYSTEM | 10, R.string.menu_zoom_to_selection);
        menu.add(GROUP_BASE, MENUITEM_SHARE_POSITION, Menu.CATEGORY_SYSTEM | 10, R.string.share_position);
        menu.add(GROUP_BASE, MENUITEM_PREFERENCES, Menu.CATEGORY_SYSTEM | 10, R.string.menu_config)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_config));
        Preferences prefs = new Preferences(main);
        menu.add(GROUP_BASE, MENUITEM_JS_CONSOLE, Menu.CATEGORY_SYSTEM | 10, R.string.tag_menu_js_console).setEnabled(prefs.isJsConsoleEnabled());
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help))
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_TAG:
            main.performTagEdit(element, null, false, false, false);
            break;
        case MENUITEM_TAG_LAST:
            main.performTagEdit(element, null, true, false, false);
            break;
        case MENUITEM_DELETE:
            menuDelete(mode);
            break;
        case MENUITEM_HISTORY:
            showHistory();
            break;
        case MENUITEM_COPY:
            logic.copyToClipboard(element);
            mode.finish();
            break;
        case MENUITEM_CUT:
            logic.cutToClipboard(main, element);
            mode.finish();
            break;
        case MENUITEM_PASTE_TAGS:
            main.performTagEdit(element, null, new HashMap<>(App.getTagClipboard(main).paste()), false);
            break;
        case MENUITEM_RELATION:
            deselect = false;
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setSelectedRelation(null);
            main.startSupportActionMode(new AddRelationMemberActionModeCallback(manager, element));
            break;
        case MENUITEM_EXTEND_SELECTION:
            deselect = false;
            main.startSupportActionMode(new ExtendSelectionActionModeCallback(manager, element));
            break;
        case MENUITEM_ELEMENT_INFO:
            main.descheduleAutoLock();
            // as we want to display relation membership changes too
            // we can't rely on the element status
            UndoElement ue = App.getDelegator().getUndo().getOriginal(element);
            if (ue != null) {
                ElementInfo.showDialog(main, ue, element);
            } else {
                ElementInfo.showDialog(main, element);
            }
            break;
        case MENUITEM_PREFERENCES:
            PrefEditor.start(main, main.getMap().getViewBox());
            break;
        case MENUITEM_ZOOM_TO_SELECTION:
            main.zoomTo(element);
            main.invalidateMap();
            break;
        case MENUITEM_JS_CONSOLE:
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
     * Element specific delete action
     * 
     * @param mode the ActionMode
     */
    protected abstract void menuDelete(ActionMode mode);

    /**
     * Opens the history page of the selected element in a browser
     */
    private void showHistory() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Preferences prefs = new Preferences(main);
        intent.setData(Uri.parse(prefs.getServer().getWebsiteBaseUrl() + element.getName() + "/" + element.getOsmId() + "/history"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        main.startActivity(intent);
    }

    /**
     * Element selection action mode is ending
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        if (deselect) {
            Log.d(DEBUG_TAG, "deselecting");
            logic.deselectAll();
        }
        super.onDestroyActionMode(mode);
    }

    @Override
    public boolean processShortcut(Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_copy)) {
            logic.copyToClipboard(element);
            manager.finish();
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_cut)) {
            logic.cutToClipboard(main, element);
            manager.finish();
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_info)) {
            ElementInfo.showDialog(main, element);
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_tagedit)) {
            main.performTagEdit(element, null, false, false, false);
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_paste_tags)) {
            main.performTagEdit(element, null, new HashMap<>(App.getTagClipboard(main).paste()), false);
            return true;
        }
        return false;
    }

    /**
     * Finds which nodes can be append targets.
     * 
     * @param way The way that will be appended to.
     * @return The set of nodes suitable for appending.
     */
    protected Set<OsmElement> findAppendableNodes(@NonNull Way way) {
        Set<OsmElement> result = new HashSet<>();
        for (Node node : way.getNodes()) {
            if (way.isEndNode(node)) {
                result.add(node);
            }
        }
        // don't allow appending to circular ways
        if (result.size() == 1) {
            result.clear();
        }
        return result;
    }

}
