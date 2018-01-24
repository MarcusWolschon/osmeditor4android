package de.blau.android.views.layers;

import android.graphics.Canvas;
import android.view.View;
import de.blau.android.Map;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.views.IMapView;

public class MapTilesOverlayLayer extends MapTilesLayer {

    private boolean enabled = false;

    /**
     * Construct a tile layer for showing transparent tiles over over tiles/data
     * 
     * @param aView the view we are displaying in
     */
    public MapTilesOverlayLayer(final View aView) {
        super(aView, TileLayerServer.get(aView.getContext(), ((Map) aView).getPrefs().overlayLayer(), true), null);
    }

    @Override
    public boolean isReadyToDraw() {
        TileLayerServer layer = getRendererInfo();
        if (layer == null) {
            enabled = false;
        } else {
            String id = layer.getId();
            enabled = !(id == null || TileLayerServer.LAYER_NOOVERLAY.equals(id) || TileLayerServer.LAYER_NONE.equals(id) || "".equals(id));
        }
        return enabled && super.isReadyToDraw();
    }

    @Override
    public void onDraw(Canvas c, IMapView osmv) {
        if (enabled) {
            super.onDraw(c, osmv);
        }
    }
}
