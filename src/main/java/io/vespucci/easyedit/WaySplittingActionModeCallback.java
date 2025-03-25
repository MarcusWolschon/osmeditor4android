package io.vespucci.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.dialogs.Tip;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.exception.StorageException;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Result;
import io.vespucci.osm.Way;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.SerializableState;
import io.vespucci.util.Util;

public class WaySplittingActionModeCallback extends NonSimpleActionModeCallback {

    private static final String CREATE_POLYGONS_KEY = "create polygons";

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
        setup(createPolygons);
    }

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public WaySplittingActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        way = getSavedWay(state);
        setup(state.getBoolean(CREATE_POLYGONS_KEY));
    }

    /**
     * Setup code that is common to both constructors
     * 
     * @param createPolygons if a closed way create polygons
     */
    private void setup(@Nullable Boolean createPolygons) {
        nodes.addAll(way.getNodes());
        if (!way.isClosed()) {
            // remove first and last node
            nodes.remove(0);
            nodes.remove(nodes.size() - 1);
        } else {
            this.createPolygons = createPolygons != null && createPolygons;
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
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be clicked
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
                    manager.finish();
                    logic.setSelectedWay((Way) result.get(0).getElement());
                    manager.editElements();
                } catch (OsmIllegalOperationException | StorageException ex) {
                    // toast has already been displayed
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
            ScreenMessage.toastTopWarning(main, R.string.toast_part_selection_not_supported);
        } else {
            main.startSupportActionMode(new WaySelectPartActionModeCallback(manager, way, (Node) element));
        }
        return true;
    }

    @Override
    public boolean usesLongClick() {
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }

    @Override
    public void saveState(SerializableState state) {
        state.putLong(WAY_ID_KEY, way.getOsmId());
        state.putBoolean(CREATE_POLYGONS_KEY, createPolygons);
    }
}
