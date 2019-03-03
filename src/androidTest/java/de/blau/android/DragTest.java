package de.blau.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.GeoMath;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DragTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Map                  map     = null;
    Logic                logic   = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        final CountDownLatch signal1 = new CountDownLatch(1);
        logic = App.getLogic();
        logic.deselectAll();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test2.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
    }

    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToLevel(main, 18);
    }

    /**
     * Drag the screen a bit
     */
    @Test
    public void drag() {
        map.getDataLayer().setVisible(true);
        try {
            BoundingBox bbox = GeoMath.createBoundingBoxForCoordinates(47.390339D, 8.38782D, 50D, true);
            App.getLogic().getViewBox().setBorders(main.getMap(), bbox);
            main.getMap().invalidate();
            try {
                Thread.sleep(1000); // NOSONAR
            } catch (InterruptedException e) {
            }
        } catch (OsmException e) {
            Assert.fail(e.getMessage());
        }

        BoundingBox before = new BoundingBox(map.getViewBox());
        TestUtils.drag(map, 8.38782, 47.390339, 8.388, 47.391, false, 100);
        BoundingBox after = new BoundingBox(map.getViewBox());

        double diffLon = 8.38782 - 8.388;
        double diffLat = 47.390339 - 47.391;

        Assert.assertEquals(diffLon, (after.getLeft() - before.getLeft()) / 1E7D, 0.00001);
        Assert.assertEquals(diffLon, (after.getRight() - before.getRight()) / 1E7D, 0.00001);
        Assert.assertEquals(diffLat, (after.getBottom() - before.getBottom()) / 1E7D, 0.00001);
        Assert.assertEquals(diffLat, (after.getTop() - before.getTop()) / 1E7D, 0.00001);
    }
}
