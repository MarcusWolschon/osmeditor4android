package de.blau.android.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.ShadowWorkManager;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

/**
 * Note: these test currently only test the filter logic not the UI
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class IndoorFilterTest {

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Robolectric.buildActivity(Main.class).create().resume();
    }

    /**
     * Test if a node is filtered correctly (without UI)
     */
    @Test
    public void indoorFilterNode() {
        TreeMap<String, String> tags = new TreeMap<>();
        tags.put(Tags.KEY_LEVEL, "" + 1);
        tags.put(Tags.KEY_REPEAT_ON, "" + 11);
        Logic logic = App.getLogic();
        Node n = logic.performAddNode(null, 1.0D, 1.0D);
        IndoorFilter f = new IndoorFilter();
        Assert.assertTrue(!f.include(n, false));
        f.clear();
        logic.setTags(null, n, tags);
        f.setLevel(9);
        Assert.assertTrue(f.include(n, true));
        logic.setSelectedNode(null);
        f.clear();
        logic.setTags(null, n, tags);
        f.setLevel(9);
        Assert.assertTrue(!f.include(n, false));
        f.clear();
        f.setLevel(1);
        Assert.assertTrue(f.include(n, false));
        f.clear();
        f.setLevel(11);
        Assert.assertTrue(f.include(n, false));
        List<OsmElement> members = new ArrayList<>();
        members.add(n);
        Relation r = logic.createRelation(null, "", members);
        f.clear();
        tags.clear();
        tags.put(Tags.KEY_MIN_LEVEL, "" + 8);
        tags.put(Tags.KEY_MAX_LEVEL, "" + 10);
        tags.put(Tags.KEY_BUILDING, "yes");
        logic.setTags(null, r, tags);
        f.setLevel(9);
        Assert.assertTrue(f.include(n, false));
    }

    /**
     * Test if a node is filtered correctly if the filter is inverted (without UI)
     */
    @Test
    public void indoorFilterNodeInverted() {
        TreeMap<String, String> tags = new TreeMap<>();
        tags.put(Tags.KEY_ENTRANCE, "yes");
        Logic logic = App.getLogic();
        Node n = logic.performAddNode(null, 1.0D, 1.0D);
        logic.setTags(null, n, tags);

        IndoorFilter f = new IndoorFilter();
        f.setInverted(true);
        f.setLevel(9);
        Assert.assertTrue(f.include(n, false));
    }

    /**
     * Test if a way is filtered correctly (without UI)
     */
    @Test
    public void indoorFilterWay() {
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_LEVEL, "" + 1);
            tags.put(Tags.KEY_REPEAT_ON, "" + 11);
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(null, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(null, w, tags);

            IndoorFilter f = new IndoorFilter();
            f.setLevel(9);
            Assert.assertTrue(!f.include(w, false));
            f.clear();
            f.setLevel(1);
            Assert.assertTrue(f.include(w, false));
            f.clear();
            f.setLevel(11);
            Assert.assertTrue(f.include(w, false));
            List<OsmElement> members = new ArrayList<>();
            members.add(w);
            Relation r = logic.createRelation(null, "", members);
            f.clear();
            tags.clear();
            tags.put(Tags.KEY_MIN_LEVEL, "" + 8);
            tags.put(Tags.KEY_MAX_LEVEL, "" + 10);
            tags.put(Tags.KEY_BUILDING, "yes");
            logic.setTags(null, r, tags);
            f.setLevel(9);
            Assert.assertTrue(f.include(w, false));
            // check way nodes
            Assert.assertTrue(f.include(n1, false));
            Assert.assertTrue(f.include(n2, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test if a way is filtered correctly when filter is inverted (without UI)
     */
    @Test
    public void indoorFilterWayInverted() {
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_ENTRANCE, "yes");
            Logic logic = App.getLogic();
            logic.performAdd(null, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(null, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(null, w, tags);

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

    /**
     * Test if a building relation is filtered correctly (without UI)
     */
    @Test
    public void indoorFilterBuildingRelation() {
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);
            logic.performAdd(null, 1000.0f, 1000.0f);
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.performAdd(null, 100.0f, 400.0f);
            logic.performAdd(null, 1000.0f, 1000.0f);
            Way w2 = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            tags.put(Tags.KEY_LEVEL, "" + 1);
            logic.setTags(null, w2, tags);

            IndoorFilter f = new IndoorFilter();
            List<OsmElement> members = new ArrayList<>();
            members.add(w);
            members.add(w2);
            Relation r = logic.createRelation(null, "", members);
            f.clear();
            f.setLevel(1);
            // check that relation without level does change member status
            Assert.assertFalse(f.include(r, false));
            Assert.assertFalse(f.include(w, false));
            Assert.assertTrue(f.include(w2, false));

            // now check inheritance from relation
            tags.clear();
            tags.put(Tags.KEY_MIN_LEVEL, "" + 8);
            tags.put(Tags.KEY_MAX_LEVEL, "" + 10);
            tags.put(Tags.KEY_BUILDING, "yes");
            logic.setTags(null, r, tags);
            f.setLevel(9);
            Assert.assertTrue(f.include(r, false));
            Assert.assertTrue(f.include(w, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test if a relation is filtered correctly (without UI)
     */
    @Test
    public void indoorFilterRelation() {
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);
            logic.performAdd(null, 1000.0f, 1000.0f);
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.performAdd(null, 100.0f, 400.0f);
            logic.performAdd(null, 1000.0f, 1000.0f);
            Way w2 = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            tags.put(Tags.KEY_LEVEL, "" + 1);
            logic.setTags(null, w2, tags);

            IndoorFilter f = new IndoorFilter();
            List<OsmElement> members = new ArrayList<>();
            members.add(w);
            members.add(w2);
            Relation r = logic.createRelation(null, "", members);
            f.clear();
            f.setLevel(1);

            // now check relation and inheritance from relation
            tags.clear();
            tags.put(Tags.KEY_LEVEL, "" + 1);
            logic.setTags(null, r, tags);
            Assert.assertTrue(f.include(r, true));
            Assert.assertTrue(f.include(w, true));
            f.setInverted(true);
            Assert.assertTrue(f.include(r, false));
            Assert.assertTrue(f.include(w, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }
}
