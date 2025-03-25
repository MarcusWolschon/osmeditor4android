package io.vespucci.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.layer.LayerConfig;
import io.vespucci.layer.LayerType;
import io.vespucci.layer.Util;
import io.vespucci.prefs.AdvancedPrefDatabase;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class LayerDBTest {

    /**
     * Add two GPX layers, remove the one with a NULL content id
     */
    @Test
    public void removeGPXNull() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        Util.addLayer(ctx, LayerType.GPX);
        Util.addLayer(ctx, LayerType.GPX, "test");
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
            LayerConfig[] layerConfigs = db.getLayers();
            int count = layerConfigs.length;
            assertTrue(hasLayer(layerConfigs, LayerType.GPX, null));
            assertTrue(hasLayer(layerConfigs, LayerType.GPX, "test"));
            db.deleteLayer(LayerType.GPX);
            layerConfigs = db.getLayers();
            assertEquals(count - 1, layerConfigs.length);
            assertFalse(hasLayer(layerConfigs, LayerType.GPX, null));
            assertTrue(hasLayer(layerConfigs, LayerType.GPX, "test"));
        }
    }

    /**
     * Test if layer is in layer config
     * 
     * @param layerConfigs an array of LayerConfig
     * @param type the layer type
     * @param contentId the layer id
     * @return true if the layer is present
     */
    private boolean hasLayer(@NonNull LayerConfig[] layerConfigs, @NonNull LayerType type, @Nullable String contentId) {
        for (LayerConfig l : layerConfigs) {
            final String id = l.getContentId();
            if (l.getType() == type && ((id != null && id.equals(contentId)) || (id == null && contentId == null))) {
                return true;
            }
        }
        return false;
    }
}
