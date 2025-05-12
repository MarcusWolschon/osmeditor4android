package de.blau.android.util;

import android.app.Dialog;
import android.view.ViewGroup;

/**
 * Fixed width version of DialogFragment
 */
public abstract class SizedFixedDialogFragment extends CancelableDialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int dialogWidth = (int) (Screen.getScreenSmallDimension(getActivity()) * 0.9);
            dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
