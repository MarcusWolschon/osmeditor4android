package de.blau.android.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import de.blau.android.HelpViewer;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.Util;

/**
 * Display a dialog giving new users minimal instructions
 *
 */
public class Newbie extends DialogFragment {

    private static final String DEBUG_TAG = Newbie.class.getSimpleName();

    private static final String TAG = "fragment_newbie";

    private Main main;

    /**
     * Display a dialog giving new users minimal instructions
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            Newbie newbieFragment = newInstance();
            newbieFragment.show(fm, TAG);
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
     * Get a new instance of Newbie
     * 
     * @return a new Newbie instance
     */
    @NonNull
    private static Newbie newInstance() {
        Newbie f = new Newbie();
        f.setShowsDialog(true);
        return f;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(DEBUG_TAG, "onAttach");
        super.onAttach(context);
        if (!(context instanceof Main)) {
            throw new ClassCastException(context.toString() + " can only be called from Main");
        }
        main = (Main) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(null);
        builder.setTitle(R.string.welcome_title);
        String message = getString(R.string.welcome_message);
        if (main.isFullScreen()) {
            message = message + getString(R.string.welcome_message_fullscreen);
        }
        builder.setMessage(Util.fromHtml(message));
        builder.setPositiveButton(R.string.okay, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Main main = (Main) getActivity();
                if (main != null) {
                    main.gotoBoxPicker(R.string.boxpicker_firsttimetitle);
                } else {
                    // FIXME do something intelligent here
                    Log.e(DEBUG_TAG, "getActivity returned null in onClick");
                }
            }
        });
        builder.setNeutralButton(R.string.read_introduction, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Context context = getActivity();
                if (context != null) {
                    HelpViewer.start(context, R.string.help_introduction);
                } else {
                    // FIXME do something intelligent here
                    Log.e(DEBUG_TAG, "getActivity returned null in onClick");
                }
            }
        });

        return builder.create();
    }
}
