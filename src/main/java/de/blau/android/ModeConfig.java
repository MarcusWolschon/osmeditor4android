package de.blau.android;

import java.util.HashMap;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset;

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
	 * @param main the current instance of Main
	 * @param logic the current instance of Logic
	 */
	void teardown(Main main, Logic logic);
	
	/**
	 * Called before PropertyEditor startup to provide any mode specific tags
	 * 
	 * @param logic
	 * @param e
	 * @return
	 */
	@Nullable
	HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e);
	
	/**
	 * 
	 * @param logic
	 * @param e
	 * @return
	 */
	@Nullable
	Preset getPreset(@NonNull Logic logic, @NonNull OsmElement e);
}
