package de.blau.android.util;

import static de.blau.android.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.util.Log;
import androidx.test.core.app.ApplicationProvider;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.gpx.Track;
import de.blau.android.gpx.TrackPoint;
import de.blau.android.gpx.WayPoint;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
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
        float[] output = new float[4];
        Geometry.offset(input, output, input.length, false, 1);
        assertArrayEquals(new float[] { -5f, 4f, 5f, 4f }, output, 0.000001f);

        float[] input2 = { -5f, -5f, 5f, 5f };
        float[] output2 = new float[4];
        Geometry.offset(input2, output2, input2.length, false, 1);
        assertArrayEquals(new float[] { -4.2928934f, -5.7071066f, 5.7071066f, 4.2928934f }, output2, 0.000001f);

        float[] input3 = { -5f, 5f, 5f, 5f, 5f, 5f, 5f, -5f, 5f, -5f, -5f, -5f, -5f, -5f, -5f, 5f };
        float[] output3 = new float[16];
        Geometry.offset(input3, output3, input3.length, true, 1);
        assertArrayEquals(new float[] { -4f, 4f, 4f, 4f, 4f, 4f, 4f, -4f, 4f, -4f, -4f, -4f, -4f, -4f, -4f, 4f }, output3, 0.000001f);
    }

    /**
     * Test that calculating the centroid of degenerate ways does what is expected
     */
    @Test
    public void centroidDegenerateWays() {
        final StorageDelegator delegator = App.getDelegator();
        OsmElementFactory factory = delegator.getFactory();
        Way w = factory.createWayWithNewId();

        double[] centroid = Geometry.centroidLonLat(w);
        assertEquals(0, centroid.length);
        Node n0 = factory.createNodeWithNewId(toE7(51.5019094D), toE7(-0.1417412D));
        delegator.addNodeToWay(n0, w);
        centroid = Geometry.centroidLonLat(w);
        assertEquals(2, centroid.length);
        assertEquals(centroid[0], -0.1417412D, 0.0000001);
        assertEquals(centroid[1], 51.5019094D, 0.0000001);
    }

    /**
     * Calculate the centroid of a list of screen coordinates of a polygon
     */
    @Test
    public void centroidFromPointlist() {
        float[] pointlist = new float[] { 2, 2, 2, 0, 0, 0, 0, 2 };
        Coordinates c = Geometry.centroidFromPointlist(pointlist, pointlist.length, new Coordinates(0, 0));
        assertEquals(1D, c.x, 0.000001);
        assertEquals(1D, c.y, 0.000001);
    }

    /**
     * Calculate the midpoint of a list of screen coordinates of a linestring
     */
    @Test
    public void midpointFromPointlist() {
        float[] pointlist = new float[] { -1, 0, 0, 1, 2, -1, 3, 0 };
        Coordinates c = Geometry.midpointFromPointlist(pointlist, pointlist.length, new Coordinates(0, 0));
        assertEquals(1D, c.x, 0.000001);
        assertEquals(0D, c.y, 0.000001);
    }

    @Test
    public void simplify() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<Node> nodes = new ArrayList<>();
        Node n0 = factory.createNodeWithNewId(toE7(47.4363893D), toE7(8.2119496D));
        nodes.add(n0);
        Node n1 = factory.createNodeWithNewId(toE7(47.4363848D), toE7(8.2119805D));
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(47.4363719D), toE7(8.2119926D));
        nodes.add(n2);
        Node n3 = factory.createNodeWithNewId(toE7(47.4363915D), toE7(8.2120353D));
        nodes.add(n3);
        Node n4 = factory.createNodeWithNewId(toE7(47.4363544D), toE7(8.2120104D));
        nodes.add(n4);
        Way w = factory.createWayWithNewId();
        w.getNodes().addAll(nodes);
        List<Node> simplified = Geometry.simplify(nodes, 2.0);
        assertTrue(simplified.contains(n0));
        assertFalse(simplified.contains(n1));
        assertTrue(simplified.contains(n2));
        assertTrue(simplified.contains(n3));
        assertTrue(simplified.contains(n4));
    }

    @Test
    public void simplify2() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<Node> nodes = new ArrayList<>();
        Node n0 = factory.createNodeWithNewId(toE7(47.4363893D), toE7(8.2119496D));
        nodes.add(n0);
        Node n1 = factory.createNodeWithNewId(toE7(47.4363848D), toE7(8.2119805D));
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(47.4363719D), toE7(8.2119926D));
        nodes.add(n2);
        Node n3 = factory.createNodeWithNewId(toE7(47.4363915D), toE7(8.2120353D));
        nodes.add(n3);
        Node n4 = factory.createNodeWithNewId(toE7(47.4363544D), toE7(8.2120104D));
        nodes.add(n4);
        Way w = factory.createWayWithNewId();
        w.getNodes().addAll(nodes);
        List<Node> simplified = Geometry.simplify(nodes, 7.0);
        assertTrue(simplified.contains(n0));
        assertFalse(simplified.contains(n1));
        assertFalse(simplified.contains(n2));
        assertFalse(simplified.contains(n3));
        assertTrue(simplified.contains(n4));
    }

    @Test
    public void simplify3() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<Node> nodes = new ArrayList<>();
        Node n0 = factory.createNodeWithNewId(toE7(47.4363893D), toE7(8.2119496D));
        nodes.add(n0);
        Node n1 = factory.createNodeWithNewId(toE7(47.4363848D), toE7(8.2119805D));
        Map<String, String> tags = new TreeMap<>();
        tags.put("test", "test");
        App.getDelegator().setTags(n1, tags);
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(47.4363719D), toE7(8.2119926D));
        nodes.add(n2);
        Node n3 = factory.createNodeWithNewId(toE7(47.4363915D), toE7(8.2120353D));
        nodes.add(n3);
        Node n4 = factory.createNodeWithNewId(toE7(47.4363544D), toE7(8.2120104D));
        nodes.add(n4);
        Way w = factory.createWayWithNewId();
        w.getNodes().addAll(nodes);
        List<Node> simplified = Geometry.simplify(nodes, 2.0);
        assertTrue(simplified.contains(n0));
        assertTrue(simplified.contains(n1));
        assertTrue(simplified.contains(n2));
        assertTrue(simplified.contains(n3));
        assertTrue(simplified.contains(n4));
    }

    @Test
    public void simplify4() {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<Node> nodes = new ArrayList<>();
        Node n0 = factory.createNodeWithNewId(toE7(47.4363893D), toE7(8.2119496D));
        nodes.add(n0);
        Node n1 = factory.createNodeWithNewId(toE7(47.4363848D), toE7(8.2119805D));
        Map<String, String> tags = new TreeMap<>();
        tags.put("test", "test");
        App.getDelegator().setTags(n1, tags);
        nodes.add(n1);
        Node n2 = factory.createNodeWithNewId(toE7(47.4363719D), toE7(8.2119926D));
        nodes.add(n2);
        Node n3 = factory.createNodeWithNewId(toE7(47.4363915D), toE7(8.2120353D));
        nodes.add(n3);
        Node n4 = factory.createNodeWithNewId(toE7(47.4363544D), toE7(8.2120104D));
        nodes.add(n4);
        Way w = factory.createWayWithNewId();
        w.getNodes().addAll(nodes);
        List<Node> simplified = Geometry.simplify(nodes, 7.0);
        assertTrue(simplified.contains(n0));
        assertTrue(simplified.contains(n1));
        assertFalse(simplified.contains(n2));
        assertFalse(simplified.contains(n3));
        assertTrue(simplified.contains(n4));
    }

    @Test
    public void radialDistanceFilter() {
        try {
            File jitterFile = JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "jitter-short.gpx", null, "/");
            Track track = new Track(ApplicationProvider.getApplicationContext(), false);
            try (InputStream is = new FileInputStream(jitterFile); BufferedInputStream in = new BufferedInputStream(is)) {
                track.importFromGPX(in);
                final List<TrackPoint> trackPoints = track.getTrackPoints();
                final int size = trackPoints.size();
                assertEquals(29, size);
                List<Coordinates> coords = new ArrayList<>(size);
                for (GeoPoint p : trackPoints) {
                    coords.add(new Coordinates(GeoMath.lonE7ToMercatorMeters(p.getLon()), GeoMath.latE7ToMercatorMeters(p.getLat())));
                }
                List<TrackPoint> filtered = Geometry.radialDistanceFilter(trackPoints, coords, 1.0);
                assertEquals(9, filtered.size());
                assertEquals(trackPoints.get(0), filtered.get(0));
                assertEquals(trackPoints.get(size - 1), filtered.get(filtered.size() - 1));
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}