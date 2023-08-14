package de.blau.android.dialogs;

import java.net.HttpURLConnection;

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
import de.blau.android.AsyncResult;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.UploadResult;
import de.blau.android.osm.OsmElement;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
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
            result = de.blau.android.util.Util.getSerializeable(savedInstanceState, UPLOAD_RESULT_KEY, UploadResult.class);
        } else {
            result = de.blau.android.util.Util.getSerializeable(getArguments(), UPLOAD_RESULT_KEY, UploadResult.class);
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
        String elementType = result.getElementType();
        long osmId = result.getOsmId();
        final OsmElement elementOnServer = logic.getElement(getActivity(), elementType, osmId);
        final OsmElement elementLocal = App.getDelegator().getOsmElement(elementType, osmId);
        final long newVersion;
        try {
            boolean useServerOnly = false;
            final long localVersion = elementLocal.getOsmVersion();
            if (elementOnServer != null) {
                final long serverVersion = elementOnServer.getOsmVersion();
                if (elementLocal.getState() == OsmElement.STATE_DELETED) {
                    // we are deleting an element that is still in use on the server
                    builder.setMessage(res.getString(R.string.upload_conflict_message_referential, elementLocal.getDescription(true)));
                    useServerOnly = true;
                } else {
                    if (localVersion != serverVersion) {
                        builder.setMessage(
                                res.getString(R.string.upload_conflict_message_version, elementLocal.getDescription(true), localVersion, serverVersion));
                    } else if (HttpURLConnection.HTTP_PRECON_FAILED == result.getHttpError()) {
                        builder.setMessage(res.getString(R.string.upload_conflict_message_missing_references, elementLocal.getDescription(true)));
                        useServerOnly = true;
                    } else {
                        builder.setMessage(res.getString(R.string.upload_conflict_message_unknown, elementLocal.getDescription(true), result.getMessage()));
                    }
                }
                newVersion = serverVersion;
            } else {
                if (elementLocal.getState() == OsmElement.STATE_DELETED) {
                    // we are trying to delete something that already is
                    builder.setMessage(res.getString(R.string.upload_conflict_message_already_deleted, elementLocal.getDescription(true)));
                    App.getDelegator().removeFromUpload(elementLocal, OsmElement.STATE_DELETED);
                    builder.setPositiveButton(R.string.retry, (dialog, which) -> ReviewAndUpload.showDialog(getActivity(), null));
                    return builder.create();
                } else { // can this happen? don't think so
                    builder.setMessage(res.getString(R.string.upload_conflict_message_deleted, elementLocal.getDescription(true), localVersion));
                }
                newVersion = localVersion + 1;
            }
            if (!useServerOnly) {
                builder.setPositiveButton(R.string.use_local_version, (dialog, which) -> {
                    logic.fixElementWithConflict(getActivity(), newVersion, elementLocal, elementOnServer);
                    ReviewAndUpload.showDialog(getActivity(), null);
                });
            }
            final FragmentActivity activity = getActivity();
            builder.setNeutralButton(R.string.use_server_version, (dialog, which) -> {
                PostAsyncActionHandler handler = new PostAsyncActionHandler() {
                    @Override
                    public void onSuccess() {
                        if (activity instanceof Main) {
                            ((Main) activity).invalidateMap();
                        }
                        if (App.getDelegator().hasChanges()) {
                            ReviewAndUpload.showDialog(activity, null);
                        }
                    }

                    @Override
                    public void onError(@Nullable AsyncResult result) {
                        Snack.toastTopError(activity, activity.getString(R.string.toast_download_server_version_failed, elementLocal.getDescription()));
                    }
                };
                if (elementOnServer != null) {
                    logic.replaceElement(activity, elementLocal, handler);
                } else { // delete local element
                    logic.updateToDeleted(activity, elementLocal);
                    handler.onSuccess();
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
