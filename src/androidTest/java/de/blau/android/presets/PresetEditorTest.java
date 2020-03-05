package de.blau.android.presets;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.resources.TileLayerServer;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okio.Buffer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PresetEditorTest {

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
        main = (Main) mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);
    }

    /**
     * Download a preset and activate
     */
    @Test
    public void downloadPreset() {
        PresetEditorActivity.start(main);
        Activity presetEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(presetEditor instanceof PresetEditorActivity);
        mockServer = new MockWebServerPlus();
        HttpUrl url = mockServer.server().url("military.zip");
        // MockWebServerPLus currently doesn't handle non-text bodies properly
        // so we do this manually
        MockResponse response = new MockResponse();
        response.setHeader("Content-type", "application/zip");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream("fixtures/military.zip");
        Buffer buffer = new Buffer();
        try {
            buffer.readFrom(inputStream);
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        }
        response.setBody(buffer);
        mockServer.server().enqueue(response);
        TestUtils.clickText(device, false, main.getString(R.string.urldialog_add_preset), false);
        device.wait(Until.findObject(By.clickable(true).res("de.blau.android:id/listedit_editName")), 500);
        UiObject name = device.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/listedit_editName"));
        try {
            name.setText("Test");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        UiObject value = device.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/listedit_editValue"));
        try {
            value.setText(url.toString());
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickText(device, true, main.getString(R.string.okay), true);
        TestUtils.clickText(device, false, "Test", false);
        TestUtils.clickHome(device);
        App.resetPresets();
        Preset[] presets = App.getCurrentPresets(main);
        Assert.assertEquals(3, presets.length);
    }
}
