package io.vespucci;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.exception.OsmException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.GeoMath;

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

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(App.getLogic(), map);
    }

    /**
     * Drag the screen a bit
     */
    @Test
    public void drag() {
        map.getDataLayer().setVisible(true);
        try {
            BoundingBox bbox = GeoMath.createBoundingBoxForCoordinates(47.390339D, 8.38782D, 50D);
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
        TestUtils.drag(device, map, 8.38782, 47.390339, 8.388, 47.391, false, 100);
        BoundingBox after = new BoundingBox(map.getViewBox());

        double diffLon = 8.38782 - 8.388;
        double diffLat = 47.390339 - 47.391;

        Assert.assertEquals(diffLon, (after.getLeft() - before.getLeft()) / 1E7D, 0.00002);
        Assert.assertEquals(diffLon, (after.getRight() - before.getRight()) / 1E7D, 0.00002);
        Assert.assertEquals(diffLat, (after.getBottom() - before.getBottom()) / 1E7D, 0.00002);
        Assert.assertEquals(diffLat, (after.getTop() - before.getTop()) / 1E7D, 0.00002);
    }
}
