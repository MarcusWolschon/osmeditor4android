package de.blau.android.dialogs;

import org.acra.ACRA;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
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
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.UploadResult;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.ThemeUtils;

/**
 * Dialog to resolve upload conflicts one by one
 * 
 * @author simon
 *
 */
public class UploadConflict extends DialogFragment {

    private static final String UPLOADRESULT = "uploadresult";

    private static final String DEBUG_TAG = UploadConflict.class.getSimpleName();

    private static final String TAG = "fragment_upload_conflict";

    private UploadResult result;

    static public void showDialog(FragmentActivity activity, UploadResult result) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        try {
            UploadConflict uploadConflictDialogFragment = newInstance(result);
            uploadConflictDialogFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
    }

    private static void dismissDialog(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(TAG);
        try {
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
    static private UploadConflict newInstance(final UploadResult result) {
        UploadConflict f = new UploadConflict();

        Bundle args = new Bundle();
        args.putSerializable(UPLOADRESULT, result);

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
        result = (UploadResult) getArguments().getSerializable(UPLOADRESULT);
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
                    builder.setPositiveButton(R.string.retry, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((Main) getActivity()).confirmUpload(); // FIXME this should be made independent from Main
                        }
                    });
                    return builder.create();
                } else {
                    builder.setMessage(
                            res.getString(R.string.upload_conflict_message_deleted, elementLocal.getDescription(true), elementLocal.getOsmVersion()));
                }
                newVersion = elementLocal.getOsmVersion() + 1;
            }
            if (!useServerOnly) {
                builder.setPositiveButton(R.string.use_local_version, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logic.fixElementWithConflict(getActivity(), newVersion, elementLocal, elementOnServer);
                        ((Main) getActivity()).confirmUpload(); // FIXME this should be made independent from Main
                    }
                });
            }
            builder.setNeutralButton(R.string.use_server_version, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    StorageDelegator storageDelegator = App.getDelegator();
                    storageDelegator.removeFromUpload(elementLocal);
                    if (elementOnServer != null) {
                        logic.downloadElement(getActivity(), elementLocal.getName(), elementLocal.getOsmId(), false, true, null);
                    } else { // delete local element
                        logic.updateToDeleted(getActivity(), elementLocal);
                    }
                    if (!storageDelegator.getApiStorage().isEmpty()) {
                        ((Main) getActivity()).confirmUpload(); // FIXME this should be made independent from Main
                    }
                }
            });
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Caught exception " + e);
            ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
            ACRA.getErrorReporter().handleException(e);
        }
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }
}
