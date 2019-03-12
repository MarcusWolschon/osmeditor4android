package de.blau.android.dialogs;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for confirmation before starting an activity that might result in data loss.
 *
 */
public class DataLossActivity extends DialogFragment {

    private static final String DEBUG_TAG = DataLossActivity.class.getSimpleName();

    private static final String TAG             = "fragment_dataloss_activity";
    private static final String INTENT_KEY      = "intent";
    private static final String REQUESTCODE_KEY = "requestcode";

    private Intent intent;
    private int    requestCode;

    /**
     * Shows a dialog warning the user that he has unsaved changes that will be discarded.
     * 
     * @param activity Activity creating the dialog and starting the intent Activity if confirmed
     * @param intent intent for the activity to start
     * @param requestCode If the activity should return a result, a non-negative request code. If no result is expected,
     *            set to -1.
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull final Intent intent, final int requestCode) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            DataLossActivity dataLossActivityFragment = newInstance(intent, requestCode);
            dataLossActivityFragment.show(fm, TAG);
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
     * Get a new DataLossActivity dialog instance
     * 
     * @param intent the Intent to start
     * @param requestCode the intent request code
     * @return a new DataLossActivity dialog instance
     */
    @NonNull
    private static DataLossActivity newInstance(@NonNull final Intent intent, final int requestCode) {
        DataLossActivity f = new DataLossActivity();

        Bundle args = new Bundle();
        args.putParcelable(INTENT_KEY, intent);
        args.putInt(REQUESTCODE_KEY, requestCode);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        intent = getArguments().getParcelable(INTENT_KEY);
        requestCode = getArguments().getInt(REQUESTCODE_KEY);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.unsaved_data_title);
        builder.setMessage(R.string.unsaved_data_message);
        builder.setPositiveButton(R.string.unsaved_data_proceed, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getActivity().startActivityForResult(intent, requestCode);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }
}
