package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.Util;

/**
 * Display a dialog displaying information on a new version and offering to display the release notes.
 *
 */
public class NewVersion extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, NewVersion.class.getSimpleName().length());
    private static final String DEBUG_TAG = NewVersion.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_newversion";

    /**
     * Display a dialog displaying information on a new version and offering to display the release notes
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            NewVersion newVersionFragment = newInstance();
            newVersionFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new NewVersion dialog instance
     * 
     * @return a new NewVersion instance
     */
    @NonNull
    private static NewVersion newInstance() {
        NewVersion f = new NewVersion();
        f.setShowsDialog(true);
        return f;
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.upgrade_title);
        String message = getString(R.string.upgrade_message);
        builder.setMessage(Util.fromHtml(message));
        builder.setNegativeButton(R.string.skip, (d, which) -> dismiss());
        builder.setNeutralButton(R.string.read_upgrade, (d, which) -> {
            dismiss();
            HelpViewer.start(activity, R.string.help_upgrade);
        });
        return builder.create();
    }
}
