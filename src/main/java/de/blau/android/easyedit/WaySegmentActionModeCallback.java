package de.blau.android.easyedit;

import java.util.HashSet;
import java.util.List;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.Geometry;
import de.blau.android.util.Util;

public class WaySegmentActionModeCallback extends NonSimpleActionModeCallback {

    private final Way way;
    private float     x = -Float.MAX_VALUE;
    private float     y = -Float.MAX_VALUE;

    /**
     * Construct a new WayMergingActionModeCallback from an existing Way and potentially mergable Ways
     * 
     * @param manager the current EasyEditManager instance
     * @param way the existing Way
     */
    public WaySegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way) {
        super(manager);
        this.way = way;
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

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid ways can be
                                                            // clicked
        super.handleElementClick(element);
        // race conditions with touch events seem to make the impossible possible
        // TODO fix properly
        if (!(element instanceof Way) || x == -Float.MAX_VALUE || y == -Float.MAX_VALUE) {
            return false;
        }
        List<Node> wayNodes = way.getNodes();

        float node1X = Float.MAX_VALUE;
        float node1Y = Float.MAX_VALUE;
        Node node1 = null;
        Node node2 = null;
        double distance = -1D;
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

            distance = Geometry.isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y);
            if (distance >= 0) {
                break;
            }
            node1X = node2X;
            node1Y = node2Y;
        }

        if (distance >= 0 && node1 != null) {
            List<Result> result = logic.performExtractSegment(main, way, node1, node2);
            if (result != null) {
                checkSplitResult(way, result);
                Way segment = newWayFromSplitResult(result);
                if (segment.hasTagKey(Tags.KEY_HIGHWAY) || segment.hasTagKey(Tags.KEY_WATERWAY)) {
                    main.startSupportActionMode(new WaySegmentModifyActionModeCallback(manager, segment));
                } else {
                    main.startSupportActionMode(new WaySelectionActionModeCallback(manager, segment));
                }
            }
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
