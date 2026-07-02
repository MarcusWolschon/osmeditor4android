package de.blau.android.util;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Main;

/**
 * Only removes some potential code duplication
 */
public abstract class CancelableDialogFragment extends DialogFragment {

    private boolean restartAutolock = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        FragmentActivity activity = getActivity();
        if (activity instanceof Main && restartAutolock) {
            ((Main) activity).scheduleAutoLock();
        }
    }

    /**
     * Enable restarting auto-lock
     */
    protected void enableAutolockReschedule() {
        this.restartAutolock = true;
    }
}
