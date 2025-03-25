package io.vespucci.util;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public final class FragmentUtil {
    private static final String DEBUG_TAG = FragmentUtil.class.getSimpleName().substring(0, Math.min(23, FragmentUtil.class.getSimpleName().length()));

    /**
     * Private constructor
     */
    private FragmentUtil() {
        // nothing
    }

    /**
     * Find a Fragment by tag, descending in to one layer of children
     * 
     * @param activity current Activity
     * @param tag the tag
     * @return the fragment or null if not found
     */
    @Nullable
    public static Fragment findFragmentByTag(@NonNull FragmentActivity activity, @NonNull String tag) {
        FragmentManager fm = activity.getSupportFragmentManager();
        List<Fragment> parents = new ArrayList<>(fm.getFragments());
        for (Fragment parent : parents) {
            Fragment fragment = parent.getChildFragmentManager().findFragmentByTag(tag);
            if (fragment != null) {
                return fragment;
            }
        }
        Log.e(DEBUG_TAG, "Fragment not found");
        return null;
    }

    /**
     * Get the Dialog associated with a DialogFragment by tag
     * 
     * @param activity current Activity
     * @param tag the tag
     * @return the Dialog or null if not found
     */
    @Nullable
    public static Dialog findDialogByTag(@NonNull FragmentActivity activity, @NonNull String tag) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment instanceof DialogFragment) {
            return ((DialogFragment) fragment).getDialog();
        }
        Log.e(DEBUG_TAG, "Fragment not found or not a DialogFragment");
        return null;
    }
}
