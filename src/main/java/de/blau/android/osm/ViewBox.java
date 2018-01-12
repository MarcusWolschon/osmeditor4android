package de.blau.android.osm;

import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.Map;
import de.blau.android.exception.OsmException;
import de.blau.android.util.GeoMath;

/**
 * A BoundingBox with additional support for the operations typically needed when it represents a View (zooming, translation etc)
 * 
 * @author mb
 * @author simon
 */
public class ViewBox extends BoundingBox {

    private static final long serialVersionUID = -2708721312405863618L;

    /**
     * Mercator value for the bottom of the BBos
     */
    private double bottomMercator;

    /**
     * Default zoom in factor. Must be greater than 0.
     */
    private static final float ZOOM_IN = 0.125f;

    /**
     * Default zoom out factor. Must be less than 0.
     */
    private static final float ZOOM_OUT = -0.16666666f;

    /**
     * Minimum width to zoom in.
     */
    private static final int MIN_ZOOM_WIDTH = 250; // roughly 3 m at the equator

    /**
     * Maximum width to zoom out.
     */
    private static final long MAX_ZOOM_WIDTH = 3599999999L;

    private static final String DEBUG_TAG = ViewBox.class.getSimpleName();

    /**
     * The screen w/h ratio of this BoundingBox. Only needed when it's used as a viewbox.
     */
    private float ratio = 1;

    /**
     * @return a BoundingBox initialized to the maximum extent of mercator projection
     */
    public static ViewBox getMaxMercatorExtent() {
        ViewBox box = new ViewBox();
        box.setLeft((int) (-180 * 1E7));
        box.setBottom((int) (-GeoMath.MAX_LAT * 1E7));
        box.setRight((int) (180 * 1E7));
        box.setTop((int) (GeoMath.MAX_LAT * 1E7));
        box.calcDimensions();
        box.calcBottomMercator();
        return box;
    }

    /**
     * Default constructor
     */
    public ViewBox() {
        super();
    }
    
