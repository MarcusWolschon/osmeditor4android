package io.vespucci.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.vespucci.exception.OsmException;
import io.vespucci.osm.GeoPoint;
import io.vespucci.osm.ViewBox;
import io.vespucci.util.GeoMath;
import io.vespucci.util.collections.FloatPrimitiveList;

public class GeoMathTest {

    /**
     * Test of point reduction at lower zoom levels
     */
    @Test
    public void squashPointsArray() {
        FloatPrimitiveList points = new FloatPrimitiveList();

        // short list
        GeoMath.squashPointsArray(points, 0.1f);
        assertEquals(0, points.size());
        for (int i = 0; i < 4; i++) {
            points.add(i);
        }
        GeoMath.squashPointsArray(points, 0.1f);
        assertEquals(4, points.size());

        // all to long
        points.clear();
        for (int i = 0; i < 12; i++) {
            points.add(i);
        }
        GeoMath.squashPointsArray(points, 0.1f);
        assertEquals(12, points.size());
        for (int i = 0; i < 12; i++) {
            assertEquals(i, points.get(i), 0.01);
        }

        // none adjacent
        GeoMath.squashPointsArray(points, 100);
        assertEquals(12, points.size());
        for (int i = 0; i < 12; i++) {
            assertEquals(i, points.get(i), 0.01);
        }

        // all adjacent
        points.clear();
        points.add(0);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(2);
        points.add(3);
        points.add(4);
        points.add(5);
        points.add(4);
        points.add(5);
        points.add(6);
        points.add(7);
        GeoMath.squashPointsArray(points, 100);
        assertEquals(4, points.size());
        assertEquals(0, points.get(0), 0.01);
        assertEquals(1, points.get(1), 0.01);
        assertEquals(6, points.get(2), 0.01);
        assertEquals(7, points.get(3), 0.01);

        // head+tail not adjacent
        points.clear();
        points.add(0);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(4);
        points.add(3);
        points.add(4);
        points.add(5);
        points.add(6);
        points.add(5);
        points.add(6);
        points.add(7);
        points.add(8);
        points.add(6);
        points.add(7);
        points.add(8);
        points.add(9);
        GeoMath.squashPointsArray(points, 100);
        assertEquals(12, points.size());
        assertEquals(0, points.get(0), 0.01);
        assertEquals(1, points.get(1), 0.01);
        assertEquals(2, points.get(2), 0.01);
        assertEquals(3, points.get(3), 0.01);
        assertEquals(1, points.get(4), 0.01);
        assertEquals(2, points.get(5), 0.01);
        assertEquals(7, points.get(6), 0.01);
        assertEquals(8, points.get(7), 0.01);
        assertEquals(6, points.get(8), 0.01);
        assertEquals(7, points.get(9), 0.01);
        assertEquals(8, points.get(10), 0.01);
        assertEquals(9, points.get(11), 0.01);

        // multiple segments
        points.clear();
        points.add(0);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(2);
        points.add(3);
        points.add(4);
        points.add(5);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(4);
        points.add(3);
        points.add(4);
        points.add(5);
        points.add(6);
        GeoMath.squashPointsArray(points, 100);
        assertEquals(8, points.size());
        assertEquals(0, points.get(0), 0.01);
        assertEquals(1, points.get(1), 0.01);
        assertEquals(4, points.get(2), 0.01);
        assertEquals(5, points.get(3), 0.01);
        assertEquals(1, points.get(4), 0.01);
        assertEquals(2, points.get(5), 0.01);
        assertEquals(5, points.get(6), 0.01);
        assertEquals(6, points.get(7), 0.01);

        // single line to long
        points.clear();
        points.add(0);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(2);
        points.add(3);
        points.add(0);
        points.add(1);
        points.add(0);
        points.add(1);
        points.add(7);
        points.add(8);
        GeoMath.squashPointsArray(points, 5);
        assertEquals(8, points.size());
        assertEquals(0, points.get(0), 0.01);
        assertEquals(1, points.get(1), 0.01);
        assertEquals(0, points.get(2), 0.01);
        assertEquals(1, points.get(3), 0.01);
        assertEquals(0, points.get(4), 0.01);
        assertEquals(1, points.get(5), 0.01);
        assertEquals(7, points.get(6), 0.01);
        assertEquals(8, points.get(7), 0.01);

        // sum to long
        points.clear();
        points.add(0);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(2);
        points.add(3);
        points.add(4);
        points.add(5);
        points.add(4);
        points.add(5);
        points.add(6);
        points.add(7);
        GeoMath.squashPointsArray(points, 6);
        assertEquals(8, points.size());
        assertEquals(0, points.get(0), 0.01);
        assertEquals(1, points.get(1), 0.01);
        assertEquals(4, points.get(2), 0.01);
        assertEquals(5, points.get(3), 0.01);
        assertEquals(4, points.get(4), 0.01);
        assertEquals(5, points.get(5), 0.01);
        assertEquals(6, points.get(6), 0.01);
        assertEquals(7, points.get(7), 0.01);

        // none squashed. hits all 'i != i0' conditions
        points.clear();
        points.add(0);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(1);
        points.add(2);
        points.add(3);
        points.add(4);
        points.add(2);
        points.add(3);
        points.add(16);
        points.add(17);
        points.add(4);
        points.add(5);
        points.add(6);
        points.add(7);
        GeoMath.squashPointsArray(points, 6);
        assertEquals(16, points.size());
        assertEquals(0, points.get(0), 0.01);
        assertEquals(1, points.get(1), 0.01);
        assertEquals(2, points.get(2), 0.01);
        assertEquals(3, points.get(3), 0.01);
        assertEquals(1, points.get(4), 0.01);
        assertEquals(2, points.get(5), 0.01);
        assertEquals(3, points.get(6), 0.01);
        assertEquals(4, points.get(7), 0.01);
        assertEquals(2, points.get(8), 0.01);
        assertEquals(3, points.get(9), 0.01);
        assertEquals(16, points.get(10), 0.01);
        assertEquals(17, points.get(11), 0.01);
        assertEquals(4, points.get(12), 0.01);
        assertEquals(5, points.get(13), 0.01);
        assertEquals(6, points.get(14), 0.01);
        assertEquals(7, points.get(15), 0.01);
    }

