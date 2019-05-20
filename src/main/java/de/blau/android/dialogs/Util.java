package de.blau.android.dialogs;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public final class Util {
    private static final String DEBUG_TAG = "dialogs.Util";

    /**
     * Disallow instantiation
     */
    private Util() {
        // not used
    }

    /**
     * Dismiss a DialogFragment
     * 
     * @param activity the calling FragmentActivity
     * @param tag the tag of the DialogFragment we want to remove
     */
    public static void dismissDialog(@NonNull FragmentActivity activity, @NonNull String tag) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment != null) {
            ft.remove(fragment);
        }
        try {
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog " + tag, isex);
        }
    }
}
