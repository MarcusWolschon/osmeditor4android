package de.blau.android.util;

import java.util.List;

import androidx.annotation.NonNull;
import de.blau.android.osm.Node;
import de.blau.android.osm.ViewBox;

/**
 * Wrapper for a screen coordinate tupel
 * 
 * @author simon
 *
 */
public class Coordinates {

    public double x; // NOSONAR
    public double y; // NOSONAR

    /**
     * Construct a new Coordinate object
     * 
     * @param x screen x coordinate
     * @param y screen y coordinate
     */
    public Coordinates(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Subtract Coordinates from this object
     * 
     * @param s the Coordinates to subtract
     * @return the result of the operation
     */
    @NonNull
    public Coordinates subtract(@NonNull Coordinates s) {
        return new Coordinates(this.x - s.x, this.y - s.y);
    }

    /**
     * Add Coordinates to this object
     * 
     * @param p the Coordinates to add
     * @return the result of the operation
     */
    @NonNull
    public Coordinates add(@NonNull Coordinates p) {
        return new Coordinates(this.x + p.x, this.y + p.y);
    }

    /**
     * Multiple this object with a scalar value
     * 
     * @param m the scalar value to multiply with
     * @return the result of the operation
     */
    @NonNull
    public Coordinates multiply(double m) {
        return new Coordinates((float) (this.x * m), (float) (this.y * m));
    }

    /**
     * Divide this object by a scalar value
     * 
     * @param d the scalar value to divide by
     * @return the result of the operation
     */
    public Coordinates divide(double d) {
        return new Coordinates((float) (this.x / d), (float) (this.y / d));
    }

    /**
     * The scalar length
     * 
     * @return the length of this assuming it is a vector from 0,0
     */
    public double length() {
        return (float) Math.hypot(x, y);
    }

    /**
     * Convert the coordinates from a list of Nodes to an array of screen coordinates
     * 
     * @param width screen width
     * @param height screen height
     * @param box current ViewBox
     * @param nodes the List of Nodes
     * @return an array of Coordinates
     */
    @NonNull
    public static Coordinates[] nodeListToCoordinateArray(int width, int height, @NonNull ViewBox box, @NonNull List<Node> nodes) {
        int size = nodes.size();
        Coordinates[] points = new Coordinates[size];
        // loop over all nodes
        for (int i = 0; i < size; i++) {
            points[i] = nodeToCoordinates(width, height, box, nodes.get(i));
        }
        return points;
    }

    /**
     * Convert the coordinates from a Node to screen coordinates
     * 
     * @param width screen width
     * @param height screen height
     * @param box current ViewBox
     * @param node the Node
     * @return a Coordinates instance
     */
    @NonNull
    public static Coordinates nodeToCoordinates(int width, int height, @NonNull ViewBox box, @NonNull Node node) {
        return new Coordinates(GeoMath.lonE7ToX(width, box, node.getLon()), GeoMath.latE7ToY(height, width, box, node.getLat()));
    }

    /**
     * Normalize the vector and then scale by a factor
     * 
     * @param v the vector
     * @param scale the scaling factor
     * @return a new object with the result of the operation
     */
    public static Coordinates normalize(@NonNull Coordinates v, double scale) {
        Coordinates result = new Coordinates(v.x, v.y);
        double length = v.length();
        if (length != 0) {
            result.x = result.x / length;
            result.y = result.y / length;
        }
        result.x = result.x * scale;
        result.y = result.y * scale;
        return result;
    }

    /**
     * Return the dot product for two vectors
     * 
     * @param v1 vector 1
     * @param v2 vector 2
     * @return the dot product
     */
    public static double dotproduct(@NonNull Coordinates v1, @NonNull Coordinates v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    /**
     * Return the determinate for two vectors
     * 
     * @param v1 vector 1
     * @param v2 vector 2
     * @return the determinate
     */
    public static double determinate(@NonNull Coordinates v1, @NonNull Coordinates v2) {
        return v1.x * v2.y - v1.y * v2.x;
    }

    /**
     * Calculate the angle between two vectors
     * 
     * @param v1 vector 1
     * @param v2 vector 2
     * @return the angle between them in radians
     */
    public static double angle(@NonNull Coordinates v1, @NonNull Coordinates v2) {
        return Math.atan2(determinate(v1, v2), dotproduct(v1, v2));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Coordinates other = (Coordinates) obj;
        if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        return Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
    }
}
