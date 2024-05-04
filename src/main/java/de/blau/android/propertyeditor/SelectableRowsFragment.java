package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.Util;

/**
 * Handle saving and restoring state of the action modes used to select rows
 * 
 * @author Simon Poole
 *
 */
public abstract class SelectableRowsFragment extends BaseFragment implements PropertyRows {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, SelectableRowsFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = SelectableRowsFragment.class.getSimpleName().substring(0, TAG_LEN);

    protected SelectedRowsActionModeCallback actionModeCallback     = null;
    protected final Object                   actionModeCallbackLock = new Object();

    private boolean restartActionMode;

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getIntegerArrayList(SelectedRowsActionModeCallback.SELECTED_ROWS_KEY) != null
                && actionModeCallback == null) {
            actionModeCallback = getActionModeCallback();
            actionModeCallback.restoreState(savedInstanceState);
            restartActionMode = true;
        }
    }

    /**
     * Construct and return the appropriate action mode callback
     * 
     * @return an instance of SelectedRowsActionModeCallback or a subclass of it
     */
    protected abstract SelectedRowsActionModeCallback getActionModeCallback();

    @Override
    public void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        if (restartActionMode) {
            restartActionMode = false;
            ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback != null) {
                actionModeCallback.saveState(outState);
            }
        }
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback != null) {
                actionModeCallback.currentAction.finish();
                actionModeCallback = null;
            }
        }
    }

    @Override
    public void deselectRow() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback != null && actionModeCallback.rowsDeselected(true)) {
                actionModeCallback = null;
            }
        }
    }
}
