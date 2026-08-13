package de.blau.android.util;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import de.blau.android.UnitTestUtils;
import de.blau.android.osm.BoundingBox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.*;

import org.junit.Test;

public class SimplePolygonSplitterTest {

    private Polygon square() {
        return Polygon.fromLngLats(Collections.singletonList(
                Arrays.asList(Point.fromLngLat(0, 0), Point.fromLngLat(10, 0), Point.fromLngLat(10, 10), Point.fromLngLat(0, 10), Point.fromLngLat(0, 0))));
    }

    @Test
    public void testVerticalSplitSquare() {
        List<Polygon> result = SimplePolygonSplitter.split(square(), 5.0, true);
        assertEquals(2, result.size());
    }

    @Test
    public void testHorizontalSplitSquare() {
        List<Polygon> result = SimplePolygonSplitter.split(square(), 5.0, true);
        assertEquals(2, result.size());
    }

    @Test
    public void testNoSplitOutside() {
        List<Polygon> result = SimplePolygonSplitter.split(square(), 20.0, true);

        // Should remain 1 polygon
        assertEquals(1, result.size());
    }

    @Test
    public void testConcavePolygonSplit() {

        Polygon concave = Polygon.fromLngLats(Collections.singletonList(Arrays.asList(Point.fromLngLat(0, 0), Point.fromLngLat(10, 0), Point.fromLngLat(10, 10),
                Point.fromLngLat(5, 5), Point.fromLngLat(0, 10), Point.fromLngLat(0, 0))));

        List<Polygon> result = SimplePolygonSplitter.split(concave, 5.0, true);

        assertTrue(result.size() >= 2);
    }

    @Test
    public void testSplitThroughVertex() {
        List<Polygon> result = SimplePolygonSplitter.split(square(), 0.0, true);

        // Edge-aligned split should not explode
        assertTrue(result.size() >= 1);
    }

    @Test
    public void testSplitProducesMultiplePolygonsOnOneSide() {

        // A concave "U"-shaped polygon
        Polygon poly = Polygon.fromLngLats(
                Collections.singletonList(Arrays.asList(Point.fromLngLat(0, 0), Point.fromLngLat(10, 0), Point.fromLngLat(10, 10), Point.fromLngLat(7, 10),
                        Point.fromLngLat(7, 3), Point.fromLngLat(3, 3), Point.fromLngLat(3, 10), Point.fromLngLat(0, 10), Point.fromLngLat(0, 0))));

        // Horizontal split through the "gap" of the U
        List<Polygon> result = SimplePolygonSplitter.split(poly, 5.0, false);

        // Expect:
        // - Bottom part → 1 polygon
        // - Top part → 2 separate polygons
        // Total = 3
        assertEquals(3, result.size());
    }

    /**
     * This polygon has a vertex that matches in the horizontal coord exactly the value of the split position creating an
     * issue with assembly of the resulting polygons. This tests that the fix for this works.
     */
    @Test
    public void testPolygonWithExactSplitPositionMatch() {
        try {
            Polygon polygon = Polygon.fromJson(UnitTestUtils.fileFromResourcesAsString(getClass(), "test-polygon.geojson"));
            BoundingBox box = GeoJson.getBounds(polygon);
            box.calcDimensions();

            double hCenter = box.getLeft() + box.getWidth() / 2D;

            final int maxDimE7 = 17996;

            final int maxDim2 = maxDimE7 * 2;

            double splitPosition = (box.getWidth() > maxDim2 ? box.getLeft() + maxDimE7 : hCenter) / 1E7D;

            List<Polygon> result = SimplePolygonSplitter.split(polygon, splitPosition, true);
            assertEquals(3, result.size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}