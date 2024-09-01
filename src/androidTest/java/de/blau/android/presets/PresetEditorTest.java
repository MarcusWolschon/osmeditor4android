package de.blau.android.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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
import android.content.Intent;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.PresetEditorActivity;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PresetEditorTest {

    private static final String TEST_PRESET_NAME = "Test";
    
    MockWebServerPlus    mockServer      = null;
    Context              context         = null;
    ActivityMonitor      monitor         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    Main                 main            = null;
    UiDevice             device          = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        monitor = instrumentation.addMonitor(PresetEditorActivity.class.getName(), null, false);
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
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
        // delete all additional presets
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            for (PresetInfo p : db.getPresets()) {
                if (TEST_PRESET_NAME.equals(p.name)) {
                    db.deletePreset(p.id);
                }
            }
        }
        App.resetPresets();
    }

    /**
     * Download a preset, activate test matching
     */
    @Test
    public void downloadPreset() {
        PresetEditorActivity.start(main);
        Activity presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(presetEditor instanceof PresetEditorActivity);
        mockServer = new MockWebServerPlus();
        HttpUrl url = mockServer.server().url("military.zip");
        mockServer.server().enqueue(TestUtils.createBinaryReponse("application/zip", "fixtures/military.zip"));
        TestUtils.clickText(device, false, main.getString(R.string.urldialog_add_preset), false, false);
        device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/listedit_editName")), 500);
        UiObject name = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_editName"));
        try {
            name.setText(TEST_PRESET_NAME);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        UiObject value = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_editValue"));
        try {
            value.setText(url.toString());
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        // TestUtils.clickText(device, true, main.getString(R.string.okay), true, false);
        assertTrue(TestUtils.clickButton(device, "android:id/button1", true));
        assertTrue(TestUtils.clickText(device, false, TEST_PRESET_NAME, false, false));
        TestUtils.clickHome(device, true);
        App.resetPresets();
        Preset[] presets = App.getCurrentPresets(main);
        assertEquals(3, presets.length);

        HashMap<String, String> tags = new HashMap<>();
        tags.put("landuse", "military");
        PresetItem match = Preset.findBestMatch(presets, tags, null, null);
        assertEquals("Military", match.getName());

        // move military preset up
        monitor = instrumentation.addMonitor(PresetEditorActivity.class.getName(), null, false);
        PresetEditorActivity.start(main);
        presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(presetEditor instanceof PresetEditorActivity);
        UiObject2 entry = TestUtils.findObjectWithText(device, false, TEST_PRESET_NAME, 100, false);
        UiObject2 menu = entry.getParent().getParent().findObject(By.res(device.getCurrentPackageName() + ":id/listItemMenu"));
        menu.click();
        TestUtils.clickText(device, false, main.getString(R.string.tag_menu_move_up), true, true); // Up needs exact
                                                                                                   // match
        TestUtils.clickHome(device, true);
        App.resetPresets();
        presets = App.getCurrentPresets(main);
        match = Preset.findBestMatch(presets, tags, null, null);
        assertEquals("Military landuse", match.getName());
        assertTrue(match.getIconpath().endsWith("military_military.png"));

        // move display content and check that version is displayed
        monitor = instrumentation.addMonitor(PresetEditorActivity.class.getName(), null, false);
        PresetEditorActivity.start(main);
        presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(presetEditor instanceof PresetEditorActivity);
        entry = TestUtils.findObjectWithText(device, false, TEST_PRESET_NAME, 100, false);
        menu = entry.getParent().getParent().findObject(By.res(device.getCurrentPackageName() + ":id/listItemMenu"));
        menu.click();
        TestUtils.clickText(device, false, main.getString(R.string.menu_edit), true);
        TestUtils.findText(device, false, "0.1.0");
        TestUtils.clickText(device, false, main.getString(R.string.okay), true);
        TestUtils.clickHome(device, true);

        // delete the test preset
        monitor = instrumentation.addMonitor(PresetEditorActivity.class.getName(), null, false);
        PresetEditorActivity.start(main);
        presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(presetEditor instanceof PresetEditorActivity);
        entry = TestUtils.findObjectWithText(device, false, TEST_PRESET_NAME, 100, false);
        menu = entry.getParent().getParent().findObject(By.res(device.getCurrentPackageName() + ":id/listItemMenu"));
        menu.click();
        TestUtils.clickText(device, false, main.getString(R.string.delete), true);
        TestUtils.clickHome(device, true);
        App.resetPresets();
    }

    /**
     * Download a preset via an intent
     */
    @Test
    public void downloadPresetViaIntent() {
        mockServer = new MockWebServerPlus();
        HttpUrl url = mockServer.server().url("military.zip");
        mockServer.server().enqueue(TestUtils.createBinaryReponse("application/zip", "fixtures/military.zip"));
        try {
            Uri uri = Uri.parse("vespucci://preset/?preseturl=" + url.toString() + "&presetname=Military%20objects");
            main.startActivity(new Intent(Intent.ACTION_VIEW, uri));

            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.urldialog_add_preset), true, false));

            Activity presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
            assertTrue(presetEditor instanceof PresetEditorActivity);
            assertTrue(TestUtils.clickButton(device, "android:id/button1", true));
            // assertTrue(TestUtils.clickText(device, true, main.getString(R.string.okay), true, false));
            assertTrue(TestUtils.findText(device, false, main.getString(R.string.urldialog_preset_download_successful), 5000, true));
        } finally {
            App.resetPresets();
        }
    }

    /**
     * Load preset from on device file
     * 
     * Note this tests recursive searching for the xml file too
     */
    @Test
    public void loadPreset() {
        PresetEditorActivity.start(main);
        Activity presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(presetEditor instanceof PresetEditorActivity);
        try {
            File preset = JavaResources.copyFileFromResources(main, "military2.zip", "fixtures/", "/");
            try {
                TestUtils.clickText(device, false, main.getString(R.string.urldialog_add_preset), false, false);
                device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/listedit_editName")), 500);
                UiObject name = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_editName"));
                try {
                    name.setText(TEST_PRESET_NAME);
                } catch (UiObjectNotFoundException e) {
                    fail(e.getMessage());
                }
                UiObject fileButton = device
                        .findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_file_button"));
                try {
                    fileButton.click();
                } catch (UiObjectNotFoundException e) {
                    fail(e.getMessage());
                }
                TestUtils.selectFile(device, main, null, "military2.zip", true);
                assertTrue(TestUtils.findText(device, false, main.getString(R.string.listedit_use_translations), 1000));
                // TestUtils.clickText(device, true, main.getString(R.string.okay), true, false);
                assertTrue(TestUtils.clickButton(device, "android:id/button1", true));
                
                assertTrue(TestUtils.clickText(device, false, TEST_PRESET_NAME, false, false));
                TestUtils.clickHome(device, true);
                App.resetPresets();
                Preset[] presets = App.getCurrentPresets(main);
                assertEquals(2+1, presets.length); // +1 for the auto preset

                HashMap<String, String> tags = new HashMap<>();
                tags.put("military", "trench");
                PresetItem match = Preset.findBestMatch(presets, tags, null, null);
                assertNotNull(match);
                assertEquals("Trench", match.getName());

                // delete the test preset
                monitor = instrumentation.addMonitor(PresetEditorActivity.class.getName(), null, false);
                PresetEditorActivity.start(main);
                presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
                assertTrue(presetEditor instanceof PresetEditorActivity);
                UiObject2 entry = TestUtils.findObjectWithText(device, false, TEST_PRESET_NAME, 100, false);
                UiObject2 menu = entry.getParent().getParent().findObject(By.res(device.getCurrentPackageName() + ":id/listItemMenu"));
                menu.click();
                TestUtils.clickText(device, false, main.getString(R.string.Delete), true);
                TestUtils.clickHome(device, true);
                App.resetPresets();
            } finally {
                if (preset != null) {
                    preset.delete();
                }
            }
        } catch (IOException e1) {
            fail(e1.getMessage());
        }
    }
}
