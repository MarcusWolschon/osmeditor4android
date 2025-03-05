package de.blau.android.easyedit;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.PopupMenu;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.ClipboardStorage;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.tasks.NoteFragment;
import de.blau.android.util.NumberDrawable;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.collections.MRUList;
import de.blau.android.voice.Commands;

public class SimpleActionModeCallback extends EasyEditActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {

    private static final int ICON_INSET = 6;
    private static final int ICON_LAYER = 1;

    interface SimpleActionCallback {
        /**
         * Executes the actual code associated with a SimpleAction
         * 
         * @param main the current Main instance
         * @param manager the current EasyEditManager
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        void action(@NonNull final Main main, @NonNull final EasyEditManager manager, final float x, final float y);
    }

    public enum SimpleAction {
        /**
         * Add a node without merging with nearby elements, start the PropertyEditor with the Preset tab
         */
        NODE_TAGS(R.string.menu_add_node_tags, R.string.menu_add_node_tags, R.string.add_node_instruction, (main, manager, x, y) -> {
            Node node = App.getLogic().performAddNode(main, x, y);
            main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, node));
            main.performTagEdit(node, null, false, true);
        }) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }
        },
        /**
         * Add an address node with with nearby elements, start the PropertyEditor with the Preset tab
         */
        ADDRESS_NODE(R.string.menu_add_node_address, R.string.menu_add_node_address, R.string.simple_add_node, (main, manager, x, y) -> {
            App.getLogic().performAdd(main, x, y);
            Node node = App.getLogic().getSelectedNode();
            main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, node));
            main.performTagEdit(node, null, true, false);
        }) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }
        },
        /**
         * Add a way starting the normal path creation mode
         */
        WAY(R.string.menu_add_way, R.string.menu_add_way, R.string.add_way_start_instruction,
                (main, manager, x, y) -> main.startSupportActionMode(new PathCreationActionModeCallback(manager, x, y))) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }

            @Override
            public void addMenuItems(EasyEditManager manager, Context ctx, Menu menu) {
                boolean snap = App.getLogic().getPrefs().isWaySnapEnabled();
                PathCreationActionModeCallback.addSnapCheckBox(ctx, menu, snap,
                        (CompoundButton buttonView, boolean isChecked) -> App.getLogic().getPrefs().enableWaySnap(isChecked));
            }
        },
        /**
         * Add a way starting the normal path creation mode
         */
        INTERPOLATION_WAY(R.string.menu_add_address_interpolation, R.string.menu_add_address_interpolation, R.string.add_way_start_instruction,
                (main, manager, x, y) -> main.startSupportActionMode(new AddressInterpolationActionModeCallback(manager, x, y))) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }
        },
        /**
         * Add a note
         */
        NOTE(R.string.menu_add_map_note, R.string.menu_add_map_note, R.string.simple_add_note, (main, manager, x, y) -> {
            manager.finish();
            de.blau.android.layer.tasks.MapOverlay layer = main.getMap().getTaskLayer();
            if (layer == null) { // turn it on, this is supposed to be "simple"
                de.blau.android.layer.Util.addLayer(main, LayerType.TASKS);
                main.getMap().setUpLayers(main);
                layer = main.getMap().getTaskLayer();
                if (layer == null) {
                    ScreenMessage.toastTopError(main, R.string.toast_unable_to_create_task_layer);
                    return;
                }
                main.getMap().invalidate();
            }
            NoteFragment.showDialog(main, App.getLogic().makeNewNote(x, y));
        }) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }
        },
        /**
         * Add a node, merging with nearby elements
         */
        NODE(R.string.menu_add_node, R.string.menu_add_node, R.string.simple_add_node, (main, manager, x, y) -> {
            try {
                App.getLogic().performAdd(main, x, y);
                main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, App.getLogic().getSelectedNode()));
            } catch (OsmIllegalOperationException | StorageException e) {
                // this will have already been messaged
            }
        }) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }
        },
        /**
         * Add node from voice input
         */
        VOICE_NODE(R.string.menu_add_node_tags, R.string.menu_add_node_tags, R.string.simple_add_node, (main, manager, x, y) -> {
            Logic logic = App.getLogic();
            Commands.startVoiceRecognition(main, Main.VOICE_RECOGNITION_REQUEST_CODE, logic.xToLonE7(x), logic.yToLatE7(y));
        }) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }
        },
        /**
         * Add a note
         */
        VOICE_NOTE(R.string.menu_add_map_note, R.string.menu_add_map_note, R.string.simple_add_note, (main, manager, x, y) -> {
            Logic logic = App.getLogic();
            Commands.startVoiceRecognition(main, Main.VOICE_RECOGNITION_NOTE_REQUEST_CODE, logic.xToLonE7(x), logic.yToLatE7(y));
        }) {
            @Override
            public boolean isEnabled() {
                return App.getLogic().getMode().enabledSimpleActions().contains(this);
            }
        },
        /**
         * Paste an object from the clipboard
         */
        PASTE(R.string.menu_paste_object, R.string.menu_paste_object, R.string.simple_paste, SimpleActionModeCallback::paste) {
            @Override
            public boolean isEnabled() {
                return !App.getLogic().clipboardIsEmpty();
            }

            @Override
            public void addMenuItems(@NonNull EasyEditManager manager, @NonNull Context ctx, @NonNull Menu menu) {
                setUpClipboardButtons(manager, ctx, menu);
            }
        },
        /**
         * Paste an object from the clipboard multiple times
         */
        PASTEMULTIPLE(R.string.menu_paste_multiple, R.string.menu_paste_multiple, R.string.simple_paste_multiple, (main, manager, x, y) -> {
            App.getLogic().pasteFromClipboard(main, 0, x, y);
            main.startSupportActionMode(new PasteMultipleActionModeCallback(manager));
        }) {
            @Override
            public boolean isEnabled() {
                return !App.getLogic().clipboardIsEmpty();
            }

            @Override
            public void addMenuItems(@NonNull EasyEditManager manager, @NonNull Context ctx, @NonNull Menu menu) {
                setUpClipboardButtons(manager, ctx, menu);
            }
        };

        final int                  menuTextId;
        final int                  titleId;
        final int                  subTitleId;
        final SimpleActionCallback actionCallback;

        /**
         * Construct a SimpleAction
         * 
         * @param menuTextId resource for the menu text
         * @param titleId resource for the text displayed in the ActionMode
         * @param subTitleId a sub title, typically used for instructions
         * @param actionCallback callback with actual code for the action
         */
        SimpleAction(final int menuTextId, final int titleId, int subTitleId, @NonNull final SimpleActionCallback actionCallback) {
            this.menuTextId = menuTextId;
            this.titleId = titleId;
            this.subTitleId = subTitleId;
            this.actionCallback = actionCallback;
        }

        /**
         * Get the resource id for the menu text
         * 
         * @return the resource id for the menu text
         */
        public int getMenuTextId() {
            return menuTextId;
        }

        /**
         * Get the resource id for the ActionMode text
         * 
         * @return the resource id for the ActionMode text
         */
        public int getTitleTextId() {
            return titleId;
        }

        /**
         * Execute the callback with the code for the action
         * 
         * @param main the current Main instance
         * @param manager the current EasyEditManager
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        public void execute(@NonNull final Main main, @NonNull final EasyEditManager manager, final float x, final float y) {
            actionCallback.action(main, manager, x, y);
        }

        /**
         * Check if this entry should be displayed
         * 
         * @return true if enabled
         */
        public boolean isEnabled() {
            return true;
        }

        /**
         * Add one or more menu items to the initial menu
         * 
         * @param manager the current EasyEditManager
         * @param ctx an Android Context
         * @param menu the Menu we append the items too
         */
        public void addMenuItems(@NonNull final EasyEditManager manager, @NonNull Context ctx, @NonNull Menu menu) {
            // nothing
        }
    }

    private final SimpleAction simpleAction;

    /**
     * Construct a callback for when a simple action has been selected
     * 
     * @param manager the EasyEditManager instance
     * @param simpleMode Enum indicating what to do
     */
    public SimpleActionModeCallback(@NonNull EasyEditManager manager, @NonNull SimpleAction simpleMode) {
        super(manager);
        this.simpleAction = simpleMode;
    }

    public void invalidate() {
        manager.invalidate();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_simple_actions;
        super.onCreateActionMode(mode, menu);
        mode.setTitle(simpleAction.titleId);
        if (simpleAction.subTitleId != -1) {
            mode.setSubtitle(simpleAction.subTitleId);
        } else {
            mode.setSubtitle(null);
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.clear();
        menuUtil.reset();
        simpleAction.addMenuItems(manager, main, menu);
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return true;
    }

    @Override
    public boolean handleClick(float x, float y) {
        simpleAction.execute(main, manager, x, y);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        logic.setSelectedRelation(null);
        main.getMap().deselectObjects();
        super.onDestroyActionMode(mode);
    }

    @Override
    public boolean onMenuItemClick(MenuItem arg0) {
        return false;
    }

    /**
     * Get a menu suitable for display by clicking on a button
     * 
     * @param main the current instance of Main
     * @param anchor the anchor View for the popup
     * @return a PopupMenu instance
     */
    @NonNull
    public static PopupMenu getMenu(@NonNull Main main, @NonNull View anchor) {
        PopupMenu popup = new PopupMenu(main, anchor);
        for (SimpleAction simpleMode : SimpleAction.values()) {
            if (simpleMode.isEnabled()) {
                MenuItem item = popup.getMenu().add(simpleMode.getMenuTextId());
                item.setOnMenuItemClickListener(arg0 -> {
                    main.startSupportActionMode(new SimpleActionModeCallback(main.getEasyEditManager(), simpleMode));
                    return true;
                });
            }
        }
        return popup;
    }

    /**
     * Paste objects to a location
     * 
     * @param activity optional calling Activity
     * @param manager the current EasyEditManager
     * @param x screen x
     * @param y screen y
     */
    public static void paste(@Nullable final Activity activity, @NonNull final EasyEditManager manager, final float x, final float y) {
        List<OsmElement> elements = App.getLogic().pasteFromClipboard(activity, 0, x, y);
        if (elements != null && !elements.isEmpty()) {
            if (elements.size() > 1) {
                manager.finish();
                App.getLogic().setSelection(elements);
                manager.editElements();
            } else {
                manager.editElement(elements.get(0));
            }
        } else {
            manager.finish();
        }
    }

    /**
     * Set up the buttons representing clipboards
     * 
     * @param manager the current EasyEditManager instance
     * @param ctx an Android Context
     * @param menu the Menu to add the buttons to
     */
    static void setUpClipboardButtons(@NonNull EasyEditManager manager, @NonNull Context ctx, @NonNull Menu menu) {
        final List<ClipboardStorage> clipboards = App.getDelegator().getClipboards();
        final Drawable bgEmpty = AppCompatResources.getDrawable(ctx, R.drawable.clipboard_bg);
        final Drawable bgOrange = AppCompatResources.getDrawable(ctx, R.drawable.clipboard_bg_orange);
        int count = 0;
        for (ClipboardStorage clipboard : clipboards) {
            final int c = count;
            MenuItem item = menu.add(Integer.toString(count + 1)).setOnMenuItemClickListener((MenuItem menuItem) -> {
                for (int j = 0; j < menu.size(); j++) {
                    MenuItem mi = menu.getItem(j);
                    setIconBackground(mi, bgEmpty);
                }
                setIconBackground(menuItem, bgOrange);
                ((MRUList<ClipboardStorage>) clipboards).push(clipboards.get(c));
                manager.invalidate();
                return true;
            });
            Drawable icon = clipboard.getIcon(ctx);
            if (icon == ClipboardStorage.NO_ICON) {
                icon = new NumberDrawable(ctx);
                ((NumberDrawable) icon).setNumber(count + 1);
            }
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] { bgEmpty, icon });
            layerDrawable.setId(0, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                layerDrawable.setLayerGravity(0, Gravity.CENTER);
                layerDrawable.setLayerGravity(ICON_LAYER, Gravity.CENTER);
            }
            layerDrawable.setLayerInset(ICON_LAYER, ICON_INSET, ICON_INSET, ICON_INSET, ICON_INSET);
            item.setIcon(layerDrawable);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            if (count == 0) {
                setIconBackground(item, bgOrange);
            }
            count++;
        }
    }

    /**
     * Set the background image for a clipboard button
     * 
     * @param item the MenuItem
     * @param bg the background Drawable
     */
    private static void setIconBackground(@NonNull MenuItem item, @NonNull Drawable bg) {
        Drawable drawable = item.getIcon();
        if (drawable instanceof LayerDrawable) {
            ((LayerDrawable) drawable).setDrawableByLayerId(0, bg);
            drawable.invalidateSelf();
        }
    }
}
