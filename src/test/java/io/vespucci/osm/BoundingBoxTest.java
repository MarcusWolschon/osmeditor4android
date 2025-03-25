package io.vespucci.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.vespucci.osm.BoundingBox;

public class BoundingBoxTest {

    /**
     * Tests for BoundingBox methods
     */
    @Test
    public void boundingBox() {
        BoundingBox existingBox = new BoundingBox(-10.0, -10.0, 10.0, 10.0);

        ArrayList<BoundingBox> existing = new ArrayList<BoundingBox>();
        existing.add(existingBox);

        BoundingBox newBox = new BoundingBox(0.0, 0.0, 20.0, 20.0);
        List<BoundingBox> result = BoundingBox.newBoxes(existing, newBox);
        assertEquals(2, result.size());

        newBox = new BoundingBox(0.0, 0.0, 5.0, 20.0);
        result = BoundingBox.newBoxes(existing, newBox);
        assertEquals(1, result.size());

        newBox = new BoundingBox(0.0, 0.0, 5.0, 10.0);
        result = BoundingBox.newBoxes(existing, newBox);
        assertEquals(0, result.size());

        newBox = new BoundingBox(-15.0, -15.0, 15.0, 15.0);
        result = BoundingBox.newBoxes(existing, newBox);
        assertEquals(4, result.size());

        newBox = new BoundingBox(20.0, 20.0, 25.0, 25.0);
        result = BoundingBox.newBoxes(existing, newBox);
        assertEquals(1, result.size());

        // new bounding box ops
        newBox = new BoundingBox(20.0, 20.0, 25.0, 25.0);
        BoundingBox node = new BoundingBox((int) (30 * 1E7), (int) (30 * 1E7));
        newBox.union(node);
        assertEquals((int) (20 * 1E7), newBox.getLeft());
        assertEquals((int) (20 * 1E7), newBox.getBottom());
        assertEquals((int) (30 * 1E7), newBox.getTop());
        assertEquals((int) (30 * 1E7), newBox.getRight());
        node = new BoundingBox((int) (15 * 1E7), (int) (15 * 1E7));
        newBox.union(node);
        assertEquals((int) (15 * 1E7), newBox.getLeft());
        assertEquals((int) (15 * 1E7), newBox.getBottom());
        assertEquals((int) (30 * 1E7), newBox.getTop());
        assertEquals((int) (30 * 1E7), newBox.getRight());
        BoundingBox newBox2 = new BoundingBox(10.0, 10.0, 25.0, 25.0);
        assertTrue(newBox2.intersects(newBox));
        newBox2 = new BoundingBox(25.0, 25.0, 40.0, 40.0);
        assertTrue(newBox2.intersects(newBox));
        newBox2 = new BoundingBox(10.0, 25.0, 20.0, 40.0);
        assertTrue(newBox2.intersects(newBox));

        assertTrue(BoundingBox.intersects(newBox2, newBox));
        BoundingBox newBox3 = new BoundingBox(10.0, 10.0, 12.0, 12.0);
        assertTrue(!newBox3.intersects(newBox));
        assertTrue(!BoundingBox.intersects(newBox3, newBox));

        BoundingBox newBox4 = new BoundingBox(15.0, 20.0, 25.0, 30.0);

        assertTrue(BoundingBox.intersects(newBox2, newBox4));
        newBox2.intersection(newBox4);
        assertEquals((int) (15 * 1E7), newBox2.getLeft());
        assertEquals((int) (25 * 1E7), newBox2.getBottom());
        assertEquals((int) (20 * 1E7), newBox2.getRight());
        assertEquals((int) (30 * 1E7), newBox2.getTop());

        BoundingBox newBox5 = new BoundingBox(-179.999989, -85.412032, 179.999989, 89.000000);
        assertEquals("-179.9999890, -85.4120320, 179.9999890, 89.0000000", newBox5.toPrettyString());
    }

    /**
     * Create a too large BB and then validate
     */
    @Test
    public void checkAndMakeValid() {
        BoundingBox box = new BoundingBox(-2500000, -3000000, 2500000, 3000000);
        assertFalse(box.isValidForApi(0.25f));
        box.makeValidForApi(0.25f);
        assertTrue(box.isValidForApi(0.25f));
    }
}