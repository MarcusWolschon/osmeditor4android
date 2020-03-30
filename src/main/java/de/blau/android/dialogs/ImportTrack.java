package de.blau.android.dialogs;

import java.io.FileNotFoundException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for a file name to save to
 *
 */
public class ImportTrack extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = ImportTrack.class.getSimpleName();

    private static final String URI_KEY = "uri";

    private static final String TAG = "fragment_import_track";

    private Uri uri;

    /**
     * Show a dialog allowing the user to confirm the track upload and set privacy options
     * 
     * @param activity the calling Activity
     * @param uri the Uri of the track to be imported
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Uri uri) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ImportTrack importTrackFragment = newInstance(uri);
            importTrackFragment.show(fm, TAG);
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
     * Get a new instance of the ImportTrack dialog
     * 
     * @param uri the Uri of the track to be imported
     * @return a new ImportTrack dialog instance
     */
    @NonNull
    private static ImportTrack newInstance(@NonNull Uri uri) {
        ImportTrack f = new ImportTrack();
        Bundle args = new Bundle();
        args.putParcelable(URI_KEY, uri);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uri = getArguments().getParcelable(URI_KEY);
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.existing_track_title);
        builder.setMessage(R.string.existing_track_message);

        builder.setPositiveButton(R.string.replace, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Main main = (Main) getActivity();
                if (main != null) {
                    main.getTracker().stopTracking(true);
                    try {
                        main.getTracker().importGPXFile(main, uri, null);
                    } catch (FileNotFoundException e) {
                        try {
                            Snack.barError(main, main.getResources().getString(R.string.toast_file_not_found, uri.toString()));
                        } catch (Exception ex) {
                            // protect against translation errors
                        }
                    }
                }
            }
        });
        builder.setNeutralButton(R.string.keep, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Main main = (Main) getActivity();
                if (main != null) {
                    main.getTracker().stopTracking(false);
                    try {
                        main.getTracker().importGPXFile(main, uri, null);
                    } catch (FileNotFoundException e) {
                        try {
                            Snack.barError(main, main.getResources().getString(R.string.toast_file_not_found, uri.toString()));
                        } catch (Exception ex) {
                            // protect against translation errors
                        }
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }
}
