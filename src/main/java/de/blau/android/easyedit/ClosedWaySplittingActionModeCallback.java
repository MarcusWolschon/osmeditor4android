package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

/**
 * Callback for splitting a closed way/polygon
 * 
 * @author simon
 *
 */
public class ClosedWaySplittingActionModeCallback extends AbstractClosedWaySplittingActionModeCallback {
    private static final String DEBUG_TAG      = "ClosedWaySplit...";
    private final Way           way;
    private final Node          node;
    private boolean             createPolygons = false;

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
        List<Node> allNodes = way.getNodes();
        nodes.addAll(allNodes);
        if (createPolygons) { // remove neighbouring nodes
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
                Way[] result = logic.performClosedWaySplit(main, way, node, (Node) element, createPolygons);
                if (result.length == 2) {
                    logic.setSelectedNode(null);
                    logic.setSelectedRelation(null);
                    logic.setSelectedWay(result[0]);
                    logic.addSelectedWay(result[1]);
                    List<OsmElement> selection = new ArrayList<>();
                    selection.addAll(logic.getSelectedWays());
                    main.startSupportActionMode(new ExtendSelectionActionModeCallback(manager, selection));
                    return true;
                }
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            // toast has already been displayed
        }
        manager.finish();
        Log.d(DEBUG_TAG, "split failed at element " + (element != null ? element : "null"));
        return true;
    }
}
