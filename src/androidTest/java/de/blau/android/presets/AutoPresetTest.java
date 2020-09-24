package de.blau.android.presets;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import android.view.KeyEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.util.FileUtil;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AutoPresetTest {

    private static final String AMENITY_PAYMENT_CENTRE = "amenity\tpayment_centre";
    MockWebServerPlus           mockTaginfoServer      = null;
    MockWebServerPlus           mockApiServer          = null;
    Context                     context                = null;
    AdvancedPrefDatabase        prefDB                 = null;
    Instrumentation             instrumentation        = null;
    Main                        main                   = null;
    UiDevice                    device                 = null;
    Preferences                 prefs                  = null;
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
        TestUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        mockTaginfoServer = new MockWebServerPlus();
        HttpUrl mockTaginfoUrl = mockTaginfoServer.server().url("");
        System.out.println("mock api url " + mockTaginfoUrl.toString()); // NOSONAR
        prefs.setTaginfoServer(mockTaginfoUrl.toString());
        mockApiServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockApiServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        System.out.println(prefs.getServer().getReadWriteUrl());
        device = UiDevice.getInstance(instrumentation);
        monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
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
        try {
            // zap the generated preset file
            FileUtil.copyFileFromAssets(context, Files.FILE_NAME_AUTOPRESET_TEMPLATE,
                    FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), Files.FILE_NAME_AUTOPRESET);
        } catch (IOException e) {
            System.out.println("Removing auto-preset exception " + e); // NOSONAR
        }
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
            Assert.fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580L);
        Assert.assertNotNull(n);

        main.performTagEdit(n, null, false, true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(propertyEditor instanceof PropertyEditor);
        // in case these presets exist, this removes them
        // from the index so that they will not be found when
        // de-duping
        Preset.removeItem(main, AMENITY_PAYMENT_CENTRE);
        Preset.removeItem(main, "amenity\tpayment_terminal");

        UiSelector uiSelector = new UiSelector().resourceId(device.getCurrentPackageName() + ":id/preset_search_edit");
        UiObject field = device.findObject(uiSelector);
        try {
            field.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
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
        Assert.assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/search_online", true));
        Assert.assertTrue(TestUtils.findText(device, false, "amenity payment_centre", 10000));
        Assert.assertTrue(TestUtils.clickText(device, false, "amenity payment_centre", true, true));
        TestUtils.clickHome(device, true); // close the PropertEditor and save
        Assert.assertEquals("payment_centre", n.getTagWithKey(Tags.KEY_AMENITY));
        // check auto-preset
        Preset[] presets = App.getCurrentPresets(main);
        Preset autoPreset = presets[presets.length - 1];
        Assert.assertNotNull(autoPreset);
        Assert.assertFalse(autoPreset.getItemByTag(AMENITY_PAYMENT_CENTRE).isEmpty());
        // restart and remove
        instrumentation.removeMonitor(monitor);
        monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
        main.performTagEdit(n, null, false, true);
        propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(propertyEditor instanceof PropertyEditor);
        TestUtils.clickText(device, false, "Auto-preset", true);
        TestUtils.longClickText(device, "amenity payment_centre");
        TestUtils.findText(device, true, "Delete", 10000);
        TestUtils.clickText(device, true, "Delete", true);
        TestUtils.clickHome(device, true);
        Assert.assertTrue(autoPreset.getItemByTag(AMENITY_PAYMENT_CENTRE).isEmpty());
    }
}
