package de.blau.android.propertyeditor;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public final class Util {

    private static final String DEBUG_TAG = "propertyeditor.Util";

    /**
     * Disallow instantiation
     */
    private Util() {
        // not used
    }
    
    /**
     * Dismiss a child Fragment
     * 
     * @param fm  the FragmentManager from the calling Fragment
     * @param tag the tag of the Fragment we want to remove
     */
    public static void removeChildFragment(@NonNull FragmentManager fm, @NonNull String tag) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(tag);
        if (prev != null) {
            ft.remove(prev);
        }
        try {
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "removeChildFragment " + tag, isex);
        }
    }
}
