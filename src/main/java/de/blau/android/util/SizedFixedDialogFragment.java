package de.blau.android.util;

/**
 * Fixed width version of DialogFragment
 */
public abstract class SizedFixedDialogFragment extends CancelableDialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        de.blau.android.dialogs.Util.limitWindowWidth(this);
    }
}
