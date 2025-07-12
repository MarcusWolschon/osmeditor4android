package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import de.blau.android.UploadResult;
import de.blau.android.listener.UploadListener;
import de.blau.android.osm.Changeset;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.UpdateFromChanges;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Dialog to retry uploads that have failed due to network issues
 * 
 * @author simon
 *
 */
public class UploadRetry extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, UploadRetry.class.getSimpleName().length());
    private static final String DEBUG_TAG = UploadRetry.class.getSimpleName().substring(0, TAG_LEN);

    private static final int CHANGES_PARTIALLY_UPLOADED = 2;
    private static final int ALL_CHANGES_UPLOADED       = 1;
    private static final int NO_CHANGES_UPLOADED        = 0;

    private static final String RESULT_KEY          = "uploadresult";
    private static final String CHANGESET_ID_KEY    = "changeset_id";
    private static final String CLOSE_CHANGESET_KEY = "close_changeset";
    private static final String COMMENT_KEY         = "comment";
    private static final String SOURCE_KEY          = "source";
    private static final String EXTRA_TAGS_KEY      = "extra_tags";
    private static final String PROGRESS_STATUS_TAG = "progress_status";

    private static final String TAG = "fragment_upload_retry";

    private List<OsmElement>        elements;
    private UploadResult            result;
    private long                    changesetId;
    private boolean                 closeChangeset;
    private String                  comment;
    private String                  source;
    private HashMap<String, String> extraTags;

    private TextView retryMessage;

    /**
     * Show a dialog after a likely network caused upload failure that alloes the user to retry
     * 
     * @param activity the calling Activity
     * @param changesetId the changesetId if suspected open else -1
     * @param result the UploadResult
     * 
     * @param arguments the original upload arguments
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull UploadResult result, long changesetId,
            @NonNull UploadListener.UploadArguments arguments) {
        dismissDialog(activity);
        FragmentManager fm = activity.getSupportFragmentManager();
        try {
            UploadRetry uploadConflictDialogFragment = newInstance(result, changesetId, arguments);
            uploadConflictDialogFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
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
     * Construct a new UploadConflict dialog
     * 
     * @param result the upload result
     * @param changesetId the changesetId if suspected open else -1
     * @param closeChangeset flag indicating if the changeset was supposed to be closed
     * @param arguments the original upload arguments
     * 
     * @return an UploadConflict dialog
     */
    @NonNull
    private static UploadRetry newInstance(@NonNull UploadResult result, long changesetId, @NonNull UploadListener.UploadArguments arguments) {
        UploadRetry f = new UploadRetry();

        Bundle args = new Bundle();
        args.putSerializable(RESULT_KEY, result);
        args.putLong(CHANGESET_ID_KEY, changesetId);
        args.putBoolean(CLOSE_CHANGESET_KEY, arguments.closeChangeset);
        args.putString(COMMENT_KEY, arguments.comment);
        args.putString(SOURCE_KEY, arguments.source);
        args.putSerializable(EXTRA_TAGS_KEY, new HashMap<>(arguments.extraTags));
        if (arguments.elements != null) {
            Util.putElementsInBundle(arguments.elements, args);
        }
        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            getArgumentsFromBundle(savedInstanceState);
        } else {
            getArgumentsFromBundle(getArguments());
        }
    }

    /**
     * Get our arguments or saved state from a bundle
     * 
     * @param bundle the Bundle
     */
    private void getArgumentsFromBundle(Bundle bundle) {
        result = de.blau.android.util.Util.getSerializeable(bundle, RESULT_KEY, UploadResult.class);
        changesetId = bundle.getLong(CHANGESET_ID_KEY, -1);
        closeChangeset = bundle.getBoolean(CLOSE_CHANGESET_KEY, true);
        comment = bundle.getString(COMMENT_KEY);
        source = bundle.getString(SOURCE_KEY);
        extraTags = de.blau.android.util.Util.getSerializeable(bundle, EXTRA_TAGS_KEY, HashMap.class);
        elements = de.blau.android.dialogs.Util.getElements(getContext(), bundle);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {

        Builder builder = ThemeUtils.getAlertDialogBuilder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.upload_retry_title);
        builder.setNeutralButton(R.string.cancel, null); // set early in case of exceptions

        try {
            final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.retry, null);
            TextView issue = layout.findViewById(R.id.retry_issue);
            issue.setText(getString(R.string.upload_retry_issue, result.getMessage()));
            retryMessage = layout.findViewById(R.id.retry_message);
            builder.setNegativeButton(R.string.retry, null);
            builder.setPositiveButton(R.string.menu_transfer_update, null);

            builder.setView(layout);
            AlertDialog dialog = builder.create();

            final Logic logic = App.getLogic();
            if (changesetId == -1) {
                retryMessage.setText(getString(R.string.upload_retry_message_no_open_changeset));
                final UploadListener.UploadArguments arguments = new UploadListener.UploadArguments(comment, source, false, false, extraTags, elements);
                dialog.setOnShowListener((DialogInterface d) -> {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
                    setupButton(dialog.getButton(DialogInterface.BUTTON_NEGATIVE), R.string.retry, (View v) -> logic.upload(getActivity(), arguments, null));
                });
                return dialog;
            }
            determineStatus(App.getPreferences(getActivity()), logic, changesetId, dialog);
            dialog.setOnShowListener((DialogInterface d) -> {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
            });
            return dialog;
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Caught exception " + e);
            retryMessage.setText(e.getLocalizedMessage());
            ACRAHelper.nocrashReport(e, e.getMessage());
        }

        return builder.create();
    }

    /**
     * Query the API to determine the status of the upload
     * 
     * @param prefs the current Preferences
     * @param logic the current Logic instance
     * @param changesetId the id of the changeset that failed
     * @param dialog the dialog we are displaying
     */
    private void determineStatus(@NonNull final Preferences prefs, @NonNull final Logic logic, @NonNull final long changesetId, @NonNull AlertDialog dialog) {
        new ExecutorTask<Long, Void, Integer>(logic.getExecutorService(), logic.getHandler()) {

            private final StorageDelegator delegator = App.getDelegator();
            private Storage                storage;

            @Override
            protected Integer doInBackground(Long id) throws NumberFormatException, XmlPullParserException, IOException {
                Progress.showDialog(getActivity(), Progress.PROGRESS_DETERMINING_STATUS, PROGRESS_STATUS_TAG);

                Server server = prefs.getServer();
                // get the osmChange xml for the changeset
                Changeset changeset = server.getChangeset(id);
                if (changeset.getChanges() == 0) {
                    return NO_CHANGES_UPLOADED;
                    // nothing uploaded -> safe to retry
                }
                storage = server.getChanges(id);
                if (storage.getElementCount() == delegator.getApiElementCount()) {
                    // we uploaded everything and re-downloading is the best way to proceed
                    return ALL_CHANGES_UPLOADED;
                }
                if (delegator.getApiElementCount() > storage.getElementCount()) {
                    // we've likely successfully uploaded, but didn't get the results back
                    return CHANGES_PARTIALLY_UPLOADED;
                }
                return null;
            }

            @Override
            protected void onBackgroundError(Exception e) {
                if (!isAdded()) {
                    Log.e(DEBUG_TAG, "onBackgroundError fragment not attached");
                    return;
                }
                Progress.dismissDialog(getActivity(), Progress.PROGRESS_DETERMINING_STATUS, PROGRESS_STATUS_TAG);
                setErrorText(getString(R.string.upload_retry_message_no_changeset_status, changesetId));
            }

            /**
             * If something has gone wrong show a message and suggest saving changes
             * 
             * @param message the message
             * 
             */
            public void setErrorText(@NonNull String message) {
                retryMessage.setText(message);
                setupButton(dialog.getButton(DialogInterface.BUTTON_NEGATIVE), R.string.save_changes,
                        (View v) -> Main.saveOscFile(getActivity(), App.getDelegator(), prefs));
            }

            @Override
            protected void onPostExecute(Integer result) {
                Progress.dismissDialog(getActivity(), Progress.PROGRESS_DETERMINING_STATUS, PROGRESS_STATUS_TAG);
                if (result == null) {
                    setErrorText(getString(R.string.upload_retry_message_unexpected_error, changesetId));
                    return;
                }
                final Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                switch (result) {
                case NO_CHANGES_UPLOADED:
                    retryMessage.setText(getString(R.string.upload_retry_message_nothing_uploaded, changesetId));
                    UploadListener.UploadArguments arguments = new UploadListener.UploadArguments(comment, source, false, false, extraTags, elements);
                    setupButton(negative, R.string.retry, (View v) -> logic.upload(getActivity(), arguments, null));
                    return;
                case ALL_CHANGES_UPLOADED:
                    retryMessage.setText(getString(R.string.upload_retry_message_all_uploaded, changesetId));
                    setupButton(negative, R.string.update_data, (View v) -> updateData(delegator, storage));
                    setupButton(dialog.getButton(DialogInterface.BUTTON_POSITIVE), R.string.redownload, null);
                    return;
                case CHANGES_PARTIALLY_UPLOADED:
                    retryMessage.setText(getString(R.string.upload_retry_message_partial_upload, changesetId));
                    setupButton(negative, R.string.update_data, (View v) -> updateData(delegator, storage));
                    return;
                default:
                    Log.e(DEBUG_TAG, "Unexpected result vale " + result);
                }
            }
        }.execute(changesetId);
    }

    /**
     * Make a button visible, set text and listener
     * 
     * @param button the button
     * @param text the text displayed on the button
     * @param listener the listener that is called when the button is clicked
     */
    private void setupButton(@NonNull final Button button, final int textRes, @Nullable final View.OnClickListener listener) {
        button.setVisibility(View.VISIBLE);
        button.setText(textRes);
        button.setOnClickListener(listener);
    }

    /**
     * Try to synchronize the data in the StorageDElegator with the contents of the storage object
     * 
     * 
     * @param delegator the StorageDelegator instance
     * @param storage the updated data
     */
    private void updateData(@NonNull final StorageDelegator delegator, @NonNull final Storage storage) {
        ExecutorTask<Void, Void, Boolean> task = new ExecutorTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void v) throws NumberFormatException, XmlPullParserException, IOException {
                Progress.showDialog(getActivity(), Progress.PROGRESS_UPDATING, PROGRESS_STATUS_TAG);
                return UpdateFromChanges.update(delegator, storage);
            }

            @Override
            protected void onBackgroundError(Exception e) {
                Progress.dismissDialog(getActivity(), Progress.PROGRESS_UPDATING, PROGRESS_STATUS_TAG);
            }
        };
        task.execute();
        try {
            if (!Boolean.TRUE.equals(task.get(30, TimeUnit.SECONDS))) {
                ThemeUtils.getAlertDialogBuilder(getActivity()).setTitle(R.string.upload_retry_message_update_failed_title)
                        .setMessage(R.string.upload_retry_message_update_failed).setNeutralButton(R.string.cancel, null)
                        .setNegativeButton(R.string.save_changes,
                                (dialog, which) -> Main.saveOscFile(getActivity(), App.getDelegator(), App.getPreferences(getContext())))
                        .create().show();
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.e(DEBUG_TAG, "Unable to patch data " + e.getMessage());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(RESULT_KEY, result);
        outState.putLong(CHANGESET_ID_KEY, changesetId);
        outState.putBoolean(CLOSE_CHANGESET_KEY, closeChangeset);
        outState.putString(COMMENT_KEY, comment);
        outState.putString(SOURCE_KEY, source);
        outState.putSerializable(EXTRA_TAGS_KEY, extraTags);
        if (elements != null) {
            Util.putElementsInBundle(elements, outState);
        }
    }
}
