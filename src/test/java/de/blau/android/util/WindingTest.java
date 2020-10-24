package de.blau.android.util;

import static de.blau.android.osm.StorageDelegatorTest.toE7;
import static de.blau.android.util.Winding.winding;
import static de.blau.android.util.Winding.COLINEAR;
import static de.blau.android.util.Winding.CLOCKWISE;
import static de.blau.android.util.Winding.COUNTERCLOCKWISE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.blau.android.App;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElementFactory;

public class WindingTest {

    /**
     * Check 3 nodes in a row
     */
    @Test
    public void colinear() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<Node> nodes = new ArrayList<>();
        Node n0 = factory.createNodeWithNewId(toE7(51.478D), toE7(0D));
        nodes.add(n0);
        Node n1 = factory.createNodeWithNewId(toE7(51.578D), toE7(0D));
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.678D), toE7(0D));
        nodes.add(n2);
        assertEquals(COLINEAR, winding(nodes));

        nodes.clear();
        n0 = factory.createNodeWithNewId(toE7(0D), toE7(51.478D));
        nodes.add(n0);
        n1 = factory.createNodeWithNewId(toE7(0D), toE7(51.578D));
        nodes.add(n1);
        n2 = factory.createNodeWithNewId(toE7(0D), toE7(51.678D));
        nodes.add(n2);
        assertEquals(COLINEAR, winding(nodes));

        nodes.clear();
        n0 = factory.createNodeWithNewId(toE7(51.478D), toE7(51.478D));
        nodes.add(n0);
        n1 = factory.createNodeWithNewId(toE7(51.578D), toE7(51.578D));
        nodes.add(n1);
        n2 = factory.createNodeWithNewId(toE7(51.678D), toE7(51.678D));
        nodes.add(n2);
        assertEquals(COLINEAR, winding(nodes));
    }

    /**
     * Check 3 nodes clockwise
     */
    @Test
    public void clockwise() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<Node> nodes = new ArrayList<>();
        Node n0 = factory.createNodeWithNewId(toE7(51.5019433D), toE7(-0.1423007D));
        nodes.add(n0);
        Node n1 = factory.createNodeWithNewId(toE7(51.5019019D), toE7(-0.1421827D));
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.5018651D), toE7(-0.1423209D));
        nodes.add(n2);
        assertEquals(CLOCKWISE, winding(nodes));
    }
    
    /**
     * Check 3 nodes counterclockwise
     */
    @Test
    public void counterclockwise() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<Node> nodes = new ArrayList<>();
        Node n0 = factory.createNodeWithNewId(toE7(51.5018651D), toE7(-0.1423209D));
        nodes.add(n0);
        Node n1 = factory.createNodeWithNewId(toE7(51.5019019D), toE7(-0.1421827D));
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.5019433D), toE7(-0.1423007D));
        nodes.add(n2);
        assertEquals(COUNTERCLOCKWISE, winding(nodes));
    }
}