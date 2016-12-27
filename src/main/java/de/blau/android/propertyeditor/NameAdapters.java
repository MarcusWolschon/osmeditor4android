package de.blau.android.propertyeditor;

import java.util.ArrayList;

import android.widget.ArrayAdapter;
import de.blau.android.presets.ValueWithCount;

/**
 * Interface for retrieving name adapaters
 */
interface NameAdapters {

	ArrayAdapter<ValueWithCount> getStreetNameAdapter(ArrayList<String> tagValues);

	ArrayAdapter<ValueWithCount> getPlaceNameAdapter(ArrayList<String> tagValues);
}

