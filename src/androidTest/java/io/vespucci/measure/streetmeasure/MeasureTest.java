package io.vespucci.measure.streetmeasure;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Way;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.prefs.API.AuthParams;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.presets.PresetTagField;
import io.vespucci.presets.ValueType;
import io.vespucci.propertyeditor.PropertyEditorActivity;
import io.vespucci.propertyeditor.PropertyEditorTest;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MeasureTest {

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
        File mruTags = new File("/sdcard/Vespucci/mrutags.xml");
        mruTags.delete();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        main = (Main) mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        device = UiDevice.getInstance(instrumentation);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.unlock(device);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 21);
        try {
            mockServer.server().shutdown();
            instrumentation.removeMonitor(monitor);
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
    }

    /**
     * Select a way and "measure" the width
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void wayWidthMeasure() {
        Way w = setUpWay();
        TestUtils.clickText(device, false, main.getString(R.string.measure), true); // click measure button

        // exit and test that everything has been set correctly
        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(w.hasTag("width", "1.23"));
    }

    /**
     * Select a way and set the width
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void wayWidthSet() {
        Way w = setUpWay();
        UiObject text = TestUtils.findObjectWithResourceId(device, false, device.getCurrentPackageName() + ":id/text_line_edit");
        try {
            text.setText("3.21");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickText(device, false, main.getString(R.string.save), true); // click save button

        // exit and test that everything has been set correctly
        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(w.hasTag("width", "3.21"));
    }

    /**
     * Setup everything in the PropertyEditor and return the Way in question
     * 
     * @return the Way we are testing on
     */
    private Way setUpWay() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 23);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3848461, 47.3899166, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↖ Kindhauserstrasse", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);
        // make sure width has the right ValueType
        PresetItem tertiary = Preset.findBestMatch(App.getCurrentPresets(main), w.getTags(), null, null);
        assertNotNull(tertiary);
        PresetTagField widthField = tertiary.getField("width");
        assertNotNull(widthField);
        widthField.setValueType(ValueType.DIMENSION_HORIZONTAL);

        assertTrue(TestUtils.clickMenuButton(device, "Properties", false, true));
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        instrumentation.waitForIdleSync();
        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse"));

        // Apply best preset
        assertTrue(TestUtils.clickMenuButton(device, "Apply preset with optional", false, false));

        UiObject2 width = null;
        try {
            width = PropertyEditorTest.getField(device, "Width (meters)", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(width);
        width.click();
        return w;
    }

}
