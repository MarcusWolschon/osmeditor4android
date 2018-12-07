package de.blau.android.easyedit;

import java.util.Set;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class WayAppendingActionModeCallback extends NonSimpleActionModeCallback {
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
        mode.setSubtitle(R.string.menu_append);
        logic.setClickableElements(nodes);
        logic.setReturnRelations(false);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be
                                                            // clicked
        super.handleElementClick(element);
        main.startSupportActionMode(new PathCreationActionModeCallback(manager, way, (Node) element));
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
