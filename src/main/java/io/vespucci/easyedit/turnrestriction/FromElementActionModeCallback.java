package io.vespucci.easyedit.turnrestriction;

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
import io.vespucci.easyedit.EasyEditManager;
import io.vespucci.easyedit.NonSimpleActionModeCallback;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.exception.StorageException;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Result;
import io.vespucci.osm.Way;
import io.vespucci.util.ScreenMessage;

public class FromElementActionModeCallback extends NonSimpleActionModeCallback {
    private static final String   DEBUG_TAG   = "FromElement...";
    private final Way             fromWay;
    private final Set<OsmElement> viaElements;
    private boolean               viaSelected = false;
    private int                   titleId     = R.string.actionmode_restriction_via;

    /**
     * Construct a new callback for determining the from element of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param way the "from" role Way
     * @param vias potential "via" role elements
     */
    public FromElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Set<OsmElement> vias) {
        super(manager);
        this.fromWay = way;
        viaElements = vias;
    }

    /**
     * Construct a new callback for determining the from element of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param titleId the resource id for an alternative title
     * @param way the "from" role Way
     * @param vias potential "via" role elements
     * @param results saved intermediate results
     */
    public FromElementActionModeCallback(@NonNull EasyEditManager manager, int titleId, @NonNull Way way, @NonNull Set<OsmElement> vias,
            @Nullable Map<OsmElement, Result> results) {
        this(manager, way, vias);
        this.titleId = titleId;
        if (results != null) {
            savedResults = results;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addingrestriction;
        mode.setTitle(titleId);
        logic.setClickableElements(viaElements);
        logic.setReturnRelations(false);
        logic.setSelectedRelationWays(null); // just to be safe
        logic.addSelectedRelationWay(fromWay);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    /**
     * In the simplest case this selects the next step in creating the restriction, in the worst it splits both the via
     * and from way and restarts the process.
     */
    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid nodes can be clicked
        super.handleElementClick(element);
        // check if we have to split from or via
        Node viaNode = null;
        Way viaWay = null;
        if (Node.NAME.equals(element.getName())) {
            viaNode = (Node) element;
        } else if (Way.NAME.equals(element.getName())) {
            viaWay = (Way) element;
            viaNode = fromWay.getCommonNode(viaWay);
        } else {
            unexpectedElement(DEBUG_TAG, element);
            return true;
        }
        if (viaWay != null && viaWay.equals(fromWay)) {
            // we need to split fromWay first
            main.startSupportActionMode(
                    new RestrictionWaySplittingActionModeCallback(manager, R.string.actionmode_restriction_split_from, fromWay, null, savedResults));
            return true;
        }

        final boolean fromNeedsSplit = !fromWay.isEndNode(viaNode);
        final boolean viaNeedsSplit = viaWay != null && !viaWay.isEndNode(viaNode);

        if (fromNeedsSplit || viaNeedsSplit) {
            List<Way> toSplit = new ArrayList<>();
            if (fromNeedsSplit) {
                toSplit.add(fromWay);
            }
            if (viaNeedsSplit) {
                toSplit.add(viaWay);
            }
            final Node splitNode = viaNode;
            final Way splitViaWay = viaWay;
            splitSafe(toSplit, () -> {
                try {
                    Way newFromWay = null;
                    if (fromNeedsSplit) {
                        // split from at node
                        List<Result> result = logic.performSplit(main, fromWay, splitNode, true);
                        newFromWay = newWayFromSplitResult(result);
                        saveSplitResult(fromWay, result);
                    }
                    Way newViaWay = null;
                    if (viaNeedsSplit) {
                        List<Result> result = logic.performSplit(main, splitViaWay, splitNode, true);
                        newViaWay = newWayFromSplitResult(result);
                        saveSplitResult(splitViaWay, result);
                    }
                    nextStep(element, newFromWay, newViaWay);
                } catch (OsmIllegalOperationException | StorageException ex) {
                    // toast has already been displayed
                    manager.finish();
                }
            });
        } else {
            nextStep(element, null, null);
        }
        return true;
    }

    /**
     * Continue with the next step in adding the restriction
     * 
     * @param element the original clicked via element
     * @param newFromWay a new from way or null
     * @param newViaWay a new via way or null
     */
    private void nextStep(@NonNull OsmElement element, @Nullable Way newFromWay, @Nullable Way newViaWay) {
        Set<OsmElement> newViaElements = new HashSet<>();
        newViaElements.add(element);
        if (newViaWay != null) {
            newViaElements.add(newViaWay);
        }
        if (newFromWay != null) {
            Set<OsmElement> fromElements = new HashSet<>();
            fromElements.add(fromWay);
            fromElements.add(newFromWay);
            ScreenMessage.barInfo(main, newViaWay == null ? R.string.toast_split_from : R.string.toast_split_from_and_via);
            main.startSupportActionMode(new RestartFromElementActionModeCallback(manager, fromElements, newViaElements, savedResults));
        } else if (newViaWay != null) {
            // restart via selection
            ScreenMessage.barInfo(main, R.string.toast_split_via);
            main.startSupportActionMode(
                    new FromElementActionModeCallback(manager, R.string.actionmode_restriction_restart_via, fromWay, newViaElements, savedResults));
        } else {
            viaSelected = true;
            main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, element, savedResults));
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        deselect(logic, !viaSelected);
        super.onDestroyActionMode(mode);
    }
}
