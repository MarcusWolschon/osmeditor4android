package de.blau.android;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.easyedit.SimpleActionModeCallback.SimpleAction;
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
    <M extends Map<String, String> & Serializable> M getExtraTags(@NonNull Logic logic, @NonNull OsmElement e);

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
    <L extends List<PresetElementPath> & Serializable> L getPresetItems(@NonNull Context ctx, @NonNull OsmElement e);

    /**
     * Return the simple actions that are enabled in the menu
     * 
     * @return a Set of SimpleAction
     */
    @NonNull
    default Set<SimpleAction> enabledSimpleActions() {
        EnumSet<SimpleAction> actions = EnumSet.allOf(SimpleAction.class);
        actions.remove(SimpleAction.ADDRESS_NODE);
        actions.remove(SimpleAction.INTERPOLATION_WAY);
        actions.remove(SimpleAction.VOICE_NODE);
        actions.remove(SimpleAction.VOICE_NOTE);
        return Collections.unmodifiableSet(actions);
    }
}
