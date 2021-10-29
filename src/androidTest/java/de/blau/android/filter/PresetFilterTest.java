package de.blau.android.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
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
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PresetFilterTest {

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
        monitor = instrumentation.addMonitor(PresetFilterActivity.class.getName(), null, false);
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        TestUtils.loadTestData(main, "test2.osm");
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);
    }

    /**
     * Set preset filter to Church and select one
     */
    @Test
    public void presetFilter() {
        TestUtils.unlock(device);
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.menu_enable_presetfilter), true, false);
        TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/tagFilterButton", true);
        Activity presetFilterActivity = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(presetFilterActivity instanceof PresetFilterActivity);
        assertTrue(TestUtils.clickText(device, false, "Facilities", true));
        assertTrue(TestUtils.clickText(device, false, "Place of Worship", true, true));
        assertTrue(TestUtils.clickText(device, false, "Church", true));
        TestUtils.sleep(2000);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.38819D, 47.38961D);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_wayselect), 5000));
        assertEquals(206010144L, App.getLogic().getSelectedWay().getOsmId());
        TestUtils.clickUp(device);
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.menu_enable_presetfilter), true, false);
    }
}
