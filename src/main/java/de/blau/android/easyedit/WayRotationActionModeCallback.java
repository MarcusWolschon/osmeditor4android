package de.blau.android.easyedit;

import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import de.blau.android.R;
import de.blau.android.osm.Way;

public class WayRotationActionModeCallback extends NonSimpleActionModeCallback {
    private static final String DEBUG_TAG = "WayRotationAction...";

    /**
     * Construct a new WayRotationActionModeCallback from an existing Way
     * 
     * Current assumes that the Way is selected
     * 
     * @param manager the current EasyEditMAnager instance
     * @param way the Way to rotate
     */
    public WayRotationActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way way) {
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
