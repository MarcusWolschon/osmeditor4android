package io.vespucci.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.MockTileServer;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.gpx.GpxTest;
import io.vespucci.gpx.Track;
import io.vespucci.gpx.TrackPoint;
import io.vespucci.layer.LayerType;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.services.TrackerService;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GpxBarometerTest {

    public static final int TIMEOUT = 180;

    Main            main            = null;
    UiDevice        device          = null;
    Instrumentation instrumentation = null;
    MockWebServer   tileServer      = null;
    Preferences     prefs           = null;

    /**
     * Manual start of activity so that we can set up the monitor for main
     */
    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        // this sets the mock location permission
        instrumentation.getUiAutomation().executeShellCommand("appops set de.blau.android android:mock_location allow");

        prefs = App.getPreferences(instrumentation.getTargetContext());
        prefs.setGpsDistance(0);
        prefs.enableBarometricHeight(true); // this needs to be set before TrackerService is created

        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        ActivityScenario.launch(Main.class);

        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);

        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);

        Logic logic = App.getLogic();
        Map map = main.getMap();

        App.getDelegator().reset(true);
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            db.deleteLayer(LayerType.GPX, null);
        }

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        instrumentation.waitForIdleSync();
        prefs.enableBarometricHeight(false);
        try {
            tileServer.close();
        } catch (IOException | NullPointerException e) {
            // ignore
        }
        if (main != null) {
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
                db.deleteLayer(LayerType.GPX, null);
            }
            TestUtils.stopEasyEdit(main);
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }
    }

    /**
     * Turn on using barometric height, calibrate default pressure at sea level to -100m
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void recordWithBarometricElevation() {
        Intent intent = new Intent(main, TrackerService.class);
        intent.putExtra(TrackerService.CALIBRATE_KEY, true);
        intent.putExtra(TrackerService.CALIBRATE_HEIGHT_KEY, -100);
        main.bindService(intent, main, Context.BIND_AUTO_CREATE);

        main.startService(intent);
        //
        TestUtils.setupMockLocation(main, Criteria.ACCURACY_FINE);
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
                    fail("Tracker service didn't start");
                }
            }
        }

        TestUtils.zoomToLevel(device, main, 19);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/follow", false);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("short.gpx");
        Track track = new Track(main, false);
        track.importFromGPX(is);

        // set a different current location so that the first point always gets recorded
        int trackSize = track.getTrackPoints().size();
        TrackPoint startPoint = track.getTrackPoints().get(trackSize / 2);
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(startPoint.getLatitude());
        loc.setLongitude(startPoint.getLongitude());
        main.getTracker().updateLocation(loc);
        TestUtils.sleep();
        main.invalidateOptionsMenu();

        GpxTest.clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_start), false, false));
        GpxTest.clickAwayTip(device, main);

        final CountDownLatch signal = new CountDownLatch(1);
        main.getTracker().getTrack().reset(); // clear out anything saved
        TestUtils.injectLocation(main, track.getTrackPoints(), Criteria.ACCURACY_FINE, 1000, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        GpxTest.clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_pause), true, false));

        TrackPoint recorded = main.getTracker().getTrack().getTrackPoints().get(1);
        assertTrue(recorded.hasAltitude());
        assertEquals(-105f, recorded.getAltitude(), 1f);
    }
}
