package de.blau.android.gpx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.SignalHandler;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.osm.Track;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.osm.Track.WayPoint;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.util.FileUtil;
import de.blau.android.util.Util;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GpxTest {

    public static final int      TIMEOUT                = 115;
    private static final Pattern EXPORT_MESSAGE_PATTERN = Pattern.compile("^Exported\\sto\\s(.*\\.gpx)$", Pattern.CASE_INSENSITIVE);

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

        prefs = new Preferences(main);
        tileServer = TestUtils.setupTileServer(main, prefs, "ersatz_background.mbt");
        App.getLogic().setPrefs(prefs);
        main.getMap().setPrefs(main, prefs);

        TestUtils.grantPermissons(device);
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
            System.out.println("main is null");
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
     * Replay a pre-recorded track and check that we record the same
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void recordSaveAndImportGpx() {
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

        TestUtils.zoomToLevel(device, main, 19);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/follow", false);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("20110513_121244-tp.gpx");
        Track track = new Track(main);
        track.importFromGPX(is);

        // set a different current location so that the first point always gets recorded
        int trackSize = track.getTrack().size();
        TrackPoint startPoint = track.getTrack().get(trackSize / 2);
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(startPoint.getLatitude());
        loc.setLongitude(startPoint.getLongitude());
        main.getTracker().updateLocation(loc);
        main.invalidateOptionsMenu();

        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Start GPX track", false));

        final CountDownLatch signal = new CountDownLatch(1);
        main.getTracker().getTrack().reset(); // clear out anything saved
        TestUtils.injectLocation(main, track.getTrack(), Criteria.ACCURACY_FINE, 1000, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Pause GPX track", true));
        List<TrackPoint> recordedTrack = main.getTracker().getTrack().getTrack();

        compareTrack(track, recordedTrack);
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "GPX track management", true));

        UiObject snackbarTextView = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/snackbar_text"));
        Assert.assertTrue(TestUtils.clickText(device, false, "Export GPX track", false));
        //
        Assert.assertTrue(snackbarTextView.waitForExists(10000));
        String filename = null;
        try {
            String t = snackbarTextView.getText();
            Matcher m = EXPORT_MESSAGE_PATTERN.matcher(t);
            if (m.find()) {
                filename = m.group(1);
            }
            System.out.println("filename >" + filename + "<");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        // need to wait for the snackbar to go away
        snackbarTextView.waitUntilGone(5000);
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "GPX track management", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Import GPX track", true));
        TestUtils.selectFile(device, null, filename);
        Assert.assertTrue(TestUtils.clickText(device, false, "Replace", true));
        recordedTrack = main.getTracker().getTrack().getTrack(); // has been reloaded
        compareTrack(track, recordedTrack);
        try {
            File exportedFile = new File(FileUtil.getPublicDirectory(), filename);
            exportedFile.delete();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Import a track file with waypoints and create an OSM object from one of them
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void importWayPoints() {
        Assert.assertNotNull(main);
        main.getTracker().getTrack().reset(); // clear out anything saved
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("20110513_121244-tp.gpx");
        main.getTracker().getTrack().importFromGPX(is);
        Assert.assertEquals(112, main.getTracker().getTrack().getTrack().size());
        Assert.assertEquals(79, main.getTracker().getTrack().getWayPoints().length);
        WayPoint foundWp = null;
        for (WayPoint wp : main.getTracker().getTrack().getWayPoints()) {
            if (doubleEquals(47.3976189, wp.getLatitude()) && doubleEquals(8.3770144, wp.getLongitude())) {
                foundWp = wp;
                break;
            }
        }
        Assert.assertNotNull(foundWp);
        Map map = main.getMap();
        ViewBox viewBox = map.getViewBox();
        App.getLogic().setZoom(map, 19);
        viewBox.moveTo(map, foundWp.getLon(), foundWp.getLat());
        map.invalidate();

        TestUtils.unlock(device);

        TestUtils.clickAtCoordinates(device, map, foundWp.getLon(), foundWp.getLat(), true);

        Assert.assertTrue(TestUtils.clickText(device, true, "Create osm object from", true));
        Assert.assertTrue(TestUtils.findText(device, false, "Church"));
    }

    /**
     * Replay a track and pretend the output are network generated locations
     */
    @Test
    public void followNetworkLocation() {
        Assert.assertNotNull(main);

        // set min distance to 1m
        prefs.setGpsDistance(0);

        TestUtils.zoomToLevel(device, main, 19);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/follow", false);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("20110513_121244-tp.gpx");
        Track track = new Track(main);
        track.importFromGPX(is);
        main.getTracker().getTrack().reset(); // clear out anything saved
        final CountDownLatch signal = new CountDownLatch(1);
        TestUtils.injectLocation(main, track.getTrack(), Criteria.ACCURACY_COARSE, 1000, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Pause GPX track", true));
        // compare roughly with last location
        TrackPoint lastPoint = track.getTrack().get(track.getTrack().size() - 1);
        ViewBox box = main.getMap().getViewBox();
        Assert.assertEquals(lastPoint.getLatitude(), box.getCenterLat(), 0.001);
        Assert.assertEquals(lastPoint.getLongitude(), ((box.getLeft() - box.getRight()) / 2 + box.getRight()) / 1E7D, 0.001);
    }

    /**
     * Check if too doubles are equal with a fixed epsilon
     * 
     * @param d1 double one
     * @param d2 double two
     * @return true if equal
     */
    boolean doubleEquals(double d1, double d2) {
        double epsilon = 0.0000001D;
        return d1 < d2 + epsilon && d1 > d2 - epsilon;
    }

    /**
     * Check if two tracks are equal, ignores up to two missing points
     * 
     * @param track reference track
     * @param recordedTrack new track
     */
    private void compareTrack(Track track, List<TrackPoint> recordedTrack) {
        List<TrackPoint> trackPoints = track.getTrackPoints();
        int trackSize = trackPoints.size();
        int recordedTrackSize = recordedTrack.size();
        // compare with a bit of tolerance
        Assert.assertTrue((recordedTrackSize >= (trackSize - 2)) && (recordedTrackSize <= trackSize));
        int i = 0;
        int offset = 0;
        TrackPoint trackPoint = trackPoints.get(0);
        TrackPoint trackPoint2 = recordedTrack.get(0);
        if (!Util.equals(trackPoint.getLatitude(), trackPoint2.getLatitude(), 0.000001)
                || !Util.equals(trackPoint.getLongitude(), trackPoint2.getLongitude(), 0.000001)) {
            i = 1;
            offset = 1;
        }
        for (; i < Math.min(trackSize, recordedTrackSize); i++) {
            TrackPoint tp = trackPoints.get(i);
            TrackPoint recordedTrackPoint = recordedTrack.get(i - offset);
            Assert.assertEquals(tp.getLatitude(), recordedTrackPoint.getLatitude(), 0.000001);
            Assert.assertEquals(tp.getLongitude(), recordedTrackPoint.getLongitude(), 0.000001);
            Assert.assertEquals(tp.getAltitude(), recordedTrackPoint.getAltitude(), 0.000001);
        }
    }
}
