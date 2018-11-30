package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

/**
 * Callback for splitting a closed way/polygon
 * 
 * @author simon
 *
 */
public class ClosedWaySplittingActionModeCallback extends NonSimpleActionModeCallback {
    private static final String   DEBUG_TAG      = "ClosedWaySplit...";
    private final Way             way;
    private final Node            node;
    private final Set<OsmElement> nodes          = new HashSet<>();    // nodes that we can use for splitting
    private boolean               createPolygons = false;

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
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_closedwaysplitting;
        super.onCreateActionMode(mode, menu);
        mode.setSubtitle(R.string.actionmode_closed_way_split_2);
        logic.setClickableElements(nodes);
        logic.setReturnRelations(false);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be
                                                            // clicked
        super.handleElementClick(element);
        if (element instanceof Node) {
            Way[] result = logic.performClosedWaySplit(main, way, node, (Node) element, createPolygons);
            if (result != null && result.length == 2) {
                logic.setSelectedNode(null);
                logic.setSelectedRelation(null);
                logic.setSelectedWay(result[0]);
                logic.addSelectedWay(result[1]);
                ArrayList<OsmElement> selection = new ArrayList<>();
                selection.addAll(logic.getSelectedWays());
                main.startSupportActionMode(new ExtendSelectionActionModeCallback(manager, selection));
                return true;
            }
        }
        // FIXME toast here?
        Log.d(DEBUG_TAG, "split failed at element " + (element != null ? element : "null"));
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
