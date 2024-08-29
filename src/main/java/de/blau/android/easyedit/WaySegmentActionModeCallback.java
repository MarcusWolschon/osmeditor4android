package de.blau.android.easyedit;

import java.util.HashSet;
import java.util.List;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.Geometry;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Util;

public class WaySegmentActionModeCallback extends NonSimpleActionModeCallback {

    private final Way way;
    private float     x = -Float.MAX_VALUE;
    private float     y = -Float.MAX_VALUE;

    /**
     * Construct a new WaySegmentActionModeCallback from an existing Way
     * 
     * @param manager the current EasyEditManager instance
     * @param way the existing Way
     */
    public WaySegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way) {
        super(manager);
        this.way = way;
    }

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public WaySegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        way = getSavedWay(state);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_extractsegment;
        mode.setTitle(R.string.menu_extract_segment);
        mode.setSubtitle(R.string.actionmode_extract_segment_select);
        logic.setClickableElements(new HashSet<>(Util.wrapInList(way)));
        logic.setReturnRelations(false);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleClick(float x, float y) {
        this.x = x;
        this.y = y;
        return false;
    }

    /**
     * Find a way "segment" from screen coordinates
     * 
     * Note this returns the first segment with the coordinates inside the tolerance values
     * 
     * @param wayNodes a list of way Nodes
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @return an array holding start and end Node of the segment or null
     */
    @NonNull
    static Node[] findSegmentFromCoordinates(@NonNull final List<Node> wayNodes, final float x, final float y) {
        Logic logic = App.getLogic();
        final float tolerance = logic.getMap().getDataStyle().getCurrent().getWayToleranceValue();
        Node node1 = null;
        Node node2 = null;

        float node1X = Float.MAX_VALUE;
        float node1Y = Float.MAX_VALUE;

        // Iterate over all WayNodes, but not the last one.
        for (int k = 0, wayNodesSize = wayNodes.size(); k < wayNodesSize - 1; ++k) {
            node1 = wayNodes.get(k);
            node2 = wayNodes.get(k + 1);
            if (node1X == Float.MAX_VALUE) {
                node1X = logic.lonE7ToX(node1.getLon());
                node1Y = logic.latE7ToY(node1.getLat());
            }
            float node2X = logic.lonE7ToX(node2.getLon());
            float node2Y = logic.latE7ToY(node2.getLat());
            if (Geometry.isPositionOnLine(tolerance, x, y, node1X, node1Y, node2X, node2Y) >= 0) {
                return new Node[] { node1, node2 };
            }
            node1X = node2X;
            node1Y = node2Y;
        }
        return new Node[] {};
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid ways can be
                                                            // clicked
        // race conditions with touch events seem to make the impossible possible
        if (!(element instanceof Way) || x == -Float.MAX_VALUE || y == -Float.MAX_VALUE) {
            return false;
        }
        List<Node> wayNodes = way.getNodes();
        Node[] segmentNodes = findSegmentFromCoordinates(wayNodes, x, y);
        if (segmentNodes.length == 2) {
            final Node n1 = segmentNodes[0];
            final Node n2 = segmentNodes[1];
            splitSafe(Util.wrapInList(way), () -> {
                try {
                    List<Result> result = logic.performExtractSegment(main, way, n1, n2);
                    checkSplitResult(way, result);
                    Way segment = newWayFromSplitResult(result);
                    if (segment.hasTagKey(Tags.KEY_HIGHWAY) || segment.hasTagKey(Tags.KEY_WATERWAY)) {
                        main.startSupportActionMode(new WaySegmentModifyActionModeCallback(manager, segment));
                    } else {
                        main.startSupportActionMode(new WaySelectionActionModeCallback(manager, segment));
                    }
                } catch (OsmIllegalOperationException | StorageException ex) {
                    // toast has already been displayed
                    manager.finish();
                }
            });
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }

    @Override
    public void saveState(SerializableState state) {
        state.putLong(WAY_ID_KEY, way.getOsmId());
    }
}
