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
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SaveLoadStateTest {

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
        prefs.setBackGroundLayer(TileLayerSource.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerSource.LAYER_NOOVERLAY);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
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
        TestUtils.stopEasyEdit(main);
        map.getViewBox().fitToBoundingBox(map, map.getDataLayer().getExtent());
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        map.invalidate();
        TestUtils.unlock(device);
        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.zoomToLevel(device, main, 18);
    }

    /**
     * Select, show info dialog, show position dialog, delete, undelete
     */
    @Test
    public void saveThenLoad() {
        StorageDelegator delegator = App.getDelegator();
        Storage storage = delegator.getCurrentStorage();
        int nodeCount = storage.getNodeCount();
        int wayCount = storage.getWayCount();
        int relCount = storage.getRelationCount();
        logic.save(main);
        delegator.reset(false);
        storage = delegator.getCurrentStorage();
        Assert.assertEquals(0, storage.getNodeCount());
        Assert.assertEquals(0, storage.getWayCount());
        Assert.assertEquals(0, storage.getRelationCount());
        final CountDownLatch signal = new CountDownLatch(1);
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logic.loadStateFromFile(main, new SignalHandler(signal));
            }
        });

        try {
            signal.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        storage = delegator.getCurrentStorage();
        Assert.assertEquals(nodeCount, storage.getNodeCount());
        Assert.assertEquals(wayCount, storage.getWayCount());
        Assert.assertEquals(relCount, storage.getRelationCount());
    }
}
