package io.vespucci.easyedit;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.List;
import java.util.Set;

import android.content.res.Resources.NotFoundException;
import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.dialogs.ElementIssueDialog;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Result;
import io.vespucci.osm.Way;
import io.vespucci.util.ScreenMessage;

public class WayMergingActionModeCallback extends NonSimpleActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WayMergingActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = WayMergingActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private final Way             way;
    private final Set<OsmElement> ways;

    /**
     * Construct a new WayMergingActionModeCallback from an existing Way and potentially mergable Ways
     * 
     * @param manager the current EasyEditManager instance
     * @param way the existing Way
     * @param mergeableWays a Set of ways that could be merged
     */
    public WayMergingActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way, @NonNull Set<OsmElement> mergeableWays) {
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
        if (!findMergeableWays(way).contains(element)) {
            return false;
        }
        try {
            List<Result> result = logic.performMerge(main, way, (Way) element);
            Result r = result.get(0);
            main.startSupportActionMode(new WaySelectionActionModeCallback(manager, (Way) r.getElement()));
            if (result.size() > 1 || r.hasIssue()) {
                ElementIssueDialog.showTagConflictDialog(main, result);
            }
        } catch (OsmIllegalOperationException e) {
            ScreenMessage.barError(main, e.getLocalizedMessage());
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
