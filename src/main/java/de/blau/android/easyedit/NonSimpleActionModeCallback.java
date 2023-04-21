package de.blau.android.easyedit;

import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.SerializableState;
import de.blau.android.util.ThemeUtils;

/**
 * Extend this class instead of EasyEditActionModeCallback if you need to disable the SimpleActionButton
 * 
 * @author simon
 *
 */
public class NonSimpleActionModeCallback extends EasyEditActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {

    protected static final String WAY_ID_KEY  = "way id";
    protected static final String NODE_ID_KEY = "node id";

    protected final Preferences prefs;

    /**
     * Construct a callback for that disables/enables the SimpleActionsButton
     * 
     * @param manager the EasyEditManager instance
     */
    public NonSimpleActionModeCallback(@NonNull EasyEditManager manager) {
        super(manager);
        prefs = App.getPreferences(main);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (prefs.areSimpleActionsEnabled()) {
            main.disableSimpleActionsButton();
        }
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.clear();
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        if (prefs.areSimpleActionsEnabled()) {
            main.enableSimpleActionsButton();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem arg0) {
        return false;
    }

    /**
     * Get a way from saved state
     * 
     * @param state the saved state
     */
    protected Way getSavedWay(@NonNull SerializableState state) {
        Long wayId = state.getLong(WAY_ID_KEY);
        if (wayId != null) {
            return (Way) App.getDelegator().getOsmElement(Way.NAME, wayId);
        } else {
            throw new IllegalStateException("Failed to find way " + wayId);
        }
    }

    /**
     * Get a node from saved state
     * 
     * @param state the saved state
     */
    protected Node getSavedNode(SerializableState state) {
        Long nodeId = state.getLong(NODE_ID_KEY);
        if (nodeId != null) {
            return (Node) App.getDelegator().getOsmElement(Node.NAME, nodeId);
        } else {
            throw new IllegalStateException("Failed to find node " + nodeId);
        }
    }
}
