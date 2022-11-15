package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.dialogs.Tip;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Way;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

public class WaySplittingActionModeCallback extends NonSimpleActionModeCallback {
    private final Way        way;
    private List<OsmElement> nodes          = new ArrayList<>();
    private boolean          createPolygons = false;

    /**
     * Construct a WaySplittingActionModeCallback from an existing Way
     * 
     * @param manager the current EasyEditMAnager instance
     * @param way the existing Way
     * @param createPolygons create two polygons instead of unclosed ways if true and way is closed
     */
    public WaySplittingActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, boolean createPolygons) {
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
        if (way.isClosed()) {
            mode.setTitle(R.string.actionmode_split_closed_way);
            mode.setSubtitle(R.string.actionmode_closed_way_split_1);
            Tip.showDialog(main, R.string.tip_closed_way_splitting_key, R.string.tip_closed_way_splitting);
        } else {
            mode.setTitle(R.string.actionmode_split_way);
            mode.setSubtitle(R.string.actionmode_split_way_node_selection);
            Tip.showDialog(main, R.string.tip_way_splitting_key, R.string.tip_way_splitting);
        }
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
            return false;
        }
        if (way.isClosed()) {
            main.startSupportActionMode(new ClosedWaySplittingActionModeCallback(manager, way, (Node) element, createPolygons));
        } else {
            splitSafe(Util.wrapInList(way), () -> {
                try {
                    List<Result> result = logic.performSplit(main, way, (Node) element, true);
                    checkSplitResult(way, result);
                } catch (OsmIllegalOperationException | StorageException ex) {
                    // toast has already been displayed
                } finally {
                    manager.finish();
                }
            });
        }
        return true;
    }

    @Override
    public boolean handleElementLongClick(@NonNull OsmElement element) {
        super.handleElementLongClick(element);
        if (way.isClosed()) {
            Snack.toastTopWarning(main, R.string.toast_part_selection_not_supported);
        } else {
            main.startSupportActionMode(new WaySelectPartActionModeCallback(manager, way, (Node) element));
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
