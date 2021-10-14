package de.blau.android.easyedit.route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.TagConflictDialog;
import de.blau.android.easyedit.BuilderActionModeCallback;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.ElementSelectionActionModeCallback;
import de.blau.android.easyedit.RelationSelectionActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Result;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

public class RouteSegmentActionModeCallback extends BuilderActionModeCallback {

    private static final String DEBUG_TAG = "RouteSegment...";

    static final String SEGMENT_IDS_KEY = "segment ids";
    static final String ROUTE_ID_KEY    = "route id";

    private static final int MENUITEM_REVERT = 1;

    private MenuItem revertItem = null;

    private final List<Way>       segments = new ArrayList<>();
    private final Set<OsmElement> potentialSegments;
    private int                   titleId  = R.string.actionmode_add_segment;
    private Relation              route    = null;

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public RouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        List<Long> ids = state.getList(SEGMENT_IDS_KEY);
        StorageDelegator delegator = App.getDelegator();
        for (Long id : ids) {
            Way segment = (Way) delegator.getOsmElement(Way.NAME, id);
            if (segment != null) {
                segments.add(segment);
            } else {
                throw new IllegalStateException("Failed to find segment " + id);
            }
        }
        potentialSegments = findViaElements(segments.get(segments.size() - 1));
        Long routeId = state.getLong(ROUTE_ID_KEY);
        if (routeId != null) {
            route = (Relation) delegator.getOsmElement(Relation.NAME, routeId);
        }
    }

    /**
     * Construct a new callback for adding segments to a route
     * 
     * @param manager the current EasyEditManager instance
     * @param way the "from" role Way
     * @param potentialSegments potential further segments
     * 
     */
    public RouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Set<OsmElement> potentialSegments) {
        super(manager);
        Log.d(DEBUG_TAG, "Restarting");
        segments.add(way);
        this.potentialSegments = potentialSegments;
    }

    /**
     * Construct a new callback for adding segments to a route
     * 
     * @param manager the current EasyEditManager instance
     * @param titleId the resource id for an alternative title
     * @param way the "from" role Way
     * @param potentialSegments potential further segments
     */
    public RouteSegmentActionModeCallback(@NonNull EasyEditManager manager, int titleId, @NonNull Way way, @NonNull Set<OsmElement> potentialSegments) {
        this(manager, way, potentialSegments);
        this.titleId = titleId;
    }

    /**
     * Construct a new callback for adding segments to a route
     * 
     * @param manager the current EasyEditManager instance
     * @param way the Way where we start appending
     * @param route the route the way will be added to
     * @param potentialSegments potential further segments
     */
    public RouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Relation route,
            @NonNull Set<OsmElement> potentialSegments, @Nullable Map<OsmElement, Result> results) {
        this(manager, way, potentialSegments);
        this.route = route;
        if (results != null) {
            this.savedResults = results;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_add_route_segment;
        mode.setTitle(titleId);
        logic.setClickableElements(potentialSegments);
        logic.setReturnRelations(false);
        logic.setSelectedRelationWays(null); // just to be safe
        if (route != null) {
            for (RelationMember rm : route.getMembers()) {
                if (Way.NAME.equals(rm.getType()) && rm.downloaded()) {
                    logic.addSelectedRelationWay((Way) rm.getElement());
                }
            }
        }
        logic.addSelectedRelationWay(segments.get(segments.size() - 1));
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);

        // menu setup
        super.onCreateActionMode(mode, menu);
        menu = replaceMenu(menu, mode, this);
        revertItem = menu.add(Menu.NONE, MENUITEM_REVERT, Menu.NONE, R.string.tag_menu_revert)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo));

        arrangeMenu(menu); // needed at least once
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        logic.setClickableElements(findViaElements(segments.get(segments.size() - 1), false));
        if (route != null) {
            // new ways might be downloaded
            for (RelationMember rm : route.getMembers()) {
                if (Way.NAME.equals(rm.getType()) && rm.downloaded()) {
                    List<Way> relationWays = logic.getSelectedRelationWays();
                    if (relationWays != null && !relationWays.contains(rm.getElement())) {
                        logic.addSelectedRelationWay((Way) rm.getElement());
                    }
                }
            }
        }
        // menu setup
        menu = replaceMenu(menu, mode, this);
        boolean updated = super.onPrepareActionMode(mode, menu);
        updated |= ElementSelectionActionModeCallback.setItemVisibility(segments.size() > 1, revertItem, false);
        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item) && MENUITEM_REVERT == item.getItemId() && segments.size() > 1) {
            Log.d(DEBUG_TAG, "Reverting last segment addition");
            Way lastSegment = segments.remove(segments.size() - 1);
            logic.removeSelectedRelationWay(lastSegment);
            logic.setClickableElements(findViaElements(segments.get(segments.size() - 1), true));
            main.invalidateMap();
            manager.invalidate();
            return true;
        }
        return false;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid elements can be clicked
        super.handleElementClick(element);
        List<Way> relationWays = logic.getSelectedRelationWays();
        if (relationWays != null && relationWays.contains(element)) {
            new AlertDialog.Builder(main).setTitle(R.string.duplicate_route_segment_title).setMessage(R.string.duplicate_route_segment_message)
                    .setPositiveButton(R.string.duplicate_route_segment_button, (dialog, which) -> addSegment(element)).setNeutralButton(R.string.cancel, null)
                    .show();
        } else {
            if (addSegment(element)) {
                manager.invalidate();
            }
        }
        return true;
    }

    /**
     * Add the selected element as a segment
     * 
     * In the simplest case this selects the next segment, in the worst it splits both the current and the next segment
     * and restarts the process.
     * 
     * @param element the clicked OSM element
     * @return true is a segment was added
     */
    private boolean addSegment(@NonNull OsmElement element) {
        // check if we have to split from or via
        final int size = segments.size();
        Way currentSegment = segments.get(size - 1);
        Way nextSegment = null;
        Node commonNode = null;
        if (Way.NAME.equals(element.getName())) {
            nextSegment = (Way) element;
            commonNode = currentSegment.getCommonNode(nextSegment);
        } else {
            // This shouldn't happen
            Log.e(DEBUG_TAG, element.getName() + " clicked");
            return false;
        }

        Way newCurrentSegment = null;
        if (!currentSegment.isClosed() && !currentSegment.getFirstNode().equals(commonNode) && !currentSegment.getLastNode().equals(commonNode)) {
            // split from at node
            List<Result> result = logic.performSplit(main, currentSegment, commonNode);
            newCurrentSegment = newWayFromSplitResult(result);
            saveSplitResult(currentSegment, result);
            if (size > 1) { // not the first segment
                // keep the bit that connects to previous segment
                Way prevSegment = segments.get(size - 2);
                if (prevSegment.getFirstNode().equals(currentSegment.getFirstNode()) || prevSegment.getFirstNode().equals(currentSegment.getLastNode())
                        || prevSegment.getLastNode().equals(currentSegment.getFirstNode()) || prevSegment.getLastNode().equals(currentSegment.getLastNode())) {
                    newCurrentSegment = null;
                } else {
                    segments.set(size - 1, newCurrentSegment);
                    currentSegment = newCurrentSegment;
                    newCurrentSegment = null;
                }
                logic.setSelectedRelationWays(segments);
            }
        }
        Way newNextSegment = null;
        if (!nextSegment.isClosed() && !nextSegment.getFirstNode().equals(commonNode) && !nextSegment.getLastNode().equals(commonNode)) {
            List<Result> result = logic.performSplit(main, nextSegment, commonNode);
            newNextSegment = newWayFromSplitResult(result);
            saveSplitResult(nextSegment, result);
        } else if (newCurrentSegment == null) {
            segments.add(nextSegment);
            logic.setSelectedRelationWays(segments);
            currentSegment = nextSegment;
            logic.setClickableElements(findViaElements(currentSegment, true));
            return true;
        }

        if (newCurrentSegment != null) {
            Set<Way> fromElements = new HashSet<>();
            fromElements.add(currentSegment);
            fromElements.add(newCurrentSegment);
            Snack.barInfo(main, newNextSegment == null ? R.string.toast_split_first_segment : R.string.toast_split_first_and_next_segment);
            main.startSupportActionMode(new RestartRouteSegmentActionModeCallback(manager, fromElements, route, savedResults));
            return false;
        }
        if (newNextSegment != null) {
            Snack.barInfo(main, R.string.toast_split_next_segment);
            logic.setClickableElements(findViaElements(currentSegment, false));
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        logic.setSelectedRelationWays(null);
        logic.setSelectedRelationNodes(null);
        super.onDestroyActionMode(mode);
    }

    @Override
    public void finishBuilding() {
        List<OsmElement> elements = new ArrayList<>();
        for (Way w : segments) {
            elements.add(w);
        }
        if (route != null) {
            logic.addMembers(main, route, elements);
        } else {
            route = logic.createRelation(main, Tags.VALUE_ROUTE, elements);
        }
        segments.clear();
        main.performTagEdit(route, Tags.VALUE_ROUTE, false, false);
        main.startSupportActionMode(new RelationSelectionActionModeCallback(manager, route));
        if (!savedResults.isEmpty()) {
            TagConflictDialog.showDialog(main, new ArrayList<>(savedResults.values()));
        }
    }

    @Override
    public void saveState(SerializableState state) {
        List<Long> segmentIds = new ArrayList<>();
        for (Way w : segments) {
            segmentIds.add(w.getOsmId());
        }
        state.putList(SEGMENT_IDS_KEY, segmentIds);
        if (route != null) {
            state.putLong(ROUTE_ID_KEY, route.getOsmId());
        }
    }

    @Override
    protected boolean hasData() {
        return !segments.isEmpty();
    }
}
