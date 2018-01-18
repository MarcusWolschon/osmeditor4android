package de.blau.android.osm;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
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
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.SignalHandler;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BasicStuffTest {

    Context context = null;
    Main    main    = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        App.getDelegator().reset(false);
        App.getDelegator().setOriginalBox(ViewBox.getMaxMercatorExtent());
    }

    @After
    public void teardown() {
    }

    @Test
    public void setTags() {
        //
        Logic logic = App.getLogic();
        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedRelation(null);
        // nodes
        try {
            logic.performAdd(null, 100.0f, 100.0f);
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
        Node n = logic.getSelectedNode();
        Assert.assertNotNull(n);
        System.out.println(n);
        Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
        OsmElementFactory factory = App.getDelegator().getFactory();
        Node n2 = factory.createNodeWithNewId(0, 0);
        setTagsElement(logic, n, n2);
        // ways
        try {
            logic.performAdd(null, 150.0f, 150.0f);
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
        Way w = logic.getSelectedWay();
        Assert.assertNotNull(w);
        System.out.println(w);
        Assert.assertEquals(1, App.getDelegator().getApiWayCount());
        Way w2 = factory.createWayWithNewId(); // node-less way!
        setTagsElement(logic, w, w2);
        // FIXME do the same for relations
    }

    private void setTagsElement(Logic logic, OsmElement eInStorage, OsmElement eNotInStorage) {
        String key1 = "key1";
        String value1 = "value1";
        String key2 = "key2";
        String value2 = "value2";
        Assert.assertFalse(eInStorage.hasTags());
        Map<String, String> tags = new HashMap<String, String>();

        tags.put(key1, value1);
        // new form
        try {
            logic.setTags(main, eInStorage, tags);
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(eInStorage.hasTags());
        Assert.assertTrue(eInStorage.hasTagKey(key1));
        Assert.assertTrue(eInStorage.hasTag(key1, value1));
        Assert.assertEquals(value1, eInStorage.getTagWithKey(key1));
        SortedMap<String, String> m = eInStorage.getTags();
        try {
            m.put(key2, value2);
            Assert.fail("Map returned from getTags should be immutable");
        } catch (UnsupportedOperationException ex) {
            // just carry on
        }
        tags.clear();
        tags.putAll(m);
        tags.put(key2, value2);
        try {
            logic.setTags(main, eInStorage, tags);
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(eInStorage.hasTags());
        Assert.assertTrue(eInStorage.hasTagKey(key1));
        Assert.assertTrue(eInStorage.hasTag(key1, value1));
        Assert.assertEquals(value1, eInStorage.getTagWithKey(key1));
        Assert.assertTrue(eInStorage.hasTagKey(key2));
        Assert.assertTrue(eInStorage.hasTag(key2, value2));
        Assert.assertEquals(value2, eInStorage.getTagWithKey(key2));
        try {
            logic.setTags(main, eInStorage, null);
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertFalse(eInStorage.hasTags());
        // old form
        try {
            logic.setTags(main, eInStorage.getName(), eInStorage.getOsmId(), tags);
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(eInStorage.hasTags());
        Assert.assertTrue(eInStorage.hasTagKey(key1));
        Assert.assertTrue(eInStorage.hasTag(key1, value1));
        Assert.assertEquals(value1, eInStorage.getTagWithKey(key1));
        try {
            logic.setTags(main, eInStorage.getName(), eInStorage.getOsmId(), null);
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertFalse(eInStorage.hasTags());
        //

        try {
            logic.setTags(main, eNotInStorage, tags);
            Assert.fail("Element not in storage should fail");
        } catch (OsmIllegalOperationException e) {
            // carry on
        }
        try {
            logic.setTags(main, eNotInStorage.getName(), eNotInStorage.getOsmId(), tags);
            Assert.fail("Element not in storage should fail");
        } catch (OsmIllegalOperationException e) {
            // carry on
        }
    }

    @Test
    /**
     * Remove an end node from a closed way
     */
    public void deleteEndNodeFromClosedWay() {
        try {
            final CountDownLatch signal = new CountDownLatch(1);
            Logic logic = App.getLogic();

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream is = loader.getResourceAsStream("closedways.osm");
            logic.readOsmFile(main, is, false, new SignalHandler(signal));
            try {
                signal.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
            StorageDelegator delegator = App.getDelegator();
            // 2 node closed way, 1 node will silently get deleted when parsing
            Way way0 = (Way) delegator.getOsmElement(Way.NAME, 1547L);
            Assert.assertNotNull(way0);
            Assert.assertEquals(5, way0.getNodes().size());
            delegator.removeNode(way0.getFirstNode());
            Assert.assertNotNull(way0);
            Assert.assertEquals(OsmElement.STATE_MODIFIED, way0.getState());
            Assert.assertTrue(way0.isClosed());
            Assert.assertEquals(4, way0.getNodes().size());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }

    @Test
    /**
     * Remove a node from a degenerate (two node) way
     */
    public void deleteNodeFromDegenerateWay() {
        try {
            final CountDownLatch signal = new CountDownLatch(1);
            Logic logic = App.getLogic();

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream is = loader.getResourceAsStream("closedways.osm");
            logic.readOsmFile(main, is, false, new SignalHandler(signal));
            try {
                signal.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
            StorageDelegator delegator = App.getDelegator();
            // 2 node closed way, 1 node will silently get deleted when parsing
            Way way0 = (Way) delegator.getOsmElement(Way.NAME, 1544L);
            Assert.assertNotNull(way0);
            Assert.assertEquals(1, way0.getNodes().size());
            delegator.removeNode(way0.getFirstNode());
            Assert.assertNotNull(way0);
            Assert.assertEquals(OsmElement.STATE_DELETED, way0.getState());

            // 3(+2) node closed way
            Way way1 = (Way) delegator.getOsmElement(Way.NAME, 1545L);
            Assert.assertNotNull(way1);
            Assert.assertEquals(5, way1.getNodes().size());
            Node node1 = (Node) delegator.getOsmElement(Node.NAME, 296055259L);
            Assert.assertNotNull(node1);
            delegator.removeNode(node1);
            Assert.assertNotNull(way1);
            Assert.assertEquals(OsmElement.STATE_DELETED, way1.getState());
            Node node2 = (Node) delegator.getOsmElement(Node.NAME, 289987511L);
            Assert.assertNotNull(node2);
            Assert.assertEquals(OsmElement.STATE_UNCHANGED, node2.getState());

            // 5(+2) node closed way with 2x2 the same node
            Way way2 = (Way) delegator.getOsmElement(Way.NAME, 1546L);
            Assert.assertNotNull(way2);
            Assert.assertEquals(7, way2.getNodes().size());
            node1 = (Node) delegator.getOsmElement(Node.NAME, 296055272L);
            Assert.assertNotNull(node1);
            delegator.removeNode(node1);
            Assert.assertNotNull(way2);
            Assert.assertEquals(OsmElement.STATE_MODIFIED, way2.getState());
            Assert.assertEquals(4, way2.getNodes().size());
            delegator.removeNode(way2.getFirstNode());
            Assert.assertNotNull(way2);
            Assert.assertEquals(OsmElement.STATE_DELETED, way2.getState());
        } catch (Exception igit) {
            Assert.fail(igit.getMessage());
        }
    }
}
