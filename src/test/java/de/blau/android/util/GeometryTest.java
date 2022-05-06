package de.blau.android.util;

import static de.blau.android.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.blau.android.App;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElementFactory;

public class GeometryTest {

    /**
     * Is node in polygon or not
     */
    @Test
    public void isInsideTest() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        Node[] nodes = new Node[4];
        Node n0 = factory.createNodeWithNewId(toE7(51.5019094D), toE7(-0.1417412D));
        nodes[0] = n0;
        Node n1 = factory.createNodeWithNewId(toE7(51.5016867D), toE7(-0.1410033D));
        nodes[1] = n1;
        Node n2 = factory.createNodeWithNewId(toE7(51.5011566D), toE7(-0.1413721D));
        nodes[2] = n2;
        Node n3 = factory.createNodeWithNewId(toE7(51.5013694D), toE7(-0.1421482D));
        nodes[3] = n3;

        Node inside1 = factory.createNodeWithNewId(toE7(51.50166452D), toE7(-0.1416078D));
        assertTrue(Geometry.isInside(nodes, inside1));

        assertTrue(Geometry.isInside(nodes, n2));

        Node outside1 = factory.createNodeWithNewId(toE7(51.5020192D), toE7(-0.1422925D));
        assertFalse(Geometry.isInside(nodes, outside1));

        Node outside2 = factory.createNodeWithNewId(toE7(51.5017543D), toE7(-0.1422112D));
        assertFalse(Geometry.isInside(nodes, outside2));
    }

    /**
     * Test offseting a geometry, the input data is rather trivial
     */
    @Test
    public void offsetTest() {
        float[] input = { -5f, 5f, 5f, 5f };
        Geometry.offset(input, input.length, false, 1);
        assertArrayEquals(new float[] { -5f, 4f, 5f, 4f }, input, 0.000001f);

        float[] input2 = { -5f, -5f, 5f, 5f };
        Geometry.offset(input2, input2.length, false, 1);
        assertArrayEquals(new float[] { -4.2928934f, -5.7071066f, 5.7071066f, 4.2928934f }, input2, 0.000001f);

        float[] input3 = { -5f, 5f, 5f, 5f, 5f, 5f, 5f, -5f, 5f, -5f, -5f, -5f, -5f, -5f, -5f, 5f };
        Geometry.offset(input3, input3.length, true, 1);
        assertArrayEquals(new float[] { -4f, 4f, 4f, 4f, 4f, 4f, 4f, -4f, 4f, -4f, -4f, -4f, -4f, -4f, -4f, 4f }, input3, 0.000001f);
    }
}