package io.vespucci;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.Main;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Result;
import io.vespucci.osm.Way;
import io.vespucci.util.Util;

@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class LogicTest {

    private Main main;

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        main = Robolectric.setupActivity(Main.class);
        App.getDelegator().reset(true);
    }

    /**
     * Test if node gets merged to the correct location
     * 
     * @see https://github.com/MarcusWolschon/osmeditor4android/issues/1714
     */
    @Test
    public void panhandle() {
        UnitTestUtils.loadTestData(getClass(), "panhandle.osm");
        Way pan = (Way) App.getDelegator().getOsmElement(Way.NAME, -1);
        assertNotNull(pan);
        main.zoomTo(pan);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, -8);
        App.getLogic().performJoinNodeToWays(null, Util.wrapInList(pan), n);
        assertEquals(5, pan.getNodes().indexOf(n));
    }

    /**
     * Test if node gets merged and additional Node is ignored
     * 
     */
    @Test
    public void mergeNodeToWay() {
        UnitTestUtils.loadTestData(getClass(), "join_to_way.osm");
        Way way = (Way) App.getDelegator().getOsmElement(Way.NAME, -1);
        assertNotNull(way);
        main.zoomTo(way);
        Node n1 = (Node) App.getDelegator().getOsmElement(Node.NAME, -3);
        assertNotNull(n1);
        Node n2 = (Node) App.getDelegator().getOsmElement(Node.NAME, -4);
        assertNotNull(n2);
        List<OsmElement> list = new ArrayList<>();
        list.add(way);
        list.add(n2);
        App.getLogic().performJoinNodeToWays(null, list, n1);
        assertEquals(1, way.getNodes().indexOf(n1));
    }

    /**
     * Replace way geometry by new one, this should simply adjust the position of the three nodes and delete the fourth
     * one
     */
    @Test
    public void replaceGeometry1() {
        assertTrue(replaceGeometry("replace_geometry.osm", -1, -2).isEmpty());
    }

    /**
     * Replace way geometry by new one, this should simply adjust the position of the four existing nodes and add two
     * more
     */
    @Test
    public void replaceGeometry2() {
        assertTrue(replaceGeometry("replace_geometry.osm", -1, -3).isEmpty());
    }

    /**
     * Replace way geometry by new one, this should simply adjust the position of the four existing nodes and add two
     * more
     */
    @Test
    public void replaceGeometry3() {
        List<Result> result = replaceGeometry("replace_geometry2.osm", -1, -3);
        assertEquals(1, result.size());
        Result r = result.get(0);
        assertEquals(-14, r.getElement().getOsmId());
        assertTrue(App.getLogic().getWaysForNode((Node) r.getElement()).isEmpty());
    }

    /**
     * Replace way geometry by new one
     */
    private List<Result> replaceGeometry(@NonNull String input, int targetId, int sourceId) {
        UnitTestUtils.loadTestData(getClass(), input);
        Way target = (Way) App.getDelegator().getOsmElement(Way.NAME, targetId);
        assertNotNull(target);
        Way source = (Way) App.getDelegator().getOsmElement(Way.NAME, sourceId);
        assertNotNull(source);
        List<Result> result = App.getLogic().performReplaceGeometry(null, target, source.getNodes());
        assertEquals(target.nodeCount(), source.nodeCount());
        List<Node> sourceNodes = source.getNodes();
        List<Node> targetNodes = target.getNodes();
        for (int i = 0; i < source.nodeCount(); i++) {
            Node s = sourceNodes.get(i);
            Node t = targetNodes.get(i);
            assertEquals(s.getLon(), t.getLon());
            assertEquals(s.getLat(), t.getLat());
        }
        return result;
    }
}
