package de.blau.android;

import java.io.IOException;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.resources.TileLayerSource.TileType;
import okhttp3.mockwebserver.MockWebServer;

public final class MockTileServer {

    private static final String DEBUG_TAG = MockTileServer.class.getName();

    public static final String MOCK_TILE_SOURCE = "VESPUCCITEST";

    /**
     * Private constructor to stop instantiation
     */
    private MockTileServer() {
        // don't instantiate
    }

    /**
     * Setup a mock web server that serves tiles from a MBT source and set it to the current source
     * 
     * @param context an Android Context
     * @param prefs the current Preferences
     * @param mbtSource the MBT file name
     * @param removeLayers if true remove any other layers
     * @return a MockWebServer
     */
    public static MockWebServer setupTileServer(@NonNull Context context, @NonNull Preferences prefs, @NonNull String mbtSource, boolean removeLayers) {
        return setupTileServer(context, prefs, mbtSource, removeLayers, LayerType.IMAGERY, TileType.BITMAP, MOCK_TILE_SOURCE);
    }

    /**
     * Setup a mock web server that serves tiles from a MBT source and set it to the current source
     * 
     * @param context an Android Context
     * @param prefs the current Preferences
     * @param mbtSource the MBT file name
     * @param removeLayers if true remove any other layers
     * @param layerType the LayerType
     * @param tileType the type of tiles to serve
     * @param id layer id
     * @return a MockWebServer
     */
    public static MockWebServer setupTileServer(@NonNull Context context, @NonNull Preferences prefs, @NonNull String mbtSource, boolean removeLayers,
            @NonNull LayerType layerType, @NonNull TileType tileType, @NonNull String id) {
        MockWebServer tileServer = new MockWebServer();
        try {
            tileServer.setDispatcher(new TileDispatcher(context, mbtSource));
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "setDispatcher " + e.getMessage());
            e.printStackTrace();
        }

        String tileUrl = tileServer.url("/").toString() + "{zoom}/{x}/{y}";
        Log.i(DEBUG_TAG, "Set up tileserver on " + tileUrl + " for id " + id);
        try (TileLayerDatabase db = new TileLayerDatabase(context)) {
            TileLayerSource.addOrUpdateCustomLayer(context, db.getWritableDatabase(), id, null, -1, -1, "Vespucci Test", new Provider(), Category.other, null,
                    tileType, 0, 19, TileLayerSource.DEFAULT_TILE_SIZE, false, tileUrl);
        }
        if (removeLayers) {
            LayerUtils.removeImageryLayers(context);
        }
        de.blau.android.layer.Util.addLayer(context, layerType, id);
        return tileServer;
    }
}
