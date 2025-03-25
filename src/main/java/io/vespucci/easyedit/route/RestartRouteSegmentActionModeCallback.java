package io.vespucci.easyedit.route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.easyedit.EasyEditManager;
import io.vespucci.easyedit.NonSimpleActionModeCallback;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Result;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;
import io.vespucci.util.SerializableState;

public class RestartRouteSegmentActionModeCallback extends NonSimpleActionModeCallback {

    private final Set<Way> segmentWays;
    private boolean        segmentSelected = false;
    private final Relation route;

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public RestartRouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        segmentWays = new HashSet<>();
        List<Long> ids = state.getList(RouteSegmentActionModeCallback.SEGMENT_IDS_KEY);
        StorageDelegator delegator = App.getDelegator();
        for (Long id : ids) {
            Way segment = (Way) delegator.getOsmElement(Way.NAME, id);
            if (segment != null) {
                segmentWays.add(segment);
            } else {
                throw new IllegalStateException("Failed to find segment " + id);
            }
        }
        Long routeId = state.getLong(RouteSegmentActionModeCallback.ROUTE_ID_KEY);
        route = routeId != null ? (Relation) delegator.getOsmElement(Relation.NAME, routeId) : null;
    }

    /**
     * Construct a new callback for determining the from element of a turn restriction from multiple Ways
     * 
     * @param manager the current EasyEditManager instance
     * @param segments potential initial segments Ways
     * @param route if not null the route to add too
     * @param results results from way splitting
     */
    public RestartRouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Set<Way> segments, @Nullable Relation route,
            @Nullable Map<OsmElement, Result> results) {
        super(manager);
        segmentWays = segments;
        this.route = route;
        if (results != null) {
            this.savedResults = results;
        }
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
        main.descheduleAutoLock();
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) {
        super.handleElementClick(element);
        segmentSelected = true;
        main.startSupportActionMode(new RouteSegmentActionModeCallback(manager, (Way) element, route, findViaElements((Way) element, true), savedResults));
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        deselect(logic, !segmentSelected);
        super.onDestroyActionMode(mode);
    }

    @Override
    public void saveState(SerializableState state) {
        List<Long> segmentIds = new ArrayList<>();
        for (Way w : segmentWays) {
            segmentIds.add(w.getOsmId());
        }
        state.putList(RouteSegmentActionModeCallback.SEGMENT_IDS_KEY, segmentIds);
        if (route != null) {
            state.putLong(RouteSegmentActionModeCallback.ROUTE_ID_KEY, route.getOsmId());
        }
        // Note segmentSelected doesn't need to be save as it is only set on exiting
    }
}
