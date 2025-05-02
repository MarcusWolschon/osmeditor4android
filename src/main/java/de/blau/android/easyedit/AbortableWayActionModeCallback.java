package de.blau.android.easyedit;

public abstract class AbortableWayActionModeCallback extends NonSimpleActionModeCallback {

    /**
     * Common code if you want to abort an action mode and rollback any changes
     * 
     * @param manager
     */
    protected AbortableWayActionModeCallback(EasyEditManager manager) {
        super(manager);
    }
    
    @Override
    protected void onCloseClicked() {
        onBackPressed();
    }

    @Override
    public boolean onBackPressed() {
        manager.finish();
        manager.editElements();
        return false;
    }
}
