package de.blau.android.easyedit.route;

import java.util.Set;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class RestartRouteSegmentActionModeCallback extends NonSimpleActionModeCallback {
    private static final String   DEBUG_TAG       = "RestartRoute...";
    private final Set<OsmElement> segmentWays;
    private boolean               segmentSelected = false;

    /**
     * Construct a new callback for determining the from element of a turn restriction from multiple Ways
     * 
     * @param manager
     *            the current EasyEditManager instance
     * @param segments
     *            potential "from" role Ways
     * @param vias
     *            potential "via" role elements
     */
    public RestartRouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Set<OsmElement> segments) {
        super(manager);
        segmentWays = segments;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_add_route_segment;
        mode.setTitle(R.string.actionmode_reselect_first_segment);
        logic.setClickableElements(segmentWays);
        logic.setReturnRelations(false);
        logic.setSelectedRelationWays(null);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be
                                                            // clicked
        super.handleElementClick(element);
        segmentSelected = true;
        main.startSupportActionMode(
                new RouteSegmentActionModeCallback(manager, (Way) element, findViaElements((Way) element, true)));
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        if (!segmentSelected) {
            logic.setSelectedRelationWays(null);
            logic.setSelectedRelationNodes(null);
        }
        super.onDestroyActionMode(mode);
    }
}
