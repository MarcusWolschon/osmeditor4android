package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.util.Util;

@Config(shadows = { ShadowWorkManager.class }, sdk=33)
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
}
