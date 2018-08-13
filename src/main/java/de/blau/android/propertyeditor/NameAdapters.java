package de.blau.android.propertyeditor;

import java.util.List;

import android.widget.ArrayAdapter;
import de.blau.android.presets.ValueWithCount;

/**
 * Interface for retrieving name adapaters
 */
interface NameAdapters {

    /**
     * Gets an adapter for the auto-completion of street names based on the neighborhood of the edited item. Note the
     * adapter will be cached
     * 
     * @param values any existing values for the key
     * @return an ArrayAdapter for street names
     */
    ArrayAdapter<ValueWithCount> getStreetNameAdapter(List<String> values);

    /**
     * Gets an adapter for the auto-completion of place names based on the neighborhood of the edited item. Note the
     * adapter will be cached
     * 
     * @param values any existing values for the key
     * @return an ArrayAdapter for place names
     */
    ArrayAdapter<ValueWithCount> getPlaceNameAdapter(List<String> values);
}
