package de.blau.android.osm;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.util.GeoMath;
import de.blau.android.util.rtree.BoundedObject;

/**
 * BoundingBox represents a bounding box for a selection of an area. All values are in decimal-degree (WGS84),
 * multiplied by 1E7.
 * 
 * @author mb
 * @author simon
 */
public class BoundingBox implements Serializable, JosmXmlSerializable, BoundedObject {

    private static final long serialVersionUID = -2708721312405863618L;

    private static final String DEBUG_TAG = "BoundingBox";
    
    /**
     * left border of the bounding box, multiplied by 1E7
     */
    private int left;

    /**
     * bottom border of the bounding box, multiplied by 1E7
     */
    private int bottom;

    /**
     * right border of the bounding box, multiplied by 1E7
     */
    private int right;

    /**
     * top border of the bounding box, multiplied by 1E7
     */
    private int top;

    /**
     * The width of the bounding box. Always positive.
     */
    private long width;

    /**
     * The height of the bounding box. Always positive.
     */
    private int height;

    /**
     * Maximum latitude ({@link GeoMath#MAX_LAT}) in 1E7.
     */
    public static final int MAX_LAT_E7 = (int) (GeoMath.MAX_LAT * 1E7);

    /**
     * Maximum Longitude.
     */
    public static final int MAX_LON_E7 = (int) (GeoMath.MAX_LON * 1E7);

    /**
     * Delimiter for the bounding box as String representation.
     */
    private static final String STRING_DELIMITER = ",";

    /**
     * The maximum difference between two borders of the bounding box for the OSM-API.
     * {@link http://wiki.openstreetmap.org/index.php/Getting_Data#Construct_an_URL_for_the_HTTP_API }
     */
    public static final int API_MAX_DEGREE_DIFFERENCE = 5000000;

    /**
     * The name of the tag in the OSM-XML file.
     */
    public static final String NAME = "bounds";

    /**
     * Creates a new bounding box with coordinates initialized to zero Careful: will fail validation
     */
    public BoundingBox() {
        left = 0;
        bottom = 0;
        right = 0;
        top = 0;
        width = 0;
        height = 0;
    }

    /**
     * Creates a degenerated BoundingBox with the corners set to the node coordinates validate will cause an exception
     * if called on this
     * 
     * @param lonE7 longitude of the node (x1E7)
     * @param latE7 latitude of the node (x1E7)
     */
    public BoundingBox(int lonE7, int latE7) {
        resetTo(lonE7, latE7);
    }

    /**
     * Creates a degenerated BoundingBox with the corners set to the node coordinates validate will cause an exception
     * if called on this
     * 
     * @param lon longitude of the node
     * @param lat latitude of the node
     */
    public BoundingBox(double lon, double lat) {
        resetTo((int) Math.round(lon * 1E7), (int) Math.round(lat * 1E7));
    }

    /**
     * Resets to a degenerated BoundingBox with the corners set to the node coordinates validate will cause an exception
     * if called on a box after this has been called
     * 
     * @param lonE7 longitude of the node
     * @param latE7 latitude of the node
     */
    public void resetTo(int lonE7, int latE7) {
        left = lonE7;
        bottom = latE7;
        right = lonE7;
        top = latE7;
        width = 0;
        height = 0;
    }

    /**
     * Generates a bounding box with the given borders. Of course, left must be left to right and top must be top of
     * bottom.
     * 
     * @param left degree of the left Border, multiplied by 1E7
     * @param bottom degree of the bottom Border, multiplied by 1E7
     * @param right degree of the right Border, multiplied by 1E7
     * @param top degree of the top Border, multiplied by 1E7
     * @throws OsmException when the borders are mixed up or outside of {@link #MAX_LAT_E7}/{@link #MAX_LON_E7}
     *             (!{@link #isValid()})
     */
    public BoundingBox(final int left, final int bottom, final int right, final int top) {
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.top = top;
        calcDimensions();
    }

    /**
     * Generates a bounding box with the given borders.
     * 
     * @param left degree of the left Border
     * @param bottom degree of the bottom Border
     * @param right degree of the right Border
     * @param top degree of the top Border
     * @throws OsmException see {@link #BoundingBox(int, int, int, int)}
     */
    public BoundingBox(final double left, final double bottom, final double right, final double top) {
        this((int) (left * 1E7), (int) (bottom * 1E7), (int) (right * 1E7), (int) (top * 1E7));
    }

