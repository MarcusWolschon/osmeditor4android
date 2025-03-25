package io.vespucci.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.Main;
import io.vespucci.MockTileServer;
import io.vespucci.ShadowWorkManager;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.services.util.MapTile;
import io.vespucci.services.util.MapTileTester;
import io.vespucci.services.util.ShadowSQLiteCloseable;
import io.vespucci.services.util.ShadowSQLiteProgram;
import io.vespucci.services.util.ShadowSQLiteStatement;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class, ShadowSQLiteStatement.class, ShadowSQLiteProgram.class, ShadowSQLiteCloseable.class }, sdk=33)
@LargeTest
public class MapTileTesterTest {

    Main          main = null;
    MockWebServer tileServer;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        ShadowLog.setupLogging();
        main = Robolectric.buildActivity(Main.class).create().resume().get();
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
        try {
            tileServer.close();
        } catch (IOException e) {
            // ignore
        }
        if (main != null) {
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }
    }

    /**
     * Get a tile that exists
     */
    @Test
    public void testOK() {
        MapTile mapTile = new MapTile(MockTileServer.MOCK_TILE_SOURCE, 19, 274335, 183513);

        MapTileTester tester = new MapTileTester(main, mapTile);
        assertTrue(tester.run());
        assertEquals(8650, tester.getTile().length);
    }

    /**
     * Get a tile that doesn't exist
     */
    @Test
    public void testMarkinvalid() {
        MapTile mapTile = new MapTile(MockTileServer.MOCK_TILE_SOURCE, 21, 34270, 22946);

        MapTileTester tester = new MapTileTester(main, mapTile);
        assertFalse(tester.run());
        assertTrue(tester.getOutput().contains("invalid"));
    }
}