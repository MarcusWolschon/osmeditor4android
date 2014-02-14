package de.blau.android.views.overlay;

import android.graphics.Canvas;
import android.view.View;
import de.blau.android.Map;
import de.blau.android.views.IMapView;
import de.blau.android.views.util.OpenStreetMapTileServer;

public class OpenStreetMapGPSTilesOverlay extends OpenStreetMapTilesOverlay {
	
	Map map;
	
	public OpenStreetMapGPSTilesOverlay(final View aView) {
		super(aView, OpenStreetMapTileServer.get(aView.getResources(), "OSMGPS", true), null);
		map = (Map)aView;
	}
	
	@Override
	public boolean isReadyToDraw() {
		if (map.getPrefs().isGPSOverlayEnabled()) {
			return super.isReadyToDraw();
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas c, IMapView osmv) {
		if (map.getPrefs().isGPSOverlayEnabled())
			super.onDraw(c, osmv);
	}
}
