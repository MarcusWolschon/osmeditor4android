package de.blau.android.views;


import android.graphics.Canvas;
import android.graphics.Rect;
import de.blau.android.osm.BoundingBox;

/**
 * Interface for a graphical component that can
 * be used with {@link org.andnav.osm.views.overlay.OpenStreetMapTilesOverlay} and
 * other {@link org.andnav.osm.views.overlay.OpenStreetMapViewOverlay} to render on.
 */
public interface IMapView {

	/**
	 * Get the current ZoomLevel for the map tiles.
	 * @param viewPort 
	 * @return the current ZoomLevel between 0 (equator) and 18/19(closest),
	 *         depending on the Renderer chosen.
	 */
	public abstract int getZoomLevel(Rect viewPort);

	/**
	 * @return The visible area in decimal-degree (WGS84) -space.
	 */
	public BoundingBox getViewBox();
}