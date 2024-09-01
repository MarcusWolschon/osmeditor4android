package de.blau.android.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.validation.Validator;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TagFilterTest {

    Main            main            = null;
    Map             map             = null;
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
        map = main.getMap();
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
        main.deleteDatabase(TagFilterDatabaseHelper.DATABASE_NAME);
    }

    /**
     * Enable tagfilter, add filter, click object, remove filter etc
     */
    @Test
    public void simpleTagTest() {
        TestUtils.unlock(device);
        Node t = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(t);
        assertTrue(t.hasTag("amenity", "toilets"));

        // enable tag filter
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_enable_tagfilter), false));

        // start filter config activity
        monitor = instrumentation.addMonitor(TagFilterActivity.class.getName(), null, false);
        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/tagFilterButton", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); //
        instrumentation.removeMonitor(monitor);

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.empty_list), 5000));

        // add empty entry
        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/add", true));

        UiObject2 key = TestUtils.findObjectWithText(device, false, main.getString(R.string.key), 500, false);
        key.setText("amenity");
        UiObject2 value = TestUtils.findObjectWithText(device, false, main.getString(R.string.value), 500, false);
        value.setText("toilets");

        // exit config activity
        TestUtils.clickHome(device, false);
        TestUtils.sleep(5000); // android 9 needs this

        TestUtils.clickAtCoordinates(device, map, t.getLon() / 1E7D, t.getLat() / 1E7D, true);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_nodeselect), 5000));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(3465444349L, node.getOsmId());

        // start filter config activity
        monitor = instrumentation.addMonitor(TagFilterActivity.class.getName(), null, false);
        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/tagFilterButton", true));
        instrumentation.waitForMonitorWithTimeout(monitor, 40000); //
        instrumentation.removeMonitor(monitor);

        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/delete", false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.empty_list), 5000));

        // exit config activity
        TestUtils.clickHome(device, false);
        TestUtils.sleep(5000); // android 9 needs this

        // clicking now should select nothing
        TestUtils.clickAtCoordinates(device, map, t.getLon() / 1E7D, t.getLat() / 1E7D, true);
        assertFalse(TestUtils.findText(device, false, main.getString(R.string.actionmode_nodeselect), 2000));

        // disable tag filter
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_enable_tagfilter), false));
    }
}