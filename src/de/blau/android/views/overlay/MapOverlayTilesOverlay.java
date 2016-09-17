package de.blau.android.views.overlay;

import android.graphics.Canvas;
import android.view.View;
import de.blau.android.Application;
import de.blau.android.Map;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.views.IMapView;

public class MapOverlayTilesOverlay extends MapTilesOverlay {
	
	Map map;
	boolean enabled = false;
	
	public MapOverlayTilesOverlay(final View aView) {
		super(aView, TileLayerServer.get(Application.mainActivity, ((Map)aView).getPrefs().overlayLayer(), true), null);
		map = (Map)aView;
	}
	
	@Override
	public boolean isReadyToDraw() {
		enabled = !getRendererInfo().getId().equals("NONE");
		if (enabled) {
			return super.isReadyToDraw();
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas c, IMapView osmv) {
		if (enabled)
			super.onDraw(c, osmv);
	}
}
