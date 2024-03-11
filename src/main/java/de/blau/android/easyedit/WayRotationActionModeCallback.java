package de.blau.android.easyedit;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.osm.Way;

public class WayRotationActionModeCallback extends NonSimpleActionModeCallback {
    private static final String DEBUG_TAG = WayRotationActionModeCallback.class.getSimpleName().substring(0, Math.min(23, WayRotationActionModeCallback.class.getSimpleName().length()));

    private Drawable savedButton;

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
        logic.createCheckpoint(main, R.string.undo_action_rotateway);
        logic.setRotationMode(true);
        logic.showCrosshairsForCentroid();

        FloatingActionButton button = main.getSimpleActionsButton();
        button.setOnClickListener(v -> {
            Way way = logic.getSelectedWay();
            if (way != null) {
                main.startSupportActionMode(new WaySelectionActionModeCallback(manager, way));
            }
        });
        savedButton = button.getDrawable();
        button.setImageResource(R.drawable.ic_done_white_36dp);
        mode.setTitle(R.string.actionmode_rotateway);
        mode.setSubtitle(null);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        super.onPrepareActionMode(mode, menu);
        main.enableSimpleActionsButton();
        if (!prefs.areSimpleActionsEnabled()) {
            main.showSimpleActionsButton();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.deselectAll();
        logic.setRotationMode(false);
        logic.hideCrosshairs();
        FloatingActionButton button = main.getSimpleActionsButton();
        button.setImageDrawable(savedButton);
        main.setSimpleActionsButtonListener();
        if (!prefs.areSimpleActionsEnabled()) {
            main.hideSimpleActionsButton();
        }
        super.onDestroyActionMode(mode);
    }

    @Override
    protected void onCloseClicked() {
        onBackPressed();
    }

    @Override
    public boolean onBackPressed() {
        new AlertDialog.Builder(main).setTitle(R.string.abort_action_title).setPositiveButton(R.string.yes, (dialog, which) -> {
            logic.rollback();
            super.onBackPressed();
        }).setNeutralButton(R.string.cancel, null).show();
        return false;
    }
}
