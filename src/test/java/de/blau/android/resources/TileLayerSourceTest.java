package de.blau.android.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import de.blau.android.net.UserAgentInterceptor;
import de.blau.android.resources.TileLayerSource.Header;
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
     * Custom header
     */
    @Test
    public void customHeader() {
        try {
            TileLayerDatabase.addSource(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
            TileLayerSource.parseImageryFile(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI,
                    getClass().getResourceAsStream("/imagery_test_1_1.geojson"), true);
            TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db.getReadableDatabase(), true);
            String[] ids = TileLayerSource.getIds(null, false, null, null);
            assertEquals(1, ids.length);
            TileLayerSource b = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "B", false);
            assertNotNull(b);
            List<Header> headers = b.getHeaders();
            assertNotNull(headers);
            assertEquals(1, headers.size());
            assertEquals(UserAgentInterceptor.USER_AGENT_HEADER, headers.get(0).getName());
            assertEquals("Mozilla/5.0 (JOSM)", headers.get(0).getValue());
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
                    "https://ows.geo.tg.ch/geofy_access_proxy/basisplanf?LAYERS=Basisplan_farbig&STYLES=&FORMAT=image/png&CRS=EPSG:4326&WIDTH=256&HEIGHT=256&BBOX=47.554286701279565,8.8934326171875,47.55799385903775,8.89892578125&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap",
                    url);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test wms url creation when config doesn't contain projection
     */
    @Test
    public void projFromWmsUrl() {
        TileLayerDatabase.addSource(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
        final String id = "kt_tg_av_test".toUpperCase();
        TileLayerSource.addOrUpdateCustomLayer(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), id, null, -1L, -1L, "Test", null, null,
                null, null, 1, 20, 256, false,
                "https://ows.geo.tg.ch/geofy_access_proxy/basisplanf?LAYERS=Basisplan_farbig&STYLES=&FORMAT=image/png&CRS=EPSG:4326&WIDTH={width}&HEIGHT={height}&BBOX={bbox}&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap");
        TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db.getReadableDatabase(), true);
        String[] ids = TileLayerSource.getIds(null, false, null, null);
        assertEquals(1, ids.length);
        MapTile tile = new MapTile("", 16, 34387, 22901);

        TileLayerSource thurgau = TileLayerSource.get(ApplicationProvider.getApplicationContext(), id, false);
        assertNotNull(thurgau);
        String url = thurgau.getTileURLString(tile);
        assertEquals(
                "https://ows.geo.tg.ch/geofy_access_proxy/basisplanf?LAYERS=Basisplan_farbig&STYLES=&FORMAT=image/png&CRS=EPSG:4326&WIDTH=256&HEIGHT=256&BBOX=47.554286701279565,8.8934326171875,47.55799385903775,8.89892578125&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap",
                url);
        assertEquals(TileLayerSource.EPSG_4326, thurgau.getProj());
    }

    /**
     * Test that setting min and max zoom works correctly if offsets have been set
     */
    @Test
    public void changeZoomLevels() {
        try {
            TileLayerDatabase.addSource(db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI);
            TileLayerSource.parseImageryFile(ApplicationProvider.getApplicationContext(), db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI,
                    getClass().getResourceAsStream("/imagery_test.geojson"), true);
            TileLayerSource.getListsLocked(ApplicationProvider.getApplicationContext(), db.getReadableDatabase(), true);
            String[] ids = TileLayerSource.getIds(null, false, null, null);
            assertEquals(5, ids.length);
            TileLayerSource a = TileLayerSource.get(ApplicationProvider.getApplicationContext(), "A", false);
            assertNotNull(a);
            a.setOffset(0, 19, 0.1D, 0.1D);
            assertEquals(20, a.getOffsets().length);
            a.setMinZoom(1);
            assertEquals(19, a.getOffsets().length);
            a.setMinZoom(0);
            assertEquals(20, a.getOffsets().length);
            assertNull(a.getOffset(0));
            a.setMaxZoom(18);
            assertEquals(19, a.getOffsets().length);
            a.setMaxZoom(19);
            assertEquals(20, a.getOffsets().length);
            assertNull(a.getOffset(19));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
