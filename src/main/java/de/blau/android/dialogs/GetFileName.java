package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
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
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.SaveFile;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for a file name to save to
 *
 */
public class GetFileName extends DialogFragment {

    private static final String DEBUG_TAG = GetFileName.class.getSimpleName();

    private static final String TAG = "fragment_save_file";

    private SaveFile callback;

    /**
     * Display a dialog asking for a file name to save to
     * 
     * @param activity the calling Activity
     * @param callback a callback for saving the file
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull de.blau.android.util.SaveFile callback) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            GetFileName saveFileFragment = newInstance(callback);
            saveFileFragment.show(fm, TAG);
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
     * Get a new GetFileName dialog instance
     * 
     * @param callback a callback for saving the file
     * @return a new GetFileName dialog instance
     */
    @NonNull
    private static GetFileName newInstance(@NonNull de.blau.android.util.SaveFile callback) {
        GetFileName f = new GetFileName();
        Bundle args = new Bundle();
        args.putSerializable("callback", callback);
        f.setArguments(args);
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
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        callback = (SaveFile) getArguments().getSerializable("callback");
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.save_file);
        LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.save_file, null);
        builder.setView(searchLayout);
        final EditText saveFileEdit = (EditText) searchLayout.findViewById(R.id.save_file_edit);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.save, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.save(Uri.fromParts("", "", saveFileEdit.getText().toString()));
            }
        });
        return builder.create();
    }
}