    /**
     * Copy-Constructor.
     * 
     * @param box box with the new borders.
     */
    public BoundingBox(final BoundingBox box) {
        // this(box.left, box.bottom, box.right, box.top); not good, forces a recalc of everything
        this.left = box.left;
        this.bottom = box.bottom;
        this.right = box.right;
        this.top = box.top;
        this.width = box.width;
        this.height = box.height;
    }

    /**
     * @return returns a copy of this object.
     */
    public BoundingBox copy() {
        return new BoundingBox(this);
    }

    /**
     * @return a String, representing the bounding box. Format: "left,bottom,right,top" in decimal degrees.
     */
    public String toApiString() {
        return Double.toString(left / 1E7D) + STRING_DELIMITER + Double.toString(bottom / 1E7D) + STRING_DELIMITER + Double.toString(right / 1E7D)
                + STRING_DELIMITER + Double.toString(top / 1E7D);
    }
    
    /**
     * Construct a BoundingBox from a String in the format L, B, R, T possibly enclosed in square brackets
     * 
     * @param boxString the String to parse
     * @return a BoundingBox or null if it couldn't be parsed
     */
    @Nullable
    public static BoundingBox fromString(@NonNull String boxString) {
        String[] corners = boxString.trim().split(",",4);
        if (corners.length == 4) {
            if (corners[0].startsWith("[")) {
                corners[0] = corners[0].substring(1, corners[0].length());
            }
            if (corners[3].endsWith("]")) {
                corners[3] = corners[3].substring(1, corners[3].length());
            }
            BoundingBox box = new BoundingBox();
            try {
                box.left = Integer.parseInt(corners[0]);
                box.bottom = Integer.parseInt(corners[1]);
                box.right = Integer.parseInt(corners[2]);
                box.top = Integer.parseInt(corners[3]);
                return box;
            } catch (NumberFormatException e) {
                // fall off the bottom
            }
        }
        Log.e(DEBUG_TAG, "Could not convert " + boxString + " to a valid BoundingBox");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + left + STRING_DELIMITER + bottom + STRING_DELIMITER + right + STRING_DELIMITER + top + ")";
    }

    /**
     * Get the left (western-most) side of the box.
     * 
     * @return The 1E7 longitude of the left side of the box.
     */
    public int getLeft() {
        return left;
    }

    /**
     * Get the bottom (southern-most) side of the box.
     * 
     * @return The 1E7 latitude of the bottom side of the box.
     */
    public int getBottom() {
        return bottom;
    }

    /**
     * Get the right (eastern-most) side of the box.
     * 
     * @return The 1E7 longitude of the right side of the box.
     */
    public int getRight() {
        return right;
    }

    /**
     * Get the top (northern-most) side of the box.
     * 
     * @return The 1E7 latitude of the top side of the box.
     */
    public int getTop() {
        return top;
    }

    /**
     * Get the width of the box.
     * 
     * @return The difference in 1E7 degrees between the right and left sides.
     */
    public long getWidth() {
        return width;
    }

    /**
     * Get the height of the box.
     * 
     * @return The difference in 1E7 degrees between the top and bottom sides.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Checks if lat/lon is in this bounding box.
     * 
     * @param lonE7 longitude (x1E7)
     * @param latE7 latitude (x1E7)
     * @return true if lat/lon is inside this bounding box.
     */
    public boolean isIn(final int lonE7, final int latE7) {
        return lonE7 >= left && lonE7 <= right && latE7 >= bottom && latE7 <= top;
    }

    /**
     * Checks if a line between lat/lon and lat2/lon2 may intersect with this bounding box.
     * 
     * @param lon longitude (x1E7) of 1st node
     * @param lat latitude (x1E7) of 1st node
     * @param lon2 longitude (x1E7) of 2nd node
     * @param lat2 latitude (x1E7) of 2nd node
     * @return true, when at least one lat/lon is inside, or a intersection could not be excluded.
     */
    public boolean intersects(final int lon, final int lat, final int lon2, final int lat2) {
        return isIn(lon, lat) || isIn(lon2, lat2) || isIntersectionPossible(lon, lat, lon2, lat2);
    }

    /**
     * Checks if this BoundingBox intersects with another
     * 
     * @param b the other BoundingBox
     * @return true if the they intersect
     */
    public boolean intersects(@NonNull final BoundingBox b) {
        if (right < b.left)
            return false; // a is left of b
        if (left > b.right)
            return false; // a is right of b
        if (top < b.bottom)
            return false; // a is above b
        if (bottom > b.top)
            return false; // a is below b
        return true; // boxes overlap
    }

    /**
     * Java Rect compatibility Return true if the boxes intersect
     * 
     * @param box2 1st BoundingBox
     * @param box 2nd BoundingBox
     * @return true if the they intersect
     */
    public static boolean intersects(@NonNull BoundingBox box2, @NonNull BoundingBox box) {
        return box2.intersects(box);
    }

