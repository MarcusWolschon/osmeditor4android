package io.vespucci.layer.tiles;

import android.graphics.Canvas;
import androidx.annotation.NonNull;
import io.vespucci.Map;
import io.vespucci.layer.LayerType;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.util.collections.MRUList;
import io.vespucci.views.IMapView;

public class MapTilesOverlayLayer<T> extends MapTilesLayer<T> {

    private static final MRUList<String> lastServers = new MRUList<>(MRU_SIZE);

    private boolean enabled = false;

    /**
     * Construct a tile layer for showing transparent tiles over over tiles/data
     * 
     * @param map the view we are displaying in
     * @param aTileRenderer the TileRenderer for T
     */
    public MapTilesOverlayLayer(@NonNull final Map map, @NonNull TileRenderer<T> aTileRenderer) {
        super(map, TileLayerSource.get(map.getContext(), null, true), null, aTileRenderer);
    }

    @Override
    public boolean isReadyToDraw() {
        TileLayerSource layerSource = getTileLayerConfiguration();
        if (layerSource == null) {
            return false;
        }
        String id = layerSource.getId();
        enabled = !(TileLayerSource.LAYER_NOOVERLAY.equals(id) || "".equals(id));
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

    @Override
    MRUList<String> getLastServers() {
        return lastServers;
    }
}
