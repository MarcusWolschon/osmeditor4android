package de.blau.android.easyedit;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class SimpleActionModeCallback extends EasyEditActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {
    private static final String DEBUG_TAG = "SimpleActionMode...";

    interface SimpleActionCallback {
        /**
         * Executes the actual code associated with a SimpleAction
         * 
         * @param main the current Main instance
         * @param manager the current EasyEditManager
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        void action(final Main main, final EasyEditManager manager, final float x, final float y);
    }

    public enum SimpleAction {
        /**
         * Add a node without merging with nearby elements, start the PropertyEditor with the Preset tab
         */
        NODE_TAGS(R.string.menu_add_node_tags, R.string.simple_add_node, new SimpleActionCallback() {

            @Override
            public void action(final Main main, final EasyEditManager manager, final float x, final float y) {
                de.blau.android.Map map = main.getMap();
                ViewBox box = map.getViewBox();
                int width = map.getWidth();
                int height = map.getHeight();
                Node node = App.getLogic().performAddNode(main, GeoMath.xToLonE7(width, box, x) / 1E7D, GeoMath.yToLatE7(height, width, box, y) / 1E7D);
                main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, node));
                main.performTagEdit(node, null, false, true, false);

            }
        }),
        /**
         * Add a way starting the normal path creation mode
         */
        WAY(R.string.menu_add_way, R.string.simple_add_way, new SimpleActionCallback() {

            @Override
            public void action(final Main main, final EasyEditManager manager, final float x, final float y) {
                main.startSupportActionMode(new PathCreationActionModeCallback(manager, x, y));
            }
        }),
        /**
         * Add a note
         */
        NOTE(R.string.menu_add_map_note, R.string.simple_add_note, new SimpleActionCallback() {

            @Override
            public void action(final Main main, final EasyEditManager manager, final float x, final float y) {
                manager.finish();
                de.blau.android.layer.tasks.MapOverlay layer = main.getMap().getTaskLayer();
                if (layer == null) { // turn it on, this is supposed to be "simple"
                    Preferences prefs = new Preferences(main);
                    prefs.setBugsEnabled(true);
                    main.updatePrefs(prefs);
                    App.getLogic().getMap().setPrefs(main, prefs);
                    layer = main.getMap().getTaskLayer();
                    if (layer == null) {
                        Snack.toastTopError(main, R.string.toast_unable_to_create_task_layer);
                        return;
                    }
                    main.getMap().invalidate();
                }
                Note note = App.getLogic().makeNewNote(x, y);
                TaskFragment.showDialog(main, note);
            }
        }),
        /**
         * Add a node merging with nearby elements
         */
        NODE(R.string.menu_add_node, R.string.simple_add_node, new SimpleActionCallback() {

            @Override
            public void action(final Main main, final EasyEditManager manager, final float x, final float y) {
                App.getLogic().performAdd(main, x, y);
                main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, App.getLogic().getSelectedNode()));
            }
        });

        final int                  menuTextId;
        final int                  titleId;
        final SimpleActionCallback actionCallback;

        /**
         * Construct a SimpleAction
         * 
         * @param menuTextId resource for the menu text
         * @param titleId resource for the text displayed in the ActionMode
         * @param actionCallback callback with actual code for the action
         */
        SimpleAction(final int menuTextId, final int titleId, @NonNull final SimpleActionCallback actionCallback) {
            this.menuTextId = menuTextId;
            this.titleId = titleId;
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
        public void execute(final Main main, final EasyEditManager manager, final float x, final float y) {
            actionCallback.action(main, manager, x, y);
        }
    }

    private final SimpleAction simpleAction;

    /**
     * Construct a callback for when a long click has occurred
     * 
     * @param manager the EasyEditManager instance
     * @param simpleMode Enum indicating what to do
     */
    public SimpleActionModeCallback(@NonNull EasyEditManager manager, @NonNull SimpleAction simpleMode) {
        super(manager);
        this.simpleAction = simpleMode;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_simple_actions;
        super.onCreateActionMode(mode, menu);
        mode.setTitle(simpleAction.titleId);
        mode.setSubtitle(null);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.clear();
        menuUtil.reset();
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help))
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
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
}
