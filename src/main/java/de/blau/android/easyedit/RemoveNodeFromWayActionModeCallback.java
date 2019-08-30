package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class RemoveNodeFromWayActionModeCallback extends NonSimpleActionModeCallback {
    private final Way        way;
    private List<OsmElement> nodes = new ArrayList<>();

    /**
     * Construct a RemoveNodeFromWayActionModeCallback from an existing Way
     * 
     * @param manager the current EasyEditMAnager instance
     * @param way the existing Way
     */
    public RemoveNodeFromWayActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way) {
        super(manager);
        this.way = way;
        nodes.addAll(way.getNodes());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_wayselection;
        super.onCreateActionMode(mode, menu);
        mode.setSubtitle(R.string.menu_remove_node_from_way);
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
        logic.performRemoveNodeFromWay(main, way, (Node) element);
        manager.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
