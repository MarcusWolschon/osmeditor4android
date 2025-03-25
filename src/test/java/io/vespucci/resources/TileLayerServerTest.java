package io.vespucci.resources;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.ShadowWorkManager;
import io.vespucci.contract.Files;
import io.vespucci.layer.LayerType;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.KeyDatabaseHelper;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.services.util.MapTile;
import okhttp3.mockwebserver.MockResponse;
import okio.Buffer;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class TileLayerServerTest {

    Main    main = null;
    View    v    = null;
    Context ctx  = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        ctx = ApplicationProvider.getApplicationContext();
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(ctx); InputStream is = loader.getResourceAsStream(Files.FILE_NAME_KEYS_V2)) {
            keyDatabase.keysFromStream(null, is);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try (TileLayerDatabase db = new TileLayerDatabase(ctx)) {
            TileLayerSource.createOrUpdateFromAssetsSource(ctx, db.getWritableDatabase(), true, false);
        }
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        if (main != null) {
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }
    }

    /**
     * Get a tile url for the "standard" style layer
     */
    @Test
    public void buildurl() {
        Map map = main.getMap();
        MapTile mapTile = new MapTile("", 20, 1111, 2222);
        Preferences prefs = new Preferences(main);

        LayerUtils.removeImageryLayers(main);
        io.vespucci.layer.Util.addLayer(main, LayerType.IMAGERY, TileLayerSource.LAYER_MAPNIK);
        main.getMap().setPrefs(main, prefs);

        TileLayerSource t2 = map.getBackgroundLayer().getTileLayerConfiguration();
        System.out.println(t2.toString());

        String tileUrl = t2.getTileURLString(mapTile);

        System.out.println("Parameters replaced " + tileUrl);

        Assert.assertTrue(tileUrl.contains("1111"));
        Assert.assertTrue(tileUrl.contains("2222"));
        Assert.assertTrue(tileUrl.contains("20"));
    }

    /**
     * Test that imagery is sorted as expected
     */
    @Test
    public void sort() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("imagery_test.geojson");
        try (TileLayerDatabase db = new TileLayerDatabase(main)) {
            TileLayerSource.parseImageryFile(main, db.getWritableDatabase(), TileLayerDatabase.SOURCE_JOSM_IMAGERY, is, false);
            TileLayerSource.getListsLocked(main, db.getReadableDatabase(), true);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        List<String> names = Arrays.asList(TileLayerSource.getNames(null, false));

        int iA = names.indexOf("A imagery");
        Assert.assertNotEquals(-1, iA);
        int iAnoDate = names.indexOf("A no date imagery");
        Assert.assertNotEquals(-1, iAnoDate);
        int iB = names.indexOf("B imagery");
        Assert.assertNotEquals(-1, iB);
        int iBnoDate = names.indexOf("B no date imagery");
        Assert.assertNotEquals(-1, iBnoDate);
        int iC = names.indexOf("C imagery");
        Assert.assertNotEquals(-1, iC);

        Assert.assertTrue(iAnoDate < iBnoDate); // alphabetic

        TileLayerSource a = TileLayerSource.get(main, "A", false);
        TileLayerSource b = TileLayerSource.get(main, "B", false);
        Assert.assertTrue(a.getEndDate() < b.getEndDate());
        Assert.assertTrue(iA > iB); // date
        Assert.assertTrue(iA > iC && iB > iC); // preference
    }

    /**
     * Test if bing metadata retrieval works
     */
    @Test
    public void bingMeta() {
        MockWebServerPlus mockServer = new MockWebServerPlus();
        Buffer data = new Buffer();
        try {
            String url = mockServer.url("/?key={apikey}");
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // for whatever reason the fixture mechanism doesn't seem to work here
            InputStream is = loader.getResourceAsStream("fixtures/bing_metadata.xml");
            data.readFrom(is);
            mockServer.server().enqueue(new MockResponse().setResponseCode(200).setBody(data));
            TileLayerSource bing = new TileLayerSource(ctx, TileLayerSource.LAYER_BING, "Bing meta test", url, TileLayerSource.TYPE_BING, null, false, false,
                    null, null, null, null, null, 0, 20, 0, 256, 256, null, 0, 0, 0, null, null, null, false);
            Assert.assertTrue(bing.isMetadataLoaded());
            MapTile mapTile = new MapTile("", 20, 1111, 2222);
            String s = bing.getTileURLString(mapTile); // note this would fail if the metainfo cannot be retrieved
            Assert.assertTrue(s.contains(bing.quadTree(mapTile)));
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            data.close();
            try {
                mockServer.server().shutdown();
            } catch (IOException e) {
                // do nothing
            }
        }
    }
}