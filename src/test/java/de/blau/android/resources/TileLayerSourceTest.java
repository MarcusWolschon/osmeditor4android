package de.blau.android.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.resources.TileLayerSource.Provider.CoverageArea;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class TileLayerSourceTest {
    TileLayerDatabase db;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        ApplicationProvider.getApplicationContext().deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        db = new TileLayerDatabase(ApplicationProvider.getApplicationContext());
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        db.close();
        ApplicationProvider.getApplicationContext().deleteDatabase(TileLayerDatabase.DATABASE_NAME);
    }

    /**
     * Polygon vs MultiPolygon test
     */
    @Test
    public void fakePolygonVsMultiPolygon() {
        try {
            TileLayerDatabase.addSource(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
            TileLayerSource.parseImageryFile(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI,
                    getClass().getResourceAsStream("/imagery_test_with_meta.geojson"), true);
            TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db.getReadableDatabase(), true);
            String[] ids = TileLayerSource.getIds(null, false, null, null);
            assertEquals(5, ids.length);
            TileLayerSource b = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "B", false);
            assertNotNull(b);
            List<CoverageArea> areas = b.getCoverage();
            assertEquals(2, areas.size());
            TileLayerSource bNoDate = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "B NO DATE", false);
            assertNotNull(bNoDate);
            areas = bNoDate.getCoverage();
            assertEquals(2, areas.size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Proper polygon test
     */
    @Test
    public void polygon() {
        try {
            TileLayerDatabase.addSource(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
            TileLayerSource.parseImageryFile(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI,
                    getClass().getResourceAsStream("/imagery_test_1_1.geojson"), true);
            TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db.getReadableDatabase(), true);
            String[] ids = TileLayerSource.getIds(null, false, null, null);
            assertEquals(1, ids.length);
            TileLayerSource b = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "B", false);
            assertNotNull(b);
            List<CoverageArea> areas = b.getCoverage();
            assertEquals(1, areas.size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
