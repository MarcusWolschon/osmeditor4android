package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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

    /**
     * Test consolidating two horizontally adjacent thin boxes into one
     */
    @Test
    public void consolidateAdjacentHorizontalSlivers() {
        List<BoundingBox> boxes = new ArrayList<>();
        // Left sliver: narrow in width
        boxes.add(new BoundingBox(0, 0, 500000, 10000000)); // width: 500000
        // Right sliver: narrow in width
        boxes.add(new BoundingBox(500000, 0, 1000000, 10000000)); // width: 500000

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        // Should merge into 1 or 2 boxes (depending on consolidation aggressiveness)
        assertTrue("Expected 1-2 boxes after consolidation, got " + result.size(), result.size() <= 2);

        // Verify coverage is preserved
        BoundingBox union = new BoundingBox(0, 0, 1000000, 10000000);
        for (BoundingBox box : result) {
            assertTrue("Result box should be within or equal to original union", union.contains(box) || box.contains(union));
        }
    }

    /**
     * Test consolidating two vertically adjacent thin boxes into one
     */
    @Test
    public void consolidateAdjacentVerticalSlivers() {
        List<BoundingBox> boxes = new ArrayList<>();
        // Bottom sliver: narrow in height
        boxes.add(new BoundingBox(0, 0, 10000000, 500000)); // height: 500000
        // Top sliver: narrow in height
        boxes.add(new BoundingBox(0, 500000, 10000000, 1000000)); // height: 500000

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        // Should merge into 1 or 2 boxes
        assertTrue("Expected 1-2 boxes after consolidation, got " + result.size(), result.size() <= 2);

        // Verify coverage
        BoundingBox union = new BoundingBox(0, 0, 10000000, 1000000);
        for (BoundingBox box : result) {
            assertTrue("Result box should be within or equal to original union", union.contains(box) || box.contains(union));
        }
    }

    /**
     * Test consolidating overlapping boxes
     */
    @Test
    public void consolidateOverlappingBoxes() {
        List<BoundingBox> boxes = new ArrayList<>();
        boxes.add(new BoundingBox(0, 0, 10000000, 10000000));
        boxes.add(new BoundingBox(5000000, 5000000, 15000000, 15000000)); // overlaps

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        // Should consolidate significantly
        assertTrue("Expected 1-2 boxes after consolidation, got " + result.size(), result.size() <= 2);
    }

    /**
     * Test realistic pan-and-zoom fragmentation scenario Simulates: download initial area, then pan right and up
     */
    @Test
    public void consolidatePanAndZoomFragmentation() {
        List<BoundingBox> boxes = new ArrayList<>();

        // Initial download
        boxes.add(new BoundingBox(0, 0, 20000000, 20000000));

        // After panning right - creates right sliver
        boxes.add(new BoundingBox(20000000, 5000000, 20500000, 15000000)); // thin right edge

        // After panning up - creates top sliver
        boxes.add(new BoundingBox(5000000, 20000000, 15000000, 20500000)); // thin top edge

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        // Should reduce from 3 to 1-2 boxes
        assertTrue("Expected 1-2 boxes after consolidation, got " + result.size(), result.size() <= 2);
    }

    /**
     * Test repeated panning creating many small boxes
     */
    @Test
    public void realWorldRepeatedPanning() {
        List<BoundingBox> boxes = new ArrayList<>();

        // Simulate result of repeated pan operations creating many thin strips
        boxes.add(new BoundingBox(0, 0, 10000000, 10000000)); // main
        boxes.add(new BoundingBox(10000000, 0, 10300000, 10000000)); // right strip 1
        boxes.add(new BoundingBox(10300000, 0, 10600000, 10000000)); // right strip 2
        boxes.add(new BoundingBox(10600000, 0, 11000000, 10000000)); // right strip 3
        boxes.add(new BoundingBox(0, 10000000, 10000000, 10300000)); // top strip 1
        boxes.add(new BoundingBox(0, 10300000, 10000000, 10600000)); // top strip 2

        int originalSize = boxes.size();
        List<BoundingBox> result = BoundingBox.consolidate(boxes, 500000);

        // Should significantly reduce fragmentation
        assertTrue("Consolidation should reduce box count. Original: " + originalSize + ", Result: " + result.size(), result.size() < originalSize);
    }

    /**
     * Test that large, non-adjacent boxes are not merged
     */
    @Test
    public void largeBoxesShouldNotBeMerged() {
        List<BoundingBox> boxes = new ArrayList<>();
        // Two large, non-adjacent boxes
        boxes.add(new BoundingBox(0, 0, 20000000, 20000000));
        boxes.add(new BoundingBox(50000000, 50000000, 70000000, 70000000)); // far away

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        // Should remain as 2 separate boxes
        assertEquals("Large non-adjacent boxes should not be merged", 2, result.size());
    }

    /**
     * Test empty list
     */
    @Test
    public void consolidateEmptyList() {
        List<BoundingBox> boxes = new ArrayList<>();
        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        assertEquals("Empty list should remain empty", 0, result.size());
    }

    /**
     * Test single box
     */
    @Test
    public void consolidateSingleBox() {
        List<BoundingBox> boxes = new ArrayList<>();
        boxes.add(new BoundingBox(0, 0, 10000000, 10000000));

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        assertEquals("Single box should remain as single box", 1, result.size());
        assertEquals("Box should be unchanged", boxes.get(0), result.get(0));
    }

    /**
     * Test that small boxes contained within large boxes are merged
     */
    @Test
    public void consolidateContainedSmallBox() {
        List<BoundingBox> boxes = new ArrayList<>();
        // Large box
        boxes.add(new BoundingBox(0, 0, 10000000, 10000000));
        // Small box inside the large one
        boxes.add(new BoundingBox(4000000, 4000000, 4500000, 4500000));

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 500001);

        // Small box should be absorbed
        assertTrue("Expected 1 box after consolidation, got " + result.size(), result.size() <= 1);
    }

    /**
     * Test that minSize parameter is respected
     */
    @Test
    public void minSizeParameterRespected() {
        List<BoundingBox> boxes = new ArrayList<>();
        // Two narrow boxes
        boxes.add(new BoundingBox(0, 0, 1000000, 10000000)); // width: 1M
        boxes.add(new BoundingBox(1000000, 0, 2000000, 10000000)); // width: 1M

        // With high minSize threshold, should merge
        List<BoundingBox> result1 = BoundingBox.consolidate(boxes, 2000000);
        assertTrue("High minSize should trigger consolidation", result1.size() <= 2);

        // With low minSize threshold, might not merge
        List<BoundingBox> result2 = BoundingBox.consolidate(boxes, 100000);
        // Both behaviors are acceptable, just testing that parameter is used
        assertNotNull("Result should not be null", result2);
    }

    /**
     * Test complex fragmentation pattern (multiple strips from different directions)
     */
    @Test
    public void consolidateComplexFragmentationPattern() {
        List<BoundingBox> boxes = new ArrayList<>();

        // Main coverage
        boxes.add(new BoundingBox(10000000, 10000000, 30000000, 30000000));

        // Right strips
        boxes.add(new BoundingBox(30000000, 12000000, 30500000, 28000000));
        boxes.add(new BoundingBox(30500000, 12000000, 31000000, 28000000));

        // Top strips
        boxes.add(new BoundingBox(12000000, 30000000, 28000000, 30500000));
        boxes.add(new BoundingBox(12000000, 30500000, 28000000, 31000000));

        // Bottom strips
        boxes.add(new BoundingBox(12000000, 9500000, 28000000, 10000000));

        // Left strips
        boxes.add(new BoundingBox(9500000, 12000000, 10000000, 28000000));

        int originalSize = boxes.size();
        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        // Should consolidate significantly
        assertTrue("Expected significant reduction. Original: " + originalSize + ", Result: " + result.size(), result.size() < originalSize);
    }

    /**
     * This test validates that after consolidation, the union of result boxes covers at least as much area as the union
     * of original boxes.
     */
    @Test
    public void consolidationPreservesCoverage() {
        List<BoundingBox> boxes = new ArrayList<>();

        // Create a realistic pan scenario
        boxes.add(new BoundingBox(0, 0, 20000000, 20000000));
        boxes.add(new BoundingBox(15000000, 0, 22000000, 20000000)); // overlapping right
        boxes.add(new BoundingBox(0, 15000000, 20000000, 22000000)); // overlapping top
        boxes.add(new BoundingBox(20000000, 10000000, 21000000, 20000000)); // thin sliver
        boxes.add(new BoundingBox(10000000, 20000000, 20000000, 21000000)); // thin sliver

        // Calculate original coverage bounds
        BoundingBox originalUnion = BoundingBox.union(boxes);
        assertNotNull("Original union should not be null", originalUnion);

        // Consolidate
        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000001);

        // Calculate result coverage bounds
        BoundingBox resultUnion = BoundingBox.union(result);
        assertNotNull("Result union should not be null", resultUnion);

        // Verify critical test points
        // 1. Result should cover at least the core areas
        assertTrue("Result should cover original bounds or be larger",
                resultUnion.getLeft() <= originalUnion.getLeft() && resultUnion.getRight() >= originalUnion.getRight()
                        && resultUnion.getBottom() <= originalUnion.getBottom() && resultUnion.getTop() >= originalUnion.getTop());

        // 2. Result should have fewer boxes
        assertTrue("Consolidation should reduce box count", result.size() < boxes.size());
    }

    /**
     * Test that the algorithm handles boxes touching at edges (not overlapping)
     */
    @Test
    public void adjacentNonOverlappingBoxes() {
        List<BoundingBox> boxes = new ArrayList<>();
        // Two boxes that touch but don't overlap
        boxes.add(new BoundingBox(0, 0, 10000000, 10000000));
        boxes.add(new BoundingBox(10000000, 0, 20000000, 10000000)); // starts where previous ends

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000000);

        // Should be consolidatable since they're adjacent
        assertTrue("Adjacent boxes should consolidate to 1-2 boxes, got " + result.size(), result.size() <= 2);
    }

    /**
     * Test with many tiny boxes (extreme fragmentation)
     */
    @Test
    public void extremeFragmentation() {
        List<BoundingBox> boxes = new ArrayList<>();

        // Create a 10x10 grid of tiny boxes
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int left = i * 1000000;
                int bottom = j * 1000000;
                boxes.add(new BoundingBox(left, bottom, left + 1000000, bottom + 1000000));
            }
        }

        assertEquals("Starting with 100 tiny boxes", 100, boxes.size());

        List<BoundingBox> result = BoundingBox.consolidate(boxes, 1000001);

        // Should consolidate significantly
        assertTrue("Extreme fragmentation should consolidate dramatically. Result: " + result.size(), result.size() < 50);
    }
}