package de.blau.android.dialogs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
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
