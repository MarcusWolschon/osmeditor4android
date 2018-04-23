package de.blau.android.easyedit;

import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.osm.Way;

public class WayRotationActionModeCallback extends EasyEditActionModeCallback {
    private static final String DEBUG_TAG = "WayRotationAction...";

    public WayRotationActionModeCallback(EasyEditManager manager, Way way) {
        super(manager);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_wayselection;
        super.onCreateActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onCreateActionMode");
        logic.setRotationMode(true);
        logic.showCrosshairsForCentroid();
        mode.setTitle(R.string.actionmode_rotateway);
        mode.setSubtitle(null);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.deselectAll();
        logic.setRotationMode(false);
        logic.hideCrosshairs();
        super.onDestroyActionMode(mode);
    }
}
