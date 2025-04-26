package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.ViewBox;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for confirmation before starting an activity that might result in data loss.
 *
 */
public class TooMuchData extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TooMuchData.class.getSimpleName().length());
    private static final String DEBUG_TAG = TooMuchData.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG           = "fragment_too_much_data";
    private static final String NODECOUNT_KEY = "nodeCount";

    private int nodeCount;

    /**
     * Shows a dialog warning the user that he has unsaved changes that will be discarded.
     * 
     * @param activity Activity creating the dialog
     * @param nodeCount the current number of Nodes
     */
    public static void showDialog(@NonNull FragmentActivity activity, final int nodeCount) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            TooMuchData tooMuchDataFragment = newInstance(nodeCount);
            tooMuchDataFragment.show(fm, TAG);
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
     * Get a new TooMuchData dialog instance
     * 
     * @param nodeCount the current number of nodes
     * @return a new TooMuchData dialog instance
     */
    @NonNull
    private static TooMuchData newInstance(final int nodeCount) {
        TooMuchData f = new TooMuchData();

        Bundle args = new Bundle();
        args.putInt(NODECOUNT_KEY, nodeCount);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            nodeCount = savedInstanceState.getInt(NODECOUNT_KEY);
            Log.d(DEBUG_TAG, "restoring from saved state");
        } else {
            nodeCount = getArguments().getInt(NODECOUNT_KEY);
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(ThemeUtils.getResIdFromAttribute(activity, R.attr.alert_dialog));
        builder.setTitle(R.string.too_much_data_title);
        builder.setMessage(getString(R.string.too_much_data_message, nodeCount));
        if (activity instanceof Main) {
            builder.setPositiveButton(R.string.upload_data_now, (dialog, which) -> ((Main) activity).confirmUpload(null));
            Logic logic = App.getLogic();
            builder.setNegativeButton(R.string.prune_data_now,
                    (dialog, which) -> new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
                        @Override
                        protected void onPreExecute() {
                            Progress.showDialog(activity, Progress.PROGRESS_PRUNING);
                        }

                        @Override
                        protected Void doInBackground(Void arg) {
                            ViewBox pruneBox = new ViewBox(App.getLogic().getViewBox());
                            pruneBox.scale(1.1);
                            App.getDelegator().prune(pruneBox);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            Progress.dismissDialog(activity, Progress.PROGRESS_PRUNING);
                        }
                    }.execute());
        }

        builder.setNeutralButton(R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(NODECOUNT_KEY, nodeCount);
    }
}
