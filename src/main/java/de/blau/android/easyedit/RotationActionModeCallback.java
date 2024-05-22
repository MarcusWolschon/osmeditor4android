package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;

/**
 * Rotate the current selection
 */
public class RotationActionModeCallback extends NonSimpleActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RotationActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = RotationActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private Drawable savedButton;

    /**
     * Construct a new WayRotationActionModeCallback
     * 
     * Current assumes that the Way is selected
     * 
     * @param manager the current EasyEditMAnager instance
     * @param way the Way to rotate
     */
    public RotationActionModeCallback(@NonNull EasyEditManager manager) {
        super(manager);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_rotate;
        super.onCreateActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onCreateActionMode");
        logic.createCheckpoint(main, R.string.undo_action_rotate);
        logic.setRotationMode(true);
        logic.showCrosshairsForCentroid();

        FloatingActionButton button = main.getSimpleActionsButton();
        button.setOnClickListener(v -> manager.startElementSelectionMode());
        savedButton = button.getDrawable();
        button.setImageResource(R.drawable.ic_done_white_36dp);
        mode.setTitle(R.string.actionmode_rotate);
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
