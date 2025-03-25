package io.vespucci.dialogs;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.vespucci.App;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.StorageDelegator;

public final class Util {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Util.class.getSimpleName().length());
    private static final String DEBUG_TAG = Util.class.getSimpleName().substring(0, TAG_LEN);

    private static final String ELEMENT_IDS_KEY   = "ids";
    private static final String ELEMENT_TYPES_KEY = "types";

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
    static void dismiss(FragmentManager fm, @NonNull String tag) {
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

    /**
     * Put element ids and types in to a bundle
     * 
     * @param elements a List of OsmELement
     * @param bundle the target bundle
     */
    static void putElementsInBundle(@NonNull List<OsmElement> elements, @NonNull Bundle bundle) {
        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<String> types = new ArrayList<>();
        for (OsmElement e : elements) {
            ids.add(e.getOsmId());
            types.add(e.getName());
        }
        bundle.putStringArrayList(ELEMENT_TYPES_KEY, types);
        bundle.putSerializable(ELEMENT_IDS_KEY, ids);
    }

    /**
     * Get elements from bundle if any
     * 
     * @param bundle the bundle
     * @return return a List of elements or null
     */
    @Nullable
    static List<OsmElement> getElementsFromBundle(@NonNull Bundle bundle) {
        List<OsmElement> result = new ArrayList<>();
        List<Long> ids = io.vespucci.util.Util.getSerializeableArrayList(bundle, ELEMENT_IDS_KEY, Long.class);
        List<String> types = bundle.getStringArrayList(ELEMENT_TYPES_KEY);
        if (ids != null && types != null) {
            if (ids.size() != types.size()) {
                throw new IllegalArgumentException("Mismatched ids types size " + ids.size() + " != " + types.size());
            }
            StorageDelegator delegator = App.getDelegator();
            for (int i = 0; i < ids.size(); i++) {
                final String elementType = types.get(i);
                final long osmId = ids.get(i);
                OsmElement e = delegator.getOsmElement(elementType, osmId);
                if (e == null) {
                    throw new IllegalStateException(elementType + " " + osmId + " not in memory");
                }
                result.add(e);
            }
            return result;
        }
        return null;
    }
}
