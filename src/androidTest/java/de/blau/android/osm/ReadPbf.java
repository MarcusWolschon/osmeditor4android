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
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadPbf {

    private static final String PBF_FILE = "vaduz.pbf";
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
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        try {
            TestUtils.copyFileFromResources(PBF_FILE, ".");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        App.getDelegator().reset(false);;
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
    }

    /**
     * Read a PBF file and do some superficial checks that it was successfully read
     */
    @Test
    public void pbfRead() {
        TestUtils.clickMenuButton("Transfer", false, false);
        TestUtils.clickText(mDevice, false, "File",  false);
        TestUtils.clickText(mDevice, false, "Read from PBF file",  false);
        //
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.scrollIntoView(new UiSelector().text(PBF_FILE));
        } catch (UiObjectNotFoundException e) {
            // if there is no scrollable then this will fail
        }
        TestUtils.clickText(mDevice, false, PBF_FILE, true);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        StorageDelegator delegator = App.getDelegator();
        Assert.assertNotNull(delegator.getOsmElement(Relation.NAME, 1252853L));
        Assert.assertNotNull(delegator.getOsmElement(Way.NAME,243055643L));
        Storage current = delegator.getCurrentStorage();
        Assert.assertEquals(3404, current.getNodes().size());
        Assert.assertEquals(391, current.getWays().size());
        Assert.assertEquals(76, current.getRelations().size());
    }
}
