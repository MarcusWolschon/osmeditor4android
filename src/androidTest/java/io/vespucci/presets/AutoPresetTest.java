package io.vespucci.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Set;
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
import android.util.Log;
import android.view.KeyEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.contract.Paths;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.Tags;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.prefs.API.AuthParams;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.propertyeditor.PropertyEditorActivity;
import io.vespucci.util.FileUtil;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AutoPresetTest {

    private static final String AMENITY_PAYMENT_CENTRE_LABEL = "amenity payment_centre";
    private static final String AMENITY_PAYMENT_CENTRE       = "amenity\tpayment_centre";
    MockWebServerPlus           mockTaginfoServer            = null;
    MockWebServerPlus           mockApiServer                = null;
    Context                     context                      = null;
    AdvancedPrefDatabase        prefDB                       = null;
    Instrumentation             instrumentation              = null;
    Main                        main                         = null;
    UiDevice                    device                       = null;
    Preferences                 prefs                        = null;
    private ActivityMonitor     monitor;

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
        prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        mockTaginfoServer = new MockWebServerPlus();
        HttpUrl mockTaginfoUrl = mockTaginfoServer.server().url("");
        System.out.println("mock api url " + mockTaginfoUrl.toString()); // NOSONAR
        prefs.setTaginfoServer(mockTaginfoUrl.toString());
        mockApiServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockApiServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        System.out.println(prefs.getServer().getReadWriteUrl());
        device = UiDevice.getInstance(instrumentation);
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockTaginfoServer.server().shutdown();
            instrumentation.removeMonitor(monitor);
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }

        // zap the contents of the directory
        try {
            File dir = new File(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET);
            for (String fileName : dir.list()) {
                try {
                    java.nio.file.Files.delete(new File(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET + Paths.DELIMITER + fileName).toPath());
                } catch (IOException e) {
                    System.out.println("Removing auto-preset file " + fileName + " exception " + e); // NOSONAR
                }
            }
        } catch (IOException e) {
            System.out.println("Removing auto-preset exception " + e); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
    }

    /**
     * Search for a term, apply and create a preset item
     */
    @Test
    public void onlinePresetSearch() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockApiServer.enqueue("capabilities1");
        mockApiServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580L);
        assertNotNull(n);

        main.performTagEdit(n, null, false, true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        // in case these presets exist, this removes them
        // from the index so that they will not be found when
        // de-duping
        Preset.removeItem(main, AMENITY_PAYMENT_CENTRE);
        Preset.removeItem(main, "amenity\tpayment_terminal");
        for (Preset preset : App.getCurrentPresets(main)) {
            if (preset != null) {
                Set<PresetItem> existingPresets = preset.getItemByTag("amenity\tpayment_terminal");
                if (!existingPresets.isEmpty()) {
                    fail("amenity\tpayment_terminal exists in Preset " + preset.getShortDescription());
                }
            }
        }

        UiSelector uiSelector = new UiSelector().resourceId(device.getCurrentPackageName() + ":id/preset_search_edit");
        UiObject field = device.findObject(uiSelector);
        try {
            field.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_P);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_A);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_Y);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_M);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_E);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_N);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_T);
        mockTaginfoServer.enqueue("autopreset1");
        mockTaginfoServer.enqueue("autopreset2");
        mockTaginfoServer.enqueue("autopreset3");
        mockTaginfoServer.enqueue("autopreset4");
        mockTaginfoServer.enqueue("autopreset5");
        mockTaginfoServer.enqueue("autopreset6");
        mockTaginfoServer.enqueue("autopreset7");
        device.waitForIdle();
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/search_online", true));
        try {
            mockTaginfoServer.server().takeRequest();
            mockTaginfoServer.server().takeRequest();
            mockTaginfoServer.server().takeRequest();
            mockTaginfoServer.server().takeRequest();
            mockTaginfoServer.server().takeRequest();
            mockTaginfoServer.server().takeRequest();
            mockTaginfoServer.server().takeRequest();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        UiObject2 preset = TestUtils.findObjectWithText(device, false, AMENITY_PAYMENT_CENTRE_LABEL, 20000, false);
        assertNotNull(preset);
        preset.clickAndWait(Until.newWindow(), 10000);

        TestUtils.sleep(30000);
        TestUtils.clickHome(device, true); // close the PropertEditor and save
        assertEquals("payment_centre", n.getTagWithKey(Tags.KEY_AMENITY));
        // check auto-preset
        Preset[] presets = App.getCurrentPresets(main);
        Preset autoPreset = presets[presets.length - 1];
        assertNotNull(autoPreset);
        assertFalse(autoPreset.getItemByTag(AMENITY_PAYMENT_CENTRE).isEmpty());
        // restart and remove
        instrumentation.removeMonitor(monitor);
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        main.performTagEdit(n, null, false, true);
        propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        TestUtils.clickText(device, false, "Auto-preset", true);
        TestUtils.longClickText(device, AMENITY_PAYMENT_CENTRE_LABEL);
        TestUtils.findText(device, true, "Delete", 10000);
        TestUtils.clickText(device, true, "Delete", true);
        TestUtils.clickHome(device, true);
        assertTrue(autoPreset.getItemByTag(AMENITY_PAYMENT_CENTRE).isEmpty());
    }
}
