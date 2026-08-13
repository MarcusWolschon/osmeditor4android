package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
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

    private static final String FRAGMENT_NAME = "fragmentName";

    protected SelectedRowsActionModeCallback actionModeCallback     = null;
    protected final Object                   actionModeCallbackLock = new Object();

    private boolean restartActionMode;

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.d(DEBUG_TAG, "onViewStateRestored");
        if (savedInstanceState != null && this.getClass().getCanonicalName().equals(savedInstanceState.getString(FRAGMENT_NAME))
                && savedInstanceState.getIntegerArrayList(SelectedRowsActionModeCallback.SELECTED_ROWS_KEY) != null && actionModeCallback == null) {
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
        restartActionMode();
    }

    /**
     * Restart the action mode if necessary
     */
    void restartActionMode() {
        if (restartActionMode) {
            restartActionMode = false;
            ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveActionModeState(outState);
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    /**
     * Save only the action mode state to a BUndle
     * 
     * @param outState the Bundle to save to
     */
    void saveActionModeState(@NonNull Bundle outState) {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback != null) {
                outState.putString(FRAGMENT_NAME, this.getClass().getCanonicalName());
                actionModeCallback.saveState(outState);
            }
        }
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

    /**
     * Start the ActionMode for when an element is selected
     */
    protected void onRowSelected() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback == null) {
                actionModeCallback = getActionModeCallback();
                ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
            }
            actionModeCallback.invalidate();
        }
    }

    @Override
    public void onDeselectRow() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback != null) {
                if (actionModeCallback.rowsDeselected()) {
                    actionModeCallback = null;
                } else {
                    actionModeCallback.invalidate();
                }
            }
        }
    }

    @Override
    public void selectAllRows() {
        setSelectedRows((boolean current) -> true);
    }

    @Override
    public void deselectAllRows() {
        setSelectedRows((boolean current) -> false);
    }

    @Override
    public void invertSelectedRows() {
        setSelectedRows((boolean current) -> !current);
    }

    /**
     * Finish any action mode
     */
    public void finishActionMode() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback != null) {
                actionModeCallback.currentAction.finish();
                actionModeCallback = null;
            }
        }
    }

    /**
     * Iterate over all rows and set the selection status
     * 
     * @param change method that sets the selection status
     */
    protected abstract void setSelectedRows(@NonNull final ChangeSelectionStatus change);

    @Override
    public void startStopActionModeIfRowSelected() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback == null) {
                actionModeCallback = getActionModeCallback();
                if (!actionModeCallback.rowsDeselected()) {
                    ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
                }
                return;
            }
            if (actionModeCallback.rowsDeselected()) {
                actionModeCallback = null;
            }
        }
    }
}
