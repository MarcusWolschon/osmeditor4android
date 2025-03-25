package io.vespucci.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiSelector;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.Node;
import io.vespucci.osm.Way;
import io.vespucci.prefs.PrefEditor;
import io.vespucci.prefs.Preferences;
import io.vespucci.validation.Validator;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ValidationPrefTest {

    Main            main            = null;
    View            v               = null;
    ActivityMonitor monitor         = null;
    Instrumentation instrumentation = null;
    UiDevice        device          = null;

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
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(main);
        LayerUtils.removeImageryLayers(main);
        Map map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        if (monitor != null) {
            instrumentation.removeMonitor(monitor);
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Start prefs, advanced prefs, validator
     */
    @Test
    public void resurveyTest() {
        Node t = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(t);
        assertTrue(t.hasTag("amenity", "toilets"));
        assertEquals(Validator.AGE, t.hasProblem(main, App.getDefaultValidator(main)));
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); //

        if (!TestUtils.scrollToAndSelect(device, main.getString(R.string.config_validatorprefs_title), new UiSelector().scrollable(true))) {
            fail("Didn't find " + main.getString(R.string.config_validatorprefs_title));
        }

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_validatorprefs_title), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_validatorprefs_title)));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_validatorprefs_summary), true, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.resurvey_entries)));

        assertTrue(TestUtils.clickText(device, false, "toilets", true, true));
        assertTrue(TestUtils.findText(device, false, "toilets", 1000));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.delete), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        TestUtils.clickHome(device, false);
        TestUtils.clickHome(device, false);
        assertEquals(Validator.OK, t.hasProblem(main, App.getDefaultValidator(main)));
    }

    /**
     * Start prefs, advanced prefs, validator
     */
    @Test
    public void missingTagTest() {
        Way t = (Way) App.getDelegator().getOsmElement(Way.NAME, 96291968L);
        assertNotNull(t);
        assertTrue(t.hasTag("amenity", "school"));
        assertEquals(Validator.MISSING_TAG, t.hasProblem(main, App.getDefaultValidator(main)));
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); //

        if (!TestUtils.scrollToAndSelect(device, main.getString(R.string.config_validatorprefs_title), new UiSelector().scrollable(true))) {
            fail("Didn't find " + main.getString(R.string.config_validatorprefs_title));
        }

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_validatorprefs_title), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_validatorprefs_title)));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_validatorprefs_summary), true, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.resurvey_entries)));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.check_entries), true, true));

        assertTrue(TestUtils.clickText(device, false, "wheelchair", true, true));
        assertTrue(TestUtils.findText(device, false, "wheelchair", 1000));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.delete), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        TestUtils.clickHome(device, false);
        TestUtils.clickHome(device, false);
        assertEquals(Validator.OK, t.hasProblem(main, App.getDefaultValidator(main)));
    }

    /**
     * Start prefs, validator, disable missing tag validation ...
     */
    @Test
    public void validationDisable() {
        Way t = (Way) App.getDelegator().getOsmElement(Way.NAME, 96291968L);
        assertNotNull(t);
        assertTrue(t.hasTag("amenity", "school"));
        t.resetHasProblem();
        assertEquals(Validator.MISSING_TAG, t.hasProblem(main, App.getDefaultValidator(main)));

        // toggle off
        toggleValidation();
        assertEquals(Validator.OK, t.hasProblem(main, App.getDefaultValidator(main)));

        // toggle on
        toggleValidation();
        assertEquals(Validator.MISSING_TAG, t.hasProblem(main, App.getDefaultValidator(main)));
    }

    /**
     * Toggle the Missing tags validation
     */
    private void toggleValidation() {
        monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); //

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_validatorprefs_title), true));

        if (!TestUtils.scrollToAndSelect(device, main.getString(R.string.config_enabledValidations_title), new UiSelector().scrollable(true))) {
            fail("Didn't find " + main.getString(R.string.config_enabledValidations_title));
        }

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_enabledValidations_title), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.config_enabledValidations_title)));

        assertTrue(TestUtils.clickText(device, false, "Missing tags", false, false));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        TestUtils.clickHome(device, false);
        TestUtils.clickHome(device, false);
    }
}