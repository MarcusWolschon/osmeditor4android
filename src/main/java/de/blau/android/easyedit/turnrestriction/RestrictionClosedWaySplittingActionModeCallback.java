package de.blau.android.easyedit.turnrestriction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

/**
 * Callback for splitting a closed way/polygon as part of a turn restriction
 * 
 * @author simon
 *
 */
public class RestrictionClosedWaySplittingActionModeCallback extends NonSimpleActionModeCallback {
    private static final String   DEBUG_TAG = "ClosedWaySplit...";
    private final Way             way;
    private final Node            node;
    private final Way             fromWay;
    private final Set<OsmElement> nodes     = new HashSet<>();    // nodes that we can use for splitting

    /**
     * Construct a new callback for splitting a closed way/polygon as part of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param way the closed way
     * @param node the first node to split at the callback will ask for the 2nd one
     * @param fromWay the current from segment or null
     */
    public RestrictionClosedWaySplittingActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Node node, @Nullable Way fromWay) {
        super(manager);
        this.way = way;
        this.node = node;
        this.fromWay = fromWay;
        List<Node> allNodes = way.getNodes();
        nodes.addAll(allNodes);
        nodes.remove(node);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_closedwaysplitting;
        super.onCreateActionMode(mode, menu);
        mode.setSubtitle(R.string.actionmode_closed_way_split_2);
        logic.setClickableElements(nodes);
        logic.setReturnRelations(false);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid nodes can be clicked
        super.handleElementClick(element);
        if (element instanceof Node) {
            Way[] result = logic.performClosedWaySplit(main, way, node, (Node) element, false);
            if (result != null && result.length == 2) {
                if (fromWay == null) {
                    Set<OsmElement> candidates = new HashSet<>();
                    candidates.add(result[0]);
                    candidates.add(result[1]);
                    main.startSupportActionMode(new RestartFromElementActionModeCallback(manager, candidates, candidates));
                } else {
                    Way viaWay = result[0];
                    if (fromWay.hasCommonNode(result[1])) {
                        viaWay = result[1];
                    }
                    main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, viaWay));
                }
                return true;
            }
        }
        // FIXME toast here?
        Log.d(DEBUG_TAG, "split failed at element " + (element != null ? element : "null"));
        manager.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mode.setSubtitle("");
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
