package de.blau.android.util;

import org.junit.Test;

import de.blau.android.util.collections.FloatPrimitiveList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeoMathTest {

    @Test
    public void squashPointsArray() {
        FloatPrimitiveList points = new FloatPrimitiveList();

        // short list
        GeoMath.squashPointsArray(points, 0.1f);
        assertEquals(0, points.size());
        for(int i=0; i<4; i++)
            points.add(i);
        GeoMath.squashPointsArray(points, 0.1f);
        assertEquals(4, points.size());

        // all to long
        points.clear();
        for(int i=0; i<12; i++)
            points.add(i);
        GeoMath.squashPointsArray(points, 0.1f);
        assertEquals(12, points.size());
        for(int i=0; i<12; i++)
            assertEquals(i, points.get(i), 0.01);

        // none adjacent
        GeoMath.squashPointsArray(points, 100);
        assertEquals(12, points.size());
        for(int i=0; i<12; i++)
            assertEquals(i, points.get(i), 0.01);

        // all adjacent
        points.clear();
        points.add(0); points.add(1); points.add(2); points.add(3);
        points.add(2); points.add(3); points.add(4); points.add(5);
        points.add(4); points.add(5); points.add(6); points.add(7);
        GeoMath.squashPointsArray(points, 100);
        assertEquals(4, points.size());
        assertEquals(0, points.get(0), 0.01);
        assertEquals(1, points.get(1), 0.01);
        assertEquals(6, points.get(2), 0.01);
        assertEquals(7, points.get(3), 0.01);

        // head+tail not adjacent
        points.clear();
        points.add(0); points.add(1); points.add(2); points.add(3);
        points.add(1); points.add(2); points.add(3); points.add(4);
        points.add(3); points.add(4); points.add(5); points.add(6);
        points.add(5); points.add(6); points.add(7); points.add(8);
        points.add(6); points.add(7); points.add(8); points.add(9);
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
        points.add(0); points.add(1); points.add(2); points.add(3);
        points.add(2); points.add(3); points.add(4); points.add(5);
        points.add(1); points.add(2); points.add(3); points.add(4);
        points.add(3); points.add(4); points.add(5); points.add(6);
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
        points.add(0); points.add(1); points.add(2); points.add(3);
        points.add(2); points.add(3); points.add(0); points.add(1);
        points.add(0); points.add(1); points.add(7); points.add(8);
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
        points.add(0); points.add(1); points.add(2); points.add(3);
        points.add(2); points.add(3); points.add(4); points.add(5);
        points.add(4); points.add(5); points.add(6); points.add(7);
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
        points.add(0); points.add(1); points.add(2); points.add(3);
        points.add(1); points.add(2); points.add(3); points.add(4);
        points.add(2); points.add(3); points.add(16); points.add(17);
        points.add(4); points.add(5); points.add(6); points.add(7);
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
}
