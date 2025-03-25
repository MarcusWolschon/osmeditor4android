package io.vespucci.imageryoffset;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.MockTileServer;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.imageryoffset.ImageryOffset;
import io.vespucci.imageryoffset.ImageryOffsetDatabase;
import io.vespucci.imageryoffset.ImageryOffsetUtils;
import io.vespucci.imageryoffset.Offset;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.resources.TileLayerSource;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OffsetTest {

    Main            main            = null;
    UiDevice        device          = null;
    Instrumentation instrumentation = null;
    Preferences     prefs           = null;
    MockWebServer   tileServer      = null;

    /**
     * Manual start of activity so that we can set up the monitor for main
     */
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();

        main = mActivityRule.getActivity();

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        TestUtils.grantPermissons(device);

        LayerUtils.removeImageryLayers(main);
        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);
        prefs = new Preferences(main);
        main.getMap().setPrefs(main, prefs);
        TestUtils.resetOffsets(main.getMap());
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        if (main != null) {
            TestUtils.resetOffsets(main.getMap());
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }
        try {
            tileServer.close();
        } catch (Exception e) {
            // ignore
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Set an offset and apply it
     */
    @Test
    public void saveAndApplyOffset() {
        final CountDownLatch signal = new CountDownLatch(1);
        instrumentation.waitForIdle(() -> (new SignalHandler(signal)).onSuccess());
        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        final Map map = main.getMap();
        ImageryOffset offset = new ImageryOffset();
        TileLayerSource osmts = map.getBackgroundLayer().getTileLayerConfiguration();
        offset.imageryId = osmts.getImageryOffsetId();
        offset.setLon(8.21598D);
        offset.setLat(47.40804D);
        offset.setImageryLon(8.21608D);
        offset.setImageryLat(47.40814D);
        offset.setMinZoom(16);
        offset.setMaxZoom(19);
        try (ImageryOffsetDatabase db = new ImageryOffsetDatabase(main)) {
            ImageryOffsetDatabase.addOffset(db.getWritableDatabase(), offset);
        }
        App.getLogic().setZoom(map, 19);
        map.getViewBox().moveTo(map, (int) (offset.getLon() * 1E7D), (int) ((offset.getLat() - 0.0001) * 1E7D));
        map.invalidate();
        TestUtils.sleep(10000);
        map.setPrefs(main, prefs);
        Offset[] tlo = osmts.getOffsets();
        Assert.assertEquals(20, tlo.length);
        List<ImageryOffset> appliedOffsets = ImageryOffsetUtils.offsets2ImageryOffset(osmts, map.getViewBox(), null);
        Assert.assertEquals(1, appliedOffsets.size());
        Assert.assertEquals(16, appliedOffsets.get(0).getMinZoom());
        Assert.assertEquals(19, appliedOffsets.get(0).getMaxZoom());
        double deltaLon = offset.getLon() - offset.getImageryLon();
        double deltaLat = offset.getLat() - offset.getImageryLat();
        ImageryOffset newOffset = appliedOffsets.get(0);
        double newDeltaLon = newOffset.getLon() - newOffset.getImageryLon();
        double newDeltaLat = newOffset.getLat() - newOffset.getImageryLat();
        Assert.assertEquals(deltaLon, newDeltaLon, 0.000001);
        Assert.assertEquals(deltaLat, newDeltaLat, 0.000001);
    }
}
