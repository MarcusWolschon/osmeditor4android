package de.blau.android.easyedit;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
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

    @Override
    protected void onCloseClicked() {
        Way way = logic.getSelectedWay();
        if (way != null) {
            main.startSupportActionMode(new WaySelectionActionModeCallback(manager, way));
        } else {
            super.onCloseClicked();
        }
    }
}
