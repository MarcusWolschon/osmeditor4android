package de.blau.android.dialogs;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ImmersiveDialogFragment;

/**
 * Display a dialog displaying information on a new version and offering to display the release notes.
 *
 */
public class NewVersion extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = NewVersion.class.getSimpleName();

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
        Builder builder = new AlertDialog.Builder(getActivity());
        // builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
        builder.setTitle(R.string.upgrade_title);
        builder.setMessage(R.string.upgrade_message);
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setNegativeButton(R.string.cancel, doNothingListener);
        builder.setNeutralButton(R.string.read_upgrade, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HelpViewer.start(getActivity(), R.string.help_upgrade);
            }
        });
        return builder.create();
    }
}
