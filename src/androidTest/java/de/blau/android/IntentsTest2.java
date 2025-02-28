package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class IntentsTest2 {

    MockWebServerPlus    mockServer       = null;
    MockWebServerPlus    mockServerNotes  = null;
    MockWebServerPlus    mockServerOsmose = null;
    Context              context          = null;
    ActivityMonitor      geoMonitor       = null;
    ActivityMonitor      rcMonitor        = null;
    ActivityMonitor      mainMonitor      = null;
    AdvancedPrefDatabase prefDB           = null;
    Instrumentation      instrumentation  = null;
    UiDevice             device           = null;
    Main                 main             = null;

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
        App.getDelegator().reset(false);
        App.getTaskStorage().reset();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefs.setTaskFilter(null);
        final Map map = main.getMap();
        map.setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        mockServerNotes = new MockWebServerPlus();
        HttpUrl mockNotesUrl = mockServerNotes.server().url("/api/0.6/");
        System.out.println("mock notes api url " + mockNotesUrl.toString());
        //
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, mockNotesUrl.toString(), new AuthParams(API.Auth.BASIC, "user", "pass", null, null));
        prefDB.selectAPI("Test");
        mockServerOsmose = new MockWebServerPlus();
        mockBaseUrl = mockServerOsmose.server().url("/en/api/0.2/");
        prefs.putString(R.string.config_osmoseServer_key, mockBaseUrl.scheme() + "://" + mockBaseUrl.host() + ":" + mockBaseUrl.port() + "/");
        App.getLogic().setPrefs(prefs);
        LayerUtils.addTaskLayer(main);
        map.setUpLayers(main);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        App.getDelegator().reset(false);
        App.getTaskStorage().reset();
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        LayerUtils.removeTaskLayer(main);
        if (geoMonitor != null) {
            instrumentation.removeMonitor(geoMonitor);
        }
        if (rcMonitor != null) {
            instrumentation.removeMonitor(rcMonitor);
        }
        try {
            mockServer.server().shutdown();
            mockServerNotes.server().shutdown();
            mockServerOsmose.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Start via url see https://developer.android.com/training/app-links/deep-linking which however doesn't work during
     * testing so we use a hack
     */
    private void startViaUrl(@NonNull String url) {
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        main.startActivity(intent);
        String justOnce = "Just once";
        if (TestUtils.findText(device, false, "Open with Vespucci", 5000)) {
            TestUtils.clickText(device, false, justOnce, false, false);
        } else {
            TestUtils.clickText(device, false, TestUtils.VESPUCCI, false, false);
            TestUtils.clickText(device, false, justOnce, false, false);
        }
        Main m = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 60000);
        assertNotNull(m);
        // instrumentation.getUiAutomation().executeShellCommand("am start -W -a android.intent.action.VIEW -d \"" + url
        // + "\"");
    }

    /**
     * Test osm links work
     */
    @Test
    public void openstreetmapWebsiteNode() {
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("node-1");
        startViaUrl("https://www.openstreetmap.org/node/1#12345");
        TestUtils.selectIntentRecipient(device);
        TestUtils.clickAwayTip(device, context);
        Node n = App.getLogic().getSelectedNode();
        assertNotNull(n);
        assertEquals(1L, n.getOsmId());
    }

    /**
     * Test osm links work
     */
    @Test
    public void openstreetmapWebsiteWay() {
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("way-100");
        startViaUrl("https://www.openstreetmap.org/way/100");

        TestUtils.clickAwayTip(device, context);
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);
        assertEquals(100L, w.getOsmId());
    }

    /**
     * Test osm links work
     */
    @Test
    public void openstreetmapWebsiteNote() {
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("note-100");
        startViaUrl("https://www.openstreetmap.org/note/100");

        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.openstreetbug_edit_title)));
    }
}
