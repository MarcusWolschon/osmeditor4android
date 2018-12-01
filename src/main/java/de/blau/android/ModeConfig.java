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
    void setup(@NonNull Main main, @NonNull Logic logic);

    /**
     * Restore any necessary state and other cleanup
     * 
     * @param main the current instance of Main
     * @param logic the current instance of Logic
     */
    void teardown(@NonNull Main main, @NonNull Logic logic);

    /**
     * Called before PropertyEditor startup to provide any mode specific tags
     * 
     * Note explicit HashMap as the result needs to be Serializable
     * 
     * @param logic the current instance of Logic
     * @param e selected OsmElement
     * @return HashMap with tags to apply
     */
    @Nullable
    HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e);

    /**
     * Called before PropertyEditor startup to provide any mode specific PresetItems
     * 
     * Note explicit ArrayList as the result needs to be Serializable
     * 
     * @param ctx Android context
     * @param e selected OsmElement
     * @return list of PrestItems to apply
     */
    @Nullable
    ArrayList<PresetElementPath> getPresetItems(@NonNull Context ctx, @NonNull OsmElement e);
}
