package de.blau.android.util;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * Workaround for immersive mode breaking when dialogs are shown
 * 
 * See https://stackoverflow.com/questions/22794049/how-do-i-maintain-the-immersive-mode-in-dialogs/38469972#38469972
 *
 */
public abstract class SizedFixedImmersiveDialogFragment extends DialogFragment {

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Make the dialog non-focusable before showing it
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            showImmersive(manager);
        }
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        int result = super.show(transaction, tag);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            showImmersive(getFragmentManager());
        }
        return result;
    }

    /**
     * Show a dialog in immersive mode
     * 
     * @param manager our FragmentManager
     */
    @SuppressLint("NewApi")
    private void showImmersive(FragmentManager manager) {
        // It is necessary to call executePendingTransactions() on the FragmentManager
        // before hiding the navigation bar, because otherwise getWindow() would raise a
        // NullPointerException since the window was not yet created.
        manager.executePendingTransactions();

        Dialog dialog = getDialog();

        if (dialog != null && dialog.getWindow() != null) { // seems to be an issue on some systems
            Window dialogWindow = dialog.getWindow();
            // Copy flags from the activity, assuming it's fullscreen.
            // It is important to do this after show() was called. If we would do this in onCreateDialog(),
            // we would get a requestFeature() error.
            dialogWindow.getDecorView().setSystemUiVisibility(getActivity().getWindow().getDecorView().getSystemUiVisibility());

            // Make the dialogs window focusable again
            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int dialogWidth = (int) (Screen.getScreenSmallDimemsion(getActivity()) * 0.9);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                // workaround the popupmenu being clipped in at least 2.3 by simply making the dialog as large as
                // possible, ugly but works
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                // noinspection deprecation
                dialog.getWindow().setLayout(dialogWidth, (int) (display.getHeight() * 0.9)); // NOSONAR
            }
        }
    }
}
