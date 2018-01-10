package de.blau.android.views.overlay;

import android.graphics.Canvas;
import android.view.View;
import de.blau.android.Map;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.views.IMapView;

public class MapOverlayTilesOverlay extends MapTilesOverlay {

    private Map     map;
    private boolean enabled = false;

    public MapOverlayTilesOverlay(final View aView) {
        super(aView, TileLayerServer.get(aView.getContext(), ((Map) aView).getPrefs().overlayLayer(), true), null);
        map = (Map) aView;
    }

    @Override
    public boolean isReadyToDraw() {
        enabled = !getRendererInfo().getId().equals("NONE");
        return enabled && super.isReadyToDraw();
    }

    @Override
    public void onDraw(Canvas c, IMapView osmv) {
        if (enabled) {
            super.onDraw(c, osmv);
        }
    }
}
