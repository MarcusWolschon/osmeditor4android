package de.blau.android.easyedit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Util;

public class WaySelectPartActionModeCallback extends AbortableWayActionModeCallback {

    private final Way  way;
    private final Node node;
    private float      x = -Float.MAX_VALUE;
    private float      y = -Float.MAX_VALUE;

    /**
     * Construct a new WaySelectPartActionModeCallback from an existing Way and splitting node
     * 
     * @param manager the current EasyEditManager instance
     * @param way the existing Way
     * @param node the splitting Node
     */
    public WaySelectPartActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Node node) {
        super(manager);
        this.way = way;
        this.node = node;
    }

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public WaySelectPartActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        way = getSavedWay(state);
        node = getSavedNode(state);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_waysplitting;
        mode.setTitle(R.string.actionmode_split_way);
        mode.setSubtitle(R.string.actionmode_split_way_select_part);
        logic.setReturnRelations(false);
        logic.setClickableElements(new HashSet<>(Arrays.asList(way, node)));
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleClick(float x, float y) {
        this.x = x;
        this.y = y;
        return false;
    }

    @Override
    public boolean handleElementClick(OsmElement element) {
        // race conditions with touch events seem to make the impossible possible
        if (!(element instanceof Way) || x == -Float.MAX_VALUE || y == -Float.MAX_VALUE) {
            return false;
        }
        List<Node> wayNodes = way.getNodes();
        Node[] segment = WaySegmentActionModeCallback.findSegmentFromCoordinates(wayNodes, x, y);
        if (segment.length == 2 && node != null) {
            final boolean fromEnd = wayNodes.indexOf(segment[1]) > wayNodes.indexOf(node);
            splitSafe(Util.wrapInList(way), () -> {
                try {
                    List<Result> result = logic.performSplit(main, way, node, fromEnd);
                    checkSplitResult(way, result);
                    manager.finish();
                    logic.setSelectedWay((Way) result.get(0).getElement());
                    manager.editElements();
                } catch (OsmIllegalOperationException | StorageException ex) {
                    // toast has already been displayed
                    manager.finish();
                }
            });
        }
        return true;
    }

    @Override
    public void saveState(SerializableState state) {
        state.putLong(WAY_ID_KEY, way.getOsmId());
        state.putLong(NODE_ID_KEY, node.getOsmId());
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
