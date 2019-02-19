package de.blau.android.osm;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
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
    UiDevice                    mDevice  = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        prefDB = new AdvancedPrefDatabase(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
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
        TestUtils.clickMenuButton("Preferences", false, true);
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.scrollIntoView(new UiSelector().text("Advanced preferences"));
            TestUtils.clickText(mDevice, false, "Advanced preferences", true);
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickText(mDevice, false, "Server settings", true);
        TestUtils.clickText(mDevice, false, "OSM API URL", true);
        TestUtils.longClickText(mDevice, "OpenStreetMap");
        TestUtils.clickText(mDevice, false, "Edit", true);
        TestUtils.clickButton("de.blau.android:id/listedit_file_button", true);
        appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.scrollIntoView(new UiSelector().text(MSF_FILE));
            TestUtils.clickText(mDevice, false, MSF_FILE, true);
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickText(mDevice, false, "OK", true);
        TestUtils.clickMenuButton("Navigate up", false, true);
        TestUtils.clickMenuButton("Navigate up", false, true);
        TestUtils.clickMenuButton("Navigate up", false, true);
        TestUtils.clickMenuButton("Navigate up", false, true);

        Map map = main.getMap();
        map.getViewBox().fitToBoundingBox(map, new BoundingBox(9.51749D, 47.13685D, 9.52597D, 47.14135D));
        map.invalidate();
        try {
            Thread.sleep(1000); // NOSONAR
        } catch (InterruptedException e) {
        }
        // read some data from the source
        TestUtils.clickMenuButton("Transfer", false, false);
        TestUtils.clickText(mDevice, false, "Load current view", false);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        StorageDelegator delegator = App.getDelegator();
        Assert.assertNotNull(delegator.getOsmElement(Relation.NAME, 1252853L));
        Assert.assertNotNull(delegator.getOsmElement(Way.NAME, 243055643L));
        Storage current = delegator.getCurrentStorage();
        Assert.assertEquals(5364, current.getNodes().size());
        Assert.assertEquals(697, current.getWays().size());
        Assert.assertEquals(60, current.getRelations().size());
    }
}
