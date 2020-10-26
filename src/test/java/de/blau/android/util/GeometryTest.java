package de.blau.android.util;

import static de.blau.android.osm.StorageDelegatorTest.toE7;
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
}