    /**
     * Silly test
     */
    @Test
    public void constants() {
        Assert.assertEquals(180d, GeoMath.MAX_MLAT, 0.000001); // NOSONAR
    }

    /**
     * Convert some values and back again
     */
    @Test
    public void mercator() {
        assertEquals(4865942.28D, GeoMath.latE7ToMercator((int) (40 * 1E7D)) * 2 * GeoMath.EARTH_RADIUS_EQUATOR / GeoMath._360_PI, 0.01);
        assertEquals(40 * 1E7D, GeoMath.mercatorToLatE7(4865942.28D * GeoMath._360_PI / (2 * GeoMath.EARTH_RADIUS_EQUATOR)), 0.1);
    }

    private class TestPoint implements GeoPoint {
        private final int lat;
        private final int lon;
        private final int n;

        public TestPoint(int n, int lat, int lon) {
            this.n = n;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public int getLat() {
            return lat;
        }

        @Override
        public int getLon() {
            return lon;
        }

        @Override
        public String toString() {
            return Integer.toString(n);
        }
    }

    @Test
    public void pointSorting() {
        List<TestPoint> points = new ArrayList<>();
        TestPoint p1 = new TestPoint(1, 0, 0);
        TestPoint p2 = new TestPoint(2, (int) (0.0005844 * 1E7D), (int) (-0.0005404 * 1E7D));
        TestPoint p3 = new TestPoint(3, (int) (0.0012702 * 1E7D), (int) (0.0006995 * 1E7D));
        TestPoint p4 = new TestPoint(4, (int) (-0.0015952 * 1E7D), (int) (0.0005973 * 1E7D));
        points.add(p3);
        points.add(p1);
        points.add(p2);
        points.add(p4);
        try {
            GeoMath.sortGeoPoint(p1, points,
                    new ViewBox((int) (-0.0024594 * 1E7D), (int) (-0.0032556 * 1E7D), (int) (0.0020165 * 1E7D), (int) (0.0029043 * 1E7D)), 1024, 1920);
        } catch (OsmException e) {
            fail(e.getMessage());
        }

        assertEquals(p1, points.get(0));
        assertEquals(p2, points.get(1));
        assertEquals(p3, points.get(2));
        assertEquals(p4, points.get(3));

        try {
            GeoMath.sortGeoPoint(p4, points,
                    new ViewBox((int) (-0.0024594 * 1E7D), (int) (-0.0032556 * 1E7D), (int) (0.0020165 * 1E7D), (int) (0.0029043 * 1E7D)), 1024, 1920);
        } catch (OsmException e) {
            fail(e.getMessage());
        }

        assertEquals(p4, points.get(0));
        assertEquals(p1, points.get(1));
        assertEquals(p2, points.get(2));
        assertEquals(p3, points.get(3));
    }
}
