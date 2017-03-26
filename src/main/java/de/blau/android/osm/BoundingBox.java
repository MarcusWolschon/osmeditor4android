package de.blau.android.osm;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.xmlpull.v1.XmlSerializer;

import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.Map;
import de.blau.android.exception.OsmException;
import de.blau.android.util.GeoMath;
import de.blau.android.util.rtree.BoundedObject;

/**
 * BoundingBox represents a bounding box for a selection of an area. All values
 * are in decimal-degree (WGS84), multiplied by 1E7.
 * 
 * @author mb
 */
public class BoundingBox implements Serializable, JosmXmlSerializable, BoundedObject {

	private static final long serialVersionUID = -2708721312405863618L;

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
	 * Mercator value for the bottom of the BBos
	 */
	//TODO experimental code for using non-approx. projections
	private double bottomMercator;

	/**
	 * Delimiter for the bounding box as String representation.
	 */
	private static final String STRING_DELIMITER = ",";

	/**
	 * The name of the tag in the OSM-XML file.
	 */
	public static final String NAME = "bounds";

	/**
	 * Default zoom in factor. Must be greater than 0.
	 */
	private static final float ZOOM_IN = 0.125f;

	/**
	 * Default zoom out factor. Must be less than 0.
	 */
	private static final float ZOOM_OUT = -0.16666666f;

	/**
	 * The maximum difference between two borders of the bounding box for the
	 * OSM-API. {@link http://wiki.openstreetmap.org/index.php/Getting_Data#Construct_an_URL_for_the_HTTP_API }
	 */
	public static final int API_MAX_DEGREE_DIFFERENCE = 5000000;

	/**
	 * Maximum latitude ({@link GeoMath#MAX_LAT}) in 1E7.
	 */
	public static final int MAX_LAT_E7 = (int) (GeoMath.MAX_LAT * 1E7);

	/**
	 * Maximum Longitude.
	 */
	public static final int MAX_LON_E7 = (int) (GeoMath.MAX_LON * 1E7);

	/**
	 * Minimum width to zoom in.
	 */
	private static final int MIN_ZOOM_WIDTH = 1000; // roughly 110m at the equator

	/**
	 * Maximum width to zoom out.
	 */
	// private static final long MAX_ZOOM_WIDTH = 500000000L;
	private static final long MAX_ZOOM_WIDTH =   3599999999L;

	private static final String DEBUG_TAG = BoundingBox.class.getSimpleName();

	/**
	 * The ratio of this BoundingBox. Only needed when it's used as a viewbox.
	 */
	private float ratio = 1;
	
	/**
	 * @return a BoundingBox initialized to the maximum extent of mercator projection
	 */
	public static BoundingBox getMaxMercatorExtent() {
		BoundingBox box = new BoundingBox();
		box.left = (int) (-180*1E7);
		box.bottom = (int) (-GeoMath.MAX_LAT*1E7);
		box.right = (int) (180*1E7);
		box.top = (int) (GeoMath.MAX_LAT*1E7);
		box.calcDimensions();
		box.calcBottomMercator();
		return box;
	}
	
	/**
	 * Creates a new bounding box with coordinates initialized to zero
	 * Careful: will fail validation
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
	 * Creates a degenerated BoundingBox with the corners set to the node coordinates
	 * validate will cause an exception if called on this
	 * 
	 * @param lonE7 longitude of the node
	 * @param latE7 latitude of the node
	 */
	public BoundingBox(int lonE7, int latE7) {
		resetTo(lonE7, latE7);
	}
	
