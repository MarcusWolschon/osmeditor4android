package de.blau.android.osm;

import java.io.Serializable;

import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.util.GeoMath;

/**
 * BoundingBox represents a bounding box for a selection of an area. All values are in decimal-degree (WGS84),
 * multiplied by 1E7.
 * 
 * @author mb
 */
public class BoundingBox implements Serializable {

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
	private int width;

	/**
	 * The height of the bounding box. Always positive.
	 */
	private int height;

	/**
	 * Factor for stretching the latitude to fit the Mercator Projection.
	 */
	private double mercatorFactorPow3;

	/**
	 * delimiter for the bounding box as String representation.
	 */
	public static final String STRING_DELIMITER = ",";

	/**
	 * The name of the tag in the OSM-XML file.
	 */
	public static final String NAME = "bounds";

	/**
	 * Default zoom in factor. Have to be grater than 1.
	 */
	private static final float ZOOM_IN = 8;

	/**
	 * Default zoom out factor. Have to be less than -1.
	 */
	private static final float ZOOM_OUT = -6;

	/**
	 * The maximum difference between two borders of the bounding box for the OSM-API. {@link http
	 * ://wiki.openstreetmap.org/index.php/Getting_Data#Construct_an_URL_for_the_HTTP_API }
	 */
	private static final int API_MAX_DEGREE_DIFFERENCE = 5000000;

	/**
	 * Maximum latitude ({@link GeoMath#MAX_LAT}) in 1E7.
	 */
	public static final int MAX_LAT = (int) (GeoMath.MAX_LAT * 1E7);

	/**
	 * Maximum Longitude.
	 */
	public static final int MAX_LON = 1800000000;

	/**
	 * Minimum width to zoom in.
	 */
	private static final int MIN_ZOOM_WIDTH = 1000;

	/**
	 * Maximum width to zoom out.
	 */
	private static final int MAX_ZOOM_WIDTH = 500000;

	private static final String DEBUG_TAG = BoundingBox.class.getSimpleName();

	/**
	 * Count of Zoom operations.
	 */
	private int zoomCount = 0;

	/**
	 * Number of zoom operations after which should the ratio reset.
	 */
	private static final int RESET_RATIO_AFTER_ZOOMCOUNT = 100;

	/**
	 * the ratio of this BoundingBox. Only needed when it's used as a viewbox.
	 */
	private float ratio = 1;

