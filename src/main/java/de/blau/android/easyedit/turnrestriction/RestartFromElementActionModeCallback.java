package de.blau.android.easyedit.turnrestriction;

import java.util.Set;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class RestartFromElementActionModeCallback extends NonSimpleActionModeCallback {
    private static final String   DEBUG_TAG    = "RestartFrom...";
    private final Set<OsmElement> fromElements;
    private final Set<OsmElement> viaElements;
    private boolean               fromSelected = false;

    /**
     * Construct a new callback for determining the from element of a turn restriction from multiple Ways
     * 
     * @param manager the current EasyEditManager instance
     * @param froms potential "from" role Ways
     * @param vias potential "via" role elements
     */
    public RestartFromElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Set<OsmElement> froms, @NonNull Set<OsmElement> vias) {
        super(manager);
        fromElements = froms;
        viaElements = vias;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addingrestriction;
        mode.setTitle(R.string.actionmode_restriction_restart_from);
        logic.setClickableElements(fromElements);
        logic.setReturnRelations(false);
        logic.setSelectedRelationWays(null); // just to be safe
        logic.addSelectedRelationWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be
                                                            // clicked
        super.handleElementClick(element);
        if (viaElements.size() > 1) {
            fromSelected = true;
            logic.addSelectedRelationWay((Way) element);
            // redo via selection, this time with pre-split way
            main.startSupportActionMode(new FromElementActionModeCallback(manager, R.string.actionmode_restriction_restart_via, (Way) element, viaElements));
            return true;
        } else if (viaElements.size() == 1) {
            fromSelected = true;
            logic.addSelectedRelationWay((Way) element);
            main.startSupportActionMode(new ViaElementActionModeCallback(manager, (Way) element, viaElements.iterator().next()));
            return true;
        }
        Log.e(DEBUG_TAG, "viaElements size " + viaElements.size());
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        if (!fromSelected) {
            logic.setSelectedRelationWays(null);
            logic.setSelectedRelationNodes(null);
        }
        super.onDestroyActionMode(mode);
    }
}
