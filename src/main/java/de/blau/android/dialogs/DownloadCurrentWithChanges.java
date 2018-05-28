package de.blau.android.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.ConfirmUploadListener;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.DownloadCurrentListener;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for confirmation before starting an activity that might result in data loss.
 *
 */
public class DownloadCurrentWithChanges extends DialogFragment {

    private static final String DEBUG_TAG = DownloadCurrentWithChanges.class.getSimpleName();

    private static final String TAG = "fragment_newversion";

    /**
     
     */
    public static void showDialog(FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            DownloadCurrentWithChanges downloadCurrentWithChangesFragment = newInstance();
            downloadCurrentWithChangesFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    private static void dismissDialog(FragmentActivity activity) {
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
    }

    /**
     */
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.transfer_download_current_dialog_title);
        builder.setMessage(R.string.transfer_download_current_dialog_message);
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.transfer_download_current_upload, new ConfirmUploadListener((Main) getActivity()));
        builder.setNeutralButton(R.string.transfer_download_current_back, doNothingListener);
        builder.setNegativeButton(R.string.transfer_download_current_download, new DownloadCurrentListener((Main) getActivity()));

        return builder.create();
    }
}
