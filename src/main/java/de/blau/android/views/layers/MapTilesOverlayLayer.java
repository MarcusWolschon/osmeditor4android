package de.blau.android.views.layers;

import android.graphics.Canvas;
import android.view.View;
import de.blau.android.layer.LayerType;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.views.IMapView;

public class MapTilesOverlayLayer extends MapTilesLayer {

    private boolean enabled = false;

    /**
     * Construct a tile layer for showing transparent tiles over over tiles/data
     * 
     * @param aView the view we are displaying in
     */
    public MapTilesOverlayLayer(final View aView) {
        super(aView, TileLayerSource.get(aView.getContext(), null, true), null);
    }

    @Override
    public boolean isReadyToDraw() {
        TileLayerSource layer = getTileLayerConfiguration();
        if (layer == null) {
            enabled = false;
        } else {
            String id = layer.getId();
            enabled = !(id == null || TileLayerSource.LAYER_NOOVERLAY.equals(id) || TileLayerSource.LAYER_NONE.equals(id) || "".equals(id));
        }
        return enabled && super.isReadyToDraw();
    }

    @Override
    public void onDraw(Canvas c, IMapView osmv) {
        if (enabled) {
            super.onDraw(c, osmv);
        }
    }
    
    @Override
    public LayerType getType() {
       return LayerType.OVERLAYIMAGERY;
    }
}
