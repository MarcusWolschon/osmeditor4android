package de.blau.android.easyedit;

import java.util.Set;

import android.content.res.Resources.NotFoundException;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.util.Snack;

public class WayMergingActionModeCallback extends EasyEditActionModeCallback {
    private static final String DEBUG_TAG = "WayMerging...";
    private Way                 way;
    private Set<OsmElement>     ways;

    public WayMergingActionModeCallback(EasyEditManager manager, Way way, Set<OsmElement> mergeableWays) {
        super(manager);
        this.way = way;
        ways = mergeableWays;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_mergingways;
        mode.setSubtitle(R.string.menu_merge);
        logic.setClickableElements(ways);
        logic.setReturnRelations(false);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid ways can be
                                                            // clicked
        super.handleElementClick(element);
        // race conditions with touch events seem to make the impossible possible
        // TODO fix properly
        if (!(element instanceof Way)) {
            return false;
        }
        if (!findMergeableWays(way).contains((Way) element)) {
            return false;
        }
        try {
            if (!logic.performMerge(main, way, (Way) element)) {
                Snack.barWarning(main, R.string.toast_merge_tag_conflict);
                if (way.getState() != OsmElement.STATE_DELETED) {
                    main.performTagEdit(way, null, false, false, false);
                } else {
                    main.performTagEdit(element, null, false, false, false);
                }
            } else {
                if (way.getState() != OsmElement.STATE_DELETED) {
                    main.startSupportActionMode(new WaySelectionActionModeCallback(manager, way));
                } else {
                    main.startSupportActionMode(new WaySelectionActionModeCallback(manager, (Way) element));
                }
            }
        } catch (OsmIllegalOperationException e) {
            Snack.barError(main, e.getLocalizedMessage());
        } catch (NotFoundException e) {
            Log.d(DEBUG_TAG, "handleElementClick got exception " + e.getMessage());
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
