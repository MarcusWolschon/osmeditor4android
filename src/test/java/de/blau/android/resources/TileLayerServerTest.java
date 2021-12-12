package de.blau.android.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import android.view.View;
import androidx.test.filters.LargeTest;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.SignalHandler;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.ExecutorTask;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class TileLayerServerTest {

    Main main = null;
    View v    = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        main = Robolectric.buildActivity(Main.class).create().resume().get();

        try (TileLayerDatabase db = new TileLayerDatabase(main)) {
            TileLayerSource.createOrUpdateFromAssetsSource(main, db.getWritableDatabase(), true, false);
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
     * Get a tile url for bing and then for the "standard" style layer
     */
    @Test
    public void buildurl() {
        Map map = main.getMap();
        MapTile mapTile = new MapTile("", 20, 1111, 2222);
        Preferences prefs = new Preferences(main);

        LayerUtils.removeImageryLayers(main);
        de.blau.android.layer.Util.addLayer(main, LayerType.IMAGERY, TileLayerSource.LAYER_BING);
        main.getMap().setPrefs(main, prefs);

        final TileLayerSource t = map.getBackgroundLayer().getTileLayerConfiguration();
        Assert.assertNotNull(t);
        if (!t.isMetadataLoaded()) {
            final CountDownLatch signal = new CountDownLatch(1);
            final SignalHandler handler = new SignalHandler(signal);
            new ExecutorTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void param) {
                    for (int i = 0; i < 10; i++) {
                        try {
                            Thread.sleep(1000); // NOSONAR
                        } catch (InterruptedException e) {
                        }
                        if (t.isMetadataLoaded()) {
                            System.out.println("metadata is loaded");
                            break;
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    handler.onSuccess();
                }
            }.execute();
            try {
                signal.await(11, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
        }

        Assert.assertTrue(t.isMetadataLoaded());
        String s = t.getTileURLString(mapTile); // note this would fail if the metainfo cannot be retrieved

        System.out.println("Parameters replaced " + s);
        System.out.println("Quadkey " + t.quadTree(mapTile));
        Assert.assertTrue(s.contains(t.quadTree(mapTile)));

        LayerUtils.removeImageryLayers(main);
        de.blau.android.layer.Util.addLayer(main, LayerType.IMAGERY, TileLayerSource.LAYER_MAPNIK);
        main.getMap().setPrefs(main, prefs);

        TileLayerSource t2 = map.getBackgroundLayer().getTileLayerConfiguration();
        System.out.println(t2.toString());

        s = t2.getTileURLString(mapTile);

        System.out.println("Parameters replaced " + s);

        Assert.assertTrue(s.contains("1111"));
        Assert.assertTrue(s.contains("2222"));
        Assert.assertTrue(s.contains("20"));
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
}