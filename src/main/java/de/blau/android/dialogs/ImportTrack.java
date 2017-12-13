package de.blau.android.dialogs;

import java.io.FileNotFoundException;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for a file name to save to
 *
 */
public class ImportTrack extends DialogFragment {
    private static final String DEBUG_TAG = ImportTrack.class.getSimpleName();

    private static final String URI_KEY = "uri";

    private static final String TAG = "fragment_import_track";

    private Uri uri;

    /**
     
     */
    static public void showDialog(FragmentActivity activity, Uri uri) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ImportTrack importTrackFragment = newInstance(uri);
            importTrackFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    private static void dismissDialog(FragmentActivity activity) {
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
     */
    static private ImportTrack newInstance(Uri uri) {
        ImportTrack f = new ImportTrack();
        Bundle args = new Bundle();
        args.putParcelable(URI_KEY, uri);
        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        if (!(activity instanceof Main)) {
            throw new ClassCastException(activity.toString() + " can ownly be called from Main");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
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
                        main.getTracker().importGPXFile(main, uri);
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
                        main.getTracker().importGPXFile(main, uri);
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
