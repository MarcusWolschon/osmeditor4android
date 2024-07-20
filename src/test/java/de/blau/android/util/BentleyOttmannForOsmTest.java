package de.blau.android.util;

import static de.blau.android.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class BentleyOttmannForOsmTest {

    /**
     * Two ways that should intersect twice
     */
    @Test
    public void twoWays() {
        StorageDelegator d = App.getDelegator();
        OsmElementFactory factory = d.getFactory();
        Way w1 = factory.createWayWithNewId();
        Node n0 = factory.createNodeWithNewId(toE7(51.6525286), toE7(-0.0399686));
        d.addNodeToWay(n0, w1);
        Node n1 = factory.createNodeWithNewId(toE7(51.6526058), toE7(-0.0398883));
        d.addNodeToWay(n1, w1);
        Node n2 = factory.createNodeWithNewId(toE7(51.6525050), toE7(-0.0397415));
        d.addNodeToWay(n2, w1);

        Way w2 = factory.createWayWithNewId();
        Node n3 = factory.createNodeWithNewId(toE7(51.6525969), toE7(-0.0399711));
        d.addNodeToWay(n3, w2);
        Node n4 = factory.createNodeWithNewId(toE7(51.6525101), toE7(-0.0398883));
        d.addNodeToWay(n4, w2);
        Node n5 = factory.createNodeWithNewId(toE7(51.6525912), toE7(-0.0397671));
        d.addNodeToWay(n5, w2);

        List<Coordinates> intersections = BentleyOttmannForOsm.findIntersections(Arrays.asList(w1, w2));
        for (Coordinates c:intersections) {
            System.out.println(c);
        }
        assertEquals(2, intersections.size());
    }
    
    /**
     * Two ways with common start node should intersect once
     */
    @Test
    public void twoWaysWithCommonStart() {
        StorageDelegator d = App.getDelegator();
        OsmElementFactory factory = d.getFactory();
        Way w1 = factory.createWayWithNewId();
        Node n0 = factory.createNodeWithNewId(toE7(51.6525286), toE7(-0.0399686));
        d.addNodeToWay(n0, w1);
        Node n1 = factory.createNodeWithNewId(toE7(51.6526058), toE7(-0.0398883));
        d.addNodeToWay(n1, w1);
        Node n2 = factory.createNodeWithNewId(toE7(51.6525050), toE7(-0.0397415));
        d.addNodeToWay(n2, w1);

        Way w2 = factory.createWayWithNewId();
        d.addNodeToWay(n0, w2);
        Node n4 = factory.createNodeWithNewId(toE7(51.6525101), toE7(-0.0398883));
        d.addNodeToWay(n4, w2);
        Node n5 = factory.createNodeWithNewId(toE7(51.6525912), toE7(-0.0397671));
        d.addNodeToWay(n5, w2);

        List<Coordinates> intersections = BentleyOttmannForOsm.findIntersections(Arrays.asList(w1, w2));
       
        assertEquals(1, intersections.size());
    }
}