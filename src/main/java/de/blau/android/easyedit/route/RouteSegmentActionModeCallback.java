package de.blau.android.easyedit.route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.easyedit.RelationSelectionActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

public class RouteSegmentActionModeCallback extends NonSimpleActionModeCallback {
    private static final String   DEBUG_TAG       = "RouteSegment...";

    private static final int      MENUITEM_REVERT = 1;

    private MenuItem              revertItem      = null;

    private final List<Way>       segments        = new ArrayList<>();
    private final Set<OsmElement> viaElements;
    private boolean               segmentSelected = true;
    private int                   titleId         = R.string.actionmode_restriction_via;
    private Relation              route           = null;

    /**
     * Construct a new callback for determining the from element of a turn restriction
     * 
     * @param manager
     *            the current EasyEditManager instance
     * @param way
     *            the "from" role Way
     * @param vias
     *            potential "via" role elements
     */
    public RouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way,
            @NonNull Set<OsmElement> vias) {
        super(manager);
        segments.add(way);
        viaElements = vias;
    }

    /**
     * Construct a new callback for determining the from element of a turn restriction
     * 
     * @param manager
     *            the current EasyEditManager instance
     * @param titleId
     *            the resource id for an alternative title
     * @param way
     *            the "from" role Way
     * @param vias
     *            potential "via" role elements
     */
    public RouteSegmentActionModeCallback(@NonNull EasyEditManager manager, int titleId, @NonNull Way way,
            @NonNull Set<OsmElement> vias) {
        this(manager, way, vias);
        this.titleId = titleId;
    }

    /**
     * Construct a new callback for determining the from element of a turn restriction
     * 
     * @param manager
     *            the current EasyEditManager instance
     * @param way
     *            the "from" role Way
     * @param vias
     *            potential "via" role elements
     */
    public RouteSegmentActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Relation route,
            @NonNull Set<OsmElement> vias) {
        this(manager, way, vias);
        this.route = route;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_add_route_segment;
        mode.setTitle(R.string.actionmode_add_segment);
        logic.setClickableElements(viaElements);
        logic.setReturnRelations(false);
        logic.setSelectedRelationWays(null); // just to be safe
        if (route != null) {
            for (RelationMember rm : route.getMembers()) {
                if (Way.NAME.equals(rm.getType()) && rm.downloaded()) {
                    logic.addSelectedRelationWay((Way) rm.getElement());
                }
            }
        }
        logic.addSelectedRelationWay(segments.get(0));
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);

        // menu setup
        menu = replaceMenu(menu, mode, this);
        revertItem = menu.add(Menu.NONE, MENUITEM_REVERT, Menu.NONE, R.string.tag_menu_revert)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo));
        super.onCreateActionMode(mode, menu);
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
                    if (relationWays != null && rm.downloaded() && !relationWays.contains(rm.getElement())) {
                        logic.addSelectedRelationWay((Way) rm.getElement());
                    }
                }
            }
        }
        // menu setup
        menu = replaceMenu(menu, mode, this);
        boolean updated = super.onPrepareActionMode(mode, menu);
        revertItem.setEnabled(segments.size() > 1);
        return updated;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            switch (item.getItemId()) {
            case MENUITEM_REVERT: // remove last item in list
                if (segments.size() > 1) {
                    Way lastSegment = segments.remove(segments.size() - 1);
                    logic.removeSelectedRelationWay(lastSegment);
                    logic.setClickableElements(findViaElements(segments.get(segments.size() - 1), true));
                    main.invalidateMap();
                    item.setEnabled(segments.size() > 1);
                }
                break;
            default:
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid elements can be clicked
        super.handleElementClick(element);
        List<Way> relationWays = logic.getSelectedRelationWays();
        if (relationWays != null && relationWays.contains(element)) {
            new AlertDialog.Builder(main).setTitle(R.string.duplicate_route_segment_title)
                    .setMessage(R.string.duplicate_route_segment_message)
                    .setPositiveButton(R.string.duplicate_route_segment_button, (dialog, which) -> addSegment(element))
                    .setNeutralButton(R.string.cancel, null).show();
        } else {
            addSegment(element);
        }
        return true;
    }

    /**
     * Add the selected element as a segment
     * 
     * In the simplest case this selects the next segment, in the worst it splits both the current and the next segment and restarts the process.
     * 
     * @param element
     *            the clicked OSM element
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
            // FIXME show a warning
            Log.e(DEBUG_TAG, element.getName() + " clicked");
            return true;
        }

        Way newCurrentSegment = null;
        if (!currentSegment.isClosed() && !currentSegment.getFirstNode().equals(commonNode)
                && !currentSegment.getLastNode().equals(commonNode)) {
            // split from at node
            newCurrentSegment = logic.performSplit(main, currentSegment, commonNode);
            if (size > 1) { // not the first segment
                // keep the bit that connects to previous segment
                Way prevSegment = segments.get(size - 2);
                if (prevSegment.getFirstNode().equals(currentSegment.getFirstNode())
                        || prevSegment.getFirstNode().equals(currentSegment.getLastNode())
                        || prevSegment.getLastNode().equals(currentSegment.getFirstNode())
                        || prevSegment.getLastNode().equals(currentSegment.getLastNode())) {
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
        if (!nextSegment.isClosed() && !nextSegment.getFirstNode().equals(commonNode)
                && !nextSegment.getLastNode().equals(commonNode)) {
            newNextSegment = logic.performSplit(main, nextSegment, commonNode);
        } else if (newCurrentSegment == null) {
            segments.add(nextSegment);
            logic.setSelectedRelationWays(segments);
            currentSegment = nextSegment;
            logic.setClickableElements(findViaElements(currentSegment, true));
            return true;
        }

        if (newCurrentSegment != null) {
            Set<OsmElement> fromElements = new HashSet<>();
            fromElements.add(currentSegment);
            fromElements.add(newCurrentSegment);
            segmentSelected = false;
            Snack.barInfo(main, newNextSegment == null ? R.string.toast_split_first_segment : R.string.toast_split_first_and_next_segment);
            main.startSupportActionMode(new RestartRouteSegmentActionModeCallback(manager, fromElements));
            return true;
        }
        if (newNextSegment != null) {
            Snack.barInfo(main, R.string.toast_split_next_segment);
            logic.setClickableElements(findViaElements(currentSegment, false));
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);

        if (segmentSelected) {
            List<OsmElement> elements = new ArrayList<>();
            for (Way w : segments) {
                elements.add(w);
            }
            if (route != null) {
                logic.addMembers(main, route, elements);
            } else {
                route = logic.createRelation(main, Tags.VALUE_ROUTE, elements);
            }
            main.performTagEdit(route, Tags.VALUE_ROUTE, false, false);
            main.startSupportActionMode(new RelationSelectionActionModeCallback(manager, route));
        } else {
            logic.setSelectedRelationWays(null);
            logic.setSelectedRelationNodes(null);
            super.onDestroyActionMode(mode);
        }
    }
}
