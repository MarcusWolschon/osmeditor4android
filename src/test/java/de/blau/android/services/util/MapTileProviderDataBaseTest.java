package de.blau.android.services.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.exception.InvalidTileException;
import de.blau.android.services.exceptions.EmptyCacheException;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowSQLiteStatement.class, ShadowSQLiteProgram.class, ShadowSQLiteCloseable.class }, sdk=33)
@LargeTest
public class MapTileProviderDataBaseTest {

    MapTileProviderDataBase db;
    MapTile                 tile;
    byte[]                  tileBytes;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        db = new MapTileProviderDataBase(ApplicationProvider.getApplicationContext());
        tile = new MapTile("test", 10, 511, 340);
        tileBytes = getTestTile();
    }

    /**
     * Get test tile from resources
     * 
     * @return a test tile
     */
    static byte[] getTestTile() {
        try (InputStream is = MapTileProviderDataBaseTest.class.getResourceAsStream("/340.png")) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException ioex) {
            fail(ioex.getMessage());
        }
        return null;
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        db.close();
        MapTileProviderDataBase.delete(ApplicationProvider.getApplicationContext());
    }

    /**
     * Test is tile is (not) present
     */
    @Test
    public void hasTileTest() {
        assertFalse(db.hasTile(tile));
    }

    /**
     * Add a tile and test if it is present
     */
    @Test
    public void addTileTest() {
        try {
            assertEquals(tileBytes.length, db.addTile(tile, tileBytes));
            assertTrue(db.hasTile(tile));
        } catch (IOException ioex) {
            fail(ioex.getMessage());
        }
    }

    /**
     * Check the size of the DB
     */
    @Test
    public void sizeTest() {
        assertEquals(0, db.getCurrentFSCacheByteSize());
        addTileTest();
        assertEquals(tileBytes.length, db.getCurrentFSCacheByteSize());
    }

    /**
     * Remove the oldest tiles
     */
    @Test
    public void deleteOldest() {
        addTileTest();
        assertEquals(tileBytes.length, db.deleteOldest(tileBytes.length));
    }

    /**
     * Flush, that is completely empty the DB for a specific provider
     */
    @Test
    public void flushCache() {
        addTileTest();
        try {
            db.flushCache("wrong");
            fail("flush failed");
        } catch (EmptyCacheException e) {
        }
        assertEquals(tileBytes.length, db.getCurrentFSCacheByteSize());
        try {
            db.flushCache("test");
        } catch (EmptyCacheException e) {
            fail("flush failed");
        }
        assertEquals(0, db.getCurrentFSCacheByteSize());
    }

    /**
     * Retrieve a tile
     */
    @Test
    public void getTileTest() {
        try {
            addTileTest();
            assertArrayEquals(tileBytes, db.getTile(tile));
        } catch (InvalidTileException ite) {
            fail(ite.getMessage());
        } catch (IOException ioex) {
            fail(ioex.getMessage());
        }
    }

    /**
     * Mark a tile as invalid
     */
    @Test
    public void markInvalid() {
        try {
            assertEquals(0, db.addTile(tile, null));
        } catch (IOException ioex) {
            fail(ioex.getMessage());
        }
        assertTrue(db.isInvalid(tile));
    }

    /**
     * Update an invalid tile to an actual one
     */
    @Test
    public void addTileAfterInvalidTest() {
        try {
            assertEquals(0, db.addTile(tile, null));
            assertEquals(tileBytes.length, db.addTile(tile, tileBytes));
        } catch (IOException ioex) {
            fail(ioex.getMessage());
        }
    }

    /**
     * Check if the database (doesn't) exist
     */
    @Test
    public void existsTest() {
        assertFalse(MapTileProviderDataBase.exists(new File("/")));
    }
}
