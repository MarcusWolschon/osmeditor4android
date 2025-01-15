package de.blau.android.util.mvt;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.mvt.style.Background;
import de.blau.android.util.mvt.style.Fill;
import de.blau.android.util.mvt.style.Layer;
import de.blau.android.util.mvt.style.Line;
import de.blau.android.util.mvt.style.Style;
import de.blau.android.util.mvt.style.Symbol;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.util.MapTileProvider.TileDecoder;

/**
 * Very simple Mapbox Vector Tile renderer
 * 
 * 
 * @author Simon Poole
 *
 */
public class VectorTileRenderer implements MapTilesLayer.TileRenderer<Map<String, List<VectorTileDecoder.Feature>>> {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, VectorTileRenderer.class.getSimpleName().length());
    private static final String DEBUG_TAG = VectorTileRenderer.class.getSimpleName().substring(0, TAG_LEN);

    private VectorTileDecoder decoder = new VectorTileDecoder();

    private float scaleX = 1f;
    private float scaleY = 1f;

    private Set<String>              layerNames    = new HashSet<>();
    private Map<String, Set<String>> attributeKeys = new HashMap<>();

    private Rect destinationRect;
    private Rect screenRect;
    private Rect tempRect = new Rect();

    private Style   style;
    private int     lastZoom      = -1;
    private Picture symbolPicture = new Picture();
    private Canvas  symbolCanvas;
    private boolean renderPass;

    /**
     * Create a new instance
     */
    public VectorTileRenderer() {
        resetStyle();
    }

    /**
     * Set the style for the renderer
     * 
     * @param style the Style object
     */
    public void setStyle(@NonNull Style style) {
        this.style = style;
        lastZoom = -1;
    }

    /**
     * Get the renderer's current Style
     * 
     * @return a Style
     */
    @NonNull
    public Style getStyle() {
        return style;
    }

    /**
     * Reset the style to an empty one
     */
    public void resetStyle() {
        setStyle(new Style());
        style.setAutoStyle(true);
    }

    /**
     * Get a specific style layer for a source layer
     * 
     * @param sourceLayer the source layer name
     * @param type the Layer.Type we want
     * @return the style or null
     */
    @Nullable
    public Layer getLayer(@NonNull String sourceLayer, @NonNull Layer.Type type) {
        for (Layer layer : style.getLayers(sourceLayer)) {
            switch (type) {
            case LINE:
                if (layer instanceof Line) {
                    return layer;
                }
                break;
            case FILL:
                if (layer instanceof Fill) {
                    return layer;
                }
                break;
            case SYMBOL:
                if (layer instanceof Symbol) {
                    return layer;
                }
                break;
            default:
                // unimplemented
                break;
            }
        }
        return null;
    }

    private List<Layer>                     layerToRender    = new ArrayList<>();
    private List<VectorTileDecoder.Feature> featuresToRender = new ArrayList<>();

    @Override
    public void preRender(@NonNull Canvas c, int z) {
        style.resetCollisionDetection();
        setScreenRect(c);
        symbolPicture.endRecording(); // if we haven't finished rendering abort here
        symbolCanvas = symbolPicture.beginRecording(screenRect.width(), screenRect.height());
    }

    @Override
    public void postRender(@NonNull Canvas c, int z) {
        symbolPicture.draw(c);
        if (renderPass) {
            // we need to be sure that we've actually processed tiles
            lastZoom = z;
            renderPass = false;
        }
    }

    @Override
    public void render(@NonNull Canvas c, @NonNull Map<String, List<VectorTileDecoder.Feature>> features, int z, @Nullable Rect fromRect,
            @NonNull Rect destinationRect, @NonNull Paint paint) {
        renderPass = true;
        scaleX = destinationRect.width() / 256f;
        scaleY = destinationRect.height() / 256f;
        this.destinationRect = destinationRect;
        layerToRender.clear();
        List<Layer> temp = style.getLayers();
        synchronized (temp) { // protect against CME
            layerToRender.addAll(temp);
        }

        for (Layer layer : layerToRender) {
            if (layer.isVisible() && z >= layer.getMinZoom() && (layer.getMaxZoom() == -1 || z <= layer.getMaxZoom())) {
                if (layer instanceof Background) {
                    if (z != lastZoom) {
                        layer.onZoomChange(style, null, z);
                    }
                    layer.render(c, style, null, z, screenRect, destinationRect, scaleX, scaleX);
                    continue;
                }
                // feature rendering
                List<VectorTileDecoder.Feature> list = features.get(layer.getSourceLayer());
                if (list != null) {
                    JsonArray filter = layer.getFilter();
                    featuresToRender.clear();
                    for (VectorTileDecoder.Feature feature : list) {
                        if (intersectsScreen(feature) && (filter == null || layer.evaluateFilter(filter, feature))) {
                            featuresToRender.add(feature);
                        }
                    }
                    // FIXME sort here when implemented
                    if (layer instanceof Symbol) {
                        // labels and icons are not clipped at tile boundaries, further to avoid covering them if they
                        // do exceed tile boundaries, we record to symbolCanvas and then draw all of them when
                        // everything else has been done
                        renderFeatures(symbolCanvas, layer, z, destinationRect, featuresToRender);
                    } else {
                        c.save();
                        c.clipRect(destinationRect);
                        renderFeatures(c, layer, z, destinationRect, featuresToRender);
                        c.restore();
                    }
                }
            }
        }
    }

    /**
     * Set and save the screen rect if it hasn't been yet
     * 
     * @param c the Canvas
     */
    private void setScreenRect(@NonNull Canvas c) {
        if (screenRect == null) {
            screenRect = new Rect();
        }
        c.getClipBounds(screenRect);
    }

    /**
     * Actually render a list of features
     * 
     * @param c the target Canvas
     * @param layer the current Layer
     * @param z current zoom level
     * @param destinationRect destination rect
     * @param features the List of Features
     */
    private void renderFeatures(@NonNull Canvas c, @NonNull Layer layer, int z, @NonNull Rect destinationRect,
            @NonNull List<VectorTileDecoder.Feature> features) {

        boolean newZoom = z != lastZoom;
        for (VectorTileDecoder.Feature feature : features) {
            if (newZoom) {
                layer.onZoomChange(style, feature, z);
            }
            layer.render(c, style, feature, z, screenRect, destinationRect, scaleX, scaleX);
        }

    }

    @Override
    @NonNull
    public TileDecoder<Map<String, List<VectorTileDecoder.Feature>>> decoder(@NonNull de.blau.android.Map map) {
        return (byte[] data, boolean small) -> {
            try {
                Map<String, List<VectorTileDecoder.Feature>> features = decoder.decode(data).asMap();
                for (List<VectorTileDecoder.Feature> values : features.values()) {
                    for (VectorTileDecoder.Feature feature : values) {
                        final String sourceLayer = feature.getLayerName();
                        layerNames.add(sourceLayer);
                        if (style.isAutoStyle()) {
                            style.addAutoLayers(map, sourceLayer);
                        }
                        Set<String> keys = attributeKeys.get(sourceLayer);
                        if (keys == null) {
                            keys = new HashSet<>();
                            attributeKeys.put(sourceLayer, keys);
                        }
                        keys.addAll(feature.getAttributes().keySet());
                    }
                }
                return features;
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "decoding failed with " + e.getMessage());
                return null;
            }
        };
    }

    /**
     * Get a list of all layer names we've seen up to now
     * 
     * @return a List of names
     */
    @NonNull
    public List<String> getLayerNames() {
        return new ArrayList<>(layerNames);
    }

    /**
     * Get a list of all attribute keys we've seen up to now
     * 
     * @param layerName the keys for the layer
     * @return a List of keys
     */
    @NonNull
    public List<String> getAttributeKeys(@NonNull String layerName) {
        Set<String> keys = attributeKeys.get(layerName);
        return keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    }

    /**
     * Check if the feature intersects the screen
     * 
     * @param f the Feature
     * @return true if intersects the screen
     */
    private boolean intersectsScreen(@NonNull VectorTileDecoder.Feature f) {
        Geometry g = f.getGeometry();
        if (GeoJSONConstants.POINT.equals(g.type())) {
            return screenRect.contains(destinationRect.left + (int) (((Point) g).longitude() * scaleX),
                    destinationRect.top + (int) (((Point) g).latitude() * scaleY));
        }
        tempRect.set(f.getBox());
        tempRect.right = destinationRect.left + (int) (tempRect.right * scaleX);
        tempRect.left = destinationRect.left + (int) (tempRect.left * scaleX);
        tempRect.bottom = destinationRect.top + (int) (tempRect.bottom * scaleY);
        tempRect.top = destinationRect.top + (int) (tempRect.top * scaleY);
        return tempRect.intersect(screenRect);
    }
}
