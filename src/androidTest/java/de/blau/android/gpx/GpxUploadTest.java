package de.blau.android.gpx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import ch.poole.android.screenshotrule.ScreenshotRule;
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
    
    @Rule
    public ScreenshotRule screenshotRule = new ScreenshotRule();

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
        prefs = new Preferences(main);
        tileServer = MockTileServer.setupTileServer(main, prefs, "ersatz_background.mbt", true);
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
            JavaResources.copyFileFromResources(main, GPX_FILE, null, "/");
            screenshotRule.screenshot(main, "gpx_upload_1");
            clickGpsButton();
            if (TestUtils.findObjectWithText(device, false, main.getString(R.string.menu_gps_clear), 1000).getParent().getParent().getParent().isEnabled()) {
                TestUtils.clickText(device, false, main.getString(R.string.menu_gps_clear), true, false);
                TestUtils.clickText(device, false, main.getString(R.string.clear_anyway), true, false);
                clickGpsButton();
            }
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_track_managment), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_import), true, false));
            TestUtils.selectFile(device, main, null, GPX_FILE, false);
            TestUtils.textGone(device, "Imported", 10000);
            clickGpsButton();
            screenshotRule.screenshot(main, "gpx_upload_2");
            TestUtils.scrollTo(main.getString(R.string.menu_gps_goto_start));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_goto_start), true, false));
            clickGpsButton();
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_track_managment), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_upload), true, false));
            mockServer.enqueue("200");
            assertTrue(TestUtils.clickResource(device, false, "android:id/button1", true));
            assertTrue(TestUtils.textGone(device, "Uploading", 5000));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Click the GPS menu button
     */
    void clickGpsButton() {
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
    }
}
