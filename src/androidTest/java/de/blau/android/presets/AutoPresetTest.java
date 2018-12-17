package de.blau.android.presets;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.FileUtil;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AutoPresetTest {

    MockWebServerPlus    mockServer      = null;
    Context              context         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    Main                 main            = null;
    UiDevice             mDevice         = null;
    Preferences          prefs           = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        main = (Main) mActivityRule.getActivity();
        prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        mockServer = new MockWebServerPlus();
        HttpUrl mockTaginfoUrl = mockServer.server().url("");
        System.out.println("mock api url " + mockTaginfoUrl.toString());
        prefs.setTaginfoServer(mockTaginfoUrl.toString());
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        try {
            // zap the generated preset file
            FileUtil.copyFileFromAssets(context, Files.FILE_NAME_AUTOPRESET_TEMPLATE,
                    FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET), Files.FILE_NAME_AUTOPRESET);
        } catch (IOException e) {
            System.out.println("Removing auto-preset exception " + e);
        }
    }

    /**
     * Search for "payment" and create two preset items
     */
    @Test
    public void createPayment() {
        mockServer.enqueue("autopreset1");
        mockServer.enqueue("autopreset2");
        mockServer.enqueue("autopreset3");
        mockServer.enqueue("autopreset4");
        mockServer.enqueue("autopreset5");
        mockServer.enqueue("autopreset6");
        mockServer.enqueue("autopreset7");

        // in case these presets exist, this removes them
        // from the index so that they will not be found when
        // de-duping
        Preset.removeItem(main, "amenity\tpayment_centre");
        Preset.removeItem(main, "amenity\tpayment_terminal");

        AutoPreset autoPreset = new AutoPreset(context);
        Preset fromTaginfo = autoPreset.fromTaginfo("payment", 3);
        Assert.assertTrue(fromTaginfo.getItemByName("amenity payment_centre") != null);
        Assert.assertTrue(fromTaginfo.getItemByName("amenity payment_terminal") != null);
    }
}
