package de.blau.android.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import de.blau.android.MockTileServer;
import de.blau.android.PMTilesDispatcher;
import de.blau.android.layer.tiles.util.MapTileProviderCallback;
import de.blau.android.net.UserAgentInterceptor;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.util.Util;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowSQLiteStatement.class, ShadowSQLiteProgram.class, ShadowSQLiteCloseable.class }, sdk=33)
@LargeTest
public class MapTileFilesystemProviderTest {

    private static final String FIRENZE = "FIRENZE";
    MapTileFilesystemProvider   provider;
    MockWebServer               tileServerMBT;
    MockWebServer               tileServerPMT;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        ShadowLog.setupLogging();
        provider = new MapTileFilesystemProvider(ApplicationProvider.getApplicationContext(), new File("."), 1000000);
        provider.flushCache(null);
        tileServerMBT = MockTileServer.setupTileServer(ApplicationProvider.getApplicationContext(), "ersatz_background.mbt", true);
        final String fileName = "protomaps(vector)ODbL_firenze.pmtiles";
        tileServerPMT = new MockWebServer();
        try {
            PMTilesDispatcher tileDispatcher = new PMTilesDispatcher(ApplicationProvider.getApplicationContext(), fileName);
            tileServerPMT.setDispatcher(tileDispatcher);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            TileLayerDatabase.deleteLayerWithId(db.getWritableDatabase(), FIRENZE);
            TileLayerSource.addOrUpdateCustomLayer(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), FIRENZE, null, -1, -1, "Firenze",
                    new Provider(), Category.other, TileLayerSource.TYPE_PMT_3, TileType.MVT, 0, 15, TileLayerSource.DEFAULT_TILE_SIZE, false,
                    tileServerPMT.url("/").toString() + "firenze.pmtiles");
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
        try {
            tileServerMBT.close();
        } catch (IOException e) {
            // ignore
        }
        try {
            tileServerPMT.close();
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Save an individual tile
     */
    @Test
    public void saveFileTest() {
        try {
            provider.saveTile(new MapTile("test", 10, 511, 340), MapTileProviderDataBaseTest.getTestTile());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Clear the whole cache
     */
    @Test
    public void clearCurrentCacheTest() {
        saveFileTest();
        assertTrue(provider.getCurrentCacheByteSize() > 0);
        provider.clearCurrentCache();
        assertEquals(0, provider.getCurrentCacheByteSize());
    }

    @Test
    public void loadTileFromTileServerSuccess() {
        loadMapTileAsyncSuccessTest(tileServerMBT, MockTileServer.MOCK_TILE_SOURCE, 19, 274335, 183513);
    }

    @Test
    public void loadTileFromTileServerFail() {
        loadMapTileAsyncFailTest(tileServerMBT, MockTileServer.MOCK_TILE_SOURCE, 14, 11541, 3864);
    }

    @Test
    public void tileServerCustomerHeaders() {
        customHeaderTest(tileServerMBT, MockTileServer.MOCK_TILE_SOURCE, 19, 274335, 183514);
    }

    @Test
    public void loadTileFromPMTilesServerSuccess() {
        loadMapTileAsyncSuccessTest(tileServerPMT, FIRENZE, 14, 8703, 5971);
    }

    @Test
    public void loadTileFromPMTilesServerFail() {
        loadMapTileAsyncFailTest(tileServerPMT, FIRENZE, 14, 11541, 3864);
    }

    @Test
    public void pmtilesServerCustomerHeaders() {
        customHeaderTest(tileServerPMT, FIRENZE, 14, 8703, 5971);
    }

    /**
     * Load a tile successfully
     */
    public void loadMapTileAsyncSuccessTest(@NonNull MockWebServer tileServer, @NonNull String source, int zoom, int x, int y) {
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

        try {
            RecordedRequest request = tileServer.takeRequest(1, TimeUnit.SECONDS);
            System.out.println("checked request " + request);
        } catch (InterruptedException e1) {
            fail("no tileserver request found " + e1.getMessage());
        }
        // flush requests
        try {
            while (tileServer.takeRequest(1, TimeUnit.SECONDS) != null) {
            }
        } catch (InterruptedException e2) {
            fail(e2.getMessage());
        }

        // this should load from the cache
        final CountDownLatch signal2 = new CountDownLatch(1);
        provider.loadMapTileAsync(mockedTile, callback);
        try {
            signal2.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals(1, callback.result);
        try {
            RecordedRequest request = tileServer.takeRequest(1, TimeUnit.SECONDS);
            assertNull(request);
        } catch (InterruptedException e1) {
            // this is what should happen
        }
    }

    abstract class CallbackWithResult implements MapTileProviderCallback {
        int result;
    }

    /**
     * Try to load a tile that doesn't exist
     */
    public void loadMapTileAsyncFailTest(@NonNull MockWebServer tileServer, @NonNull String source, int zoom, int x, int y) {
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
            };
        };
        provider.loadMapTileAsync(mockedTile, callback);
        try {
            signal1.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        try {
            RecordedRequest request = tileServer.takeRequest(1, TimeUnit.SECONDS);
            System.out.println("checked request " + request);
        } catch (InterruptedException e1) {
            fail("no tileserver request found " + e1.getMessage());
        }
        assertEquals(MapAsyncTileProvider.DOESNOTEXIST, callback.result);
    }

    public void customHeaderTest(@NonNull MockWebServer tileServer, @NonNull String source, int zoom, int x, int y) {
        TileLayerSource layer = TileLayerSource.get(ApplicationProvider.getApplicationContext(), source, false);
        assertNotNull(layer);
        layer.setHeaders(Util.wrapInList(new TileLayerSource.Header(UserAgentInterceptor.USER_AGENT_HEADER, "Mozilla/5.0 (JOSM)")));
        // this should load from the server
        final CountDownLatch signal1 = new CountDownLatch(1);
        MapTile mockedTile = new MapTile(source, zoom, x, y); // not this needs to be a
                                                              // different tile than
                                                              // above
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

        try {
            RecordedRequest request = tileServer.takeRequest(1, TimeUnit.SECONDS);
            assertEquals("Mozilla/5.0 (JOSM)", request.getHeader(UserAgentInterceptor.USER_AGENT_HEADER));
        } catch (InterruptedException e1) {
            fail("no tileserver request found " + e1.getMessage());
        }
    }
}
