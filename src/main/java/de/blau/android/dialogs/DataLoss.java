package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.R;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for confirmation before starting an activity that might result in data loss.
 *
 */
public class DataLoss extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DataLoss.class.getSimpleName().length());
    private static final String DEBUG_TAG = DataLoss.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG        = "fragment_dataloss";
    private static final String INTENT_KEY = "intent";

    private Intent intent;

    /**
     * Shows a dialog warning the user that he has unsaved changes that will be discarded.
     * 
     * @param activity Activity creating the dialog and starting the intent Activity if confirmed
     * @param intent intent for the activity to start
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull final Intent intent) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            DataLoss dataLossActivityFragment = newInstance(intent);
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
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new DataLossActivity dialog instance
     * 
     * @param intent the Intent to start
     * @param requestCode the intent request code
     * @return a new DataLossActivity dialog instance
     */
    @NonNull
    private static DataLoss newInstance(@NonNull final Intent intent) {
        DataLoss f = new DataLoss();

        Bundle args = new Bundle();
        args.putParcelable(INTENT_KEY, intent);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = de.blau.android.util.Util.getParcelable(savedInstanceState != null ? savedInstanceState : getArguments(), INTENT_KEY, Intent.class);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        return createDialog(getActivity(), (dialog, which) -> getActivity().startActivity(intent));
    }

    /**
     * Build the actual dialog
     * 
     * @param context an Android Context
     * @param listener the listener called when proceeding
     * @return the dialog
     */
    public static AppCompatDialog createDialog(@NonNull Context context, @NonNull DialogInterface.OnClickListener listener) {
        Builder builder = ThemeUtils.getAlertDialogBuilder(context);
        builder.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.alert_dialog));
        builder.setTitle(R.string.unsaved_data_title);
        builder.setMessage(R.string.unsaved_data_message);
        builder.setPositiveButton(R.string.unsaved_data_proceed, listener);
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(INTENT_KEY, intent);
    }
}
