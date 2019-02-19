package de.blau.android.osm;

import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.filter.IndoorFilter;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 * Note: these test currently only test the filter logic not the UI
 * 
 * @author simon
 *
 */
public class IndoorFilterTest {

    MockWebServerPlus    mockServer = null;
    Context              context    = null;
    AdvancedPrefDatabase prefDB     = null;
    Main                 main       = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(context);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
    }

    @Test
    public void indoorFilterNode() {
        TreeMap<String, String> tags = new TreeMap<String, String>();
        tags.put(Tags.KEY_LEVEL, "" + 1);
        tags.put(Tags.KEY_REPEAT_ON, "" + 11);
        Logic logic = App.getLogic();
        Node n = logic.performAddNode(main, 1.0D, 1.0D);
        IndoorFilter f = new IndoorFilter();
        Assert.assertTrue(!f.include(n, false));
        f.clear();
        n.setTags(tags);
        f.setLevel(9);
        Assert.assertTrue(f.include(n, true));
        logic.setSelectedNode(null);
        f.clear();
        n.setTags(tags);
        f.setLevel(9);
        Assert.assertTrue(!f.include(n, false));
        f.clear();
        f.setLevel(1);
        Assert.assertTrue(f.include(n, false));
        f.clear();
        f.setLevel(11);
        Assert.assertTrue(f.include(n, false));
        ArrayList<OsmElement> members = new ArrayList<OsmElement>();
        members.add(n);
        Relation r = logic.createRelation(main, "", members);
        f.clear();
        tags.clear();
        tags.put(Tags.KEY_MIN_LEVEL, "" + 8);
        tags.put(Tags.KEY_MAX_LEVEL, "" + 10);
        tags.put(Tags.KEY_BUILDING, "yes");
        r.addTags(tags);
        f.setLevel(9);
        Assert.assertTrue(f.include(n, false));
    }

    @Test
    public void indoorFilterNodeInverted() {
        TreeMap<String, String> tags = new TreeMap<String, String>();
        tags.put(Tags.KEY_ENTRANCE, "yes");
        Logic logic = App.getLogic();
        Node n = logic.performAddNode(main, 1.0D, 1.0D);
        n.setTags(tags);

        IndoorFilter f = new IndoorFilter();
        f.setInverted(true);
        f.setLevel(9);
        Assert.assertTrue(f.include(n, false));
    }

    @Test
    public void indoorFilterWay() {
        try {
            TreeMap<String, String> tags = new TreeMap<String, String>();
            tags.put(Tags.KEY_LEVEL, "" + 1);
            tags.put(Tags.KEY_REPEAT_ON, "" + 11);
            Logic logic = App.getLogic();

            logic.performAdd(main, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(main, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            w.setTags(tags);

            IndoorFilter f = new IndoorFilter();
            f.setLevel(9);
            Assert.assertTrue(!f.include(w, false));
            f.clear();
            f.setLevel(1);
            Assert.assertTrue(f.include(w, false));
            f.clear();
            f.setLevel(11);
            Assert.assertTrue(f.include(w, false));
            ArrayList<OsmElement> members = new ArrayList<OsmElement>();
            members.add(w);
            Relation r = logic.createRelation(main, "", members);
            f.clear();
            tags.clear();
            tags.put(Tags.KEY_MIN_LEVEL, "" + 8);
            tags.put(Tags.KEY_MAX_LEVEL, "" + 10);
            tags.put(Tags.KEY_BUILDING, "yes");
            r.addTags(tags);
            f.setLevel(9);
            Assert.assertTrue(f.include(w, false));
            // check way nodes
            Assert.assertTrue(f.include(n1, false));
            Assert.assertTrue(f.include(n2, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void indoorFilterWayInverted() {
        try {
            TreeMap<String, String> tags = new TreeMap<String, String>();
            tags.put(Tags.KEY_ENTRANCE, "yes");
            Logic logic = App.getLogic();
            logic.performAdd(main, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(main, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            w.setTags(tags);

            IndoorFilter f = new IndoorFilter();
            f.setInverted(true);
            f.setLevel(9);
            Assert.assertTrue(f.include(w, false));
            // check way nodes
            Assert.assertTrue(f.include(n1, false));
            Assert.assertTrue(f.include(n2, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void indoorFilterRelation() {
        try {
            TreeMap<String, String> tags = new TreeMap<String, String>();
            Logic logic = App.getLogic();

            logic.performAdd(main, 100.0f, 100.0f);
            logic.performAdd(main, 1000.0f, 1000.0f);
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.performAdd(main, 100.0f, 400.0f);
            logic.performAdd(main, 1000.0f, 1000.0f);
            Way w2 = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            tags.put(Tags.KEY_LEVEL, "" + 1);
            w2.addTags(tags);

            IndoorFilter f = new IndoorFilter();
            ArrayList<OsmElement> members = new ArrayList<OsmElement>();
            members.add(w);
            members.add(w2);
            Relation r = logic.createRelation(main, "", members);
            f.clear();
            f.setLevel(1);
            // check that relation without level doesn't change member status
            Assert.assertFalse(f.include(r, false));
            Assert.assertFalse(f.include(w, false));
            Assert.assertTrue(f.include(w2, false));

            // now check inheritance from relation
            tags.clear();
            tags.put(Tags.KEY_MIN_LEVEL, "" + 8);
            tags.put(Tags.KEY_MAX_LEVEL, "" + 10);
            tags.put(Tags.KEY_BUILDING, "yes");
            r.addTags(tags);
            f.setLevel(9);
            Assert.assertTrue(f.include(r, false));
            Assert.assertTrue(f.include(w, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }
}
