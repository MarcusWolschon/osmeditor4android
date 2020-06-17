package de.blau.android.layer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.prefs.AdvancedPrefDatabase;

public final class Util {
    /**
     * Private constructor to avoid instantiation
     */
    private Util() {
        // private
    }

    /**
     * Add a layer on top
     * 
     * @param context an Android Context
     * @param type the LayerType
     */
    public static void addLayer(@NonNull final Context context, @NonNull LayerType type) {
        addLayer(context, type, null);
    }

    /**
     * Add a layer on top
     * 
     * @param context an Android Context
     * @param type the LayerType
     */
    public static void addLayer(@NonNull final Context context, @NonNull LayerType type, @Nullable String contentId) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            db.insertLayer(db.layerCount(), type, true, contentId);
        }
    }
}
