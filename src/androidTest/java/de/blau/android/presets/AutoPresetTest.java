package de.blau.android.presets;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
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
    UiDevice             device          = null;
    Preferences          prefs           = null;

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
        mockServer = new MockWebServerPlus();
        HttpUrl mockTaginfoUrl = mockServer.server().url("");
        System.out.println("mock api url " + mockTaginfoUrl.toString()); // NOSONAR
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
        assertNotNull(fromTaginfo.getItemByName("amenity payment_centre"));
        assertNotNull(fromTaginfo.getItemByName("amenity payment_terminal"));
    }
}
