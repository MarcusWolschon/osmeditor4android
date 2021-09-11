package de.blau.android.imageryoffset;

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
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.MockTileServer;
import de.blau.android.SignalHandler;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OffsetTest {

    Splash          splash          = null;
    Main            main            = null;
    UiDevice        device          = null;
    ActivityMonitor monitor         = null;
    Instrumentation instrumentation = null;
    Preferences     prefs           = null;
    MockWebServer   tileServer      = null;

    /**
     * Manual start of activity so that we can set up the monitor for main
     */
    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class, false, false);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        splash = mActivityRule.launchActivity(intent);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000); // wait for main
        Assert.assertNotNull(main);

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        TestUtils.grantPermissons(device);
        
        LayerUtils.removeImageryLayers(main);
        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);
        prefs = new Preferences(main);
        main.getMap().setPrefs(main, prefs);
        TestUtils.resetOffsets(main.getMap());
        TestUtils.dismissStartUpDialogs(device, main);
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
        instrumentation.removeMonitor(monitor);
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
        ImageryOffsetDatabase db = new ImageryOffsetDatabase(main);
        ImageryOffsetDatabase.addOffset(db.getWritableDatabase(), offset);
        db.close();
        App.getLogic().setZoom(map, 19);
        map.getViewBox().moveTo(map, (int) (offset.getLon() * 1E7D), (int) (offset.getLat() * 1E7D));
        map.invalidate();
        TestUtils.sleep();
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
