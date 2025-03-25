package io.vespucci.easyedit.turnrestriction;

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
import io.vespucci.util.Util;

public class FromElementWithViaNodeActionModeCallback extends NonSimpleActionModeCallback {
    private static final String   DEBUG_TAG   = "FromElement...";
    private final Set<OsmElement> fromWays;
    private final Node            viaNode;
    private boolean               viaSelected = false;
    private int                   titleId     = R.string.actionmode_restriction_select_from;

    /**
     * Construct a new callback for determining the from element of a turn restriction if a via node is already chosen
     * 
     * @param manager the current EasyEditManager instance
     * @param fromWays the candidate "from" role Ways
     * @param via the "via" node
     * @param results saved intermediate results
     */
    public FromElementWithViaNodeActionModeCallback(@NonNull EasyEditManager manager, @NonNull Set<OsmElement> fromWays, @NonNull Node via,
            @Nullable Map<OsmElement, Result> results) {
        super(manager);
        this.fromWays = fromWays;
        this.viaNode = via;
        if (results != null) {
            savedResults = results;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addingrestriction;
        mode.setTitle(titleId);
        logic.setClickableElements(fromWays);
        logic.setReturnRelations(false);
        logic.setSelectedRelationWays(null); // just to be safe
        logic.addSelectedRelationNode(viaNode);
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
        if (!Way.NAME.equals(element.getName())) {
            unexpectedElement(DEBUG_TAG, element);
            return true;
        }
        final Way fromWay = (Way) element;
        if (!fromWay.isEndNode(viaNode)) {
            splitSafe(Util.wrapInList(fromWay), () -> {
                try {
                    // split from at node
                    List<Result> result = logic.performSplit(main, fromWay, viaNode, true);
                    Way newFromWay = newWayFromSplitResult(result);
                    saveSplitResult(fromWay, result);
                    nextStep(fromWay, newFromWay);
                } catch (OsmIllegalOperationException | StorageException ex) {
                    // toast has already been displayed
                    manager.finish();
                }
            });
        } else {
            nextStep(fromWay, null);
        }
        return true;
    }

    /**
     * Next step in adding the restriction
     * 
     * @param fromWay the original from way
     * @param newFromWay a new from way or null
     */
    private void nextStep(@NonNull Way fromWay, @Nullable Way newFromWay) {
        if (newFromWay != null) {
            Set<OsmElement> fromElements = new HashSet<>();
            fromElements.add(fromWay);
            fromElements.add(newFromWay);
            ScreenMessage.barInfo(main, R.string.toast_split_from);
            Set<OsmElement> via = new HashSet<>();
            via.add(viaNode);
            main.startSupportActionMode(new RestartFromElementActionModeCallback(manager, fromElements, via, savedResults));
        } else {
            viaSelected = true;
            main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, viaNode, savedResults));
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        deselect(logic, !viaSelected);
        super.onDestroyActionMode(mode);
    }
}
