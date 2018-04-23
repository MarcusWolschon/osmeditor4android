package de.blau.android.easyedit.turnrestriction;

import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditActionModeCallback;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.RelationSelectionActionModeCallback;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

public class ToElementActionModeCallback extends EasyEditActionModeCallback {
    private static final String DEBUG9_TAG = "RestrictionToElement...";

    private Way        fromWay;
    private OsmElement viaElement;
    private Way        toWay;

    public ToElementActionModeCallback(EasyEditManager manager, Way from, OsmElement via, Way to) {
        super(manager);
        fromWay = from;
        viaElement = via;
        toWay = to;
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
        main.performTagEdit(restriction, !uTurn ? Tags.VALUE_RESTRICTION : null, false, false, false);
        main.startSupportActionMode(new RelationSelectionActionModeCallback(manager, restriction));
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
