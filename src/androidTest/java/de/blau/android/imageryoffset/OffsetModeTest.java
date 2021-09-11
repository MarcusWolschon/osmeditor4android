package de.blau.android.imageryoffset;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.MockTileServer;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.GeoMath;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OffsetModeTest {

    public static final int TIMEOUT         = 90;
    Splash                  splash          = null;
    Main                    main            = null;
    UiDevice                device          = null;
    ActivityMonitor         monitor         = null;
    Instrumentation         instrumentation = null;
    MockWebServerPlus       mockServer      = null;
    MockWebServer           tileServer      = null;
    Preferences             prefs           = null;
    Map                     map             = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class, false, false);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();

        device = UiDevice.getInstance(instrumentation);
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        splash = mActivityRule.launchActivity(intent);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000); // wait for main
        Assert.assertNotNull(main);

        TestUtils.grantPermissons(device);

        prefs = new Preferences(main);

        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/");
        prefs.setOffsetServer(mockBaseUrl.toString());

        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);

        map = main.getMap();
        map.setPrefs(main, prefs);
        App.getLogic().setPrefs(prefs);
        TestUtils.resetOffsets(main.getMap());
        TestUtils.dismissStartUpDialogs(device, main);
        main.invalidateOptionsMenu(); // to be sure that the menu entry is actually shown
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        if (main != null) {
            TestUtils.zoomToLevel(device, main, 18);
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
     * Start offset mode and drag the screen
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createOffset() {
        startMode();
        Assert.assertTrue(TestUtils.clickText(device, false, "Align background", true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "Align background"));
        TileLayerSource tileLayerConfiguration = map.getBackgroundLayer().getTileLayerConfiguration();
        tileLayerConfiguration.setOffset(0, 0);
        TestUtils.zoomToLevel(device, main, tileLayerConfiguration.getMaxZoom());
        int zoomLevel = map.getZoomLevel();
        Offset offset = tileLayerConfiguration.getOffset(zoomLevel);
        Assert.assertEquals(0D, offset.getDeltaLat(), 0.1E-4);
        Assert.assertEquals(0D, offset.getDeltaLon(), 0.1E-4);
        TestUtils.drag(device, map, 8.38782, 47.390339, 8.388, 47.391, true, 50);
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, "Save to database", true, false);
        // 74.22 m
        TestUtils.clickText(device, false, "Cancel", true, false);
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, "Apply", true, false);
        TestUtils.clickUp(device);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        zoomLevel = map.getZoomLevel();
        offset = tileLayerConfiguration.getOffset(zoomLevel);
        Assert.assertNotNull(offset);
        Assert.assertEquals(6.462E-4, offset.getDeltaLat(), 0.1E-4);
        Assert.assertEquals(1.773E-4, offset.getDeltaLon(), 0.1E-4);
    }

    /**
     * Zoom in and start the alignment mode
     */
    void startMode() {
        TestUtils.zoomToLevel(device, main, 18);
        try {
            BoundingBox bbox = GeoMath.createBoundingBoxForCoordinates(47.390339D, 8.38782D, 50D, true);
            App.getLogic().getViewBox().setBorders(map, bbox);
            map.setViewBox(App.getLogic().getViewBox());
            map.invalidate();
            try {
                Thread.sleep(5000); // NOSONAR
            } catch (InterruptedException e) {
            }
            main.invalidateOptionsMenu();
        } catch (OsmException e) {
            Assert.fail(e.getMessage());
        }

        if (!TestUtils.clickMenuButton(device, "Tools", false, true)) {
            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, "Tools", true, false);
        }
        if (!TestUtils.findText(device, false, "Align background")) {
            // retry
            device.pressBack();
            device.waitForWindowUpdate(null, 2000);
            if (!TestUtils.clickMenuButton(device, "Tools", false, true)) {
                TestUtils.clickOverflowButton(device);
                TestUtils.clickText(device, false, "Tools", true, false);
            }
        }
    }

    /**
     * Start offset mode and download a offset
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void downloadOffset() {
        mockServer.enqueue("imagery_offset");
        startMode();
        Assert.assertTrue(TestUtils.clickText(device, false, "Align background", true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "Align background"));
        TileLayerSource tileLayerConfiguration = map.getBackgroundLayer().getTileLayerConfiguration();
        TestUtils.zoomToLevel(device, main, tileLayerConfiguration.getMaxZoom());
        int zoomLevel = map.getZoomLevel();
        TestUtils.clickMenuButton(device, "From database", false, true);
        TestUtils.clickText(device, false, "Apply", true, false);
        TestUtils.clickUp(device);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        zoomLevel = map.getZoomLevel();
        Offset offset = tileLayerConfiguration.getOffset(zoomLevel);
        Assert.assertNotNull(offset);
        Assert.assertEquals(8.7E-6, offset.getDeltaLat(), 0.1E-6);
        Assert.assertEquals(-1.056E-5, offset.getDeltaLon(), 0.01E-5);
    }
}