    /**
     * Creates a degenerated BoundingBox with the corners set to the node coordinates validate will cause an exception
     * if called on this
     * 
     * @param lonE7 longitude of the node
     * @param latE7 latitude of the node
     */
    public ViewBox(int lonE7, int latE7) {
        resetTo(lonE7, latE7);
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
    public ViewBox(final int left, final int bottom, final int right, final int top) throws OsmException {
        setLeft(left);
        setBottom(bottom);
        setRight(right);
        setTop(top);
        calcDimensions();
        calcBottomMercator();
        validate();
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
    public ViewBox(final double left, final double bottom, final double right, final double top) throws OsmException {
        this((int) (left * 1E7), (int) (bottom * 1E7), (int) (right * 1E7), (int) (top * 1E7));
    }

    /**
     * Copy-Constructor.
     * 
     * @param box box with the new borders.
     */
    public ViewBox(final ViewBox box) {
        // this(box.left, box.bottom, box.right, box.top); not good, forces a recalc of everything
        super(box);
        this.bottomMercator = box.bottomMercator;
    }
    
    /**
     * Construct a ViewBox from a BoundingBox
     * 
     * @param box box with the new borders.
     */
    public ViewBox(final BoundingBox box) {
        // this(box.left, box.bottom, box.right, box.top); not good, forces a recalc of everything
        super(box);
        calcDimensions();
        calcBottomMercator();
    }

    /**
     * @return returns a copy of this object.
     */
    @Override
    public ViewBox copy() {
        return new ViewBox(this);
    }

    /**
     * @return true if left is less than right and bottom is less than top.
     */
    private boolean isValid() {
        int left = getLeft();
        int right = getRight();
        int top = getTop();
        int bottom = getBottom();
        return (left < right) && (bottom < top) && (left >= -MAX_LON_E7) && (right <= MAX_LON_E7) && (top <= MAX_LAT_E7) && (bottom >= -MAX_LAT_E7);
    }


    /**
     */
    private void calcBottomMercator() {
        bottomMercator = GeoMath.latE7ToMercator(getBottom());
    }
    

    public double getPixelRadius(int screenWidth) {
        return (double) screenWidth / (getWidth() / 1E7d);
    }

    /**
     * Changes the dimensions of this bounding box to fit the given ratio. Ratio is width divided by height. The
     * smallest dimension will remain, the larger one will be resized to fit ratio.
     * 
     * @param map an instance
     * @param ratio The new aspect ratio.
     */
    public void setRatio(Map map, final float ratio) throws OsmException {
        setRatio(map, ratio, false);
    }

    /**
     * Changes the dimensions of this bounding box to fit the given ratio.
     * 
     * @param ratio The new aspect ratio.
     * @param preserveZoom If true, maintains the current level of zoom by creating a new boundingbox at the required
     *            ratio at the same center. If false, the new bounding box is sized such that the currently visible area
     *            is still visible with the new aspect ratio applied.
     */
    public void setRatio(Map map, final float ratio, final boolean preserveZoom) throws OsmException {
        long mTop = GeoMath.latE7ToMercatorE7(getTop()); // note long or else we get an int overflow on calculating the
                                                    // center
        long mBottom = GeoMath.latE7ToMercatorE7(getBottom());
        long mHeight = mTop - mBottom;
        if (getWidth() <= 0 || mHeight <= 0) {
            long width = getWidth();
            // should't happen, but just in case
            Log.d(DEBUG_TAG, "Width or height zero: " + width + "/" + mHeight);
            ViewBox bbox = new ViewBox(GeoMath.createBoundingBoxForCoordinates(GeoMath.mercatorE7ToLat((int) (mBottom + mHeight / 2)),
                    GeoMath.mercatorE7ToLat((int) (getLeft() + width / 2)), 10.0f, true));
            set(bbox);
            calcDimensions();
            mTop = GeoMath.latE7ToMercatorE7(getTop()); // note long or else we get an int overflow on calculating the center
            mBottom = GeoMath.latE7ToMercatorE7(getBottom());
            mHeight = mTop - mBottom;
        }

        // Log.d(DEBUG_TAG,"current ratio " + this.ratio + " new ratio " + ratio);
        if ((ratio > 0) && !Float.isNaN(ratio)) {
            long width = getWidth();
            if (preserveZoom) {
                // Apply the new aspect ratio, but preserve the level of zoom
                // so that for example, rotating portrait<-->landscape won't
                // zoom out
                long centerX = getLeft() + width / 2L; // divide first to stay < 2^32
                long centerY = mBottom + mHeight / 2L;

                long newHeight2 = 0;
                long newWidth2 = 0;
                if (ratio <= 1.0) { // portrait and square
                    if (width <= mHeight) {
                        newHeight2 = Math.round(((width / 2D) / ratio));
                        newWidth2 = width / 2L;
                    } else { // switch landscape --> portrait
                        float pixelDeg = (float) map.getHeight() / (float) width; // height was the old width
                        newWidth2 = (long) (map.getWidth() / pixelDeg) / 2L;
                        newHeight2 = Math.round(newWidth2 / ratio);
                    }
                } else { // landscape
                    if (width < mHeight) { // switch portrait -> landscape
                        float pixelDeg = (float) map.getHeight() / (float) width; // height was the old width
                        newWidth2 = (long) (map.getWidth() / pixelDeg) / 2L;
                        newHeight2 = Math.round(newWidth2 / ratio);
                    } else {
                        newHeight2 = Math.round((width / 2D) / ratio);
                        newWidth2 = width / 2L;
                    }
                }

                if (centerX + newWidth2 > MAX_LON_E7) {
                    setRight(MAX_LON_E7);
                    setLeft((int) Math.max(-MAX_LON_E7, MAX_LON_E7 - 2 * newWidth2));
                } else if (centerX - newWidth2 < -MAX_LON_E7) {
                    setLeft(-MAX_LON_E7);
                    setRight((int) Math.min(MAX_LON_E7, centerX + 2 * newWidth2));
                } else {
                    setLeft((int) (centerX - newWidth2));
                    setRight((int) (centerX + newWidth2));
                }

                //
                if ((centerY + newHeight2) > GeoMath.MAX_MLAT_E7) {
                    mTop = GeoMath.MAX_MLAT_E7;
                    mBottom = Math.max(-GeoMath.MAX_MLAT_E7, GeoMath.MAX_MLAT_E7 - 2 * newHeight2);
                } else if ((centerY - newHeight2) < -GeoMath.MAX_MLAT_E7) {
                    mBottom = -GeoMath.MAX_MLAT_E7;
                    mTop = Math.min(GeoMath.MAX_MLAT_E7, -GeoMath.MAX_MLAT_E7 + 2 * newHeight2);
                } else {
                    mTop = centerY + newHeight2;
                    mBottom = centerY - newHeight2;
                }
            } else {
                int singleBorderMovement;
                // Ensure currently visible area is entirely visible in the new box
                if ((width / (mHeight)) < ratio) {
                    // The actual box is wider than it should be.
                    /*
                     * Here comes the math: width/height = ratio width = ratio * height newWidth = width - ratio *
                     * height
                     */
                    singleBorderMovement = Math.round((width - ratio * mHeight) / 2);
                    setLeft(getLeft() + singleBorderMovement);
                    setRight(getRight() - singleBorderMovement);
                } else {
                    // The actual box is more narrow than it should be.
                    /*
                     * Same in here, only different: width/height = ratio height = width/ratio newHeight = height -
                     * width/ratio
                     */
                    singleBorderMovement = Math.round((mHeight - width / ratio) / 2);
                    mBottom += singleBorderMovement;
                    mTop -= singleBorderMovement;
                }
            }
            setTop(GeoMath.mercatorE7ToLatE7((int) mTop));
            setBottom(GeoMath.mercatorE7ToLatE7((int) mBottom));
            // border-sizes changed. So we have to recalculate the dimensions.
            calcDimensions();
            calcBottomMercator();
            this.ratio = ratio;
            validate();
        }
    }

    /**
     * Performs a translation so the center of this bounding box will be at (lonCenter|latCenter).
     * 
     * @param map current map view
     * @param lonCenter the absolute longitude for the center (deg*1E7)
     * @param latCenter the absolute latitude for the center (deg*1E7)
     */
    public void moveTo(Map map, final int lonCenter, final int latCenter) {
        // new middle in mercator
        double mLatCenter = GeoMath.latE7ToMercator(latCenter);
        double mTop = GeoMath.latE7ToMercator(getTop());
        int newBottom = GeoMath.mercatorToLatE7(mLatCenter - (mTop - bottomMercator) / 2);

        try {
            translate(map, (lonCenter - getLeft() - (int) (getWidth() / 2L)), newBottom - getBottom());
        } catch (OsmException e) {
            Log.d(DEBUG_TAG, "moveTo got exception " + e.getMessage());
        }
    }

    /**
     * Relative translation.
     * 
     * Note clamping based on direction of movement can cause problems, always check that we are in bounds
     * 
     * @param map instance of the current map view
     * @param dLon the relative longitude change.
     * @param dLat the relative latitude change.
     */
    public synchronized void translate(@Nullable Map map, int dLon, int dLat) throws OsmException {
        int right = getRight();
        int left = getLeft();
        int top = getTop();
        int bottom = getBottom();
        if ((long) right + (long) dLon > (long) MAX_LON_E7) {
            dLon = MAX_LON_E7 - right;
        } else if ((long) left + (long) dLon < (long) -MAX_LON_E7) {
            dLon = -MAX_LON_E7 - left;
        }
        if (top + dLat > MAX_LAT_E7) {
            dLat = MAX_LAT_E7 - top;
        } else if (bottom + dLat < -MAX_LAT_E7) {
            dLat = -MAX_LAT_E7 - bottom;
        }
        setLeft(left + dLon);
        setRight(right + dLon);
        setTop(top + dLat);
        setBottom(bottom + dLat);
        calcBottomMercator();
        if (map != null) {
            setRatio(map, ratio, true); // TODO slightly expensive likely to be better to do everything in mercator
        }
        validate();
    }

    /**
     * Calculate the largest zoom-in factor that can be applied to the current view.
     * 
     * @return The largest allowable zoom-in factor.
     */
    private float zoomInLimit() {
        long width = getWidth();
        return (width - MIN_ZOOM_WIDTH) / 2f / width;
    }

    /**
     * Calculate the largest zoom-out factor that can be applied to the current view.
     * 
     * @return The largest allowable zoom-out factor.
     */
    private float zoomOutLimit() {
        long width = getWidth();
        long mTop = GeoMath.latE7ToMercatorE7(getTop());
        long mBottom = GeoMath.latE7ToMercatorE7(getBottom());
        long mHeight = mTop - mBottom;
        return -Math.min((MAX_ZOOM_WIDTH - width) / 2f / width, ((2L * (long) GeoMath.MAX_MLAT_E7) - mHeight) / 2f / mHeight);
    }

    /**
     * Test if the box can be zoomed in.
     * 
     * @return true if the box can be zoomed in, false if it can't.
     */
    public boolean canZoomIn() {
        return (ZOOM_IN < zoomInLimit());
    }

    /**
     * Test if the box can be zoomed out.
     * 
     * @return true if the box can be zoomed out, false if it can't.
     */
    public boolean canZoomOut() {
        // return (ZOOM_OUT > zoomOutLimit());
        return zoomOutLimit() < -3.1E-9; // determined experimental
    }

    /**
     * Reduces this bounding box by the ZOOM_IN factor. The ratio of width and height remains.
     */
    public void zoomIn() {
        zoom(ZOOM_IN);
    }

    /**
     * Enlarges this bounding box by the ZOOM_OUT factor. The ratio of width and height remains.
     */
    public void zoomOut() {
        zoom(ZOOM_OUT);
    }

    /**
     * Enlarges/reduces the borders by zoomFactor.
     * 
     * @param zoomFactor factor enlarge/reduce the borders.
     */
    public void zoom(float zoomFactor) {
        // Log.d(DEBUG_TAG,"zoom " + this.toString());
        zoomFactor = Math.min(zoomInLimit(), zoomFactor);
        zoomFactor = Math.max(zoomOutLimit(), zoomFactor);

        long mTop = GeoMath.latE7ToMercatorE7(getTop());
        long mBottom = GeoMath.latE7ToMercatorE7(getBottom());
        long mHeight = mTop - mBottom;

        long horizontalChange = (long) (getWidth() * zoomFactor);
        long verticalChange = (long) (mHeight * zoomFactor);
        int left = getLeft();
        long tmpLeft = left;
        long tmpRight = getRight();
        //
        if (tmpLeft + horizontalChange < (long) -MAX_LON_E7) {
            long rest = left + horizontalChange + (long) MAX_LON_E7;
            tmpLeft = -MAX_LON_E7;
            tmpRight = tmpRight - rest;
        } else {
            tmpLeft = tmpLeft + horizontalChange;
        }
        if (tmpRight - horizontalChange > (long) MAX_LON_E7) {
            long rest = tmpRight - horizontalChange - (long) MAX_LON_E7;
            tmpRight = MAX_LON_E7;
            tmpLeft = Math.max((long) -MAX_LON_E7, tmpLeft + rest);
        } else {
            tmpRight = tmpRight - horizontalChange;
        }
        setLeft((int) tmpLeft);
        setRight((int) tmpRight);
        // left = Math.max(-MAX_LON, left + (int)horizontalChange);
        // right = Math.min(MAX_LON, right - (int)horizontalChange);

        if ((mBottom + verticalChange) < -GeoMath.MAX_MLAT_E7) {
            long rest = mBottom + verticalChange + (long) GeoMath.MAX_MLAT_E7;
            mBottom = -GeoMath.MAX_MLAT_E7;
            mTop = mTop - rest;
        } else {
            mBottom = mBottom + verticalChange;
        }
        if ((mTop - verticalChange) > (long) GeoMath.MAX_MLAT_E7) {
            long rest = mTop - verticalChange - (long) GeoMath.MAX_MLAT_E7;
            mTop = GeoMath.MAX_MLAT_E7;
            mBottom = Math.max(-GeoMath.MAX_MLAT_E7, mBottom - rest);
        } else {
            mTop = mTop - verticalChange;
        }
        setBottom(GeoMath.mercatorE7ToLatE7((int) mBottom));
        setTop(GeoMath.mercatorE7ToLatE7((int) mTop));
        // bottom = Math.max(-MAX_LAT_E7, GeoMath.mercatorE7ToLatE7((int)(mBottom + (long)verticalChange)));
        // top = Math.min(MAX_LAT_E7, GeoMath.mercatorE7ToLatE7((int)(mTop - (long)verticalChange)));

        // setRatio(ratio, true);

        calcDimensions(); // need to do this or else centering will not work
        calcBottomMercator();
    }

    /**
     * set current zoom level to a tile zoom level equivalent, powers of 2 assuming 256x256 tiles maintain center of
     * bounding box
     * 
     * @param tileZoomLevel The TMS zoom level to zoom to (from 0 for the whole world to about 19 for small areas).
     */
    public void setZoom(Map map, int tileZoomLevel) {
        // setting an exact zoom level implies one screen pixel == one tile pixel
        // calculate one pixel in degrees (mercator) at this zoom level
        double degE7PerPixel = 3600000000.0d / (256 * Math.pow(2, tileZoomLevel));
        double wDegE7 = map.getWidth() * degE7PerPixel;
        double hDegE7 = map.getHeight() * degE7PerPixel;
        long centerLon = getLeft() + getWidth() / 2;
        setLeft((int) (centerLon - wDegE7 / 2));
        setRight((int) (getLeft() + wDegE7));
        long mBottom = GeoMath.latE7ToMercatorE7(getBottom());
        long mTop = GeoMath.latE7ToMercatorE7(getTop());
        long centerLat = mBottom + (mTop - mBottom) / 2;
        setBottom(GeoMath.mercatorE7ToLatE7((int) (centerLat - hDegE7 / 2)));
        setTop(GeoMath.mercatorE7ToLatE7((int) (centerLat + hDegE7 / 2)));
        calcDimensions(); //
        calcBottomMercator();
    }

    /**
     * Sets the borders to the ones of newBox. Recalculates dimensions to fit the current ratio (that of the window) and
     * maintains zoom level
     * 
     * @param map current map view
     * @param newBox box with the new borders.
     */
    public void setBorders(Map map, final BoundingBox newBox) {
        setBorders(map, newBox, this.ratio);
    }

    /**
     * Sets the borders to the ones of newBox. Recalculates dimensions to fit the ratio and maintains zoom level
     * 
     * @param map current map view
     * @param newBox new bounding box
     * @param ratio width/height ratio
     */
    private void setBorders(Map map, final BoundingBox newBox, float ratio) {
        setBorders(map, newBox, ratio, true);
    }

    /**
     * Sets the borders to the ones of newBox. Recalculates dimensions to fit the current ratio (that of the window) and
     * maintains zoom level depending on the value of preserveZoom
     * 
     * @param map current map view
     * @param newBox new bounding box
     * @param preserveZoom maintain current zoom level
     */
    public void setBorders(final Map map, final BoundingBox newBox, boolean preserveZoom) {
        setBorders(map, newBox, this.ratio, preserveZoom);
    }

    /**
     * Sets the borders to the ones of newBox. Recalculates dimensions to fit the current ratio (that of the window) and
     * maintains zoom level depending on the value of preserveZoom
     * 
     * @param map map current map view
     * @param newBox new bounding box
     * @param ratio current window ratio
     * @param preserveZoom maintain current zoom level
     */
    public void setBorders(final Map map, final BoundingBox newBox, float ratio, boolean preserveZoom) {
        set(newBox);
        Log.d(DEBUG_TAG, "setBorders " + newBox.toString() + " ratio is " + ratio);
        try {
            calcDimensions(); // neede to recalc width
            setRatio(map, ratio, preserveZoom);
            validate();
        } catch (Exception e) {
            Log.d(DEBUG_TAG, "setBorders got exception " + e.getMessage());
        } // TODO slightly expensive likely to be better to do everything in mercator
    }

    /**
     * Make the bounding box a valid request for the API, shrinking into its center if necessary.
     */
    @Override
    public void makeValidForApi() {
        super.makeValidForApi();
        calcBottomMercator();
    }

    /**
     * Check if this is a valid bounding box
     * 
     * @throws OsmException if not a valid bounding box
     */
    private void validate() throws OsmException {
        if (!isValid()) {
            Log.e(DEBUG_TAG, toString());
            throw new OsmException("left must be less than right and bottom must be less than top");
        }
    }

    /**
     * Return pre-caclulated mercator value for the bottom of the bounding box
     * 
     * @return the projected bottom value
     */
    public double getBottomMercator() {
        return bottomMercator;
    }

    /**
     * Return lat value of the vertical center of the bounding box
     * 
     * @return vertical center of the bounding box in degrees
     */
    public double getCenterLat() {
        int mBottom = GeoMath.latE7ToMercatorE7(getBottom());
        int mHeight = GeoMath.latE7ToMercatorE7(getTop()) - mBottom;
        return GeoMath.mercatorE7ToLat(mBottom + mHeight / 2);
    }

    /**
     * Get the center of a bounding box in WGS84 coords
     * 
     * @return an array with lon and lat value
     */
    public double[] getCenter() {
        double[] result = new double[2];
        result[0] = ((getRight() - getLeft()) / 2D + getLeft()) / 1E7D;
        result[1] = getCenterLat();
        return result;
    }
}
