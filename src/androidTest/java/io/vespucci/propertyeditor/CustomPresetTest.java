package io.vespucci.propertyeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.Until;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.Way;
import io.vespucci.prefs.Preferences;
import io.vespucci.propertyeditor.PropertyEditorActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CustomPresetTest {

    private Context         context         = null;
    private Instrumentation instrumentation = null;
    private Main            main            = null;
    private UiDevice        device          = null;
    private Map             map;
    private Logic           logic;

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
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(() -> main.showSimpleActionsButton());
        map = main.getMap();
        map.setPrefs(main, prefs);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        App.getTaskStorage().reset();
    }

    /**
     * Select a service, tag it as a driveway and turn it in to a preset
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void driveway() {
        map.getDataLayer().setVisible(true);

        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);

        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3869798, 47.3892145, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect), 5000));
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);
        assertEquals(185670974L, w.getOsmId());

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3869798, 47.3892145, true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        assertTrue(TestUtils.findText(device, false, "Service way type"));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false));
        try {
            UiObject2 value = getFieldForKey(device, "service", 2);
            value.setText("driveway");
            UiObject2 check = getFieldForKey(device, "highway", 0);
            check.click();
            check = getFieldForKey(device, "service", 0);
            check.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.tag_action_tag_title)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_menu_create_preset), false, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.create_preset_title)));
        UiObject presetName = TestUtils.findObjectWithResourceId(device, false, device.getCurrentPackageName() + ":id/text_line_edit");
        try {
            presetName.setText("Custom preset test");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        // go to tag form tab
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false));
        // go to preset tab
        assertTrue(TestUtils.findText(device, true, main.getString(R.string.tag_menu_preset), 2000));
        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        assertTrue(TestUtils.findText(device, true, main.getString(R.string.preset_autopreset)));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.preset_autopreset), true, false));
        assertTrue(TestUtils.clickText(device, true, "Custom preset test", true, false));
    }

    /**
     * Get a row field for a specific key
     * 
     * @param mDevice the device
     * @param key the text display for the key
     * @param fieldIndex the index of the field
     * @return an UiObject2 for the value field
     * @throws UiObjectNotFoundException if we couldn't find the object with text
     */
    public static UiObject2 getFieldForKey(UiDevice mDevice, @NonNull String key, int fieldIndex) throws UiObjectNotFoundException {
        TestUtils.scrollTo(key, false);
        BySelector bySelector = By.textStartsWith(key).res(mDevice.getCurrentPackageName() + ":id/editKey");
        UiObject2 keyField = mDevice.wait(Until.findObject(bySelector), 500);
        UiObject2 linearLayout = keyField.getParent();
        if (!linearLayout.getClassName().equals("android.widget.LinearLayout")) {
            // some of the text fields are nested one level deeper
            linearLayout = linearLayout.getParent();
        }
        return linearLayout.getChildren().get(fieldIndex);
    }
}
