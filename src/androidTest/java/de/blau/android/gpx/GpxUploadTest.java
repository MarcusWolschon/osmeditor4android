package de.blau.android.gpx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.Main;
import de.blau.android.MockTileServer;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GpxUploadTest {

    private static final String GPX_FILE        = "20110513_121244-tp.gpx";
    Main                        main            = null;
    UiDevice                    device          = null;
    Instrumentation             instrumentation = null;
    MockWebServer               tileServer      = null;
    Preferences                 prefs           = null;
    MockWebServerPlus           mockServer      = null;
    AdvancedPrefDatabase        prefDB          = null;

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
        main = mActivityRule.getActivity();
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        prefDB = new AdvancedPrefDatabase(main);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);
        prefs = new Preferences(main);
        App.getLogic().setPrefs(prefs);
        main.getMap().setPrefs(main, prefs);

        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        main.finish();
        try {
            tileServer.close();
        } catch (IOException | NullPointerException e) {
            // ignore
        }
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        instrumentation.waitForIdleSync();
    }

    /**
     * Replay a pre-recorded track and check that we record the same
     */
    @Test
    public void uploadGpx() {
        assertNotNull(main);

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
        try {
            File gpxFile = JavaResources.copyFileFromResources(main, GPX_FILE, null, "/");
            try {
                clickGpsButton(device);
                UiObject2 clearItem = TestUtils.findObjectWithText(device, false, main.getString(R.string.menu_gps_clear), 1000, false);
                assertNotNull(clearItem);
                if (isEnabled(clearItem)) {
                    TestUtils.clickText(device, false, main.getString(R.string.menu_gps_clear), true, false);
                    TestUtils.clickText(device, false, main.getString(R.string.clear_anyway), true, false);
                    clickGpsButton(device);
                }
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_track_managment), true, false));
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_import), true, false));
                TestUtils.selectFile(device, main, null, GPX_FILE, true);
                TestUtils.textGone(device, "Imported", 10000);
                clickGpsButton(device);
                if (!TestUtils.findText(device, false, main.getString(R.string.menu_gps_goto_start))) {
                    TestUtils.scrollTo(main.getString(R.string.menu_gps_goto_start));
                }
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_goto_start), true, false));
                TestUtils.clickText(device, false, main.getString(R.string.okay), false); // click away tip
                clickGpsButton(device);
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_track_managment), true, false));
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_upload), true, false));
                mockServer.enqueue("200");
                assertTrue(TestUtils.clickResource(device, false, "android:id/button1", true));
                assertTrue(TestUtils.textGone(device, "Uploading", 5000));
            } finally {
                TestUtils.deleteFile(main, GPX_FILE);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Check if all parents are enabled
     * 
     * @param item the object to check
     * @return true if all parents are enabled
     */
    public static boolean isEnabled(@NonNull UiObject2 item) {
        do {
            if (!item.isEnabled()) {
                return false;
            }
            item = item.getParent();
        } while (item != null);
        return true;
    }

    /**
     * Click the GPS menu button
     * 
     * @param device the UiDevice
     */
    public static void clickGpsButton(@NonNull UiDevice device) {
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
    }
}
