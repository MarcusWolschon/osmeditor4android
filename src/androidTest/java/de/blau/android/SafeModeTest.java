package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
import de.blau.android.layer.MapViewLayer;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyleManager;

/**
 * 
 * This test was originally written with ActivityScenario however that delivers the launch intent -twice- to the
 * activity, the 1st time without the intent extras
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
    Splash          splash          = null;

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
        splash = mActivityRule.launchActivity(start);
        assertNotNull(splash);
    }

    /**
     * Reset map style and disable all layers
     */
    @Test
    public void defaultOptions() {
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.Continue), true));
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        Main main = (Main) monitor.waitForActivityWithTimeout(30000);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, context);

        Preferences prefs = App.getLogic().getPrefs();
        DataStyleManager styles = App.getDataStyleManager(main);
        assertEquals(styles.getBuiltinStyleName(), prefs.getDataStyle(styles));

        Map map = App.getLogic().getMap();
        for (MapViewLayer l : map.getLayers()) {
            assertFalse(l.isVisible());
        }
    }

    /**
     * Reset editing state
     */
    @Test
    public void resetEditingState() {
        try (FileOutputStream out = splash.openFileOutput(Logic.EDITSTATE_FILENAME, Context.MODE_PRIVATE)) {
            out.write(1);
            out.flush();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try (FileInputStream in = splash.openFileInput(Logic.EDITSTATE_FILENAME)) {
            assertEquals(1, in.read());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/safe_editing_state_check", false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.Continue), true));
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        Main main = (Main) monitor.waitForActivityWithTimeout(30000);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, context);
        try (FileInputStream in = main.openFileInput(Logic.EDITSTATE_FILENAME)) {
            assertNotEquals(1, in.read()); // as onPause gets called somewhere the file will actually have data in it
        } catch (IOException fnex) {
            // good
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
        Main main = (Main) monitor.waitForActivityWithTimeout(30000);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, context);
        assertTrue(App.getDelegator().isEmpty());
    }
}
