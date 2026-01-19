package de.blau.android.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;

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
import android.util.Log;
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
import de.blau.android.prefs.PresetConfigurationEditorActivity;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ImportExportConfigurationTest {

    private static final String PRESET_ID = "aab1d149-6183-4df8-8e28-2fc805e8d12f";

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
        monitor = instrumentation.addMonitor(PresetConfigurationEditorActivity.class.getName(), null, false);
        main = mActivityRule.getActivity();
        deleteTestPreset();
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
        deleteTestPreset();
        App.resetPresets();
    }

    /**
     * 
     */
    private void deleteTestPreset() {
        // delete test preset
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            db.deletePreset(PRESET_ID);
        }
    }

    /**
     * import a configuration and download a preset configured in it
     */
    @Test
    public void importConfiguration() {
        File configFile = null;
        try {
            configFile = JavaResources.copyFileFromResources(main, "config.xml", null, "/");

            mockServer = new MockWebServerPlus();
            HttpUrl url = mockServer.server().url("military.zip");
       
            String line = new String(Files.readAllBytes(configFile.toPath()));
            String changed = line.replace("https://github.com/simonpoole/militarypreset/releases/latest/download/military.zip", url.toString());
            Files.write(configFile.toPath(), changed.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            
            mockServer.server().enqueue(TestUtils.createBinaryReponse("application/zip", "fixtures/military.zip"));

            if (!TestUtils.clickMenuButton(device, main.getString(R.string.menu_tools), false, true)) {
                TestUtils.clickOverflowButton(device);
                TestUtils.clickText(device, false, main.getString(R.string.menu_tools), true, false);
            }
            TestUtils.scrollTo(main.getString(R.string.menu_tools_import_configuration), false);
            TestUtils.clickText(device, false, main.getString(R.string.menu_tools_import_configuration), true, false);
            TestUtils.selectFile(device, main, null, "config.xml", true);
            assertTrue(TestUtils.findText(device, false, main.getString(R.string.download_missing_title), 10000));
            TestUtils.clickButton(device, "android:id/button1", true);
            try {
                RecordedRequest request = mockServer.takeRequest();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            TestUtils.sleep(2000);
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
                assertNotNull(db.getPreset(PRESET_ID));
                assertTrue(db.getResourceDirectory(PRESET_ID).exists());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            configFile.delete();
        }
    }

}
