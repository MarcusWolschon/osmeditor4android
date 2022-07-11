package de.blau.android.util;

// other imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.presets.ValueWithCount;

/**
 * Project: OSMEditor
 * 
 * TagKeyAutocompletionAdapter.java
 * 
 * created: 12.06.2010 10:43:37
 * 
 * <b>Adapter for the {@link AutoCompleteTextView} in the {@link PropertyEditor} that is for the VALUE for the key
 * "addr:street" .</b>
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @author Simon Poole
 */
public class StreetPlaceNamesAdapter extends ArrayAdapter<ValueWithCount> {

    /**
     * The tag we use for Android-logging.
     */
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = StreetPlaceNamesAdapter.class.getName();

    private ElementSearch es;

    /**
     * Get an Adapter containing near by street or place names
     * 
     * @param aContext Android context
     * @param aTextViewResourceId the resource id of the AutoCompleteTextView
     * @param delegator the current StorageDelegator instance
     * @param osmElementType the type of OsmElement
     * @param osmId the id of the OsmElement
     * @param extraValues any existing values
     * @param places if true return an adapter for place names
     */
    public StreetPlaceNamesAdapter(@NonNull final Context aContext, final int aTextViewResourceId, @NonNull final StorageDelegator delegator,
            @NonNull final String osmElementType, final long osmId, @Nullable List<String> extraValues, boolean places) {
        super(aContext, aTextViewResourceId);
        Log.d(DEBUG_TAG, "constructor called");

        Map<String, Integer> counter = new HashMap<>();
        if (extraValues != null && !extraValues.isEmpty()) {
            for (String t : extraValues) {
                if ("".equals(t)) {
                    continue;
                }
                if (counter.containsKey(t)) {
                    counter.put(t, counter.get(t) + 1);
                } else {
                    counter.put(t, 1);
                }
            }
            List<String> keys = new ArrayList<>(counter.keySet());
            Collections.sort(keys);
            for (String t : keys) {
                ValueWithCount v = new ValueWithCount(t, counter.get(t));
                super.add(v);
            }
        }

        IntCoordinates center = Util.getCenter(delegator, osmElementType, osmId);
        if (center != null) {
            es = new ElementSearch(center, false);
            String[] names = places ? es.getPlaceNames() : es.getStreetNames();
            for (String s : names) {
                if (counter.size() > 0 && counter.containsKey(s)) {
                    continue; // skip values that we already have
                }
                ValueWithCount v = new ValueWithCount(s);
                super.add(v);
            }
        } else {
            Log.e(DEBUG_TAG, "center for " + osmElementType + " " + osmId + " is null");
        }
    }

    /**
     * Get the names in this adapter
     * 
     * @return a String array containing the names
     */
    public String[] getStreetNames() {
        return es.getStreetNames();
    }

    /**
     * Get the osm id for a specific street
     * 
     * @param name the name
     * @return the osm id
     * @throws OsmException if the name is not found
     */
    public long getStreetId(@NonNull String name) throws OsmException {
        return es.getStreetId(name);
    }

    /**
     * This avoids generating everything twice
     * 
     * @return the ElementSearch instance used to create the adapter
     */
    public ElementSearch getElementSearch() {
        return es;
    }
}
