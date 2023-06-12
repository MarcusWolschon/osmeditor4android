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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.MockTileServer;
import de.blau.android.net.UserAgentInterceptor;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.Util;
import de.blau.android.views.util.MapTileProviderCallback;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowSQLiteStatement.class, ShadowSQLiteProgram.class, ShadowSQLiteCloseable.class })
@LargeTest
public class MapTileFilesystemProviderTest {

    MapTileFilesystemProvider provider;
    MockWebServer             tileServer;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        ShadowLog.setupLogging();
        provider = new MapTileFilesystemProvider(ApplicationProvider.getApplicationContext(), new File("."), 1000000);
        tileServer = MockTileServer.setupTileServer(ApplicationProvider.getApplicationContext(), "ersatz_background.mbt", true);
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
            tileServer.close();
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
            provider.saveFile(new MapTile("test", 10, 511, 340), MapTileProviderDataBaseTest.getTestTile());
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

    /**
     * Load a tile successfully
     */
    @Test
    public void loadMapTileAsyncSuccessTest() {
        // this should load from the server
        final CountDownLatch signal1 = new CountDownLatch(1);
        MapTile mockedTile = new MapTile(MockTileServer.MOCK_TILE_SOURCE, 19, 274335, 183513);
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
        assertEquals(1, tileServer.getRequestCount());

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
        assertEquals(1, tileServer.getRequestCount());
    }

    abstract class CallbackWithResult implements MapTileProviderCallback {
        int result;
    }

    /**
     * Try to load a tile that doesn't exist
     */
    @Test
    public void loadMapTileAsyncFailTest() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        MapTile mockedTile = new MapTile(MockTileServer.MOCK_TILE_SOURCE, 14, 11541, 3864);
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
        assertEquals(1, tileServer.getRequestCount());
    }

    @Test
    public void customHeaderTest() {
        TileLayerSource layer = TileLayerSource.get(ApplicationProvider.getApplicationContext(), MockTileServer.MOCK_TILE_SOURCE, false);
        assertNotNull(layer);
        layer.setHeaders(Util.wrapInList(new TileLayerSource.Header(UserAgentInterceptor.USER_AGENT_HEADER, "Mozilla/5.0 (JOSM)")));
        // this should load from the server
        final CountDownLatch signal1 = new CountDownLatch(1);
        MapTile mockedTile = new MapTile(MockTileServer.MOCK_TILE_SOURCE, 19, 274335, 183514); // not this needs to be a
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
            assertNotNull(request);
            assertEquals("Mozilla/5.0 (JOSM)", request.getHeader(UserAgentInterceptor.USER_AGENT_HEADER));
        } catch (InterruptedException e1) {
            fail("no tileserver request found " + e1.getMessage());
        }
        assertEquals(1, tileServer.getRequestCount());
    }
}
