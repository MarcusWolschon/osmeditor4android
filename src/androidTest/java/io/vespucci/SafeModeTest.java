package io.vespucci;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.Splash;
import io.vespucci.layer.MapViewLayer;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.DataStyle;

/**
 * 
 * This test was originally written with ActivityScenario however that delivers the launch intent -twice- to the
 * activity, the 1st time with out the intent extras
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SafeModeTest {

    Instrumentation instrumentation = null;
    Context         context         = null;
    UiDevice        device          = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class, false, false);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = instrumentation.getTargetContext();
        Intent start = Intent.makeMainActivity(new ComponentName(context, Splash.class));
        start.putExtra(Splash.SAFE, true);
        Splash splash = mActivityRule.launchActivity(start);
        assertNotNull(splash);
    }

    /**
     * Reset map style and disable all layers
     */
    @Test
    public void defaultOptions() {
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.Continue), true));
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        Main main = (Main) monitor.waitForActivityWithTimeout(5000);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, context);

        Preferences prefs = App.getLogic().getPrefs();
        DataStyle styles = App.getDataStyle(main);
        assertEquals(prefs.getDataStyle(styles), styles.getBuiltinStyleName());

        Map map = App.getLogic().getMap();
        for (MapViewLayer l : map.getLayers()) {
            assertFalse(l.isVisible());
        }
    }

    /**
     * Reset map style and disable all layers
     */
    @Test
    public void resetState() {
        TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/safe_state_check", false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.Continue), true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.safe_delete_state_title), 5000));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.safe_delete_state_text), true));
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        Main main = (Main) monitor.waitForActivityWithTimeout(5000);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, context);
        assertTrue(App.getDelegator().isEmpty());
    }
}
