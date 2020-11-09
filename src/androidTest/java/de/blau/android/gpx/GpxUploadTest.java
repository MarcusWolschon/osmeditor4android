package de.blau.android.gpx;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
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
import de.blau.android.App;
import de.blau.android.Main;
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
        prefs = new Preferences(main);
        tileServer = TestUtils.setupTileServer(main, prefs, "ersatz_background.mbt", true);
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

        try {
            TestUtils.copyFileFromResources(main, GPX_FILE, "/", false);
            clickGpsButton();
            if (TestUtils.findObjectWithText(device, false, "Clear", 1000).getParent().getParent().getParent().isEnabled()) {
                TestUtils.clickText(device, false, "Clear", true, false);
                TestUtils.clickText(device, false, "Clear anyway", true, false);
                clickGpsButton();
            }
            Assert.assertTrue(TestUtils.clickText(device, false, "GPX track management", true, false));
            Assert.assertTrue(TestUtils.clickText(device, false, "Import GPX track", true, false));
            TestUtils.selectFile(device, main, null, GPX_FILE, false);
            TestUtils.textGone(device, "Imported", 10000);
            clickGpsButton();
            Assert.assertTrue(TestUtils.clickText(device, false, "Go to start", true, false));
            clickGpsButton();
            Assert.assertTrue(TestUtils.clickText(device, false, "GPX track management", true, false));
            Assert.assertTrue(TestUtils.clickText(device, false, "Upload", true, false));
            mockServer.enqueue("200");
            Assert.assertTrue(TestUtils.clickResource(device, false, "android:id/button1", true));
            Assert.assertTrue(TestUtils.textGone(device, "Uploading", 5000));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Click the GPS menu button
     */
    void clickGpsButton() {
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
    }
}
