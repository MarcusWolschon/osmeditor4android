package io.vespucci.propertyeditor;

import android.util.Log;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.vespucci.presets.PresetItem;

public final class Util {

    private static final String DEBUG_TAG = Util.class.getSimpleName().substring(0, Math.min(23, Util.class.getSimpleName().length()));

    /**
     * Disallow instantiation
     */
    private Util() {
        // not used
    }

    /**
     * Dismiss a child Fragment
     * 
     * @param fm the FragmentManager from the calling Fragment
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

    /**
     * Add the MRU presets fragment
     * 
     * @param fm a FragmentManager
     * @param container container resource id
     * @param elementId the id of an osm element
     * @param elementName the type of an osm element
     */
    public static void addMRUPresetsFragment(@NonNull FragmentManager fm, int container, long elementId, @NonNull String elementName) {
        Log.d(DEBUG_TAG, "Adding MRU prests");
        FragmentTransaction ft = fm.beginTransaction();
        Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditorFragment.RECENTPRESETS_FRAGMENT);
        if (recentPresetsFragment != null) {
            ft.remove(recentPresetsFragment);
        }
        recentPresetsFragment = RecentPresetsFragment.newInstance(elementId, elementName);
        ft.add(container, recentPresetsFragment, PropertyEditorFragment.RECENTPRESETS_FRAGMENT);
        ft.commit();
    }

    /**
     * Add the alternative presets fragment
     * 
     * @param fm a FragmentManager
     * @param container container resource id
     * @param item the PresetItem that has alternatives or null
     */
    public static void addAlternativePresetItemsFragment(@NonNull FragmentManager fm, int container, @Nullable PresetItem item) {
        if (item != null && item.getAlternativePresetItems() != null) {
            Log.d(DEBUG_TAG, "Adding alternative presets");
            FragmentTransaction ft = fm.beginTransaction();
            Fragment alternativePresetItemsFragment = AlternativePresetItemsFragment.newInstance(item.getPath(item.getPreset().getRootGroup()));
            ft.replace(container, alternativePresetItemsFragment, AlternativePresetItemsFragment.TAG);
            ft.commit();
        } else {
            Fragment alternativePresetItemsFragment = fm.findFragmentByTag(AlternativePresetItemsFragment.TAG);
            if (alternativePresetItemsFragment != null) {
                Log.d(DEBUG_TAG, "Removing alternative presets");
                FragmentTransaction ft = fm.beginTransaction();
                ft.remove(alternativePresetItemsFragment);
                ft.commit();
            }
        }
    }

    /**
     * Remove all but one child of a LinearLayout, run the runnable for the remaining one
     * 
     * @param layout the LinearLayer
     * @param runnable the Runnable
     */
    public static void resetValueLayout(@NonNull LinearLayout layout, @NonNull Runnable runnable) {
        int childCount = layout.getChildCount();
        for (int pos = 0; pos < childCount; pos++) { // don't delete first child, just clear
            if (pos == 0) {
                runnable.run();
            } else {
                layout.removeViewAt(1);
            }
        }
    }
}
