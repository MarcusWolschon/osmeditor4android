package de.blau.android.views;


import de.blau.android.osm.BoundingBox;

/**
 * Interface for a graphical component that can
 * be used with {@link org.MapTilesOverlay.osm.views.overlay.OpenStreetMapTilesOverlay} and
 * other {@link org.MapViewOverlay.osm.views.overlay.OpenStreetMapViewOverlay} to render on.
 */
public interface IMapView {

	/**
	 * Get the current ZoomLevel for the map tiles.
	 * @return the current ZoomLevel between 0 (equator) and 18/19(closest),
	 *         depending on the Renderer chosen.
	 */
	public abstract int getZoomLevel();

	/**
	 * @return The visible area in decimal-degree (WGS84) -space.
	 */
	public BoundingBox getViewBox();
}