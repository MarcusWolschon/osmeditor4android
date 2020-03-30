package de.blau.android.imageryoffset;

import java.io.IOException;
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
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.SignalHandler;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;
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
     * Pre-teset setup
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

        prefs = new Preferences(main);
        tileServer = TestUtils.setupTileServer(main, prefs, "ersatz_background.mbt");

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
        } catch (IOException e) {
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
        instrumentation.waitForIdle(new Runnable() {
            @Override
            public void run() {
                (new SignalHandler(signal)).onSuccess();
            }
        });
        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        final Map map = main.getMap();
        ImageryOffset offset = new ImageryOffset();
        TileLayerServer osmts = map.getBackgroundLayer().getTileLayerConfiguration();
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
        App.getLogic().setZoom(map, 18);
        map.getViewBox().moveTo(map, (int) (offset.getLon() * 1E7D), (int) (offset.getLat() * 1E7D));
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                map.invalidate();
                map.setPrefs(main, prefs);
                (new SignalHandler(signal2)).onSuccess();
            }
        });
        try {
            signal2.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Offset[] tlo = osmts.getOffsets();
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
