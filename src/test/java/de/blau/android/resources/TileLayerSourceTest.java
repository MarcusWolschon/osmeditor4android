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
import de.blau.android.services.util.MapTile;

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

    /**
     * Test for tile size
     */
    @Test
    public void tileSize() {
        try {
            TileLayerDatabase.addSource(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
            TileLayerSource.parseImageryFile(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI,
                    getClass().getResourceAsStream("/imagery_test.geojson"), true);
            TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db.getReadableDatabase(), true);
            String[] ids = TileLayerSource.getIds(null, false, null, null);
            assertEquals(5, ids.length);
            TileLayerSource b = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "B", false);
            assertNotNull(b);
            assertEquals(256, b.getTileWidth());
            assertEquals(256, b.getTileHeight());
            TileLayerSource c = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "C", false);
            assertNotNull(c);
            assertEquals(512, c.getTileWidth());
            assertEquals(512, c.getTileHeight());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test wms url creation
     */
    @Test
    public void wmsUrl() {
        try {
            TileLayerDatabase.addSource(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
            TileLayerSource.parseImageryFile(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI,
                    getClass().getResourceAsStream("/wms.geojson"), true);
            TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db.getReadableDatabase(), true);
            String[] ids = TileLayerSource.getIds(null, false, null, null);
            assertEquals(2, ids.length);
            TileLayerSource swisstopo = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "swisstopo_swissimage".toUpperCase(), false);
            assertNotNull(swisstopo);
            MapTile tile = new MapTile("", 16, 34387, 22901);
            String url = swisstopo.getTileURLString(tile);
            assertEquals(
                    "https://wms.geo.admin.ch?LAYERS=ch.swisstopo.swissimage&STYLES=default&FORMAT=image/jpeg&CRS=EPSG:3857&WIDTH=512&HEIGHT=512&BBOX=990012.3903496042,6033021.768492393,990623.8865758851,6033633.264718674&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap",
                    url);
            TileLayerSource thurgau = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "kt_tg_av".toUpperCase(), false);
            assertNotNull(thurgau);
            url = thurgau.getTileURLString(tile);
            assertEquals(
                    " https://ows.geo.tg.ch/geofy_access_proxy/basisplanf?LAYERS=Basisplan_farbig&STYLES=&FORMAT=image/png&CRS=EPSG:4326&WIDTH=256&HEIGHT=256&BBOX=47.554286701279565,8.8934326171875,47.55799385903775,8.89892578125&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap",
                    url);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
