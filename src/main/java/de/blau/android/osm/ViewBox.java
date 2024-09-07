package de.blau.android.osm;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.Map;
import de.blau.android.exception.OsmException;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.GeoMath;

/**
 * A BoundingBox with additional support for the operations typically needed when it represents a View (zooming,
 * translation etc). The box is constrained to Mercartor compatible latitude values.
 * 
 * @author mb
 * @author simon
 */
public class ViewBox extends BoundingBox {

    private static final long serialVersionUID = -2708721312405863618L;

    private static final String DEBUG_TAG = ViewBox.class.getSimpleName().substring(0, Math.min(23, ViewBox.class.getSimpleName().length()));

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

    /**
     * The screen w/h ratio of this ViewBox.
     */
    private float ratio = 1;

    /**
     * @return a BoundingBox initialized to the maximum extent of mercator projection
     */
    public static ViewBox getMaxMercatorExtent() {
        ViewBox box = new ViewBox();
        box.setLeft((int) (-180 * 1E7));
        box.setBottom(-GeoMath.MAX_COMPAT_LAT_E7);
        box.setRight((int) (180 * 1E7));
        box.setTop(GeoMath.MAX_COMPAT_LAT_E7);
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
     * @throws OsmException when the borders are mixed up or outside of {@link #MAX_COMPAT_LAT_E7}/{@link #MAX_LON_E7}
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
    public ViewBox(@NonNull final ViewBox box) {
        super(box);
        this.bottomMercator = box.bottomMercator;
    }

    /**
     * Construct a ViewBox from a BoundingBox
     * 
     * @param box box with the new borders.
     */
    public ViewBox(@NonNull final BoundingBox box) {
        super(box);
        calcDimensions();
        calcBottomMercator();
    }

    @Override
    public ViewBox copy() {
        return new ViewBox(this);
    }

    @Override
    public void set(@NonNull final BoundingBox box) {
        super.set(box);
        if (box instanceof ViewBox) {
            this.bottomMercator = ((ViewBox) box).bottomMercator;
        } else {
            calcDimensions();
            calcBottomMercator();
        }
    }

    /**
     * @return true if left is less than right and bottom is less than top and within the legal bounds for a web
     *         mercator box.
     */
    @Override
    public boolean isValid() {
        int top = getTop();
        int bottom = getBottom();
        return super.isValid() && (top <= GeoMath.MAX_COMPAT_LAT_E7) && (bottom >= -GeoMath.MAX_COMPAT_LAT_E7);
    }

    /**
     */
    private void calcBottomMercator() {
        bottomMercator = GeoMath.latE7ToMercator(getBottom());
    }

    /**
     * Get the size of a pixel in WGS84°
     * 
     * @param screenWidth the current screen width in pixel
     * @return the size of a pixel in WGS84°
     */
    public double getPixelRadius(int screenWidth) {
        return screenWidth / (getWidth() / 1E7d);
    }

    /**
     * Changes the dimensions of this bounding box to fit the given ratio. Ratio is width divided by height. The
     * smallest dimension will remain, the larger one will be resized to fit ratio.
     * 
     * @param map an instance
     * @param ratio The new aspect ratio.
     * @throws OsmException if resulting ViewBox doesn't have valid corners
     */
    public void setRatio(@NonNull Map map, final float ratio) throws OsmException {
        setRatio(map, ratio, false);
    }

    /**
     * Changes the dimensions of this ViewBox to fit the given ratio.
     * 
     * @param map the current Map instance
     * @param newRatio The new aspect ratio.
     * @param preserveZoom If true, maintains the current level of zoom by creating a new boundingbox at the required
     *            ratio at the same center. If false, the new bounding box is sized such that the currently visible area
     *            is still visible with the new aspect ratio applied.
     * @throws OsmException if resulting ViewBox doesn't have valid corners
     */
    public void setRatio(@NonNull Map map, final float newRatio, final boolean preserveZoom) throws OsmException {
        long mTop = GeoMath.latE7ToMercatorE7(getTop()); // note long or else we get an int overflow on calculating the
        // center
        long mBottom = GeoMath.latE7ToMercatorE7(getBottom());
        long mHeight = mTop - mBottom;
        if (getWidth() <= 0 || mHeight <= 0) {
            long width = getWidth();
            // should't happen, but just in case
            Log.d(DEBUG_TAG, "Width or height zero: " + width + "/" + mHeight);
            ViewBox bbox = new ViewBox(GeoMath.createBoundingBoxForCoordinates(GeoMath.mercatorE7ToLat((int) (mBottom + mHeight / 2)),
                    GeoMath.mercatorE7ToLat((int) (getLeft() + width / 2)), 10.0f));
            set(bbox);
            calcDimensions();
            mTop = GeoMath.latE7ToMercatorE7(getTop()); // note long or else we get an int overflow on calculating the
                                                        // center
            mBottom = GeoMath.latE7ToMercatorE7(getBottom());
            mHeight = mTop - mBottom;
        }

        if ((newRatio > 0) && !Float.isNaN(newRatio)) {
            long width = getWidth();
            if (preserveZoom) {
                // Apply the new aspect ratio, but preserve the level of zoom
                // so that for example, rotating portrait<-->landscape won't
                // zoom out
                long centerX = getLeft() + width / 2L; // divide first to stay < 2^32
                long centerY = mBottom + mHeight / 2L;

                long newHeight2 = 0;
                long newWidth2 = 0;
                if (newRatio <= 1.0) { // portrait and square
                    if (width <= mHeight) {
                        newHeight2 = Math.round(((width / 2D) / newRatio));
                        newWidth2 = width / 2L;
                    } else { // switch landscape --> portrait
                        float pixelDeg = (float) map.getHeight() / (float) width; // height was the old width
                        newWidth2 = (long) (map.getWidth() / pixelDeg) / 2L;
                        newHeight2 = Math.round(newWidth2 / newRatio);
                    }
                } else { // landscape
                    if (width < mHeight) { // switch portrait -> landscape
                        float pixelDeg = (float) map.getHeight() / (float) width; // height was the old width
                        newWidth2 = (long) (map.getWidth() / pixelDeg) / 2L;
                        newHeight2 = Math.round(newWidth2 / newRatio);
                    } else {
                        newHeight2 = Math.round((width / 2D) / newRatio);
                        newWidth2 = width / 2L;
                    }
                }

                if (centerX + newWidth2 > GeoMath.MAX_LON_E7) {
                    setRight(GeoMath.MAX_LON_E7);
                    setLeft((int) Math.max(-GeoMath.MAX_LON_E7, GeoMath.MAX_LON_E7 - 2 * newWidth2));
                } else if (centerX - newWidth2 < -GeoMath.MAX_LON_E7) {
                    setLeft(-GeoMath.MAX_LON_E7);
                    setRight((int) Math.min(GeoMath.MAX_LON_E7, centerX + 2 * newWidth2));
                } else {
                    setLeft((int) Math.max(-GeoMath.MAX_LON_E7, centerX - newWidth2));
                    setRight((int) Math.min(GeoMath.MAX_LON_E7, centerX + newWidth2));
                }

                //
                if ((centerY + newHeight2) > GeoMath.MAX_MLAT_E7) {
                    mTop = GeoMath.MAX_MLAT_E7;
                    mBottom = Math.max(-GeoMath.MAX_MLAT_E7, GeoMath.MAX_MLAT_E7 - 2 * newHeight2);
                } else if ((centerY - newHeight2) < -GeoMath.MAX_MLAT_E7) {
                    mBottom = -GeoMath.MAX_MLAT_E7;
                    mTop = Math.min(GeoMath.MAX_MLAT_E7, -GeoMath.MAX_MLAT_E7 + 2 * newHeight2);
                } else {
                    mTop = Math.min(GeoMath.MAX_MLAT_E7, centerY + newHeight2);
                    mBottom = Math.max(-GeoMath.MAX_MLAT_E7, centerY - newHeight2);
                }
            } else {
                int singleBorderMovement;
                // Ensure currently visible area is entirely visible in the new box
                if ((width / (mHeight)) < newRatio) {
                    // The actual box is wider than it should be.
                    /*
                     * Here comes the math: width/height = ratio width = ratio * height newWidth = width - ratio *
                     * height
                     */
                    singleBorderMovement = Math.round((width - newRatio * mHeight) / 2);
                    setLeft(getLeft() + singleBorderMovement);
                    setRight(getRight() - singleBorderMovement);
                } else {
                    // The actual box is more narrow than it should be.
                    /*
                     * Same in here, only different: width/height = ratio height = width/ratio newHeight = height -
                     * width/ratio
                     */
                    singleBorderMovement = Math.round((mHeight - width / newRatio) / 2);
                    mBottom += singleBorderMovement;
                    mTop -= singleBorderMovement;
                }
            }
            setTop(GeoMath.mercatorE7ToLatE7((int) mTop));
            setBottom(GeoMath.mercatorE7ToLatE7((int) mBottom));
            // border-sizes changed. So we have to recalculate the dimensions.
            calcDimensions();
            calcBottomMercator();
            this.ratio = newRatio;
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
    public void moveTo(@Nullable Map map, final int lonCenter, final int latCenter) {
        // new middle in mercator
        double mLatCenter = GeoMath.latE7ToMercator(latCenter);
        double mTop = GeoMath.latE7ToMercator(getTop());
        int newBottom = GeoMath.mercatorToLatE7(mLatCenter - (mTop - bottomMercator) / 2);
        try {
            translate(map, (long) lonCenter - (long) getLeft() - (getWidth() / 2L), (long) newBottom - (long) getBottom());
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
     * @throws OsmException if resulting ViewBox doesn't have valid corners
     */
    public synchronized void translate(@Nullable Map map, long dLon, long dLat) throws OsmException {
        int right = getRight();
        int left = getLeft();
        int top = getTop();
        int bottom = getBottom();
        if (dLon > 0) {
            if (right + dLon > GeoMath.MAX_LON_E7 && left + dLon < GeoMath.MAX_LON_E7) {
                dLon = GeoMath.MAX_LON_E7 - (long) right;
            }
        } else if (left + dLon < -GeoMath.MAX_LON_E7 && right + dLon > -GeoMath.MAX_LON_E7) {
            dLon = -GeoMath.MAX_LON_E7 - (long) left;
        }
        if (dLat > 0) {
            if (top + dLat > GeoMath.MAX_COMPAT_LAT_E7) {
                dLat = GeoMath.MAX_COMPAT_LAT_E7 - (long) top;
            }
        } else if (bottom + dLat < -GeoMath.MAX_COMPAT_LAT_E7) {
            dLat = -GeoMath.MAX_COMPAT_LAT_E7 - (long) bottom;
        }
        setLeft(normalizeLon(left + dLon));
        setRight(normalizeLon(right + dLon));
        setTop((int) (top + dLat));
        setBottom((int) (bottom + dLat));
        calcBottomMercator();
        if (map != null) {
            setRatio(map, ratio, true); // TODO slightly expensive likely to be better to do everything in mercator
        }
        validate();
    }

    /**
     * Normalize longitude to always be between -180° and +180°
     * 
     * @param coord the longitude coordinate
     * @return the normalized longitude
     */
    private int normalizeLon(long coord) {
        if (coord < -GeoMath.MAX_LON_E7) {
            coord = coord + 2L * GeoMath.MAX_LON_E7;
        } else if (coord > GeoMath.MAX_LON_E7) {
            coord = coord - 2L * GeoMath.MAX_LON_E7;
        }
        return (int) coord;
    }

    /**
     * Calculate the largest zoom-in factor that can be applied to the current view.
     * 
     * @return The largest allowable zoom-in factor.
     */
    private float zoomInLimit() {
        long width = getWidth();
        return (Math.max(0, width - MIN_ZOOM_WIDTH)) / 2f / width;
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
        return -Math.min((MAX_ZOOM_WIDTH - width) / 2f / width, ((2L * GeoMath.MAX_MLAT_E7) - mHeight) / 2f / mHeight);
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
     * Enlarges/reduces the borders by zoomFactor + 1.
     * 
     * @param zoomFactor factor enlarge/reduce the borders.
     */
    public void zoom(float zoomFactor) {
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
        if (tmpLeft + horizontalChange < -GeoMath.MAX_LON_E7) {
            long rest = left + horizontalChange + GeoMath.MAX_LON_E7;
            tmpLeft = -GeoMath.MAX_LON_E7;
            tmpRight = Math.min(GeoMath.MAX_LON_E7, tmpRight - rest);
        } else {
            tmpLeft = tmpLeft + horizontalChange;
        }
        if (tmpRight - horizontalChange > GeoMath.MAX_LON_E7) {
            long rest = tmpRight - horizontalChange - GeoMath.MAX_LON_E7;
            tmpRight = GeoMath.MAX_LON_E7;
            tmpLeft = Math.max(-GeoMath.MAX_LON_E7, tmpLeft + rest);
        } else {
            tmpRight = tmpRight - horizontalChange;
        }
        setLeft((int) tmpLeft);
        setRight((int) tmpRight);

        if ((mBottom + verticalChange) < -GeoMath.MAX_MLAT_E7) {
            long rest = mBottom + verticalChange + GeoMath.MAX_MLAT_E7;
            mBottom = -GeoMath.MAX_MLAT_E7;
            mTop = Math.min(GeoMath.MAX_MLAT_E7, mTop - rest);
        } else {
            mBottom = mBottom + verticalChange;
        }
        if ((mTop - verticalChange) > GeoMath.MAX_MLAT_E7) {
            long rest = mTop - verticalChange - GeoMath.MAX_MLAT_E7;
            mTop = GeoMath.MAX_MLAT_E7;
            mBottom = Math.max(-GeoMath.MAX_MLAT_E7, mBottom - rest);
        } else {
            mTop = mTop - verticalChange;
        }
        setBottom(GeoMath.mercatorE7ToLatE7((int) mBottom));
        setTop(GeoMath.mercatorE7ToLatE7((int) mTop));

        calcDimensions(); // need to do this or else centering will not work
        calcBottomMercator();
    }

    private static final double MAX_W = 2D * GeoMath.MAX_LON_E7;
    private static final double MAX_H = 2D * GeoMath.MAX_MLAT_E7;

    /**
     * Set current zoom level to a tile zoom level equivalent, powers of 2 assuming 256x256 tiles maintain center of
     * bounding box
     * 
     * If one dimension of the screen would exceed the maximum allows bounds for mercator coordinates, it is clamped 
     * and the other dimension adjusted.
     * 
     * @param map the current Map instance
     * @param tileZoomLevel The TMS zoom level to zoom to (from 0 for the whole world to about 19 for small areas).
     */
    public void setZoom(@NonNull Map map, int tileZoomLevel) {
        // setting an exact zoom level implies one screen pixel == one tile pixel
        // calculate one pixel in degrees (mercator) at this zoom level
        double degE7PerPixel = MAX_W / (TileLayerSource.DEFAULT_TILE_SIZE * Math.pow(2, tileZoomLevel));
        double wDegE7 = map.getWidth() * degE7PerPixel;
        double hDegE7 = map.getHeight() * degE7PerPixel;
        double r = map.getWidth() / (double) map.getHeight();
        if (hDegE7 > MAX_H) {
            hDegE7 = MAX_H;
            wDegE7 = r * hDegE7;
        }
        if (wDegE7 > MAX_W) {
            wDegE7 = MAX_W;
            hDegE7 = MAX_W / r;
        }
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
    public void setBorders(@NonNull Map map, @NonNull final BoundingBox newBox) {
        setBorders(map, newBox, this.ratio);
    }

    /**
     * Sets the borders to the ones of newBox. Recalculates dimensions to fit the ratio and maintains zoom level
     * 
     * @param map current map view
     * @param newBox new bounding box
     * @param ratio width/height ratio
     */
    private void setBorders(@NonNull Map map, @NonNull final BoundingBox newBox, float ratio) {
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
    public void setBorders(@NonNull final Map map, @NonNull final BoundingBox newBox, boolean preserveZoom) {
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
    public void setBorders(@NonNull final Map map, @NonNull final BoundingBox newBox, float ratio, boolean preserveZoom) {
        set(newBox);
        Log.d(DEBUG_TAG, "setBorders " + newBox.toString() + " ratio is " + ratio);
        try {
            calcDimensions(); // needed to recalc width
            setRatio(map, ratio, preserveZoom); // this validates too
        } catch (Exception e) {
            Log.d(DEBUG_TAG, "setBorders got exception " + e.getMessage());
        } // TODO slightly expensive likely to be better to do everything in mercator
    }

    /**
     * Make the bounding box a valid request for the API, shrinking into its center if necessary.
     */
    @Override
    public void makeValidForApi(float maxAreaDegrees) {
        super.makeValidForApi(maxAreaDegrees);
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
        return GeoMath.mercatorToLat((mBottom + mHeight / 2D) / 1E7D);
    }

    /**
     * Get the center of a bounding box in WGS84 coords
     * 
     * @return an array with lon and lat value
     */
    @NonNull
    public double[] getCenter() {
        double[] result = new double[2];
        result[0] = ((long) getRight() + (long) getLeft()) / 2E7D;
        result[1] = getCenterLat();
        return result;
    }

    /**
     * Change dimensions so that the given BoundingBox will fit preserving the aspect ratio
     * 
     * @param map the current Map instance
     * @param extent the BoundingBox we want to fit in to, if extent is empty we will simply move the center to it
     */
    public void fitToBoundingBox(@NonNull Map map, @NonNull BoundingBox extent) {
        if (extent.isEmpty()) { // don't try to fit, simply center on it
            moveTo(map, extent.getLeft(), extent.getBottom());
            return;
        }
        BoundingBox box = new BoundingBox(extent);
        box.calcDimensions();
        long mTop = GeoMath.latE7ToMercatorE7(box.getTop());
        long mBottom = GeoMath.latE7ToMercatorE7(box.getBottom());
        long mHeight = mTop - mBottom;

        double boxRatio = (double) box.getWidth() / mHeight;
        long center = ((long) box.getLeft() - (long) box.getRight()) / 2L + box.getRight();
        if (boxRatio > ratio) { // less high than our screen -> make box higher
            long mCenter = mBottom + mHeight / 2;
            long newHeight = (long) (box.getWidth() / ratio);
            if (newHeight > GeoMath.MAX_MLAT_E7 * 2L) { // clamp
                box.setTop(GeoMath.MAX_COMPAT_LAT_E7);
                box.setBottom(-GeoMath.MAX_COMPAT_LAT_E7);
                long newWidth = (long) (GeoMath.MAX_MLAT_E7 * ratio);
                box.setLeft((int) (center + newWidth));
                box.setRight((int) (center - newWidth));
            } else if (newHeight < MIN_ZOOM_WIDTH) {
                box.setTop(GeoMath.mercatorE7ToLatE7((int) (mCenter + MIN_ZOOM_WIDTH / 2)));
                box.setBottom(GeoMath.mercatorE7ToLatE7((int) (mCenter - MIN_ZOOM_WIDTH / 2)));
                long newWidth = (long) (MIN_ZOOM_WIDTH * ratio);
                box.setLeft((int) (center + newWidth / 2));
                box.setRight((int) (center - newWidth / 2));
            } else {
                box.setTop(GeoMath.mercatorE7ToLatE7((int) (mCenter + newHeight / 2)));
                box.setBottom(GeoMath.mercatorE7ToLatE7((int) (mCenter - newHeight / 2)));
            }
        } else if (boxRatio < ratio) { // higher than our screen -> make box wider
            long newWidth = (long) (mHeight * ratio);
            if (newWidth > 2D * GeoMath.MAX_LON_E7) { // clamp
                box.setLeft(GeoMath.MAX_LON_E7);
                box.setRight(-GeoMath.MAX_LON_E7);
                long mCenter = mBottom + mHeight / 2;
                long newHeight = (long) (GeoMath.MAX_LON_E7 / ratio);
                box.setTop(GeoMath.mercatorE7ToLatE7((int) (mCenter + newHeight)));
                box.setBottom(GeoMath.mercatorE7ToLatE7((int) (mCenter - newHeight)));
            } else if (newWidth < MIN_ZOOM_WIDTH) {
                box.setLeft((int) (center + MIN_ZOOM_WIDTH / 2));
                box.setRight((int) (center - MIN_ZOOM_WIDTH / 2));
                long mCenter = mBottom + mHeight / 2;
                long newHeight = (long) (MIN_ZOOM_WIDTH / ratio);
                box.setTop(GeoMath.mercatorE7ToLatE7((int) (mCenter + newHeight / 2)));
                box.setBottom(GeoMath.mercatorE7ToLatE7((int) (mCenter - newHeight / 2)));
            } else {
                box.setLeft((int) (center + newWidth / 2));
                box.setRight((int) (center - newWidth / 2));
            }
        }
        setBorders(map, box, false);
    }

    /**
     * Expand the bounding box by d meters on every side Clamps the resulting coordinates to legal values
     * 
     * @param d distance in meters to expand the box
     */
    public void expand(double d) {
        double delta = GeoMath.convertMetersToGeoDistance(d);
        int deltaE7 = (int) (delta * 1E7);
        setTop(Math.min(GeoMath.MAX_COMPAT_LAT_E7, getTop() + deltaE7));
        setBottom(Math.max(-GeoMath.MAX_COMPAT_LAT_E7, getBottom() - deltaE7));
        int deltaHE7 = (int) ((delta / Math.cos(Math.toRadians(getTop() / 1E7D))) * 1E7D);
        setLeft(Math.max(-GeoMath.MAX_LON_E7, getLeft() - deltaHE7));
        setRight(Math.min(GeoMath.MAX_LON_E7, getRight() + deltaHE7));
        calcDimensions();
    }

    /**
     * Expand the bounding box so that it is at least d long in each dimension Clamps the resulting coordinates to legal
     * values
     * 
     * @param d minimum distance in meters for each size
     */
    public void ensureMinumumSize(double d) {
        double min = GeoMath.convertMetersToGeoDistance(d);
        int minE7 = (int) (min * 1E7);
        if (getHeight() < minE7) {
            int deltaE7 = (minE7 - getHeight()) / 2;
            setTop(Math.min(GeoMath.MAX_COMPAT_LAT_E7, getTop() + deltaE7));
            setBottom(Math.max(-GeoMath.MAX_COMPAT_LAT_E7, getBottom() - deltaE7));
        }
        int minWE7 = (int) ((min / Math.cos(Math.toRadians(getTop() / 1E7D))) * 1E7D);
        if (getWidth() < minWE7) {
            long deltaWE7 = (minWE7 - getWidth()) / 2;
            setLeft((int) Math.max(-GeoMath.MAX_LON_E7, getLeft() - deltaWE7));
            setRight((int) Math.min(GeoMath.MAX_LON_E7, getRight() + deltaWE7));
        }
        calcDimensions();
    }

    /**
     * Scale the bounding box sides by a factor. Clamps the resulting coordinates to legal values
     * 
     * @param f factor to make the sides larger
     */
    public void scale(double f) {
        int hDeltaE7 = (int) (getHeight() * f - getHeight()) / 2;
        setTop(Math.min(GeoMath.MAX_COMPAT_LAT_E7, getTop() + hDeltaE7));
        setBottom(Math.max(-GeoMath.MAX_COMPAT_LAT_E7, getBottom() - hDeltaE7));
        int wDeltaE7 = (int) (getWidth() * f - getWidth()) / 2;
        setLeft(Math.max(-GeoMath.MAX_LON_E7, getLeft() - wDeltaE7));
        setRight(Math.min(GeoMath.MAX_LON_E7, getRight() + wDeltaE7));
        calcDimensions();
    }
}
