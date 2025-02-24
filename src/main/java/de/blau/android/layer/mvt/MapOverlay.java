package de.blau.android.layer.mvt;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
import android.graphics.Rect;
import android.net.Uri;
import android.text.SpannableString;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.dialogs.FeatureInfo;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.StyleableInterface;
import de.blau.android.layer.tiles.MapTilesOverlayLayer;
import de.blau.android.layer.tiles.util.MapTileProvider;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Hash;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SelectFile;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.util.mvt.style.Background;
import de.blau.android.util.mvt.style.Layer;
import de.blau.android.util.mvt.style.Layer.Type;
import de.blau.android.util.mvt.style.Style;
import de.blau.android.util.mvt.style.Symbol;

public class MapOverlay extends MapTilesOverlayLayer<java.util.Map<String, List<VectorTileDecoder.Feature>>>
        implements ClickableInterface<VectorTileDecoder.Feature>, StyleableInterface {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    private static final Type[] DEFAULT_STYLE_TYPES = new Type[] { Type.LINE, Type.FILL, Type.SYMBOL };

    private final boolean overlay;

    protected final TileRenderer<java.util.Map<String, List<VectorTileDecoder.Feature>>> tileRenderer;

    private final SavingHelper<Style> styleSavingHelper = new SavingHelper<>();
    private boolean                   dirty             = false;

    /**
     * Construct a new MVT layer
     * 
     * @param map the current Map instance
     * @param aTileRenderer the TileRenderer to use
     * @param overlay if true this is an overlay
     */
    public MapOverlay(@NonNull final Map map, @NonNull TileRenderer<java.util.Map<String, List<VectorTileDecoder.Feature>>> aTileRenderer, boolean overlay) {
        super(map, aTileRenderer);
        this.tileRenderer = aTileRenderer;
        this.overlay = overlay;
    }

    @Override
    @NonNull
    public LayerType getType() {
        return overlay ? LayerType.OVERLAYIMAGERY : LayerType.IMAGERY;
    }

    @Override
    public boolean stylingEnabled() {
        return ((VectorTileRenderer) tileRenderer).getStyle().isAutoStyle();
    }

    @Override
    public List<VectorTileDecoder.Feature> getClicked(final float x, final float y, @NonNull final ViewBox viewBox) {
        Log.d(DEBUG_TAG, "getClicked");

        Set<VectorTileDecoder.Feature> result = new LinkedHashSet<>();
        if (layerSource == null) {
            Log.e(DEBUG_TAG, "No tile layer source set");
            return new ArrayList<>();
        }
        int z = map.getZoomLevel();

        MapTile mapTile = getTile(z, x, y);
        java.util.Map<String, List<VectorTileDecoder.Feature>> tile = mTileProvider.getMapTileFromCache(mapTile);
        while (tile == null && z > layerSource.getMinZoom()) { // try zooming out
            z--;
            mapTile = getTile(z, x, y);
            tile = mTileProvider.getMapTileFromCache(mapTile);
        }
        if (tile == null) {
            Log.e(DEBUG_TAG, "Tile " + mapTile + " not found in cache");
            return new ArrayList<>();
        }
        Rect rect = getScreenRectForTile(new Rect(), map.getWidth(), map.getHeight(), map, mapTile.zoomLevel, mapTile.y, mapTile.x, true, 0, 0);
        final float tolerance = map.getDataStyle().getCurrent().getNodeToleranceValue() * 256 / rect.width();
        final float scaledX = (x - rect.left) * 256 / rect.width();
        final float scaledY = (y - rect.top) * 256 / rect.height();
        Style style = ((VectorTileRenderer) tileRenderer).getStyle();
        // we need layer information to be able to check the interactive status
        for (Layer layer : style.getLayers()) {
            if (layer instanceof Background) {
                continue; // this is not particularly safe
            }
            for (List<VectorTileDecoder.Feature> list : tile.values()) {
                for (VectorTileDecoder.Feature f : list) {
                    if (f.getLayerName().equals(layer.getSourceLayer()) && (layer.getFilter() == null || layer.evaluateFilter(layer.getFilter(), f))
                            && layer.isInteractive()) {
                        Geometry g = f.getGeometry();
                        if (geometryClicked(scaledX, scaledY, tolerance, g)) {
                            result.add(f);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Check if a geometry has been clicked
     * 
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
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
     * @param vertices the list of lineString vertices
     * @param tolerance the tolerance
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
    @NonNull
    private MapTile getTile(int z, float x, float y) {
        int n = 1 << z;
        int tileX = xTileNumber(GeoMath.xToLonE7(map.getWidth(), map.getViewBox(), x) / 1E7D, n);
        int tileY = yTileNumber((float) Math.toRadians(GeoMath.yToLatE7(map.getHeight(), map.getWidth(), map.getViewBox(), y) / 1E7D), n);
        return new MapTile(layerSource.getId(), z, tileX, tileY);
    }

    @Override
    public void deselectObjects() {
        // Empty
    }

    @Override
    public void onSelected(FragmentActivity activity, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        JsonObject properties = new JsonObject();
        properties.add(activity.getString(R.string.vt_layer), new JsonPrimitive(f.getLayerName()));
        for (Entry<String, Object> e : f.getAttributes().entrySet()) {
            if (e.getValue() instanceof String) {
                properties.add(e.getKey(), new JsonPrimitive((String) e.getValue()));
            } else {
                properties.add(e.getKey(), new JsonPrimitive(e.getValue().toString()));
            }
        }
        com.mapbox.geojson.Feature geojson = com.mapbox.geojson.Feature.fromGeometry(f.getGeometry(), properties);
        FeatureInfo.showDialog(activity, geojson, R.string.vt_feature_information);
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
    public SpannableString getDescription(de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        return getDescription(map.getContext(), f);
    }

    @Override
    public SpannableString getDescription(Context context, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        Object nameObject = f.getAttributes().get(Tags.KEY_NAME);
        return new SpannableString(
                (nameObject != null ? nameObject.toString() : Long.toString(f.getId())) + " " + f.getGeometry().type() + " " + f.getLayerName());
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public int getColor(@NonNull String layerName) {
        Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.LINE);
        if (style != null) {
            return style.getColor();
        }
        return 0;
    }

    @Override
    public void setColor(int color) {
        // empty
    }

    @Override
    public void setColor(@NonNull String layerName, int color) {
        for (Type type : DEFAULT_STYLE_TYPES) {
            Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, type);
            if (style != null) {
                style.setColor(color);
            }
        }
    }

    @Override
    public float getStrokeWidth() {
        return 0;
    }

    @Override
    public float getStrokeWidth(@NonNull String layerName) {
        Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.LINE);
        if (style != null) {
            return style.getStrokeWidth();
        }
        return 0;
    }

    @Override
    public void setStrokeWidth(float width) {
        // empty
    }

    @Override
    public void setStrokeWidth(@NonNull String layerName, float width) {
        for (Type type : DEFAULT_STYLE_TYPES) {
            Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, type);
            if (style != null) {
                style.setStrokeWidth(width);
            }
        }
    }

    @Override
    public String getPointSymbol(@NonNull String layerName) {
        Symbol style = (Symbol) ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.SYMBOL);
        if (style != null) {
            return style.getSymbol();
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
    public void setPointSymbol(@NonNull String layerName, @Nullable String symbol) {
        Symbol style = (Symbol) ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.SYMBOL);
        if (style != null) {
            style.setSymbol(symbol, map.getDataStyle());
            flushTileCache();
        }
    }

    @Override
    public void resetStyling() {
        ((VectorTileRenderer) tileRenderer).resetStyle();
    }

    @Override
    @NonNull
    public List<String> getLabelList(@Nullable String layerName) {
        return layerName != null ? ((VectorTileRenderer) tileRenderer).getAttributeKeys(layerName) : new ArrayList<>();
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public String getLabel(@NonNull String layerName) {
        Symbol style = (Symbol) ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.SYMBOL);
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
    public void setLabel(@NonNull String layerName, @NonNull String key) {
        Symbol style = (Symbol) ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.SYMBOL);
        if (style != null) {
            style.setLabelKey(key);
            style.setTextJustify(Style.TEXT_JUSTIFY_CENTER);
            flushTileCache();
        }
    }

    @Override
    public int getMinZoom(@NonNull String layerName) {
        Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.LINE);
        if (style != null) {
            return style.getMinZoom();
        }
        return 0;
    }

    @Override
    public void setMinZoom(@NonNull String layerName, int zoom) {
        for (Type type : DEFAULT_STYLE_TYPES) {
            Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, type);
            if (style != null) {
                style.setMinZoom(zoom);
            }
        }
    }

    @Override
    public int getMaxZoom(String layerName) {
        Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, Type.LINE);
        if (style != null) {
            return style.getMaxZoom();
        }
        return -1;
    }

    @Override
    public void setMaxZoom(@NonNull String layerName, int zoom) {
        for (Type type : DEFAULT_STYLE_TYPES) {
            Layer style = ((VectorTileRenderer) tileRenderer).getLayer(layerName, type);
            if (style != null) {
                style.setMaxZoom(zoom);
            }
        }
    }

    @Override
    @NonNull
    public List<String> getLayerList() {
        return ((VectorTileRenderer) tileRenderer).getLayerNames();
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        super.onSaveState(ctx);
        styleSavingHelper.save(ctx, getStateFileName(), ((VectorTileRenderer) tileRenderer).getStyle(), false, true);
        dirty = false;
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        super.onRestoreState(ctx);
        Style style = styleSavingHelper.load(ctx, getStateFileName(), false, true, true);
        if (style != null) {
            if (!dirty) {
                for (Layer layer : style.getLayers()) {
                    if (layer instanceof Symbol) {
                        // these need a DataStyle that isn't present when de-serializing
                        final DataStyle dataStyle = map.getDataStyle();
                        ((Symbol) layer).setSymbol(((Symbol) layer).getSymbol(), dataStyle);
                        ((Symbol) layer).setLabelFont(dataStyle.getInternal(DataStyle.LABELTEXT_NORMAL).getPaint().getTypeface());
                    }
                }
                ((VectorTileRenderer) tileRenderer).setStyle(style);
            }
        } else {
            ((VectorTileRenderer) tileRenderer).resetStyle();
        }
        return true;
    }

    /**
     * Get a state filename
     * 
     * If a TileLayerSource is set for the layer it will return a name based on that otherwise based on the class name
     * 
     * @return a filename
     */
    @NonNull
    private String getStateFileName() {
        TileLayerSource source = getTileLayerConfiguration();
        return (source != null ? Hash.sha256(source.getTileUrl()) : MapOverlay.class.getSimpleName()) + "." + FileExtensions.RES;
    }

    /**
     * Get the current style
     * 
     * @return the Style for this layer
     */
    @NonNull
    public Style getStyle() {
        return ((VectorTileRenderer) tileRenderer).getStyle();
    }

    /**
     * Set the style
     * 
     * @param style a Mapbox-GL derived Style or null
     */
    public void setStyle(@NonNull Style style) {
        ((VectorTileRenderer) tileRenderer).setStyle(style);
    }

    /**
     * Load a mapbox gl version 8 style file
     * 
     * @param activity the calling FragmentActivity
     * @throws IOException is reading goes wrong
     */
    public void loadStyleFromFile(@NonNull FragmentActivity activity) throws IOException {
        SelectFile.read(activity, R.string.config_osmPreferredDir_key, new ReadFile() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean read(FragmentActivity currentActivity, Uri fileUri) {
                try (InputStream is = currentActivity.getContentResolver().openInputStream(fileUri)) {
                    Style style = new Style();
                    style.loadStyle(currentActivity, is);
                    setStyle(style);
                    flushTileCache();
                    dirty = true;
                    Log.d(DEBUG_TAG, "Loaded " + fileUri + " successfully");
                    return true;
                } catch (SecurityException sex) {
                    Log.e(DEBUG_TAG, sex.getMessage());
                    // note need a context here that is on the ui thread
                    ScreenMessage.toastTopError(currentActivity, currentActivity.getString(R.string.toast_permission_denied, fileUri.toString()));
                    return false;
                } catch (FileNotFoundException e) {
                    ScreenMessage.toastTopError(currentActivity, currentActivity.getString(R.string.toast_file_not_found, fileUri.toString()));
                } catch (IOException e) {
                    ScreenMessage.toastTopError(currentActivity, currentActivity.getString(R.string.toast_error_reading, fileUri.toString()));
                }
                return true;
            }
        });
    }

    /**
     * Set dirty to true
     */
    protected void dirty() {
        dirty = true;
    }

    /**
     * Flush the in memory tile cache
     */
    protected void flushTileCache() {
        MapTileProvider<java.util.Map<String, List<VectorTileDecoder.Feature>>> provider = getTileProvider();
        provider.flushCache(layerSource.getId(), false);
    }
}
