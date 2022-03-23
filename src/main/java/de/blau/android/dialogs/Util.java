package de.blau.android.dialogs;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
        dismiss(activity.getSupportFragmentManager(), tag);
    }

    /**
     * Dismiss a DialogFragment
     * 
     * @param fragment the calling Fragment
     * @param tag the tag of the DialogFragment we want to remove
     */
    public static void dismissDialog(@NonNull Fragment fragment, @NonNull String tag) {
        dismiss(fragment.getChildFragmentManager(), tag);
    }

    /**
     * Dismiss a DialogFragment
     * 
     * @param fm the FragmentManager
     * @param tag the tag
     */
    private static void dismiss(FragmentManager fm, @NonNull String tag) {
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
