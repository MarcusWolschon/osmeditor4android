package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Main.UndoListener;
import de.blau.android.R;
import de.blau.android.dialogs.ElementInfo;
import de.blau.android.dialogs.EmptyRelation;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.search.Search;
import de.blau.android.services.TrackerService;
import de.blau.android.util.Snack;
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

    private static final String DEBUG_TAG                     = "ElementSelectionActi...";
    private static final int    MENUITEM_UNDO                 = 0;
    static final int            MENUITEM_TAG                  = 1;
    static final int            MENUITEM_DELETE               = 2;
    private static final int    MENUITEM_HISTORY              = 3;
    static final int            MENUITEM_COPY                 = 4;
    static final int            MENUITEM_CUT                  = 5;
    private static final int    MENUITEM_PASTE_TAGS           = 6;
    private static final int    MENUITEM_CREATE_RELATION      = 7;
    private static final int    MENUITEM_ADD_RELATION_MEMBERS = 8;
    private static final int    MENUITEM_EXTEND_SELECTION     = 9;
    private static final int    MENUITEM_ELEMENT_INFO         = 10;
    protected static final int  LAST_REGULAR_MENUITEM         = MENUITEM_ELEMENT_INFO;

    private static final int   MENUITEM_UPLOAD              = 31;
    protected static final int MENUITEM_SHARE_POSITION      = 32;
    private static final int   MENUITEM_TAG_LAST            = 33;
    private static final int   MENUITEM_ZOOM_TO_SELECTION   = 34;
    private static final int   MENUITEM_SEARCH_OBJECTS      = 35;
    private static final int   MENUITEM_CALIBRATE_BAROMETER = 36;
    static final int           MENUITEM_PREFERENCES         = 37;
    static final int           MENUITEM_JS_CONSOLE          = 38;

    OsmElement element = null;

    boolean deselect = true;

    UndoListener     undoListener;
    private MenuItem undoItem;
    private MenuItem uploadItem;
    private MenuItem pasteItem;
    private MenuItem calibrateItem;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param element the selected OsmElement
     */
    protected ElementSelectionActionModeCallback(EasyEditManager manager, OsmElement element) {
        super(manager);
        this.element = element;
        undoListener = main.new UndoListener();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        logic.setSelectedRelationWays(null);
        logic.setSelectedRelationNodes(null);
        main.getMap().deselectObjects();

        Preferences prefs = logic.getPrefs();
        // setup menu
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        main.getMenuInflater().inflate(R.menu.undo_action, menu);
        undoItem = menu.findItem(R.id.undo_action);

        View undoView = undoItem.getActionView();
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);

        menu.add(Menu.NONE, MENUITEM_TAG, Menu.NONE, R.string.menu_tags).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_tags));
        menu.add(Menu.NONE, MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_delete));
        if (!(element instanceof Relation)) {
            menu.add(Menu.NONE, MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_copy));
            menu.add(Menu.NONE, MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_cut));
        }
        pasteItem = menu.add(Menu.NONE, MENUITEM_PASTE_TAGS, Menu.CATEGORY_SECONDARY, R.string.menu_paste_tags);

        menu.add(GROUP_BASE, MENUITEM_EXTEND_SELECTION, Menu.CATEGORY_SYSTEM, R.string.menu_extend_selection)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_multi_select));
        menu.add(Menu.NONE, MENUITEM_CREATE_RELATION, Menu.CATEGORY_SYSTEM, R.string.menu_relation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation));
        menu.add(Menu.NONE, MENUITEM_ADD_RELATION_MEMBERS, Menu.CATEGORY_SYSTEM,
                element instanceof Relation ? R.string.menu_add_relation_member : R.string.tag_menu_addtorelation)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_relation_add_member));
        if (element.getOsmId() > 0) {
            menu.add(GROUP_BASE, MENUITEM_HISTORY, Menu.CATEGORY_SYSTEM, R.string.menu_history)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_history)).setEnabled(main.isConnectedOrConnecting());
        }
        menu.add(GROUP_BASE, MENUITEM_ELEMENT_INFO, Menu.CATEGORY_SYSTEM, R.string.menu_information)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_information));
        menu.add(GROUP_BASE, MENUITEM_ZOOM_TO_SELECTION, Menu.CATEGORY_SYSTEM | 10, R.string.menu_zoom_to_selection);
        menu.add(GROUP_BASE, MENUITEM_SEARCH_OBJECTS, Menu.CATEGORY_SYSTEM | 10, R.string.search_objects_title);

        uploadItem = menu.add(GROUP_BASE, MENUITEM_UPLOAD, Menu.CATEGORY_SYSTEM | 10, R.string.menu_upload_element);

        menu.add(GROUP_BASE, MENUITEM_SHARE_POSITION, Menu.CATEGORY_SYSTEM | 10, R.string.share_position);
        if (prefs.useBarometricHeight()) {
            menu.add(GROUP_BASE, MENUITEM_CALIBRATE_BAROMETER, Menu.CATEGORY_SYSTEM | 10, R.string.menu_tools_calibrate_height);
        }
        menu.add(GROUP_BASE, MENUITEM_PREFERENCES, Menu.CATEGORY_SYSTEM | 10, R.string.menu_config)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_config));

        menu.add(GROUP_BASE, MENUITEM_JS_CONSOLE, Menu.CATEGORY_SYSTEM | 10, R.string.tag_menu_js_console).setEnabled(prefs.isJsConsoleEnabled());
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        return true;
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
        if (element.equals(this.element)) {
            // remove any empty move undo checkpoint
            switch (element.getName()) {
            case Node.NAME:
                App.getLogic().removeCheckpoint(main, R.string.undo_action_movenode);
                break;
            case Way.NAME:
                App.getLogic().removeCheckpoint(main, R.string.undo_action_moveway);
                break;
            default:
            }
            main.performTagEdit(element, null, false, false);
            return true;
        }
        return false;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        boolean updated = false;
        super.onPrepareActionMode(mode, menu);
        if (logic.getUndo().canUndo() || logic.getUndo().canRedo()) {
            if (!undoItem.isVisible()) {
                undoItem.setVisible(true);
                updated = true;
            }
        } else if (undoItem.isVisible()) {
            undoItem.setVisible(false);
            updated = true;
        }

        updated |= setItemVisibility(!element.isUnchanged(), uploadItem, true);
        updated |= setItemVisibility(!App.getTagClipboard(main).isEmpty(), pasteItem, true);
        if (calibrateItem != null) {
            String ele = element.getTagWithKey(Tags.KEY_ELE);
            updated |= setItemVisibility(ele != null && !"".equals(ele), calibrateItem, true);
        }
        return updated;

    }

    /**
     * Set a menus visibility or enabled status
     * 
     * @param condition the condition visibility depends on
     * @param item the MenuItem
     * @param setEnabled set enabled status instead of visibility
     * @return true if the visibility was changed
     */
    public static boolean setItemVisibility(boolean condition, @NonNull MenuItem item, boolean setEnabled) {
        if (condition) {
            if (setEnabled) {
                if (!item.isEnabled()) {
                    item.setEnabled(true);
                    return true;
                }
            } else {
                if (!item.isVisible()) {
                    item.setVisible(true);
                    return true;
                }
            }
        } else {
            if (setEnabled) {
                if (item.isEnabled()) {
                    item.setEnabled(false);
                    return true;
                }
            } else {
                if (item.isVisible()) {
                    item.setVisible(false);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_TAG:
            main.performTagEdit(element, null, false, false);
            break;
        case MENUITEM_TAG_LAST:
            main.performTagEdit(element, null, true, false);
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
        case MENUITEM_CREATE_RELATION:
            buildPresetSelectDialog(main, p -> {
                deselect = false;
                logic.setSelectedNode(null);
                logic.setSelectedWay(null);
                logic.setSelectedRelation(null);
                main.startSupportActionMode(new EditRelationMembersActionModeCallback(manager,
                        p == null ? null : p.getPath(App.getCurrentRootPreset(main).getRootGroup()), element));
            }, ElementType.RELATION, R.string.select_relation_type_title, Tags.KEY_TYPE, null).show();
            break;
        case MENUITEM_ADD_RELATION_MEMBERS:
            if (element instanceof Relation) {
                main.startSupportActionMode(new EditRelationMembersActionModeCallback(manager, (Relation) element, (OsmElement) null));
            } else {
                buildRelationSelectDialog(main, r -> {
                    Relation relation = (Relation) App.getDelegator().getOsmElement(Relation.NAME, r);
                    if (relation != null) {
                        main.startSupportActionMode(new EditRelationMembersActionModeCallback(manager, relation, element));
                    }
                }, -1, R.string.select_relation_title, null, null).show();
            }
            break;
        case MENUITEM_EXTEND_SELECTION:
            deselect = false;
            main.startSupportActionMode(new ExtendSelectionActionModeCallback(manager, element));
            break;
        case MENUITEM_ELEMENT_INFO:
            main.descheduleAutoLock();
            // as we want to display relation membership changes too
            // we can't rely on the element status
            ElementInfo.showDialog(main, 0, element, false);
            break;
        case MENUITEM_UPLOAD:
            main.descheduleAutoLock();
            main.confirmUpload(addRequiredElements(main, Util.wrapInList(element)));
            break;
        case MENUITEM_PREFERENCES:
            PrefEditor.start(main);
            break;
        case MENUITEM_ZOOM_TO_SELECTION:
            main.zoomTo(element);
            main.invalidateMap();
            break;
        case MENUITEM_SEARCH_OBJECTS:
            Search.search(main);
            break;
        case MENUITEM_CALIBRATE_BAROMETER:
            Intent intent = new Intent(main, TrackerService.class);
            intent.putExtra(TrackerService.CALIBRATE_KEY, true);
            try {
                intent.putExtra(TrackerService.CALIBRATE_HEIGHT_KEY, Integer.parseInt(element.getTagWithKey(Tags.KEY_ELE)));
                main.startService(intent);
            } catch (NumberFormatException nfex) {
                Snack.toastTopError(main, main.getString(R.string.toast_invalid_number_format, nfex.getMessage()));
            }
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
     * Check if any of the relations are empty and offer to delete them
     * 
     * @param activity calling FragmentActivity
     * @param relations a List of Relation to check
     */
    public static void checkEmptyRelations(@NonNull FragmentActivity activity, @Nullable List<Relation> relations) {
        if (relations != null) {
            Set<Long> empty = new HashSet<>();
            for (Relation r : relations) {
                final List<RelationMember> members = r.getMembers();
                if (members == null || members.isEmpty()) {
                    empty.add(r.getOsmId());
                }
            }
            if (!empty.isEmpty()) {
                EmptyRelation.showDialog(activity, new ArrayList<>(empty));
            }
        }
    }

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
            main.performTagEdit(element, null, false, false);
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_paste_tags)) {
            Map<String, String> tags = App.getTagClipboard(main).paste();
            if (tags != null) {
                main.performTagEdit(element, null, new HashMap<>(tags), false);
            }
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_undo)) {
            undoListener.onClick(null);
            return true;
        } else if (c == Util.getShortCut(main, R.string.shortcut_remove)) {
            menuDelete(mode);
            return true;
        }
        return super.processShortcut(c);
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

    /**
     * Add any required referenced elements to upload
     * 
     * @param context and Android Context
     * @param elements the List of elements
     * @return the List of elements for convenience
     */
    static List<OsmElement> addRequiredElements(@NonNull final Context context, @NonNull final List<OsmElement> elements) {
        int originalSize = elements.size();
        for (OsmElement e : new ArrayList<>(elements)) {
            if (e instanceof Way) {
                for (Node n : ((Way) e).getNodes()) {
                    if (n.getOsmId() < 0 && !elements.contains(n)) {
                        elements.add(n);
                    }
                }
            } else if (e instanceof Relation) {
                for (RelationMember rm : ((Relation) e).getMembers()) {
                    if (rm.getRef() < 0 && !elements.contains(rm.getElement())) {
                        elements.add(rm.getElement());
                    }
                }
            }
        }
        int added = elements.size() - originalSize;
        if (added > 0) {
            Snack.toastTopWarning(context, context.getResources().getQuantityString(R.plurals.added_required_elements, added, added));
        }
        return elements;
    }

    interface OnRelationSelectedListener {
        /**
         * Call back for when a Relation has been selected
         * 
         * @param id the OSM id of the Relation
         */
        void selected(long id);
    }

    /**
     * Create a dialog allowing a relation to be selected
     * 
     * @param context an Android Context
     * @param onRelationSelectedListener called when a relation has been selected
     * @param currentId a potentially pre-selected relation or -1
     * @param titleId string resource id to the title
     * @param filterKey key to use for filtering
     * @param filterValue value to use for filtering (filterKey must not be null)
     * @return a dialog
     */
    @NonNull
    static AlertDialog buildRelationSelectDialog(@NonNull Context context, @NonNull OnRelationSelectedListener onRelationSelectedListener, long currentId,
            int titleId, @Nullable String filterKey, @Nullable String filterValue) {
        Builder builder = new AlertDialog.Builder(context);

        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(context);

        final View layout = themedInflater.inflate(R.layout.relation_selection_dialog, null);

        builder.setView(layout);
        builder.setTitle(titleId);
        builder.setNegativeButton(R.string.cancel, null);

        RecyclerView relationList = (RecyclerView) layout.findViewById(R.id.relationList);
        LayoutParams buttonLayoutParams = relationList.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        relationList.setLayoutManager(layoutManager);

        List<Relation> allRelations = App.getDelegator().getCurrentStorage().getRelations();
        List<Long> ids = new ArrayList<>();
        // filter
        if (filterKey != null) {
            for (Relation r : allRelations) {
                String value = r.getTagWithKey(filterKey);
                if (value != null && (filterValue == null || filterValue.equals(value))) {
                    ids.add(r.getOsmId());
                }
            }
        } else {
            for (Relation r : allRelations) {
                ids.add(r.getOsmId());
            }
        }

        if (ids.isEmpty()) {
            builder.setMessage(R.string.no_suitable_relations_message);
        }

        final AlertDialog dialog = builder.create();

        final Handler handler = new Handler();
        OnCheckedChangeListener onCheckedChangeListener = (group, position) -> {
            if (position != -1) {
                onRelationSelectedListener.selected(ids.get(position));
            }
            // allow a tiny bit of time to see that the action actually worked
            handler.postDelayed(dialog::dismiss, 100);
        };

        RelationListAdapter adapter = new RelationListAdapter(context, ids, currentId, buttonLayoutParams, onCheckedChangeListener);
        relationList.setAdapter(adapter);

        return dialog;
    }

    interface OnPresetSelectedListener {
        /**
         * Call back for when a Relation has been selected
         * 
         * @param item the PresetItem
         */
        void selected(@Nullable PresetItem item);
    }

    /**
     * Create a dialog allowing a relation to be selected
     * 
     * @param context an Android Context
     * @param onPresetSelectedListener called when a preset has been selected
     * @param type type of element
     * @param titleId string resource id to the title
     * @param filterKey key to use for filtering
     * @param filterValue value to use for filtering (filterKey must not be null)
     * @return a dialog
     */
    @NonNull
    static AlertDialog buildPresetSelectDialog(@NonNull Context context, @NonNull final OnPresetSelectedListener onPresetSelectedListener, ElementType type,
            int titleId, @Nullable String filterKey, @Nullable String filterValue) {
        Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(titleId);
        builder.setNegativeButton(R.string.cancel, null);

        final Map<String, PresetItem> items = new HashMap<>();
        for (Preset preset : App.getCurrentPresets(context)) {
            if (preset != null) {
                for (PresetItem item : preset.getItemsForType(type).values()) {
                    if (filterKey != null) {
                        PresetField field = item.getField(filterKey);
                        if (field != null && (filterValue == null
                                || (field instanceof PresetFixedField && filterValue.equals(((PresetFixedField) field).getValue().getValue())))) {
                            items.put(item.getTranslatedName(), item);
                        }
                    } else {
                        items.put(item.getTranslatedName(), item);
                    }
                }
            }
        }
        List<String> itemNames = new ArrayList<>(items.keySet());
        Collections.sort(itemNames);
        itemNames.add(context.getString(R.string.select_relation_type_other));

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.search_results_item, itemNames);

        builder.setAdapter(adapter, (dialog, which) -> {
            String key = adapter.getItem(which);
            onPresetSelectedListener.selected(items.get(key));
        });

        return builder.create();
    }
}
