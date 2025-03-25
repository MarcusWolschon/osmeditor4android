package io.vespucci.osm;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import io.vespucci.App;
import io.vespucci.JavaResources;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.Relation;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapSplitSourceTest {

    private static final String MSF_FILE = "liechtenstein.msf";
    Context                     context  = null;
    AdvancedPrefDatabase        prefDB   = null;
    Main                        main     = null;
    UiDevice                    device   = null;
    File                        msfFile  = null;

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
        LayerUtils.removeTaskLayer(context);
        LayerUtils.removeImageryLayers(context);
        prefDB = new AdvancedPrefDatabase(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        try {
            msfFile = JavaResources.copyFileFromResources(main, MSF_FILE, null, ".");
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
        prefDB.setAPIDescriptors(api.id, api.name, api.url, null, api.notesurl, api.auth);
        prefDB.close();
        if (msfFile != null) {
            msfFile.delete();
        }
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
        TestUtils.clickText(device, false, "Advanced preferences", true, false);
        TestUtils.clickText(device, false, "Server settings", true, false);
        TestUtils.clickText(device, false, "OSM API URL", true, false);
        TestUtils.longClickText(device, "OpenStreetMap");
        TestUtils.clickText(device, false, "Edit", true, false);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/listedit_file_button", true);
        TestUtils.selectFile(device, main, null, MSF_FILE, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "OK", true, false));
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
        TestUtils.clickText(device, false, "Load current view", false, false);
        TestUtils.findText(device, false, "Loading", 2000); // spinner appears
        TestUtils.textGone(device, "Loading", 60000);// spinner goes away
        StorageDelegator delegator = App.getDelegator();
        Assert.assertNotNull(delegator.getOsmElement(Relation.NAME, 1252853L));
        Assert.assertNotNull(delegator.getOsmElement(Way.NAME, 243055643L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 49939577L));
    }
}
