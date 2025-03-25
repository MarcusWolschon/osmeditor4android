package io.vespucci.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.util.DownloadActivity;

/**
 * 1st attempts at testing lifecycle related aspects in easyedit modes
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DownloadActivityTest {

    private static final String AUSTRIA = "austria.msf";

    Context                 context      = null;
    AdvancedPrefDatabase    prefDB       = null;
    Main                    main         = null;
    UiDevice                device       = null;
    Map                     map          = null;
    Logic                   logic        = null;
    private Instrumentation instrumentation;
    ActivityScenario<Main>  mainScenario = null;
    MockWebServerPlus       mockServer   = null;

    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        mainScenario = ActivityScenario.launch(Main.class);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        mockServer = new MockWebServerPlus();
        prefDB = new AdvancedPrefDatabase(context);
        for (API api : prefDB.getAPIs()) {
            if (AUSTRIA.equals(api.name)) {
                prefDB.deleteAPI(api.id);
                break;
            }
        }
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        mainScenario.moveToState(State.DESTROYED);
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
    }

    /**
     * Download a MSF file
     */
    @Test
    public void msfDownload() {
        Intent intent = new Intent(main, DownloadActivity.class);
        intent.putExtra(DownloadActivity.DOWNLOAD_SITE_KEY, mockServer.server().url("/").toString());
        mockServer.enqueue("mapsplit");
        mockServer.enqueue("mapsplit_style");
        mockServer.enqueue("mapsplit_at");
        ActivityMonitor monitor = instrumentation.addMonitor(DownloadActivity.class.getName(), null, false);

        try (ActivityScenario<DownloadActivity> scenario = ActivityScenario.launch(intent)) {
            DownloadActivity activity = (DownloadActivity) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
            TestUtils.scrollTo(AUSTRIA, false);
            assertTrue(TestUtils.clickText(device, false, AUSTRIA, false));

            TestUtils.findNotification(device, "Download Manager", AUSTRIA);
            prefDB = new AdvancedPrefDatabase(context);
            boolean found = false;
            for (API api : prefDB.getAPIs()) {
                if (AUSTRIA.equals(api.name)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
            TestUtils.clickHome(device, false);
        }
    }
}
