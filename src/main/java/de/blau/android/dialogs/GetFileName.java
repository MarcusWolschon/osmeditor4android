package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.R;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.SaveFile;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for a file name to save to
 *
 */
public class GetFileName extends ImmersiveDialogFragment {

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
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
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
        builder.setPositiveButton(R.string.save, (dialog, which) -> callback.save(Uri.fromParts("", "", saveFileEdit.getText().toString())));
        return builder.create();
    }
}
