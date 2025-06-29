package de.blau.android.easyedit;

import java.util.Set;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class WayAppendingActionModeCallback extends AbortableWayActionModeCallback {
    private final Way             way;
    private final Set<OsmElement> nodes;

    /**
     * Construct a new WayAppendingActionModeCallback from an existing Way and potential Node to append
     * 
     * @param manager the current EasyEditManager instance
     * @param way the existing Way
     * @param appendNodes a Set of Nodes that can be clicked by the user for appending
     */
    public WayAppendingActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Set<OsmElement> appendNodes) {
        super(manager);
        this.way = way;
        nodes = appendNodes;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_appendtoway;
        mode.setTitle(R.string.menu_append);
        mode.setSubtitle(R.string.actionmode_select_node_to_append_to);
        logic.setClickableElements(nodes);
        logic.setReturnRelations(false);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be
                                                            // clicked
        super.handleElementClick(element);
        final PathCreationActionModeCallback callback = new PathCreationActionModeCallback(manager, way, (Node) element);
        callback.setTitle(R.string.menu_append);
        callback.setSubTitle(R.string.add_way_node_instruction);
        main.startSupportActionMode(callback);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
