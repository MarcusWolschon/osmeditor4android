package de.blau.android.easyedit;

import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

public abstract class AbortableActionModeCallback extends NonSimpleActionModeCallback {

    /**
     * Common code if you want to abort an action mode and rollback any changes
     * 
     * @param manager
     */
    protected AbortableActionModeCallback(EasyEditManager manager) {
        super(manager);
    }
    
    @Override
    protected void onCloseClicked() {
        onBackPressed();
    }

    @Override
    public boolean onBackPressed() {
        ThemeUtils.getAlertDialogBuilder(main).setTitle(R.string.abort_action_title).setPositiveButton(R.string.yes, (dialog, which) -> {
            logic.rollback();
            super.onBackPressed();
        }).setNeutralButton(R.string.cancel, null).show();
        return false;
    }
}
