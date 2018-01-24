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
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.SignalHandler;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OffsetTest {

    Context         context         = null;
    Splash          splash          = null;
    Main            main            = null;
    UiDevice        device          = null;
    ActivityMonitor monitor         = null;
    Instrumentation instrumentation = null;
    Preferences     prefs           = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class);

    @Before
    public void setup() {
        splash = mActivityRule.getActivity();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 20000); // wait for main

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        prefs = new Preferences(context);
        // allow downloading tiles here
        prefs.setBackGroundLayer(TileLayerServer.LAYER_MAPNIK);
        main.getMap().setPrefs(main, prefs);
    }

    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);
        splash.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
    }

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
        TileLayerServer osmts = map.getBackgroundLayer().getRendererInfo();
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
