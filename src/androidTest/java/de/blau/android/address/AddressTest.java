package de.blau.android.address;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.ModeTest;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.PropertyEditor;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddressTest {

    Context              context         = null;
    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    ActivityMonitor      monitor         = null;
    Instrumentation      instrumentation = null;
    Map                  map             = null;
    Logic                logic           = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        main = mActivityRule.getActivity();
        logic = App.getLogic();
        Preferences prefs = logic.getPrefs();
        LayerUtils.removeImageryLayers(context);
        map = main.getMap();
        prefs.enableSimpleActions(true);
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        Address.resetLastAddresses(main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        logic.deselectAll();
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        App.getTaskStorage().reset();
        Preferences prefs = logic.getPrefs();
        prefs.enableSimpleActions(true);
        map.setPrefs(main, prefs);
    }

    /**
     * Create a new Node by long click and check that we get correct street suggestion and then correct house number
     * from prediction
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newAddressLongClick() {
        TestUtils.unlock(device);
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.menu_simple_actions), true);
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.longClickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add)));
        monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
        TestUtils.clickMenuButton(device, main.getString(R.string.tag_menu_address), false, true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(propertyEditor instanceof PropertyEditor);
        Assert.assertTrue(TestUtils.findText(device, false, "Bergstrasse"));
        Assert.assertTrue(TestUtils.clickMenuButton(device, "More options", false, true));
        TestUtils.clickText(device, false, main.getString(R.string.tag_menu_reset_address_prediction), true, false);
        Assert.assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.tag_menu_address), false, true));
        Assert.assertTrue(TestUtils.findText(device, false, "35"));
        TestUtils.clickHome(device, true);
    }

    /**
     * Create a new address Node in address mode
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newAddress() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        ModeTest.switchMode(main, device, TestUtils.getLock(device), R.string.mode_address, Mode.MODE_ADDRESS);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickSimpleButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_add_node_address), true));
        monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
        TestUtils.clickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(propertyEditor instanceof PropertyEditor);
        Assert.assertTrue(TestUtils.findText(device, false, "Bergstrasse"));
        Assert.assertTrue(TestUtils.findText(device, false, "35"));
        TestUtils.clickHome(device, true);
    }

    /**
     * Create a new address interpolation in address mode
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newInterpolation() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        ModeTest.switchMode(main, device, TestUtils.getLock(device), R.string.mode_address, Mode.MODE_ADDRESS);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickSimpleButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_add_address_interpolation), true));
        TestUtils.findText(device, false, main.getString(R.string.simple_add_way));
        TestUtils.clickAtCoordinates(device, map, 8.3905743, 47.3903159, true);
        TestUtils.sleep();
        TestUtils.clickAtCoordinates(device, map, 8.3909863, 47.3905409, true);
        TestUtils.clickSimpleButton(device);
        Assert.assertTrue(TestUtils.findText(device, false, "Raistrasse"));
        Assert.assertTrue(TestUtils.findText(device, false, "35"));
        Assert.assertTrue(TestUtils.findText(device, false, "27"));
        TestUtils.clickText(device, false, main.getString(R.string.okay), true);
    }
}
