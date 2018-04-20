package de.blau.android.views;

import de.blau.android.osm.ViewBox;

/**
 * Interface for a graphical component that can be used with
 * {@link org.MapTilesLayer.osm.views.overlay.OpenStreetMapTilesOverlay} and other
 * {@link de.blau.android.layer.MapViewLayer.osm.views.overlay.OpenStreetMapViewOverlay} to render on.
 */
public interface IMapView {

    /**
     * Get the current zoomLevel for the map tiles.
     * 
     * Note layers using this are responsible for clamping this value to whatever min/max zoom they support
     * 
     * @return the current ZoomLevel
     */
    int getZoomLevel();

    /**
     * @return The visible area in decimal-degree (WGS84) -space.
     */
    ViewBox getViewBox();
}