package io.vespucci.util.mvt.style;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import android.graphics.Rect;
import androidx.annotation.NonNull;

/**
 * Simple collision detection
 * 
 * For a small number of boxes simply sequentially iterating over the existing bounds is most efficient.
 * 
 * To avoid re-constructing lots of boxes we hold them in a pool, the maximum number of boxes we can handle is set via
 * the constructor.
 * 
 * @author Simon
 *
 */
public class SimpleCollisionDetector implements CollisionDetector {

    private static final int DEFAULT_MAXIMUM = 200;

    class Box {
        private final float[][] vertices = new float[4][2];
        private final float[]   extreme  = new float[] { Float.MAX_VALUE, 0 };

        /**
         * Check if other intersects this box
         * 
         * @param other the other box
         * @return true if it intersects
         */
        public boolean intersect(@NonNull Box other) {
            float[] start = vertices[0];
            for (int i = 0; i < 3; i++) {
                float[] next = vertices[(i + 1) % 4];
                for (int j = 0; j < 3; j++) {
                    if (doIntersect(start, next, other.vertices[j], other.vertices[(j + 1) % 4])) {
                        return true;
                    }
                }
                start = next;
            }
            // is other completely inside
            for (float[] p : other.vertices) {
                if (isInside(this, p)) {
                    return true;
                }
            }
            // is this completely inside other
            for (float[] p : vertices) {
                if (isInside(other, p)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Set the vertices of the box from a Rect
         * 
         * @param rect the Rect
         */
        public void from(@NonNull Rect rect) {
            vertices[0][0] = rect.left;
            vertices[0][1] = rect.top;
            vertices[1][0] = rect.right;
            vertices[1][1] = rect.top;
            vertices[2][0] = rect.right;
            vertices[2][1] = rect.bottom;
            vertices[3][0] = rect.left;
            vertices[3][1] = rect.bottom;
        }

        /**
         * Set the vertices of the box from a line and a height
         * 
         * @param start start coordinates
         * @param end end coordinates
         * @param height the height to use (total height is times 2)
         */
        public void from(@NonNull float[] start, @NonNull float[] end, float height) {
            float dX = end[0] - start[0];
            float dY = end[1] - start[1];
            float r = (float) Math.sqrt(dX * dX + dY * dY);
            float xDiff = height / r * dY;
            float yDiff = height / r * dX;
            vertices[0][0] = start[0] + xDiff;
            vertices[0][1] = start[1] + yDiff;
            vertices[1][0] = end[0] + xDiff;
            vertices[1][1] = end[1] + yDiff;
            vertices[2][0] = end[0] - xDiff;
            vertices[2][1] = end[1] - yDiff;
            vertices[3][0] = start[0] - xDiff;
            vertices[3][1] = start[1] - yDiff;
        }

        /**
         * from https://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect Given three colinear points p,
         * q, r, the function checks if point q lies on line segment 'pr'
         * 
         * @param p 1st point
         * @param q 2nd point
         * @param r 2rd point
         * @return true is on line p - r
         */
        boolean onSegment(@NonNull float[] p, @NonNull float[] q, @NonNull float[] r) {
            return (q[0] <= Math.max(p[0], r[0]) && q[0] >= Math.min(p[0], r[0]) && q[1] <= Math.max(p[1], r[1]) && q[1] >= Math.min(p[1], r[1]));
        }

        /**
         * Determine winding
         * 
         * To find orientation of ordered triplet (p, q, r). T
         * 
         * he function returns following values
         * 
         * 0 --> p, q and r are colinear
         * 
         * 1 --> Clockwise
         * 
         * 2 --> Counterclockwise
         * 
         * @param p 1st point
         * @param q 2nd point
         * @param r 3rd point
         * @return winding value as described
         */
        int orientation(@NonNull float[] p, @NonNull float[] q, @NonNull float[] r) {
            // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
            // for details of below formula.
            int val = (int) ((q[1] - p[1]) * (r[0] - q[0]) - (q[0] - p[0]) * (r[1] - q[1]));

            if (val == 0) {
                return 0; // colinear
            }
            return (val > 0) ? 1 : 2; // clock or counterclock wise
        }

        /**
         * Determine if line segment 'p1q1' and 'p2q2' intersect.
         * 
         * @param p1 start line 1
         * @param q1 end line 1
         * @param p2 start line 2
         * @param q2 end line 2
         * @return true if the lines intersect
         */
        boolean doIntersect(@NonNull float[] p1, @NonNull float[] q1, @NonNull float[] p2, @NonNull float[] q2) {
            // Find the four orientations needed for general and
            // special cases
            int o1 = orientation(p1, q1, p2);
            int o2 = orientation(p1, q1, q2);
            int o3 = orientation(p2, q2, p1);
            int o4 = orientation(p2, q2, q1);

            // General case
            if (o1 != o2 && o3 != o4) {
                return true;
            }

            // Special Cases
            // p1, q1 and p2 are colinear and p2 lies on segment p1q1
            if (o1 == 0 && onSegment(p1, p2, q1)) {
                return true;
            }

            // p1, q1 and q2 are colinear and q2 lies on segment p1q1
            if (o2 == 0 && onSegment(p1, q2, q1)) {
                return true;
            }

            // p2, q2 and p1 are colinear and p1 lies on segment p2q2
            if (o3 == 0 && onSegment(p2, p1, q2)) {
                return true;
            }

            // p2, q2 and q1 are colinear and q1 lies on segment p2q2
            return o4 == 0 && onSegment(p2, q1, q2);
        }

        /**
         * Determine if point p is inside the polygon
         * 
         * @param polygon the polygon
         * @param p the point
         * @return true if inside
         */
        boolean isInside(@NonNull Box polygon, @NonNull float[] p) {
            extreme[1] = p[1];
            int intersections = 0;
            int i = 0;
            do {
                int next = (i + 1) % 4;
                if (doIntersect(polygon.vertices[i], polygon.vertices[next], p, extreme)) {
                    if (orientation(polygon.vertices[i], p, polygon.vertices[next]) == 0) {
                        return onSegment(polygon.vertices[i], p, polygon.vertices[next]);
                    }
                    intersections++;
                }
                i = next;
            } while (i != 0);
            return (intersections % 2 == 1);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (float[] v : vertices) {
                builder.append("[" + v[0] + "," + v[1] + "]");
            }
            return builder.toString();
        }
    }

    private final List<Box>  boxes = new ArrayList<>();
    private final Deque<Box> pool  = new ArrayDeque<>();
    private final int        max;

    /**
     * Construct a new instance
     */
    public SimpleCollisionDetector() {
        this(DEFAULT_MAXIMUM);
    }

    /**
     * Construct a new instance
     * 
     * @param max the maximum number of rects (and as a consequence labels) to handle
     */
    public SimpleCollisionDetector(int max) {
        this.max = max;
    }

    /**
     * Resets the current set of collision boxes and returns them all to the pool
     */
    @Override
    public void reset() {
        pool.addAll(boxes);
        boxes.clear();
    }

    /**
     * Check if rect collides with an existing one
     * 
     * If there is no collision adds rect to the exiting ones
     * 
     * @param rect the new Rect
     * @return true if there is a collision
     */
    @Override
    public boolean collides(@NonNull Rect rect) {
        if (pool.isEmpty()) {
            if (boxes.size() > max) {
                return false;
            }
            pool.add(new Box());
        }
        Box test = pool.pop();
        test.from(rect);
        return testForIntersection(test);
    }

    /**
     * Actually test if the Box intersects
     * 
     * @param test the Box
     * @return true if it intersects with any of the existing Boxes
     */
    private boolean testForIntersection(@NonNull Box test) {
        for (Box box : boxes) {
            if (box.intersect(test)) {
                pool.add(test); // return to pool
                return true;
            }
        }
        boxes.add(test);
        return false;
    }

    @Override
    public boolean collides(float[] start, float[] end, float height) {
        if (pool.isEmpty()) {
            if (boxes.size() > max) {
                return false;
            }
            pool.add(new Box());
        }
        Box test = pool.pop();
        test.from(start, end, height);
        return testForIntersection(test);
    }
}