    /**
     * Checks if an intersection with a line between lat/lon and lat2/lon2 is possible. If two coordinates (lat/lat2 or
     * lon/lon2) are outside of a border, no intersection is possible.
     * 
     * @param lon longitude (x1E7) of 1st node
     * @param lat latitude (x1E7) of 1st node
     * @param lon2 longitude (x1E7) of 2nd node
     * @param lat2 latitude (x1E7) of 2nd node
     * @return true, when an intersection is possible.
     */
    public boolean isIntersectionPossible(final int lon, final int lat, final int lon2, final int lat2) {
        return !(lat > top && lat2 > top || lat < bottom && lat2 < bottom || lon > right && lon2 > right || lon < left && lon2 < left);
    }

    /**
     * Calculates the dimensions width and height of this bounding box.
     */
    protected void calcDimensions() {
        int t;
        if (right < left) {
            t = right;
            right = left;
            left = t;
        }
        width = (long) right - (long) left;
        if (top < bottom) {
            t = top;
            top = bottom;
            bottom = t;
        }
        height = top - bottom;
        // Log.d(DEBUG_TAG, "calcdimensions width " + width + " height " + height);
    }

    /**
     * Returns true if the bounding box is completely contained in this
     * 
     * @param bb the bounding box
     * @return true if bb is completely inside this
     */
    public boolean contains(BoundingBox bb) {
        return (bb.bottom >= bottom) && (bb.top <= top) && (bb.left >= left) && (bb.right <= right);
    }

    /**
     * Returns true if the coordinates are in this bounding box
     * 
     * Right and top coordinate are considered inside
     * 
     * @param lonE7 longitude in degrees x 1E7
     * @param latE7 latitude in degrees x 1E7
     * @return true if the location is in the bounding box
     */
    public boolean contains(int lonE7, int latE7) {
        return left <= lonE7 && lonE7 <= right && bottom <= latE7 && latE7 <= top;
    }

    /**
     * Returns true if the coordinates are in this bounding box
     * 
     * Right and top coordinate are considered inside
     * 
     * @param longitude longitude in degrees
     * @param latitude latitude in degrees
     * @return true if the location is in the bounding box
     */
    public boolean contains(double longitude, double latitude) {
        return contains((int) (longitude * 1E7D), (int) (latitude * 1E7D));
    }

    /**
     * Set the top value
     * 
     * @param latE7 value to set in degrees x 1E7
     */
    protected void setTop(int latE7) {
        this.top = latE7;
    }

    /**
     * Set the bottom value
     * 
     * @param latE7 value to set in degrees x 1E7
     */
    protected void setBottom(int latE7) {
        this.bottom = latE7;
    }

    /**
     * Set the right value
     * 
     * 
     * @param lonE7 value to set in degrees x 1E7
     */
    protected void setRight(int lonE7) {
        this.right = lonE7;
    }

    /**
     * Set the left value
     * 
     * 
     * @param lonE7 value to set in degrees x 1E7
     */
    protected void setLeft(int lonE7) {
        this.left = lonE7;
    }

    /**
     * Check if the box is valid
     * 
     * Allows degenerated boxes
     * 
     * @return true is within WGS84 bounds
     */
    public boolean isValid() {
        return left <= right && bottom <= top && left >= -MAX_LON_E7 && right <= MAX_LON_E7 && bottom >= -MAX_LAT_E7 && top <= MAX_LAT_E7;
    }

    /**
     * Checks if the bounding box is valid for the OSM API.
     * 
     * @return true, if the bbox is smaller than 0.5*0.5 (here multiplied by 1E7) degree.
     */
    public boolean isValidForApi() {
        return width < API_MAX_DEGREE_DIFFERENCE && height < API_MAX_DEGREE_DIFFERENCE;
    }

    /**
     * Change this BoundingBox so that it is suitable for a query for map data to the OSM API
     */
    public void makeValidForApi() {
        if (!isValidForApi()) {
            int centerx = (getLeft() / 2 + getRight() / 2); // divide first to stay < 2^32
            int centery = (getTop() + getBottom()) / 2;
            setLeft(centerx - API_MAX_DEGREE_DIFFERENCE / 2);
            setRight(centerx + API_MAX_DEGREE_DIFFERENCE / 2);
            setTop(centery + API_MAX_DEGREE_DIFFERENCE / 2);
            setBottom(centery - API_MAX_DEGREE_DIFFERENCE / 2);
        }
    }

