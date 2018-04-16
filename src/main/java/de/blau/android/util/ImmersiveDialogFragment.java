package de.blau.android.util;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;

/**
 * Workaround for immersive mode breaking when dialogs are shown
 * 
 * See https://stackoverflow.com/questions/22794049/how-do-i-maintain-the-immersive-mode-in-dialogs/38469972#38469972
 *
 */
public abstract class ImmersiveDialogFragment extends DialogFragment {

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        // Make the dialog non-focusable before showing it
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        showImmersive(manager);
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        int result = super.show(transaction, tag);
        showImmersive(getFragmentManager());
        return result;
    }

    private void showImmersive(FragmentManager manager) {
        // It is necessary to call executePendingTransactions() on the FragmentManager
        // before hiding the navigation bar, because otherwise getWindow() would raise a
        // NullPointerException since the window was not yet created.
        manager.executePendingTransactions();

        // Copy flags from the activity, assuming it's fullscreen.
        // It is important to do this after show() was called. If we would do this in onCreateDialog(),
        // we would get a requestFeature() error.
        getDialog().getWindow().getDecorView().setSystemUiVisibility(getActivity().getWindow().getDecorView().getSystemUiVisibility());

        // Make the dialogs window focusable again
        getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
