package de.blau.android.util.mvt.style;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.graphics.Rect;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class CollisionDetectionTest {

    /**
     * Add a couple of objects and then check what collides
     */
    @Test
    public void collisionTest() {
        SimpleCollisionDetector detector = new SimpleCollisionDetector();

        assertFalse(detector.collides(new Rect(100, 100, 200, 200)));
        assertFalse(detector.collides(new float[] { 220, 100 }, new float[] { 270, 200 }, 10));

        // inside
        assertTrue(detector.collides(new Rect(120, 120, 180, 180)));
        // intersection
        assertTrue(detector.collides(new Rect(250, 150, 280, 180)));

        detector.reset();
        // these shoudn't collide now
        assertFalse(detector.collides(new Rect(120, 120, 180, 180)));
        assertFalse(detector.collides(new Rect(250, 150, 280, 180)));
    }
}
