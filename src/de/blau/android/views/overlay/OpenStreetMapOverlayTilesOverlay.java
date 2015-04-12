package de.blau.android.views.overlay;

import android.graphics.Canvas;
import android.view.View;
import de.blau.android.Application;
import de.blau.android.Map;
import de.blau.android.views.IMapView;
import de.blau.android.views.util.OpenStreetMapTileServer;

public class OpenStreetMapOverlayTilesOverlay extends OpenStreetMapTilesOverlay {
	
	Map map;
	
	public OpenStreetMapOverlayTilesOverlay(final View aView) {
		super(aView, OpenStreetMapTileServer.get(Application.mainActivity, ((Map)aView).getPrefs().overlayLayer(), true), null);
		map = (Map)aView;
	}
	
	@Override
	public boolean isReadyToDraw() {
		if (!getRendererInfo().getId().equals("NONE")) {
			return super.isReadyToDraw();
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas c, IMapView osmv) {
		if (!getRendererInfo().getId().equals("NONE"))
			super.onDraw(c, osmv);
	}
}
