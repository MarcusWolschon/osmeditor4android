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

public class ViaElementActionModeCallback extends NonSimpleActionModeCallback {
    private static final String DEBUG_TAG  = "ViaElement..";
    private final Way           fromWay;
    private OsmElement          viaElement;
    private Set<OsmElement>     cachedToElements;
    private boolean             toSelected = false;
    private int                 titleId    = R.string.actionmode_restriction_to;

    /**
     * Construct a new callback for determining the to element of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param from selected "from" role Way
     * @param via selected "via" role OsmElement
     */
    public ViaElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way from, @NonNull OsmElement via) {
        super(manager);
        fromWay = from;
        viaElement = via;
        cachedToElements = findToElements(viaElement);
    }

    /**
     * Construct a new callback for determining the to element of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param from selected "from" role Way
     * @param via selected "via" role OsmElement
     * @param toElements potential "to" role OsmElements
     */
    public ViaElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way from, @NonNull OsmElement via, @NonNull Set<OsmElement> toElements) {
        super(manager);
        fromWay = from;
        viaElement = via;
        cachedToElements = toElements;
        this.titleId = R.string.actionmode_restriction_restart_to;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addingrestriction;
        mode.setTitle(titleId);
        logic.setClickableElements(cachedToElements);
        logic.setReturnRelations(false);
        if (Node.NAME.equals(viaElement.getName())) {
            logic.addSelectedRelationNode((Node) viaElement);
        } else {
            logic.addSelectedRelationWay((Way) viaElement);
        }
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid elements can be
                                                            // clicked
        super.handleElementClick(element);
        Node viaNode = null;
        Way toWay = (Way) element;
        if (Node.NAME.equals(viaElement.getName())) {
            viaNode = (Node) viaElement;
        } else if (Way.NAME.equals(viaElement.getName())) {
            Way viaWay = (Way) viaElement;
            viaNode = ((Way) viaElement).getCommonNode(toWay);
            if (!viaWay.getFirstNode().equals(viaNode) && !viaWay.getLastNode().equals(viaNode)) {
                // split via way and use appropriate segment
                Way newViaWay = logic.performSplit(main, viaWay, viaNode);
                Snack.barInfo(main, R.string.toast_split_via);
                if (fromWay.hasNode(newViaWay.getFirstNode()) || fromWay.hasNode(newViaWay.getLastNode())) {
                    viaElement = newViaWay;
                }
            }
        } else {
            // FIXME show a warning
            Log.e(DEBUG_TAG, element.getName() + " clicked");
            return true;
        }
        // now check if we need to split the toWay
        if (!toWay.getFirstNode().equals(viaNode) && !toWay.getLastNode().equals(viaNode)) {
            Way newToWay = logic.performSplit(main, toWay, viaNode);
            Snack.barInfo(main, R.string.toast_split_to);
            Set<OsmElement> toCandidates = new HashSet<>();
            toCandidates.add(toWay);
            toCandidates.add(newToWay);
            main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, viaElement, toCandidates));
            return true;
        }

        toSelected = true;
        main.startSupportActionMode(new ToElementActionModeCallback(manager, fromWay, viaElement, (Way) element));
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        if (!toSelected) {
            // back button or done pressed early
            logic.setSelectedRelationWays(null);
            logic.setSelectedRelationNodes(null);
        }
        super.onDestroyActionMode(mode);
    }
}
