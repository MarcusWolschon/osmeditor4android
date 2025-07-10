package de.blau.android.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.JavaResources;
import de.blau.android.MockTileServer;
import de.blau.android.layer.tiles.util.MapTileProviderCallback;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.util.FileUtil;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowSQLiteStatement.class, ShadowSQLiteProgram.class, ShadowSQLiteCloseable.class, ShadowSQLiteQuery.class }, sdk=33)
@LargeTest
public class MapTileFilesystemProviderLocalTest {

    private static final String FIRENZE = "FIRENZE";
    MapTileFilesystemProvider   provider;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        ShadowLog.setupLogging();
        provider = new MapTileFilesystemProvider(ApplicationProvider.getApplicationContext(), new File("."), 1000000);
        try {
            JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "ersatz_background.mbt", null, "/");
            JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "protomaps(vector)ODbL_firenze.pmtiles", null, "/");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            File mbtFile = new File(FileUtil.getPublicDirectory(), "ersatz_background.mbt");
            final String windowsRoot = "file://" + (System.getProperty("os.name").toLowerCase().contains("windows") ? "\\" : "");
            TileLayerSource.addOrUpdateCustomLayer(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), MockTileServer.MOCK_TILE_SOURCE, null,
                    -1, -1, "Vespucci Test", new Provider(), Category.other, null, null, 0, 19, TileLayerSource.DEFAULT_TILE_SIZE, false,
                    windowsRoot + mbtFile.getAbsolutePath());
            File pmTilesFile = new File(FileUtil.getPublicDirectory(), "protomaps(vector)ODbL_firenze.pmtiles");
            TileLayerSource.addOrUpdateCustomLayer(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), FIRENZE, null, -1, -1, "Firenze",
                    new Provider(), Category.other, TileLayerSource.TYPE_PMT_3, TileType.MVT, 0, 15, TileLayerSource.DEFAULT_TILE_SIZE, false,
                    windowsRoot + pmTilesFile.getAbsolutePath());
        } catch (IOException e) {
            fail(e.getMessage());
        }
        // force update of tile sources
        try (TileLayerDatabase tlDb = new TileLayerDatabase(ApplicationProvider.getApplicationContext()); SQLiteDatabase db = tlDb.getReadableDatabase()) {
            TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db, false);
        }
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        provider.destroy();
    }

    @Test
    public void successfulMBT() {
        loadMapTileAsyncSuccessTest(MockTileServer.MOCK_TILE_SOURCE, 19, 274335, 183513);
    }

    @Test
    public void failMBT() {
        loadMapTileAsyncFailTest(MockTileServer.MOCK_TILE_SOURCE, 14, 11541, 3864);
    }

    @Test
    public void successfulPMT() {
        loadMapTileAsyncSuccessTest(FIRENZE, 14, 8704, 5972);
    }

    @Test
    public void failPMT() {
        loadMapTileAsyncFailTest(FIRENZE, 14, 11541, 3864);
    }

    /**
     * Load a tile successfully
     */
    public void loadMapTileAsyncSuccessTest(@NonNull String source, int zoom, int x, int y) {
        // this should load from the server
        final CountDownLatch signal1 = new CountDownLatch(1);
        MapTile mockedTile = new MapTile(source, zoom, x, y);
        CallbackWithResult callback = new CallbackWithResult() {

            @Override
            public void mapTileLoaded(String rendererID, int zoomLevel, int tileX, int tileY, byte[] aImage) throws IOException {
                result = 1;
                signal1.countDown();
            }

            @Override
            public void mapTileFailed(String rendererID, int zoomLevel, int tileX, int tileY, int reason, String message) throws IOException {
                result = 2;
                signal1.countDown();
            }
        };
        provider.loadMapTileAsync(mockedTile, callback);

        try {
            signal1.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals(1, callback.result);
    }

    abstract class CallbackWithResult implements MapTileProviderCallback {
        int result;
    }

    /**
     * Try to load a tile that doesn't exist
     */
    public void loadMapTileAsyncFailTest(@NonNull String source, int zoom, int x, int y) {
        final CountDownLatch signal1 = new CountDownLatch(1);
        MapTile mockedTile = new MapTile(source, zoom, x, y);
        CallbackWithResult callback = new CallbackWithResult() {

            @Override
            public void mapTileLoaded(String rendererID, int zoomLevel, int tileX, int tileY, byte[] aImage) throws IOException {
                result = 0;
                signal1.countDown();
            }

            @Override
            public void mapTileFailed(String rendererID, int zoomLevel, int tileX, int tileY, int reason, String message) throws IOException {
                result = reason;
                signal1.countDown();
            }
        };
        provider.loadMapTileAsync(mockedTile, callback);
        try {
            signal1.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals(MapAsyncTileProvider.DOESNOTEXIST, callback.result);
    }
}
