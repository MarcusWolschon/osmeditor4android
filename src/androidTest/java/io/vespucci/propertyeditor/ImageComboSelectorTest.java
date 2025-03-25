package io.vespucci.propertyeditor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

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
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.JavaResources;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.Node;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.prefs.PresetLoader;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetComboField;
import io.vespucci.presets.PresetItem;
import io.vespucci.propertyeditor.PropertyEditorActivity;
import io.vespucci.util.StringWithDescription;
import io.vespucci.util.StringWithDescriptionAndIcon;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ImageComboSelectorTest {

    private Context         context         = null;
    private Instrumentation instrumentation = null;
    private Main            main            = null;
    private UiDevice        device          = null;
    private Map             map;
    private Logic           logic;
    private String          presetId        = null;

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
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            File preset = JavaResources.copyFileFromResources(main, "bicycle_parking.zip", null, "/");
            presetId = java.util.UUID.randomUUID().toString();
            db.addPreset(presetId, "Bicycle parking", "", true);
            File presetDir = db.getPresetDirectory(presetId);
            presetDir.mkdir();
            PresetLoader.load(context, Uri.parse(preset.toURI().toString()), presetDir, Preset.PRESETXML);
            db.movePreset(1, 0); // otherwise we get the builtin preset
            App.resetPresets();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
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
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            if (presetId != null) {
                db.deletePreset(presetId);
            }
        }
    }

    /**
     * Create a new Node set it to a amenity=bicycle_parking and then set the type
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void bicycleParking() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickSimpleButton(device);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_node_tags), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_node_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.3893454, 47.3901898, true);

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);

        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        boolean found = TestUtils.clickText(device, true, PropertyEditorTest.getTranslatedPresetItemName(main, "Parking"), true, false);
        assertTrue(found);
        try {
            UiObject2 type = PropertyEditorTest.getField(device, "Type", 1);
            assertNotNull(type);
            type.click();
        } catch (UiObjectNotFoundException e) {
            fail();
        }

        // scroll to the right till we find the correct image
        while (!(TestUtils.findText(device, false, "Handlebar holder"))) {
            TestUtils.clickMenuButton(device, main.getString(R.string.forward), false, false);
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.select), true));

        assertTrue(TestUtils.findText(device, false, "Handlebar holder"));

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        device.waitForIdle();
        Node n = logic.getSelectedNode();
        assertNotNull(n);
        assertTrue(n.hasTag("bicycle_parking", "handlebar_holder"));
    }

    /**
     * Check that the images can actually be opened
     */
    @Test
    public void checkImage() {
        Preset[] presets = App.getCurrentPresets(main);
        for (Preset p:presets) {
            PresetItem item =p.getItemByName("Parking", null);
            if (item != null) {
                PresetComboField combo = (PresetComboField) item.getField("bicycle_parking");
                for (StringWithDescription value: combo.getValues()) {
                    if ("stands".equals(value.getValue()) && value instanceof StringWithDescriptionAndIcon) {
                        String imagePath = ((StringWithDescriptionAndIcon) value).getImagePath();
                        if (imagePath != null) {
                            assertTrue(new File(imagePath).exists());
                            return;
                        }
                    }
                }
            }
        }
        fail("preset not found");
    }
}
