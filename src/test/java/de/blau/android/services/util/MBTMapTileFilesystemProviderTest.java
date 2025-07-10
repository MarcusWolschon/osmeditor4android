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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.JavaResources;
import de.blau.android.MockTileServer;
import de.blau.android.layer.tiles.util.MapTileProviderCallback;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.util.FileUtil;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowSQLiteStatement.class, ShadowSQLiteProgram.class, ShadowSQLiteCloseable.class, ShadowSQLiteQuery.class }, sdk=33)
@LargeTest
public class MBTMapTileFilesystemProviderTest {

    MapTileFilesystemProvider provider;
    MockWebServer             tileServer;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        ShadowLog.setupLogging();
        provider = new MapTileFilesystemProvider(ApplicationProvider.getApplicationContext(), new File("."), 1000000);
        try {
            JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "ersatz_background.mbt", null, "/");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            File mbtFile = new File(FileUtil.getPublicDirectory(), "ersatz_background.mbt");
            TileLayerSource.addOrUpdateCustomLayer(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), MockTileServer.MOCK_TILE_SOURCE, null,
                    -1, -1, "Vespucci Test", new Provider(), Category.other, null, null, 0, 19, TileLayerSource.DEFAULT_TILE_SIZE, false,
                    "file://" + (System.getProperty("os.name").toLowerCase().contains("windows") ? "\\" : "") + mbtFile.getAbsolutePath());
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
    }

    abstract class CallbackWithResult implements MapTileProviderCallback {
        /**
         * support returning a result for testing
         */
        int result;
    }

    /**
     * Request a file that doesn't exist
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
        assertEquals(MapAsyncTileProvider.DOESNOTEXIST, callback.result);
    }
}
