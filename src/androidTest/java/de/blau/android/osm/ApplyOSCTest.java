package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplyOSCTest {

    private static final String OSC_FILE = "osctest1.osc";
    Context                     context  = null;
    AdvancedPrefDatabase        prefDB   = null;
    Main                        main     = null;
    UiDevice                    mDevice  = null;
    Logic                       logic    = null;

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
            TestUtils.copyFileFromResources(OSC_FILE, ".");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        App.getDelegator().reset(false);
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        // load some base data
        final CountDownLatch signal1 = new CountDownLatch(1);
        logic = App.getLogic();
        logic.deselectAll();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("osctest1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
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
     * Read an OSC file on existing data and do some superficial checks that that was successful
     */
    @Test
    public void readAndApply() {
        StorageDelegator delegator = App.getDelegator();

        // check initial data state
        Assert.assertNotNull(delegator.getOsmElement(Way.NAME, 210558043L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392996L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392994L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392992L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392993L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392996L));

        Way w = (Way) delegator.getOsmElement(Way.NAME, 210558045L);
        Assert.assertNotNull(w);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, w.getState());
        Assert.assertTrue(w.hasTag("addr:housenumber", "4"));

        Node n = (Node) delegator.getOsmElement(Node.NAME, 416426220L);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        System.out.println("Lat " + n.getLat());
        Assert.assertEquals(47.3898033D, n.getLat() / 1E7D, 0.00000001);
        Assert.assertEquals(8.3888382D, n.getLon() / 1E7D, 0.00000001);

        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 1638705L);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        Assert.assertNotNull(r.getMember(Way.NAME, 119104094L));

        // apply OSC file
        TestUtils.clickMenuButton("Transfer", false, false);
        TestUtils.clickText(mDevice, false, "File", false);
        TestUtils.clickText(mDevice, false, "Apply changes from OSC file", false);
        //
        TestUtils.selectFile(mDevice, OSC_FILE);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }

        // check new data state
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Way.NAME, 210558043L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Way.NAME, 210558043L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392994L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392994L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392992L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392992L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392993L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392993L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L).getState());

        // placeholder ids are renumbered on input so we need to find the Way some other way
        List<Way> ways = delegator.getCurrentStorage().getWays(new BoundingBox(8.3891745, 47.3899902, 8.3894486, 47.3901275));
        for (Way way : ways) {
            if (way.getOsmId() < 0) {
                w = way;
            }
        }
        Assert.assertNotNull(w);
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Way.NAME, w.getOsmId()));
        Assert.assertEquals(OsmElement.STATE_CREATED, w.getState());
        Assert.assertEquals(4, w.getNodes().size());
        Assert.assertTrue(w.isClosed());
        Assert.assertTrue(w.hasTag("name", "new"));

        w = (Way) delegator.getApiStorage().getOsmElement(Way.NAME, 210558045L);
        Assert.assertNotNull(w);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, w.getState());
        Assert.assertTrue(w.hasTag("addr:housenumber", "444"));

        n = (Node) delegator.getApiStorage().getOsmElement(Node.NAME, 416426220L);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        Assert.assertEquals(47.3898126D, n.getLat() / 1E7D, 0.00000001);
        Assert.assertEquals(8.3894851D, n.getLon() / 1E7D, 0.00000001);

        r = (Relation) delegator.getOsmElement(Relation.NAME, 1638705L);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        Assert.assertNull(r.getMember(Way.NAME, 119104094L));
    }
}
