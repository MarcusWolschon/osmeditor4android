package de.blau.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.PresetElementPath;

public interface ModeConfig {
	
	/**
	 * Setup any necessary logic for a mode and save any state that may need restoring
	 * 
	 * @param main the current instance of Main
	 * @param logic the current instance of Logic
	 */
	void setup(Main main, Logic logic);
	
	/**
	 * Restore any necessary state and other cleanup
	 * 
	 * @param main	the current instance of Main
	 * @param logic	the current instance of Logic
	 */
	void teardown(Main main, Logic logic);
	
	/**
	 * Called before PropertyEditor startup to provide any mode specific tags
	 * 
	 * @param logic	the current instance of Logic
	 * @param e		selected OsmElement
	 * @return Map with tags to apply 
	 */
	@Nullable
	HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e);
	
	/**
	 * Called before PropertyEditor startup to provide any mode specific PresetItems
	 * 
	 * @param ctx	Android context
	 * @param e		selected OsmElement
	 * @return list of PrestItems to apply
	 */
	@Nullable
	ArrayList<PresetElementPath> getPresetItems(@NonNull Context ctx, @NonNull OsmElement e);
}
