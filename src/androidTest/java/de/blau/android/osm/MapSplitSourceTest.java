package de.blau.android.osm;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.TestUtils;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapSplitSourceTest {

    private static final String MSF_FILE = "liechtenstein.msf";
    Context                     context  = null;
    AdvancedPrefDatabase        prefDB   = null;
    Main                        main     = null;
    UiDevice                    device   = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBugsEnabled(false);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        prefDB = new AdvancedPrefDatabase(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        try {
            TestUtils.copyFileFromResources(MSF_FILE, ".");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        App.getDelegator().reset(false);
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        API api = prefDB.getCurrentAPI();
        prefDB.setAPIDescriptors(api.id, api.name, api.url, null, api.notesurl, api.oauth);
    }

    /**
     * Configure a MapSplit source, read an area and do some superficial checks that that was successful
     */
    @Test
    public void configureAndLoad() {
        // configure the source in the preferences
        TestUtils.clickMenuButton(device, "Preferences", false, true);
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.scrollIntoView(new UiSelector().text("Advanced preferences"));
        } catch (UiObjectNotFoundException e) {
            // if there is no scrollable then this will fail
        }
        TestUtils.clickText(device, false, "Advanced preferences", true);
        TestUtils.clickText(device, false, "Server settings", true);
        TestUtils.clickText(device, false, "OSM API URL", true);
        TestUtils.longClickText(device, "OpenStreetMap");
        TestUtils.clickText(device, false, "Edit", true);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/listedit_file_button", true);
        TestUtils.selectFile(device, null, MSF_FILE);
        Assert.assertTrue(TestUtils.clickText(device, false, "OK", true));
        TestUtils.clickMenuButton(device, "Navigate up", false, true);
        TestUtils.clickMenuButton(device, "Navigate up", false, true);
        TestUtils.clickMenuButton(device, "Navigate up", false, true);
        TestUtils.clickMenuButton(device, "Navigate up", false, true);

        Map map = main.getMap();
        map.getViewBox().fitToBoundingBox(map, new BoundingBox(9.51749D, 47.13685D, 9.52597D, 47.14135D));
        map.invalidate();
        try {
            Thread.sleep(1000); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
        }
        // read some data from the source
        TestUtils.clickMenuButton(device, "Transfer", false, false);
        TestUtils.clickText(device, false, "Load current view", false);
        TestUtils.findText(device, false, "Loading", 2000); // spinner appears
        TestUtils.textGone(device, "Loading", 60000);// spinner goes away
        StorageDelegator delegator = App.getDelegator();
        Assert.assertNotNull(delegator.getOsmElement(Relation.NAME, 1252853L));
        Assert.assertNotNull(delegator.getOsmElement(Way.NAME, 243055643L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 49939577L));
    }
}
