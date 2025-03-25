package io.vespucci.easyedit.turnrestriction;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.easyedit.AbstractClosedWaySplittingActionModeCallback;
import io.vespucci.easyedit.EasyEditManager;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.exception.StorageException;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Result;
import io.vespucci.osm.Way;

/**
 * Callback for splitting a closed way/polygon as part of a turn restriction
 * 
 * @author simon
 *
 */
public class RestrictionClosedWaySplittingActionModeCallback extends AbstractClosedWaySplittingActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RestrictionClosedWaySplittingActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = RestrictionClosedWaySplittingActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private final Way  way;
    private final Node node;
    private final Way  fromWay;

    /**
     * Construct a new callback for splitting a closed way/polygon as part of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param way the closed way
     * @param node the first node to split at the callback will ask for the 2nd one
     * @param fromWay the current from segment or null
     * @param results saved intermediate results
     */
    public RestrictionClosedWaySplittingActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Node node, @Nullable Way fromWay,
            @Nullable Map<OsmElement, Result> results) {
        super(manager);
        this.way = way;
        this.node = node;
        this.fromWay = fromWay;
        List<Node> allNodes = way.getNodes();
        nodes.addAll(allNodes);
        nodes.remove(node);
        if (results != null) {
            savedResults = results;
        }
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid nodes can be clicked
        super.handleElementClick(element);
        try {
            if (element instanceof Node) {
                List<Result> results = logic.performClosedWaySplit(main, way, node, (Node) element, false);
                // FIXME we currently don't display any issues as that would be confusing
                Way way0 = (Way) results.get(0).getElement();
                Way way1 = (Way) results.get(1).getElement();
                if (fromWay == null) {
                    Set<OsmElement> candidates = new HashSet<>();
                    candidates.add(way0);
                    candidates.add(way1);
                    main.startSupportActionMode(new RestartFromElementActionModeCallback(manager, candidates, candidates, savedResults));
                } else {
                    Way viaWay = way0;
                    if (fromWay.hasCommonNode(way1)) {
                        viaWay = way1;
                    }
                    main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, viaWay, savedResults));
                }
                return true;
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            // toast has already been displayed
        }
        manager.finish();
        Log.d(DEBUG_TAG, "split failed at element " + (element != null ? element : "null"));
        return true;
    }
}