    @Override
    public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", "bounds");
        s.attribute("", "origin", "");
        s.attribute("", "maxlon", Double.toString((right / 1E7)));
        s.attribute("", "maxlat", Double.toString((top / 1E7)));
        s.attribute("", "minlon", Double.toString((left / 1E7)));
        s.attribute("", "minlat", Double.toString((bottom / 1E7)));
        s.endTag("", "bounds");
    }

    @Override
    public BoundingBox getBounds() {
        return this;
    }

    /**
     * Set corners to same values as b
     * 
     * @param b the BoundingBox to use
     */
    public void set(BoundingBox b) {
        left = b.left;
        bottom = b.bottom;
        right = b.right;
        top = b.top;
        width = b.width;
        height = b.height;
    }

    /**
     * Set the corners to the specified values
     * 
     * @param left left (longitude x1E7)
     * @param bottom bottom (latitude x1E7)
     * @param right right (longitude x1E7)
     * @param top top (latitude x1E7)
     */
    public void set(int left, int bottom, int right, int top) {
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.top = top;
        width = (long) right - left;
        height = top - bottom;
    }

    /**
     * grow this box so that it covers the point
     * 
     * @param lon longitude
     * @param lat latitude
     */
    public void union(double lon, double lat) {
        union((int) Math.round(lon * 1E7), (int) Math.round(lat * 1E7));
    }

    /**
     * grow this box so that it covers the point
     * 
     * @param lonE7 longitude (x1E7)
     * @param latE7 latitude (x1E7)
     */
    public void union(int lonE7, int latE7) {
        if (lonE7 < left) {
            left = lonE7;
        } else if (lonE7 > right) {
            right = lonE7;
        }
        if (latE7 < bottom) {
            bottom = latE7;
        } else if (latE7 > top) {
            top = latE7;
        }
        width = (long) right - left;
        height = top - bottom;
    }

    /**
     * grow this box so that it covers b
     * 
     * @param b BoundingBox that should be covered
     */
    public void union(BoundingBox b) {
        if (b.left < left) {
            left = b.left;
        }
        if (b.right > right) {
            right = b.right;
        }
        if (b.bottom < bottom) {
            bottom = b.bottom;
        }
        if (b.top > top) {
            top = b.top;
        }
        width = (long) right - left;
        height = top - bottom;
    }

    /**
     * Produce the union of a Collection of BoundingBoxe objects
     * 
     * @param boxes Collection of Boundingbox
     * @return a single BoundingBox covering all in the COlletion or null
     */
    @Nullable
    public static BoundingBox union(Collection <BoundingBox> boxes) {
        BoundingBox result = null;
        for (BoundingBox b:boxes) {
            if (result==null) {
                result = new BoundingBox(b);
            } else {
                result.union(b);
            }
        }
        return result;
    }
    
    /**
     * Return true if box is empty
     * 
     * @return true if left equals right and top equals bottom
     */
    public boolean isEmpty() {
        return left == right && top == bottom;
    }

    /**
     * Given a list of existing bounding boxes and a new bbox. Return a list of pieces of the new bbox that complete the
     * coverage
     * 
     * @param existing existing list of bounding boxes
     * @param newBox new bounding box
     * @return list of missing bounding boxes
     * @throws OsmException
     */
    public static List<BoundingBox> newBoxes(List<BoundingBox> existing, BoundingBox newBox) {
        List<BoundingBox> result = new ArrayList<>();
        result.add(newBox);
        for (BoundingBox b : existing) {
            ArrayList<BoundingBox> temp = new ArrayList<>();
            for (BoundingBox rb : result) {
                if (b.intersects(rb)) {
                    // higher than b
                    if (rb.top > b.top) {
                        temp.add(new BoundingBox(rb.left, b.top, rb.right, rb.top));
                        rb.setTop(b.top);
                    }
                    // lower than b
                    if (rb.bottom < b.bottom) {
                        temp.add(new BoundingBox(rb.left, rb.bottom, rb.right, b.bottom));
                        rb.setBottom(b.bottom);
                    }
                    // left
                    if (rb.left < b.left && rb.bottom != rb.top) {
                        temp.add(new BoundingBox(rb.left, rb.bottom, b.left, rb.top));
                        rb.setLeft(b.left);
                    }
                    // right
                    if (rb.right > b.right && rb.bottom != rb.top) {
                        temp.add(new BoundingBox(b.right, rb.bottom, rb.right, rb.top));
                        rb.setRight(b.right);
                    }
                    rb.calcDimensions();
                } else {
                    temp.add(rb);
                }
            }
            result = temp;
        }
        return result;
    }
}
