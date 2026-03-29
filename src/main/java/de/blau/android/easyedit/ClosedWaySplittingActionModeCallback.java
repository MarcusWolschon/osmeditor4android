package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.dialogs.ElementIssueDialog;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Sound;
import de.blau.android.util.Util;

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
    private final Node first;
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
        this.first = node;
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
        first = getSavedNode(state);
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
            if (way.isEndNode(first)) { // we have at least 4 nodes so this will not cause problems
                nodes.remove(allNodes.get(1)); // remove 2nd element
                nodes.remove(allNodes.get(allNodes.size() - 2)); // remove 2nd last element
            } else {
                int nodeIndex = allNodes.indexOf(first);
                nodes.remove(allNodes.get(nodeIndex - 1));
                nodes.remove(allNodes.get(nodeIndex + 1));
            }
        }
        nodes.remove(first);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        mode.setSubtitle(R.string.actionmode_closed_way_split_long_click_2);
        logic.getClickableElements().add(way);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid nodes can be clicked
        super.handleElementClick(element);
        try {
            if (element instanceof Node) {
                split((Node) element);
                return true;
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            // toast has already been displayed
        }
        manager.finish();
        Log.d(DEBUG_TAG, "split failed at element " + (element != null ? element : "null"));
        return true;
    }

    /**
     * Split the way at both the first and second node
     * 
     * @param second the second node
     */
    private void split(@NonNull Node second) {
        List<Result> results = logic.performClosedWaySplit(main, way, first, second, createPolygons);
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
    }

    @Override
    public List<OsmElement> filterElementsLongClick(List<OsmElement> nodesAndWays) {
        return WaySplittingActionModeCallback.preferNodeOverWaysFilter(nodesAndWays);
    }

    @Override
    public boolean handleElementLongClick(@NonNull OsmElement element, float x, float y) {
        super.handleElementLongClick(element, x, y);
        if (way.equals(element)) {
            split(logic.addOnWay(main, Util.wrapInList(way), x, y, true));
            return true;
        }
        Sound.beep();
        return true;
    }

    @Override
    public boolean usesLongClick() {
        return true;
    }

    @Override
    public void saveState(SerializableState state) {
        state.putLong(WAY_ID_KEY, way.getOsmId());
        state.putLong(NODE_ID_KEY, first.getOsmId());
        state.putBoolean(CREATE_POLYGONS_KEY, createPolygons);
    }
}
