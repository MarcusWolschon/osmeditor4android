package de.blau.android.layer.mvt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfException;
import com.mapbox.turf.TurfJoins;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Map;
import de.blau.android.dialogs.FeatureInfo;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.StyleableInterface;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.mvt.Style;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

public class MapOverlay extends MapTilesOverlayLayer<List<VectorTileDecoder.Feature>>
        implements ClickableInterface<VectorTileDecoder.Feature>, StyleableInterface {

    private static final String DEBUG_TAG = "mvt";

    /** Map this is an overlay of. */
    private final Map     map;
    private final boolean overlay;

    private final TileRenderer<List<VectorTileDecoder.Feature>> tileRenderer;

    private SavingHelper<java.util.HashMap<String, Style>> styleSavingHelper = new SavingHelper<>();

    /**
     * Construct a new MVT layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(@NonNull final Map map, @NonNull TileRenderer<List<VectorTileDecoder.Feature>> aTileRenderer, boolean overlay) {
        super(map, /* TileLayerSource.get(aView.getContext()), null, true), null, */ aTileRenderer);
        this.map = map;
        this.tileRenderer = aTileRenderer;
        this.overlay = overlay;
    }

    @Override
    public LayerType getType() {
        return overlay ? LayerType.OVERLAYIMAGERY : LayerType.IMAGERY;
    }

    @Override
    public List<VectorTileDecoder.Feature> getClicked(final float x, final float y, final ViewBox viewBox) {
        Log.d(DEBUG_TAG, "getClicked");
        List<VectorTileDecoder.Feature> result = new ArrayList<>();
        int z = map.getZoomLevel();

        MapTile mapTile = getTile(z, x, y);
        List<VectorTileDecoder.Feature> tile = mTileProvider.getMapTileFromCache(mapTile);
        while (tile == null && z > myRendererInfo.getMinZoom()) { // try zooming out
            z--;
            mapTile = getTile(z, x, y);
            tile = mTileProvider.getMapTileFromCache(mapTile);
        }
        if (tile != null) {
            Rect rect = getScreenRectForTile(new Rect(), map.getWidth(), map.getHeight(), map, mapTile.zoomLevel, mapTile.y, mapTile.x, true, 0, 0);
            final float tolerance = DataStyle.getCurrent().getNodeToleranceValue() * 256 / rect.width();
            final float scaledX = (x - rect.left) * 256 / rect.width();
            final float scaledY = (y - rect.top) * 256 / rect.height();
            for (VectorTileDecoder.Feature f : tile) {
                Geometry g = f.getGeometry();
                if (geometryClicked(scaledX, scaledY, tolerance, g)) {
                    result.add(f);
                }
            }
        } else {
            Log.e(DEBUG_TAG, "Tile not found");
            mTileProvider.getCacheUsageInfo();
        }
        return result;
    }

    /**
     * Check if a geometry has been clicked
     * 
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
     * @param viewBox Map view box.
     * @param tolerance the tolerance value to use
     * @param g the Geometry
     * @return true if clicked
     */
    @SuppressWarnings("unchecked")
    private boolean geometryClicked(final float x, final float y, final float tolerance, @NonNull Geometry g) {
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            return inToleranceArea(tolerance, (Point) g, x, y);
        case GeoJSONConstants.MULTIPOINT:
            for (Point q : ((CoordinateContainer<List<Point>>) g).coordinates()) {
                if (inToleranceArea(tolerance, q, x, y)) {
                    return true;
                }
            }
            break;
        case GeoJSONConstants.LINESTRING:
            return distanceToLineString(x, y, ((CoordinateContainer<List<Point>>) g).coordinates(), tolerance) >= 0;
        case GeoJSONConstants.MULTILINESTRING:
            for (List<Point> l : ((CoordinateContainer<List<List<Point>>>) g).coordinates()) {
                if (distanceToLineString(x, y, l, tolerance) >= 0) {
                    return true;
                }
            }
            break;
        case GeoJSONConstants.POLYGON:
        case GeoJSONConstants.MULTIPOLYGON:
            try {
                final Point point = Point.fromLngLat(x, y);
                return GeoJSONConstants.POLYGON.equals(g.type()) ? TurfJoins.inside(point, (Polygon) g) : TurfJoins.inside(point, (MultiPolygon) g);
            } catch (TurfException e) {
                Log.e(DEBUG_TAG, "Exception in getClicked " + e);
            }
            break;
        default:
            Log.e(DEBUG_TAG, "Unsupported geometry " + g.type());
        }
        return false;
    }

    /**
     * Check if the current touch position is in the tolerance area around a Point
     * 
     * @param viewBox the current screen ViewBox
     * @param tolerance the tolerance value
     * @param p the Position
     * @param x screen x coordinate of touch location
     * @param y screen y coordinate of touch location
     * @return true if touch position is in tolerance
     */
    private boolean inToleranceArea(float tolerance, @NonNull Point p, float x, float y) {
        float differenceX = (float) Math.abs(p.longitude() - x);
        float differenceY = (float) Math.abs(p.latitude() - y);
        return differenceX <= tolerance && differenceY <= tolerance && Math.hypot(differenceX, differenceY) <= tolerance;
    }

    /**
     * Determine if screen coords are within the tolerance for a geojson linestring
     * 
     * @param x x screen coord
     * @param y y screen coord
     * @param map map object
     * @param viewBox the current ViewBox
     * @param vertices the list of lineString vertices
     * @return if the returned value is > 0 then the coords are in the tolerance
     */
    private double distanceToLineString(final float x, final float y, List<Point> vertices, float tolerance) {
        float p1X = Float.MAX_VALUE;
        float p1Y = Float.MAX_VALUE;
        // Iterate over all points, but not the last one.
        for (int k = 0, verticesSize = vertices.size(); k < verticesSize - 1; ++k) {
            Point p1 = vertices.get(k);
            Point p2 = vertices.get(k + 1);
            if (k == 0) {
                p1X = (float) p1.longitude();
                p1Y = (float) p1.latitude();
            }
            float p2X = (float) p2.longitude();
            float p2Y = (float) p2.latitude();
            double distance = de.blau.android.util.Geometry.isPositionOnLine(tolerance / 2, x, y, p1X, p1Y, p2X, p2Y);
            if (distance >= 0) {
                return distance;
            }
            p1X = p2X;
            p1Y = p2Y;
        }
        return -1;
    }

    /**
     * Get the tile
     * 
     * @param z zoom level
     * @param x screen x
     * @param y screen y
     * @return the Tile or null
     */
    @Nullable
    private MapTile getTile(int z, float x, float y) {
        int n = 1 << z;
        int tileX = xTileNumber(GeoMath.xToLonE7(map.getWidth(), map.getViewBox(), x) / 1E7D, n);
        int tileY = yTileNumber((float) Math.toRadians(GeoMath.yToLatE7(map.getHeight(), map.getWidth(), map.getViewBox(), y) / 1E7D), n);
        return new MapTile(myRendererInfo.getId(), z, tileX, tileY);
    }

    @Override
    public void deselectObjects() {
        // Empty
    }

    @Override
    public void onSelected(FragmentActivity activity, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        JsonObject properties = new JsonObject();
        for (Entry<String, Object> e : f.getAttributes().entrySet()) {
            if (e.getValue() instanceof String) {
                properties.add(e.getKey(), new JsonPrimitive((String) e.getValue()));
            } else {
                properties.add(e.getKey(), new JsonPrimitive(e.getValue().toString()));
            }
        }
        com.mapbox.geojson.Feature geojson = com.mapbox.geojson.Feature.fromGeometry(f.getGeometry(), properties);
        FeatureInfo.showDialog(activity, geojson);
    }

    @Override
    public de.blau.android.util.mvt.VectorTileDecoder.Feature getSelected() {
        return null;
    }

    @Override
    public void setSelected(de.blau.android.util.mvt.VectorTileDecoder.Feature o) {
        // Empty
    }

    @Override
    public String getDescription(de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        return f.getLayerName() + " " + f.getGeometry().type() + " " + f.getId();
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public int getColor(String layerName) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            return style.getLinePaint().getColor();
        }
        return 0;
    }

    @Override
    public void setColor(int color) {
        // empty
    }

    @Override
    public void setColor(String layerName, int color) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            style.getLinePaint().setColor(color);
            style.getPointPaint().setColor(color);
            style.getPolygonPaint().setColor(color);
        }
    }

    @Override
    public float getStrokeWidth() {
        return 0;
    }

    @Override
    public float getStrokeWidth(String layerName) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            return style.getLinePaint().getStrokeWidth();
        }
        return 0;
    }

    @Override
    public void setStrokeWidth(float width) {
        // empty
    }

    @Override
    public void setStrokeWidth(String layerName, float width) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            style.getLinePaint().setStrokeWidth(width);
            style.getPointPaint().setStrokeWidth(width);
            style.getPolygonPaint().setStrokeWidth(width);
        }
    }

    @Override
    public String getPointSymbol(@NonNull String layerName) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            return style.getSymbolName();
        }
        return null;
    }

    /**
     * Set the Path for the symbol for points for a specific sub-layer of this layer
     * 
     * @param layerName the (sub)layer name
     * @param symbol the Path for symbol
     */
    @Override
    public void setPointSymbol(@NonNull String layerName, @NonNull String symbol) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            style.setSymbolName(symbol);
            Style.setSymbolPathFromName(style);
        }
    }

    @Override
    public void resetStyling() {
        ((VectorTileRenderer) tileRenderer).getLayerStyles().clear();
    }

    @Override
    public List<String> getLabelList(@NonNull String layerName) {
        return ((VectorTileRenderer) tileRenderer).getAttributeKeys(layerName);
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public String getLabel(String layerName) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            return style.getLabelKey();
        }
        return null;
    }

    @Override
    public void setLabel(@NonNull String key) {
        // handled by the sub-layer method
    }

    @Override
    public void setLabel(String layerName, String key) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            style.setLabelKey(key);
        }
    }

    @Override
    public int getMinZoom(@NonNull String layerName) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            return style.getMinZoom();
        }
        return 0;
    }

    @Override
    public void setMinZoom(@NonNull String layerName, int zoom) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            style.setMinZoom(zoom);
        }
    }

    @Override
    public int getMaxZoom(String layerName) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            return style.getMaxZoom();
        }
        return -1;
    }

    @Override
    public void setMaxZoom(@NonNull String layerName, int zoom) {
        Style style = ((VectorTileRenderer) tileRenderer).getLayerStyle(layerName);
        if (style != null) {
            style.setMaxZoom(zoom);
        }
    }

    @Override
    public List<String> getLayerList() {
        return ((VectorTileRenderer) tileRenderer).getLayerNames();
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        super.onSaveState(ctx);
        styleSavingHelper.save(ctx, getStateFileName(), ((VectorTileRenderer) tileRenderer).getLayerStyles(), false, true);
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        super.onRestoreState(ctx);
        HashMap<String, Style> styles = styleSavingHelper.load(ctx, getStateFileName(), false, true, true);
        if (styles != null) {
            // restore transient Style fields
            for (Style style : styles.values()) {
                Path pointPath = DataStyle.getCurrent().getSymbol(style.getSymbolName());
                style.setSymbolPath(pointPath != null ? pointPath : null);
            }
            ((VectorTileRenderer) tileRenderer).setLayerStyles(styles);
        }
        return true;
    }

    /**
     * Get an unique state filename
     * 
     * @return a filename
     */
    public String getStateFileName() {
        return (getTileLayerConfiguration().getImageryOffsetId() + ".res").replace('/', '-');
    }
}
