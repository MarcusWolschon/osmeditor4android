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
     */
    public FromElementActionModeCallback(@NonNull EasyEditManager manager, int titleId, @NonNull Way way, @NonNull Set<OsmElement> vias) {
        this(manager, way, vias);
        this.titleId = titleId;
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
            // FIXME show a warning
            Log.e(DEBUG_TAG, element.getName() + " clicked");
            return true;
        }
        if (viaWay != null && viaWay.equals(fromWay)) {
            // we need to split fromWay first
            main.startSupportActionMode(new RestrictionWaySplittingActionModeCallback(manager, R.string.actionmode_restriction_split_from, fromWay, null));
            return true;
        }
        Way newFromWay = null;
        if (!fromWay.getFirstNode().equals(viaNode) && !fromWay.getLastNode().equals(viaNode)) {
            // split from at node
            newFromWay = logic.performSplit(main, fromWay, viaNode);
        }
        Way newViaWay = null;
        if (viaWay != null && !viaWay.getFirstNode().equals(viaNode) && !viaWay.getLastNode().equals(viaNode)) {
            newViaWay = logic.performSplit(main, viaWay, viaNode);
        }
        Set<OsmElement> newViaElements = new HashSet<>();
        newViaElements.add(element);
        if (newViaWay != null) {
            newViaElements.add(newViaWay);
        }
        if (newFromWay != null) {
            Set<OsmElement> fromElements = new HashSet<>();
            fromElements.add(fromWay);
            fromElements.add(newFromWay);
            Snack.barInfo(main, newViaWay == null ? R.string.toast_split_from : R.string.toast_split_from_and_via);
            main.startSupportActionMode(new RestartFromElementActionModeCallback(manager, fromElements, newViaElements));
            return true;
        }
        if (newViaWay != null) {
            // restart via selection
            Snack.barInfo(main, R.string.toast_split_via);
            main.startSupportActionMode(new FromElementActionModeCallback(manager, R.string.actionmode_restriction_restart_via, fromWay, newViaElements));
            return true;
        }
        viaSelected = true;
        main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, element));
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
