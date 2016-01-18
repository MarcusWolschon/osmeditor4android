package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import android.widget.ArrayAdapter;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.presets.Preset.PresetItem;

/**
 * Interface for retrieving name adapaters
 */
abstract interface NameAdapters {

	abstract ArrayAdapter<ValueWithCount> getStreetNameAutocompleteAdapter(ArrayList<String> tagValues);

	abstract ArrayAdapter<ValueWithCount> getPlaceNameAutocompleteAdapter(ArrayList<String> tagValues);
}

