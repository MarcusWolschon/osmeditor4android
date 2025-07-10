package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for confirmation before downloading and overwriting existing changes
 *
 */
public class DownloadCurrentWithChanges extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DownloadCurrentWithChanges.class.getSimpleName().length());
    private static final String DEBUG_TAG = DownloadCurrentWithChanges.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_newversion";

    /**
     * Display a dialog asking for confirmation before downloading and overwriting existing changes
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            DownloadCurrentWithChanges downloadCurrentWithChangesFragment = newInstance();
            downloadCurrentWithChangesFragment.show(fm, TAG);
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
     * Get a new instance of a DownloadCurrentWithChanges dialog
     * 
     * @return a new instance of a DownloadCurrentWithChanges dialog
     */
    @NonNull
    private static DownloadCurrentWithChanges newInstance() {
        DownloadCurrentWithChanges f = new DownloadCurrentWithChanges();
        f.setShowsDialog(true);
        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        if (!(context instanceof Main)) {
            throw new ClassCastException(context.toString() + " can only be called from Main");
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = ThemeUtils.getAlertDialogBuilder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.transfer_download_current_dialog_title);
        builder.setMessage(R.string.transfer_download_current_dialog_message);
        DoNothingListener doNothingListener = new DoNothingListener();
        final Main main = ((Main) getActivity());
        builder.setPositiveButton(R.string.transfer_download_current_upload, (dialog, which) -> main.confirmUpload(null));
        builder.setNeutralButton(R.string.transfer_download_current_back, doNothingListener);
        builder.setNegativeButton(R.string.transfer_download_current_download, (dialog, which) -> main.performCurrentViewHttpLoad(false));
        return builder.create();
    }
}
