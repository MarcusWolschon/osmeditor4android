package io.vespucci.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Way;
import io.vespucci.util.ScreenMessage;

public class RemoveNodeFromWayActionModeCallback extends NonSimpleActionModeCallback {
    private static final String DEBUG_TAG = RemoveNodeFromWayActionModeCallback.class.getSimpleName().substring(0, Math.min(23, RemoveNodeFromWayActionModeCallback.class.getSimpleName().length()));

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
            Log.e(DEBUG_TAG, "Unexpected element clicked " + element);
            return false;
        }
        Node node = (Node) element;
        if (node.hasParentRelations() && !node.hasTags() && logic.getWaysForNode(node).size() <= 1) {
            // node will be deleted
            new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deletenode_relation_description)
                    .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.deletenode, (dialog, which) -> remove(node)).show();
        } else {
            remove(node);
        }
        if (OsmElement.STATE_DELETED == way.getState()) {
            manager.finish();
        } else {
            manager.editElement(way);
        }
        return true;
    }

    /**
     * Remove node from the way
     * 
     * @param node the Node
     */
    private void remove(@NonNull Node node) {
        try {
            logic.performRemoveNodeFromWay(main, way, node);
        } catch (OsmIllegalOperationException oloex) {
            Log.e(DEBUG_TAG, "Tried to remove node from way " + way.getOsmId() + " #nodes " + way.getNodes().size() + " cloased " + way.isClosed());
            ScreenMessage.toastTopError(main, oloex.getMessage()); // this should never happen
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
