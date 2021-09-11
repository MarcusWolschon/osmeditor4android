package de.blau.android.dialogs;

import android.content.res.Resources;
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
import de.blau.android.R;
import de.blau.android.UploadResult;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Dialog to resolve upload conflicts one by one
 * 
 * @author simon
 *
 */
public class UploadConflict extends ImmersiveDialogFragment {

    private static final String UPLOAD_RESULT_KEY = "uploadresult";

    private static final String DEBUG_TAG = UploadConflict.class.getSimpleName();

    private static final String TAG = "fragment_upload_conflict";

    private UploadResult result;

    /**
     * Show a dialog after a conflict has been detected and allow the user to fix it
     * 
     * @param activity the calling Activity
     * @param result the UploadResult
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull UploadResult result) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        try {
            UploadConflict uploadConflictDialogFragment = newInstance(result);
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
     * Construct a new UploadConflit dialog
     * 
     * @param result an UploadResult
     * @return an UploadConflict dialog
     */
    @NonNull
    private static UploadConflict newInstance(@NonNull final UploadResult result) {
        UploadConflict f = new UploadConflict();

        Bundle args = new Bundle();
        args.putSerializable(UPLOAD_RESULT_KEY, result);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            result = (UploadResult) savedInstanceState.getSerializable(UPLOAD_RESULT_KEY);
        } else {
            result = (UploadResult) getArguments().getSerializable(UPLOAD_RESULT_KEY);
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.upload_conflict_title);
        Resources res = getActivity().getResources();
        final Logic logic = App.getLogic();
        final OsmElement elementOnServer = logic.getElement(getActivity(), result.getElementType(), result.getOsmId());
        final OsmElement elementLocal = App.getDelegator().getOsmElement(result.getElementType(), result.getOsmId());
        final long newVersion;
        try {
            boolean useServerOnly = false;
            if (elementOnServer != null) {
                if (elementLocal.getState() == OsmElement.STATE_DELETED) {
                    builder.setMessage(res.getString(R.string.upload_conflict_message_referential, elementLocal.getDescription(true)));
                    useServerOnly = true;
                } else {
                    builder.setMessage(res.getString(R.string.upload_conflict_message_version, elementLocal.getDescription(true), elementLocal.getOsmVersion(),
                            elementOnServer.getOsmVersion()));
                }
                newVersion = elementOnServer.getOsmVersion();
            } else {
                if (elementLocal.getState() == OsmElement.STATE_DELETED) {
                    builder.setMessage(res.getString(R.string.upload_conflict_message_already_deleted, elementLocal.getDescription(true)));
                    App.getDelegator().removeFromUpload(elementLocal);
                    builder.setPositiveButton(R.string.retry, (dialog, which) -> ConfirmUpload.showDialog(getActivity(), null));
                    return builder.create();
                } else {
                    builder.setMessage(
                            res.getString(R.string.upload_conflict_message_deleted, elementLocal.getDescription(true), elementLocal.getOsmVersion()));
                }
                newVersion = elementLocal.getOsmVersion() + 1;
            }
            if (!useServerOnly) {
                builder.setPositiveButton(R.string.use_local_version, (dialog, which) -> {
                    logic.fixElementWithConflict(getActivity(), newVersion, elementLocal, elementOnServer);
                    ConfirmUpload.showDialog(getActivity(), null);
                });
            }
            builder.setNeutralButton(R.string.use_server_version, (dialog, which) -> {
                StorageDelegator storageDelegator = App.getDelegator();
                storageDelegator.removeFromUpload(elementLocal);
                if (elementOnServer != null) {
                    logic.downloadElement(getActivity(), elementLocal.getName(), elementLocal.getOsmId(), false, true, null);
                } else { // delete local element
                    logic.updateToDeleted(getActivity(), elementLocal);
                }
                if (!storageDelegator.hasChanges()) {
                    ConfirmUpload.showDialog(getActivity(), null);
                }
            });
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Caught exception " + e);
            ACRAHelper.nocrashReport(e, e.getMessage());
        }
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(UPLOAD_RESULT_KEY, result);
    }
}
