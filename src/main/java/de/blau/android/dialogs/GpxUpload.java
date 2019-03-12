package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.GpxUploadListener;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog to select a file to upload
 *
 */
public class GpxUpload extends DialogFragment {
    private static final String DEBUG_TAG = GpxUpload.class.getSimpleName();
    private static final String TAG       = "fragment_gpx_upload";

    /**
     * Show an instance of this dialog
     * 
     * @param activity the calling FragmentActivity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            GpxUpload gpxUploadFragment = newInstance();
            gpxUploadFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss any current showing instance of this dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
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
     * Get a new instance of this fragment
     * 
     * @return a new instance of GpxUpload
     */
    @NonNull
    private static GpxUpload newInstance() {
        GpxUpload f = new GpxUpload();
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.confirm_upload_title);
        DoNothingListener doNothingListener = new DoNothingListener();
        View layout = inflater.inflate(R.layout.upload_gpx, null);
        builder.setView(layout);
        builder.setPositiveButton(R.string.transfer_download_current_upload,
                new GpxUploadListener((Main) getActivity(), (EditText) layout.findViewById(R.id.upload_gpx_description),
                        (EditText) layout.findViewById(R.id.upload_gpx_tags), (Spinner) layout.findViewById(R.id.upload_gpx_visibility)));
        builder.setNegativeButton(R.string.cancel, doNothingListener);
        return builder.create();
    }
}
