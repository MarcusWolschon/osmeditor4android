package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.FileUtil;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DebugInformationTest {

    MockWebServerPlus    mockServer      = null;
    Context              context         = null;
    ActivityMonitor      monitor         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    UiDevice             device          = null;
    Main                 main            = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        monitor = instrumentation.addMonitor(DebugInformation.class.getName(), null, false);
        main = (Main) mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);

        try {
            TestUtils.turnOffNetwork(device);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);

        try {
            TestUtils.turnOnNetwork(device);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Start up the DebugInformation activity
     */
    @Test
    public void startDebugInformation() {
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.config_debugbutton_title), true, false);
        Activity debugInformation = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(debugInformation instanceof DebugInformation);

        // empty crash directory
        try {
            File crashesDir = new File(FileUtil.getPublicDirectory(), de.blau.android.contract.Paths.DIRECTORY_PATH_CRASHES);
            if (crashesDir.exists()) {
                for (File report : crashesDir.listFiles()) {
                    report.delete();
                }
                crashesDir.delete();
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // generate report as network is turned off, this should be written to the crashes directory
        TestUtils.scrollTo(main.getString(R.string.send_debug_information), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.send_debug_information), true));
        TestUtils.clickButton(device, "android:id/button1", true);
        TestUtils.sleep(10000);
        try {
            File crashesDir = new File(FileUtil.getPublicDirectory(), de.blau.android.contract.Paths.DIRECTORY_PATH_CRASHES);
            assertTrue(crashesDir.exists());
            assertEquals(1, crashesDir.listFiles().length);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
