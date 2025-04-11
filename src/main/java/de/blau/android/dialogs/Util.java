package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ScreenMessage;

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
    private static List<OsmElement> getElementsFromBundle(@NonNull Bundle bundle) {
        List<OsmElement> result = new ArrayList<>();
        List<Long> ids = de.blau.android.util.Util.getSerializeableArrayList(bundle, ELEMENT_IDS_KEY, Long.class);
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

    /**
     * Get elements from references in a bundle
     * 
     * @param context an Android context
     * @param bundle the Bundle
     */
    static List<OsmElement> getElements(@NonNull Context context, @NonNull Bundle bundle) {
        try {
            return de.blau.android.dialogs.Util.getElementsFromBundle(bundle);
        } catch (IllegalStateException ise) {
            ScreenMessage.toastTopError(context, R.string.toast_inconsistent_state);
            Log.e(DEBUG_TAG, "Inconsistent state because " + ise.getMessage());
            ACRAHelper.nocrashReport(ise, ise.getMessage());
            return new ArrayList<>();
        }
    }
}