	/**
	 * Generates a bounding box with the given borders. Of course, left must be left to right and top must be top of
	 * bottom.
	 * 
	 * @param left degree of the left Border, multiplied by 1E7
	 * @param bottom degree of the bottom Border, multiplied by 1E7
	 * @param right degree of the right Border, multiplied by 1E7
	 * @param top degree of the top Border, multiplied by 1E7
	 * @throws OsmException when the borders are mixed up or outside of {@link #MAX_LAT}/{@link #MAX_LON} (!
	 *             {@link #isValid()})
	 */
	public BoundingBox(final int left, final int bottom, final int right, final int top) throws OsmException {
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.top = top;
		calcDimensions();
		calcMercatorFactorPow3();
		if (!isValid()) {
			Log.e(DEBUG_TAG, toString());
			throw new OsmException("left must be less than right and bottom must be less than top");
		}
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
	public BoundingBox(final double centerLat, final double centerLon, final double radius) throws OsmException {
		this(centerLon - radius, centerLat - radius, centerLon + radius, centerLat + radius);
	}

	/**
	 * Copy-Constructor.
	 * 
	 * @param box box with the new borders.
	 * @throws OsmException see {@link #BoundingBox(int, int, int, int)}
	 */
	public BoundingBox(final BoundingBox box) throws OsmException {
		this(box.left, box.bottom, box.right, box.top);
	}

	/**
	 * @return returns a copy of this object.
	 */
	public BoundingBox copy() {
		try {
			return new BoundingBox(this);
		} catch (OsmException e) {
			//Not possible.
			return null;
		}
	}

	/**
	 * Checks if the bounding box is valid for the OSM API.
	 * 
	 * @return true, if the bbox is smaller than 0.5*0.5 (here multiplied by 1E7) degree.
	 */
	public boolean isValidForApi() {
		return isValid() && (width < API_MAX_DEGREE_DIFFERENCE) && (height < API_MAX_DEGREE_DIFFERENCE);
	}

	/**
	 * @return true if left is less than right and bottom is less than top.
	 */
	public boolean isValid() {
		return (left < right) && (bottom < top) && (left >= -MAX_LON) && (right <= MAX_LON) && (top <= MAX_LAT)
				&& (bottom >= -MAX_LAT);
	}

	/**
	 * @return a String, representing the bounding box. Format: "left,bottom,right,top" in decimal degrees.
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

	public int getLeft() {
		return left;
	}

	public int getBottom() {
		return bottom;
	}

	public int getRight() {
		return right;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public double getMercatorFactorPow3() {
		return mercatorFactorPow3;
	}

	/**
	 * Checks if lat/lon is in this bounding box.
	 * 
	 * @param lat
	 * @param lon
	 * @return true if lat/lon is inside this bounding box.
	 */
	public boolean isIn(final int lat, final int lon) {
		return lon >= left && lon <= right && lat >= bottom && lat <= top;
	}

	/**
	 * Checks if a line between lat/lon and lat2/lon2 may intersect with this bounding box.
	 * 
	 * @param lat
	 * @param lon
	 * @param lat2
	 * @param lon2
	 * @return true, when at least one lat/lon is inside, or a intersection could not be excluded.
	 */
	public boolean intersects(final int lat, final int lon, final int lat2, final int lon2) {
		if (isIn(lat, lon) || isIn(lat2, lon2)) {
			return true;
		}
		if (!isIntersectionPossible(lat, lon, lat2, lon2)) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if an intersection with a line between lat/lon and lat2/lon2 is impossible. If two coordinates (lat/lat2
	 * or lon/lon2) are outside of a border, no intersection is possible.
	 * 
	 * @param lat
	 * @param lon
	 * @param lat2
	 * @param lon2
	 * @return true, when an intersection is possible.
	 */
	private boolean isIntersectionPossible(final int lat, final int lon, final int lat2, final int lon2) {
		return !(lat > top && lat2 > top || lat < bottom && lat2 < bottom || lon > right && lon2 > right || lon < left
				&& lon2 < left);
	}

	/**
	 * Calculates the dimensions width and height of this bounding box.
	 */
	private void calcDimensions() {
		width = right - left;
		height = top - bottom;
	}

	/**
	 * Calculates the Mercator-Factor powers 3. In later calculations with the {@link #mercatorFactorPow3}, it would
	 * always be multiplied 3 times with itself. So we do it here once.
	 */
	private void calcMercatorFactorPow3() {
		//have to use floatingpoint, otherwise strange things will happen due to rounding errors.
		final double centerLat = ((bottom + height / 2) / 1E7);
		//powers 3 because it would be needed in later usage of this factor
		mercatorFactorPow3 = GeoMath.getMercartorFactorPow3(centerLat);
	}

	/**
	 * Changes the dimensions of this bounding box to fit the given ratio. Ratio is width divided by height. The
	 * smallest dimension will remain, the larger one will be resized to fit ratio.
	 * @param ratio The new aspect ratio.
	 */
	public void setRatio(final float ratio) {
		setRatio(ratio, false);
	}
	
	/**
	 * Changes the dimensions of this bounding box to fit the given ratio.
	 * @param ratio The new aspect ratio.
	 * @param preserveZoom If true, maintains the current level of zoom by creating a new
	 * boundingbox at the required ratio at the same center. If false, the new bounding box is
	 * sized such that the currently visible area is still visible with the new aspect ratio
	 * applied.
	 */
	public void setRatio(final float ratio, final boolean preserveZoom) {
		if ((ratio > 0) && (ratio != Float.NaN)) {
			if (preserveZoom) {
				// Apply the new aspect ratio, but preserve the level of zoom so that
				// for example, rotating portrait<-->landscape won't zoom out
				int centerx = (left / 2 + right / 2); // divide first to stay < 2^32
				int centery = (top + bottom) / 2;
				int smallest = Math.min(Math.abs(right - left), Math.abs(bottom - top)) / 2;
				if (ratio < 1.0f) {
					// tall
					left = centerx - smallest;
					right = centerx + smallest;
					smallest = (int)((float)smallest / ratio);
					top = centery + smallest;
					bottom = centery - smallest;
				} else {
					// wide
					top = centery + smallest;
					bottom = centery - smallest;
					smallest = (int)((float)smallest * ratio);
					left = centerx - smallest;
					right = centerx + smallest;
				}
			}
			else {
				int singleBorderMovement;
				// Ensure currently visible area is entirely visible in the new box
				if ((width / height) < ratio) {
					//The actual box is wider than it should be.
					/* Here comes the math:
					 * width/height = ratio
					 * width = ratio * height
					 * newWidth = width - ratio * height
					 */
					singleBorderMovement = Math.round((width - ratio * height) / 2);
					left += singleBorderMovement;
					right -= singleBorderMovement;
				} else {
					//The actual box is more narrow than it should be.
					/* Same in here, only different:
					 * width/height = ratio
					 * height = width/ratio
					 * newHeight = height - width/ratio
					 */
					singleBorderMovement = Math.round((height - width / ratio) / 2);
					bottom += singleBorderMovement;
					top -= singleBorderMovement;
				}
			}
			//border-sizes changed. So we have to recalculate the dimensions.
			calcDimensions();
			Log.w(DEBUG_TAG, "Ratio: " + ratio);
			this.ratio = ratio;
		}
	}

	/**
	 * Performs a translation so the center of this bounding box will be at (lonCenter|latCenter).
	 * 
	 * @param lonCenter the absolute longitude for the center
	 * @param latCenter the absoulte latitude for the center
	 */
	public void moveTo(final int lonCenter, final int latCenter) {
		int mercatorHeight = (int) (mercatorFactorPow3 * height - height);
		translate(lonCenter - left - width / 2, latCenter - bottom - mercatorHeight / 2);
	}

	/**
	 * Relative translation.
	 * 
	 * @param lon the relative longitude change.
	 * @param lat the relative latitude change.
	 */
	public void translate(final int lon, final int lat) {
		if (lon > 0 && right + lon > MAX_LON) {
			return;
		} else if (lon < 0 && left + lon < -MAX_LON) {
			return;
		} else if (lat > 0 && top + lat > MAX_LAT) {
			return;
		} else if (lat < 0 && bottom + lat < -MAX_LAT) {
			return;
		}
		left += lon;
		right += lon;
		top += lat;
		bottom += lat;
		calcMercatorFactorPow3();
	}

	/**
	 * Reduces this bounding box by the DEFAULT_ZOOM_FACTOR. The ratio of width and height remains.
	 */
	public void zoomIn() {
		if (width > MIN_ZOOM_WIDTH) {
			zoom(ZOOM_IN);
		}
	}

	/**
	 * Enlarges this bounding box by the DEFAULT_ZOOM_FACTOR. The ratio of width and height remains.
	 */
	public void zoomOut() {
		if (width < MAX_ZOOM_WIDTH) {
			zoom(ZOOM_OUT);
		}
	}

	/**
	 * Enlarges/reduces the borders by zoomFactor.
	 * 
	 * @param zoomFactor factor enlarge/reduce the borders.
	 */
	private void zoom(final float zoomFactor) {
		float verticalChange = width / zoomFactor;
		float horizontalChange = height / zoomFactor;
		left += verticalChange;
		right -= verticalChange;
		bottom += horizontalChange;
		top -= horizontalChange;
		//Due to Mercator-Factor-Projection we have to translate to the new center.
		translate(0, getZoomingTranslation(zoomFactor));
		calcDimensions();
		if (zoomCount++ % RESET_RATIO_AFTER_ZOOMCOUNT == 0) {
			setRatio(ratio);
		}
	}

	/**
	 * translation needed to set the middle back in the middle when we've got a mercator-factor.
	 * 
	 * @return the translation value for center-correction.
	 */
	private int getZoomingTranslation(final float zoomfactor) {
		double halfHeight = height / 2.0;
		double mercatorDiff = mercatorFactorPow3 * halfHeight - halfHeight;
		return (int) -(mercatorDiff / zoomfactor);
	}

	/**
	 * Sets the borders to the ones of newBox. Recalculates dimensions and mercator-factor.
	 * 
	 * @param newBox bos with the new borders.
	 */
	public void setBorders(final BoundingBox newBox) {
		left = newBox.left;
		right = newBox.right;
		top = newBox.top;
		bottom = newBox.bottom;
		calcDimensions();
		calcMercatorFactorPow3();
	}

}
