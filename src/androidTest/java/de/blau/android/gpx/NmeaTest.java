package de.blau.android.gpx;

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
import android.location.Location;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NmeaTest {

    public static final int TIMEOUT = 240;

    Splash          splash          = null;
    Main            main            = null;
    UiDevice        device          = null;
    ActivityMonitor monitor         = null;
    Instrumentation instrumentation = null;
    MockWebServer   tileServer      = null;
    Preferences     prefs           = null;

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
        device = UiDevice.getInstance(instrumentation);
        // this sets the mock location permission
        instrumentation.getUiAutomation().executeShellCommand("appops set de.blau.android 58 allow");
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        splash = mActivityRule.launchActivity(intent);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 60000); // wait for main

        TestUtils.grantPermissons(device);

        prefs = new Preferences(main);
        tileServer = TestUtils.setupTileServer(main, prefs, "ersatz_background.mbt");
        prefs.setGpsSource(R.string.gps_source_tcpserver);
        App.getLogic().setPrefs(prefs);
        main.getMap().setPrefs(main, prefs);

        TestUtils.dismissStartUpDialogs(device, main);
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
        instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync();
    }

    /**
     * Replay a pre-recorded track with NMEA data
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void recordNmea() {
        Assert.assertNotNull(main);

        // wait for the trackerservice to start
        // unluckily there doesn't seem to be any elegant way to do this
        int retries = 0;
        synchronized (device) {
            while (main.getTracker() == null && retries < 60) {
                try {
                    device.wait(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
                retries++;
                if (retries >= 60) {
                    Assert.fail("Tracker service didn't start");
                }
            }
        }
        // set min distance to 1m
        prefs.setGpsDistance(0);

        main.invalidateOptionsMenu();

        TestUtils.zoomToLevel(device, main, 19);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/follow", false);

        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Start GPX track", false, false));

        final CountDownLatch signal = new CountDownLatch(1);
        main.getTracker().getTrack().reset(); // clear out anything saved
        SendNMEA.send(main, "2020_03_27.nmea", new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Pause GPX track", true, false));
        List<TrackPoint> recordedTrack = main.getTracker().getTrack().getTrack();
        Assert.assertEquals(215, recordedTrack.size());
        Location lastLocation = main.getTracker().getLastLocation();
        Assert.assertEquals(47.39804275, lastLocation.getLatitude(), 0.000001);
        Assert.assertEquals(8.376432616666667, lastLocation.getLongitude(), 0.000001);
    }
}
