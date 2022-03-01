package de.blau.android.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.MockTileServer;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.gpx.MapOverlay;
import de.blau.android.osm.Node;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.services.util.ExtendedLocation;
import de.blau.android.util.FileUtil;
import de.blau.android.util.Util;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GpxTest {

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
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        // this sets the mock location permission
        instrumentation.getUiAutomation().executeShellCommand("appops set de.blau.android android:mock_location allow");

        main = mActivityRule.getActivity();

        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);
        de.blau.android.layer.Util.addLayer(main, LayerType.GPX);
        prefs = new Preferences(main);
        Logic logic = App.getLogic();
        logic.setPrefs(prefs);
        Map map = main.getMap();
        map.setPrefs(main, prefs);

        App.getDelegator().reset(true);

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
     * Replay a pre-recorded track and check that we record the same
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void recordSaveAndImportGpx() {
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
        // set min distance to 1m
        prefs.setGpsDistance(0);

        TestUtils.zoomToLevel(device, main, 19);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/follow", false);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("20110513_121244-tp.gpx");
        Track track = new Track(main, false);
        track.importFromGPX(is);

        // set a different current location so that the first point always gets recorded
        int trackSize = track.getTrack().size();
        TrackPoint startPoint = track.getTrack().get(trackSize / 2);
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(startPoint.getLatitude());
        loc.setLongitude(startPoint.getLongitude());
        main.getTracker().updateLocation(loc);
        TestUtils.sleep();
        main.invalidateOptionsMenu();

        clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_start), false, false));
        clickAwayTip(device, main);

        final CountDownLatch signal = new CountDownLatch(1);
        main.getTracker().getTrack().reset(); // clear out anything saved
        TestUtils.injectLocation(main, track.getTrack(), Criteria.ACCURACY_FINE, 1000, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_pause), true, false));
        List<TrackPoint> recordedTrack = main.getTracker().getTrack().getTrack();

        compareTrack(track, recordedTrack);
        UiObject2 menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_gpx_recording), LayerDialogTest.MENU_BUTTON);
        menuButton.click();
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_export), false, false));
        String filename = "" + System.currentTimeMillis() + ".gpx";
        TestUtils.selectFile(device, main, null, filename, true, true);

        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_gpx), true, false));
        TestUtils.selectFile(device, main, null, filename, true);

        TestUtils.textGone(device, "Imported", 10000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        recordedTrack = null;
        for (MapViewLayer l : main.getMap().getLayers()) {
            if (l instanceof de.blau.android.layer.gpx.MapOverlay && filename.equals(((de.blau.android.layer.gpx.MapOverlay) l).getName())) {
                recordedTrack = ((de.blau.android.layer.gpx.MapOverlay) l).getTrack().getTrackPoints();
            }
        }
        assertNotNull(recordedTrack);
        compareTrack(track, recordedTrack);
        try {
            File exportedFile = new File(FileUtil.getPublicDirectory(), filename);
            exportedFile.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), false, false));
        // goto start while we are here
        menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_gpx_recording), LayerDialogTest.MENU_BUTTON);
        menuButton.click();
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_goto_start), false, false));
        TestUtils.sleep(2000);
        double[] center = main.getMap().getViewBox().getCenter();
        TrackPoint first = track.getTrack().get(0);
        assertEquals(first.longitude, center[0], 0.01);
        assertEquals(first.latitude, center[1], 0.01);

        // clear out the track
        track = main.getTracker().getTrack();
        assertFalse(track.getTrack().isEmpty());
        clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_clear), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.clear_anyway), true, false));
        assertTrue(track.getTrack().isEmpty());
    }

    /**
     * Import a track file with waypoints and create an OSM object from one of them
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void importWayPoints() {
        try {
            File gpxFile = JavaResources.copyFileFromResources(main, GpxUploadTest.GPX_FILE, null, "/");
            assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
            assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_gpx), true, false));
            TestUtils.selectFile(device, main, null, GpxUploadTest.GPX_FILE, true);
            TestUtils.textGone(device, "Imported", 10000);
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true, false));
            WayPoint foundWp = null;
            for (MapViewLayer layer : main.getMap().getLayers()) {
                if (layer instanceof de.blau.android.layer.gpx.MapOverlay && GpxUploadTest.GPX_FILE.equals(layer.getName())) {
                    assertEquals(GpxUploadTest.GPX_FILE, layer.getName());
                    Track track = ((de.blau.android.layer.gpx.MapOverlay) layer).getTrack();
                    assertEquals(112, track.getTrack().size());
                    assertEquals(79, track.getWayPoints().length);
                    for (WayPoint wp : track.getWayPoints()) {
                        if (doubleEquals(47.3976189, wp.getLatitude()) && doubleEquals(8.3770144, wp.getLongitude())) {
                            foundWp = wp;
                            break;
                        }
                    }
                    break;
                }
            }
            assertNotNull(foundWp);
            Map map = main.getMap();
            ViewBox viewBox = map.getViewBox();
            App.getLogic().setZoom(map, 19);
            viewBox.moveTo(map, foundWp.getLon(), foundWp.getLat()); // NOSONAR
            map.invalidate();

            TestUtils.unlock(device);

            TestUtils.clickAtCoordinates(device, map, foundWp.getLon(), foundWp.getLat(), true);

            assertTrue(TestUtils.clickText(device, true, "Create osm object from", true, false));
            assertTrue(TestUtils.findText(device, false, "Church"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Replay a track and pretend the output are network generated locations
     */
    @Test
    public void followNetworkLocation() {
        TestUtils.setupMockLocation(main, Criteria.ACCURACY_COARSE);
        // set min distance to 1m
        prefs.setGpsDistance(0);

        TestUtils.zoomToLevel(device, main, 19);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/follow", false);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("20110513_121244-tp.gpx");
        Track track = new Track(main, false);
        track.importFromGPX(is);
        main.getTracker().getTrack().reset(); // clear out anything saved
        final CountDownLatch signal = new CountDownLatch(1);
        TestUtils.injectLocation(main, track.getTrack(), Criteria.ACCURACY_COARSE, 1000, new SignalHandler(signal));
        TestUtils.sleep(TIMEOUT * 1000L);
        clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, "Pause GPX track", true, false));
        // compare roughly with last location
        TrackPoint lastPoint = track.getTrack().get(track.getTrack().size() - 1);
        ViewBox box = main.getMap().getViewBox();
        assertEquals(lastPoint.getLatitude(), box.getCenterLat(), 0.001);
        assertEquals(lastPoint.getLongitude(), ((box.getLeft() - box.getRight()) / 2d + box.getRight()) / 1E7D, 0.001);
    }

    /**
     * Open the current position info dialog and create a Node at the current position
     */
    @Test
    public void createNodeAtLocation() {
        ExtendedLocation loc = new ExtendedLocation(LocationManager.GPS_PROVIDER);
        final double lat = 47.3978982D;
        final double lon = 8.3762937D;
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setAltitude(600);
        loc.setBarometricHeight(555);
        loc.setGeoidCorrection(48);
        loc.setGeoidHeight(552);
        loc.setHdop(2.0);
        main.getTracker().updateLocation(loc);
        main.invalidateOptionsMenu();

        clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, "Current position info", true, false));
        assertTrue(TestUtils.findText(device, false, Double.toString(lat)));
        assertTrue(TestUtils.findText(device, false, Double.toString(lon)));

        assertTrue(TestUtils.clickText(device, false, "New Node", true, false));
        assertTrue(TestUtils.findText(device, false, "Node selected"));
        Node n = App.getLogic().getSelectedNode();
        assertNotNull(n);
        assertEquals(lat, n.getLat() / 1E7D, 0.000001);
        assertEquals(lon, n.getLon() / 1E7D, 0.000001);
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
        System.out.println("Track size " + trackSize);
        System.out.println("Recorded track size " + recordedTrackSize);
        assertTrue((recordedTrackSize >= (trackSize - 2)) && (recordedTrackSize <= trackSize));
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
            assertEquals(tp.getLatitude(), recordedTrackPoint.getLatitude(), 0.000001);
            assertEquals(tp.getLongitude(), recordedTrackPoint.getLongitude(), 0.000001);
            // we don't include altitude anymore assertEquals(tp.getAltitude(), recordedTrackPoint.getAltitude(),
            // 0.000001);
        }
    }

    /**
     * Click the GPS menu button
     * 
     * @param device the UiDevice
     */
    public static void clickGpsButton(@NonNull UiDevice device) {
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
    }

    /**
     * Click away the tip dialogs
     * 
     * @param device the UiDevice
     * @param context an Android context
     */
    static void clickAwayTip(@NonNull UiDevice device, @NonNull Context context) {
        TestUtils.clickText(device, false, context.getString(R.string.next), false);
        TestUtils.clickText(device, false, context.getString(R.string.okay), false); // click away tip
    }
}
