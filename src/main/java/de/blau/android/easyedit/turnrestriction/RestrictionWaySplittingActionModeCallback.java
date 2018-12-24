package de.blau.android.easyedit.turnrestriction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.easyedit.ClosedWaySplittingActionModeCallback;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class RestrictionWaySplittingActionModeCallback extends NonSimpleActionModeCallback {
    private final Way        way;
    private final Way        fromWay;
    private List<OsmElement> nodes          = new ArrayList<>();
    private final int    subTitle;

    /**
     * Construct a WaySplittingActionModeCallback from an existing Way
     * 
     * @param manager the current EasyEditMAnager instance
     * @param way the existing Way
     * @param createPolygons create two polygons instead of unclosed ways if true and way is closed
     */
    public RestrictionWaySplittingActionModeCallback(@NonNull EasyEditManager manager, int subTitle, @NonNull Way way, @Nullable Way fromWay) {
        super(manager);
        this.way = way;
        this.fromWay = fromWay;
        nodes.addAll(way.getNodes());
        if (!way.isClosed()) {
            // remove first and last node
            nodes.remove(0);
            nodes.remove(nodes.size() - 1);
        } 
        this.subTitle = subTitle;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_waysplitting;
        super.onCreateActionMode(mode, menu);
        mode.setSubtitle(subTitle);
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
        if (way.isClosed()) {
            // main.startSupportActionMode(new ClosedWaySplittingActionModeCallback(manager, way, (Node) element, createPolygons));
        } else {
            Way newWay = logic.performSplit(main, way, (Node) element);
            if (fromWay == null) {
                Set<OsmElement>candidates = new HashSet<>();  
                candidates.add(way);
                candidates.add(newWay);
                main.startSupportActionMode(new RestartFromElementActionModeCallback(manager, candidates, candidates));
            } else {
                Way viaWay = newWay;
                if (fromWay.hasCommonNode(way)) {
                    viaWay = way;
                }
                main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, viaWay));
            }
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mode.setSubtitle("");
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        super.onDestroyActionMode(mode);
    }
}