	/**
	 * Resets to a degenerated BoundingBox with the corners set to the node coordinates
	 * validate will cause an exception if called on a box after this has been called
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
	 * Generates a bounding box with the given borders. Of course, left must be
	 * left to right and top must be top of bottom.
	 * 
	 * @param left degree of the left Border, multiplied by 1E7
	 * @param bottom degree of the bottom Border, multiplied by 1E7
	 * @param right degree of the right Border, multiplied by 1E7
	 * @param top degree of the top Border, multiplied by 1E7
	 * @throws OsmException when the borders are mixed up or outside of
	 * {@link #MAX_LAT_E7}/{@link #MAX_LON_E7} (!{@link #isValid()})
	 */
	public BoundingBox(final int left, final int bottom, final int right, final int top) throws OsmException {
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.top = top;
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
	public BoundingBox(final double left, final double bottom, final double right, final double top)
			throws OsmException {
		this((int) (left * 1E7), (int) (bottom * 1E7), (int) (right * 1E7), (int) (top * 1E7));
	}

	/**
	 * Generates a bounding box with a given radius and a center-position.
	 * 
	 * @param centerLat latitude of the center
	 * @param centerLon longitude of the center
	 * @param radius radius in degree
	 * @throws OsmException see {@link #BoundingBox(int, int, int, int)}
	 */
	private BoundingBox(final double centerLat, final double centerLon, final double radius) throws OsmException {
		this(centerLon - radius, centerLat - radius, centerLon + radius, centerLat + radius);
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
		this.bottomMercator = box.bottomMercator;
	}

	/**
	 * @return returns a copy of this object.
	 */
	public BoundingBox copy() {
		return new BoundingBox(this);
	}

	/**
	 * Checks if the bounding box is valid for the OSM API.
	 * 
	 * @return true, if the bbox is smaller than 0.5*0.5 (here multiplied by
	 * 1E7) degree.
	 */
	public boolean isValidForApi() {
		return isValid() && (width < API_MAX_DEGREE_DIFFERENCE) && (height < API_MAX_DEGREE_DIFFERENCE);
	}

	/**
	 * @return true if left is less than right and bottom is less than top.
	 */
	private boolean isValid() {
		return (left < right) && (bottom < top) && (left >= -MAX_LON_E7) && (right <= MAX_LON_E7) && (top <= MAX_LAT_E7)
				&& (bottom >= -MAX_LAT_E7);
	}

	/**
	 * @return a String, representing the bounding box. Format:
	 * "left,bottom,right,top" in decimal degrees.
	 */
	public String toApiString() {
		return "" + left / 1E7 + STRING_DELIMITER + bottom / 1E7 + STRING_DELIMITER + right / 1E7 + STRING_DELIMITER
				+ top / 1E7;
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
	 * @return The 1E7 longitude of the left side of the box.
	 */
	public int getLeft() {
		return left;
	}

	/**
	 * Get the bottom (southern-most) side of the box.
	 * @return The 1E7 latitude of the bottom side of the box.
	 */
	public int getBottom() {
		return bottom;
	}

	/**
	 * Get the right (eastern-most) side of the box.
	 * @return The 1E7 longitude of the right side of the box.
	 */
	public int getRight() {
		return right;
	}

	/**
	 * Get the top (northern-most) side of the box.
	 * @return The 1E7 latitude of the top side of the box.
	 */
	public int getTop() {
		return top;
	}

	/**
	 * Get the width of the box.
	 * @return The difference in 1E7 degrees between the right and left sides.
	 */
	public long getWidth() {
		return width;
	}

	/**
	 * Get the height of the box.
	 * @return The difference in 1E7 degrees between the top and bottom sides.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Checks if lat/lon is in this bounding box.
	 * 
	 * @param latE7
	 * @param lonE7
	 * @return true if lat/lon is inside this bounding box.
	 */
	public boolean isIn(final int latE7, final int lonE7) {
		return lonE7 >= left && lonE7 <= right && latE7 >= bottom && latE7 <= top;
	}

	/**
	 * Checks if a line between lat/lon and lat2/lon2 may intersect with this
	 * bounding box.
	 * 
	 * @param lat
	 * @param lon
	 * @param lat2
	 * @param lon2
	 * @return true, when at least one lat/lon is inside, or a intersection
	 * could not be excluded.
	 */
	public boolean intersects(final int lat, final int lon, final int lat2, final int lon2) {
		return isIn(lat, lon) || isIn(lat2, lon2) || isIntersectionPossible(lat, lon, lat2, lon2);
	}
	
	public boolean intersects(final BoundingBox b) {
		if (right < b.left) return false; // a is left of b
		if (left > b.right) return false; // a is right of b
		if (top < b.bottom) return false; // a is above b
		if (bottom > b.top) return false; // a is below b
		return true; // boxes overlap
	}
	
	/**
	 * Java Rect compatibility
	 * Return true if the boxes intersect
	 * @param box2
	 * @param box
	 * @return
	 */
	public static boolean intersects(BoundingBox box2, BoundingBox box) {
		return box2.intersects(box);
	}

	/**
	 * Checks if an intersection with a line between lat/lon and lat2/lon2 is
	 * impossible. If two coordinates (lat/lat2 or lon/lon2) are outside of a
	 * border, no intersection is possible.
	 * 
	 * @param lat
	 * @param lon
	 * @param lat2
	 * @param lon2
	 * @return true, when an intersection is possible.
	 */
	private boolean isIntersectionPossible(final int lat, final int lon, final int lat2, final int lon2) {
		return !(lat > top   && lat2 > top   || lat < bottom && lat2 < bottom ||
				 lon > right && lon2 > right || lon < left   && lon2 < left);
	}

	/**
	 * Calculates the dimensions width and height of this bounding box.
	 */
	private void calcDimensions() {
		int t;
		if (right < left) {
			t = right;
			right = left;
			left = t;
		}
		width = (long)right - (long)left;
		if (top < bottom) {
			t = top;
			top = bottom;
			bottom = t;
		}
		height = top - bottom;
		// Log.d("BoundingBox", "calcdimensions width " + width + " height " + height);
	}

	/**
	 */
	private void calcBottomMercator() {
		bottomMercator = GeoMath.latE7ToMercator(bottom);
	}

	/**
	 * Changes the dimensions of this bounding box to fit the given ratio.
	 * Ratio is width divided by height. The smallest dimension will remain,
	 * the larger one will be resized to fit ratio.
	 * 
	 * @param map an instance 
	 * @param ratio The new aspect ratio.
	 */
	public void setRatio(Map map, final float ratio) throws OsmException {
		setRatio(map, ratio, false);
	}
	
	/**
	 * Changes the dimensions of this bounding box to fit the given ratio.
	 * @param ratio The new aspect ratio.
	 * @param preserveZoom If true, maintains the current level of zoom by
	 * creating a new boundingbox at the required ratio at the same center. If
	 * false, the new bounding box is sized such that the currently visible
	 * area is still visible with the new aspect ratio applied.
	 */
	public void setRatio(Map map, final float ratio, final boolean preserveZoom) throws OsmException {
		long mTop = GeoMath.latE7ToMercatorE7(top); // note long or else we get an int overflow on calculating the center
		long mBottom = GeoMath.latE7ToMercatorE7(bottom);
		long mHeight = mTop - mBottom;

		if (width <= 0 || mHeight <=0) {
			// should't happen, but just in case
			Log.d("BoundingBox","Width or height zero: " + width + "/" + height);
			BoundingBox bbox = GeoMath.createBoundingBoxForCoordinates(GeoMath.mercatorE7ToLat((int) (mBottom+mHeight/2)), GeoMath.mercatorE7ToLat((int) (left+width/2)), 10.0f, true);
			left = bbox.left;
			bottom = bbox.bottom;
			right = bbox.right;
			top = bbox.top;
			calcDimensions();
			mTop = GeoMath.latE7ToMercatorE7(top); // note long or else we get an int overflow on calculating the center
			mBottom = GeoMath.latE7ToMercatorE7(bottom);
			mHeight = mTop - mBottom;
		}
		
		//Log.d("BoundingBox","current ratio " + this.ratio + " new ratio " + ratio);
		if ((ratio > 0) && !Float.isNaN(ratio)) {
			if (preserveZoom) {
				// Apply the new aspect ratio, but preserve the level of zoom
				// so that for example, rotating portrait<-->landscape won't
				// zoom out
				long centerX = left + width / 2L; // divide first to stay < 2^32
				long centerY = mBottom + mHeight / 2L;
				
				long newHeight2 = 0;
				long newWidth2 = 0;
				if (ratio <= 1.0) { // portrait and square
					if (width <= mHeight) { 
						newHeight2 = (long)((width / 2L) / ratio);
						newWidth2 = width / 2L;
					} else { // switch landscape --> portrait
						float pixelDeg = (float)map.getHeight()/(float)width; // height was the old width
						newWidth2 = (long)(map.getWidth() / pixelDeg)/2L;
						newHeight2 = (long)(newWidth2 / ratio );
					}
				} else { // landscape
					if (width < mHeight) { // switch portrait -> landscape
						float pixelDeg = (float)map.getHeight()/(float)width; // height was the old width
						newWidth2 = (long)(map.getWidth() / pixelDeg)/2L;
						newHeight2 = (long)(newWidth2 / ratio );
					} else {
						newHeight2 =(long)((width / 2L) / ratio);
						newWidth2 = width / 2L;
					}
				}
				
				if (centerX + newWidth2 > MAX_LON_E7) {
					right = MAX_LON_E7;
					left = (int) Math.max(-MAX_LON_E7, MAX_LON_E7 - 2*newWidth2);
				} else if (centerX - newWidth2 < -MAX_LON_E7) {
					left = -MAX_LON_E7;
					right = (int) Math.min(MAX_LON_E7, centerX + 2*newWidth2);
				} else {
					left = (int) (centerX - newWidth2);
					right = (int) (centerX + newWidth2);
				}
				
				// 
				if ((centerY + newHeight2) > GeoMath.MAX_MLAT_E7) { 
					mTop = GeoMath.MAX_MLAT_E7;
					mBottom = Math.max(-GeoMath.MAX_MLAT_E7, GeoMath.MAX_MLAT_E7 - 2*newHeight2);
				} else if ((centerY - newHeight2) < -GeoMath.MAX_MLAT_E7) {
					mBottom = -GeoMath.MAX_MLAT_E7;
					mTop = Math.min(GeoMath.MAX_MLAT_E7, -GeoMath.MAX_MLAT_E7 + 2*newHeight2);
				} else {
					mTop = centerY + newHeight2;
					mBottom = centerY - newHeight2;
				}
			} else {
				int singleBorderMovement;
				// Ensure currently visible area is entirely visible in the new box
				if ((width / (mHeight)) < ratio) {
					// The actual box is wider than it should be.
					/* Here comes the math:
					 * width/height = ratio
					 * width = ratio * height
					 * newWidth = width - ratio * height
					 */
					singleBorderMovement = Math.round((width - ratio * mHeight) / 2);
					left += singleBorderMovement;
					right -= singleBorderMovement;
				} else {
					// The actual box is more narrow than it should be.
					/* Same in here, only different:
					 * width/height = ratio
					 * height = width/ratio
					 * newHeight = height - width/ratio
					 */
					singleBorderMovement = Math.round((mHeight - width / ratio) / 2);
					mBottom += singleBorderMovement;
					mTop -= singleBorderMovement;
				}
			}
			top = GeoMath.mercatorE7ToLatE7((int)mTop);
			bottom = GeoMath.mercatorE7ToLatE7((int)mBottom);
			// border-sizes changed. So we have to recalculate the dimensions.
			calcDimensions();
			calcBottomMercator();
			this.ratio = ratio;
			validate();
		}
	}

	/**
	 * Performs a translation so the center of this bounding box will be at
	 * (lonCenter|latCenter).
	 * 
	 * @param map current map view
	 * @param lonCenter the absolute longitude for the center (deg*1E7)
	 * @param latCenter the absolute latitude for the center (deg*1E7)
	 */
	public void moveTo(Map map, final int lonCenter, final int latCenter) {
		// new middle in mercator
		double mLatCenter = GeoMath.latE7ToMercator(latCenter);
		double mTop = GeoMath.latE7ToMercator(top);
		int newBottom = GeoMath.mercatorToLatE7(mLatCenter - (mTop - bottomMercator)/2);
		
		try {
			translate(map, (lonCenter - left - (int)(width / 2L)), newBottom - bottom);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	

	/**
	 * Relative translation.
	 * 
	 * Note clamping based on direction of movement can cause problems, always check that we are in bounds
	 * 
	 * @param map instance of the current map view
	 * @param lon the relative longitude change.
	 * @param lat the relative latitude change.
	 */
	public void translate(@Nullable Map map, int lon, int lat) throws OsmException {
		if ((long)right + (long)lon > (long)MAX_LON_E7) {
			lon = MAX_LON_E7 - right;
		} else if ((long)left + (long)lon < (long)-MAX_LON_E7) {
			lon = -MAX_LON_E7 - left;
		} 
		if (top + lat > MAX_LAT_E7) {
			lat = MAX_LAT_E7 - top;
		} else if (bottom + lat < -MAX_LAT_E7) {
			lat = -MAX_LAT_E7 - bottom;
		}
		left += lon;
		right += lon;
		top += lat;
		bottom += lat;
		if (map != null) {
			setRatio(map, ratio, true); //TODO slightly expensive likely to be better to do everything in mercator
		}
		validate();
	}
	
	/** Calculate the largest zoom-in factor that can be applied to the current
	 * view.
	 * @return The largest allowable zoom-in factor.
	 */
	private float zoomInLimit() {
		return  (width - MIN_ZOOM_WIDTH) / 2f / width;
	}
	
	/**
	 * Calculate the largest zoom-out factor that can be applied to the current
	 * view.
	 * @return The largest allowable zoom-out factor.
	 */
	private float zoomOutLimit() {
		long mTop = GeoMath.latE7ToMercatorE7(top);
		long mBottom = GeoMath.latE7ToMercatorE7(bottom);
		long mHeight = mTop - mBottom; 
		return -Math.min((MAX_ZOOM_WIDTH - width) / 2f / width, ((2L*(long)GeoMath.MAX_MLAT_E7) - mHeight) / 2f /mHeight);
	}
	
	/**
	 * Test if the box can be zoomed in.
	 * @return true if the box can be zoomed in, false if it can't.
	 */
	public boolean canZoomIn() {
		return (ZOOM_IN < zoomInLimit());
	}
	
	/**
	 * Test if the box can be zoomed out.
	 * @return true if the box can be zoomed out, false if it can't.
	 */
	public boolean canZoomOut() {
		// return (ZOOM_OUT > zoomOutLimit());
		return zoomOutLimit() < -3.1E-9; // determined experimental
	}
	
	/**
	 * Reduces this bounding box by the ZOOM_IN factor. The ratio of width and
	 * height remains.
	 */
	public void zoomIn()  {
		try {
			zoom(ZOOM_IN);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Enlarges this bounding box by the ZOOM_OUT factor. The ratio of width
	 * and height remains.
	 */
	public void zoomOut()  {
		try {
			zoom(ZOOM_OUT);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Enlarges/reduces the borders by zoomFactor.
	 * 
	 * @param zoomFactor factor enlarge/reduce the borders.
	 */
	public void zoom(float zoomFactor) throws OsmException {
		// Log.d("BoundingBox","zoom " + this.toString());
		zoomFactor = Math.min(zoomInLimit(), zoomFactor);
		zoomFactor = Math.max(zoomOutLimit(), zoomFactor);
	
		long mTop = GeoMath.latE7ToMercatorE7(top);
		long mBottom = GeoMath.latE7ToMercatorE7(bottom);
		long mHeight = mTop - mBottom;
		
		long horizontalChange = (long)(width * zoomFactor);
		long verticalChange = (long)(mHeight * zoomFactor);
		long tmpLeft=left; 
		long tmpRight=right;
		// 
		if (tmpLeft + horizontalChange < (long)-MAX_LON_E7) {
			long rest = left + horizontalChange + (long)MAX_LON_E7;
			tmpLeft = -MAX_LON_E7;
			tmpRight = tmpRight - rest;
		} else {
			tmpLeft = tmpLeft + horizontalChange;
		}
		if (tmpRight - horizontalChange > (long)MAX_LON_E7) {
			long rest = tmpRight - horizontalChange - (long)MAX_LON_E7;
			tmpRight = MAX_LON_E7;
			tmpLeft = Math.max((long)-MAX_LON_E7,tmpLeft + rest);
		} else {
			tmpRight = tmpRight - horizontalChange;
		}
		left = (int)tmpLeft;
		right = (int)tmpRight;
		// left = Math.max(-MAX_LON, left + (int)horizontalChange);
		// right = Math.min(MAX_LON, right - (int)horizontalChange);
		
		if ((mBottom + verticalChange) < -GeoMath.MAX_MLAT_E7) {
			long rest = mBottom + (long)verticalChange + (long)GeoMath.MAX_MLAT_E7;
			mBottom = - GeoMath.MAX_MLAT_E7;
			mTop = mTop - rest;	
		} else {
			mBottom = mBottom + verticalChange;
		}
		if ((mTop - verticalChange) > (long)GeoMath.MAX_MLAT_E7) {
			long rest = mTop - verticalChange - (long)GeoMath.MAX_MLAT_E7;
			mTop = GeoMath.MAX_MLAT_E7;
			mBottom = Math.max(-GeoMath.MAX_MLAT_E7,mBottom - rest);	
		} else {
			mTop = mTop - verticalChange;
		}
		bottom = GeoMath.mercatorE7ToLatE7((int)mBottom);
		top = GeoMath.mercatorE7ToLatE7((int)mTop);
		// bottom = Math.max(-MAX_LAT_E7, GeoMath.mercatorE7ToLatE7((int)(mBottom + (long)verticalChange)));
		// top = Math.min(MAX_LAT_E7, GeoMath.mercatorE7ToLatE7((int)(mTop - (long)verticalChange)));

		// setRatio(ratio, true);
		
		calcDimensions(); // need to do this or else centering will not work
		calcBottomMercator();
	}
	
	/**
	 * set current zoom level to a tile zoom level equivalent, powers of 2 assuming 256x256 tiles
	 * maintain center of bounding box
	 * @param tileZoomLevel The TMS zoom level to zoom to (from 0 for the whole world to about 19 for small areas).
	 */
	public void setZoom(Map map, int tileZoomLevel) {
		// setting an exact zoom level implies one screen pixel == one tile pixel
		// calculate one pixel in degrees (mercator) at this zoom level
		double degE7PerPixel = 3600000000.0d / (256*Math.pow(2, tileZoomLevel));
		double wDegE7 = map.getWidth() * degE7PerPixel;
		double hDegE7 = map.getHeight() * degE7PerPixel;
		long centerLon = left + width/2;
		left = (int) (centerLon - wDegE7/2);
		right = (int) (left + wDegE7);
		long mBottom = GeoMath.latE7ToMercatorE7(bottom);
		long mTop = GeoMath.latE7ToMercatorE7(top);
		long centerLat = mBottom + (mTop-mBottom)/2;
		bottom = GeoMath.mercatorE7ToLatE7((int)(centerLat - hDegE7/2));
		top = GeoMath.mercatorE7ToLatE7((int)(centerLat + hDegE7/2));
		calcDimensions(); // 
		calcBottomMercator();
	}

	/**
	 * Sets the borders to the ones of newBox. Recalculates dimensions to fit the current ratio (that of the window) 
	 * and maintains zoom level
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
	 * @param newBox
	 * @param ratio
	 */
	private void setBorders(Map map, final BoundingBox newBox, float ratio) {
		setBorders(map, newBox, ratio, true);
	}
	
	/**
	 * Sets the borders to the ones of newBox. Recalculates dimensions to fit the current ratio (that of the window) 
	 * and maintains zoom level depending on the value of preserveZoom
	 * 
	 * @param map current map view
	 * @param newBox new bounding box
	 * @param preserveZoom maintain current zoom level
	 */
	public void setBorders(final Map map, final BoundingBox newBox, boolean preserveZoom) {
		setBorders(map, newBox, this.ratio, preserveZoom);
	}
	
	/**
	 * Sets the borders to the ones of newBox. Recalculates dimensions to fit the current ratio (that of the window) 
	 * and maintains zoom level depending on the value of preserveZoom
	 * 
	 * @param map map current map view
	 * @param newBox new bounding box
	 * @param ratio current window ratio
	 * @param preserveZoom maintain current zoom level
	 */
	public void setBorders(final Map map, final BoundingBox newBox, float ratio, boolean preserveZoom) {
		left = newBox.left;
		right = newBox.right;
		top = newBox.top;
		bottom = newBox.bottom;
		Log.d("BoundingBox","setBorders " + newBox.toString() + " ratio is " + ratio);
		try {
			calcDimensions(); // neede to recalc width
			setRatio(map, ratio, preserveZoom);
			validate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //TODO slightly expensive likely to be better to do everything in mercator
	}
	
	/**
	 * Make the bounding box a valid request for the API, shrinking into its center if necessary.
	 */
	public void makeValidForApi() throws OsmException {
		if (!isValidForApi()) {
			int centerx = (left / 2 + right / 2); // divide first to stay < 2^32
			int centery = (top + bottom) / 2;
			left   = centerx - API_MAX_DEGREE_DIFFERENCE / 2;
			right  = centerx + API_MAX_DEGREE_DIFFERENCE / 2;
			top    = centery + API_MAX_DEGREE_DIFFERENCE / 2;
			bottom = centery - API_MAX_DEGREE_DIFFERENCE / 2;
			calcDimensions();
			calcBottomMercator();
		}
		validate();
	}
	
	private void validate() throws OsmException {
		if (!isValid()) {
			Log.e(DEBUG_TAG, toString());
			throw new OsmException("left must be less than right and bottom must be less than top");
		}
	}

	public boolean contains(BoundingBox bb) {
		return (bb.bottom >= bottom) && (bb.top <= top) && (bb.left >= left) && (bb.right <= right);
	}
	
	/**
	 * Returns true if the coordinates are in the box
	 * Right and top coordinate are considered inside
	 * @param lonE7
	 * @param latE7
	 * @return
	 */
	public boolean contains(int lonE7, int latE7) {
		return left <= lonE7 && lonE7 <= right && bottom <= latE7 && latE7 <= top;
	}

	/**
	 * Return pre-caclulated meracator value of bottom of the bounding box
	 * @return
	 */
	public double getBottomMercator() {
		return bottomMercator;
	}

	/**
	 * The setters are private since without calling calcDimensions the BB will be left in an inconsistent state
	 * @param latE7
	 */
	private void setTop(int latE7) {
		this.top = latE7;
	}
	
	private void setBottom(int latE7) {
		this.bottom = latE7;
	}
	
	private void setRight(int lonE7) {
		this.right = lonE7;
	}
	
	private void setLeft(int lonE7) {
		this.left = lonE7;
	}
	
	/**
	 * Return lat value of the center of the bounding box 
	 * @return
	 */
	public double getCenterLat() {
		int mBottom = GeoMath.latE7ToMercatorE7(bottom);
		int mHeight = GeoMath.latE7ToMercatorE7(top) - mBottom;
		return GeoMath.mercatorE7ToLat(mBottom + mHeight/2); 
	}
	
	/**
	 * Given a list of existing bounding boxes and a new bbox. Return a list of pieces of the new bbox that complete the coverage
	 * @param existing
	 * @param newBox
	 * @return
	 * @throws OsmException 
	 */
	public static ArrayList<BoundingBox> newBoxes(ArrayList<BoundingBox> existing, BoundingBox newBox) {
		ArrayList<BoundingBox> result = new ArrayList<BoundingBox>();
		result.add(newBox);
		for (BoundingBox b:existing) {
			ArrayList<BoundingBox> temp = new ArrayList<BoundingBox>();
			for (BoundingBox rb:result) {
				if (b.intersects(rb)) {
					try {
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
						rb.calcBottomMercator();
					} catch (OsmException e) {
						Log.d("BoundingBox", "Exception " + e.getMessage());
					}
				} else {
					temp.add(rb);
				}
			}
			result = temp;
		}
		return result;
	}
	
	@Override
	public void toJosmXml(final XmlSerializer s)
			throws IllegalArgumentException, IllegalStateException, IOException {
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
	 * CAREFUL does not update other fields
	 * @param b
	 */
	public void set(BoundingBox b) {
		left=b.left;
		bottom=b.bottom;
		right=b.right;
		top=b.top;
		width = b.width;
		height = b.height;
	}

	/**
	 * grow this box so that it covers the point
	 * @param lonE7
	 * @param latE7
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
		width = right - left;
		height = top - bottom;
	}
	
	/**
	 * grow this box so that it covers b
	 * @param b
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
		width = right - left;
		height = top - bottom;
	}
	
	/**
	 * Return true if box is empty
	 * @return
	 */
	public boolean isEmpty() {
		return left == right && top == bottom;
	}
}
