package io.vespucci.easyedit;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.dialogs.AddressInterpolationDialog;
import io.vespucci.osm.Node;
import io.vespucci.osm.Way;
import io.vespucci.util.SerializableState;
import io.vespucci.util.ThemeUtils;

/**
 * This callback handles path creation for an address interpolation way.
 */
public class AddressInterpolationActionModeCallback extends PathCreationActionModeCallback {

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public AddressInterpolationActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager, state);
    }

    /**
     * Construct a new PathCreationActionModeCallback starting with screen coordinates
     * 
     * @param manager the current EasyEditManager instance
     * @param x screen x
     * @param y screen y
     */
    public AddressInterpolationActionModeCallback(@NonNull EasyEditManager manager, float x, float y) {
        super(manager, x, y);
    }

    /**
     * Construct a new PathCreationActionModeCallback starting with an existing Way and an existing Node to add
     * 
     * @param manager the current EasyEditManager instance
     * @param way the exiting Way
     * @param node the existing Node to add
     */
    public AddressInterpolationActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Node node) {
        super(manager, way, node);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        menu.add(Menu.NONE, MENUITEM_UNDO, Menu.NONE, R.string.undo).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo))
                .setVisible(!addedNodes.isEmpty());
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return true;
    }

    /**
     * Common code for finishing a path
     * 
     * @param way the way
     * @param node the node
     */
    @Override
    protected void finishPath(@Nullable final Way way, @Nullable final Node node) {
        manager.finish();
        removeCheckpoint();
        delayedResetHasProblem(way);
        if (way != null) {
            AddressInterpolationDialog.showDialog(main, way);
        }
    }
}
