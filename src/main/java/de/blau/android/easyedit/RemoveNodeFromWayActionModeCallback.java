package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.util.Snack;

public class RemoveNodeFromWayActionModeCallback extends NonSimpleActionModeCallback {
    private static final String DEBUG_TAG = "RemoveNode...";

    private final Way        way;
    private List<OsmElement> nodes = new ArrayList<>();

    /**
     * Construct a RemoveNodeFromWayActionModeCallback from an existing Way
     * 
     * @param manager the current EasyEditMAnager instance
     * @param way the existing Way
     */
    public RemoveNodeFromWayActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way) {
        super(manager);
        this.way = way;
        nodes.addAll(way.getNodes());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_wayselection;
        super.onCreateActionMode(mode, menu);
        mode.setSubtitle(R.string.menu_remove_node_from_way);
        logic.setClickableElements(new HashSet<>(nodes));
        logic.setReturnRelations(false);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be
                                                            // clicked
        super.handleElementClick(element);
        // protect against race conditions
        if (!(element instanceof Node)) {
            // TODO fix properly
            return false;
        }
        try {
            logic.performRemoveNodeFromWay(main, way, (Node) element);
        } catch (OsmIllegalOperationException oloex) {
            Log.e(DEBUG_TAG, "Tried to remove node from way " + way.getOsmId() + " #nodes " + way.getNodes().size() + " cloased " + way.isClosed());
            Snack.toastTopError(main, oloex.getMessage()); // this should never happen
        }
        manager.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
