package de.blau.android.easyedit.turnrestriction;

import java.util.ArrayList;
import java.util.Map;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.dialogs.TagConflictDialog;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.easyedit.RelationSelectionActionModeCallback;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Result;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

public class ToElementActionModeCallback extends NonSimpleActionModeCallback {
    private static final String DEBUG9_TAG = "RestrictionToElement...";

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
     */
    public ToElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way from, @NonNull OsmElement via, Way to, @Nullable Map<OsmElement, Result> results) {
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
        Relation restriction = logic.createRestriction(main, fromWay, viaElement, toWay, uTurn ? Tags.VALUE_NO_U_TURN : null);
        Log.i(DEBUG9_TAG, "Created restriction");
        main.performTagEdit(restriction, !uTurn ? Tags.VALUE_RESTRICTION : null, false, false);
        main.startSupportActionMode(new RelationSelectionActionModeCallback(manager, restriction));
        if (!savedResults.isEmpty()) {
            TagConflictDialog.showDialog(main, new ArrayList<>(savedResults.values()));
        }
        return false; // we are actually already finished
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) { // note never called
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedRelationWays(null);
        logic.setSelectedRelationNodes(null);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        super.onDestroyActionMode(mode);
    }
}
