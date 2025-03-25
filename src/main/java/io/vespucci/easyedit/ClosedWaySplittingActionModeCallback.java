package io.vespucci.easyedit;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.dialogs.ElementIssueDialog;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.exception.StorageException;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Result;
import io.vespucci.osm.Way;
import io.vespucci.util.SerializableState;

/**
 * Callback for splitting a closed way/polygon
 * 
 * @author simon
 *
 */
public class ClosedWaySplittingActionModeCallback extends AbstractClosedWaySplittingActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ClosedWaySplittingActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = ClosedWaySplittingActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final String CREATE_POLYGONS_KEY = "create polygons";

    private final Way  way;
    private final Node node;
    private boolean    createPolygons = false;

    /**
     * Construct a new callback for splitting a closed way/polygon
     * 
     * @param manager the current EasyEditManager instance
     * @param way the closed way
     * @param node the first node to split at the callback will ask for the 2nd one
     * @param createPolygons create two polygons instead of unclosed ways if true
     */
    public ClosedWaySplittingActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Node node, boolean createPolygons) {
        super(manager);
        this.way = way;
        this.node = node;
        this.createPolygons = createPolygons;
        setup(createPolygons);
    }

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public ClosedWaySplittingActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        way = getSavedWay(state);
        node = getSavedNode(state);
        setup(state.getBoolean(CREATE_POLYGONS_KEY));
    }

    /**
     * Setup code that is common to both constructors
     * 
     * @param createPolygons if a closed way create polygons
     */
    private void setup(Boolean createPolygons) {
        List<Node> allNodes = way.getNodes();
        nodes.addAll(allNodes);
        this.createPolygons = createPolygons != null && createPolygons;
        if (this.createPolygons) { // remove neighbouring nodes
            if (way.isEndNode(node)) { // we have at least 4 nodes so this will not cause problems
                nodes.remove(allNodes.get(1)); // remove 2nd element
                nodes.remove(allNodes.get(allNodes.size() - 2)); // remove 2nd last element
            } else {
                int nodeIndex = allNodes.indexOf(node);
                nodes.remove(allNodes.get(nodeIndex - 1));
                nodes.remove(allNodes.get(nodeIndex + 1));
            }
        }
        nodes.remove(node);
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid nodes can be clicked
        super.handleElementClick(element);
        try {
            if (element instanceof Node) {
                List<Result> results = logic.performClosedWaySplit(main, way, node, (Node) element, createPolygons);
                logic.setSelectedNode(null);
                logic.setSelectedRelation(null);
                logic.setSelectedWay((Way) results.get(0).getElement());
                logic.addSelectedWay((Way) results.get(1).getElement());
                List<OsmElement> selection = new ArrayList<>();
                selection.addAll(logic.getSelectedWays());
                main.startSupportActionMode(new MultiSelectWithGeometryActionModeCallback(manager, selection));
                List<Result> resultsWithIssue = new ArrayList<>();
                for (Result r : results) {
                    if (r.hasIssue()) {
                        resultsWithIssue.add(r);
                    }
                }
                if (!resultsWithIssue.isEmpty()) {
                    ElementIssueDialog.showTagConflictDialog(main, resultsWithIssue);
                }
                return true;
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            // toast has already been displayed
        }
        manager.finish();
        Log.d(DEBUG_TAG, "split failed at element " + (element != null ? element : "null"));
        return true;
    }

    @Override
    public void saveState(SerializableState state) {
        state.putLong(WAY_ID_KEY, way.getOsmId());
        state.putLong(NODE_ID_KEY, node.getOsmId());
        state.putBoolean(CREATE_POLYGONS_KEY, createPolygons);
    }
}
