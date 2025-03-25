package io.vespucci.util;

import android.app.Dialog;
import android.view.ViewGroup;

/**
 * Fixed width version of ImmersiveDialogFragment
 */
public abstract class SizedDynamicImmersiveDialogFragment extends ImmersiveDialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
