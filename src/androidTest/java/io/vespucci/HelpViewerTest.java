package io.vespucci;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HelpViewerTest {

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
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Start up the HelpViewer, should do at least some UI tests
     */
    @Test
    public void startHelp() {
        if (!TestUtils.clickMenuButton(device, "Help", false, true)) {
            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, "Help", true, false);
        }
        // Waiting with a monitor doesn't work in this case
        Assert.assertTrue(TestUtils.findText(device, false, "Help: Main map display", 10000));
        Assert.assertTrue(TestUtils.clickMenuButton(device, "OK", false, true));
        Assert.assertTrue(TestUtils.clickText(device, false, "GPS sources", true, false));
        // contents seems to take longer than the header
        BySelector bySelector = By.clickable(true).textContains("This is currently experimental");
        device.wait(Until.findObject(bySelector), 10000);
        Assert.assertTrue(TestUtils.clickMenuButton(device, "Back", false, true));
        bySelector = By.clickable(true).textContains("Main Vespucci Screen");
        device.wait(Until.findObject(bySelector), 10000);
        Assert.assertTrue(TestUtils.clickMenuButton(device, "Back", false, true));
    }
}
