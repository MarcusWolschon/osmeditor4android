package io.vespucci;

import android.content.Context;
import androidx.annotation.NonNull;
import io.vespucci.Main;
import io.vespucci.layer.LayerType;
import io.vespucci.prefs.AdvancedPrefDatabase;

public final class LayerUtils {

    /**
     * Private constructor to stop instantiation
     */
    private LayerUtils() {
        // don't instantiate
    }

    /**
     * Remove imagery layers
     * 
     * @param context Android context
     */
    public static void removeImageryLayers(@NonNull Context context) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            db.deleteLayer(LayerType.IMAGERY, null);
            db.deleteLayer(LayerType.OVERLAYIMAGERY, null);
        }
    }

    /**
     * Remove task layer
     * 
     * @param context Android context
     */
    public static void removeTaskLayer(@NonNull Context context) {
        removeLayer(context,LayerType.TASKS);
    }
    
    /**
     * Remove  layer
     * 
     * @param context Android context
     */
    public static void removeLayer(@NonNull Context context, @NonNull LayerType type) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            db.deleteLayer(type, null);
        }
    }

    /**
     * If not present add the task layer
     * 
     * @param main the current instance of main
     */
    public static void addTaskLayer(@NonNull Main main) {
        if (main.getMap().getTaskLayer() == null) {
            io.vespucci.layer.Util.addLayer(main, LayerType.TASKS);
            main.getMap().setUpLayers(main);
        }
    }
}
