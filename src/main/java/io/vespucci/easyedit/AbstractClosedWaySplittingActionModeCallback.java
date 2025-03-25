package io.vespucci.easyedit;

import java.util.HashSet;
import java.util.Set;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.osm.OsmElement;

/**
 * Common callback code for splitting a closed way/polygon
 * 
 * @author simon
 *
 */
public abstract class AbstractClosedWaySplittingActionModeCallback extends NonSimpleActionModeCallback {

    protected final Set<OsmElement> nodes = new HashSet<>(); // nodes that we can use for splitting

    /**
     * Construct a new callback
     * 
     * @param manager the current EasyEditManager instance
     */
    protected AbstractClosedWaySplittingActionModeCallback(@NonNull EasyEditManager manager) {
        super(manager);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_closedwaysplitting;
        super.onCreateActionMode(mode, menu);
        mode.setSubtitle(R.string.actionmode_closed_way_split_2);
        logic.setClickableElements(nodes);
        logic.setReturnRelations(false);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mode.setSubtitle("");
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
