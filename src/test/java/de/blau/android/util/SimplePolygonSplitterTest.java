package de.blau.android.util;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}