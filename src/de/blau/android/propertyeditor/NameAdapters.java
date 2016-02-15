package de.blau.android.propertyeditor;

import java.util.ArrayList;

import android.widget.ArrayAdapter;
import de.blau.android.presets.ValueWithCount;

/**
 * Interface for retrieving name adapaters
 */
abstract interface NameAdapters {

	abstract ArrayAdapter<ValueWithCount> getStreetNameAutocompleteAdapter(ArrayList<String> tagValues);

	abstract ArrayAdapter<ValueWithCount> getPlaceNameAutocompleteAdapter(ArrayList<String> tagValues);
}

