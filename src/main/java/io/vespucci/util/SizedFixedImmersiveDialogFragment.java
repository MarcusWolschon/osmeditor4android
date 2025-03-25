package io.vespucci.util;

import android.app.Dialog;
import android.view.ViewGroup;

/**
 * Fixed width version of ImmersiveDialogFragment
 */
public abstract class SizedFixedImmersiveDialogFragment extends ImmersiveDialogFragment {

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
