package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
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
import de.blau.android.osm.RelationUtils;
import de.blau.android.osm.Server;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetTagField;
import de.blau.android.search.Search;
import de.blau.android.services.TrackerService;
import de.blau.android.tasks.Bug;
import de.blau.android.tasks.BugFragment;
import de.blau.android.tasks.Task.State;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.tasks.Todo;
import de.blau.android.tasks.TodoFragment;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.Value;
import de.blau.android.util.WidestItemArrayAdapter;
import de.blau.android.views.CustomAutoCompleteTextView;
import me.zed.elementhistorydialog.ElementHistoryDialog;

/**
 * This action mode handles element selection. When a node or way should be selected, just start this mode. The element
 * will be automatically selected, and a second click on the same element will open the tag editor.
 * 
 * @author Jan
 *
 */
public abstract class ElementSelectionActionModeCallback extends EasyEditActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ElementSelectionActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = ElementSelectionActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final int   MENUITEM_UNDO                 = 0;
    static final int           MENUITEM_TAG                  = 1;
    static final int           MENUITEM_TASK                 = 2;
    static final int           MENUITEM_DELETE               = 3;
    private static final int   MENUITEM_HISTORY_WEB          = 4;
    private static final int   MENUITEM_HISTORY              = 5;
    static final int           MENUITEM_COPY                 = 6;
    static final int           MENUITEM_DUPLICATE            = 7;
    static final int           MENUITEM_SHALLOW_DUPLICATE    = 8;
    static final int           MENUITEM_CUT                  = 9;
    private static final int   MENUITEM_PASTE_TAGS           = 10;
    private static final int   MENUITEM_CREATE_RELATION      = 11;
    private static final int   MENUITEM_ADD_RELATION_MEMBERS = 12;
    private static final int   MENUITEM_EXTEND_SELECTION     = 13;
    private static final int   MENUITEM_ELEMENT_INFO         = 14;
    protected static final int LAST_REGULAR_MENUITEM         = MENUITEM_ELEMENT_INFO;

    static final int           MENUITEM_UPLOAD              = 40;
    protected static final int MENUITEM_SHARE_POSITION      = 41;
    private static final int   MENUITEM_TAG_LAST            = 42;
    static final int           MENUITEM_ZOOM_TO_SELECTION   = 43;
    static final int           MENUITEM_SEARCH_OBJECTS      = 44;
    private static final int   MENUITEM_REPLACE_GEOMETRY    = 45;
    private static final int   MENUITEM_CALIBRATE_BAROMETER = 46;
    static final int           MENUITEM_PREFERENCES         = 47;
    static final int           MENUITEM_JS_CONSOLE          = 48;
    static final int           MENUITEM_ADD_TO_TODO         = 49;

    private static final int MENUITEM_TODO_CLOSE_AND_NEXT = 70;
    private static final int MENUITEM_TODO_SKIP_AND_NEXT  = 71;
    private static final int MENUITEM_TASK_CLOSE_ALL      = 72;

    protected final OsmElement element;

    boolean deselect = true;

    UndoListener     undoListener;
    private MenuItem undoItem;
    private MenuItem uploadItem;
    private MenuItem pasteItem;
    private MenuItem calibrateItem;
    private MenuItem taskMenuItem;
    private MenuItem todoCloseAndNextItem;
    private MenuItem todoSkipAndNextItem;

    Preferences prefs;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param element the selected OsmElement
     */
    protected ElementSelectionActionModeCallback(@NonNull EasyEditManager manager, @NonNull OsmElement element) {
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

        prefs = logic.getPrefs();
        // setup menu
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        final MenuInflater menuInflater = main.getMenuInflater();
        menuInflater.inflate(R.menu.undo_action, menu);
        undoItem = menu.findItem(R.id.undo_action);

        View undoView = undoItem.getActionView();
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);

        menu.add(Menu.NONE, MENUITEM_TAG, Menu.NONE, R.string.menu_tags).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_tags));

        menuInflater.inflate(R.menu.task_menu, menu);
        taskMenuItem = menu.findItem(R.id.task_menu);
        SubMenu taskMenu = taskMenuItem.getSubMenu();
        todoCloseAndNextItem = taskMenu.add(Menu.NONE, MENUITEM_TODO_CLOSE_AND_NEXT, Menu.NONE, R.string.menu_todo_close_and_next);
        todoSkipAndNextItem = taskMenu.add(Menu.NONE, MENUITEM_TODO_SKIP_AND_NEXT, Menu.NONE, R.string.menu_todo_skip_and_next);
        taskMenu.add(Menu.NONE, MENUITEM_TASK_CLOSE_ALL, Menu.NONE, R.string.menu_todo_close_all_tasks);

        menu.add(Menu.NONE, MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_delete));
        final boolean isRelation = element instanceof Relation;
        if (!isRelation || element.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON)) {
            menu.add(Menu.NONE, MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_copy));
            menu.add(Menu.NONE, MENUITEM_DUPLICATE, Menu.CATEGORY_SECONDARY, R.string.menu_duplicate)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_duplicate));
        }
        if (!isRelation) {
            menu.add(Menu.NONE, MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_cut));
        }
        if (!(element instanceof Node)) {
            menu.add(Menu.NONE, MENUITEM_SHALLOW_DUPLICATE, Menu.CATEGORY_SECONDARY, R.string.menu_shallow_duplicate);
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
            boolean connectedOrConnecting = main.isConnectedOrConnecting();
            menu.add(GROUP_BASE, MENUITEM_HISTORY, Menu.CATEGORY_SYSTEM, R.string.menu_history)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_history)).setEnabled(connectedOrConnecting);
            menu.add(GROUP_BASE, MENUITEM_HISTORY_WEB, Menu.CATEGORY_SYSTEM, R.string.menu_history_web)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_history)).setEnabled(connectedOrConnecting);
        }
        menu.add(GROUP_BASE, MENUITEM_ELEMENT_INFO, Menu.CATEGORY_SYSTEM, R.string.menu_information)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_information));
        menu.add(GROUP_BASE, MENUITEM_ZOOM_TO_SELECTION, Menu.CATEGORY_SYSTEM | 10, R.string.menu_zoom_to_selection);
        menu.add(GROUP_BASE, MENUITEM_SEARCH_OBJECTS, Menu.CATEGORY_SYSTEM | 10, R.string.search_objects_title);
        menu.add(GROUP_BASE, MENUITEM_ADD_TO_TODO, Menu.CATEGORY_SYSTEM | 10, R.string.menu_add_to_todo);
        if (!isRelation) {
            menu.add(GROUP_BASE, MENUITEM_REPLACE_GEOMETRY, Menu.CATEGORY_SYSTEM | 10, R.string.menu_replace_geometry);
        }

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

        final boolean hasTaskLayer = main.getMap().getTaskLayer() != null;
        List<Bug> bugs = App.getTaskStorage().getTasksForElement(element);
        updated |= setItemVisibility(hasTaskLayer && !bugs.isEmpty(), taskMenuItem, false);
        boolean hasTodo = false;
        for (Bug b : bugs) {
            if (b instanceof Todo) {
                hasTodo = true;
                break;
            }
        }
        updated |= setItemVisibility(hasTaskLayer && hasTodo, todoCloseAndNextItem, true);
        updated |= setItemVisibility(hasTaskLayer && hasTodo, todoSkipAndNextItem, true);

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
        if (setEnabled) {
            if (item.isEnabled() == !condition) {
                item.setEnabled(condition);
                return true;
            }
        } else {
            if (item.isVisible() == !condition) {
                item.setVisible(condition);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        final TaskStorage taskStorage = App.getTaskStorage();
        final int itemId = item.getItemId();
        switch (itemId) {
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
            main.descheduleAutoLock();
            ElementHistoryDialog ehd = ElementHistoryDialog.create(prefs.getApiUrl(), element.getOsmId(), element.getName());
            ehd.show(main.getSupportFragmentManager(), "history_dialog");
            break;
        case MENUITEM_HISTORY_WEB:
            main.descheduleAutoLock();
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
        case MENUITEM_DUPLICATE:
        case MENUITEM_SHALLOW_DUPLICATE:
            List<OsmElement> result = logic.duplicate(main, Util.wrapInList(element), itemId == MENUITEM_DUPLICATE);
            mode.finish();
            App.getLogic().setSelection(result);
            manager.editElements();
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
                }, -1, R.string.select_relation_title, null, null, Util.wrapInList(element)).show();
            }
            break;
        case MENUITEM_EXTEND_SELECTION:
            deselect = false;
            main.startSupportActionMode(new MultiSelectWithGeometryActionModeCallback(manager, element));
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
            PrefEditor.start(main, Main.REQUEST_PREFERENCES);
            break;
        case MENUITEM_ZOOM_TO_SELECTION:
            main.zoomTo(element);
            main.invalidateMap();
            break;
        case MENUITEM_SEARCH_OBJECTS:
            main.descheduleAutoLock();
            Search.search(main);
            break;
        case MENUITEM_REPLACE_GEOMETRY:
            deselect = false;
            main.startSupportActionMode(new ReplaceGeometryActionModeCallback(manager, element));
            break;
        case MENUITEM_CALIBRATE_BAROMETER:
            Intent intent = new Intent(main, TrackerService.class);
            intent.putExtra(TrackerService.CALIBRATE_KEY, true);
            try {
                intent.putExtra(TrackerService.CALIBRATE_HEIGHT_KEY, Integer.parseInt(element.getTagWithKey(Tags.KEY_ELE)));
                main.startService(intent);
            } catch (NumberFormatException nfex) {
                ScreenMessage.toastTopError(main, main.getString(R.string.toast_invalid_number_format, nfex.getMessage()));
            }
            break;
        case MENUITEM_JS_CONSOLE:
            Main.showJsConsole(main);
            break;
        case MENUITEM_ADD_TO_TODO:
            addToTodoList(main, manager, Util.wrapInList(element));
            break;
        case MENUITEM_TODO_CLOSE_AND_NEXT:
        case MENUITEM_TODO_SKIP_AND_NEXT:
            final List<Todo> todos = taskStorage.getTodosForElement(element);
            State newState = itemId == MENUITEM_TODO_CLOSE_AND_NEXT ? State.CLOSED : State.SKIPPED;
            Set<StringWithDescription> listNames = new HashSet<>();
            for (int i = 0; i < todos.size(); i++) {
                listNames.add(todos.get(i).getListName(main));
            }
            if (todos.size() == 1 || listNames.size() == 1) {
                setTodoStateAndNext(taskStorage, todos.get(0), newState);
            } else {
                selectTodoList(main, new ArrayList<>(listNames),
                        (DialogInterface dialog, int which) -> setTodoStateAndNext(taskStorage, todos.get(which), newState));
            }
            break;
        case MENUITEM_TASK_CLOSE_ALL:
            taskStorage.closeTasksForElement(element);
            ScreenMessage.toastTopInfo(main, R.string.toast_todo_all_closed);
            main.invalidateMap();
            manager.invalidate();
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
     * Show a dialog that allows selection of a todo list // NOSONAR
     * 
     * @param context an Android Context
     * @param todoLists a List of todo list names // NOSONAR
     * @param listener a listener to call on selection
     */
    public static void selectTodoList(@NonNull Context context, @NonNull List<StringWithDescription> todoLists,
            @NonNull DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_todo_list);
        ArrayAdapter<StringWithDescription> adapter = new ArrayAdapter<>(context, R.layout.dialog_list_item, todoLists);
        builder.setAdapter(adapter, listener);
        builder.show();
    }

    /**
     * Close a Todo and start editing the next/nearest one // NOSONAR
     * 
     * @param taskStorage the current TaskStorage
     * @param todo the Todo //NOSONAR
     * @param state the State to set
     */
    private void setTodoStateAndNext(@NonNull final TaskStorage taskStorage, @NonNull final Todo todo, @NonNull State state) {
        todo.setState(state);
        taskStorage.setDirty();
        final StringWithDescription listName = todo.getListName(main);
        List<Todo> todoList = taskStorage.getTodos(listName.getValue(), false);
        if (todoList.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(main);
            builder.setTitle(R.string.all_todos_done_title);
            builder.setMessage(main.getString(R.string.all_todos_done_message, listName.toString()));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.delete, (DialogInterface dialog, int which) -> {
                for (Todo t : taskStorage.getTodos(listName.getValue(), true)) {
                    taskStorage.delete(t);
                }
                main.invalidateMap();
            });
            builder.show();
        } else {
            Todo next = todo.getNearest(todoList);
            final List<OsmElement> elements = next.getElements();
            final OsmElement e = !elements.isEmpty() ? elements.get(0) : null;
            if (e != null && OsmElement.STATE_DELETED != e.getState()) {
                BugFragment.gotoAndEditElement(main, App.getDelegator(), e, next.getLon(), next.getLat());
            } else {
                TodoFragment.showDialog(main, next);
            }
        }
    }

    /**
     * Add elements to a todo list // NOSONAR
     * 
     * @param activity the current activity
     * @param manager the current EasyEditManager
     * @param elements a List of OsmElement
     */
    public static void addToTodoList(@NonNull FragmentActivity activity, @NonNull EasyEditManager manager, @NonNull List<OsmElement> elements) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getQuantityString(R.plurals.add_todo_title, elements.size()));
        View layout = activity.getLayoutInflater().inflate(R.layout.add_todo, null);
        final CustomAutoCompleteTextView todoList = layout.findViewById(R.id.todoList);
        final TextView todoComment = layout.findViewById(R.id.todoComment);
        List<StringWithDescription> todoLists = App.getTaskStorage().getTodoLists(activity);
        todoList.setAdapter(new ArrayAdapter<>(activity, R.layout.autocomplete_row, todoLists));
        todoList.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (hasFocus) {
                todoList.showDropDown();
            } else {
                todoList.dismissDropDown();
            }
        });
        todoList.setOnItemClickListener((parent, view, pos, id) -> {
            Object o = parent.getItemAtPosition(pos);
            todoList.setOrReplaceText(o instanceof Value ? ((Value) o).getValue() : (String) o);
        });
        builder.setView(layout);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.add, null);
        AlertDialog addTodoDialog = builder.create();
        addTodoDialog.setOnShowListener(dialog -> {
            Button positive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(view -> {
                final TaskStorage storage = App.getTaskStorage();
                String listName = todoList.getText().toString();
                if ("".equals(listName)) {
                    listName = Todo.DEFAULT_LIST;
                }
                final String comment = todoComment.getText().toString();
                for (OsmElement e : elements) {
                    if (storage.contains(e, listName)) {
                        ScreenMessage.toastTopWarning(activity, R.string.toast_todo_already_in_list);
                        return;
                    }
                }
                for (OsmElement e : elements) {
                    Todo todo = new Todo(listName, e);
                    if (!"".equals(comment)) {
                        todo.setTitle(comment);
                    }
                    storage.add(todo);
                }
                dialog.dismiss();
                if (activity instanceof Main) {
                    ((Main) activity).invalidateMap();
                }
            });
        });
        addTodoDialog.setOnDismissListener((DialogInterface dialog) -> manager.invalidate());
        addTodoDialog.show();
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
     *
     * FIXME To avoid being caught by the pathPatterns in the manifest we use the API url, that is redirected to the
     * website in this special case, this is a hack that will require API 31 with better pattern support to fix
     * properly.
     */
    private void showHistory() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(Server.getBaseUrl(prefs.getServer().getReadWriteUrl()) + element.getName() + "/" + element.getOsmId() + "/history"));
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
            ScreenMessage.toastTopWarning(context, context.getResources().getQuantityString(R.plurals.added_required_elements, added, added));
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
     * @param selection List of Elements for sorting by distance to
     * @return a dialog
     */
    @NonNull
    static AlertDialog buildRelationSelectDialog(@NonNull Context context, @NonNull OnRelationSelectedListener onRelationSelectedListener, long currentId,
            int titleId, @Nullable String filterKey, @Nullable String filterValue, @NonNull List<OsmElement> selection) {
        Builder builder = new AlertDialog.Builder(context);

        final View layout = ThemeUtils.getLayoutInflater(context).inflate(R.layout.relation_selection_dialog, null);

        builder.setView(layout);
        builder.setTitle(titleId);
        builder.setNegativeButton(R.string.cancel, null);

        RecyclerView relationList = (RecyclerView) layout.findViewById(R.id.relationList);
        LayoutParams buttonLayoutParams = relationList.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        relationList.setLayoutManager(layoutManager);

        List<Relation> relations = new ArrayList<>();
        // filter
        for (Relation r : App.getDelegator().getCurrentStorage().getRelations()) {
            if (filterRelations(r, filterKey, filterValue) && !selection.contains(r)) {
                relations.add(r);
            }
        }

        final AlertDialog dialog = builder.create();
        if (relations.isEmpty()) {
            builder.setMessage(R.string.no_suitable_relations_message);
            return dialog;
        }

        if (selection.size() == 1 && selection.get(0) instanceof Way) {
            Way w = (Way) selection.get(0);
            selection.clear();
            selection.add(w.getFirstNode());
            selection.add(w.getLastNode());
        }

        RelationUtils.sortRelationListByDistance(selection, relations);

        List<Long> ids = new ArrayList<>();
        for (Relation r : relations) {
            ids.add(r.getOsmId());
        }

        final Handler handler = new Handler(Looper.getMainLooper());
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

    /**
     * If a filterKey isn't null check if a tag exists for it
     * 
     * @param r the Relation
     * @param filterKey optional key to filter on
     * @param filterValue optional value for key to filter on
     * @return true if the filter condition is satisfied
     */
    private static boolean filterRelations(@NonNull Relation r, @Nullable String filterKey, @Nullable String filterValue) {
        if (filterKey == null) {
            return true;
        }
        String value = r.getTagWithKey(filterKey);
        return value != null && (filterValue == null || filterValue.equals(value));
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
        final View layout = ThemeUtils.getLayoutInflater(context).inflate(R.layout.preset_selection_dialog, null);

        builder.setView(layout);

        final Map<String, PresetItem> items = new HashMap<>();
        for (Preset preset : App.getCurrentPresets(context)) {
            if (preset == null) {
                continue;
            }
            for (PresetItem item : preset.getItemsForType(type).values()) {
                if (filterKey == null) {
                    items.put(item.getTranslatedName(), item);
                    continue;
                }
                PresetTagField field = item.getField(filterKey);
                if (field != null && (filterValue == null
                        || (field instanceof PresetFixedField && filterValue.equals(((PresetFixedField) field).getValue().getValue())))) {
                    items.put(item.getTranslatedName(), item);
                }
            }
        }
        List<String> itemNames = new ArrayList<>(items.keySet());
        Collections.sort(itemNames);
        itemNames.add(context.getString(R.string.select_relation_type_other));
        final WidestItemArrayAdapter<String> adapter = new WidestItemArrayAdapter<>(context, R.layout.search_results_item, itemNames);

        ListView presetList = (ListView) layout.findViewById(R.id.presetList);

        presetList.setAdapter(adapter);
        final AlertDialog dialog = builder.create();

        final Handler handler = new Handler(Looper.getMainLooper());
        presetList.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String key = adapter.getItem(position);
            onPresetSelectedListener.selected(items.get(key));
            handler.postDelayed(dialog::dismiss, 100);
        });
        return dialog;
    }
}
