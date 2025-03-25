package io.vespucci.easyedit.turnrestriction;

import java.util.Map;
import java.util.Set;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.easyedit.EasyEditManager;
import io.vespucci.easyedit.NonSimpleActionModeCallback;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Result;
import io.vespucci.osm.Way;

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
     * @param results saved intermediate results
     */
    public RestartFromElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Set<OsmElement> froms, @NonNull Set<OsmElement> vias,
            @Nullable Map<OsmElement, Result> results) {
        super(manager);
        fromElements = froms;
        viaElements = vias;
        if (results != null) {
            savedResults = results;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addingrestriction;
        mode.setTitle(R.string.actionmode_restriction_restart_from);
        logic.setClickableElements(fromElements);
        logic.setReturnRelations(false);
        logic.setSelectedRelationWays(null);
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
            main.startSupportActionMode(
                    new FromElementActionModeCallback(manager, R.string.actionmode_restriction_restart_via, (Way) element, viaElements, savedResults));
            return true;
        } else if (viaElements.size() == 1) {
            fromSelected = true;
            logic.addSelectedRelationWay((Way) element);
            main.startSupportActionMode(new ViaElementActionModeCallback(manager, (Way) element, viaElements.iterator().next(), savedResults));
            return true;
        }
        Log.e(DEBUG_TAG, "viaElements size " + viaElements.size());
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        deselect(logic, !fromSelected);
        super.onDestroyActionMode(mode);
    }
}
