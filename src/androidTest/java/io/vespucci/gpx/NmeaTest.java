package io.vespucci.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.location.Location;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.MockTileServer;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.gpx.GpxTest;
import io.vespucci.gpx.TrackPoint;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerDatabase;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NmeaTest {

    public static final int TIMEOUT = 240;

    Main            main            = null;
    UiDevice        device          = null;
    Instrumentation instrumentation = null;
    MockWebServer   tileServer      = null;
    Preferences     prefs           = null;

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
        device = UiDevice.getInstance(instrumentation);
        // this sets the mock location permission
        instrumentation.getUiAutomation().executeShellCommand("appops set de.blau.android 58 allow");

        main = mActivityRule.getActivity();

        TestUtils.grantPermissons(device);

        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);
        prefs = new Preferences(main);
        prefs.setGpsSource(R.string.gps_source_tcpserver);
        Logic logic = App.getLogic();
        logic.setPrefs(prefs);
        Map map = main.getMap();
        map.setPrefs(main, prefs);

        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.zoomToNullIsland(logic, map);
        TestUtils.stopEasyEdit(main);
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
            System.out.println("main is null"); // NOSONAR
        }
        try {
            tileServer.close();
        } catch (IOException | NullPointerException e) {
            // ignore
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Replay a pre-recorded track with NMEA data
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void recordNmea() {
        assertNotNull(main);

        // wait for the trackerservice to start
        // unluckily there doesn't seem to be any elegant way to do this
        int retries = 0;
        while (main.getTracker() == null && retries < 60) {
            TestUtils.sleep();
            retries++;
            if (retries >= 60) {
                fail("Tracker service didn't start");
            }
        }
        // set min distance to 1m
        prefs.setGpsDistance(0);

        main.invalidateOptionsMenu();

        TestUtils.zoomToLevel(device, main, 19);
        GpxTest.clickGpsButton(device);

        UiObject2 startItem = TestUtils.findObjectWithText(device, false, main.getString(R.string.menu_gps_start), 1000, false);
        assertNotNull(startItem);
        assertTrue(GpxUploadTest.isEnabled(startItem));
        startItem.click();
        GpxTest.clickAwayTip(device, main);
        // wait for the tracking to actually start
        retries = 0;
        while (!main.getTracker().isTracking() && retries < 60) {
            TestUtils.sleep();
            retries++;
            if (retries >= 60) {
                fail("Tracker service didn't start tracking");
            }
        }

        final CountDownLatch signal = new CountDownLatch(1);
        main.getTracker().getTrack().reset(); // clear out anything saved
        SendNMEA.send(main, "2020_03_27.nmea", new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_pause), true, false));
        List<TrackPoint> recordedTrack = main.getTracker().getTrack().getTrackPoints();
        assertEquals(216, recordedTrack.size());
        Location lastLocation = main.getTracker().getLastLocation();
        assertEquals(47.39804275, lastLocation.getLatitude(), 0.000001);
        assertEquals(8.376432616666667, lastLocation.getLongitude(), 0.000001);
    }
}
