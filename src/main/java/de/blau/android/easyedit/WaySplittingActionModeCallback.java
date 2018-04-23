package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class WaySplittingActionModeCallback extends EasyEditActionModeCallback {
    private Way              way;
    private List<OsmElement> nodes          = new ArrayList<>();
    private boolean          createPolygons = false;

    public WaySplittingActionModeCallback(EasyEditManager manager, Way way, boolean createPolygons) {
        super(manager);
        this.way = way;
        nodes.addAll(way.getNodes());
        if (!way.isClosed()) {
            // remove first and last node
            nodes.remove(0);
            nodes.remove(nodes.size() - 1);
        } else {
            this.createPolygons = createPolygons;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_waysplitting;
        super.onCreateActionMode(mode, menu);
        if (way.isClosed())
            mode.setSubtitle(R.string.actionmode_closed_way_split_1);
        else
            mode.setSubtitle(R.string.menu_split);
        logic.setClickableElements(new HashSet<>(nodes));
        logic.setReturnRelations(false);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be
                                                            // clicked
        super.handleElementClick(element);
        // protect against race conditions
        if (!(element instanceof Node)) {
            // TODO fix properly
            return false;
        }
        if (way.isClosed())
            main.startSupportActionMode(new ClosedWaySplittingActionModeCallback(manager, way, (Node) element, createPolygons));
        else {
            logic.performSplit(main, way, (Node) element);
            manager.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
