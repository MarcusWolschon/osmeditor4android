package de.blau.android;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PrivacyPolicyViewerTest {

    MockWebServerPlus    mockServer      = null;
    Context              context         = null;
    ActivityMonitor      monitor         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    UiDevice             device          = null;
    Main                 main            = null;

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
        main = (Main) mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);

        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
    }

    /**
     * Start up the HelpViewer, which then should show the policy
     */
    @Test
    public void startPrivacy() {
        TestUtils.clickOverflowButton();
        TestUtils.clickText(device, false, "Privacy", true);
        Assert.assertTrue(TestUtils.findText(device, false, "Privacy statement", 30000));
        Assert.assertTrue(TestUtils.clickMenuButton("Back", false, true));
    }
}
