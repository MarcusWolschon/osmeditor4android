package de.blau.android.resources;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OAMTest {

    MockWebServerPlus    mockServer       = null;
    Context              context          = null;
    AdvancedPrefDatabase prefDB           = null;
    Main                 main             = null;
    String               mockServerString = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        UiDevice device = UiDevice.getInstance(instrumentation);
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/meta");
        System.out.println("mock api url " + mockBaseUrl.toString());
        mockServerString = mockBaseUrl.scheme() + "://" + mockBaseUrl.host() + ":" + mockBaseUrl.port() + "/";
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        System.out.println(prefs.getServer().getReadWriteUrl());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
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
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Query the OAM catalog and assert that a certain layer is present
     */
    @Test
    public void oamCatalog() {
        mockServer.enqueue("oam");
        OAMCatalog catalog = new OAMCatalog();
        List<OAMCatalog.Entry> list = null;
        try {
            list = catalog.getEntries(main, mockServerString, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(list);
        Assert.assertEquals(100, list.size());
        Assert.assertEquals(100, catalog.getLimit());
        OAMCatalog.Entry entry = list.get(12);
        Assert.assertEquals("Bricenio Ecuador Earthquake", entry.title);
        Assert.assertEquals("http://tiles.openaerialmap.org/572b2552cd0663bb003c32a2/0/572b25b72b67227a79b4fbef/{z}/{x}/{y}.png", entry.tileUrl);
        Assert.assertEquals(-80.29468433281558D, entry.box.getLeft() / 1E7D, 0.0000001);
        Assert.assertEquals(-0.43514940868965246D, entry.box.getBottom() / 1E7D, 0.0000001);
        Assert.assertEquals(-80.29117135958357D, entry.box.getRight() / 1E7D, 0.0000001);
        Assert.assertEquals(-0.4300190885506524D, entry.box.getTop() / 1E7D, 0.0000001);
    }

    /**
     * Query the OAM catalog and check that the filtering out of junk works
     */
    @Test
    public void oamCatalogFiltered() {
        mockServer.enqueue("oam2");
        OAMCatalog catalog = new OAMCatalog();
        List<OAMCatalog.Entry> list = null;
        try {
            list = catalog.getEntries(main, mockServerString, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());
    }
}
