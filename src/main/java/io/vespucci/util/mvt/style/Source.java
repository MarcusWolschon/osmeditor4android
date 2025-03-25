package io.vespucci.util.mvt.style;

import java.io.Serializable;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.resources.TileLayerSource.Category;
import io.vespucci.resources.TileLayerSource.Provider;
import io.vespucci.resources.TileLayerSource.TileType;
import io.vespucci.resources.TileLayerSource.Provider.CoverageArea;
import io.vespucci.util.GeoMath;

public class Source implements Serializable {

    private static final long serialVersionUID = 1L;

    enum SourceType {
        VECTOR, RASTER
    }

    final SourceType  type;
    String[]          tileUrls;
    final BoundingBox bounds  = new BoundingBox(-GeoMath.MAX_LON, -GeoMath.MAX_COMPAT_LAT, GeoMath.MAX_LON, GeoMath.MAX_COMPAT_LAT);
    int               minZoom = 0;
    int               maxZoom = 22;
    String            attribution;

    /**
     * Construct a new Source
     * 
     * @param type the type of Source
     */
    public Source(@NonNull SourceType type) {
        this.type = type;
    }

    /**
     * Get the array of tile urls
     * 
     * @return the tileUrls
     */
    @Nullable
    public String[] getTileUrls() {
        return tileUrls;
    }

    /**
     * Get the minimum supported zoom level for this source
     * 
     * @return the maximum zoom level
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * Get the maximum supported zoom level for this source
     * 
     * @return the maximum zoom level
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * Get the attribution String for this source if set
     * 
     * @return the attribution string or null
     */
    @Nullable
    public String getAttribution() {
        return attribution;
    }

    /**
     * Get the bounding box / extent for this layer
     * 
     * @return the bounding box
     */
    @NonNull
    public BoundingBox getBounds() {
        return bounds;
    }

    /**
     * Add this source as a custom layer
     * 
     * @param ctx an Android Context
     * @param db writable TileLayerDatabase
     * @param name name of the layer
     * @param category layer Category
     * @param isOverlay true if the layer is an overlay
     * @return the id for the layer
     */
    @NonNull
    public String createLayer(@NonNull final Context ctx, @NonNull final SQLiteDatabase db, @NonNull String name, @NonNull Category category,
            boolean isOverlay) {
        Provider provider = new Provider();
        provider.setAttribution(attribution);
        provider.addCoverageArea(new CoverageArea(minZoom, maxZoom, bounds));
        String id = name.toUpperCase(Locale.US);
        for (int i = 1; i < 10; i++) {
            if (TileLayerSource.get(ctx, id, isOverlay) == null) {
                // doesn't exist, good
                TileLayerSource.addOrUpdateCustomLayer(ctx, db, id, null, -1, -1, name, provider, category, TileLayerSource.TYPE_TMS, TileType.MVT, minZoom, maxZoom,
                        TileLayerSource.DEFAULT_TILE_SIZE, isOverlay, tileUrls[0]);
                return id;
            }
            id = id + "-" + Integer.toString(i); // NOSONAR
        }
        throw new OsmIllegalOperationException("Existing layers ids");
    }
}