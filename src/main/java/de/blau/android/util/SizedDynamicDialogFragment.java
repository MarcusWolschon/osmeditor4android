package de.blau.android.util;

import android.app.Dialog;
import android.view.ViewGroup;

/**
 * Non-fixed width version of DialogFragment
 */
public abstract class SizedDynamicDialogFragment extends CancelableDialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
