package io.vespucci.easyedit.turnrestriction;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Map;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.dialogs.ElementIssueDialog;
import io.vespucci.easyedit.EasyEditManager;
import io.vespucci.easyedit.NonSimpleActionModeCallback;
import io.vespucci.easyedit.RelationSelectionActionModeCallback;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.exception.StorageException;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Result;
import io.vespucci.osm.Tags;
import io.vespucci.osm.Way;

public class ToElementActionModeCallback extends NonSimpleActionModeCallback {
    
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ToElementActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = ToElementActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private final Way        fromWay;
    private final OsmElement viaElement;
    private final Way        toWay;

    /**
     * Final step in constructing a turn restrictions
     * 
     * @param manager the current EasyEditManager instance
     * @param from the "from" role Way
     * @param via the "via" role OsmELement
     * @param to the "to" role Way
     * @param results saved intermediate results
     */
    public ToElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way from, @NonNull OsmElement via, Way to,
            @Nullable Map<OsmElement, Result> results) {
        super(manager);
        fromWay = from;
        viaElement = via;
        toWay = to;
        if (results != null) {
            savedResults = results;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addingrestriction;
        mode.setTitle(R.string.actionmode_restriction);
        super.onCreateActionMode(mode, menu);
        logic.addSelectedRelationWay(toWay);
        boolean uTurn = fromWay == toWay;
        try {
            Relation restriction = logic.createRestriction(main, fromWay, viaElement, toWay, uTurn ? Tags.VALUE_NO_U_TURN : null);
            Log.i(DEBUG_TAG, "Created restriction");
            main.performTagEdit(restriction, !uTurn ? Tags.VALUE_RESTRICTION : null, false, false);
            main.startSupportActionMode(new RelationSelectionActionModeCallback(manager, restriction));
            if (!savedResults.isEmpty()) {
                ElementIssueDialog.showTagConflictDialog(main, new ArrayList<>(savedResults.values()));
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            // logic will have already toasted
        }
        return false; // we are actually already finished
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) { // note never called
        deselect(logic, true);
        super.onDestroyActionMode(mode);
    }
}
