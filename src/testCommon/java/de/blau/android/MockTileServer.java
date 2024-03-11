package de.blau.android;

import static org.junit.Assert.fail;

import java.io.IOException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.resources.MBTileConstants;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.services.util.MBTileProviderDataBase;
import okhttp3.mockwebserver.MockWebServer;

public final class MockTileServer {

    private static final String DEBUG_TAG = MockTileServer.class.getSimpleName().substring(0, Math.min(23, MockTileServer.class.getSimpleName().length()));

    public static final String MOCK_TILE_SOURCE = "VespucciTest";

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
     * @param mbtSource the MBT file name
     * @param removeLayers if true remove any other layers
     * @return a MockWebServer
     */
    @NonNull
    public static MockWebServer setupTileServer(@NonNull Context context, @NonNull String mbtSource, boolean removeLayers) {
        return setupTileServer(context, mbtSource, removeLayers, LayerType.IMAGERY, TileType.BITMAP, MOCK_TILE_SOURCE);
    }

    /**
     * Setup a mock web server that serves tiles from a MBT source and set it to the current source
     * 
     * @param context an Android Context
     * @param mbtSource the MBT file name
     * @param removeLayers if true remove any other layers
     * @param layerType the LayerType
     * @param tileType the type of tiles to serve
     * @param id layer id
     * @return a MockWebServer
     */
    @NonNull
    public static MockWebServer setupTileServer(@NonNull Context context, @NonNull String mbtSource, boolean removeLayers, @NonNull LayerType layerType,
            @NonNull TileType tileType, @NonNull String id) {
        MockWebServer tileServer = new MockWebServer();
        try {
            TileDispatcher tileDispatcher = new TileDispatcher(context, mbtSource);
            tileServer.setDispatcher(tileDispatcher);
            MBTileProviderDataBase mbt = tileDispatcher.getSource();
            java.util.Map<String, String> metadata = mbt.getMetadata();
            String name = "Vespucci Test";
            if (metadata != null && metadata.containsKey(MBTileConstants.NAME)) {
                name = metadata.get(MBTileConstants.NAME);
            }
            String tileUrl = tileServer.url("/").toString() + "{zoom}/{x}/{y}";
            Log.i(DEBUG_TAG, "Set up tileserver on " + tileUrl + " for id " + id);
            try (TileLayerDatabase db = new TileLayerDatabase(context); SQLiteDatabase writableDatabase = db.getWritableDatabase()) {
                TileLayerDatabase.deleteLayerWithId(writableDatabase, id);
                TileLayerSource.addOrUpdateCustomLayer(context, writableDatabase, id, null, -1, -1, name, new Provider(), Category.other, null, tileType,
                        mbt.getMinMaxZoom()[0], mbt.getMinMaxZoom()[1], TileLayerSource.DEFAULT_TILE_SIZE, false, tileUrl);
                TileLayerSource.getListsLocked(context, writableDatabase, true);
            }
            if (removeLayers) {
                LayerUtils.removeImageryLayers(context);
            }
            if (layerType == LayerType.IMAGERY || layerType == LayerType.OVERLAYIMAGERY) {
                try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
                    de.blau.android.layer.Util.addImageryLayer(db, db.getLayers(), LayerType.OVERLAYIMAGERY.equals(layerType), id);
                }
            } else {
                de.blau.android.layer.Util.addLayer(context, layerType, id);
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "setDispatcher " + e.getMessage());
            fail("setupTileServer " + e.getMessage());
        }
        return tileServer;
    }
}
