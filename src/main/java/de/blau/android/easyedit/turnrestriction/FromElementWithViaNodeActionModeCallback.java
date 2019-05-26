package de.blau.android.easyedit.turnrestriction;

import java.util.HashSet;
import java.util.Set;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.util.Snack;

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
     */
    public FromElementWithViaNodeActionModeCallback(@NonNull EasyEditManager manager, @NonNull Set<OsmElement> fromWays, @NonNull Node via) {
        super(manager);
        this.fromWays = fromWays;
        this.viaNode = via;
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

        Way fromWay = null;
        if (Way.NAME.equals(element.getName())) {
            fromWay = (Way) element;
        } else {
            // FIXME show a warning
            Log.e(DEBUG_TAG, element.getName() + " clicked");
            return true;
        }

        Way newFromWay = null;
        if (!fromWay.getFirstNode().equals(viaNode) && !fromWay.getLastNode().equals(viaNode)) {
            // split from at node
            newFromWay = logic.performSplit(main, fromWay, viaNode);
        }

        if (newFromWay != null) {
            Set<OsmElement> fromElements = new HashSet<>();
            fromElements.add(fromWay);
            fromElements.add(newFromWay);
            Snack.barInfo(main, R.string.toast_split_from);
            Set<OsmElement> via = new HashSet<>();
            via.add(viaNode);
            main.startSupportActionMode(new RestartFromElementActionModeCallback(manager, fromElements, via));
            return true;
        }
        viaSelected = true;
        main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, viaNode));
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        if (!viaSelected) { // back button or done pressed early
            logic.setSelectedRelationWays(null);
            logic.setSelectedRelationNodes(null);
        }
        super.onDestroyActionMode(mode);
    }
}
